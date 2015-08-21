#!/bin/bash

source sim_cases.sh
COUNT=5
sim result_100x100 ls-tpc.10nodes_100x100
sim result_200x200 ls-tpc.10nodes_200x200
sim result_300x300 ls-tpc.10nodes_300x300

