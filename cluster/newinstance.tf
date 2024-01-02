provider "aws" {
  region = "us-east-1"
}

#private instance creation

resource "aws_instance" "priv_instance" {
  count = var.instance_count
  
  root_block_device {
    volume_size = var.storage_size
  }
  
  ami                         = var.amiId
  instance_type               = var.InstanceType
  associate_public_ip_address = false
  
  vpc_security_group_ids = [aws_security_group.security_sg.id]
  subnet_id              = element(var.private_subnets, count.index)
  
  key_name   = var.key_name
  user_data  = file("./myscript.sh")
  
  tags = {
    Name   = "${var.Name}-${count.index + 1}"
    Owner  = var.Owner
    mailID = var.mailID
  }
}
