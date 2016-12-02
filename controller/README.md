------
CONTROLLER
------

# INTRODUCTION

This directory holds the main source code for the Kilda Controller.

# DEVELOPER ONBOARDING

The code is designed to compile and run in isolation. However, it integrates with
zookeeper, kafka, storm, and the other components in the services directory. To get
the full experience, ensure the following services are operational somewhere, and pass
the configuration in.

## BUILDING, IMAGING, RUNNING

### Building Options

1. Use project.sh (this will run the code inside a container):
	- ./project.sh build
	- ./project.sh image
	- ./project.sh run
2. Use Maven locallY (run outside of a container)
	-  `mvn -q package && mvn -q exec:java`
	-  NB: this is faster.

__NB: There is a dependency on base images (eg: kilda-controller/base/build-base.sh)__

### Logging
- SLF4J and Log4J2 are used. The configuration file is in src/main/resources/log4j2.properties

### Troubleshooting

#### Maven Build: Maven invalid LOC header (bad signature)
One of the dependendent libraries is corrupt. Delete from ~/.m2/repositories and run maven again.

## TESTING

At present we are using JUnit 4 (4.12 as of this writing). [JUnit 5](http://junit.org/junit5/) will release soon and we may migrate to that. 

# ARCHITECTURE

## BOOTUP

### PROPERTIES

Kilda uses the [Java Properties](https://docs.oracle.com/javase/tutorial/essential/environment/properties.html) patterns to load in default values, and the ability to
override these defaults.
