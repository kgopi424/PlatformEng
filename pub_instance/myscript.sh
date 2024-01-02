#! /bin/bash
sudo apt update -y
sudo mkdir -p /home/$(whoami)/.ssh
sleep 1
sudo bash -c "echo 'ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDCWCGpnXT5ZW7xqpLHDTj1DTAZXmxcNyEVY2lXg+zay2ByUP2n4mBdaMY2sPfc3ADp+mo2gllSYmfEhL+nfs2AGU01WRlt+DWMTMYEtzuOSLOc3lHj/IMjrnE3zLg+1D1UMZ6iu80fV/IkLPAJVc0L/fNASsWtLQAwlokScv3oFnaKWdbfBkyyxexeQcsrQfVBnPS0XTpl/jpFsFHj1tAxRYgrYgftfs2Mz6Jdgeve9yo7VYuFgOtfQphi5VvA30Tl8ySxiMKXlCuu5X4gnL5hdbcSMNLTG3sjX9U9GJZ3YxVvdVbgEiRJHhCDztKRyU6la4fL5QYB0f89DmJMnPcz' > /home/$(whoami)/.ssh/authorized_keys"
sleep 1
sudo chown -R $(whoami):$(whoami) /home/ansible/.ssh
sleep 1
sudo chmod 700 /home/$(whoami)/.ssh
sudo chmod 600 /home/$(whoami)/.ssh/authorized_keys
