# See if ANDROID_NDK env var is set
ifeq ($(strip $(ANDROID_NDK)),)
    $(error ANDROID_NDK is not set in environment. Set ANDROID_NDK properly.)
endif

$(TARGET)_prep=$(OUTDIR)/lib/libc++_shared.so

ANDROID_API_LEVEL=21

ifeq ($(PLATFORM),win32)
    android_host=windows-x86_64
else ifeq ($(PLATFORM),linux)
    android_host=linux-x86_64
else ifeq ($(PLATFORM),darwin)
    android_host=darwin-x86_64
else
    $(error unsupported host platform for android ndk)
endif

# Put Android NDK on path - must come first to appease openssl configure
# path searching heuristics...
android_tcpath := $(ANDROID_NDK)/toolchains/llvm/prebuilt/$(android_host)/bin
export PATH := $(android_tcpath):$(PATH)

$(OUTDIR)/lib/libc++_shared.so: $(ANDROID_NDK)/toolchains/llvm/prebuilt/$(android_host)/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so
	$(CP) $< $@

# Android ABI
ANDROID_ABI=arm64-v8a

# Tools
CC=aarch64-linux-android$(ANDROID_API_LEVEL)-clang
CXX=aarch64-linux-android$(ANDROID_API_LEVEL)-clang++
CPP=aarch64-linux-android$(ANDROID_API_LEVEL)-clang -E
LD=ld
RANLIB=llvm-ranlib
AR=llvm-ar
STRIP=llvm-strip -S

# "host" argument to autoconf-based configure scripts
# Leave blank for autodetect/non-cross compile
CONFIGURE_TARGET=--host aarch64-linux-android$(ANDROID_API_LEVEL)
CONFIGURE_debug=--enable-debug

# Library naming
LIB_PREFIX=lib
LIB_SHAREDSUFFIX=so
LIB_STATICSUFFIX=a

# Object file naming
OBJ_SUFFIX=o

# Flags - common to all packages
# aarch64 in ndk r25b needs no-outline-atomics to prevent undefined symbols
# in output due to missing builtins. See https://reviews.llvm.org/D93585 and
# https://github.com/android/ndk/issues/1614
CFLAGS_generic:=-mno-outline-atomics -fPIC -fstack-protector-all -march=armv8-a -ftree-vectorize
CFLAGS_release:=-O3
CFLAGS_debug:=-g -O0
CXXFLAGS_generic:=$(CFLAGS_generic)
CXXFLAGS_release:=$(CFLAGS_release)
CXXFLAGS_debug:=$(CFLAGS_debug)
LDFLAGS_generic:=-static-libstdc++ $(shell $(android_tcpath)/$(CC) -print-libgcc-file-name)

# Per-package flags
kdu_PLATFORM=arm64-clang

openssl_CFLAGS_generic=
openssl_CFLAGS_release=
openssl_CFLAGS_debug=
openssl_CXXFLAGS_generic=
openssl_CXXFLAGS_release=
openssl_CXXFLAGS_debug=
openssl_CONFIG=ANDROID_NDK_HOME=$(ANDROID_NDK) ./Configure android-arm64 -D__ANDROID_API__=$(ANDROID_API_LEVEL) no-shared no-module --prefix=$(OUTDIR_CYGSAFE) $(openssl_CFLAGS)
openssl_LDFLAGS=


# Target-specific patches for libkml, space separated
libkml_EXTRAPATCHES=
# Target-specific patches to be applied before libkml's autoconf is run
libkml_EXTRAPATCHES_PREAC=

# Mr. SID binary bundle file path
# binbundle has mrsid/ top level directory with include/ under that
mrsid_BINBUNDLE=$(DISTFILESDIR)/mrsid/android.tar.gz
# Path within expansion of above where the Mr.Sid library can be found
# omit mrsid/ initial directory
mrsid_BINLIBPATH=lib/arm64-v8a

gdal_CFLAGS_generic=-DKDU_MAJOR_VERSION=6
gdal_CXXFLAGS_generic=$(gdal_CFLAGS_generic)

assimp_CONFIG_EX=-DCMAKE_SYSTEM_NAME=Android                    \
    -DCMAKE_ANDROID_NDK=$(call PATH_CYGSAFE,$(ANDROID_NDK))     \
    -DCMAKE_SYSTEM_VERSION=$(ANDROID_API_LEVEL)                 \
    -DCMAKE_ANDROID_STL_TYPE=c++_static                         \
    -DCMAKE_ANDROID_ARCH_ABI=$(ANDROID_ABI)


curl_LIBS=-lc++ -lm -llog
proj_LIBS=-lc++ -lm -llog
expat_LIBS=-lc++ -lm -llog
libkml_LIBS=-lc++ -lm -llog
libspatialite_LIBS=-lc++ -lm -llog
protobuf_LIBS=-llog
gdal_LIBS=-L$(OUTDIR_CYGSAFE)/lib -lssl -lcrypto -lc++

gdal_LDFLAGS=

gdal_KILL_JAVA_INCLUDE=yes


libxml2_installtargets=install-libLTLIBRARIES install-data
libxml2_buildtarget=libxml2.la

#commoncommo_BUILDSTATIC=yes
commoncommo_BUILDJAVA=yes
