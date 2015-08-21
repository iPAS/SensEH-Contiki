#!/bin/bash
# Ranges Rates Combination Simulation

CSC_COUNT=5
MODELS='1 0'
COUNT=5

function sim {
	RESULT_DIR=$1
	CSC=$2
	rm -rf ${RESULT_DIR}
	mkdir ${RESULT_DIR}

	for NO in `seq 1 ${CSC_COUNT}`; do
		for MODEL in ${MODELS}; do
			CSC_FILE=${CSC}.${NO}.csc
			echo "Sim: ${CSC_FILE} on Model ${MODEL}"
			nice -19 ionice -c 3 ./sim.sh ${CSC_FILE} --count=${COUNT} --result_dir=${RESULT_DIR} --ls=${MODEL}
		done
	done
}

