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
                    git branch: "template-service", credentialsId: 'gitlab-microservices', url: 'http://10.63.32.87/platformengineering/microservices'
                }
            }
        }
        stage('Build Docker Image') {
            steps {
                script {
                    echo "Building Docker image..."
                    docImage = "${JOB_NAME}".toLowerCase()
                    imageName = "${DOCKER_REGISTRY}/${JOB_NAME}:${BUILD_NUMBER}".toLowerCase()
                    k8sImage = "${JOB_NAME}:${BUILD_NUMBER}"
                    withCredentials([usernamePassword(credentialsId: 'dockerHubPrivateRepo', passwordVariable: 'DOCKER_PASSWD', usernameVariable: 'DOCKER_USER')]) {
                        sh "docker build -t ${imageName} ."
                        sh "docker login ${DOCKER_REGISTRY} -u ${DOCKER_USER} -p ${DOCKER_PASSWD}"
                        sh "docker push ${imageName}"
                    }
                    cleanWs()
                }
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    ws("workspace/${JOB_NAME}/${BUILD_NUMBER}") {
                        createNamespaceAndSecret()
                        cleanWs()
                        git branch: "helm-microservices", credentialsId: 'gitlab', url: 'http://10.63.32.87/platformengineering/devops'
                        sh "helm install template-service template-service --set deployment.templateService.image.imageName=${docImage} --set deployment.templateService.image.repository=${DOCKER_REGISTRY} --set deployment.templateService.image.tag=${BUILD_NUMBER} --set deployment.namespace=${NAMESPACE} --set deployment.templateService.imagePullSecrets=regcred" 
                    }
                }
            }
        }
    }
}
def createNamespaceAndSecret() {
    def namespaceExists = sh(script: "kubectl get ns ${NAMESPACE}", returnStatus: true)
    def secretExists = sh(script: "kubectl get secret regcred -n ${NAMESPACE}", returnStatus: true)
    if (namespaceExists != 0) {
        sh "kubectl create ns ${NAMESPACE}"
        sh "kubectl label namespace ${NAMESPACE} istio-injection=enabled"
    } else {
        echo "The '${NAMESPACE}' namespace already exists, no need to create it."
    }
    if (secretExists != 0) {
        sh "kubectl create secret docker-registry regcred --docker-server=http://${DOCKER_REGISTRY} --docker-username=admin --docker-password=admin -n ${NAMESPACE}"
    } else {
        echo "The 'regcred' secret already exists, no need to create it."
    }
}