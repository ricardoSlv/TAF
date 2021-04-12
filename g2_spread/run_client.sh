#!/bin/bash
mvn clean compile exec:java -Dexec.mainClass="TesterSpread" -Dexec.args="$1 $2 $3" 