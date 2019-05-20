#!/bin/bash

 gnome-terminal --tab --title="rmi service" -- bash -c "cd bin && rmiregistry"
#cd bin && java utils.RMI
echo "rmi service running on another tab"
