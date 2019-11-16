# Dockerfile
FROM ubuntu:latest

RUN \
# Update
apt-get update -y && \
# Install Java
apt-get install default-jre -y


##################
#RUN mkdir /namenode
#WORKDIR "/namenode"
#RUN pwd
#
#ADD ./out/artifacts/namenode_jar/distributedSystemsp.jar ../Excutable_Java.jar
#RUN ls
#RUN ls ..
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
RUN ls ..# Dockerfile
         FROM ubuntu:latest

         RUN \
         # Update
         apt-get update -y && \
         # Install Java
         apt-get install default-jre -y


         ##################
         #RUN mkdir /namenode
         #WORKDIR "/namenode"
         #RUN pwd
         #
         #ADD ./out/artifacts/namenode_jar/distributedSystemsp.jar ../Excutable_Java.jar
         #RUN ls
         #RUN ls ..
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
CMD java -jar ../Excutable_Java.jar
###################
EXPOSE 8000
CMD java -jar ../Excutable_Java.jar