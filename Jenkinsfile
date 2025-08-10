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
                    def tomcatWebapps = '"C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1\\webapps"'
                    def backupDir = "${tomcatWebapps}\\backup"
                    def warName = "student.war"
                    def warSource = "\"${env.WORKSPACE}\\target\\${warName}\""
                    def warDest = "${tomcatWebapps}\\${warName}"

                    // Create backup directory
                    bat "mkdir ${backupDir}"

                    // Backup existing WAR
                    bat """
                        if exist ${warDest} (
                            copy ${warDest} ${backupDir}\\${warName}_backup_%DATE:~10,4%-%DATE:~4,2%-%DATE:~7,2%_%TIME:~0,2%-%TIME:~3,2%-%TIME:~6,2%.war
                            del ${warDest}
                        )
                    """

                    // Copy new WAR to Tomcat
                    bat "copy ${warSource} ${warDest}"
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



