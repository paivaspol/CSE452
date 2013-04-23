#!/usr/bin/perl

# Simple script to start a Node Manager that uses a compiled lib.jar

main();

sub main {
    
    $classpath = "proj/:jars/plume.jar:jars/lib.jar:jars/gson-2.2.3.jar";
    
    $args = join " ", @ARGV;

    exec("java -cp $classpath edu.washington.cs.cse490h.lib.MessageLayer $args");
}

