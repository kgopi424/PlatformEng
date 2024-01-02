pipeline {
    parameters {
        string(name: 'serverName', defaultValue: 'platformEngg', description: 'name of database server')
        string(name: 'serverType', defaultValue: 'postgres', description: 'Database server type (postgres or mongodb)')
        string(name: 'serverMode', defaultValue: 'exclusive', description: 'Database server modes (exclusive or shared)')
        string(name: 'admin_username',defaultValue: 'postgres', description: 'db admin username')
        string(name: 'admin_password',defaultValue: 'Admin@123', description: 'db admin password')
        string(name: 'storage_size', defaultValue: '50Mi', description: 'storage size of database')
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
    // agent {
    //     label 'kubernetes'
    // }
    stages {
        stage('Creating Inventory File') {
            steps {
                script {
                    ws("/home/ubuntu/jenkins/workspace/${JOB_NAME}/"){
                        
                        echo "HOSTNAME ${params.hostname}"
                        
                        sh "mkdir ./${BUILD_NUMBER}"
                        sh "cd ./${BUILD_NUMBER}"
                        
                        gitclone()
                        echo "${BUILD_URL}"
                        
                        def chartName = params.serverType == "mongodb" ? "bitnami/mongodb" : "bitnami/postgresql"
                        
                        if (serverType == 'postgres' || serverType == 'mongodb') {
                            sh """
                                ansible-playbook ./${params.serverType}/create-server.yaml \
                                -e restIP=${restIP} \
                                -e restPORT=${restPORT} \
                                -e hostname=${params.hostname} \
                                -e serverName=${params.serverName} \
                                -e serverType=${params.serverType} \
                                -e serverMode=${params.serverMode} \
                                -e admin_username=${params.admin_username} \
                                -e admin_password=${params.admin_password} \
                                -e size=${params.storage_size} \
                                -e helm_chart_name=${chartName} \
                                -e helm_release_namespace="dbprovision" \
                                -e helm_chart_version="latest" \
                                -e number_of_replicas="1" \
                                -e storageClass="local-hostpath-storage" \
                                -e accessMode="ReadWriteOnce"
                            """
                        } else {
                            error("Invalid dbtype: Only 'postgres' and 'mongodb' are allowed.")
                        }
                        
                        sh "cd .."
                        sh "sudo rm -rf ../${BUILD_NUMBER}*"
                    }
                }
            }
        }
    }
}


def gitclone(){
    git branch: 'dbprovisioning', credentialsId: 'gitlab', url: 'http://10.63.32.87/platformengineering/devops'
}