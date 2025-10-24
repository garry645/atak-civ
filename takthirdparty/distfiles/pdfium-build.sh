#/bin/sh
# Adapted from
# https://raw.githubusercontent.com/rouault/pdfium/build/build-lin.sh
# and
# https://raw.githubusercontent.com/rouault/pdfium/build/build-win.bat

##########################
# Editable variables begin here
# Note: If editing variables after running once, completely removing
# the "pdfium" directory created herein is recommended.

# Build type
BUILDTYPE=Release
#BUILDTYPE=Debug

# System to build *for*. Building for windows supported only on windows
# and android only on Linux. 
# Pick *one* of these.
#SYSTEM=win64
#SYSTEM=win32
#SYSTEM=android
SYSTEM=linux-arm32
#SYSTEM=linux
#SYSTEM=macos

# Android API level (SYSTEM=android only)
API_LEVEL=21

# Version of MSVC to use (SYSTEM=win* only)
export GYP_MSVS_VERSION="2015"

# Path of takthirdparty.  (currently only needed for SYSTEM=win* only)
TTP=$(cd $(dirname $0) ; pwd -P)/../takthirdparty

# Python executable to use
# NOTE:  Python 2.x required!  Python 3.x will not work due to upstream
# gyp and pdfium requiring 2.x!
LOCAL_PYTHON=python2.7


# End editable variables
##########################


failstate=dep

function fail()
{
    if [ $# -ne 1 -o "$1" = "" ] ; then
        err="Unknown failure"
    else
        err=$1
    fi
    echo "$err" >&2
    if [ "$failstate" = "$dep" ] ; then
        rm -rf pdfium_deps
    fi
    exit 1
}

depsdir=`pwd`/pdfium_deps
export PATH=$depsdir/depot_tools:$PATH

if [ ! -e "pdfium_deps/.deps_ok" ] ; then
  rm -rf pdfium_deps
  mkdir pdfium_deps || fail
  cd pdfium_deps || fail

  git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git || fail "Could not clone depot_tools"
  echo "Depot tools rev:" > deps-revs
  ( cd depot_tools && git rev-parse HEAD >> ../deps-revs ) || fail
  git clone https://chromium.googlesource.com/external/gyp.git || fail "Could not clone gyp"
  echo "gyp rev:" >> deps-revs
  ( cd gyp && git rev-parse HEAD >> ../deps-revs ) || fail
  cd gyp || fail
  if [ "$SYSTEM" = "android" -o "$SYSTEM" = "linux" ] ; then
      $LOCAL_PYTHON ./setup.py build || fail "Failed to setup GYP"
  else
      $LOCAL_PYTHON ./setup.py install || fail "Failed to setup GYP"
  fi
  cd ../.. || fail
  touch pdfium_deps/.deps_ok
fi

failstate=main

# Download pdfium
if [ ! -e pdfium ] ; then
  git clone https://github.com/rouault/pdfium || fail "Could not clone pdfium"
  
  cp pdfium_deps/deps-revs pdfium/ || fail
  cd pdfium || fail
  if [ "$SYSTEM" = "android" -o "$SYSTEM" = "linux" ] ; then
    patch -p1 <<__EOF__
diff --git a/build/standalone.gypi b/build/standalone.gypi
index ecf849b..3407a56 100644
--- a/build/standalone.gypi
+++ b/build/standalone.gypi
@@ -151,7 +151,7 @@
       '-Wno-unused-parameter',
       '-pthread',
       '-fno-exceptions',
-      '-fvisibility=hidden',
+      #'-fvisibility=hidden',
       '-fPIC',
     ],
     'cflags_cc': [
diff --git a/core/src/fxge/android/fpf_skiafontmgr.cpp b/core/src/fxge/android/fpf_skiafontmgr.cpp
index 86bb052..939ff07 100644
--- a/core/src/fxge/android/fpf_skiafontmgr.cpp
+++ b/core/src/fxge/android/fpf_skiafontmgr.cpp
@@ -10,6 +10,7 @@
 #define FPF_SKIAMATCHWEIGHT_NAME2	60
 #define FPF_SKIAMATCHWEIGHT_1		16
 #define FPF_SKIAMATCHWEIGHT_2		8
+#include "../../../include/fxcrt/fx_ext.h"
 #include "fpf_skiafontmgr.h"
 #include "fpf_skiafont.h"
 #ifdef __cplusplus
diff --git a/pdfium.gyp b/pdfium.gyp
index 10cd716..f3bae94 100644
--- a/pdfium.gyp
+++ b/pdfium.gyp
@@ -18,6 +18,16 @@
       'V8_DEPRECATION_WARNINGS',
       '_CRT_SECURE_NO_WARNINGS',
     ],
