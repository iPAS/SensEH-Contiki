#!/bin/bash

rm -f *.{log,testlog,simlog}

[[ "$1" == "-a" ]] && rm -f result/*.{log,testlog,simlog}
