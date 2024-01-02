resource "aws_security_group" "security_sg"  {
    name        = "example-sg-109"
    description = "Kubernetes Security Group"
    vpc_id      = var.vpc_id
}
# Inbound rules
resource "aws_security_group_rule" "inbound_rules" {
    count = 6 
    type        = "ingress"
    from_port   = element([22, 80, 8080, 9000, 9090, 3000], count.index)
    to_port     = element([22, 80, 8080, 9000, 9090, 3000], count.index)
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
