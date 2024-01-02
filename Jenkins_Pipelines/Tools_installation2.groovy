pipeline {
    parameters {
        string(name: 'Instance_IP', description: 'Instance IP to install the required package')
        // choice(name: 'Tool', choices: ['Ansible', 'Gerrit', 'GitLab', 'Grafana', 'Java', 'Jenkins', 'Kafka', 'Projectlibre', 'MySQL', 'PostgreSQL', 'Python3', 'MongoDB', 'Prometheus'], description: 'Select the required Tool to install')
    } 
    agent {
	label 'master'
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
                            sh "echo ${instance} ansible_user=ubuntu >> myinventory"
                        }
                    }
                }
            }
        }
        stage('Installing required Package Using Ansible') {
            steps {
                script {
                    ws("/var/lib/jenkins/workspace/${JOB_NAME}/Playbooks/") {
                        sh 'ansible-playbook UpdateCache.yaml'
                        params.Tool.split(',').each { tool ->
                            sh "ansible-playbook ${tool}.yaml"
                        }
                    }
                    
                }
            }
        }
    }
}
def gitclone(){
    cleanWs()
    git branch: 'Sprint-3', credentialsId: 'gitlab', url: 'http://10.63.32.87/platformengineering/devops'
}