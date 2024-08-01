# RouterRebooter
A containerizable Selenide script to reboot an Arris SURFboard G54 WiFi router and TP-Link RE650 extender

# Context:
* My home network often experiences latency and other issues that are resolved after a router/extender restart.
* This script automates the process of restarting these devices.

# General Information:
* This project has 2 branches: master (RouterRebooter codebase) and ExtenderRebooter (ExtenderRebooter codebase).
* A Dockerfile is provided to allow containerization and secure execution of the script triggered by a separate (SSH enabled) container.
* The Selenide code can be easily adapted to match your make and model of router/extender.

# Reason for containerization:
* I use HomeAssistant (in a container) to trigger the script via an SSH shell command.
  * To allow this, HomeAssistant must have SSH access to the device running the script.
  * Therefore, to avoid allowing the HomeAssistant container to access the host machine, the script can be containerized.
  * Additionally, if your HomeAssistant container is in a MACVlan network it cannot access the host, so containerization of RouterRebooter is necessary.

# How to use:
### Note: If you wish to only use the router or extender rebooter instead of both, simply follow the below instructions below for the one you wish to use.
1.  Use the ```maven package``` command to build an executable jar file for the script. This will create a Rebooter.jar file in the 'target' folder.  
  a. Rename the jar file to the respective name: ```RouterRebooter.jar``` or ```ExtenderRebooter.jar``` (depending on which branch you are on) and copy it into a folder of your choosing.
2.  Repeat the first step for both branches (if desired).

### Note: If you do not wish to containerize the script, you may stop at this step and use the executable jar files as you wish after exporting ```ROUTER_PASSWORD``` or ```EXTENDER_PASSWORD``` environment variables.

3. To build a docker image for the scripts, run the following Docker build command from the RouterRebooter project folder:  
  3.a ```docker build -t mwdle/router_rebooter:latest .```
4. Create a file ```router_pass``` and ```extender_pass``` containing the gateway access passwords (not Wi-Fi passwords) of the respective devices.
5. Move the .jar and password files you created in steps 1 and 4 to a folder of your choosing, ensure the .jar files are marked as executable, and update the bind mounts in the Docker Compose file accordingly.
6. Modify the bind mount in the Docker Compose file for ```id_rsa.pub``` to use the public key of the device that initiates the SSH connection to RouterRebooter.
7. Start by running ```docker compose up -d```
8. To run the rebooter, ssh or docker exec into the container and execute ```/RouterRebooter/executeRouterRebooter.sh``` or ```/RouterRebooter/executeExtenderRebooter.sh```

# Home Assistant Triggers & Sensors
To add entities in Home Assistant for triggering manually, or via scripts and automations:  
1. Ensure HomeAssistant is added to the RouterRebooter network in Docker, and that the id_rsa.pub you provided in the RouterRebooter Docker Compose file is the public key of HomeAssistant.
2. Add the following to your configuration.yaml and restart Home Assistant

```
shell_command:
  router_rebooter: ssh -i /config/.ssh/id_rsa -o 'StrictHostKeyChecking=no' root@RouterRebooter '/RouterRebooter/executeRouterRebooter.sh' && exit
  extender_rebooter: ssh -i /config/.ssh/id_rsa -o 'StrictHostKeyChecking=no' root@RouterRebooter '/RouterRebooter/executeExtenderRebooter.sh' && exit

command_line:
  - sensor:
      name: RouterRebooter Status
      scan_interval: 300 # 5 minute polling interval
      command: "ssh -i /config/.ssh/id_rsa -o 'StrictHostKeyChecking=no' root@RouterRebooter 'cat /RouterRebooter/RouterRebooter.log' && exit"
  - sensor:
      name: ExtenderRebooter Status
      scan_interval: 300 # 5 minute polling interval
      command: "ssh -i /config/.ssh/id_rsa -o 'StrictHostKeyChecking=no' root@RouterRebooter 'cat /RouterRebooter/ExtenderRebooter.log' && exit"
``` 

The entities card works nicely for these entities in Home Assistant:  

```
type: entities
entities:
  - entity: script.router_rebooter
  - entity: sensor.routerstatus
  - entity: script.extender_rebooter
  - entity: sensor.extenderstatus
title: Rebooters
```