# Dockerfile
FROM ubuntu:latest

RUN \
# Update
apt-get update -y && \
# Install Java
apt-get install default-jre -y


##################
RUN mkdir /datanode
WORKDIR "/datanode"
ADD ./out/artifacts/datanode_jar/distributedSystemsp.jar ../Excutable_Java.jar
##################


EXPOSE 8000
CMD java -jar ../Excutable_Java.jar