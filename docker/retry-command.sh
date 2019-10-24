#!/bin/bash
n=0
until [ $n -ge 5 ]
do
   $@ && break  # substitute your command here
   n=$[$n+1]
   echo "Retrying ($n/5): sleeping for 15 ..."
   sleep 15
done
