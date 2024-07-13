FROM alpine:latest

# Install latest Chromium.
RUN apk upgrade --no-cache --available \
    && apk add --no-cache \
      chromium-swiftshader \
      ttf-freefont \
      font-noto-emoji \
    && apk add --no-cache \
      --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community \
      font-wqy-zenhei
COPY local.conf /etc/fonts/local.conf
ENV CHROME_BIN=/usr/bin/chromium-browser \
    CHROME_PATH=/usr/lib/chromium/ \
    CHROMIUM_FLAGS="--disable-software-rasterizer --disable-dev-shm-usage"

# Install Java 17
RUN apk add openjdk17

# Create user 'rr'
RUN mkdir /home/rr \
    && adduser -h /home/rr -s /bin/sh -D rr \
    && chown -R rr:rr /home/rr \
# Create SSH configuration for user rr
RUN mkdir -p /home/rr/.ssh \
    && chmod 0700 /home/rr/.ssh \
    && passwd -u rr
# Supply your pub key via `--build-arg ssh_pub_key="$(cat ~/.ssh/id_rsa.pub)"` when running `docker build`
ARG ssh_pub_key
RUN echo "$ssh_pub_key" > /home/rr/.ssh/authorized_keys
# Install OpenRC and OpenSSH
RUN apk add openrc openssh \
    && ssh-keygen -A \
    && echo -e "PasswordAuthentication no" >> /etc/ssh/sshd_config
# Touch softlevel because system was initialized without openrc
RUN touch /run/openrc/softlevel
# Expose ssh
EXPOSE 22

RUN mkdir -p /RouterRebooter \
    && chown -R rr:rr /RouterRebooter \
    && chmod 700 /RouterRebooter

# Supply your pub key via `--build-arg rr_jar_path="/path/to/rr.jar"` when running `docker build`
ARG rr_jar_path
# Copy RouterRebooter jar
COPY "$rr_jar_path" /RouterRebooter/

# Supply your pub key via `--build-arg er_jar_path="/path/to/er.jar"` when running `docker build`
ARG er_jar_path
# Copy ExtenderRebooter jar
COPY "$er_jar_path" /RouterRebooter/

USER rr

# Run SSH server
ENTRYPOINT ["sh", "-c", "rc-status; rc-service sshd start"]