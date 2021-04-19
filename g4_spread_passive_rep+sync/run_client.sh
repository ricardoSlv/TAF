#!/bin/bash
mvn clean compile exec:java -Dexec.mainClass="TesterAsync" -Dexec.args="$1" 