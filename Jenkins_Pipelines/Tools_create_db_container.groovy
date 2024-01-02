pipeline {
    parameters {
        string(name: 'serverName', defaultValue: 'platformEngg', description: 'name of database server')
        string(name: 'serverType', defaultValue: 'postgres', description: 'Database server type (postgres or mongodb)')
        string(name: 'serverMode', defaultValue: 'exclusive', description: 'Database server modes (exclusive or shared)')
        string(name: 'admin_username',defaultValue: 'postgres', description: 'db admin username')
        string(name: 'admin_password',defaultValue: 'Admin@123', description: 'db admin password')
        string(name: 'db_name',defaultValue: 'Admin@123', description: 'db admin password')
        string(name: 'db_ip',defaultValue: 'Admin@123', description: 'db admin password')
        string(name: 'db_port',defaultValue: 'Admin@123', description: 'db admin password')
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
                        
                        if (params.serverType == 'postgres' || params.serverType == 'mongodb') {
                            sh """
                                ansible-playbook ./${params.serverType}/create-db.yaml \
                                -e restIP=${restIP} \
                                -e restPORT=${restPORT} \
                                -e hostname=${params.hostname} \
                                -e serverName=${params.serverName} \
                                -e serverType=${params.serverType} \
                                -e serverMode=${params.serverMode} \
                                -e admin_username=${params.admin_username} \
                                -e admin_password=${params.admin_password} \
                                -e db_ip=${params.db_ip} \
                                -e db_port=${params.db_port} \
                                -e db_name=${params.db_name} 
                            """
                        } else {
                            error("Invalid dbtype: Only 'postgres' and 'mongodb' are allowed.")
                        }
                        
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


