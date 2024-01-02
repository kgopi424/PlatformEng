pipeline {
    agent {
        label 'master'
    }
    tools {
        maven 'maven'
    }
    parameters {
        string(name: 'Git_URL', description: 'provide the git URL')
        // choice(name: 'GitLab_Creds', choices:['test'], description: 'provide the credentials')
        choice(name: 'Language', choices: ['java','python'], description: 'select the language to Build' )
        string(name: 'Docker_Image_Name', defaultValue: 'webapp', description: 'provide the Docker Image name')
        string(name: 'Docker_Image_Tag', defaultValue: 'latest', description: 'provide the Docker Image Tag')
    }
    environment{
        NAMESPACE="default"
    }
    stages {
        stage('REGISTRYIP'){
            agent{
                label 'kubernetes'
            }
            steps{
                script{
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
                    gitClone()
                }
            }
        }
        stage('Build') {
            steps {
                script {
                    if(params.Language == "java"){
                        sh "mvn clean package"
                    }
                }
            }
        }
        stage('Code Quality Analysis'){
            steps{
                script{
                    if(params.Language == "java"){
                        withSonarQubeEnv(credentialsId: 'sonarQube') {
                            sh "mvn clean verify sonar:sonar -Dsonar.projectKey=SampleWebApp -Dsonar.projectName='SampleWebApp'"
                        }
                    }
                    else{
                        sh "echo 'sonar.projectKey=python' > sonar-project.properties"
                        def scannerHome = tool 'sonar-scanner';
                        withSonarQubeEnv() {
                            sh "${scannerHome}/bin/sonar-scanner"
                        }
                    }
                }
            }
        }
        stage('Build Docker Image') {
            steps {
                script {
                    docImage = "${params.Docker_Image_Name}".toLowerCase()
                    imageName = "${DOCKER_REGISTRY}/${params.Docker_Image_Name}:${params.Docker_Image_Tag}".toLowerCase()
                    k8sImage = "${params.Docker_Image_Name}:${params.Docker_Image_Tag}"
                    withCredentials([usernamePassword(credentialsId: 'dockerHubPrivateRepo', passwordVariable: 'DOCKER_PASSWD', usernameVariable: 'DOCKER_USER')]) {
                        sh "docker build -t ${imageName} ."
                        sh "docker login ${DOCKER_REGISTRY} -u ${DOCKER_USER} -p ${DOCKER_PASSWD}"
                        sh "docker push ${imageName}"
                    }
                    cleanWs()
                }
            }
        }
        stage('Vulnerability Scan'){
            steps{
                script{
                    sh "grype docker:${imageName} >scan-report"
                    sh "cat scan-report"
                }
                cleanWs()
            }
        }
        stage('Deploy to Kubernetes') {
            agent {
                label 'kubernetes'
            }
            steps {
                script {
                    ws("workspace/${JOB_NAME}/${BUILD_NUMBER}"){
                        createNamespaceAndSecret()
                        gitClone2()
                        tag="${params.Docker_Image_Tag}".toLowerCase()
                        sh "helm install ${docImage} webapp/  --set image.repository=${DOCKER_REGISTRY} --set image.imageName=${docImage} --set image.imageTag=${tag} --set imagePullSecrets=regcred --set namespace=${NAMESPACE} --set service.port=8080"                    
                    } 
                    cleanWs()
                }
            }
        }
    }
}
def gitClone() {
    cleanWs()
    if (params.GitLab_Creds == 'None') {
        git url: "${params.Git_URL}"
    } else {
        git credentialsId: "${params.GitLab_Creds}", url: "${params.Git_URL}"
    }
}
def gitClone2(){
    cleanWs()
    git branch: 'Sprint-2', credentialsId: 'gitlab', url: 'http://10.63.32.87/platformengineering/devops'
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




// https://github.com/bennzhang/docker-demo-with-simple-python-app
// https://github.com/DaggupatiPavan/java-web-app-docker 