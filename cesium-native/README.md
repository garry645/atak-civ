# Environment Variables

* ANDROID_NDK_HOME
* ANDROID_ABI

```
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
```