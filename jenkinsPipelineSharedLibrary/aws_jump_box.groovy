pipeline {
    parameters {
        string(name: 'Name', description: 'Instance Name')
        string(name: 'storage_size', defaultValue: '10', description: 'Storage Size')
        choice(name: 'InstanceType', choices: ['t2.micro', 't2.small', 't2.medium', 't2.large', 't2.xlarge', 't2.2xlarge'], description: 'Instance Type')
        choice(name: 'OS', choices: ['Windows', 'Linux'], description: 'Instance OS selection')
        choice(name: 'instance_count', choices: ['1', '2', '3'], description: 'Instance Count')
    }
    agent {
        label 'master'
    }
    stages {
        stage('Cloning') {
            steps {
                script {
                    gitclone()
                }
            }
        }
        stage('JumpBox Creation') {
            steps {
                script {
                    def amiId
                    if (params.OS == 'Windows') {
                        amiId = 'ami-09301a37d119fe4c5'
                    } else {
                        amiId = 'ami-053b0d53c279acc90'
                    }
                    def Owner = 'Jenkins'
                    def mailID = 'jenkins@gmail.com'
                    def key_name = 'platformEng'
                    ws("/var/lib/jenkins/workspace/${JOB_NAME}/pub_instance"){
                         sh """
                           sed -i "s/xyz1/${params.Name}/g" backend.tf
                           sed -i "s/My-Pub-SG/public_SG_\${BUILD_NUMBER}/g" terraform.tfvars
                        """
                        sh "terraform init -backend=true"
                        sh "terraform apply -no-color -var Name=${params.Name} -var Owner=${Owner} -var mailID=${mailID} -var storage_size=${params.storage_size} -var InstanceType=${params.InstanceType} -var amiId='${amiId}' -var key_name=${key_name} -var instance_count=${params.instance_count} --auto-approve"
                    }
                }
            }
        }
    }
}
def gitclone(){
    sh 'rm -rf * .git'
    git branch: 'Sprint-1', credentialsId: 'gitlab', url: 'http://10.63.32.87/platformengineering/devops'
}