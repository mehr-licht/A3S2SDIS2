#!/bin/sh
#$1 serverID
#$2 server_port
if [ "$#" -ne 2 ]; then
  echo "Usage: create_server.sh <server_ID> <port>"
  exit 1
fi

if [[ "$2" -gt 3002  || "$2" -lt 3000 ]]; then
  echo "Server_port must be between 3000 and 3002, you provided $1"
  exit 1
fi

cd bin && java server.Server "$1" "$2"
