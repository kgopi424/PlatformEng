pipeline {
    agent {
        label 'master'
    }
    parameters {
        choice(name: 'action', choices: ['start', 'stop', 'changeType','modifyVolume','addNodeToCluster'], description: 'Action for instance')
        string(name: 'instanceID', description: 'Provide the Instance ID to take Action')
        choice(name: 'change_instance_to', choices: ['t2.nano', 't2.micro', 't2.small','t2.medium', 't2.large', 't2.xlarge'], description: 'Select Instance Type')
        string(name: 'newVolumeSize',description: 'New size cannot be smaller than existing size')
        string(name: 'hostname', defaultValue: 'cluster.example2.com', description: 'platformEngg gatway hostname')
        string(name: 'platform_rest_ip', defaultValue: '0.0.0.0', description: 'platformEngg rest server ip')
        string(name: 'platform_rest_port', defaultValue: '8080', description: 'platformEngg rest server port')
        string(name: 'envType', defaultValue: 'none', description: 'type of env for LifeCycle')
        string(name: 'masterIP', defaultValue: 'none', description: 'Master IP of the cluster')
        string(name: 'workerIP', defaultValue: 'none', description: 'Worker IP to Join this Node to Cluster')
    }
    stages {
        stage('CloneRepo Start, Stop') {
            steps {
                script {
                    gitclone()
                }
            }
        }
        stage('Action on Instances') {
            steps {
                script {
                    def workspacePath = "/var/lib/jenkins/workspace/${JOB_NAME}/LifeCycle"
                    ws(workspacePath) {
                        def fileCount = sh(script: 'ls -la | wc -l', returnStdout: true).trim()
                        echo "File count: $fileCount"
                        if (fileCount.toInteger() == 3) {
                            gitclone()
                            sh 'cp -r LifeCycle/* ./'
                            Actions()
                        }else{
                            Actions()
                        }
                    }
                }
            }
        }
        stage('Adding Node to Cluster'){
            steps{
                script{
                    def workspacePath = "/var/lib/jenkins/workspace/${JOB_NAME}/LifeCycle"
                    ws(workspacePath) {
                        if (params.action == 'addNodeToCluster'){
                            sh "rm -rf inventory 2> /dev/null && touch inventory"
                            sh 'echo "[master]" >> inventory '
                            sh "echo -n 'master ansible_host='  >> inventory"
                            sh "echo ${params.masterIP} >> inventory"
                            sh 'echo "[worker]" >> inventory'
                            params.workerIP.split(',').each { IP -> 
                                sh "echo ${IP}  >> inventory"
                            }
                            sh "ansible-playbook add_node.yaml"
                        }
                    }
                }
            }
        }
        stage('Sending Data to DB'){
            steps{
                script{
                    def workspacePath = "/var/lib/jenkins/workspace/${JOB_NAME}/LifeCycle"
                    
                    ws(workspacePath){
                        
                        if(params.envType == 'Cluster' || params.envType == 'cluster'){
                            
                            def apiUrl = ""
                            def restIP = params.platform_rest_ip
                            def restPORT = params.platform_rest_port
                            
                            if(params.action == 'start'){
                                
                                apiUrl = "${restIP}:${restPORT}/platform/v1/database/startInstance"
                                
                                // Construct the curl command
                                def curlCommand = """
                                    curl -X POST  \\
                                    -H "Host: ${params.hostname}" \\
                                    -F "instance_id=${params.instanceID}" \\
                                    ${apiUrl} 
                                """
                                
                                sh(curlCommand)
                            }
                            else if(params.action == 'stop'){
                                
                                apiUrl = "${restIP}:${restPORT}/platform/v1/database/stopInstance"
                                
                                // Construct the curl command
                                def curlCommand = """
                                    curl -X POST \\
                                    -H "Host: ${params.hostname}" \\
                                    -F "instance_id=${params.instanceID}" \\
                                    ${apiUrl} 
                                """
                                
                                sh(curlCommand)
                            }
                            else if(params.action == 'changeType'){
                                
                                apiUrl = "${restIP}:${restPORT}/platform/v1/database/updateInstanceType"
                                
                                // Construct the curl command
                                def curlCommand = """
                                    curl -X POST \\
                                    -H "Host: ${params.hostname}" \\
                                    -F "instance_id=${params.instanceID}" \\
                                    -F "instance_type=${params.change_instance_to}" \\
                                    ${apiUrl} 
                                """
                                
                                sh(curlCommand)
                            }
                            else if(params.action == 'modifyVolume'){
                                
                                apiUrl = "${restIP}:${restPORT}/platform/v1/database/updateInstanceVolume"
                                
                                // Construct the curl command
                                def curlCommand = """
                                    curl -X POST \\
                                    -H "Host: ${params.hostname}" \\
                                    -F "instance_id=${params.instanceID}" \\
                                    -F "instance_volume=${params.newVolumeSize}" \\
                                    ${apiUrl} 
                                """
                                
                                sh(curlCommand)
                            }
                            else{
                                error "Invalid action specified"
                            }
                            
                        }
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
def Actions(){
    if (params.action == 'start') {
        sh "ansible-playbook -i inventory Start.yaml -e ansible_ec2_instance_id=${params.instanceID}"
    } else if (params.action == 'stop') {
        sh "ansible-playbook -i inventory Stop.yaml -e ansible_ec2_instance_id=${params.instanceID}"
    } else if (params.action == 'changeType') {
        sh "ansible-playbook -i inventory typeChange.yaml -e ansible_ec2_instance_id=${params.instanceID} -e change_instance_to=${params.change_instance_to}"
    }else if (params.action == 'modifyVolume') {
        sh "ansible-playbook ebs_expansion.yaml -e ansible_ec2_instance_id=${params.instanceID} -e new_vol_size=${params.newVolumeSize}"
    }else if (params.action == 'addNodeToCluster'){
        echo "Adding Node stage will execute"
    }else {
        error "Invalid action specified"
    }
}