+    'cflags':[
+      '-fPIC',
+    ],
+    'cflags_cc':[
+      '-std=c++11',
+      '-fPIC',
+    ],
+    'cflags!':[
+      '-fvisibility=hidden',
+    ], 
     'include_dirs': [
       'third_party/freetype/include',
     ],
diff --git a/core/src/fxcrt/fx_basic_list.cpp b/core/src/fxcrt/fx_basic_list.cpp
index c9619f9..8929f89 100644
--- a/core/src/fxcrt/fx_basic_list.cpp
+++ b/core/src/fxcrt/fx_basic_list.cpp
@@ -83,8 +83,10 @@ void CFX_PtrList::RemoveAll()
 {
     m_nCount = 0;
     m_pNodeHead = m_pNodeTail = m_pNodeFree = NULL;
+    if (m_pBlocks) {
     m_pBlocks->FreeDataChain();
     m_pBlocks = NULL;
+    }
 }
 CFX_PtrList::CNode*
 CFX_PtrList::NewNode(CFX_PtrList::CNode* pPrev, CFX_PtrList::CNode* pNext)
diff --git a/core/src/fxcrt/fx_basic_maps.cpp b/core/src/fxcrt/fx_basic_maps.cpp
index e34acb0..0d75d5f 100644
--- a/core/src/fxcrt/fx_basic_maps.cpp
+++ b/core/src/fxcrt/fx_basic_maps.cpp
@@ -33,8 +33,10 @@ void CFX_MapPtrToPtr::RemoveAll()
     }
     m_nCount = 0;
     m_pFreeList = NULL;
-    m_pBlocks->FreeDataChain();
-    m_pBlocks = NULL;
+    if (m_pBlocks) {
+        m_pBlocks->FreeDataChain();
+        m_pBlocks = NULL;
+    }
 }
 CFX_MapPtrToPtr::~CFX_MapPtrToPtr()
 {
@@ -206,8 +208,10 @@ void CFX_MapByteStringToPtr::RemoveAll()
     }
     m_nCount = 0;
     m_pFreeList = NULL;
