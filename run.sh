#!/bin/sh
# This file should build and run your code.
# It will run if you're in nodocker mode on Mac or Linux,
# or if you're running in docker.


# old version

#!/bin/sh
# build the java files.
# there will eventually be a separate build step, but for now the build counts against your time.
javac *.java -classpath /battlecode-java:.
java -Xmx80m -classpath /battlecode-java:. Player
