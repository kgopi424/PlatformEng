
pipeline {
    parameters {
        choice(name: 'Action', choices: ['apply','destroy'], description: 'apply will create a resource, destroy will delete')
        choice(name: 'environment', choices: ['stag', 'dev', 'prod'], description: 'Environment for the infrastructure')
        string(name: 'business_divsion', defaultValue: 'PlatformEngineering', description: 'Business divsion')
    }
    agent {
        label 'master'
    }
    stages {
        stage('Cloning') {
            steps {
                script {
                    gitclone()
                }
            }
        }
        stage('Infra Creation') {
            steps {
                script {
                    if (params.Action == 'apply'){
                        ws("/var/lib/jenkins/workspace/${JOB_NAME}/vpc") {
                            sh "terraform init -backend=true"
                            sh "terraform apply -no-color -var environment=${params.environment} -var business_divsion=${params.business_divsion} --auto-approve"
                        }
                    }
                    else{
                        ws("/var/lib/jenkins/workspace/${JOB_NAME}/vpc") {
                            sh 'terraform destroy -no-color --auto-approve'
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