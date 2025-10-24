# Tools
CC=arm-linux-gnueabihf-gcc
CXX=arm-linux-gnueabihf-g++
CPP=arm-linux-gnueabihf-cpp
RANLIB=arm-linux-gnueabihf-ranlib
STRIP=arm-linux-gnueabihf-strip -S

# "host" argument to autoconf-based configure scripts
# Leave blank for autodetect/non-cross compile
# CONFIGURE_TARGET=--host blah-blah-blah
CONFIGURE_TARGET=--host arm-linux-gnueabihf
CONFIGURE_debug=--enable-debug

# Library naming
LIB_PREFIX=lib
LIB_SHAREDSUFFIX=so
LIB_STATICSUFFIX=a

# Object file naming
OBJ_SUFFIX=o

# Flags - common to all packages
CFLAGS_generic:=-fPIC
CFLAGS_release:=-O2
CFLAGS_debug:=-g -O0
CXXFLAGS_generic:=$(CFLAGS_generic)
CXXFLAGS_release:=$(CFLAGS_release)
CXXFLAGS_debug:=$(CFLAGS_debug)
LDFLAGS_generic:=

# Per-package flags
kdu_PLATFORM=Linux-x86-64-gcc

openssl_CFLAGS_generic=
openssl_CFLAGS_release=
openssl_CFLAGS_debug=
openssl_CXXFLAGS_generic=
openssl_CXXFLAGS_release=
openssl_CXXFLAGS_debug=
openssl_CONFIG=./Configure linux-latomic --cross-compile-prefix=arm-linux-gnueabihf- --prefix=$(OUTDIR_CYGSAFE) --libdir=lib $(openssl_CFLAGS) -DPURIFY no-asm no-module
openssl_LDFLAGS=


# Target-specific patches for libkml, space separated
libkml_EXTRAPATCHES=
# Target-specific patches to be applied before libkml's autoconf is run
libkml_EXTRAPATCHES_PREAC=

# Mr. SID binary bundle file path
# binbundle has mrsid/ top level directory with include/ under that
mrsid_BINBUNDLE=disabled
# Path within expansion of above where the Mr.Sid library can be found
# omit mrsid/ initial directory
mrsid_BINLIBPATH=disabled

gdal_CFLAGS_generic=-DKDU_MAJOR_VERSION=6
gdal_CXXFLAGS_generic=$(gdal_CFLAGS_generic)

assimp_CONFIG_EX=-DCMAKE_SYSTEM_NAME=Linux                       \
                 -DCMAKE_SYSTEM_PROCESSOR=armv7l                 \
                 -DCMAKE_C_COMPILER=$(CC)                        \
                 -DCMAKE_CXX_COMPILER=$(CXX)                     \
                 -DCMAKE_FIND_ROOT_PATH=/usr/arm-linux-gnueabihf \
                 -DCMAKE_FIND_ROOT_PATH_MODE_PROGRAM=NEVER

curl_LIBS=
proj_LIBS=
libkml_LIBS=
# tbb needed for mrsid dsdk
gdal_LIBS=-L$(OUTDIR)/lib -Wl,-rpath=$(OUTDIR)/lib -Wl,-rpath=$(OUTDIR)/lib/ogdi -lssl -lcrypto -ldl

gdal_LDFLAGS=-rpath=$(OUTDIR)/lib

libxml2_installtargets=install

commoncommo_BUILDJAVA=yes
commoncommo_ANT_FLAGS=-Dnative-init=internal
libspatialite_LIBS=-ldl -lpthread -lm -lstdc++
