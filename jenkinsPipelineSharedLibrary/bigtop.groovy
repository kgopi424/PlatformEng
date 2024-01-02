pipeline {
    agent {
        label 'master'
    }
    parameters {
        string(name: 'Name', description: 'Name of the Cluster')
        choice(name: 'Action', choices: ['install','uninstall'], description: 'Select the Action to Perform')
        choice(name: 'platform', choices: ['docker','kubernetes'], description: 'Select the OS')
        choice(name: 'NumOfNodes', choices: ['1','2','3','4'], description: 'Select the Number of Nodes')
        choice(name: 'OS', choices: ['ubuntu','debian'], description: 'Select the OS')
        choice(name: 'Tool', choices: ['hdfs','hdfs_hbase','hdfs_yarn_spark','hdfs_spark-standalone','hdfs_yarn_spark_hive','hdfs_yarn_hive_pig'], description: 'Select the Tool')
    }
    stages {
        stage('checkoutSCM'){
            steps{
                script{
                    if(params.Action == 'install'){
                        if(params.platform == "kubernetes" ){
                        cleanWs()
                        git branch: 'helm-tools', credentialsId: 'gitlab', url: 'http://10.63.32.87/platformengineering/devops/'
                        echo "done"
                        }
                        else{
                            git branch: 'branch-3.2', url: 'https://github.com/apache/bigtop'
                        }
                    }else{
                        echo "No need to Clone for Uninstall"
                    }
                }
            }
        }
        stage('Action on Bigtop in Cluster') {
            steps {
                script {
                    if (params.platform == "kubernetes"){
                        def os = params.OS
                        def tool = params.Tool
                        def name = params.Name
                        def toolName = sh(script: "echo ${params.Tool} | sed 's/_/-/g'", returnStdout: true).trim()
                        // Output selected parameters
                        echo "Selected OS: ${os}"
                        echo "Selected Tool: ${tool}"
                        
                        // Perform actions based on user input
                        if (os == 'ubuntu' ) {
                            echo "Performing actions for Ubuntu with ${tool}"
                            if(params.Action == "uninstall"){
                                sh "helm uninstall ${name}-${toolName}"
                            }
                            else{
                            sh "helm install ${name}-${toolName} bigtop/ --set image.tag=1.2.1-ubuntu-16.04-${tool} --set replicaCount=${params.NumOfNodes}"}
                        } else if (os == 'debian') {
                            echo "Performing actions for Debian with ${tool}"
                            if(params.Action == "uninstall"){
                                sh "helm un ${name}-${toolName}"
                            }
                            else{
                            sh "helm install ${os}-${toolName} bigtop/ --set image.tag=debian-8_${tool} --set replicaCount=${params.NumOfNodes}"}
                        } else {
                            echo "Unsupported combination: ${os} with ${tool}"
                        }
                    }
                    else{
                        dir('provisioner/docker/'){
                            if(params.Action == "install"){
                                if (os == 'ubuntu' ) {
                                    sh "sudo ./docker-hadoop.sh -C config_ubuntu-20.04.yaml -c ${params.NumOfNodes}"
                                    sh "sudo ./docker-hadoop.sh --exec 1 bash -c 'echo \"export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64\" >> /root/.bashrc'"
                                    //sh "sleep 10 && sudo ./docker-hadoop.sh --exec 1 bash -c \"export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64 && sleep 10 && hdfs --daemon start datanode && sleep 10 && hdfs --daemon start namenode && sleep 10  && hadoop fs -mkdir /mydirectory\" "
                                }else if (os == 'debian') {
                                    sh "sudo ./docker-hadoop.sh -C config_debian-11.yaml -c ${params.NumOfNodes}"
                                    sh "sudo ./docker-hadoop.sh --exec 1 bash -c 'echo \"export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64\" >> /root/.bashrc'"
                                    // sh "sleep 10 && sudo ./docker-hadoop.sh --exec 1 bash -c \"export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64 && sleep 10 && hdfs --daemon start datanode && sleep 10 && hdfs --daemon start namenode && sleep 10  && hadoop fs -mkdir /mydirectory\" "
                                }else{
                                    echo "Unsupported combination: ${os} with ${params.platform}"
                                }
                            }else {
                                try {
                                    sh "sudo ./docker-hadoop.sh -d"
                                } catch (Exception e) {
                                    echo "Docker container is not running to remove"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
