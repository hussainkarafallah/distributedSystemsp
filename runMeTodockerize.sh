#!/bin/bash
sudo rm Dockerfile
cp clientDockerfile Dockerfile
echo "Client Done"

sudo docker build -t jafarbadour/client-dfs:latest .
sudo docker push jafarbadour/client-dfs:latest



rm Dockerfile
cp namenodeDockerfile Dockerfile
echo "namenode Done"
sudo docker build -t jafarbadour/namenode-dfs:latest .
sudo docker push jafarbadour/namenode-dfs:latest

rm Dockerfile
cp datanodeDockerfile Dockerfile
echo "datanode Done"
sudo docker build -t jafarbadour/datanode-dfs:latest .
sudo docker push jafarbadour/datanode-dfs:latest