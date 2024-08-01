FROM ubuntu:22.04

# Install Google Chrome
RUN apt-get update \
    && apt-get -y install wget \
    && wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
    && apt-get install ./google-chrome*.deb --yes \
    && apt-get clean

# Install Java 21
RUN apt-get -y install openjdk-21-jdk \
    && apt-get clean

# Setup SSH
RUN mkdir -p /root/.ssh \
    && chmod 0700 /root/.ssh \
    && apt-get -y install openssh-server \
    && apt-get clean \
    && ssh-keygen -A \
    && echo "PasswordAuthentication no" >> /etc/ssh/sshd_config \
    && mkdir /var/run/sshd
EXPOSE 22

# Setup Router / Extender rebooter
RUN mkdir -p /RouterRebooter \
    && chmod 700 /RouterRebooter \
    && echo "Not run yet" > /RouterRebooter/RouterRebooter.log \
    && echo "Not run yet" > /RouterRebooter/ExtenderRebooter.log

RUN echo "#!/bin/bash\nexport ROUTER_PASSWORD='$(cat /RouterRebooter/secrets/router_pass)'\njava -jar /RouterRebooter/RouterRebooter.jar" > /RouterRebooter/executeRouterRebooter.sh \
    && chmod +x /RouterRebooter/executeRouterRebooter.sh \
    && echo "#!/bin/bash\nexport EXTENDER_PASSWORD='$(cat /RouterRebooter/secrets/extender_pass)'\njava -jar /RouterRebooter/ExtenderRebooter.jar" > /RouterRebooter/executeExtenderRebooter.sh \
    && chmod +x /RouterRebooter/executeExtenderRebooter.sh

# Run SSH server
CMD ["/usr/sbin/sshd","-D"]