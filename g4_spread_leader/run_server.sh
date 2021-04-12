#!/bin/bash
mvn clean compile exec:java -Dexec.mainClass="ServerAsync" -Dexec.args=$1