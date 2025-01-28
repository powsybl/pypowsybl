#!/bin/bash

MODULE_NAME=$1
COMMAND=$2
CURRENT_OS=$3
CURRENT_PYTHON=$4
RESULT_FILE="job_result_$CURRENT_OS-$CURRENT_PYTHON.txt"
echo "$RESULT_FILE"

echo "Building $MODULE_NAME..."
$COMMAND
BUILD_EXIT=$?
if [ $BUILD_EXIT -ne 0 ]; then
    printf '%s;%s;%s;failure\n' "$CURRENT_OS" "$CURRENT_PYTHON" "$MODULE_NAME" >> "$RESULT_FILE"
else
    printf '%s;%s;%s;success\n' "$CURRENT_OS" "$CURRENT_PYTHON" "$MODULE_NAME" >> "$RESULT_FILE"
fi
exit $BUILD_EXIT