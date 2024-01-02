output "jump_Box_Public_ips" {
  description = "Public IP addresses of the Jump-Box instance"
  value       = aws_instance.public_instance[*].public_ip
} 

