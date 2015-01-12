#!/bin/sh
mvn clean package -DskipTests
cp shrclient-omod/target/shrclient-1.0-SNAPSHOT.omod
