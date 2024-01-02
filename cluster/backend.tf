terraform {
  backend "s3" {
    bucket = "pe2dev"
    key    = "platform_Enginnering/cluster/xyz1/terraform.tfstate"
    region = "us-east-1"
  }
}
