pipeline {
    agent any
    environment {
        BACKUP_DIR = 'C:\\Jenkins\\Backups\\StudentApp'
        TOMCAT_WEBAPPS = 'C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1\\webapps'
        APP_NAME = 'student'
    }

    stages {
        stage('Build') {
            steps {
                script {
                    echo "=== Building Application ==="
                    bat 'mvn clean package -DskipTests=true'
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/target/*.war', fingerprint: true
                }
            }
        }

        stage('Create Backup') {
            steps {
                script {
                    echo "=== Creating Backup ==="

                    def timestamp = bat(
                        script: '@powershell -Command "(Get-Date).ToString(\'yyyyMMdd_HHmmss\')"',
                        returnStdout: true
                    ).trim()

                    bat """
                        @echo off
                        if not exist "${BACKUP_DIR}" mkdir "${BACKUP_DIR}"

                        if exist "${TOMCAT_WEBAPPS}\\${APP_NAME}.war" (
                            copy "${TOMCAT_WEBAPPS}\\${APP_NAME}.war" "${BACKUP_DIR}\\${APP_NAME}.war.backup.${timestamp}"
                            echo ✅ Backup created at ${BACKUP_DIR}\\${APP_NAME}.war.backup.${timestamp}
                        ) else (
                            echo ℹ️ No existing WAR file found - fresh deployment
                        )
                    """
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    echo "=== Deploying WAR File ==="
                    bat """
                        @echo off
                        echo Removing old deployment...
                        if exist "${TOMCAT_WEBAPPS}\\${APP_NAME}.war" del /q "${TOMCAT_WEBAPPS}\\${APP_NAME}.war"
                        if exist "${TOMCAT_WEBAPPS}\\${APP_NAME}" rmdir /s /q "${TOMCAT_WEBAPPS}\\${APP_NAME}"

                        echo Copying new WAR file...
                        copy /y "target\\*.war" "${TOMCAT_WEBAPPS}\\${APP_NAME}.war"

                        echo ✅ Deployment complete at ${TOMCAT_WEBAPPS}\\${APP_NAME}.war
                        echo ℹ️ Please restart Tomcat manually to complete deployment
                    """
                }
            }
        }
    }

    post {
        always {
            echo "=== Cleaning Workspace ==="
            deleteDir()
        }
        success {
            echo "🎉 Pipeline completed successfully!"
            echo "WAR file deployed to: ${TOMCAT_WEBAPPS}\\${APP_NAME}.war"
            echo "Backup created at: ${BACKUP_DIR}\\${APP_NAME}.war.backup.*"
        }
    }
}