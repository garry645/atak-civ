#ifdef __ANDROID__

// This is a modified version of the `rehash` application for OpenSSL. The original logic is largely
// intact, but any functionality not required was removed.

/*
* Copyright 2015-2022 The OpenSSL Project Authors. All Rights Reserved.
* Copyright (c) 2013-2014 Timo Teräs <timo.teras@gmail.com>
*
* Licensed under the Apache License 2.0 (the "License").  You may not use
* this file except in compliance with the License.  You can obtain a copy
* in the file LICENSE in the source distribution or at
* https://www.openssl.org/source/license.html
*/

# include <unistd.h>
# include <stdio.h>
# include <limits.h>
# include <errno.h>
# include <string.h>
# include <ctype.h>
# include <sys/stat.h>

#include <vector> //+
#include <port/STLVectorAdapter.h> //+
#include <util/IO2.h> //+
#include <util/Logging2.h> //+

namespace
{

/*
* Make sure that the processing of symbol names is treated the same as when
* libcrypto is built.  This is done automatically for public headers (see
* include/openssl/__DECC_INCLUDE_PROLOGUE.H and __DECC_INCLUDE_EPILOGUE.H),
* but not for internal headers.
*/
# ifdef __VMS
#  pragma names save
#  pragma names as_is,shortened
# endif

# ifdef __VMS
#  pragma names restore
# endif

# include <openssl/evp.h>
# include <openssl/pem.h>
# include <openssl/x509.h>


# ifndef PATH_MAX
#  define PATH_MAX 4096
# endif
# ifndef NAME_MAX
#  define NAME_MAX 255
# endif
# define MAX_COLLISIONS  256

# if defined(OPENSSL_SYS_VXWORKS)
/*
* VxWorks has no symbolic links
*/

#  define lstat(path, buf) stat(path, buf)

int symlink(const char *target, const char *linkpath)
{
errno = ENOSYS;
return -1;
}

ssize_t readlink(const char *pathname, char *buf, size_t bufsiz)
{
errno = ENOSYS;
return -1;
}
# endif

/* vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv */
// API substitutions

#define BIO_printf(...) TAK::Engine::Util::Logger_log(__VA_ARGS__)
#define BIO_puts(ll, s) TAK::Engine::Util::Logger_log(ll, s)
#define bio_err TAK::Engine::Util::TELL_Error
#define bio_out TAK::Engine::Util::TELL_Info

#define opt_getprog() "openssl_rehash"

#define OPENSSL_strncasecmp(...) strncasecmp(__VA_ARGS__)
#define OPENSSL_strcasecmp(...) strcasecmp(__VA_ARGS__)

# define OSSL_NELEM(x)    (sizeof(x)/sizeof((x)[0]))

template<typename T>
T *app_malloc(size_t sz, const char *dbg) {
    return (T *) OPENSSL_malloc(sz);
}

int app_access(const char *path, int access) {
    // XXX -
    return 0;
}

struct OPENSSL_DIR_CTX {
    std::vector<TAK::Engine::Port::String> listing;
    std::size_t index{0u};
};

const char *OPENSSL_DIR_read(OPENSSL_DIR_CTX **ctx, const char *dirname) {
    if (!(*ctx)) {
        *ctx = new OPENSSL_DIR_CTX();
        TAK::Engine::Port::STLVectorAdapter<TAK::Engine::Port::String> listing((*ctx)->listing);
        TAK::Engine::Util::IO_listFiles(listing, dirname);
    }
    if ((*ctx)->index < (*ctx)->listing.size()) {
        const char *path = (*ctx)->listing[(*ctx)->index++].get();
        const size_t lim = strlen(path);
        for(size_t i = lim; i > 0; i--) {
            if(path[i-1u] == '/')
                return path+i;
        }
        return path;
    } else {
        return nullptr;
    }
}

void OPENSSL_DIR_end(OPENSSL_DIR_CTX **ctx) {
    if (*ctx)
        delete (*ctx);
}
/* ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ */

typedef struct hentry_st {
    struct hentry_st *next;
    char *filename;
    unsigned short old_id;
    unsigned char need_symlink;
    unsigned char digest[EVP_MAX_MD_SIZE];
} HENTRY;

typedef struct bucket_st {
    struct bucket_st *next;
    HENTRY *first_entry, *last_entry;
    unsigned int hash;
    unsigned short type;
    unsigned short num_needed;
} BUCKET;

enum Type {
    /* Keep in sync with |suffixes|, below. */
    TYPE_CERT = 0, TYPE_CRL = 1
};


static int evpmdsize;
static const EVP_MD *evpmd;
static int remove_links = 1;
static int verbose = 0;
static BUCKET *hash_table[257];

static const char *suffixes[] = {"", "r"};
static const char *extensions[] = {"pem", "crt", "cer", "crl"};


static void bit_set(unsigned char *set, unsigned int bit) {
    set[bit >> 3] |= 1 << (bit & 0x7);
}

static int bit_isset(unsigned char *set, unsigned int bit) {
    return set[bit >> 3] & (1 << (bit & 0x7));
}


/*
* Process an entry; return number of errors.
*/
static int add_entry(enum Type type, unsigned int hash, const char *filename,
                     const unsigned char *digest, int need_symlink,
                     unsigned short old_id) {
    static BUCKET nilbucket;
    static HENTRY nilhentry;
    BUCKET *bp;
    HENTRY *ep, *found = NULL;
    unsigned int ndx = (type + hash) % OSSL_NELEM(hash_table);

    for (bp = hash_table[ndx]; bp; bp = bp->next)
        if (bp->type == type && bp->hash == hash)
            break;
    if (bp == NULL) {
        bp = app_malloc<bucket_st>(sizeof(*bp), "hash bucket");
        *bp = nilbucket;
        bp->next = hash_table[ndx];
        bp->type = type;
        bp->hash = hash;
        hash_table[ndx] = bp;
    }

    for (ep = bp->first_entry; ep; ep = ep->next) {
        if (digest && memcmp(digest, ep->digest, evpmdsize) == 0) {
            BIO_printf(bio_err,
                       "%s: warning: skipping duplicate %s in %s\n",
                       opt_getprog(),
                       type == TYPE_CERT ? "certificate" : "CRL", filename);
            return 0;
        }
        if (strcmp(filename, ep->filename) == 0) {
            found = ep;
            if (digest == NULL)
                break;
        }
    }
    ep = found;
    if (ep == NULL) {
        if (bp->num_needed >= MAX_COLLISIONS) {
            BIO_printf(bio_err,
                       "%s: error: hash table overflow for %s\n",
                       opt_getprog(), filename);
            return 1;
        }
        ep = app_malloc<hentry_st>(sizeof(*ep), "collision bucket");
        *ep = nilhentry;
        ep->old_id = ~0;
        ep->filename = OPENSSL_strdup(filename);
        if (bp->last_entry)
            bp->last_entry->next = ep;
        if (bp->first_entry == NULL)
            bp->first_entry = ep;
        bp->last_entry = ep;
    }

    if (old_id < ep->old_id)
        ep->old_id = old_id;
    if (need_symlink && !ep->need_symlink) {
        ep->need_symlink = 1;
        bp->num_needed++;
        memcpy(ep->digest, digest, evpmdsize);
    }
    return 0;
}

/*
* Check if a symlink goes to the right spot; return 0 if okay.
* This can be -1 if bad filename, or an error count.
*/
static int handle_symlink(const char *filename, const char *fullpath) {
    unsigned int hash = 0;
    int i, type, id;
    unsigned char ch;
    char linktarget[PATH_MAX], *endptr;
    ossl_ssize_t n;

    for (i = 0; i < 8; i++) {
        ch = filename[i];
        if (!isxdigit(ch))
            return -1;
        hash <<= 4;
        hash += OPENSSL_hexchar2int(ch);
    }
    if (filename[i++] != '.')
        return -1;
    for (type = OSSL_NELEM(suffixes) - 1; type > 0; type--) {
        const char *suffix = suffixes[type];
        if (strncasecmp(suffix, &filename[i], strlen(suffix)) == 0)
            break;
    }
    i += strlen(suffixes[type]);

    id = strtoul(&filename[i], &endptr, 10);
    if (*endptr != '\0')
        return -1;

    n = readlink(fullpath, linktarget, sizeof(linktarget));
    if (n < 0 || n >= (int) sizeof(linktarget))
        return -1;
    linktarget[n] = 0;

    return add_entry((Type) type, hash, linktarget, NULL, 0, id);
}

/*
* process a file, return number of errors.
*/
static int do_file(const char *filename, const char *fullpath) {
    STACK_OF (X509_INFO) *inf = NULL;
    X509_INFO *x;
    X509_NAME *name = NULL;
    BIO *b;
    const char *ext;
    unsigned char digest[EVP_MAX_MD_SIZE];
    int type, errs = 0;
    size_t i;

    /* Does it have X.509 data in it? */
    if ((b = BIO_new_file(fullpath, "r")) == NULL) {
        BIO_printf(bio_err, "%s: error: skipping %s, cannot open file\n",
                   opt_getprog(), filename);
        errs++;
        goto end;
    }
    inf = PEM_X509_INFO_read_bio(b, NULL, NULL, NULL);
    BIO_free(b);
    if (inf == NULL)
        goto end;

    if (sk_X509_INFO_num(inf) != 1) {
        BIO_printf(bio_err,
                   "%s: warning: skipping %s,"
                   "it does not contain exactly one certificate or CRL\n",
                   opt_getprog(), filename);
        /* This is not an error. */
        goto end;
    }
    x = sk_X509_INFO_value(inf, 0);
    if (x->x509 != NULL) {
        type = TYPE_CERT;
        name = X509_get_subject_name(x->x509);
        X509_digest(x->x509, evpmd, digest, NULL);
    } else if (x->crl != NULL) {
        type = TYPE_CRL;
        name = X509_CRL_get_issuer(x->crl);
        X509_CRL_digest(x->crl, evpmd, digest, NULL);
    } else {
        ++errs;
        goto end;
    }
    if (name != NULL) {
        errs += add_entry((Type) type, X509_NAME_hash(name), filename, digest, 1, ~0);
    }

    end:
    sk_X509_INFO_pop_free(inf, X509_INFO_free);
    return errs;
}

static void str_free(char *s) {
    OPENSSL_free(s);
}

static int ends_with_dirsep(const char *path) {
    if (*path != '\0')
        path += strlen(path) - 1;
# if defined __VMS
    if (*path == ']' || *path == '>' || *path == ':')
    return 1;
# elif defined _WIN32
    if (*path == '\\')
    return 1;
# endif
    return *path == '/';
}

/*
* Process a directory; return number of errors found.
*/
static int do_dir(const char *sdirname, const char *ddirname) {
    BUCKET *bp, *nextbp;
    HENTRY *ep, *nextep;
    OPENSSL_DIR_CTX *d = NULL;
    struct stat st;
    unsigned char idmask[MAX_COLLISIONS / 8];
    int n, numfiles, nextid, buflen, errs = 0;
    size_t i;
    const char *pathsep;
    const char *filename;
    char *buf, *copy = NULL;
    STACK_OF(OPENSSL_STRING) *files = NULL;

    if (app_access(sdirname, W_OK) < 0) {
        BIO_printf(bio_err, "Skipping %s, can't write\n", sdirname);
        return 1;
    }
    buflen = strlen(sdirname);
    pathsep = (buflen && !ends_with_dirsep(sdirname)) ? "/" : "";
    buflen += NAME_MAX + 1 + 1;
    buf = app_malloc<char>(buflen, "filename buffer");

    if (verbose)
        BIO_printf(bio_out, "Doing %s\n", sdirname);

    if ((files = sk_OPENSSL_STRING_new_null()) == NULL) {
        BIO_printf(bio_err, "Skipping %s, out of memory\n", sdirname);
        errs = 1;
        goto err;
    }
    while ((filename = OPENSSL_DIR_read(&d, sdirname)) != NULL) {
        if ((copy = OPENSSL_strdup(filename)) == NULL
            || sk_OPENSSL_STRING_push(files, copy) == 0) {
            OPENSSL_free(copy);
            BIO_puts(bio_err, "out of memory\n");
            errs = 1;
            goto err;
        }
    }
    OPENSSL_DIR_end(&d);
    sk_OPENSSL_STRING_sort(files);

    numfiles = sk_OPENSSL_STRING_num(files);
    for (n = 0; n < numfiles; ++n) {
        filename = sk_OPENSSL_STRING_value(files, n);
        if (BIO_snprintf(buf, buflen, "%s%s%s",
                         sdirname, pathsep, filename) >= buflen)
            continue;
        if (lstat(buf, &st) < 0)
            continue;
        if (S_ISLNK(st.st_mode) && handle_symlink(filename, buf) == 0)
            continue;
        errs += do_file(filename, buf);
    }

    for (i = 0; i < OSSL_NELEM(hash_table); i++) {
        for (bp = hash_table[i]; bp; bp = nextbp) {
            nextbp = bp->next;
            nextid = 0;
            memset(idmask, 0, (bp->num_needed + 7) / 8);
            for (ep = bp->first_entry; ep; ep = ep->next)
                if (ep->old_id < bp->num_needed)
                    bit_set(idmask, ep->old_id);

            for (ep = bp->first_entry; ep; ep = nextep) {
                nextep = ep->next;
                /* New link needed (it may replace something) */
                while (bit_isset(idmask, nextid))
                    nextid++;

                BIO_snprintf(buf, buflen, "%s%s%s",
                             sdirname, pathsep, ep->filename);
                TAK::Engine::Port::String sfile(buf);
                BIO_snprintf(buf, buflen, "%s%s%n%08x.%s%d",
                             ddirname, pathsep, &n, bp->hash,
                             suffixes[bp->type], nextid);
                TAK::Engine::Port::String dfile(buf);
                if (verbose)
                    BIO_printf(bio_out, "new link %s -> %s\n",
                               sfile.get(), dfile.get());

                if (symlink(sfile, dfile) < 0) {
                    BIO_printf(TAK::Engine::Util::TELL_Warning,
                               "%s: Can't symlink %s, %s, copying\n",
                               opt_getprog(), ep->filename,
                               strerror(errno));


                    errs++;

                    // copy instead
                    TAK::Engine::Util::IO_copy(dfile, sfile);
                }
                bit_set(idmask, nextid);

                OPENSSL_free(ep->filename);
                OPENSSL_free(ep);
            }
            OPENSSL_free(bp);
        }
        hash_table[i] = NULL;
    }

    err:
    sk_OPENSSL_STRING_pop_free(files, str_free);
    OPENSSL_free(buf);
    return errs;
}
}

// extern'd
// NOT thread safe solely due to use of the global evp
int rehash(const char *srcdir, const char *dstdir)
{
    evpmd = EVP_sha1();
    evpmdsize = EVP_MD_get_size(evpmd);

    return do_dir(srcdir, dstdir);
}

#endif
