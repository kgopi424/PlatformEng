pipeline {
    agent any
    
    stages {
        stage('Ok') {
            steps {
                sh "echo 'Ok' > out.pdf"
            }
        }
    }
    post {
        always {
emailext attachLog: true, attachmentsPattern: '*.pdf', body: 'hello', compressLog: true, subject: 'hi', to: 'pkumar05944@gmail.com'
        }
    }
}