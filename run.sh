#!/bin/bash

if [ $# -lt 2 ]; then
    echo $# arguments
    echo usage: "sh ./run.sh service <peer_ID> <opnd_1> <opnd_2>"
    echo "service is either BACKUP, RESTORE, RECLAIM, DELETE or STATE."
    exit 1
fi

version=1.0
service=$1
peer=$2

#STATE não pede nada
if [ $# -eq 2 ]; then
  if [ $service != "STATE" ]; then
	echo $# arguments
	echo "$service needs more arguments"
 	
echo .
echo "BACKUP example: sh ./run.sh BACKUP 2 "big.pdf" 1"
echo "DELETE example: sh ./run.sh DELETE 2 "big.pdf"
echo "RESTORE example: sh ./run.sh RESTORE 2 "big.pdf"
echo "RECLAIM example: sh ./run.sh RECLAIM 2 64"
echo "STATE example: sh ./run.sh STATE 2"
	exit 1
  fi
fi

#BACKUP pede file e replic
#DELETE pede file
#RESTORE pede file
#RECLAIM pede space
if [ $# -ge 3 ]; then
#path name of the file or amount of space to be RECLAIM
	if [ $service = "RECLAIM" ]; then
	space=$3
	fi
	if [ $service != "RECLAIM" ]; then
	file=$3
fi
fi;

if [ $# -eq 4 ]; then
  if [ $service != "BACKUP" ]; then
	echo $# arguments
	echo "too many arguments for $service"
echo "BACKUP example: sh ./run.sh BACKUP 2 "files/cenas.txt" 1"
  	echo "service is either BACKUP, RESTORE, RECLAIM, DELETE or STATE."
        exit 1
  fi
#specifies the desired replication degree. Applies only to BACKUP sub-protocol
replic=$4
fi




#BACKUP file replic
#DELETE file
#RECLAIM space
#RESTORE file
#STATE
#run the TestApp: "sh [BACKUP|RESTORE|RECLAIM|DELETE|STATE].sh"
if [ $service = "DELETE" ] || [ $service = "RESTORE" ]; then
 	java -classpath bin utils.Client peer$peer $service $file
fi
if [ $service = "RECLAIM" ]; then
	java -classpath bin utils.Client peer$peer RECLAIM $space
fi
if [ $service = "BACKUP" ]; then
	java -classpath bin utils.Client peer$peer BACKUP $file $replic
fi
if [ $service = "STATE" ]; then
	java -classpath bin utils.Client peer$peer STATE
fi


echo "file $service d"
