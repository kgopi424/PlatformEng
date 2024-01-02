pipeline {
    agent {
        label 'master'
    }

    stages {
        stage('SCM Checkout') {
            steps {
                script {
                    git branch: 'sandbox-env', credentialsId: 'gitlab-microservices', url: 'http://10.63.32.87/platformengineering/microservices/'
                }
            }
        }

        stage("Code Coverage") {
            steps {
                script {
                    sh "rm -rf binarytree.py"
                    sh "rm -rf binary"
                    sh "git clone https://github.com/AbhishekRaoV/binary.git"
                    sh " rm -rf tests"
                    sh " mkdir tests"
                    dir('initialApp'){
                        sh '''
                        count=0
                        for file in $(ls -r *.py | head -n 2 | sort); do
                            if [ -f "$file" ]; then
                                sgpt --code "Generate unit test cases using unittest framework" < "$file" > "../tests/test_$file"
                                cat ../tests/test_$file >> $file
                              
                                 count=$((count + 1))
                        
                                if [ "$count" -eq 2 ]; then
                                    break
                                fi
                            fi
                        done
                        '''
                    }
                    dir('binary'){
                    sh '''
                    cat binarytree.py | sgpt --code "generate unit test cases using unittest framework" > ../tests/test_binary.py
                    cat ../tests/test_binary.py >> binarytree.py
                    python3 -m coverage run binarytree.py
                    python3 -m coverage report binarytree.py
                    '''
                    }
                    
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/initialApp/*.py'
                    archiveArtifacts artifacts: '**/tests/*'
                }
            }
        }
    }
}