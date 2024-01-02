pipeline {
    agent {
        label 'kubernetes'
    }

    stages {
        stage('checkSCM') {
            steps {
                sh "sudo rm -rf * .git"
                git branch: "dockerConfig ", credentialsId: 'gitlab', url: 'http://10.63.32.87/platformengineering/devops'
            }
        }
        stage('Ansible-playboook'){
            steps{
                sh "bash dockerconfig.sh"
                sh "ansible-playbook restartDockerService.yaml"
            }
        }
    }
}
