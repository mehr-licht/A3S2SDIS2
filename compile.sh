#!/bin/bash
rm -rf bin
rm -rf fileSystem
mkdir -p bin
mkdir -p fileSystem

javac $(find src | grep .java) -d bin
