#!/bin/sh
rm cpu.txt
touch cpu.txt
while :
do
    top -bn 2 -d 0.01 | grep '^%Cpu' | tail -n 1 | gawk '{print $2+$4+$6}' >> cpu.txt
    sleep 0.1
done
