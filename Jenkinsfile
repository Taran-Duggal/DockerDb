// pipeline {
//     agent any
//     tools {
//         maven "Maven-3.8.6"
//     }
//
//     stages {
//         stage('Clone Repo') {
//             steps {
//                 git branch: 'main', url: 'https://github.com/Taran-Duggal/DockerDb'
//             }
//         }
//
//         stage('Build Artifact') {
//             steps {
//                 bat 'mvn clean package -DskipTests=true'
//                 archiveArtifacts artifacts: 'target/*.jar'
//             }
//         }
//
//         stage('Test Maven - JUnit') {
//             steps {
//                 bat 'mvn test'
//             }
//             post {
//                 always {
//                     script {
//                         if (fileExists('target/surefire-reports')) {
//                             junit 'target/surefire-reports/*.xml'
//                         } else {
//                             echo 'No test reports found. Skipping junit step.'
//                         }
//                     }
//                 }
//             }
//         }
//     }
// }


pipeline{
    agent any

    environment {
        BACKUP_DIR = 'C:\\Jenkins\\Backups\\StudentApp'
        TOMCAT_WEBAPPS = 'C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1\\webapps'
        TOMCAT_SERVICE = 'Tomcat10'
        APP_NAME = 'student'
    }

    stages{
        stage('Build'){
            steps{
                script{
                    echo "=== Starting Maven Build ==="
                    echo "Testing Maven Central connectivity..."

                    // Test HTTP connectivity to Maven Central
                    def httpTest = bat(script: 'curl -I --connect-timeout 10 https://repo.maven.apache.org/maven2/ 2>nul', returnStatus: true)

                    if(httpTest == 0) {
                        echo "Maven Central is accessible - proceeding with normal build"
                        bat 'mvn clean package -DskipTests=true'
                    } else {
                        echo "HTTPS connection to Maven Central failed - trying alternatives"
                        try {
                            bat 'mvn clean package -DskipTests=true -o'
                            echo "Offline build successful!"
                        } catch (Exception e) {
                            echo "Offline build failed - trying with HTTP repository"
                            bat 'mvn clean package -DskipTests=true -Dmaven.repo.remote=http://repo.maven.apache.org/maven2'
                        }
                    }
                }
            }
            post{
                success{
                    echo 'Build completed successfully!'
                    echo 'Now Archiving the Artifacts'
                    archiveArtifacts artifacts: '**/target/*.war'
                }
                failure{
                    echo '=== BUILD FAILED ==='
                    echo 'Please check Maven configuration and network connectivity'
                }
            }
        }

        stage('Pre-Deployment Checks'){
            steps{
                script{
                    echo "=== Pre-Deployment Checks ==="

                    // Create backup directory if it doesn't exist
                    bat """
                        echo "Creating backup directory if it doesn't exist..."
                        if not exist "${BACKUP_DIR}" (
                            mkdir "${BACKUP_DIR}"
                            echo "✅ Backup directory created: ${BACKUP_DIR}"
                        ) else (
                            echo "✅ Backup directory already exists: ${BACKUP_DIR}"
                        )
                    """

                    // Check if Jenkins has permission to access Tomcat directories
                    bat """
                        echo "Checking Tomcat directory access..."
                        if exist "${TOMCAT_WEBAPPS}" (
                            echo "✅ Tomcat webapps directory accessible"
                        ) else (
                            echo "❌ Cannot access Tomcat webapps directory"
                            exit 1
                        )
                    """

                    // Check current Tomcat service status
                    def serviceStatus = bat(script: "sc query ${TOMCAT_SERVICE} | findstr STATE", returnStdout: true).trim()
                    echo "Current Tomcat service status: ${serviceStatus}"

                    if(serviceStatus.contains("RUNNING")) {
                        echo "⚠️  Tomcat is currently running - will need admin privileges to stop"
                        env.TOMCAT_RUNNING = 'true'
                    } else {
                        echo "✅ Tomcat is not running - can proceed safely"
                        env.TOMCAT_RUNNING = 'false'
                    }
                }
            }
        }

        stage('Create Backup'){
            steps{
                script{
                    echo "=== Creating Application Backup ==="

                    // Generate timestamp for backup
                    def timestamp = bat(script: '@powershell -Command "(Get-Date).ToString(\'yyyyMMdd_HHmmss\')"', returnStdout: true).trim()
                    env.BACKUP_TIMESTAMP = timestamp
                    env.BACKUP_FILE = "${BACKUP_DIR}\\${APP_NAME}.war.backup.${timestamp}"

                    // Create backup with proper escaping
                    bat """
                        @echo off
                        set TIMESTAMP=${timestamp}
                        set BACKUP_FILE=${env.BACKUP_FILE}

                        echo Backup timestamp: %TIMESTAMP%
                        echo Backup location: %BACKUP_FILE%

                        if exist "${TOMCAT_WEBAPPS}\\${APP_NAME}.war" (
                            echo Creating backup of existing WAR file...
                            copy "${TOMCAT_WEBAPPS}\\${APP_NAME}.war" "%BACKUP_FILE%"
                            echo Backup created successfully: %BACKUP_FILE%

                            if exist "${TOMCAT_WEBAPPS}\\${APP_NAME}" (
                                echo Creating backup of application directory...
                                xcopy "${TOMCAT_WEBAPPS}\\${APP_NAME}" "${BACKUP_DIR}\\${APP_NAME}_%TIMESTAMP%" /E /I /H /Y
                                echo Application directory backed up
                            )
                        ) else (
                            echo No existing WAR file found - fresh deployment
                            echo FRESH_DEPLOYMENT > "${BACKUP_DIR}\\deployment_type_%TIMESTAMP%.txt"
                        )

                        echo Cleaning old backups (keeping last 5)...
                        for /f "skip=5 delims=" %%i in ('dir /b /o-d "${BACKUP_DIR}\\${APP_NAME}.war.backup.*" 2^>nul') do (
                            del "${BACKUP_DIR}\\%%i"
                            echo Removed old backup: %%i
                        )
                    """
                }
            }
        }

        stage('Stop Tomcat') {
                    steps {
                        script {
                            echo "=== Stopping Tomcat Service ==="

                            withCredentials([usernamePassword(
                                credentialsId: 'tomcat-admin-creds',
                                usernameVariable: 'ADMIN_USER',
                                passwordVariable: 'ADMIN_PASS'
                            )]) {
                                // Method 1: Using PowerShell with credentials
                                bat """
                                    powershell -Command "\$securePass = ConvertTo-SecureString '${ADMIN_PASS}' -AsPlainText -Force; \
                                    \$credential = New-Object System.Management.Automation.PSCredential('${ADMIN_USER}', \$securePass); \
                                    Start-Process -FilePath 'cmd.exe' -ArgumentList '/c','net stop ${TOMCAT_SERVICE}' -Credential \$credential -NoNewWindow -Wait"
                                """

                                // Verify service stopped
                                def status = bat(script: "sc query ${TOMCAT_SERVICE} | findstr STATE", returnStdout: true).trim()
                                if(status.contains("STOPPED")) {
                                    echo "✅ Tomcat service confirmed stopped"
                                } else {
                                    error("❌ Failed to stop Tomcat service")
                                }
                            }
                        }
                    }
                }


        stage('Deploy Application'){
            steps{
                bat """
                    echo "=== Starting Application Deployment ==="

                    echo "Step 1: Removing old application files..."
                    if exist "${TOMCAT_WEBAPPS}\\${APP_NAME}.war" (
                        echo "Removing old WAR file..."
                        del "${TOMCAT_WEBAPPS}\\${APP_NAME}.war"
                    )

                    if exist "${TOMCAT_WEBAPPS}\\${APP_NAME}" (
                        echo "Removing old application directory..."
                        rmdir /s /q "${TOMCAT_WEBAPPS}\\${APP_NAME}"
                        echo "✅ Old application files removed"
                    )

                    echo "Step 2: Deploying new WAR file..."
                    copy target\\*.war "${TOMCAT_WEBAPPS}\\${APP_NAME}.war"
                    echo "✅ New WAR file deployed successfully"

                    echo "Step 3: Setting file permissions (if needed)..."
                    rem Grant full control to Tomcat service account if needed
                    icacls "${TOMCAT_WEBAPPS}\\${APP_NAME}.war" /grant "Everyone:(F)" /T 2>nul || echo "Permission setting skipped"
                """
            }
        }

        stage('Start Tomcat') {
            steps {
                script {
                    echo "=== Starting Tomcat Service ==="

                    withCredentials([usernamePassword(
                        credentialsId: 'tomcat-admin-creds',
                        usernameVariable: 'ADMIN_USER',
                        passwordVariable: 'ADMIN_PASS'
                    )]) {
                        bat """
                            powershell -Command "\$securePass = ConvertTo-SecureString '${ADMIN_PASS}' -AsPlainText -Force; \
                            \$credential = New-Object System.Management.Automation.PSCredential('${ADMIN_USER}', \$securePass); \
                            Start-Process -FilePath 'cmd.exe' -ArgumentList '/c','net start ${TOMCAT_SERVICE}' -Credential \$credential -NoNewWindow -Wait"
                        """

                        // Verify service started
                        def status = bat(script: "sc query ${TOMCAT_SERVICE} | findstr STATE", returnStdout: true).trim()
                        if(status.contains("RUNNING")) {
                            echo "✅ Tomcat service confirmed running"
                        } else {
                            error("❌ Failed to start Tomcat service")
                        }
                    }
                }
            }
        }

        stage('Post-Deployment Verification'){
            steps{
                script{
                    echo "=== Post-Deployment Verification ==="

                    try {
                        // Check Tomcat service status
                        bat """
                            echo "Checking Tomcat service status..."
                            sc query ${TOMCAT_SERVICE} | findstr "RUNNING" || (echo "⚠️  Tomcat service is not running" && exit 1)
                            echo "✅ Tomcat service is running"
                        """

                        // Check if application was deployed (directory exists)
                        bat """
                            echo "Checking application deployment..."
                            if exist "${TOMCAT_WEBAPPS}\\${APP_NAME}" (
                                echo "✅ Application directory exists"
                                echo "Application files:"
                                dir "${TOMCAT_WEBAPPS}\\${APP_NAME}" /W
                            ) else (
                                echo "⚠️  Application directory not found - deployment may be in progress"
                            )
                        """

                        // Test API endpoints
                        def endpoints = [
                            "http://localhost:8092/student/",
                            "http://localhost:8092/student/api/",
                            "http://localhost:8080/student/",
                            "http://localhost:8080/student/api/"
                        ]

                        def workingEndpoint = null
                        echo "Testing API endpoints..."

                        for (endpoint in endpoints) {
                            echo "Testing: ${endpoint}"
                            def result = bat(script: "curl -s -o nul -w \"%{http_code}\" --connect-timeout 10 \"${endpoint}\"", returnStdout: true).trim()

                            if (result == "200") {
                                echo "✅ ${endpoint} - HTTP 200 (SUCCESS)"
                                workingEndpoint = endpoint
                                break
                            } else if (result == "404") {
                                echo "❌ ${endpoint} - HTTP 404 (Not Found)"
                            } else if (result == "000") {
                                echo "❌ ${endpoint} - Connection failed"
                            } else {
                                echo "⚠️  ${endpoint} - HTTP ${result}"
                            }
                        }

                        if (workingEndpoint) {
                            echo "🎉 DEPLOYMENT SUCCESSFUL!"
                            echo "✅ Working endpoint found: ${workingEndpoint}"
                            currentBuild.result = 'SUCCESS'
                        } else {
                            echo "⚠️  No working endpoints found - application may still be starting"
                            currentBuild.result = 'UNSTABLE'
                        }

                    } catch (Exception e) {
                        echo "Verification encountered issues: ${e.getMessage()}"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }
    }

    post{
        always{
            script{
                echo "=== DEPLOYMENT SUMMARY ==="
                echo "Backup location: ${env.BACKUP_FILE ?: 'No backup created'}"
                echo "Backup directory: ${BACKUP_DIR}"
                echo "Tomcat stop method: ${env.TOMCAT_STOP_METHOD ?: 'Not stopped'}"
                echo "Tomcat start success: ${env.TOMCAT_START_SUCCESS ?: 'Unknown'}"
            }
            echo "=== CLEANING UP WORKSPACE ==="
            deleteDir()
        }
        success{
            echo "🎉 === BUILD AND DEPLOYMENT SUCCESSFUL ==="
            echo "✅ Student application deployed successfully"
            echo "📁 Backup stored at: ${env.BACKUP_FILE}"
            echo "🌐 Application should be accessible soon"
        }
        unstable{
            echo "⚠️  === DEPLOYMENT COMPLETED WITH WARNINGS ==="
            echo "📁 Backup available at: ${env.BACKUP_FILE}"
            echo "🔧 Manual verification may be required"
            echo "Check Tomcat logs and application endpoints manually"
        }
        failure{
            echo "❌ === DEPLOYMENT FAILED ==="
            echo "🔄 ROLLBACK OPTIONS:"
            echo "1. Restore from backup: ${env.BACKUP_FILE}"
            echo "2. Manual rollback command:"
            echo "   copy \"${env.BACKUP_FILE}\" \"${TOMCAT_WEBAPPS}\\${APP_NAME}.war\""
            echo ""
            echo "🔧 ADMIN PRIVILEGE SOLUTIONS:"
            echo "1. Run Jenkins service as Administrator"
            echo "2. Use 'Run as Administrator' for Jenkins"
            echo "3. Configure Jenkins to run with admin privileges"
            echo "4. Manual Tomcat control via Services.msc"
        }
    }
}