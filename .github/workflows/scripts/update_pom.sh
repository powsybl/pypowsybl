#!/bin/bash

pattern="$1"
version="$2"
config_name="$3"

case "$config_name" in
    ubuntu|windows)
        sed -i "s|<$pattern>.*</$pattern>|<$pattern>$version</$pattern>|" pom.xml
        ;;
    darwin|darwin-arm64)
        sed -i '' "s|<$pattern>.*</$pattern>|<$pattern>$version</$pattern>|" pom.xml
        ;;
    *)
        echo "Unsupported configuration: $config_name"
        exit 1
        ;;
esac