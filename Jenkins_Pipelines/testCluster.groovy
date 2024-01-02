pipeline {
    parameters {
        choice(name: 'Action', choices: ['apply', 'destroy'], description: 'Apply will create a resource, destroy will delete')
        string(name: 'Name', defaultValue: 'PlatformEngineering', description: 'Business division')
        string(name: 'storage_size', defaultValue: '30', description: 'Storage Size')
        choice(name: 'InstanceType', choices: ['t2.medium', 't2.large', 't2.xlarge', 't2.2xlarge'], description: 'Instance Type')
        choice(name: 'Instance_count', choices: ['2', '3', '4'], description: 'Instance Count')
        choice(name: 'cniType', choices: ['flannel', 'calico', 'multus'], description: 'k8s CNI type')
        string(name: 'platform_jumpbox_ip', defaultValue: '0.0.0.0', description: 'platformEngg jumpbox IP')
        string(name: 'platform_rest_dns', defaultValue: 'developer.com', description: 'platformEngg rest server dns')
        string(name: 'platform_rest_ip', defaultValue: '0.0.0.0', description: 'platformEngg rest server ip')
        string(name: 'platform_rest_port', defaultValue: '8080', description: 'platformEngg rest server port')
    }
    environment {
        Owner = 'Jenkins'
        mailID = 'jenkins@gmail.com'
        key_name = 'platformEng'
        PE_IP = "${params.platform_jumpbox_ip}"
        restDNS = "${params.platform_rest_dns}"
        restIP = "${params.platform_rest_ip}"
        restPORT = "${params.platform_rest_port}"
        name="${params.Name}"
        Storage_size="${params.storage_size}"
        instanceType="${params.InstanceType}"
        instance_count="${params.Instance_count}"
    }
    agent {
        label 'master'
    }
    stages {
        stage('Cloning') {
            steps {
                script {
                    gitclone()
                    echo "Parameters received on pipeline is : "
                    echo "Action : ${env.Action}"
                    echo "Name: ${env.Name}"
                    echo "Storage size: ${env.storage_size}"
                    echo "instance type: ${env.InstanceType}"
                    echo "instance count : ${env.Instance_count}"
                    echo "cni type : ${env.cniType} ${restDNS} ${restIP} ${restPORT}"
                }
            }
        }
        stage('Action on Private Instance') {
            steps {
                script {
                    def amiId = 'ami-0261755bbcb8c4a84'
                    ws("/var/lib/jenkins/workspace/${JOB_NAME}/cluster") {
                        path=sh(script:'pwd', returnStdout: true).trim()
                        def fileCount = sh(script: 'ls -la | wc -l', returnStdout: true).trim()
                        echo "File count: $fileCount"
                        if (fileCount.toInteger() == 3) {
                            gitclone()
                            sh 'cp -r cluster/* ./'
                            cluster_create()
                            sh "terraform apply -no-color -var Name=${name} -var Owner=${Owner} -var mailID=${mailID} -var storage_size=${Storage_size} -var InstanceType=${instanceType} -var amiId='${amiId}' -var key_name=${key_name} -var instance_count=${instance_count} --auto-approve"
                            instance()
                        }
                        else{
                            cluster_create()
                            sh "terraform apply -no-color -var Name=${name} -var Owner=${Owner} -var mailID=${mailID} -var storage_size=${Storage_size} -var InstanceType=${instanceType} -var amiId='${amiId}' -var key_name=${key_name} -var instance_count=${instance_count} --auto-approve"
                            instance()
                        }
                    }
                }
            }
        }
        stage('K8s installation using Ansible') {
            agent {
                label 'platformeng'
            }
            steps {
                script {
                    ws("/home/ubuntu/${JOB_NAME}/cluster_${BUILD_NUMBER}/") {
                        sh "echo ${params.cniType}"
                        sh "ansible-playbook K8s_Installation.yaml"
                        //sh "cat k8sInstallatiom.log"
                    }
                }
            }
        }
        stage('Storing Cluster info on Database'){
            steps{
                script{   
                    ws("/var/lib/jenkins/workspace/${JOB_NAME}/cluster"){
                        sh "scp -o StrictHostKeyChecking=no ubuntu@${PE_IP}:/home/ubuntu/${JOB_NAME}/cluster_${BUILD_NUMBER}/kubeconfig.yaml ./" 
                        def apiUrl = "${restIP}:${restPORT}/platform/v1/Database/saveCluster"
                        def workerIDs = sh(script: "cat clusterDetails", returnStdout: true).trim()
                        // Construct the curl command
                        def curlCommand = """
                            curl -X POST -H \"Host: ${restDNS}\" \\
                            -F \"kubeconfig=@./kubeconfig.yaml\" \\
                            -F \"clusterIDs=${workerIDs}\" \\
                            -F \"clusterName=${params.Name}\" \\
                            ${apiUrl}
                        """
                        sh(curlCommand)
                        //save instance details in database
                        sh "for i in \$(terraform output -json instance_IDs| tr -d '[]\"' | tr ',' ' '); do  LC_ALL=C.UTF-8 ansible-playbook postdata.yaml -e storage_size=${params.storage_size} -e restDNS=${restDNS} -e restIP=${restIP} -e restPORT=${restPORT} --extra-vars \"instance_IDs=['\$i']\"; done "
                    }
                }
            }
        }
    }
    post {
        failure {
            script {
                // Trigger another pipeline on failure
                build job: 'Destroy', parameters: [
                    [$class: 'StringParameterValue', name: 'Machine_Type', value: "Cluster"],
                    [$class: 'StringParameterValue', name: 'state_file_location', value: params.Name],
                    [$class: 'StringParameterValue', name: 'Nodes', value: "Complete_Cluster"]
                ]
            }
        }
    }
}


