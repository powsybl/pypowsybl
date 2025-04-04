#!/bin/bash

repo=$1
core_version=$2

# Add "-SNAPSHOT" to powsybl-core version if not already there
core_snapshot_version=$(echo "$core_version" | grep -q SNAPSHOT && echo "$core_version" || echo "$core_version-SNAPSHOT")

# Find if an CI snapshot branch exists
SNAPSHOT_BRANCH=$(git ls-remote --heads "$repo" | grep -E "refs/heads/ci/core-$core_snapshot_version" | sed 's/.*refs\/heads\///')
if [ -n "$SNAPSHOT_BRANCH" ]; then
    echo "SNAPSHOT VERSION EXIST: $SNAPSHOT_BRANCH"
    echo "SNAPSHOT_BRANCH=$SNAPSHOT_BRANCH" >> "$GITHUB_ENV"
else
    echo "No SNAPSHOT branch found"
    echo "SNAPSHOT_BRANCH=main" >> "$GITHUB_ENV"
fi