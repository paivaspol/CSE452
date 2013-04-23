#!/bin/bash

javac -cp ./jars/plume.jar:./jars/gson-2.2.3.jar lib/edu/washington/cs/cse490h/lib/*.java proj/*.java
# javac -cp ./jars/*.jar lib/edu/washington/cs/cse490h/lib/*.java proj/*.java
#javac -cp ./jars/plume.jar lib/edu/washington/cs/cse490h/lib/*.java 
#javac -cp ./jars/gson-2.2.3.jar proj/*.java
cd lib
jar cvf ../jars/lib.jar edu/washington/cs/cse490h/lib/*.class
#javac -cp ./jars/plume.jar ./jars/<gson>.jar lib/edu/washington/cs/cse490h/lib/*.java proj/*.java

exit