-    m_pBlocks->FreeDataChain();
-    m_pBlocks = NULL;
+    if (m_pBlocks) {
+        m_pBlocks->FreeDataChain();
+        m_pBlocks = NULL;
+    }
 }
 CFX_MapByteStringToPtr::~CFX_MapByteStringToPtr()
 {
diff --git a/samples/pdfium_test.cc b/samples/pdfium_test.cc
index 4b6895b..7e226ca 100644
--- a/samples/pdfium_test.cc
+++ b/samples/pdfium_test.cc
@@ -634,8 +634,8 @@ int main(int argc, const char* argv[]) {
   FPDF_DestroyLibrary();
 #ifdef _V8_SUPPORT_
   v8::V8::ShutdownPlatform();
-#endif  // ~ _V8_SUPPORT_
   delete platform;
+#endif  // ~ _V8_SUPPORT_
 
   return 0;
 }
__EOF__

    [ $? -eq 0 ] || fail "Failed to patch"
  else
    git checkout win_gdal_build || fail "Could not get windows build branch"
  fi
  git rev-parse HEAD > pdfium-rev || fail
  cd ../ || fail

fi

if [ "$SYSTEM" = "android" -o "$SYSTEM" = "linux" ] ; then
    export PYTHONPATH=$PWD/pdfium_deps/gyp/build/`ls $PWD/pdfium_deps/gyp/build`
fi

cd pdfium || fail
${LOCAL_PYTHON} ./build/gyp_pdfium || fail "Could not generate GYP project files"


if [ "$SYSTEM" = "android" -o "$SYSTEM" = "linux" ] ; then
  platformlist="native"
  if [ "$SYSTEM" = "android" ] ; then
    platformlist="native arm64-v8a armeabi-v7a x86 x86_64"
  fi
  for i in $platformlist ; do
    TCPATH=${ANDROID_NDK}/toolchains/llvm/prebuilt/linux-x86_64/bin/
    OUTNAME=$i
    case $i in
      native)
        TCCC=gcc
        TCCXX=g++
        TCPATH=
        ;;
      arm64-v8a)
        TCCC="aarch64-linux-android${API_LEVEL}-clang"
        TCCXX="aarch64-linux-android${API_LEVEL}-clang++"
        OUTNAME=android-$i
        ;;
      armeabi-v7a)
        TCCC="armv7a-linux-androideabi${API_LEVEL}-clang"
        TCCXX="armv7a-linux-androideabi${API_LEVEL}-clang++"
        OUTNAME=android-$i
        ;;
      x86)
        TCCC="i686-linux-android${API_LEVEL}-clang"
        TCCXX="i686-linux-android${API_LEVEL}-clang++"
        OUTNAME=android-$i
        ;;
      x86_64)
        TCCC="x86_64-linux-android${API_LEVEL}-clang"
        TCCXX="x86_64-linux-android${API_LEVEL}-clang++"
        OUTNAME=android-$i
        ;;
    esac

    # Clean up any leftovers
    rm -rf out || fail

    make BUILDTYPE=$BUILDTYPE \
      CXX=${TCPATH}${TCCXX} CC=${TCPATH}${TCCC} \
      pdfium \
      fdrm \
      fpdfdoc \
      fpdfapi \
      fpdftext \
      fxcodec \
      fxcrt \
      fxge \
      fxedit \
      pdfwindow \
      formfiller \
      bigint \
      freetype \
      fx_agg \
      fx_lcms2 \
      fx_zlib \
      pdfium_base \
      fx_libjpeg \
      fx_libopenjpeg || fail "Building for $i failed"
    
    rm -rf "lib" || fail
    mkdir "lib" || fail
    cd out/$BUILDTYPE/obj.target || fail
    for lib in `find -name '*.a'`;
        do ar -t $lib | xargs ar rvs libpdfium.a.new || fail "Failed to flatten .a"
    done
    mv libpdfium.a.new ../../../lib/libpdfium.a || fail
    cd ../../../ || fail
    rm -rf out || fail
    cd .. || fail
    rm -f $OUTNAME.tar.gz
    tar cfz $OUTNAME.tar.gz pdfium || fail "Failed to tar up results for $i"
    cd pdfium || fail
  done
elif [ "$SYSTEM" = "linux-arm32" ] ; then
    TCCC=arm-linux-gnueabihf-gcc
    TCCXX=arm-linux-gnueabihf-g++
    TCPATH=
    OUTNAME="linux-arm32v7"

    # Clean up any leftovers
    rm -rf out || fail

    make BUILDTYPE=$BUILDTYPE \
      CXX=${TCPATH}${TCCXX} CC=${TCPATH}${TCCC} \
      pdfium \
      fdrm \
      fpdfdoc \
      fpdfapi \
      fpdftext \
      fxcodec \
      fxcrt \
      fxge \
      fxedit \
      pdfwindow \
      formfiller \
      bigint \
      freetype \
      fx_agg \
      fx_lcms2 \
      fx_zlib \
      pdfium_base \
      fx_libjpeg \
      fx_libopenjpeg || fail "Building for arm32 failed"
    
    rm -rf "lib" || fail
    mkdir "lib" || fail
    cd out/$BUILDTYPE/obj.target || fail
    for lib in `find -name '*.a'`;
        do ar -t $lib | xargs ar rvs libpdfium.a.new || fail "Failed to flatten .a"
    done
    mv libpdfium.a.new ../../../lib/libpdfium.a || fail
    cd ../../../ || fail
    rm -rf out || fail
    cd .. || fail
    rm -f $OUTNAME.tar.gz
    tar cfz $OUTNAME.tar.gz pdfium || fail "Failed to tar up results for arm32"
    cd pdfium || fail
