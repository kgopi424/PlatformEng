@Library("microservices") _
def docImage
def k8sImage
def NAMESPACE
pipeline {
    agent {
        label 'master'
    }
    environment {
        NAMESPACE = "default"
    }
    stages {
        stage('REGISTRYIP') {
            steps {
                script {
                    cleanWs()
                    test()
                    REGISTRYIP = sh(script: 'kubectl get nodes -o wide --no-headers | awk \'{print $6}\' | head -1', returnStdout: true).trim()
                    DOCKER_REGISTRY = "${REGISTRYIP}:32003"
                    echo "Docker Registry: ${DOCKER_REGISTRY}"
                    echo "Registry IP: ${REGISTRYIP}"
                }
            }
        }
        stage('Checkout') {
            steps {
                script {
                    git branch: "redis-caching", credentialsId: 'gitlab-microservices', url: 'http://10.63.32.87/platformengineering/microservices'
                }
            }
        }
        stage('Build') {
            tools {
                maven 'mvn3.6.3'
            }
            steps {
                script {
                    echo "Building Maven project..."
                    sh "mvn clean install"
                }
            }
        }
        stage('Build Docker Image') {
            steps {
                script {
                    dockerBuildPush("${DOCKER_REGISTRY}")
                }
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    ws("workspace/${JOB_NAME}/${BUILD_NUMBER}") {
                        createNamespaceAndSecret(DOCKER_REGISTRY)
                        cleanWs()
                        git branch: "helm-microservices", credentialsId: 'gitlab', url: 'http://10.63.32.87/platformengineering/devops'
                        sh "helm install redis-caching webapp/ --set image.repository=${DOCKER_REGISTRY} --set image.imageName=${docImage} --set image.imageTag=${BUILD_NUMBER} --set service.type=NodePort --set namespace=${NAMESPACE}"
                    }
                }
            }
        }
    }
}

