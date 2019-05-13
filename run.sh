#!/usr/bin/env bash
#$1 server_id 1, 2 or 3
#$2 port 3000, 3001, 3002
echo "starting server $1 : $2"
cd bin && java server.Server $1 $2

