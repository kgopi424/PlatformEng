pipeline {
    agent {
        label 'master'
    }
    parameters {
        string(name: 'G_BUILD_NUMBER')
        string(name: 'G_BUILD_URL')
        string(name: 'G_JOB_NAME')
    }
    stages {
        stage('Retrieve Error Information') {
            steps {
                script {
                    // http://3.7.65.205:8080/job/SpgwAusfUdmUdr/6/consoleText
                    sh "rm -rf  error_check.txt"
                    echo "${G_JOB_NAME}"
                    // sh "curl -sL http://localhost:8080/job/${G_JOB_NAME}/${G_BUILD_NUMBER}/consoleText -o error_check.txt"
                    sh ' echo curl -u admin:admin "${G_BUILD_URL}"consoleText -o error_check.txt >test'
                    sh "sed -i 's/3.7.65.205/localhost/g' test"
                    sh 'bash test'
                }
            }
        }
        stage('Analyze Error') {
            steps {
                script {
                    def errorDescription = sh(script: 'cat error_check.txt | tail -200 | sgpt "find the solution and give command if error is their"', returnStdout: true).trim()
                    def errorSolution = sh(script: 'cat error_check.txt | tail -200 | sgpt -s "correct the code"', returnStdout: true).trim()
                    env.ERROR_DESCRIPTION = errorDescription
                    env.ERROR_SOLUTION = errorSolution
                    echo "Suggested Description: ${errorDescription}"
                    echo "Suggested Solution: ${errorSolution}"
                    // sh "cat  error_check.txt"
                    echo "Note: The suggested solution provided above is based on automated analysis and may require further verification and testing in your specific environment. Use it as a starting point for troubleshooting, but make sure to adapt it to your needs and test thoroughly."
                    env.Note = "The suggested solution provided above is based on automated analysis and may require further verification and testing in your specific environment. Use it as a starting point for troubleshooting, but make sure to adapt it to your needs and test thoroughly."
                }
            }
        }
    }
    post {
        success {
            mail bcc: '', body: "<b>Build Error</b><br>Project: ${G_JOB_NAME} <br>Build Number:${G_BUILD_NUMBER}<br>Build URL: ${G_BUILD_URL} <br><br><b>Suggested Description</b><br>${env.ERROR_DESCRIPTION} <br><br> <b>Suggested Solution</b> <br> ${env.ERROR_SOLUTION} <br><br> <b>Note: -</b> <br> ${env.Note}", cc: 'pkumar05944@gmail.com', charset: 'UTF-8', from: 'cloudawspoc@gmail.com', mimeType: 'text/html', replyTo: '', subject: "Job Failure Alert: Project name -> ${G_JOB_NAME}", to: "pkumar05944@gmail.com";
        }
    }
}