def gitclone(){
    sh 'rm -rf * .git'
    git branch: 'Sprint-1', credentialsId: 'gitlab', url: 'http://10.63.32.87/platformengineering/devops'
}

def cluster_create(){
    sh """
        sed -i "s/xyz1/${params.Name}/g" backend.tf
        sed -i "s/Cluster_SG/cluster_SG_\${BUILD_NUMBER}/g" securityGroup.tf
    """
    sh "terraform init -no-color -backend=true"
    sh " ls"
}
def instance(){
    sh "sleep 120"
    // Create an Ansible inventory file
    sh "rm -rf clusterDetails 2> /dev/null && touch clusterDetails"
    sh "rm -rf myinventory 2> /dev/null && touch myinventory"
    sh 'echo "[master]" >> myinventory '
    sh "echo -n 'master ansible_host='  >> myinventory"
    sh "terraform output -no-color -json instance_private_ip | tr -d '[]\"' | tr ',' '\n' | head -1 >> myinventory"
    sh 'echo "[worker]" >> myinventory'
    sh "terraform output -no-color -json instance_private_ip | tr -d '[]\"' | tr ',' '\n' | tail -n +2 >> myinventory"
    def workerIDs = sh(script: "terraform output -json instance_IDs", returnStdout: true).trim()
    // Copy the inventory file to the remote server
    sh "echo ${workerIDs} >> clusterDetails"
    sh "ssh -o StrictHostKeyChecking=no ubuntu@${PE_IP} -- 'mkdir -p ~/${JOB_NAME}/cluster_${BUILD_NUMBER}'"
    sh "scp -o StrictHostKeyChecking=no clusterDetails ubuntu@${PE_IP}:~/${JOB_NAME}/cluster_${BUILD_NUMBER}/"
    sh "scp -o StrictHostKeyChecking=no myinventory ubuntu@${PE_IP}:~/${JOB_NAME}/cluster_${BUILD_NUMBER}/"
    sh "scp -o StrictHostKeyChecking=no K8s_Installation.yaml ubuntu@${PE_IP}:~/${JOB_NAME}/cluster_${BUILD_NUMBER}/"
    sh "scp -o StrictHostKeyChecking=no ansible.cfg ubuntu@${PE_IP}:~/${JOB_NAME}/cluster_${BUILD_NUMBER}/"
}