#!/bin/bash

rm -rf storage
rm -f *.log
rm -f *.replay
./execute.pl -s -n TwitterNodeWrapper -f 4 -c scripts/ProjTwoShortTest

