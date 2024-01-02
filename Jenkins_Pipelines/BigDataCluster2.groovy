
pipeline {
    agent {
        label 'master'
    }
    parameters {
        choice(name: 'Action', choices: ['apply', 'delete'], description: 'Select the Action to apply on Cluster to install Tools')
        string(name: 'Deployments',description: 'Delete the Deployments ')
        string(name: 'PVC',description: 'Delete the Persistant Volume ')
        string(name: 'clusterName', description: 'Enter Cluster Name')
        choice(name: 'Ingestion', choices: ['kafka', 'flume', 'storm', 'sqoop'], description: 'Select Ingestion method')
        choice(name: 'Processing', choices: ['hue', 'spark', 'hbase', 'hive', 'zookeeper'], description: 'Select Processing tools')
        choice(name: 'Database', choices: ['cassandra', 'mongoDB', 'mysql', 'postgresql'], description: 'Select Database method')
        choice(name: 'Tools', choices: ['jupyter', 'grafana', 'pentaho', 'tableau'], description: 'Select Tools')
        string (name: 'ControllerIngestionSize',description: 'Enter the Volume Size in GB for IngestionTool')
        string (name: 'brokerIngestionSize',description: 'Enter the Volume Size in GB for ProcessingTool')
        string (name: 'DatabaseSize',description: 'Enter the Volume Size in GB for DatabaseTool')
        string (name: 'ToolsSize',description: 'Enter the Volume Size in GB for otherTools')
    }
    stages {
        stage('Cloning the Deployment Files'){
            steps{
                script{
                    if (params.Action == "apply") {
                        sh "rm -rf * .git"
                        git branch: 'bigDataCluster', credentialsId: 'gitlab', url: 'http://10.63.32.87/platformengineering/devops/'
                        echo "done"
                    }
                    echo "done"
                }
            }
        }
        stage('Installing/Deleting Tools on Cluster') {
            steps {
                script {
                    if (params.Action == "apply") {
                        applyTools()
                    } else if (params.Action == "delete") {
                        if (params.PVC != null ) {
                            deleteDeployments(params.Deployments)
                            deletePVC(params.PVC)
                        }
                        else if (params.Deployments != null ){
                            deleteDeployments(params.Deployments)
                        }
                    }
                }
            }
        }
    }
}

def deletePVC(pvcs) {
    echo "Deleting PVC from the cluster: ${params.clusterName}"
    pvcs.split(',').each { pvc ->
        sh "kubectl delete pvc ${pvc}"
    }

}
def deleteDeployments(deploy) {
    echo "Deleting Deployments from the cluster: ${params.clusterName}"
    deploy.split(',').each { tool ->
        if (tool =~ /.*mysql.*/ || tool =~ /.*grafana.*/) {
            sh "kubectl delete deployment ${tool}"
            sh "kubectl delete svc ${tool}"           
        } else if (tool =~ /.*spark.*/ || tool =~ /.*kafka.*/) {
            sh "helm uninstall ${tool}"
        }
    }
}

def applyTools() {
    echo "Applying tools on the cluster: ${params.clusterName}"

    if (params.Ingestion) {
        installIngestionTool(params.Ingestion)
    }

    if (params.Processing) {
        installProcessingTool(params.Processing)
    }

    if (params.Database) {
        installDatabaseTool(params.Database)
    }

    if (params.Tools) {
        installOtherTool(params.Tools)
    }
}

def installIngestionTool(ingestionTool) {
    echo "Installing Ingestion Tool: ${ingestionTool}"
    switch (ingestionTool) {
        case 'kafka':
            def controllerPVCName = "efs-controller-${params.Ingestion}-${params.clusterName}" // Assuming you want to use params.Ingestion
            def brokerPVCName = "efs-broker-${params.Ingestion}-${params.clusterName}"
            echo "controllerPVC Name: ${controllerPVCName}"
            echo "brokerPVC Name: ${brokerPVCName}"
            sh "sed -i 's/efs-XYZ/${controllerPVCName}/g' kafka/controller-pvc.yaml"
            sh "sed -i 's/XGi/${params.ControllerIngestionSize}Gi/g' kafka/controller-pvc.yaml"
            sh "sed -i 's/efs-XYZ/${brokerPVCName}/g' kafka/broker-pvc.yaml"
            sh "sed -i 's/XGi/${params.ControllerIngestionSize}Gi/g' kafka/broker-pvc.yaml"
            sh "kubectl apply -f kafka/"
            sh "helm install ${params.clusterName}-kafka bitnami/kafka --set controller.persistence.existingClaim=${controllerPVCName} --set broker.persistence.existingClaim=${brokerPVCName} --set broker.replicaCount=1 --set controller.replicaCount=1"
        // Add cases for other ingestion tools if needed
        default:
            echo "Unknown Ingestion Tool: ${ingestionTool}. Need to update the packages."
    }
}

def installOtherTool(otherTool) {
    echo "Installing Other Tool: ${otherTool}"
    switch (otherTool) {
        case 'grafana':
            def pvcName = "efs-${params.Tools}-${params.clusterName}" // Assuming you want to use params.Ingestion
            echo "PVC Name: ${pvcName}"
            sh "sed -i 's/efs-XYZ/${pvcName}/g' grafana/pvc.yaml"
            sh "sed -i 's/XGi/${params.ToolsSize}Gi/g' grafana/pvc.yaml"
            sh "sed -i 's/pvcName/${pvcName}/g' grafana/grafana-deploy.yaml"
            sh "sed -i 's/XYZ/${params.clusterName}/g' grafana/grafana-deploy.yaml"
            sh "kubectl apply -f grafana/"
            break
        default:
            echo "Unknown Tool: ${otherTool}. Need to update the packages."
    }
}
def installDatabaseTool(databaseTool) {
    echo "Installing Database Tool: ${databaseTool}"
    switch (databaseTool) {
        case 'mysql':
            echo "${databaseTool}"
            def pvcName = "efs-${params.Database}-${params.clusterName}" // Assuming you want to use params.Ingestion
            echo "PVC Name: ${pvcName}"
            sh "sed -i 's/efs-XYZ/${pvcName}/g' mysql/pvc.yaml"
            sh "sed -i 's/XGi/${params.DatabaseSize}Gi/g' mysql/pvc.yaml"
            sh "sed -i 's/pvcName/${pvcName}/g' mysql/mysql-deploy.yaml"
            sh "sed -i 's/XYZ/${params.clusterName}/g' mysql/mysql-deploy.yaml"
            sh "kubectl apply -f mysql/"
            break
        // Add cases for other database tools if needed
        default:
            echo "Unknown Database Tool: ${databaseTool}. Need to update the packages."
    }
}

def installProcessingTool(processingTool) {
    echo "Installing Processing Tool: ${processingTool}"
    switch (processingTool) {
        case 'spark':
           echo "${processingTool}"
           sh "helm repo add bitnami https://charts.bitnami.com/bitnami && helm repo update"
           sh "helm install ${params.clusterName}-spark bitnami/spark --set enableWebhook=true"
            break
        default:
            echo "Unknown Processing Tool: ${processingTool}. Need to update the packages."
    }
}
