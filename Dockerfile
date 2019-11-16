
# Dockerfile
FROM ubuntu:latest

RUN \
# Update
apt-get update -y && \
# Install Java
apt-get install default-jre -y
#ADD ./out/artifacts/namenode_jar/distributedSystemsp.jar Excutable-Java.jar
#ADD ./namenode/namenode_hosts.conf ./namenode/namenode_hosts.conf

##################
#RUN mkdir /datanode
#WORKDIR "/datanode"
#ADD ./out/artifacts/datanode_jar/distributedSystemsp.jar ../Excutable_Java.jar
##################

###################
RUN mkdir /client
WORKDIR "/client"
RUN pwd

ADD ./out/artifacts/clientnode_jar/distributedSystemsp.jar ../Excutable_Java.jar
RUN ls ..
###################
EXPOSE 8000
CMD java -jar ../Excutable_Java.jar # IF client node, datanode
# CMD java -jar Excutable_Java.jar # IF namenode

