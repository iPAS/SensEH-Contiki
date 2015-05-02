CONTIKI = ../..

TARGET ?= sky

CFLAGS += -DPROJECT_CONF_H=\"project-conf.h\" 

#CONTIKIDIRS += ./senseh-files/

PROJECT_SOURCEFILES += battery.c energy-cons.c energy-harv.c lookup-table.c LUTs.c
	
all: senseh-tx
APPS+=powertrace
include $(CONTIKI)/Makefile.include
