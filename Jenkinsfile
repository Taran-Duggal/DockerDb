
pipeline {
    agent any
    environment {
        BACKUP_DIR = 'C:\\Jenkins\\Backups\\StudentApp'
        TOMCAT_WEBAPPS = 'C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1\\webapps'
        TOMCAT_SERVICE = 'Tomcat10'
        APP_NAME = 'student'
        MAVEN_REPO_URL = 'repo.maven.apache.org'
    }

    stages {
        stage('Verify Connectivity') {
            steps {
                script {
                    echo "=== Verifying Maven Central Connectivity ==="

                    // DNS Resolution Check
                    def dnsCheck = bat(
                        script: "nslookup ${MAVEN_REPO_URL}",
                        returnStatus: true
                    )

                    if (dnsCheck != 0) {
                        error "❌ DNS resolution failed for ${MAVEN_REPO_URL}"
                    }
                    echo "✅ DNS resolution verified for ${MAVEN_REPO_URL}"

                    // HTTP Connectivity Check using PowerShell
                    def httpStatus = powershell(
                        script: """
                            try {
                                \$response = Invoke-WebRequest -Uri "https://${MAVEN_REPO_URL}/maven2/" -Method Head -UseBasicParsing -TimeoutSec 10
                                Write-Output \$response.StatusCode
                            } catch {
                                Write-Output \$_.Exception.Response.StatusCode.Value__
                            }
                        """,
                        returnStdout: true
                    ).trim()

                    echo "Maven Central connectivity check returned HTTP ${httpStatus}"

                    if (httpStatus == "200") {
                        env.MAVEN_ONLINE = true
                        echo "✅ Maven Central is accessible"
                    } else {
                        env.MAVEN_ONLINE = false
                        echo "⚠️ Maven Central not accessible (HTTP ${httpStatus}) - will use offline mode"
                    }
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    echo "=== Starting Maven Build ==="

                    // Remove the problematic curl command
                    // Add Maven version check for debugging
                    bat 'mvn --version'

                    try {
                        if (env.MAVEN_ONLINE == "true") {
                            bat 'mvn clean package -DskipTests=true'
                        } else {
                            bat 'mvn clean package -DskipTests=true -o'
                        }
                    } catch (Exception e) {
                        error "❌ Build failed: ${e.getMessage()}"
                    }
                }
            }
            post {
                failure {
                    echo "❌ Build failed - checking possible causes..."
                    bat 'where mvn'
                    bat 'echo %PATH%'
                    bat 'echo %MAVEN_HOME%'
                }
            }
        }

        stage('Pre-Deployment Checks') {
            steps {
                script {
                    echo "=== Pre-Deployment Checks ==="

                    // Create backup directory
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

                    if (serviceStatus.contains("RUNNING")) {
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
                    """
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

        stage('Verify Deployment') {
            steps {
                script {
                    echo "=== Verifying Deployment ==="

                    // 1. Verify WAR file exists
                    bat """
                        if not exist "${TOMCAT_WEBAPPS}\\${APP_NAME}.war" (
                            echo ❌ WAR file not deployed
                            exit 1
                        )
                        echo ✅ WAR file deployed
                    """

                    // 2. Start Tomcat if not running
                    if (env.TOMCAT_RUNNING == 'false') {
                        echo "Starting Tomcat service..."
                        bat "net start ${TOMCAT_SERVICE}"
                        sleep(time: 30, unit: 'SECONDS') // Wait for Tomcat to start
                    }

                    // 3. Test application health endpoints with proper curl commands
                    def healthy = false
                    def endpoints = [
                        "http://localhost:8080/${APP_NAME}/api/health",
                        "http://localhost:8080/${APP_NAME}/health"
                    ]

                    for (endpoint in endpoints) {
                        try {
                            def status = bat(
                                script: """
                                    curl -s -o nul -w "%%{http_code}" --connect-timeout 10 "${endpoint}"
                                """,
                                returnStdout: true
                            ).trim()

                            if (status == "200") {
                                healthy = true
                                echo "✅ Endpoint ${endpoint} is healthy (HTTP 200)"
                                break
                            } else {
                                echo "⚠️ Endpoint ${endpoint} returned HTTP ${status}"
                            }
                        } catch (Exception e) {
                            echo "⚠️ Failed to check ${endpoint}: ${e.getMessage()}"
                        }
                    }

                    if (!healthy) {
                        echo "⚠️ No healthy endpoints found - check Tomcat logs"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }
    }
}