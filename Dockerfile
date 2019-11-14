
# Dockerfile
FROM ubuntu:latest

RUN \
# Update
apt-get update -y && \
# Install Java
apt-get install default-jre -y

ADD ./out/artifacts/namenode_jar/distributedSystemsp.jar Namenode-Java.jar

EXPOSE 8000

CMD java -jar Namenode-Java.jar
