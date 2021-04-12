#!/bin/bash
mvn clean compile exec:java -Dexec.mainClass="ServerSpread" -Dexec.args=$1