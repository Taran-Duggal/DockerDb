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
                sh 'mvn clean package -DskipTests=true'
                archiveArtifacts artifacts: 'target/*.jar'
            }
        }

        stage('Test Maven - JUnit') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
    }
}
