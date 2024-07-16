FROM ubuntu:22.04

# Install Google Chrome
RUN apt-get update
RUN apt-get -y install wget
RUN wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
RUN apt-get install ./google-chrome*.deb --yes

# Install Java 21
RUN apt-get -y install openjdk-21-jdk

# Supply your SSH pub key via `--build-arg SSH_PUB_KEY="$(cat ~/.ssh/id_rsa.pub)"` when running `docker build`
ARG SSH_PUB_KEY
# Setup SSH
RUN mkdir -p /root/.ssh \
    && chmod 0700 /root/.ssh \
    && echo "$SSH_PUB_KEY" > /root/.ssh/authorized_keys \
    && apt-get -y install openssh-server \
    && ssh-keygen -A \
    && echo "PasswordAuthentication no" >> /etc/ssh/sshd_config \
    && mkdir /var/run/sshd
EXPOSE 22

# Setup Router / Extender rebooter
RUN mkdir -p /RouterRebooter \
    && chmod 700 /RouterRebooter \
    && echo "Not run yet" > /RouterRebooter/RouterRebooter.log \
    && echo "Not run yet" > /RouterRebooter/ExtenderRebooter.log \
# Supply your router rebooter executable jar via `--build-arg RR_JAR_PATH="/path/to/rr.jar"` when running `docker build` (path must be relative to Dockerfile execution context)
ARG RR_JAR_PATH
COPY "$RR_JAR_PATH" /RouterRebooter
# Supply your extender rebooter executable jar via `--build-arg ER_JAR_PATH="/path/to/er.jar"` when running `docker build` (path must be relative to Dockerfile execution context)
ARG ER_JAR_PATH
COPY "$ER_JAR_PATH" /RouterRebooter
# Supply your router password via `--build-arg ROUTER_PASSWORD=''` when running `docker build`
ARG ROUTER_PASSWORD
# Supply your extender password via `--build-arg EXTENDER_PASSWORD=''` when running `docker build`
ARG EXTENDER_PASSWORD
RUN echo "#!/bin/bash\nexport ROUTER_PASSWORD='${ROUTER_PASSWORD}'\nexport EXTENDER_PASSWORD='${EXTENDER_PASSWORD}'\njava -jar /RouterRebooter/RouterRebooter.jar" > /RouterRebooter/executeRouterRebooter.sh \
    && chmod +x /RouterRebooter/executeRouterRebooter.sh \
    && echo "#!/bin/bash\nexport EXTENDER_PASSWORD='${EXTENDER_PASSWORD}'\njava -jar /RouterRebooter/ExtenderRebooter.jar" > /RouterRebooter/executeExtenderRebooter.sh \
    && chmod +x /RouterRebooter/executeExtenderRebooter.sh

# Run SSH server
CMD ["/usr/sbin/sshd","-D"]