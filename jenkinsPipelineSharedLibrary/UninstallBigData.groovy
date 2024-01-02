pipeline {
    agent {
        label 'kubernetes'
    }
    parameters {
        string(name: 'clusterName', description: 'Select Cluster')
        string(name: 'Tool', description: 'Select Tools')
    }
    stages {
        stage('Uninstallation of Tools') {
           when {
                expression {
                    params.clusterName != null
                }
            }
            steps {
                script {
                    
                        uninstallPackage(params.clusterName,params.Tool)
                }
            }
        }
    }
}

def bitnami() {
    sh 'helm repo add bitnami https://charts.bitnami.com/bitnami && helm repo update'
}

def uninstallPackage(clusterName,tool) {
    sh "helm uninstall ${clusterName}-${tool}"
}
