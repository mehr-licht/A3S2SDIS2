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
local msg3="sh run.sh STATE 1"
local msg4="sh run.sh STATE 2"
local msg5="sh run.sh STATE 3"
echo $msg1 > /dev/tty
echo $msg > /dev/tty
echo $msg > /dev/tty
echo $msg3 > /dev/tty
gnome-terminal --tab --title="STATE 1" -- bash -c "sh run.sh STATE 1"
$(confirm)
echo $msg4 > /dev/tty
gnome-terminal --tab --title="STATE 2" -- bash -c "sh run.sh STATE 2"
$(confirm)
echo $msg5 > /dev/tty
gnome-terminal --tab --title="STATE 3" -- bash -c "sh run.sh STATE 3"
echo $msg > /dev/tty
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

echo "sudo sh killPort.sh 2000"
sudo sh killPort.sh 2000

echo " "
echo " "
$(confirm)

echo "sudo sh killPort.sh 2001"
sudo sh killPort.sh 2001

echo " "
echo " "
$(confirm)

echo "sudo sh killPort.sh 2002"
sudo sh killPort.sh 2002

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

echo " ./create_server.sh 1 2000"
 ./create_server.sh 1 2000

echo " "
echo " "
$(confirm)


echo " "
echo " "

echo " ./create_server.sh 2 2001"
 ./create_server.sh 2 2001

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
echo "please confirm that there are replicated AND encrypted chunks on the backup folder of peers 2 and 3"
$(confirm)
echo " "
echo " "

$(state_confirm)


###################################### RESTORE
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% RESTORE %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "


echo "./run.sh RESTORE 1 'run.sh'"
 ./run.sh RESTORE 1 "run.sh"
echo "please confirm that there is an unencrypted file on the Restore folder of peer 1"
$(confirm)

echo " "
echo " "
$(state_confirm)

echo " "


###################################### DELETE
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% DELETE %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "

echo "./run.sh DELETE 1 'run.sh'"
 ./run.sh DELETE 1 "run.sh"
 echo " "
echo "please confirm that replicated chunks of run.sh were deleted on peers 2 and 3"
echo " "
$(confirm)
echo " "
echo "./run.sh DELETE 1 'Untitled.png'"
 ./run.sh DELETE 1 "Untitled.png"
echo "an error 'Erro ao apagar ficheiro: BackupUtil alheio' must appear on peer 1 tab"

echo " "
$(confirm)
echo " "
echo " "
$(state_confirm)

echo " "
###################################### RECLAIM
echo " "
echo " "
echo "%%%%%%%%%%%%%%%%%%%%%% RECLAIM %%%%%%%%%%%%%%%%%%%%%%"
echo " "
echo " "
 ./run.sh BACKUP 1 "Untitled.png" 2
echo "please confirm that peer 2 has chunks of Untitled.png"

$(confirm)
echo " "
echo " "

echo "./run.sh RECLAIM 2 5"
 ./run.sh RECLAIM 2 5
 echo " "
echo "please confirm that replicated chunks were deleted on peer 2 after reclaim"
echo " "
gnome-terminal --tab --title="STATE 1" -- bash -c "./run.sh STATE 1"
$(confirm)
echo " "

echo " %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"
echo " %%%%%%%%%%%              %%%%%%%%%%%"
echo " %%%%%%%%%%% all tests ok %%%%%%%%%%%"
echo " %%%%%%%%%%%              %%%%%%%%%%%"
echo " %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"

exit 0
