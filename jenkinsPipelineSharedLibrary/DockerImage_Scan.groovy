pipeline {
    parameters {
        string(name: 'name', defaultValue: 'test', description: 'name of the vuln report')
        string(name: 'image', defaultValue: 'ubuntu', description: 'name of the docker image')
        string(name: 'tag', defaultValue: 'latest', description: 'tag of the docker image')
        string(name: 'hostname', defaultValue: 'scan.example.com', description: 'Host name for gatway')
        string(name: 'platform_rest_ip', defaultValue: '0.0.0.0', description: 'platformEngg rest server ip')
        string(name: 'platform_rest_port', defaultValue: '8080', description: 'platformEngg rest server port')
    } 
    environment {
        Owner = 'Jenkins'
        mailID = 'jenkins@gmail.com'
        key_name = 'platformEng'
        jenkins_url = "http://10.63.20.41:8080"
        restIP = "${params.platform_rest_ip}"
        restPORT = "${params.platform_rest_port}"
        trivy_server_url = "http://10.63.20.69:32007"
        trivy_server_token = "YWRtaW46YWRtaW4K"
        registry_url = "http://10.63.20.43:32003/"
        registry_user = "admin"
        registry_pass = "admin"
    }
    agent {
        label 'master'
    }
    stages {
        stage('Generate Trivy vulnerability Report') {
            steps {
                script {
                    ws("workspace/${JOB_NAME}/${BUILD_NUMBER}"){
                        
                        sh "sudo docker login ${registry_url} -u ${registry_user} -p ${registry_pass}"
                        
                        sh "sudo apt-get install pandoc -y"
                        
                        sh "sudo docker pull ${image}:${tag}"
                        
                        sh """
                            sudo docker run --rm --privileged \
                                --network host -v /var/run/docker.sock:/var/run/docker.sock \
                                aquasec/trivy:latest image --scanners vuln ${image}:${tag} \
                                --server ${trivy_server_url} --token ${trivy_server_token} > report.txt
                        """
                        
                        sh "cat report.txt"
                        
                        sh "sudo pandoc report.txt -o vulnerability_report.pdf --pdf-engine=xelatex 2>/dev/null"
                        archiveArtifacts artifacts: 'vulnerability_report.pdf' 
                    }
                    //sh "sudo rm -rf ./${BUILD_NUMBER}*"
                }
            }
        }
        stage('Save Report') {
            steps {
                script {
                    ws("workspace/${JOB_NAME}/${BUILD_NUMBER}"){
                        
                        def apiUrl = "http://${restIP}:${restPORT}/platform/v1/database/saveReport"
                        def downloadUrl = "${BUILD_URL}artifact/vulnerability_report.pdf"

                        def curlCommand = """
                            curl -X POST \
                            -H "Content-Type: application/json" \
                            -H "Host: ${params.hostname}" \
                            -d '{ \
                                "name": "${params.name}", \
                                "image": "${params.image}", \
                                "tag": "${params.tag}", \
                                "url": "${downloadUrl}" \
                            }' \
                            ${apiUrl}
                        """
                        
                        sh(curlCommand)
                    }
                }
            }
        }
    }
}
