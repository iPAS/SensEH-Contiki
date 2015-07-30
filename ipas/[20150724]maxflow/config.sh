#!/bin/bash

CONTIKI_PATH='../..'
COOJA="${CONTIKI_PATH}/tools/cooja/dist/cooja.jar"
CONTIKI_REALPATH=$(cd ${CONTIKI_PATH}; pwd)
CLASSPATH="${CLASSPATH}:${CONTIKI_REALPATH}/tools/cooja/build"

function sim() {
	CSC_SIM=$1
	java -mx1024m -jar ${COOJA} -nogui="${CSC_SIM}" -contiki=${CONTIKI_PATH}
}
