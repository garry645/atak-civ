# Support script for mapengine/CMakeLists.txt that sets sources, definitions, includes, link libraries, link directories,
# and compiler options that are specific to Windows targets.

set(takenginejni_WINDOWS_DEFS
    _CRT_SECURE_NO_WARNINGS
    _USE_MATH_DEFINES
    $<$<BOOL:${MSVC}>:MSVC>
    TINYGLTF_ENABLE_DRACO
    SPDLOG_COMPILED_LIB
    LIBASYNC_STATIC
)

set(takenginejni_WINDOWS_INCS
    ${khronos_INCLUDE_DIRS}
    ${JNI_INCLUDE_DIRS}
    ${SRC_DIR}/../../../build/java/generated/jni
    ${draco_INCLUDE_DIRS}
    ${cesium-native_INCLUDE_DIRS}
)

set(takenginejni_WINDOWS_LIBS
    # GLES
    GLESv2.lib

    # TTP Dependencies
    gdal_i.lib
    mdb.lib
    iconv.dll.lib
    libssl.lib
    libcurl.lib
    proj_i.lib
    geos_c_i.lib
    libxml2.lib
    spatialite.lib
    sqlite3_i.lib
    zlibwapi.lib
    kernel32.lib
    user32.lib
    gdi32.lib
    winspool.lib
    comdlg32.lib
    advapi32.lib
    shell32.lib
    ole32.lib
    oleaut32.lib
    uuid.lib
    odbc32.lib
    odbccp32.lib
    ${JNI_LIBRARIES}

    debug dracod.lib
    optimized draco.lib
)

set(takenginejni_WINDOWS_LDIRS
    ${ttp-dist_LIB_DIRS}
    ${GLES-stub_LIB_DIRS}
    ${draco_LIB_DIRS}
)

set(takenginejni_WINDOWS_OPTS
    # Set optimization level based on configuration.
    $<IF:$<CONFIG:Debug>,/Od,/O2>
)

set(takenginejni_WINDOWS_SRCS
)
