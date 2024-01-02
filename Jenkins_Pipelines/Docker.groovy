pipeline {
    agent {
        label 'master'
    }

    stages {
        stage('cloning') {
            steps {
                sh "rm -rf * .git .gitignore"
                sh "git clone https://github.com/DaggupatiPavan/java-web-app-docker.git ./"
                sh 'ls'
            }
        }
        stage('Building'){
            steps{
                script{
                        sh "mvn -B clean package"
                }
            }
        }
        stage('Docker Image Building'){
            steps{
                script{
                    sh "sudo docker rmi -f webconsole"
                    sh "sudo docker build -t webconsole ."
                }
            }
        }
        stage('Docker Conatiner creation'){
            steps{
                script{
                    // sh "sudo docker rm -f webconsole"
                    sh "sudo docker run -itd -p 9000:8080 --name webconsole webconsole:latest"
                }
            }
        }
    }
    
    post {
        failure {
            build job: 'Suggestions', propagate: false , parameters: [string(name: 'G_BUILD_NUMBER', value: env.BUILD_NUMBER), string(name: 'G_BUILD_URL', value: env.BUILD_URL)]
        }
    }

}

