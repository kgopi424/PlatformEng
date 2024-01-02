pipeline {
    agent{
        label 'master'
    }
    parameters {
        base64File description: 'Please upload a file to read', name: 'FilePath'
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                   git branch: 'main', url: 'https://github.com/AbhishekRaoV/Augmented_AI.git'
                }
            }
        }
        stage("Review") {
            steps {
                script {
                    withFileParameter('FilePath') {
                        // sh "cat ${FilePath}"
                        sh "cp ${FilePath} /var/lib/jenkins/workspace/Sourcery/binarytree.py"
                        sh "cat binarytree.py"
                        sh "sudo -E /var/lib/jenkins/.local/bin/sourcery review binarytree.py>Review.txt"
                    }
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/Review.txt'
                }
            }
        }
        stage("Fix") {
            steps {
                script {
                    withFileParameter('FilePath') {
                        sh "sudo -E /var/lib/jenkins/.local/bin/sourcery review --fix binarytree.py"
                    }
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/binarytree.py'
                    sh 'cp binarytree.py /var/lib/jenkins/workspace/Sourcery/Augmented_AI'
                    dir('/var/lib/jenkins/workspace/Sourcery/Augmented_AI') {
                    sh 'git add .'
                    sh 'git commit -m "Jenkins Build"'
                    sh 'git push https://AbhishekRaoV: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@github.com/AbhishekRaoV/Augmented_AI.git main'
                    sh 'rm -f binarytree.py'
                }
                }
            }
        }
    }
}
