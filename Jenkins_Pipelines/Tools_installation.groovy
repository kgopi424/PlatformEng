pipeline {
    parameters {
        string(name: 'Instance_IP', description: 'Instance IP to install the required package')
        // choice(name: 'Tool', choices: ['Ansible', 'Gerrit', 'GitLab', 'Grafana', 'Java', 'Jenkins', 'Kafka', 'Projectlibre', 'MySQL', 'PostgreSQL', 'Python3', 'MongoDB', 'Prometheus'], description: 'Select the required Tool to install')
    } 
    agent {
	label 'master'
    }
    environment {
        PE_IP = '34.207.99.30'
    }
    stages {
        stage('Cloning') {
            steps {
                gitclone()
                echo "${BUILD_URL}"
            }
        }
        stage('Creating Inventory File'){
            steps {
                script {
                    ws("/var/lib/jenkins/workspace/${JOB_NAME}/Playbooks/") {
                        sh "rm -rf myinventory 2> /dev/null && touch myinventory"
                        params.Instance_IP.split(',').each { instance ->
                            sh "echo ${instance} >> myinventory"
                        }
                        // Copy the inventory file to the remote server
                        sh "scp -o StrictHostKeyChecking=no myinventory ubuntu@${PE_IP}:~/ansible_playbooks/"
                        // Copy Ansible configuration and playbook to the remote server
                        sh "scp -o StrictHostKeyChecking=no ansible.cfg ubuntu@${PE_IP}:~/ansible_playbooks/"
                        sh "scp -o StrictHostKeyChecking=no UpdateCache.yaml ubuntu@${PE_IP}:~/ansible_playbooks/"
                        params.Tool.split(',').each { tool ->
                            sh "scp -o StrictHostKeyChecking=no ${tool}.yaml ubuntu@${PE_IP}:~/ansible_playbooks/"
                        }
                    }
                    cleanWs()
                }
            }
        }
        stage('Installing required Package Using Ansible') {
            agent {
                label 'platformeng'
            }
            steps {
                script {
                    ws('/home/ubuntu/ansible_playbooks') {
                        sh 'ansible-playbook UpdateCache.yaml'
                        params.Tool.split(',').each { tool ->
                            sh "ansible-playbook ${tool}.yaml"
                        }
                    }
                    cleanWs()
                }
            }
        }
    }

    // post {
    //     failure {
    //         build job: 'Suggestions', propagate: false , parameters: [string(name: 'G_BUILD_NUMBER', value: env.BUILD_NUMBER), string(name: 'G_BUILD_URL', value: env.BUILD_URL)]
    //     }
    // }
}
def gitclone(){
    sh 'rm -rf * .git'
    git branch: 'Sprint-1', credentialsId: 'gitlab', url: 'http://10.63.32.87/platformengineering/devops'
}

// jenkins@ip-10-63-20-41:~$ cat Playbooks.sh
// #! /bin/bash/
// curl -s https://api.github.com/repos/10.63.32.87/contents/Playbooks | grep "name" | cut -d '"' -f 4 | grep -v -e "UpdateCache.yaml" -e "ansible.cfg" | sed 's/.yaml//g'
