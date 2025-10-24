#!/bin/bash

if [ "${ANDROID_NDK_HOME}" == "" ] ; then
  echo '$ANDROID_NDK_HOME must be defined'
  exit 1
fi

if [ "${ANDROID_ABI}" == "" ] ; then
  echo '$ANDROID_ABI must be defined'
  exit 1
fi
case $ANDROID_ABI in
  "arm64-v8a")
    VCPKG_TRIPLET=arm64-android
    ;;
  "armeabi-v7a")
    VCPKG_TRIPLET=arm-neon-android
    ;;
  "x86")
    VCPKG_TRIPLET=x86-android
    ;;
  "x86_64")
    VCPKG_TRIPLET=x64-android
    ;;
  *)
    echo '$ANDROID_ABI must be one of "arm64-v8a", "armeabi-v7a", "x86", "x86_64"'
    exit 1
    ;;
esac

pushd overlay-ports
./stage.sh || exit 1
popd

mkdir -p build/android-${ANDROID_ABI}/ezvcpkg
export EZVCPKG_BASEDIR=`pwd`/build/android-${ANDROID_ABI}/ezvcpkg

if [ "$WARNINGS_AS_ERRORS" == "OFF" ] ; then
  sed -i -e 's/-Werror//g' cesium-native/cmake/macros/configure_cesium_library.cmake
fi

# CMake configure
cmake \
  -B build/android-${ANDROID_ABI} \
  -S cesium-native \
  -DCMAKE_MESSAGE_LOG_LEVEL=TRACE \
  -DANDROID_ABI=${ANDROID_ABI} \
  -DANDROID=1 \
  -DANDROID_NDK_HOME=${ANDROID_NDK_HOME} \
  -DCMAKE_POSITION_INDEPENDENT_CODE=ON \
  -DCMAKE_BUILD_TYPE=Release \
  -DVCPKG_CHAINLOAD_TOOLCHAIN_FILE=`pwd`/cmake/toolchains/android.cmake \
  -DCMAKE_INSTALL_PREFIX=build/android-${ANDROID_ABI} \
  -DVCPKG_OVERLAY_PORTS=`pwd`/overlay-ports \
  -DVCPKG_TRIPLET=${VCPKG_TRIPLET} \
  -DVCPKG_BUILD_TYPE=release \
  -DCESIUM_TESTS_ENABLED=OFF
# CMake build
cmake --build build/android-${ANDROID_ABI} --config Release --target install -j16
