
pipeline {
    agent {
        label 'master'
    }
    parameters {
        base64File description: 'Please upload a file to read', name: 'FilePath'
        choice choices: ['Document','Technology-refreshment'], description: 'document generation and Technology refreshment', name: 'choice'
        choice choices: ['Python','Rust','Golang'], description: 'select the any one of the above Language', name: 'LanguageSection'
    }
    environment{
        LanguageSection = "Python,Rust,Golang"
    }

    stages {
        stage('Read File Contents') {
            steps {
                script {
                    withFileParameter('FilePath') {
                        sh "cat ${FilePath}"
                    }
                }
            }
        }
        stage('conversion of the File'){
            steps{
                script{
                    if (params.choice == "Technology-refreshment"){
                    withFileParameter('FilePath') {
                        LanguageSection.split(',').each { lang ->
                            echo "${lang}"
                            sh "cat ${FilePath} | sgpt --code \"convert it into the ${lang} language\" >ConvertedFile.${lang}"
                            sh "cat ConvertedFile.${lang}"
                        }
                    }
                    }
                }
            }
        }
        stage("Documentation Generation"){
            steps{
                script{
                    withFileParameter('FilePath') {
                        if (params.choice == "Document"){
                            sh "cat ${FilePath} | sgpt \" generate the documentation\" > Document.txt"
                        }
                    }
                }
            }
            post{
                success {
                    archiveArtifacts artifacts: 'Document.txt'
                }
            }
        }
        stage('File Conversion and Execution') {
            steps {
                script {
                    if (params.choice == "Technology-refreshment"){
                    LanguageSection.split(',').each { lang ->
                        if ("${lang}" == "Python") {
                            sh "mv ConvertedFile.Python ConvertedFile.py"
                            def a = sh(script: "python3 ConvertedFile.py | tail -1",returnStdout: true).trim()
                            def b = sh(script: "python3 ConvertedFile.py | tail -1",returnStdout: true).trim()
                            def c = sh(script: "python3 ConvertedFile.py | tail -1",returnStdout: true).trim()
                            ExecutionTime1 = sh(script:"echo ${a} ${b} ${c} | sgpt \"give only average\" ",returnStdout: true).trim()
                        } else if ("${lang}" == "Rust") {
                            sh "mv ConvertedFile.Rust ConvertedFile.rs"
                            // sh "rustc ConvertedFile.rs"
                            def a = sh(script: "./ConvertedFile | tail -1",returnStdout: true).trim()
                            def b = sh(script: "./ConvertedFile | tail -1",returnStdout: true).trim()
                            def c = sh(script: "./ConvertedFile | tail -1",returnStdout: true).trim()
                            ExecutionTime2 = sh(script:"echo ${a} ${b} ${c} | sgpt \"give only average\" ",returnStdout: true).trim()
                        } else if ("${lang}" == "Golang") {
                            sh "mv ConvertedFile.Golang ConvertedFile.go"
                            def a = sh(script: "go run ConvertedFile.go | tail -1",returnStdout: true).trim()
                            def b = sh(script: "go run ConvertedFile.go | tail -1",returnStdout: true).trim()
                            def c = sh(script: "go run ConvertedFile.go | tail -1",returnStdout: true).trim()
                            ExecutionTime3 = sh(script:"echo ${a} ${b} ${c} | sgpt \"give only average\" ",returnStdout: true).trim()
                            
                        } else {
                            error "Language not available"
                        }
                    }
                    }
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'ConvertedFile.*'
                }
            }
        }

        stage('Average') {
            steps {
                script {
                    if (params.choice == "Technology-refreshment"){
                        echo "Python, average execution time = ${ExecutionTime1}"
                        echo "Rust, average execution time = ${ExecutionTime2}"
                        echo "Golang, average execution time = ${ExecutionTime3}"
                    }
                }
            }
        }
    }
}
