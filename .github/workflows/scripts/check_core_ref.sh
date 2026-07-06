#!/bin/bash

# Find if a "ci_ref" tag exists
REF_SHA=$(git ls-remote --tags "https://github.com/powsybl/powsybl-core.git" | grep -E "refs/tags/ci_ref\^\{\}" | awk '{print $1}')
if [ -n "$REF_SHA" ]; then
    echo "'ci_ref' tag found: using it (SHA=$REF_SHA)"
    echo "CORE_REF=ci_ref" >> "$GITHUB_ENV"
else
    echo "'ci_ref' tag not found: using 'main' branch"
    echo "CORE_REF=main" >> "$GITHUB_ENV"
fi
