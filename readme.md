# Distributed File System

### Description 

Academic project Fall 2019 (Distributed Systems Course)

## How to run the project on your local device or on instances which use public ip

### On local devices

#### Namenode

Please create a folder say "namenode" and inside this folder create a file namenode_hosts.conf. Configuration of this file are shown below
```$xslt
PORT
```
Here PORT is the number of port that the namenode will be using to listen to new datanodes/clients.

#### Datanode

Please create a folder say "datanode" and inside this folder create a file datanode_hosts.conf. Configuration is shown below

```$xslt
DataNodePORT NameNodeIP NameNodePORT DataNodeName

```
DataNodePORT: is the port which the datanode will listen to other nodes.
NameNodeIP: is the name node ip 
NameNodePORT:  is the port which the namenode will listen to other nodes.
DataNodeName: any string would do.

#### Client

Create a folder called client containing one file called client_hosts.conf having the following configuration

```$xslt
NameNodeIP NameNodePORT ClientName
```

NameNodeIP: is the name node ip 
NameNodePORT:  is the port which the namenode will listen to other nodes.
ClientName: any string would do.

<b>Please Note if you are using AWS instances or you are using public ips. You have to permit the ports to access to all TCP. Since we are using random ports 
for connecting the nodes better have all the range of the ports connect to all TCP.</b>


#### How to run
 On namenode run:
 ```
sudo docker run -v $(pwd):/namenode -it --network host jafarbadour/namenode-dfs:latest
```

you have also to  configure users.conf which is a json file of {user:password}
example
```
{"hussain":"123456","almir":"123456","jafar":"123456"}
```

On datanode run:
```
sudo docker run -v $(pwd):/datanode -it --network host jafarbadour/datanode-dfs:latest
```
18.220.0.246
On clientnode run:

```
sudo docker run -v $(pwd):/client -it --network host jafarbadour/client-dfs:latest
```

-v will let docker container mount the folder (namenode,datanode,client) in the host OS file system