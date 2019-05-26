#!/bin/bash
rm -r bin
mkdir -p bin
mkdir -p fileSystem

javac $(find src | grep .java) -d bin
