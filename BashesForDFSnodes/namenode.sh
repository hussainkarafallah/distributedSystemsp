#!/bin/bash
snap install docker
mkdir /home/ubuntu/namenode
echo "26200" >>/home/ubuntu/namenode/namenode_hosts.conf
echo "{\"hussain\":\"123456\",\"almir\":\"123456\",\"jafar\":\"123456\"}" >>/home/ubuntu/namenode/users.conf
cd /home/ubuntu
sudo docker run -v $(pwd):/namenode -it jafarbadour/namenode-dfs