elif [ "$SYSTEM" = "macos" ] ; then
  sed -e 's,x86_64 i386,x86_64,' pdfium.xcodeproj/project.pbxproj > pdfium.xcodeproj/project.pbxproj.tmp && mv pdfium.xcodeproj/project.pbxproj.tmp pdfium.xcodeproj/project.pbxproj || fail "error updating proj"
  sed -e 's,stdlib=libstdc++,stdlib=libc++,' pdfium.xcodeproj/project.pbxproj > pdfium.xcodeproj/project.pbxproj.tmp && mv pdfium.xcodeproj/project.pbxproj.tmp pdfium.xcodeproj/project.pbxproj || fail "error updating proj"

  xcodebuild -configuration Release_x64 \
  -target pdfium \
  -target fdrm \
  -target fpdfdoc \
  -target fpdfapi \
  -target fpdftext \
  -target fxcodec \
  -target fxcrt \
  -target fxge \
  -target fxedit \
  -target pdfwindow \
  -target formfiller || fail "xcodebuild failed"

  cd third_party

  sed -e 's,x86_64 i386,x86_64,' third_party.xcodeproj/project.pbxproj > third_party.xcodeproj/project.pbxproj.tmp && mv third_party.xcodeproj/project.pbxproj.tmp third_party.xcodeproj/project.pbxproj || fail "error updating proj"
  sed -e 's,stdlib=libstdc++,stdlib=libc++,' third_party.xcodeproj/project.pbxproj > third_party.xcodeproj/project.pbxproj.tmp && mv third_party.xcodeproj/project.pbxproj.tmp third_party.xcodeproj/project.pbxproj || fail "error updating proj"

  xcodebuild -configuration Release_x64 \
  -target bigint \
  -target freetype \
  -target fx_agg \
  -target fx_lcms2 \
  -target fx_zlib \
  -target pdfium_base \
  -target fx_libjpeg \
  -target fx_libopenjpeg || fail "xcodebuild failed"

  cd ..
  rm -rf "lib" || fail
  mkdir "lib" || fail
  cp xcodebuild/${BUILDTYPE}_x64/*.a lib/ || fail
  rm -rf xcodebuild/${BUILDTYPE}_x64

  cd .. || fail
  rm -f ${SYSTEM}-64.tar.gz
  tar cfz ${SYSTEM}-64.tar.gz pdfium || fail "Failed to tar up results"

else

  case "$SYSTEM" in
    win32)
      for i in *.vcxproj ; do
        sed -e 's,<Lib>,<Lib><TargetMachine>MachineX86</TargetMachine>,' $i > $i.tmp && mv $i.tmp $i
      done
      vss="${TTP}/mk/vs14_x86.sh"
      wplatform="Win32"
      ;;
    win64)
      vss="${TTP}/mk/vs14_x64.sh"
      wplatform="x64"
      ;;
    *)
      fail "Unsupported windows version - check SYSTEM variable (top of script)"
      ;;
  esac

  $vss msbuild build\\all.sln \
      /p:Configuration=${BUILDTYPE} /p:Platform=${wplatform} /m  \
    || fail "msbuild failed"

  rm -rf "lib" || fail
  mkdir "lib" || fail
  cp build/${BUILDTYPE}/lib/*.lib lib/ || fail
  rm -rf build/${BUILDTYPE}

  cd .. || fail
  rm -f ${SYSTEM}.tar.gz
  tar cfz ${SYSTEM}.tar.gz pdfium || fail "Failed to tar up results"

fi

