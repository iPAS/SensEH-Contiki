#!/bin/bash 
shopt -s extglob # for pattern of 'case'

. sim_config

#################
## Information ##

DATE=$(date +'%Y%m%d')
TIME=$(date +'%H%M')

REQUIRED_PARAMS=1

echo '########## Simulation Script for Contiki & COOJA ##########'
echo 'Author: iPAS'
echo 'Revised: 2015 05 23'

EXAMPLE='./sim.sh test.csc'

if [ $# -lt ${REQUIRED_PARAMS} ]; then
	echo "Usage:"
	echo " > $(basename $0) <COOJA.csc> ..."
	echo "   [--count=1..N times of validation]"
	echo "   [--rdc=contikimac_driver|xmac_driver|cxmac_driver|lpp_driver|nullrdc_driver]"
	echo "   [--rdc_rate=1,2,4,8, or n in power of 2]" 
	echo "   [--mac=csma_driver|nullmac_driver]"
	echo "   [--ls=1|0] LS-TPC or not"
	echo "   [--result_dir=<directory>"
	echo "   [--mkconf to make configuration files only without simulation]"
	echo
	echo "Example:"
	echo
	echo ' > '${EXAMPLE}
	echo
	echo 'or, in case of graphical simulation, so that open COOJA then.'
	echo
	echo ' > '${EXAMPLE}' --mkconf'
	echo 
	echo 'or, with full options'
	echo
	echo -n ' > '${EXAMPLE}
	echo ' --rdc=contikimac_driver --rdc_rate=8 --mac=csma_driver --result_dir=result'
	echo 
	echo "Note:"
	echo " 1) Three aforementioned files are path-relative to current dir."
	echo " 2) The options are rearrangeable."
	exit 255
fi

#[[ ! -f "$1" ]] && echo " -> '$1' COOJA project do not exist!" && exit 255
CSC=$1
shift ${REQUIRED_PARAMS}

###########
## Paths ##

if [ ! -f "${CONTIKI_PATH}/Makefile.include" ]; then
    echo " -> '${CONTIKI_PATH}' contiki path is not true."
    echo "Please check 'sim_config' and 'Makefile'."
    echo "Or if you have not downloat it yet,"
    echo " getting it by:"
    echo " > git clone https://github.com/contiki-os/contiki.git"
    exit 255
fi

COOJA="${CONTIKI_PATH}/tools/cooja/dist/cooja.jar"
CONTIKI_REALPATH=$(cd ${CONTIKI_PATH}; pwd)
CLASSPATH="${CLASSPATH}:${CONTIKI_REALPATH}/tools/cooja/java"

if [ ! -f "${COOJA}" ]; then  
    echo " -> '${COOJA}' do not exist!"
    echo "Please 'git submodule update --init' to get MSPSim,"
	echo "go to '${CONTIKI_PATH}/tools/cooja' directory,"
    echo "and then run 'ant run' once."
    exit 255
fi


function dir_to {
	echo "$( cd "$( dirname "$1" )" && pwd )"
}

function esc_it {
	echo "$1" | sed -e 's/\\/\\\\/g' -e 's/\//\\\//g' -e 's/&/\\\&/g'
}

CSC_DIR=$(dir_to "${CSC}")
CSC_NAME=$(basename "${CSC}" .csc)

################################
## Extract options parameters ##

RDC=contikimac_driver
RDC_RATE=8
MAC=csma_driver
COUNT=1
RESULT_DIR=result
LS_TPC=1

while [ $# -gt 0 ]; do
	
	case $1 in
	--count=+([[:digit:]]))
		COUNT=$(echo $1 | sed 's/--count=//')
		;;

	--rdc=@(contikimac_driver|xmac_driver|cxmac_driver|lpp_driver|nullrdc_driver)) 
		RDC=$(echo $1 | sed 's/--rdc=//')
		;;
	
	--rdc_rate=@(1|2|4|6|8|16|32|64|128|256|512|1024))
		RDC_RATE=$(echo $1 | sed 's/--rdc_rate=//')
		;;

	--ls=@(1|0))
		LS_TPC=$(echo $1 | sed 's/--ls=//')
		;;

	--mac=@(csma_driver|nullmac_driver)) 
		MAC=$(echo $1 | sed 's/--mac=//')
		;;

	--mkconf)
		NOSIM='nosim'
		;;

	--result_dir=+([[:ascii:]]))
		RESULT_DIR=$(echo $1 | sed 's/--result_dir=//')
		[[ ! -d ${RESULT_DIR} ]] && echo 'Result (output) directory not found!' && exit 255 
		;;
	*)
		echo "Parameter '$1' error!"
		exit 255
		;;
	esac

	shift 1
done

###############################
## Create configuration file ##

CSC_SIM=${CSC}

#H_TEMPFILE=$(tempfile -p ${TIME}_ -s _${FW_NAME}.h) || ( echo 'h tempfile creation error!' && exit 255 )
#CONF_SIM=${H_TEMPFILE}
CONF_SIM=project-conf.h
cat project-conf.h.template | \
sed "s/%SIM_RDC%/${RDC}/g" | \
sed "s/%SIM_RDC_RATE%/${RDC_RATE}/g" | \
sed "s/%LS_TPC%/${LS_TPC}/g" | \
sed "s/%SIM_MAC%/${MAC}/g" > "${CONF_SIM}"

if [ ${NOSIM} ]; then
	exit
else
	trap "rm -f \"${CONF_SIM}\"" EXIT
fi

#####################
## Simulation loop ##

MODE=-nogui
LOOP=1
NAME="csc=${CSC_NAME},rdc=${RDC},rdcr=${RDC_RATE},mac=${MAC},ls_tpc=${LS_TPC},date=${DATE},time=${TIME}"
LOG="${RESULT_DIR}/${NAME}.simlog"
echo "#############################################################" | tee -a ${LOG}
echo "Simulation: ${NAME}" | tee -a ${LOG}
echo | tee -a ${LOG}

while [ $COUNT -gt 0 ]; do 
	COUNT=$((COUNT - 1))
	echo -e " -> $(echo ${NAME} | sed 's/,/\\n    /g')" | tee -a 
	echo " -> Run: #${LOOP}, remain ${COUNT}" | tee -a ${LOG}
	echo | tee -a ${LOG}

	function sim() 
	{
		java -mx512m -jar ${COOJA} ${MODE}="${CSC_SIM}" -contiki=${CONTIKI_PATH} | tee -a ${LOG}
	}

	time sim | tee -a ${LOG} 

	if [[ -f COOJA.testlog ]]; then 
		TESTLOG=${RESULT_DIR}/${NAME}_${LOOP}.testlog
		echo -e "$(echo ${NAME} | sed 's/=/: /g' | sed 's/\([a-z]\)\([a-z]*\): /\u\1\2: /g' | sed 's/,/\\n/g')" > ${TESTLOG}
		cat COOJA.testlog >> ${TESTLOG}
		rm -f COOJA.testlog 
	fi
	
	((LOOP++))

	echo '#############' | tee -a ${LOG}
	echo | tee -a ${LOG}
done 

rm -f COOJA.log

