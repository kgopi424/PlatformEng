pipeline {
    agent {
        label 'master'
    }
    parameters {
        choice(name: 'Action', choices: ['Install', 'Uninstall'], description: 'Select the Action to Perform the operation')
        // string(name: 'tools', description: "provide the tools separated by comma")
    }
    environment {
        NAMESPACE = 'testings'
    }
    stages {
        stage('Action on Deployment') {
            steps {
                script {
                    if (params.Action == "Install") {
                        params.tools.split(',').each { tool ->
                            switch (tool.trim().toLowerCase()) {
                                case "mysql":
                                    sh "helm install mysql wso2/mysql --set persistence.storageClass=ebs-sc-1b --set mysqlRootPassword=secretpassword"
                                    break
                                case "postgresql":
                                    sh "helm install postgresql bitnami/postgresql --set primary.persistence.storageClass=ebs-sc-1b"
                                    break
                                case "grafana":
                                    sh "helm install grafana grafana/grafana --set service.type=NodePort --set service.nodePort=32233 --set persistence.enabled=true --set persistence.storageClassName=ebs-sc-1b --set service.type=NodePort"
                                    break
                                case "kafka":
                                    sh "helm install kafka bitnami/kafka --set controller.persistence.storageClass=ebs-sc-1b --set broker.persistence.storageClass=ebs-sc-1b"
                                    break
                                case "trivy":
                                    sh "helm install trivy aquasecurity/trivy"
                                    break
                                case "spark":
                                    sh "helm install spark bitnami/spark --set enableWebhook=true"
                                    break
                                case "sonarqube":
                                    sh "helm install sonarqube sonarqube/sonarqube --set persistence.enabled=true --set persistence.storageClass=ebs-sc-1b --set service.type=NodePort"
                                    break
                                case "nexus":
                                    sh "helm install nexus stevehipwell/nexus3 --set persistence.enabled=true --set persistence.storageClass=ebs-sc-1b --set service.type=NodePort"
                                    break
                                default:
                                    error "Tool is not yet listed, please utilize the listed tools"
                            }
                        }
                    } else {
                        params.tools.split(',').each { tool ->
                            sh "helm uninstall ${tool.trim().toLowerCase()}"
                        }
                    }
                }
            }
        }
    }
}



==============================================================================


pipeline {
    agent {
        label 'master'
    }
    environment {
        NAMESPACE = 'testings'  
    }
    parameters {
        choice(name: 'Action', choices: ['Install', 'Uninstall'], description: 'Select Action')
    }
    stages {
        stage('Helm Setup') {
            steps {
                script {
                    // Add repos if missing
                    addRepoIfMissing('wso2', 'https://helm.wso2.com')
                    addRepoIfMissing('bitnami', 'https://charts.bitnami.com/bitnami')
                    addRepoIfMissing('grafana', 'https://grafana.github.io/helm-charts')
                    addRepoIfMissing('aquasecurity','https://aquasecurity.github.io/helm-charts')
                    addRepoIfMissing('sonarqube','https://SonarSource.github.io/helm-chart-sonarqube')
                    addRepoIfMissing('stevehipwell','https://stevehipwell.github.io/helm-charts/')
                }
            }
        }

    stage('Deploy Tools') {
        steps {
            script {
                if (params.Action == 'Install') {
                    params.Tools.split(',').each { tool -> 
                        switch (tool.trim().toLowerCase()) {
                            case "mysql":
                                sh "helm install mysql wso2/mysql --set persistence.storageClass=ebs-sc-1b --set mysqlRootPassword=secretpassword"
                                break
                            case "efk":
                                git branch: "efk", credentialsId: 'gitlab', url: 'http://10.63.32.87/platformengineering/devops'
                                sh "helm install efk ."
                            case "postgresql":
                                sh "helm install postgresql bitnami/postgresql --set primary.persistence.storageClass=ebs-sc-1b"
                                break
                            case "grafana":
                                sh "helm install grafana grafana/grafana --set service.type=NodePort --set service.nodePort=32233 --set persistence.enabled=true --set persistence.storageClassName=ebs-sc-1b --set service.type=NodePort"
                                break
                            case "kafka":
                                sh "helm install kafka bitnami/kafka --set controller.persistence.storageClass=ebs-sc-1b --set broker.persistence.storageClass=ebs-sc-1b"
                                break
                            case "trivy":
                                sh "helm install trivy aquasecurity/trivy"
                                break
                            case "spark":
                                sh "helm install spark bitnami/spark --set enableWebhook=true"
                                break
                            case "sonarqube":
                                sh "helm install sonarqube sonarqube/sonarqube --set persistence.enabled=true --set persistence.storageClass=ebs-sc-1b --set service.type=NodePort"
                                break
                            case "nexus":
                                sh "helm install nexus stevehipwell/nexus3 --set persistence.enabled=true --set persistence.storageClass=ebs-sc-1b --set service.type=NodePort"
                                break
                            default:
                                error "Tool is not yet listed, please utilize the listed tools"
                            }
                        }
                    } else {
                        params.tools.split(',').each { tool ->
                            sh "helm uninstall ${tool.trim().toLowerCase()}"
                        }
                    }
                }  
            }
        }
   }
}

// Define function to add repo if missing
def addRepoIfMissing(String name, String url) {
    sh "helm repo add ${name} ${url}"
}