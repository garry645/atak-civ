#!/bin/bash

ATAK=../atak/ATAK/app

function fail()
{
    echo "Failed build while $1" >&2
    exit 1
}


echo "Building libunwindstack sources"
mkdir -p libunwind
for i in arm64-v8a armeabi-v7a x86 x86_64 ; do
    mkdir -p libunwind/$i || fail "creating directory for libunwind"
    cd libunwind/$i || fail "swapping to directory for libunwind"
    neonopt=""
    if [ $i = armeabi-v7a ] ; then
        neonopt="-DCMAKE_ANDROID_ARM_NEON=ON"
    fi

    cmake \
        -DCMAKE_SYSTEM_NAME=Android \
        -DCMAKE_ANDROID_ARCH_ABI=$i \
        -DCMAKE_SYSTEM_VERSION=21 \
        -DANDROID_STL=c++_shared \
        $neonopt \
        -DANDROID_NDK=${ANDROID_NDK} \
        -DCMAKE_POSITION_INDEPENDENT_CODE=True \
        ../../../libunwindstack-ndk/cmake || fail "cmake configuration failed"
    make || fail "Failed to build libunwindstack for $i"
    cd ../../
    
done



echo "Building Java sources...."

${JAVA_HOME}/bin/javac -h jni/jniGen com/atakmap/jnicrash/*.java || fail "compiling java sources"
${JAVA_HOME}/bin/jar cf jnicrash.jar com/atakmap/jnicrash/*.class || fail "creating JAR file"

rm -f jni/jjnicrash.h
mv jni/jniGen/com_atakmap_jnicrash_JNICrash.h jni/jjnicrash.h || fail

${ANDROID_NDK}/ndk-build || fail "building native sources"

echo "Build success - copying files to ATAK"
cp jnicrash.jar ${ATAK}/libs/ || fail "copying jar file to ATAK"
for i in arm64-v8a armeabi-v7a x86 x86_64 ; do
    cp libs/${i}/libjnicrash.so ${ATAK}/src/main/jniLibs/${i}/ || fail "copying native lib for $i"
done

echo "Build and install complete!"

