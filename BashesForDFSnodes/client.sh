#!/bin/bash
snap install docker
mkdir /home/ubuntu/client
echo "10.0.1.72  26200 client" >>/home/ubuntu/client/client_hosts.conf