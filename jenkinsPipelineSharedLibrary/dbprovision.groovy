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
                    git branch: "dbprovision", credentialsId: 'gitlab-microservices', url: 'http://10.63.32.87/platformengineering/microservices'
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
                    // echo "Building Docker image..."
                    // docImage = "${JOB_NAME}".toLowerCase()
                    // imageName = "${DOCKER_REGISTRY}/${JOB_NAME}:${BUILD_NUMBER}".toLowerCase()
                    // k8sImage = "${JOB_NAME}:${BUILD_NUMBER}"
                    // withCredentials([usernamePassword(credentialsId: 'dockerHubPrivateRepo', passwordVariable: 'DOCKER_PASSWD', usernameVariable: 'DOCKER_USER')]) {
                    //     sh "docker build -t ${imageName} ."
                    //     sh "docker login ${DOCKER_REGISTRY} -u ${DOCKER_USER} -p ${DOCKER_PASSWD}"
                    //     sh "docker push ${imageName}"
                    // }
                    // cleanWs()
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
                        sh "helm install dbprovision dbprovision --set dbprovision.dbprovisionContainer.image.imageName=${docImage} --set dbprovision.dbprovisionContainer.image.repository=${DOCKER_REGISTRY} --set dbprovision.dbprovisionContainer.image.tag=${BUILD_NUMBER} --set dbprovision.namespace=${NAMESPACE} --set dbprovision.dbprovisionContainer.imagePullSecrets=regcred" 
                    }
                }
            }
        }
    }
}

