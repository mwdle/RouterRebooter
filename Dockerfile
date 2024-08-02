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

# Setup Router / Extender rebooter directories
RUN mkdir -p /RouterRebooter/data \
    && mkdir -p /RouterRebooter/secrets \
    && chmod -R 0700 /RouterRebooter

COPY entrypoint.sh /RouterRebooter/entrypoint.sh

RUN chmod +x /RouterRebooter/entrypoint.sh

# Run SSH server
CMD ["/RouterRebooter/entrypoint.sh"]