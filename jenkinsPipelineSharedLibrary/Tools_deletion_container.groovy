pipeline {
    parameters {
        string(name: 'release_name', defaultValue: 'postgres-xxx', description: 'name of helm release')
        string(name: 'release_namespace', defaultValue: 'dbprovision', description: 'namespace where db is installed')
        string(name: 'hostname', defaultValue: 'db.example.com', description: 'platformEngg gateway hostname')
        string(name: 'platform_rest_ip', defaultValue: '0.0.0.0', description: 'platformEngg rest server ip')
        string(name: 'platform_rest_port', defaultValue: '8080', description: 'platformEngg rest server port')
    } 
    environment {
        Owner = 'Jenkins'
        mailID = 'jenkins@gmail.com'
        restIP = "${params.platform_rest_ip}"
        restPORT = "${params.platform_rest_port}"
    }
    agent {
        label 'kubernetes'
    }
    stages {
        stage('Creating Inventory File') {
            steps {
                script {
                    ws("/home/ubuntu/jenkins/workspace/${JOB_NAME}/"){
                        
                        sh "mkdir ./${BUILD_NUMBER}"
                        sh "cd ./${BUILD_NUMBER}"
                        
                        gitclone()
                        echo "${BUILD_URL}"
                        
                        sh """
                            ansible-playbook delete-server.yaml \
                            -e restIP=${restIP} \
                            -e restPORT=${restPORT} \
                            -e hostname=${params.hostname} \
                            -e helm_release_name=${params.release_name} \
                            -e helm_release_namespace=${params.release_namespace} 
                        """
                        sh "cd .."
                        sh "sudo rm -rf ./${BUILD_NUMBER}*"
                    }
                }
            }
        }
    }
}


def gitclone(){
    git branch: 'dbprovisioning', credentialsId: 'gitlab', url: 'http://10.63.32.87/platformengineering/devops'
}

