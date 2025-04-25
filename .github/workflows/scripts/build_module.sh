#!/bin/bash

MODULE_NAME=$1
COMMAND=$2
CURRENT_OS=$3
CURRENT_PYTHON=$4

echo "Building $MODULE_NAME..."
$COMMAND
BUILD_EXIT=$?
if [ $BUILD_EXIT -ne 0 ]; then
    printf 'BUILD_RESULT=%s;%s;%s;failure\n' "$CURRENT_OS" "$CURRENT_PYTHON" "$MODULE_NAME" >> "$GITHUB_OUTPUT"
else
    printf 'BUILD_RESULT=%s;%s;%s;success\n' "$CURRENT_OS" "$CURRENT_PYTHON" "$MODULE_NAME" >> "$GITHUB_OUTPUT"
fi
exit $BUILD_EXIT