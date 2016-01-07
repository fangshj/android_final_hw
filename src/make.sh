#!/bin/bash
sudo javac -cp ".:../libs/json.jar:../libs/mongo-java-driver-3.2.0.jar" Myserver.java ServerThread.java
sudo java -cp ".:../libs/json.jar:../libs/mongo-java-driver-3.2.0.jar" Myserver
