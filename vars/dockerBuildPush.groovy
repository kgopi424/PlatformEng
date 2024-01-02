def call(DOCKER_REGISTRY){
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
