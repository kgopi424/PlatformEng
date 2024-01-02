resource "aws_security_group" "security_sg_pub" {
    name        = var.security_sg_pub_name
    description = "Allow SSH and RDP access"
    vpc_id      = data.terraform_remote_state.vpc.outputs.vpc_id

    ingress {
        description = "SSH ingress"
        from_port   = 22
        to_port     = 22
        protocol    = "tcp"
        cidr_blocks = ["0.0.0.0/0"]
    }

    ingress {
        description = "RDP ingress"
        from_port   = 3389
        to_port     = 3389
        protocol    = "tcp"
        cidr_blocks = ["0.0.0.0/0"]
    }

    egress {
        from_port   = 0
        to_port     = 0
        protocol    = "-1"
        cidr_blocks = ["0.0.0.0/0"]
    }
}
