pipeline {
  agent any
  tools {
        maven "Maven-3.8.6"
   }

  stages {
      stage('clone repo') {
            steps {
              git branch: 'main', url: 'https://github.com/Taran-Duggal/DockerDb'
            }
      }
      stage('Build Artifact') {
            steps {
              sh 'mvn clean package -DskipTests=true'
              archive 'target/*.jar'
            }
      }
            }
       }
      stage('Test Maven - JUnit') {
            steps {
              sh "mvn test"
            }
            post{
              always{
                junit 'target/surefire-reports/*.xml'
              }
            }
        }


     }
}