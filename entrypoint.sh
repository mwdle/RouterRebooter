#!/bin/bash
if [ ! -f "/RouterRebooter/data/RouterRebooter.log" ]; then
    echo "Not run yet" > "/RouterRebooter/data/RouterRebooter.log"
fi

if [ ! -f "/RouterRebooter/data/ExtenderRebooter.log" ]; then
    echo "Not run yet" > "/RouterRebooter/data/ExtenderRebooter.log"
fi

/usr/sbin/sshd -D