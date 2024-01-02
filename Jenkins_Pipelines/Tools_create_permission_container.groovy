pipeline {
    parameters {
        string(name: 'platform_rest_ip', defaultValue: '0.0.0.0', description: 'platformEngg rest server ip')
        string(name: 'platform_rest_port', defaultValue: '8080', description: 'platformEngg rest server port')
        string(name: 'hostname', defaultValue: 'db.example.com', description: 'platformEngg gatway hostname')
        string(name: 'serverName', defaultValue: 'yogesh test', description: 'name of server')
        string(name: 'serverType', defaultValue: 'postgres', description: 'type of server')
        string(name: 'admin_username',defaultValue: 'postgres', description: 'db admin username')
        string(name: 'admin_password',defaultValue: 'Admin@123', description: 'db admin password')
        string(name: 'db_ip', defaultValue: 'user1', description: 'db ip')
        string(name: 'db_port', defaultValue: 'user1', description: 'db port')
        string(name: 'username', defaultValue: 'user1', description: 'db username')
        string(name: 'db_name', defaultValue: 'pass1' , description: 'db password')
        string(name: 'priviledges', defaultValue: 'ALL', description: 'permission on DB')
    } 
    environment {
        Owner = 'Jenkins'
        mailID = 'jenkins@gmail.com'
        restIP = "${params.platform_rest_ip}"
        restPORT = "${params.platform_rest_port}"
    }
    // agent {
    //     label 'kubernetes'
    // }
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
                                ansible-playbook ./${params.serverType}/create-permission.yaml \
                                -e restIP=${restIP} \
                                -e restPORT=${restPORT} \
                                -e hostname=${params.hostname} \
                                -e serverName=${params.serverName} \
                                -e admin_username=${params.admin_username} \
                                -e admin_password=${params.admin_password} \
                                -e db_ip=${params.db_ip} \
                                -e db_port=${params.db_port} \
                                -e username=${params.username} \
                                -e db_name=${params.db_name} \
                                -e priviledges=${params.priviledges} 
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


