pipeline {
    agent any
    tools {
        maven "Maven-3.8.6"
    }

    stages {
        stage('Clone Repo') {
            steps {
                git branch: 'main', url: 'https://github.com/Taran-Duggal/DockerDb'
            }
        }

        stage('Build Artifact') {
            steps {
                bat 'mvn clean package -DskipTests=true'
                archiveArtifacts artifacts: 'target/*.jar'
            }
        }

        stage('Test Maven - JUnit') {
            steps {
                bat 'mvn test'
            }
            post {
                always {
                    script {
                        if (fileExists('target/surefire-reports')) {
                            junit 'target/surefire-reports/*.xml'
                        } else {
                            echo 'No test reports found. Skipping junit step.'
                        }
                    }
                }
            }
        }
    }
}



