#!/bin/bash

confirm(){
#wait for yes to continue or exit to exit...
local msg=" "
echo $msg > /dev/tty
read -p "Continue (y)es? (ctrl+c to exit)" choice
echo $msg > /dev/tty

while [ $choice != "y" ] 
do
case "$choose" in
  [eE]) exit 0;;
esac
if [ $choice = "e" ] || [ $choice = "E" ]; then
exit 0
fi
#echo $msg1 > /dev/tty;
 #  case "$choice" in 
 # "y"|"Y" ) break;echo $msg2 > /dev/tty;;
 # "e"|"E" ) echo $msg3 > /dev/tty;exit 1;;
#esac
done
}


ask_state(){
#ask for the state of the 3 peers
local msg=" "
echo $msg > /dev/tty
echo $msg > /dev/tty
local msg1="asking each peer for their state"
local msg2="check their state on each tab"
echo $msg1 > /dev/tty
echo $msg > /dev/tty
./run.sh STATE 1
./run.sh STATE 2
./run.sh STATE 3

echo $msg2 > /dev/tty 
echo $msg > /dev/tty
}


state_confirm(){
$(ask_state)
$(confirm)
}




##########################  START  ###############################

##################################### kill rmi service if running
echo " "
echo " "

echo "sudo sh killRMI.sh"
sh killRMI.sh

echo " "
echo " "
$(confirm)
##################################### kill server ports if open
echo " "
echo " "

echo "sudo sh killPort.sh 3000"
sudo sh killPort.sh 3000

echo " "
echo " "
$(confirm)

echo "sudo sh killPort.sh 3001"
sudo sh killPort.sh 3001

echo " "
echo " "
$(confirm)

echo "sudo sh killPort.sh 3002"
sudo sh killPort.sh 3002

echo " "
echo " "
$(confirm)


#################################### compile program
echo " "
echo " "

echo "./compile.sh"
./compile.sh

echo " "
echo " "
$(confirm)




#################################### start rmi service
echo " "
echo " "

echo "./rmi.sh"
./rmi.sh

echo " "
echo " "
$(confirm)


######################################create 2 servers
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% create 2 servers %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "

echo " ./create_server.sh 1 3000"
 ./create_server.sh 1 3000

echo " "
echo " "
$(confirm)


echo " "
echo " "

echo " ./create_server.sh 2 3001"
 ./create_server.sh 2 3001

echo " "
echo " "
$(confirm)


######################################create 3 peers
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% create 3 peers %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "

echo " ./create_peer.sh 1 localhost"
 ./create_peer.sh 1 localhost

echo " "
echo " "
$(confirm)

echo " "
echo " "

echo " ./create_peer.sh 2 localhost"
 ./create_peer.sh 2 localhost

echo " "
echo " "
$(confirm)


echo " "
echo " "

echo " ./create_peer.sh 3 localhost"
 ./create_peer.sh 3 localhost

echo " "
echo " "
$(state_confirm)


################################### copy run.sh to directory
echo "cp run.sh bin/Peers/Peer1/PeerDisk/MyFiles"
cp run.sh bin/Peers/DiskPeer1/MyFiles
echo "cp Untitled.png bin/Peers/PeerDisk1/MyFiles"
cp Untitled.png bin/Peers/DiskPeer1/MyFiles



echo " "
echo " "
$(confirm)





###################################### BACKUP
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% BACKUP %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "

echo "./run.sh BACKUP 1 "run.sh" 2"
./run.sh BACKUP 1 "run.sh" 2

 echo " "
echo " "
$(state_confirm)


###################################### RESTORE
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% RESTORE %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "


 ./run.sh RESTORE 1 "run.sh"


echo " "
echo " "
$(state_confirm)

echo " "
echo " %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"
echo " %%%%%%%%%%%              %%%%%%%%%%%"
echo " %%%%%%%%%%% all tests ok %%%%%%%%%%%"
echo " %%%%%%%%%%%              %%%%%%%%%%%"
echo " %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"

exit 0
