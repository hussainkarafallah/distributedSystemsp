
# Dockerfile
FROM ubuntu:latest

RUN \
# Update
apt-get update -y && \
# Install Java
apt-get install default-jre -y
#ADD ./out/artifacts/namenode_jar/distributedSystemsp.jar Excutable-Java.jar
#ADD ./namenode/namenode_hosts.conf ./namenode/namenode_hosts.conf

######
ADD ./out/artifacts/datanode_jar/distributedSystemsp.jar Excutable-Java.jar
RUN mkdir /datanode
WORKDIR "/datanode"

#ADD ./datanode1/datanode_hosts.conf datanode_hosts.conf
# ADD Dockerfile ./Dockerfile
#####
# ADD ./out/artifacts/clientnode_jar/distributedSystemsp.jar Excutable-Java.jar
#ADD ./client/client_hosts.conf client_hosts.conf

EXPOSE 8000
 CMD java -jar Excutable-Java.jar
