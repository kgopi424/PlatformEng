/* data "terraform_remote_state" "vpc" {
  backend = "local"
  config = {
    path = "../vpc/terraform.tfstate"
  }
} */

/* resource "aws_security_group" "security_sg" {
    name        = var.security_sg_pri_name
    description = "Allow HTTPS to web server"  #data.terraform_remote_state.vpc.outputs.default_security_group_id
    vpc_id      = data.terraform_remote_state.vpc.outputs.vpc_id

    ingress {
        description = "HTTPS ingress"
        from_port   = 22
        to_port     = 22
        protocol    = "tcp"
        cidr_blocks = [
            data.terraform_remote_state.vpc.outputs.vpc_cidr_block
        ]
    }
    egress {
        from_port   = 0
        to_port     = 0
        protocol    = "-1"
        cidr_blocks = ["0.0.0.0/0"]
   }
} */


resource "aws_security_group" "security_sg"  {
    name        = "Cluster_SG"
    description = "Kubernetes Security Group"
    vpc_id      =  var.vpc_id
}

# Inbound rules
resource "aws_security_group_rule" "inbound_rules" {
    count = 8 
    type        = "ingress"
    from_port   = element([22,80,6443, 2379, 10250, 10259, 10257, 30000], count.index)
    to_port     = element([22,80,6443, 2380, 10250, 10259, 10257, 32767], count.index)
    protocol    = "tcp"
    cidr_blocks = [var.CIDR]
    security_group_id = aws_security_group.security_sg.id
}

# Outbound rules (egress)
resource "aws_security_group_rule" "egress_rules" {
    type        = "egress" 
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    security_group_id = aws_security_group.security_sg.id
}

