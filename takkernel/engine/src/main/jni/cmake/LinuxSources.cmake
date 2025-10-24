# Support script for mapengine/CMakeLists.txt that sets sources, definitions, includes, link libraries, link directories,
# and compiler options that are specific to Android targets.

set(takenginejni_LINUX_DEFS
    RTTI_ENABLED
    -DTE_GLES_VERSION=3
    SQLITE_HAS_CODEC
    TINYGLTF_ENABLE_DRACO
)

set(takenginejni_LINUX_INCS
    ${khronos_INCLUDE_DIRS}
    ${JNI_INCLUDE_DIRS}
    ${SRC_DIR}/../../../build/java/generated/jni
    ${draco_INCLUDE_DIRS}
    ${cesium-native_INCLUDE_DIRS}
)

set(takenginejni_LINUX_LDIRS
    ${ttp-dist_LIB_DIRS}
    ${GLES-stub_LIB_DIRS}
    ${draco_LIB_DIRS}
)

set(takenginejni_LINUX_LIBS
    # GLES
    GLESv2

    # TTP
    spatialite
    gdal

    draco
)

set(takenginejni_LINUX_OPTS
    $<$<NOT:$<BOOL:${MSVC}>>:-O3>
)

set(takenginejni_LINUX_SRCS
)
