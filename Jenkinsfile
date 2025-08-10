pipeline {
    agent any
    environment {
        BACKUP_DIR = 'C:\\Jenkins\\Backups\\StudentApp'
        TOMCAT_WEBAPPS = 'C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1\\webapps'
        TOMCAT_SERVICE = 'Tomcat10'
        APP_NAME = 'student'
        TOMCAT_STOP_METHOD = 'not_needed' // Default value
    }

    stages {
        stage('Build') {
            steps {
                script {
                    echo "=== Starting Maven Build ==="

                    // Test Maven Central connectivity with timeout
                    def httpCode = bat(
                        script: 'curl -s -o nul -w "%{http_code}" --connect-timeout 10 https://repo.maven.apache.org/maven2/',
                        returnStdout: true
                    ).trim()

                    if(httpCode == "200") {
                        echo "Maven Central accessible (HTTP ${httpCode}) - proceeding with build"
                        bat 'mvn clean package -DskipTests=true'
                    } else {
                        echo "Maven Central connection issue (HTTP ${httpCode}) - trying offline build"
                        bat 'mvn clean package -DskipTests=true -o'
                    }
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/target/*.war', fingerprint: true
                }
            }
        }

        stage('Pre-Deployment Checks') {
            steps {
                script {
                    echo "=== Pre-Deployment Checks ==="

                    // Create backup directory with error handling
                    bat """
                        if not exist "${BACKUP_DIR}" (
                            mkdir "${BACKUP_DIR}" || exit /b 1
                            echo ✅ Created backup directory
                        )
                    """

                    // Verify Tomcat directory access
                    bat """
                        if not exist "${TOMCAT_WEBAPPS}" (
                            echo ❌ Tomcat webapps directory not accessible
                            exit 1
                        )
                    """

                    // Check Tomcat service status
                    def serviceStatus = bat(
                        script: "sc query ${TOMCAT_SERVICE} | findstr STATE",
                        returnStdout: true
                    ).trim()

                    if(serviceStatus.contains("RUNNING")) {
                        env.TOMCAT_RUNNING = 'true'
                        echo "⚠️ Tomcat running - will attempt to stop"
                    } else {
                        env.TOMCAT_RUNNING = 'false'
                        echo "✅ Tomcat already stopped"
                    }
                }
            }
        }

        stage('Create Backup') {
            steps {
                script {
                    echo "=== Creating Application Backup ==="

                    def timestamp = bat(
                        script: '@powershell -Command "(Get-Date).ToString(\'yyyyMMdd_HHmmss\')"',
                        returnStdout: true
                    ).trim()

                    env.BACKUP_FILE = "${BACKUP_DIR}\\${APP_NAME}.war.backup.${timestamp}"

                    bat """
                        @echo off
                        setlocal enabledelayedexpansion

                        if exist "${TOMCAT_WEBAPPS}\\${APP_NAME}.war" (
                            echo Creating backup of WAR file...
                            copy "${TOMCAT_WEBAPPS}\\${APP_NAME}.war" "${env.BACKUP_FILE}" || exit /b 1

                            if exist "${TOMCAT_WEBAPPS}\\${APP_NAME}" (
                                echo Backing up application directory...
                                robocopy "${TOMCAT_WEBAPPS}\\${APP_NAME}" "${BACKUP_DIR}\\${APP_NAME}_${timestamp}" /MIR /NP /NFL /NDL
                            )
                        ) else (
                            echo Fresh deployment detected
                            echo FRESH_DEPLOYMENT > "${BACKUP_DIR}\\deployment_type_${timestamp}.txt"
                        )

                        echo Cleaning old backups...
                        for /f "skip=5 delims=" %%i in ('dir /b /o-d "${BACKUP_DIR}\\${APP_NAME}.war.backup.*" 2^>nul') do (
                            del "${BACKUP_DIR}\\%%i"
                        )
                    """
                }
            }
        }

        stage('Stop Tomcat') {
            when {
                expression { env.TOMCAT_RUNNING == 'true' }
            }
            steps {
                script {
                    echo "=== Stopping Tomcat ==="

                    withCredentials([usernamePassword(
                        credentialsId: 'tomcat-admin-creds',
                        usernameVariable: 'ADMIN_USER',
                        passwordVariable: 'ADMIN_PASS'
                    )]) {
                        def stopStatus = bat(
                            script: """
                                powershell -Command "\$secpass = ConvertTo-SecureString '${ADMIN_PASS}' -AsPlainText -Force;
                                \$cred = New-Object System.Management.Automation.PSCredential('${ADMIN_USER}', \$secpass);
                                Start-Process cmd -Credential \$cred -ArgumentList '/c','net stop ${TOMCAT_SERVICE}' -Wait -NoNewWindow"
                            """,
                            returnStatus: true
                        )

                        if(stopStatus == 0) {
                            env.TOMCAT_STOP_METHOD = 'service_stop'
                            echo "✅ Tomcat stopped successfully"
                        } else {
                            error("❌ Failed to stop Tomcat")
                        }
                    }
                }
            }
        }

        stage('Deploy Application') {
            steps {
                script {
                    echo "=== Deploying Application ==="

                    bat """
                        @echo off
                        setlocal

                        echo Removing old deployment...
                        if exist "${TOMCAT_WEBAPPS}\\${APP_NAME}.war" del /q "${TOMCAT_WEBAPPS}\\${APP_NAME}.war"
                        if exist "${TOMCAT_WEBAPPS}\\${APP_NAME}" rmdir /s /q "${TOMCAT_WEBAPPS}\\${APP_NAME}"

                        echo Deploying new WAR...
                        copy /y "target\\*.war" "${TOMCAT_WEBAPPS}\\${APP_NAME}.war" || exit /b 1

                        echo Setting permissions...
                        icacls "${TOMCAT_WEBAPPS}\\${APP_NAME}.war" /grant "Everyone:(F)" >nul 2>&1
                    """
                }
            }
        }

        stage('Start Tomcat') {
            when {
                expression { env.TOMCAT_RUNNING == 'true' }
            }
            steps {
                script {
                    echo "=== Starting Tomcat ==="

                    withCredentials([usernamePassword(
                        credentialsId: 'tomcat-admin-creds',
                        usernameVariable: 'ADMIN_USER',
                        passwordVariable: 'ADMIN_PASS'
                    )]) {
                        def startStatus = bat(
                            script: """
                                powershell -Command "\$secpass = ConvertTo-SecureString '${ADMIN_PASS}' -AsPlainText -Force;
                                \$cred = New-Object System.Management.Automation.PSCredential('${ADMIN_USER}', \$secpass);
                                Start-Process cmd -Credential \$cred -ArgumentList '/c','net start ${TOMCAT_SERVICE}' -Wait -NoNewWindow"
                            """,
                            returnStatus: true
                        )

                        if(startStatus == 0) {
                            env.TOMCAT_START_SUCCESS = 'true'
                            echo "✅ Tomcat started successfully"

                            // Wait for deployment to complete
                            bat 'timeout /t 30 >nul'
                        } else {
                            env.TOMCAT_START_SUCCESS = 'false'
                            error("❌ Failed to start Tomcat")
                        }
                    }
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                script {
                    echo "=== Verifying Deployment ==="

                    // Check files were deployed
                    bat """
                        if not exist "${TOMCAT_WEBAPPS}\\${APP_NAME}.war" (
                            echo ❌ WAR file not deployed
                            exit 1
                        )
                        echo ✅ WAR file deployed
                    """

                    // Test application endpoints
                    def endpoints = ["http://localhost:8080/${APP_NAME}/api/health"]
                    def healthy = false

                    for(endpoint in endpoints) {
                        def status = bat(
                            script: "curl -s -o nul -w \"%{http_code}\" --connect-timeout 10 \"${endpoint}\"",
                            returnStdout: true
                        ).trim()

                        if(status == "200") {
                            healthy = true
                            echo "✅ Endpoint ${endpoint} is healthy (HTTP 200)"
                            break
                        }
                    }

                    if(!healthy) {
                        echo "⚠️ No healthy endpoints found - application may still be starting"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }
    }

    post {
        always {
            echo "=== Cleaning Workspace ==="
            deleteDir()

            // Final status report
            echo """
            === Deployment Summary ===
            Backup Location: ${env.BACKUP_FILE ?: 'N/A'}
            Tomcat Status: ${env.TOMCAT_RUNNING == 'true' ? 'Was running' : 'Was stopped'}
            Stop Method: ${env.TOMCAT_STOP_METHOD}
            Start Success: ${env.TOMCAT_START_SUCCESS ?: 'N/A'}
            """
        }
        success {
            echo "🎉 Deployment completed successfully!"
        }
        unstable {
            echo "⚠️ Deployment completed with warnings"
        }
        failure {
            echo """
            ❌ Deployment failed
            Rollback Options:
            1. Restore backup: copy "${env.BACKUP_FILE}" "${TOMCAT_WEBAPPS}\\${APP_NAME}.war"
            2. Check Tomcat logs: type "%CATALINA_HOME%\\logs\\catalina.out"
            """
        }
    }
}