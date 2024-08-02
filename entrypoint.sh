#!/bin/bash
if [ ! -f "/RouterRebooter/data/RouterRebooter.log" ]; then
    echo "Not run yet" > "/RouterRebooter/data/RouterRebooter.log"
fi

if [ ! -f "/RouterRebooter/data/ExtenderRebooter.log" ]; then
    echo "Not run yet" > "/RouterRebooter/data/ExtenderRebooter.log"
fi


if [ ! -f "/RouterRebooter/data/executeRebooter.sh" ]; then
    echo "#!/bin/bash\nexport ROUTER_PASSWORD=\$(cat /RouterRebooter/secrets/router_pass)\nexport EXTENDER_PASSWORD=\$(cat /RouterRebooter/secrets/extender_pass)\njava -jar /RouterRebooter/data/Rebooter.jar \$1" > "/RouterRebooter/data/executeRebooter.sh"
    chmod +x "/RouterRebooter/data/executeRebooter.sh"
fi

/usr/sbin/sshd -D