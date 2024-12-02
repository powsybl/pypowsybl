#!/bin/bash

MODULE_NAME=$1
COMMAND=$2

echo "Building $MODULE_NAME..."
$COMMAND
BUILD_EXIT=$?

if [ $BUILD_EXIT -ne 0 ]; then
    echo "❌ $MODULE_NAME build FAILED" >> $BUILD_STATUS
else
    echo "✅ $MODULE_NAME build SUCCESS" >> $BUILD_STATUS
fi

exit $BUILD_EXIT