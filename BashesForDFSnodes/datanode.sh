#!/bin/bash
snap install docker
mkdir /home/ubuntu/datanode
echo "25011 18.217.199.246 26200 datanode" >>/home/ubuntu/datanode/datanode_hosts.conf
cd /home/ubuntu/datanode
sudo docker pull jafarbadour/datanode-dfs:latest
sudo docker run -v $(pwd):/datanode -it -d --network host jafarbadour/datanode-dfs:latest
