#!/bin/bash

rm -rf se obj_* *.cooja *.sky *.native *.a *.map
mv *.log *.testlog result 2> /dev/null

rm -f cooja-tmp.csc
rm -f project-conf.h 
rm -f symbols.*
rm -f *.pyc
