#!/bin/bash

count=$(sudo lsof -i :$1 | wc -l) 
#pid=$(sudo lsof -i :$1 | awk '{print $2}')
pid=$(sudo lsof -i :$1 | awk '{print $2}' | awk 'NR==2')

if [ $count -eq 0 ]
then
echo "process not running"
else

echo $pid

if [ $count -gt 0 ]
then
echo "service is running!!!"
echo "with pid=$pid" 
echo "killing process"
echo "kill -9 $pid"
sudo kill -9 $pid
fi

count=$(sudo lsof -i :$1 | wc -l)
if [ $count -gt 0 ]
then
echo "process is still running"
else
echo "processed terminated"
fi

fi
