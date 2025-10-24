#!/bin/sh
# TODO `for each` directory
# TODO vcpkg ref

curl -s -L https://raw.githubusercontent.com/microsoft/vcpkg/refs/heads/master/ports/zlib-ng/vcpkg.json > zlib-ng/vcpkg.json
curl -s -L https://raw.githubusercontent.com/microsoft/vcpkg/refs/heads/master/ports/zlib-ng/portfile.cmake > zlib-ng/portfile.cmake
sed -i "/OPTIONS\b/a \ \ \ \ \ \ \ \ -DWITH_ARMV6=OFF" zlib-ng/portfile.cmake || exit 1
