# RouterRebooter  

A containerizable Selenide script to reboot an Arris SURFboard G54 WiFi router and TP-Link RE650 extender  

## Table of Contents  

* [Description](#routerrebooter)  
* [Context](#context)  
* [General Information](#general-information)
* [Reasons For Containerization](#reasons-for-containerization)
* [Getting Started](#getting-started)
* [Home Assistant Triggers & Sensors](#home-assistant-triggers--sensors)
* [License](#license)  
* [Disclaimer](#disclaimer)  

## Context  

* My home network often experiences latency and other issues that are resolved after a router/extender restart.  
* This script automates the process of restarting these devices.  

## General Information  

* The RouterRebooter.java Selenide code can be easily adapted to match your make and model of router/extender.  
* A Dockerfile and Docker Compose file is provided to allow containerization and secure execution of the script triggered by the host or another (SSH enabled) container.  

## Reasons For Containerization  

* I use HomeAssistant (in a container) to trigger the script via an SSH shell command.
  * To allow this, HomeAssistant must have SSH access to the device running the script.  
* Therefore, to avoid allowing the HomeAssistant container to access the host machine, the script can be containerized.  
* Additionally, if your HomeAssistant container is in a MACVlan network it cannot access the host, so the script must be containerized to allow access to your HomeAssistant container.  

## Getting Started  

1. Clone the repository:  

    ```shell
    git clone https://github.com/mwdle/RouterRebooter.git
    ```  

2. In the project directory, Execute the following command to create a Rebooter.jar file in the 'target' folder:  

    ```shell
    mvn package
    ```  

3. If you do not wish to containerize the script, you may stop at this step and run the executable jar file using the following:

    ```shell
    export ROUTER_PASSWORD=<YOUR_ROUTER_PASSWORD>
    # or
    export EXTENDER_PASSWORD=<YOUR_EXTENDER_PASSWORD>
    ```  
  
    then:
  
    ```shell
    java -jar Rebooter.jar router
    # or
    java -jar Rebooter.jar extender
    ```  

4. Create a folder on your system for Docker bind mounts / storing container files. The folder should have the following structure:  

    ```shell
    docker_volumes/
    ├── RouterRebooter/
    │   ├── data/
    │   └── secrets/
    ```  

5. Change the `.env` file properties for your configuration:  

    ```properties
    DOCKER_VOLUMES=<PATH_TO_DOCKER_VOLUMES_FOLDER> # The folder created in the previous step.
    ```  

6. To build a docker image for the scripts, execute the following Docker build command from the RouterRebooter project folder:  

    ```shell
    docker build -t mwdle/router_rebooter:latest .
    ```  

7. Create a file `router_pass` and `extender_pass` containing the gateway access passwords (not Wi-Fi passwords) of the respective devices.  
8. Move the .jar and password files you created in steps 1 and 4 to a folder of your choosing, ensure the .jar files are marked as executable, and update the bind mounts in the Docker Compose file accordingly.  
9. Modify the bind mount in the Docker Compose file for `id_rsa.pub` to use the public key of the device/container that initiates the SSH connection to RouterRebooter.  
10. Start the container by executing:

    ```shell
    docker compose up -d
    ```  

11. To execute the rebooter in Docker:

    ```shell
    docker exec -it RouterRebooter bash
    # then
    /RouterRebooter/data/executeRebooter.sh router
    # and/or
    /RouterRebooter/data/executeRebooter.sh extender
    ```

12. If setting up access for Home Assistant, you must `docker exec` into the container, execute `ssh root@router_rebooter`, then enter 'yes' to add RouterRebooter to the known hosts file.
13. Anytime you remove the container and start it again, Home Assistant will not connect to RouterRebooter until you delete the `known_hosts` file in Home Assistant and repeat step 12.  

## Home Assistant Triggers & Sensors  

To add entities in Home Assistant for triggering manually, or via scripts and automations:  

1. Ensure HomeAssistant is added to the RouterRebooter network in Docker, and that the id_rsa.pub you provided in the RouterRebooter Docker Compose file is the public key of HomeAssistant.  
2. Add the following to your configuration.yaml and restart Home Assistant  

```YAML
shell_command:
  router_rebooter: "ssh -i ~/.ssh/id_rsa -o 'StrictHostKeyChecking=no' root@RouterRebooter '/RouterRebooter/data/executeRebooter.sh router' && exit"
  extender_rebooter: "ssh -i ~/.ssh/id_rsa -o 'StrictHostKeyChecking=no' root@RouterRebooter '/RouterRebooter/data/executeRebooter.sh extender' && exit"

command_line:
  - sensor:
      name: RouterRebooter Status
      scan_interval: 300 # 5 minute polling interval
      command: "ssh -i ~/.ssh/id_rsa -o 'StrictHostKeyChecking=no' root@RouterRebooter 'cat /RouterRebooter/data/RouterRebooter.log' && exit"
  - sensor:
      name: ExtenderRebooter Status
      scan_interval: 300 # 5 minute polling interval
      command: "ssh -i ~/.ssh/id_rsa -o 'StrictHostKeyChecking=no' root@RouterRebooter 'cat /RouterRebooter/data/ExtenderRebooter.log' && exit"
```  

The entities card works nicely for these entities in Home Assistant:  

```YAML
type: entities
entities:
  - entity: script.router_rebooter
  - entity: sensor.routerstatus
  - entity: script.extender_rebooter
  - entity: sensor.extenderstatus
title: Rebooters
```  

## License  

This project is licensed under the GNU General Public License v3.0 (GPL-3.0). See the [LICENSE](LICENSE.txt) file for details.  

## Disclaimer  

This repository is provided as-is and is intended for informational and reference purposes only. The author assumes no responsibility for any errors or omissions in the content or for any consequences that may arise from the use of the information provided. Always exercise caution and seek professional advice if necessary.  
