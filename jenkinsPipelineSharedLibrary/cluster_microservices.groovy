pipeline {
    agent {
        label 'master'
    }
    tools {
        maven 'maven'
    }
    parameters {
        string(name: 'Namespace', defaultValue: 'default', description: 'provide the namespace to deploy')
        choice(name: 'Microservices', choices: ['Yes','No'], description: 'Select if Microservices need to deploy')
        choice(name: 'Tools', choices: ['Yes','No'], description: 'Select if Tools need to deploy ')
        choice(name: 'gitlab_ms_branch', choices: ['cluster-provisioning', 'efk-logging', 'sandbox-env', 'dbprovision','secret-management', 'template-service', 'vuln-scanning', 'Git-api', 'Nexus-api', 'Pipeline-api', 'Sonarqube-api'], description: 'Select the gitlab branch to clone the data')
        choice(name: 'gitlab_tool_branch', choices:['efk'], description: 'Select the gitlab branch to clone the data')
    }
    environment {
        NAMESPACE = "${params.Namespace}"
    }
    stages {
        stage('REGISTRYIP'){
            steps{
                script{
                    cleanWs()
                    REGISTRYIP = sh(script: 'kubectl get nodes -o wide --no-headers | awk \'{print $6}\' | head -1', returnStdout: true).trim()
                    DOCKER_REGISTRY = "${REGISTRYIP}:32003"
                    echo "${DOCKER_REGISTRY}"
                    echo "${REGISTRYIP}"   
                }
            }
        }
        stage('Checkout') {
            steps {
                script {
                    if(params.Microservices == "Yes"){
                        ws("workspace/${JOB_NAME}/${BUILD_NUMBER}"){
                            if (['Git-api', 'Nexus-api', 'Pipeline-api', 'Sonarqube-api'].contains(params.gitlab_ms_branch)) {
                                gitcloneapi()
                                ws("workspace/${JOB_NAME}/${BUILD_NUMBER}/${params.gitlab_ms_branch}"){
                                    sh "ls"
                                }
                            }
                            else{
                                sh 'ls'
                            }
                        }
                    }
                    if(params.Tools == "Yes"){
                        ToolsGitClone()
                        sh 'ls'
                    }
                }
            }
        }
        stage('Build') {
            tools {
                maven 'mvn3.6.3'
            }
            steps {
                script {
                    if(params.Microservices == "Yes"){
                        ws("workspace/${JOB_NAME}/${BUILD_NUMBER}"){
                            if(['Git-api', 'Nexus-api', 'Pipeline-api', 'Sonarqube-api'].contains(params.gitlab_ms_branch)) {
                                ws("workspace/${JOB_NAME}/${BUILD_NUMBER}/${params.gitlab_ms_branch}"){
                                    sh "export gitlaburl=\"http://10.63.35.196:32471/\""
                                    sh "mvn clean install"
                                }
                            }
                            else if(["cluster-provisioning","secret-management", 'efk-logging', "dbprovision"].contains(params.gitlab_ms_branch)) {
                               gitClone()
                               sh "ls"
                               sh "mvn clean install"
                            }
                            else{
                                gitClone()
                            }
                        }
                    }
                }
            }
        }
        stage('Build Docker Image') {
            steps {
                script {
                    if(params.Microservices == "Yes"){
                        ws("workspace/${JOB_NAME}/${BUILD_NUMBER}"){
                            docImage = "${gitlab_ms_branch}".toLowerCase()
                            imageName = "${DOCKER_REGISTRY}/${gitlab_ms_branch}:${BUILD_NUMBER}".toLowerCase()
                            k8sImage = "${gitlab_ms_branch}:${BUILD_NUMBER}"
                            withCredentials([usernamePassword(credentialsId: 'dockerHubPrivateRepo', passwordVariable: 'DOCKER_PASSWD', usernameVariable: 'DOCKER_USER')]) {
                                if(['Git-api', 'Nexus-api', 'Pipeline-api', 'Sonarqube-api'].contains(params.gitlab_ms_branch)) {
                                    ws("workspace/${JOB_NAME}/${BUILD_NUMBER}/${params.gitlab_ms_branch}"){
                                    sh "docker build -t ${imageName} ."
                                    }
                                }
                                else{
                                    sh "docker build -t ${imageName} ."
                                }
                                sh "docker login ${DOCKER_REGISTRY} -u ${DOCKER_USER} -p ${DOCKER_PASSWD}"
                                sh "docker push ${imageName}"
                            }
                        }
                        cleanWs()
                    }
                }
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    if(params.Microservices == "Yes"){
                        ws("workspace/${JOB_NAME}/${BUILD_NUMBER}"){
                            createNamespaceAndSecret() 
                            lowercase= "${params.gitlab_ms_branch}".toLowerCase()
                            cleanWs()
                            gitClone1()
                            helmCharts("${lowercase}")
                        } 
                    }
                    if(params.Tools == "Yes"){
                        cleanWs()
                        ToolsGitClone()
                        sh "helm install efk ."
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
                          ws("workspace/${JOB_NAME}/${BUILD_NUMBER}") {
                            if (['efk-logging'].contains(params.gitlab_ms_branch)) {
                              gitEFKTest()
                              sh "mvn clean install"
                            }
                          }
                        } else {
                          echo "Service not yet available. Retrying..."
                          sleep(30) // Adjust the sleep interval as needed
                        }
                    }
                }
            }
        }

    }
}

