# Support script for mapengine/CMakeLists.txt that sets sources, definitions, includes, link libraries, link directories,
# and compiler options that are specific to Android targets.

set(takenginejni_ANDROID_DEFS
    RTTI_ENABLED
    -DTE_GLES_VERSION=3
    TINYGLTF_ENABLE_DRACO
    SPDLOG_COMPILED_LIB
    LIBASYNC_STATIC
)

set(takenginejni_ANDROID_INCS
    ${SRC_DIR}/../../../build/android/generated/jni
    ${draco_INCLUDE_DIRS}
    ${cesium-native_INCLUDE_DIRS}
)

set(takenginejni_ANDROID_LDIRS
    ${ttp-dist_LIB_DIRS}
    ${draco_LIB_DIRS}
)

set(takenginejni_ANDROID_LIBS
    # TTP
    spatialite
    gdal
    mdb

    draco

    # System
    log
    GLESv3
)

set(takenginejni_ANDROID_OPTS
    $<$<NOT:$<BOOL:${MSVC}>>:-O3>
)

set(takenginejni_ANDROID_SRCS
    ${SRC_DIR}/util/openssl_rehash.cpp
)
