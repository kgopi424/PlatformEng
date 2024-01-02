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
                    git branch: "efk-logging", credentialsId: 'gitlab-microservices', url: 'http://10.63.32.87/platformengineering/microservices'
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
                        sh "helm install efk-ms efk-ms --set cluster.clusterContainer.image.imageName=${docImage} --set cluster.clusterContainer.image.repository=${DOCKER_REGISTRY} --set cluster.clusterContainer.image.tag=${BUILD_NUMBER} --set cluster.namespace=${NAMESPACE} --set cluster.clusterContainer.imagePullSecrets=regcred"
                    }
                }
            }
        }
        stage("EFK Test") {
            steps {
                script {
                    boolean serviceAvailable = false
                    echo "Waiting for the service to be available..."
                    while (!serviceAvailable) {
                        def curlOutput = sh(script: 'curl --location http://aa7eabfa05ed24fc3ad6d2c4007e805c-1204085443.us-east-1.elb.amazonaws.com/efk/available --header "host: efk.example.com"', returnStdout: true).trim()
                        if (curlOutput == 'available') {
                            echo "Service is available!"
                            serviceAvailable = true
                            cleanWs()
                            git branch: "efk-test", credentialsId: 'gitlab-microservices', url: 'http://10.63.32.87/platformengineering/microservices'                          
                            sh "mvn clean install"
                        }
                        else {
                            echo "Service not yet available. Retrying..."
                            sleep(30) // Adjust the sleep interval as needed
                        }
                    }
                }
            }
        }
    }
}
