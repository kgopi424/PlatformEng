/* region          = "us-east-1"
subnet_id       = "subnet-09bb946c638fdd9a3"
security_group_id    = "sg-0d56e86ef61a7dc01" */
Name = "test"
Owner =  "TCS"
mailID = "platformEngineering@gmail.com" 
storage_size = "10"
InstanceType = "t2.micro" 
amiId = "ami-051f7e7f6c2f40dc1"
key_name = "nextgen-devops-team"
instance_count = 2
security_sg_pub_name = "My-Pub-SG"
security_sg_pri_name = "My-Pri-SG"
vpc_id ="vpc-05d7fb7f1331f8f16"
private_subnets=["subnet-09bb946c638fdd9a3","subnet-02cf2e19298b8cdac"]
public_subnets=["subnet-09de534a06e014d69","subnet-09a6ca3c1f8ff1964"]
CIDR="10.63.0.0/16"

