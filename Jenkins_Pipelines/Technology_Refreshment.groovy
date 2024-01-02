
pipeline {
    agent {
        label 'master'
    }
    parameters {
        choice choices: ['py','rs','go','java'], description: 'select the any one of the above Language', name: 'LanguageSection'
    }

    stages {    
        stage('SCM Checkout') {
            steps {
                script {
                    git branch: 'main', url: 'https://github.com/AbhishekRaoV/Augmented_AI'
                }
            }
        }
        stage('conversion of the File'){
            steps{
                script{
                        sh "cat binarytree.py | sgpt --code \"convert to ${LanguageSection} code --no-cache\" >ConvertedFile.${LanguageSection}"                     
                    }
                    }
            post {
                success {
                    script{
                    if("${params.LanguageSection}" == "py"){
                        archiveArtifacts artifacts: 'ConvertedFile.py'
                    }
                    if("${params.LanguageSection}" == "rs"){
                        archiveArtifacts artifacts: 'ConvertedFile.rs'
                    }
                    if("${params.LanguageSection}" == "go"){
                        archiveArtifacts artifacts: 'ConvertedFile.go'
                    }
                    if("${params.LanguageSection}" == "java"){
                        archiveArtifacts artifacts: 'ConvertedFile.java'
                    }
                    }
                }
            }
        } 
    }
}
