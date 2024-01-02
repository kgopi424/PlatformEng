pipeline {
    agent {
        label 'master'
    }

    stages {
        stage('SCM Checkout') {
            steps {
                script {
                    git branch: 'main', url: 'https://github.com/AbhishekRaoV/Augmented_AI'
                }
            }
        }

        stage("Code Coverage") {
            steps {
                script {
                  
                        sh "cat binarytree.py | sgpt --code \"modify the code according to coding standards and optimise for time and space complexity\" > CodeDesign_Consistency.txt"
                    
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/CodeDesign_Consistency.txt'
                }
            }
        }
    }
}
