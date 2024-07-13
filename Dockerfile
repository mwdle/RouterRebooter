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

# Supply your pub key via `--build-arg ssh_pub_key="$(cat ~/.ssh/id_rsa.pub)"` when running `docker build`
ARG ssh_pub_key

# Setup SSH server
RUN mkdir -p /root/.ssh \
    && chmod 0700 /root/.ssh \
    && echo "$ssh_pub_key" > /root/.ssh/authorized_keys \
    && apk add openssh \
    && ssh-keygen -A \
    && echo -e "PasswordAuthentication no" >> /etc/ssh/sshd_config

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

# Supply your pub key via `--build-arg routerPassword="somePassword"` when running `docker build`
ARG routerPassword
# Supply your pub key via `--build-arg extenderPassword="somePassword"` when running `docker build`
ARG extenderPassword
ENV routerPassword=${routerPassword}
ENV extenderPassword=${extenderPassword}

# Run SSH server
CMD ["/usr/sbin/sshd", "-D", "-e"]
