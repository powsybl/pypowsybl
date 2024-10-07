#!/bin/bash

check_snapshot_branch() {
    local repo=$1
    local core_version=$2

    SNAPSHOT_BRANCH=$(git ls-remote --heads "$repo" | grep -E "refs/heads/$(echo $core_version | grep -q SNAPSHOT && echo "$core_version" || echo "$core_version-SNAPSHOT")" | sed 's/.*refs\/heads\///')

    if [ -n "$SNAPSHOT_BRANCH" ]; then
        echo "SNAPSHOT VERSION EXIST: $SNAPSHOT_BRANCH"
        echo "SNAPSHOT_BRANCH=$SNAPSHOT_BRANCH" >> $GITHUB_ENV
    else
        echo "No SNAPSHOT branch found"
        echo "SNAPSHOT_BRANCH=main" >> $GITHUB_ENV
    fi
}