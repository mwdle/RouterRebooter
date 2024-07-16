# RouterRebooter
A containerizable Selenide script to reboot an Arris SURFboard G54 WiFi router and TP-Link RE650 extender

## Context:
* My home network often experiences latency and other issues that are resolved after a router/extender restart.
* This script automates the process of restarting these devices.

## General Information:
* This project has 2 branches: master (RouterRebooter codebase) and ExtenderRebooter (ExtenderRebooter codebase).
* A Dockerfile is provided to allow containerization and secure execution of the script triggered by a separate (ssh enabled) container.
* The Selenide code can be easily adapted to match your make and model of router/extender.

## Purpose behind containerization:
* I use HomeAssistant to trigger the script via an SSH shell command.
  * To allow this, HomeAssistant must have SSH access to the device running the script.
  * Therefore, to avoid allowing the HomeAssistant container to access the host machine, the script can be containerized.

## How to use:
* First, use the 'maven package' command to build an executable jar file for the script. This will create a Rebooter.jar file in the 'target' folder.
  * Rename the jar file to the respective name: RouterRebooter.jar or ExtenderRebooter.jar and copy it into the project folder.
* Repeat the first step for both branches (master & ExtenderRebooter).
* If you do not wish to containerize the script, you may stop at this step and use the executable jars as you wish after exporting ROUTER_PASSWORD or EXTENDER_PASSWORD environment variables.
* To build a docker image for the scripts, run the following Docker build command from the RouterRebooter project folder:
  * Docker build -t mwdle/router_rebooter:latest --build-arg SSH_PUB_KEY="$(cat /PATH/TO/YOUR/DESIRED/id_rsa.pub)" --build-arg RR_JAR_PATH="RouterRebooter.jar" --build-arg ER_JAR_PATH="ExtenderRebooter.jar" --build-arg ROUTER_PASSWORD='<YOUR_ROUTER_PASSWORD_HERE>' --build-arg EXTENDER_PASSWORD='<YOUR_EXTENDER_PASSWORD_HERE>' .
* To start the container, run the following Docker run command after building the Docker image:
  * Docker run -d --name RouterRebooter --network=RouterRebooter --restart unless-stopped -e SCREEN_WIDTH=1920 -e SCREEN_HEIGHT=1080 -e SCREEN_DEPTH=24 -v /etc/localtime:/etc/localtime:ro -e TZ='America/Denver' mwdle/router_rebooter:latest