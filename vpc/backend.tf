terraform {
  backend "s3" {
    bucket = "pe2dev"
    key    = "platform_Enginnering/VPC/terraform.tfstate"
    region = "us-east-1"
  }
}