def gitClone() {
    git branch: "${gitlab_ms_branch}", credentialsId: 'gitlab-microservices', url: 'http://10.63.32.87/platformengineering/microservices'
}
def gitEFKTest() {
    git branch: "efk-test", credentialsId: 'gitlab-microservices', url: 'http://10.63.32.87/platformengineering/microservices'
}
def ToolsGitClone(){
    git branch: "${gitlab_tool_branch}", credentialsId: 'gitlab', url: 'http://10.63.32.87/platformengineering/devops'
}
def gitcloneapi(){ //durga's api code to clone
    git branch: "platform-dev-env", credentialsId: 'gitlab-microservices', url: 'http://10.63.32.87/platformengineering/microservices'
}
def gitClone1() {
    git branch: "helm-microservices", credentialsId: 'gitlab', url: 'http://10.63.32.87/platformengineering/devops'
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

def deploymentAndService(){
    sh "sed -i 's|IMAGE|${imageName}|g' deploy.yaml"
    sh "sed -i 's|SECRET|regcred|g' deploy.yaml"
    sh "sed -i 's|NAMESPACE|${NAMESPACE}|g' *.yaml"
    sh "kubectl apply -f config.yaml" 
    sh "kubectl apply -f deploy.yaml"
    sh "kubectl apply -f svc.yaml"          
}
def helmCharts(name){
    if(name == "cluster-provisioning"){
        sh "helm install cluster-provisioning-ms ${name} --set cluster.clusterContainer.image.imageName=${docImage} --set cluster.clusterContainer.image.repository=${DOCKER_REGISTRY} --set cluster.clusterContainer.image.tag=${BUILD_NUMBER} --set cluster.namespace=${NAMESPACE} --set cluster.clusterContainer.imagePullSecrets=regcred" 
    }
    else if(name == "sandbox-env"){
       sh "helm install sandbox-env ${name} --set deployment.sandbox.image.imageName=${docImage} --set deployment.sandbox.image.repository=${DOCKER_REGISTRY} --set deployment.sandbox.image.tag=${BUILD_NUMBER} --set deployment.namespace=${NAMESPACE} --set deployment.sandbox.imagePullSecrets=regcred" 
    }
    else if(name == "dbprovision"){
        sh "helm install dbprovision ${name} --set dbprovision.dbprovisionContainer.image.imageName=${docImage} --set dbprovision.dbprovisionContainer.image.repository=${DOCKER_REGISTRY} --set dbprovision.dbprovisionContainer.image.tag=${BUILD_NUMBER} --set dbprovision.namespace=${NAMESPACE} --set dbprovision.dbprovisionContainer.imagePullSecrets=regcred" 
    }
    else if(name == "template-service"){
        sh "helm install template-service ${name} --set deployment.templateService.image.imageName=${docImage} --set deployment.templateService.image.repository=${DOCKER_REGISTRY} --set deployment.templateService.image.tag=${BUILD_NUMBER} --set deployment.namespace=${NAMESPACE} --set deployment.templateService.imagePullSecrets=regcred" 
    }
    else if(name == "vuln-scanning"){
        sh "helm install vuln-scanning ${name} --set scanning.scanningContainer.image.imageName=${docImage} --set scanning.scanningContainer.image.repository=${DOCKER_REGISTRY} --set scanning.scanningContainer.image.tag=${BUILD_NUMBER} --set scanning.namespace=${NAMESPACE} --set scanning.scanningContainer.imagePullSecrets=regcred" 
    }
    else if(name == "Git-api".toLowerCase()){
        sh "helm install gitlab-api ${name} --set dep.gitlab.image.imageName=${docImage} --set dep.gitlab.image.repository=${DOCKER_REGISTRY} --set dep.gitlab.image.tag=${BUILD_NUMBER} --set dep.namespace=${NAMESPACE} --set dep.gitlab.imagePullSecrets=regcred" 
    }
    else if(name == "Sonarqube-api".toLowerCase()){
        sh "helm install sonarqube-api ${name} --set qubeapisDep.sonarqube.image.imageName=${docImage} --set qubeapisDep.sonarqube.image.repository=${DOCKER_REGISTRY} --set qubeapisDep.sonarqube.image.tag=${BUILD_NUMBER} --set qubeapisDep.namespace=${NAMESPACE} --set qubeapisDep.sonarqube.imagePullSecrets=regcred" 
    }
    else if(name == "Nexus-api".toLowerCase()){
        sh "helm install nexus-api ${name} --set apisDep.nexus.image.imageName=${docImage} --set apisDep.nexus.image.repository=${DOCKER_REGISTRY} --set apisDep.nexus.image.tag=${BUILD_NUMBER} --set apisDep.namespace=${NAMESPACE} --set apisDep.nexus.imagePullSecrets=regcred" 
    }
    else if(name == "Pipeline-api".toLowerCase()){
        sh "helm install pipeline-api ${name} --set apisDep.pipeline.image.imageName=${docImage} --set apisDep.pipeline.image.repository=${DOCKER_REGISTRY} --set apisDep.pipeline.image.tag=${BUILD_NUMBER} --set apisDep.namespace=${NAMESPACE} --set apisDep.pipeline.imagePullSecrets=regcred" 
    }
    else if(name == "secret-management".toLowerCase()){
        sh "helm install secret-management ${name} --set application.vaultApplication.image.imageName=${docImage} --set application.vaultApplication.image.repository=${DOCKER_REGISTRY} --set application.vaultApplication.image.tag=${BUILD_NUMBER} --set application.namespace=${NAMESPACE} --set application.vaultApplication.imagePullSecrets=regcred" 
    }
    else if(name == "efk-logging".toLowerCase()){
        sh "helm install efk-ms efk-ms --set app.efkApp.image.imageName=${docImage} --set app.efkApp.image.repository=${DOCKER_REGISTRY} --set app.efkApp.image.tag=${BUILD_NUMBER} --set app.namespace=${NAMESPACE} --set app.efkApp.imagePullSecrets=regcred" 
    }
}
