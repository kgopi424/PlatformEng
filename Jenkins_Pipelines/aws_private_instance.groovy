pipeline {
    parameters {
        string(name: 'Name', description: 'Instance Name')
        string(name: 'gatewayIP', defaultValue: '0.0.0.0', description: 'Storage Size')
        string(name: 'gatewayPORT', defaultValue: '80', description: 'Storage Size')
        string(name: 'hostname', defaultValue: 'hostname', description: 'Storage Size')
        string(name: 'storage_size', defaultValue: '10', description: 'Storage Size')
        choice(name: 'InstanceType', choices: ['t2.micro', 't2.small', 't2.medium', 't2.large', 't2.xlarge', 't2.2xlarge'], description: 'Instance Type')
        choice(name: 'instance_count', choices: ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10'], description: 'Instance Count')
    }
    environment{
        amiId = 'ami-053b0d53c279acc90'
        Owner = 'Jenkins'
        mailID = 'jenkins@gmail.com'
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
        stage('Action on Instance') {
            steps {
                script {
                    ws("/var/lib/jenkins/workspace/${JOB_NAME}/pri_instance") {
                        def fileCount = sh(script: 'ls -la | wc -l', returnStdout: true).trim()
                        echo "File count: $fileCount"
                        if (fileCount.toInteger() == 3) {
                            gitclone()
                            sh 'cp -r pri_instance/* ./'
                            terraform()
                        } else {
                            terraform()
                        }
                    }
                }
            }
        }
    }
}
def gitclone(){
    sh 'rm -rf * .git'
    git branch: 'Sprint-3', credentialsId: 'gitlab', url: 'http://10.63.32.87/platformengineering/devops'
}
def terraform() {
    sh """
        sed -i \"s/xyz1/${params.Name}/g\" backend.tf
        sed -i \"s/example-sg/private_SG_\${BUILD_NUMBER}/g\" securityGroup.tf
    """
    sh 'terraform init -backend=true -no-color'
    sh "terraform apply -no-color -var Name=${params.Name} -var Owner=${Owner} -var mailID=${mailID} -var storage_size=${params.storage_size} -var InstanceType=${params.InstanceType} -var amiId='${amiId}' -var instance_count=${params.instance_count} --auto-approve"
    sh "for i in \$(terraform output -json instance_IDs| tr -d '[]\"' | tr ',' ' '); do  LC_ALL=C.UTF-8 ansible-playbook postdata.yaml -e storage_size=${params.storage_size} -e env_name=${params.Name} -e gatewayIP=${params.gatewayIP} -e gatewayPORT=${params.gatewayPORT} -e hostname=${params.hostname} --extra-vars \"instance_IDs=['\$i']\"; done "          
    sh "terraform state list"
}
