# Distributed File System

### Description 

Academic project Fall 2019 (Distributed Systems Course)

## How to run the project on your local device or on instances in closed network with private IPs

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
DataNodeIP DataNodePORT NameNodeIP NameNodePORT DataNodeName

```
DataNodeIP: is the IP of the machine
DataNodePORT: is the port which the datanode will listen to other nodes.
NameNodeIP: is the name node ip 
NameNodePORT:  is the port which the namenode will listen to other nodes.
DataNodeName: any string would do.

#### Client

Create a folder called client containing one file called client_hosts.conf having the following configuration

```$xslt
ClientIP NameNodeIP NameNodePORT ClientName
```
ClientIP: is the IP of your current machine
NameNodeIP: is the name node ip 
NameNodePORT:  is the port which the namenode will listen to other nodes.
ClientName: any string would do.

<b>Please Note if you are using AWS instances or you are using public ips. You have to permit the ports to access to all TCP. Since we are using random ports 
for connecting the nodes better have all the range of the ports connect to all TCP.</b>


## How to run

On namenode run:
 ```
sudo docker run -v $(pwd):/namenode -it --network host jafarbadour/namenode-dfs:latest
```

you have also to  configure users.conf which is a json file of {user:password}
example
```
{"hussain":"123456","almir":"123456","jafar":"123456"}
```
Make sure there is only one line in the JSON file.

On datanode run:
```
sudo docker run -v $(pwd):/datanode -it --network host -d jafarbadour/datanode-dfs:latest
```
On clientnode run:

```
sudo docker run -v $(pwd):/client -it --network host jafarbadour/client-dfs:latest
```

-v will let docker container mount the folder (namenode,datanode,client) in the host OS file system

## AWS initialization

When initialization of namenode
in 3rd step in instance configuration -> advanced details -> User data
write the following script

```
#!/bin/bash
snap install docker
mkdir /home/ubuntu/namenode
echo "26200" >>/home/ubuntu/namenode/namenode_hosts.conf
echo "{\"hussain\":\"123456\",\"almir\":\"123456\",\"jafar\":\"123456\"}" >>/home/ubuntu/namenode/users.conf

```

Initialization of datanodes 
```
#!/bin/bash
snap install docker
mkdir /home/ubuntu/datanode
echo "25011 18.217.199.246 26200 datanode" >>/home/ubuntu/datanode/datanode_hosts.conf
cd /home/ubuntu/datanode
sudo docker pull jafarbadour/datanode-dfs:latest
sudo docker run -v $(pwd):/datanode -it -d --network host jafarbadour/datanode-dfs:latest

```
where in the last line we have the ip:port of the namenode as mentioned above in the datanode_hosts.conf

Initialization of clientnode
```
#!/bin/bash
snap install docker
mkdir /home/ubuntu/client
echo "18.217.199.246 26200 client" >>/home/ubuntu/client/client_hosts.conf

```

## Commands in our DFS
```
login 
login hussain 123456

format
Just write format

create
create /a/b/c/f.txt

upload pathInClient pathOnServer
upload ./clientfiles/toupload.pdf  ./downloads/book.pdf

download pathOnClient pathOnServer
download ./downloads/book.pdf ./serverbooks/book.pdf

delete pathToFileOnServer
delete /a/b/c/xx.yy

info pathToFileOnServer
info /a/b/c/xx.yy

cp firstFilePathOnServer nameOfFileInNewPath
cp /a/b/c/book.pdf  /d/e/f/book-copy.pdf

mv firstFilePathOnServer nameOfFileInNewPath
mv /a/b/c/book.pdf /d/e/f/book-moved.pdf

cd changeDirectory
cd /newdir/newsubdir1/

ls listAllFiles/Directories
ls
ls /a/
ls .
ls ..

mkdir createNewDirectory
mkdir newdir
mkdir newdir/newdir2/

rmdir pathToDir -r // Force recursive deletion
rmdir /a/b/c/ (must be empty dir)
rmdir /a/b/c/ -r (doesn't matter)


```


# Building the docker images

when one wants to build the docker images and push to docker hub please follow this commands

```
chmod +x runMeTodockerize.sh
./runMeTodockerize.sh
```


# Diagrams

## Main Diagram

<img src = "https://scontent-arn2-2.xx.fbcdn.net/v/t1.15752-9/77250869_746624955851703_2416014447979003904_n.png?_nc_cat=100&_nc_oc=AQlDh3uVSM5cPmwIp2CNawya6PN3kw9almegf3J3ZbBFigMoK9TD6LXlWle7Ff8Bbd8&_nc_ht=scontent-arn2-2.xx&oh=849d33364a324abe4846a8d336a15010&oe=5E521B16">

## NameNode dependent commands (ls,info..etc)
![](https://i.imgur.com/mMAe1cf.png)
![](https://i.imgur.com/xwX3SAL.png)
![](https://i.imgur.com/pt43gUE.png)
![](https://i.imgur.com/SGStEjO.png)

## Download operations
![](https://i.imgur.com/DRln1x0.png)
![](https://i.imgur.com/FKdGwbp.png)
![](https://i.imgur.com/ewmx759.png)
![](https://i.imgur.com/435CNfn.png)


## Upload operations
![](https://i.imgur.com/7ln4nlx.png)
![](https://i.imgur.com/hEwMRzk.png)
![](https://i.imgur.com/A6eXA9z.png)


## Replication
![](https://i.imgur.com/8noIPzU.png)
![](https://i.imgur.com/i3iynbr.png)
![](https://i.imgur.com/jDkvhYQ.png)
![](https://i.imgur.com/B2cSh0R.png)




