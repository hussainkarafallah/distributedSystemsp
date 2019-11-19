#!/bin/bash
snap install docker
mkdir /home/ubuntu/namenode
echo "26200" >>/home/ubuntu/namenode/namenode_hosts.conf
echo "{\"hussain\":\"123456\",\"almir\":\"123456\",\"jafar\":\"123456\"}" >>/home/ubuntu/namenode/users.conf
cd /home/ubuntu/
echo $(pwd)
sudo docker run -v $(pwd):/namenode -it --network host -d jafarbadour/namenode-dfs:latest

echo "GREAT SUCCESS NAMENODE IS RUNNING"
