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
                    def tomcatWebapps = 'C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1\\webapps'
                    def jenkinsWar = 'target\\your-app.war' // Replace with actual war name
                    def backupDir = "${tomcatWebapps}\\backup"

                    // Ensure backup directory exists
                    bat "mkdir ${backupDir}"

                    // Backup existing WAR
                    bat """
                        if exist ${tomcatWebapps}\\your-app.war (
                            copy ${tomcatWebapps}\\your-app.war ${backupDir}\\your-app_backup_%DATE:~10,4%-%DATE:~4,2%-%DATE:~7,2%_%TIME:~0,2%-%TIME:~3,2%-%TIME:~6,2%.war
                            del ${tomcatWebapps}\\your-app.war
                        )
                    """

                    // Copy new WAR to Tomcat
                    bat "copy ${jenkinsWar} ${tomcatWebapps}\\your-app.war"
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



