#!/bin/bash
#$1 peer_ID
#$2 server_ip
if [ "$#" -ne 2 ]; then
  echo "Usage: create_server.sh <peer_ID> <server_ip>"
  exit 1
fi

  gnome-terminal --tab --title="Peer $1" -- bash -c "cd bin && java peer.Peer  "$1" "$2""
echo "Peer $1 created in another tab"
