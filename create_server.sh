#!/bin/bash
#$1 serverID
#$2 server_port
if [ "$#" -ne 2 ]; then
  echo "Usage: create_server.sh <server_ID> <port>"
  exit 1
fi

if [ "$2" -gt 2002 ] || [ "$2" -lt 2000 ]; then
  echo "Server_port must be between 2000 and 2002, you provided $1"
  exit 1
fi

gnome-terminal --tab --title="Server $1" -- bash -c "cd bin && java server.Server  "$1" "$2""
echo "Server $1 created in another tab"

