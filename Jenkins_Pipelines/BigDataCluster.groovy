pipeline {
    agent {
        label 'kubernetes'
    }
    parameters {
        string(name: 'clusterName', description: 'Select Tools')
        choice(name: 'Ingestion', choices: ['Kafka', 'Flume', 'Storm', 'Sqoop'], description: 'Select Ingestion method')
        choice(name: 'Processing', choices: ['Hue', 'Spark', 'Hbase', 'Hive', 'Zookeeper'], description: 'Select Processing tools')
        choice(name: 'Database', choices: ['Cassandra', 'MongoDB', 'MySQL', 'PostgreSQL'], description: 'Select Database method')
        choice(name: 'Tools', choices: ['Jupyter', 'Grafana', 'Pentaho', 'Tableau'], description: 'Select Tools')
    }
    stages {
        stage('Bitnami package') {
            steps {
                script {
                    bitnami()
                }
            }
        }
        stage('Installation of Ingestion Tools') {
            when {
                expression {
                    params.Ingestion != null
                }
            }
            steps {
                script {
                    if (params.Ingestion == 'Kafka') {
                        // sh "helm un ${params.clusterName}-kafka"
                        sh "helm install ${params.clusterName}-kafka bitnami/kafka"
                    } else {
                        echo 'Need to update the packages'
                    }
                }
            }
        }
        stage('Installation of Processing Tools') {
            when {
                expression {
                    params.Processing != null
                }
            }
            steps {
                script {
                    if (params.Processing == 'Spark') {
                        // sh "helm un ${params.clusterName}-spark"
                        sh "helm install ${params.clusterName}-spark bitnami/spark --set enableWebhook=true"
                    } else {
                        echo 'Need to update the packages'
                    }
                }
            }
        }
        stage('Installation of Database Tools') {
            when {
                expression {
                    params.Database != null
                }
            }
            steps {
                script {
                    if (params.Database == 'MySQL') {
                        // sh "helm un ${params.clusterName}-mysql"
                        sh "helm install ${params.clusterName}-mysql stable/mysql --set persistence.existingClaim=mysqlpvc-pv3 --set mysqlRootPassword=root"
                    } else {
                        echo 'Need to update the packages'
                    }
                }
            }
        }
        stage('Installation of Tools') {
            when {
                expression {
                    params.Tools != null
                }
            }
            steps {
                script {
                    if (params.Tools == 'Grafana') {
                        // sh "helm un ${params.clusterName}-grafana"
                        sh "helm install ${params.clusterName}-grafana grafana/grafana --set service.type=NodePort --set service.nodePort=32233"
                    } else {
                        echo 'Need to update the packages'
                    }
                }
            }
        }
    }
}

def bitnami() {
    sh 'helm repo add bitnami https://charts.bitnami.com/bitnami && helm repo update'
}