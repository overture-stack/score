#!/bin/bash

BASH_SCRIPT=`readlink -f ${BASH_SOURCE[0]}`
BASH_SCRIPT_DIR=$( dirname  "${BASH_SCRIPT}")

make -f ${BASH_SCRIPT_DIR}/../Makefile $@
