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
                archiveArtifacts artifacts: 'target/*.war'
            }
        }

        stage('Deploy to Tomcat') {
            steps {
                script {
                    def tomcatBase = "C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1"
                    def warName = "student.war"
                    def warSource = "${env.WORKSPACE}\\target\\${warName}"
                    def warDest = "${tomcatBase}\\webapps\\${warName}"

                    // Generate timestamp for backup folder
                    def timestamp = new Date().format("yyyy-MM-dd_HH-mm-ss")
                    def backupDir = "${tomcatBase}\\backup-${timestamp}"

                    // Create backup directory
                    bat "mkdir \"${backupDir}\""

                    // Backup and replace WAR
                    bat """
                        if exist \"${warDest}\" (
                            copy \"${warDest}\" \"${backupDir}\\${warName}\"
                            del /Q \"${warDest}\"
                        )
                        copy \"${warSource}\" \"${warDest}\"
                    """
                }
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



