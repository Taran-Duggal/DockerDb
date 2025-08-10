
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
        stage('Verify DNS Resolution') {
            steps {
                script {
                    echo "=== Verifying DNS Resolution for Maven Central ==="

                    // First verify basic DNS resolution
                    def dnsCheck = bat(
                        script: "nslookup ${MAVEN_REPO_URL}",
                        returnStatus: true
                    )

                    if (dnsCheck != 0) {
                        error "DNS resolution failed for ${MAVEN_REPO_URL}"
                    }

                    echo "✅ DNS resolution verified for ${MAVEN_REPO_URL}"
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    echo "=== Starting Maven Build ==="

                    // Test connectivity with timeout and proper URL
                    def httpCode = bat(
                        script: """
                            curl -s -o nul -w "%{http_code}" --connect-timeout 10 \
                            "https://${MAVEN_REPO_URL}/maven2/"
                        """,
                        returnStdout: true
                    ).trim()

                    echo "Maven Central connectivity check returned HTTP ${httpCode}"

                    try {
                        if (httpCode == "200") {
                            echo "Maven Central accessible - proceeding with online build"
                            bat 'mvn clean package -DskipTests=true'
                        } else {
                            echo "HTTP ${httpCode} received - falling back to offline build"
                            bat 'mvn clean package -DskipTests=true -o'
                        }
                    } catch (Exception e) {
                        echo "Build failed with exception: ${e.getMessage()}"
                        echo "Attempting with local repository only"
                        bat 'mvn clean package -DskipTests=true -o -Dmaven.repo.local=repository'
                    }
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/target/*.war', fingerprint: true
                    echo "Build artifacts archived successfully"
                }
                failure {
                    echo "Build failed - check Maven configuration and network connectivity"
                    echo "Verify hosts file contains correct entry for ${MAVEN_REPO_URL}"
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

                    // Check files were deployed
                    bat """
                        if not exist "${TOMCAT_WEBAPPS}\\${APP_NAME}.war" (
                            echo ❌ WAR file not deployed
                            exit 1
                        )
                        echo ✅ WAR file deployed
                    """

                    // Test application health endpoint
                    def healthy = false
                    def endpoints = [
                        "http://localhost:8080/${APP_NAME}/api/health",
                        "http://localhost:8080/${APP_NAME}/health"
                    ]

                    for (endpoint in endpoints) {
                        try {
                            def status = bat(
                                script: """
                                    curl -s -o nul -w "%{http_code}" --connect-timeout 10 "${endpoint}"
                                """,
                                returnStdout: true
                            ).trim()

                            if (status == "200") {
                                healthy = true
                                echo "✅ Endpoint ${endpoint} is healthy (HTTP 200)"
                                break
                            }
                        } catch (Exception e) {
                            echo "⚠️ Failed to check ${endpoint}: ${e.getMessage()}"
                        }
                    }

                    if (!healthy) {
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
            Build Source: ${currentBuild.result == 'SUCCESS' ? 'Online' : 'Offline'}
            """
        }
        success {
            echo "🎉 Pipeline completed successfully!"
        }
        unstable {
            echo "⚠️ Pipeline completed with warnings"
            echo "Application may still be starting - check Tomcat logs if endpoints aren't responding"
        }
        failure {
            echo """
            ❌ Pipeline failed
            Troubleshooting Steps:
            1. Verify DNS resolution for ${MAVEN_REPO_URL}
            2. Check Maven build logs for errors
            3. Verify Tomcat service account has write permissions to ${TOMCAT_WEBAPPS}
            4. Check Jenkins agent connectivity
            """
        }
    }
}