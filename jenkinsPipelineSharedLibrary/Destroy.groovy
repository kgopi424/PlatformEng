pipeline {    
    agent {
        label 'master'
    }
    parameters{
        string(name: 'Name', description: 'Name')
        string(name: 'hostname', defaultValue: 'cluster.example2.com', description: 'platformEngg gateway hostname')
        string(name: 'platform_rest_ip', defaultValue: '0.0.0.0', description: 'platformEngg rest server ip')
        string(name: 'platform_rest_port', defaultValue: '8080', description: 'platformEngg rest server port')
    }
    environment {
        restIP = "${params.platform_rest_ip}"
        restPORT = "${params.platform_rest_port}"
        hostName = "${params.hostname}"
    }
    stages {
        stage('Cloning') {
            steps {
                script {
                    gitclone()
                    echo "Machine_Type: - ${params.Machine_Type}"
                }
            }
        }
        stage('Action on Instance') {
            steps {
                script {
                    if (params.Machine_Type == 'Private_Instance'){
                        if (params.Nodes == "All_Instances"){
                            ws("/var/lib/jenkins/workspace/${JOB_NAME}/pri_instance"){
                                def fileCount = sh(script: 'ls -la | wc -l', returnStdout: true).trim()
                                if (fileCount.toInteger() == 3) {
                                    gitclone()
                                    sh 'cp -r pri_instance/* ./'
                                    destroyInstance()
                                    all_instance()
                                }
                                else{
                                    destroyInstance()
                                    all_instance()
                                }
                            }
                        }
                        if (params.Nodes == "Node"){
                            ws("/var/lib/jenkins/workspace/${JOB_NAME}/pri_instance"){
                                def fileCount = sh(script: 'ls -la | wc -l', returnStdout: true).trim()
                                if (fileCount.toInteger() == 3) {
                                    gitclone()
                                    sh 'cp -r pri_instance/* ./'
                                    destroyInstance()
                                    privateNode()
                                }
                                else{
                                    destroyInstance()
                                    privateNode()
                                }
                            }
                        }
                    }
                    if (params.Machine_Type == 'Jump_Box'){
                        ws("/var/lib/jenkins/workspace/${JOB_NAME}/pub_instance"){
                            def fileCount = sh(script: 'ls -la | wc -l', returnStdout: true).trim()
                            if (fileCount.toInteger() == 3) {
                                gitclone()
                                sh "cp -r pub_instance/* ./"
                            }
                            destroyInstance()
                            sh "terraform destroy -no-color --auto-approve"
                            sh "aws s3 rm s3://pe2dev/platform_Enginnering/public_Instance/${params.state_file_location}  --recursive"
                        }
                    }
                    if (params.Machine_Type == 'Cluster'){
                        if (params.Nodes == "Complete_Cluster"){
                            ws("/var/lib/jenkins/workspace/${JOB_NAME}/cluster"){
                                def fileCount = sh(script: 'ls -la | wc -l', returnStdout: true).trim()
                                if (fileCount.toInteger() == 3) {
                                    gitclone()
                                    sh "cp -r cluster/* ./"
                                    destroyInstance()
                                    complete_cluster()
                                }
                                else { 
                                    destroyInstance()
                                    complete_cluster()
                                }
                            }
                        }
                        if (params.Nodes == "Node"){
                            ws("/var/lib/jenkins/workspace/${JOB_NAME}/cluster"){
                                def fileCount = sh(script: 'ls -la | wc -l', returnStdout: true).trim()
                                if (fileCount.toInteger() == 3) {
                                    gitclone()
                                    sh 'cp -r cluster/* ./'
                                    destroyInstance()
                                    clusternode()
                                }
                                else {
                                    destroyInstance()
                                    clusternode()
                                }
                            }
                        }
                    }
                }   
            }
        }
        stage('update database entries') {
            steps {
                script {
                    if (params.Machine_Type == 'Cluster') {
                        def restURL = params.platform_rest_url
                        ws("/var/lib/jenkins/workspace/${JOB_NAME}/cluster") {
                            if (params.Nodes == "Complete_Cluster") {
                                deleteCluster(restURL, params.state_file_location)
                            } else if (params.Nodes == "Node") {
                                deleteClusterNodes(restURL, params.state_file_location, params.Name)
                            } else {
                                error "Invalid option"
                            }
                        }
                    }
                }
            }
        }
    }
}
def all_instance(){
    sh "terraform destroy -no-color --auto-approve"
    sh "aws s3 rm s3://pe2dev/platform_Enginnering/private_Instance/${params.state_file_location}  --recursive"                        
}
def privateNode(){
    sh "aws s3 cp s3://pe2dev/platform_Enginnering/private_Instance/${params.state_file_location}terraform.tfstate ./"
    params.Name.split(',').each { name ->
        def num = (name =~ /(\d+)$/)[0][1].toInteger() - 1 // Extract the last number and subtract 1
        def test = sh(script: """cat terraform.tfstate | jq -r '.resources[] | select(.instances[].attributes.tags.Name == "${name}") | .instances[${num}].attributes.tags.Name'""", returnStdout: true).trim()
        if (test == name) {
            sh "terraform destroy -no-color -target aws_instance.priv_instance[${num}] --auto-approve"
        }
        else{
            error "Machine doesn't exists"
        }
    }
}
def complete_cluster(){
    sh "terraform destroy -no-color --auto-approve"
    sh "aws s3 rm s3://pe2dev/platform_Enginnering/cluster/${params.state_file_location}  --recursive"
}
def clusternode(){
    sh "aws s3 cp s3://pe2dev/platform_Enginnering/cluster/${params.state_file_location}/terraform.tfstate ./"
    params.Name.split(',').each { name ->
        def num = (name =~ /(\d+)$/)[0][1].toInteger() - 1 // Extract the last number and subtract 1
        def test = sh(script: """cat terraform.tfstate | jq -r '.resources[] | select(.instances[].attributes.tags.Name == "${name}") | .instances[${num}].attributes.tags.Name'""", returnStdout: true).trim()
        if (test == name) {
            sh "terraform destroy -no-color -target aws_instance.priv_instance[${num}] --auto-approve"
        }
    }
}
def gitclone(){
    sh 'rm -rf * .git'
    git branch: 'Sprint-3', credentialsId: 'gitlab', url: 'http://10.63.32.87/platformengineering/devops'
}
def destroyInstance() {
    def cleanedStateLocation = state_file_location.replaceAll(/\/$/, '')
    sh """
        cleanedStateLocation=\$(echo "${cleanedStateLocation}" | tr -d '/')
        sed -i "s/xyz1/\${cleanedStateLocation}/g" backend.tf
    """
    sh 'terraform init -backend=true -reconfigure -no-color'
}
def deleteCluster(String restURL, String stateFileLocation) {

    def apiUrl = "http://${restIP}:${restPORT}/platform/v1/database/deleteCluster"
    def curlCommand = "curl -X DELETE -H \"Host: ${hostName}\" -F \"clusterName=${stateFileLocation}\" ${apiUrl}"
    sh(curlCommand)
}

def deleteClusterNodes(String restURL, String stateFileLocation, String nodeName) {
    def apiUrl = "http://${restIP}:${restPORT}/platform/v1/database/deleteClusterNodes"
    def curlCommand = "curl -X DELETE -H \"Host: ${hostName}\" -F \"clusterName=${stateFileLocation}\" -F \"nodeName=${nodeName}\" ${apiUrl}"
    sh(curlCommand)
}

// jenkins@ip-10-63-20-41:~$ cat Private_Instance.sh
// #!/bin/bash
// aws s3 ls s3://pe2dev/platform_Enginnering/private_Instance/ |grep -i PRE | awk '{ print $2}'

// jenkins@ip-10-63-20-41:~$ cat Jump_Box.sh
// #!/bin/bash
// aws s3 ls s3://pe2dev/platform_Enginnering/public_Instance/ |grep -i PRE | awk '{ print $2}'

// jenkins@ip-10-63-20-41:~$ cat Cluster.sh
// #!/bin/bash
// aws s3 ls s3://pe2dev/platform_Enginnering/cluster/ |grep -i PRE | awk '{ print $2}'
