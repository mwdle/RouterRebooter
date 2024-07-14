FROM ubuntu:22.04

# Install Google Chrome
RUN apt-get update
RUN apt-get -y install wget
RUN wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
RUN apt-get install ./google-chrome*.deb --yes

# Install Java 21
RUN apt-get -y install openjdk-21-jdk

# Supply your pub key via `--build-arg ssh_pub_key="$(cat ~/.ssh/id_rsa.pub)"` when running `docker build`
ARG ssh_pub_key

# Setup SSH server
RUN mkdir -p /root/.ssh \
    && chmod 0700 /root/.ssh \
    && echo "$ssh_pub_key" > /root/.ssh/authorized_keys \
    && apt-get -y install openssh-server \
    && ssh-keygen -A \
    && echo "PasswordAuthentication no" >> /etc/ssh/sshd_config \
    && mkdir /var/run/sshd

# Expose ssh
EXPOSE 22

RUN mkdir -p /RouterRebooter \
    && chmod 700 /RouterRebooter
# Supply your pub key via `--build-arg rr_jar_path="/path/to/rr.jar"` when running `docker build`
ARG rr_jar_path
# Supply your pub key via `--build-arg er_jar_path="/path/to/er.jar"` when running `docker build`
ARG er_jar_path
# Copy RouterRebooter jar
COPY "$rr_jar_path" /RouterRebooter/
# Copy ExtenderRebooter jar
COPY "$er_jar_path" /RouterRebooter/

# Run SSH server
CMD ["/usr/sbin/sshd","-D"]
