Builds a binary installation of the cTAKES application.
The installation is placed in a zip file in the ```target/``` directory. 
The scripts in the ```bin/``` directory are meant to be run from within a binary installation. 
They will not work in the source project.  
To use these scripts, build a binary installation by running
>  mvn package

and then unzip the ```apache-ctakes-*-bin.zip``` or ```apache-ctakes-*.tar.z``` file in the ```ctakes-distribution/target/``` 
directory into a new directory that is _not_ within the cTAKES source directories.