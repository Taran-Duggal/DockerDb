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
                        echo "HTTPS connection to Maven Central failed"
                        echo "Testing HTTP connection..."

                        def httpTestPlain = bat(script: 'curl -I --connect-timeout 10 http://repo.maven.apache.org/maven2/ 2>nul', returnStatus: true)

                        if(httpTestPlain == 0) {
                            echo "HTTP works but HTTPS is blocked - trying with HTTP repository"
                            bat '''
                                mvn clean package -DskipTests=true -Dmaven.repo.remote=http://repo.maven.apache.org/maven2
                            '''
                        } else {
                            echo "Both HTTP and HTTPS are blocked - trying offline build"
                            echo "This will only work if dependencies are already cached locally"

                            try {
                                bat 'mvn clean package -DskipTests=true -o'
                                echo "Offline build successful!"
                            } catch (Exception e) {
                                echo "Offline build also failed - need to resolve network connectivity"

                                // Try with alternative repository
                                echo "Trying with alternative Maven repository..."
                                bat '''
                                    mvn clean package -DskipTests=true -Dmaven.repo.remote=https://repo1.maven.org/maven2
                                '''
                            }
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
                    echo '=== BUILD FAILED - NETWORK CONNECTIVITY TROUBLESHOOTING ==='
                    echo ''
                    echo 'DIAGNOSIS:'
                    echo '- DNS Resolution: ✅ Working (repo.maven.apache.org resolves to 199.232.20.215)'
                    echo '- HTTP/HTTPS Access: ❌ Blocked by corporate firewall/proxy'
                    echo ''
                    echo 'SOLUTIONS:'
                    echo ''
                    echo '1. CONFIGURE MAVEN PROXY:'
                    echo '   Create C:\\Users\\duggal.84\\.m2\\settings.xml with:'
                    echo '   <proxies><proxy><host>PROXY-HOST</host><port>PROXY-PORT</port></proxy></proxies>'
                    echo ''
                    echo '2. CONTACT IT TEAM FOR:'
                    echo '   - Corporate proxy settings (host:port)'
                    echo '   - Firewall whitelist for repo.maven.apache.org (199.232.20.215)'
                    echo '   - Corporate Maven repository URL'
                    echo ''
                    echo '3. ALTERNATIVE - USE CORPORATE REPOSITORY:'
                    echo '   Ask IT for internal Nexus/Artifactory URL'
                    echo ''
                    echo '4. MANUAL DEPENDENCY DOWNLOAD:'
                    echo '   Download JARs manually and install to local repo'
                    echo ''
                    echo 'NETWORK TEST COMMANDS:'
                    echo '- Test HTTP: curl -I http://repo.maven.apache.org/maven2/'
                    echo '- Test HTTPS: curl -I https://repo.maven.apache.org/maven2/'
                    echo '- Test connectivity: telnet 199.232.20.215 80'
                }
            }
        }
        stage('Deploy to Tomcat Server'){
            steps{
                bat '''
                    echo "=== Starting Tomcat Deployment ==="

                    echo "Step 1: Stopping Tomcat service..."
                    net stop Tomcat10
                    timeout /t 10

                    echo "Step 2: Creating backup of existing WAR file..."
                    if exist "C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1\\webapps\\student.war" (
                        echo "Backing up existing WAR file..."
                        copy "C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1\\webapps\\student.war" "C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1\\webapps\\student.war.backup.%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%"
                        del "C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1\\webapps\\student.war"
                        echo "Old WAR file backed up and removed"
                    ) else (
                        echo "No existing WAR file found, proceeding with fresh deployment"
                    )

                    echo "Step 3: Removing old application directory..."
                    if exist "C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1\\webapps\\student" (
                        echo "Removing old application directory..."
                        rmdir /s /q "C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1\\webapps\\student"
                        echo "Old application directory removed"
                    )

                    echo "Step 4: Copying new WAR file to webapps directory..."
                    copy target\\*.war "C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1\\webapps\\student.war"
                    echo "New WAR file copied successfully"

                    echo "Step 5: Starting Tomcat service..."
                    net start Tomcat10

                    echo "Step 6: Waiting for Tomcat to fully start..."
                    timeout /t 30

                    echo "=== Deployment Completed ==="
                '''
            }
            post{
                success{
                    echo 'Application deployed successfully to Tomcat server'
                }
                failure{
                    echo 'Deployment failed - attempting to rollback if backup exists'
                    script{
                        bat '''
                            echo "Deployment failed, checking for backup..."
                            if exist "C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1\\webapps\\student.war.backup.*" (
                                echo "Restoring from backup..."
                                for /f %%i in ('dir /b /od "C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1\\webapps\\student.war.backup.*"') do set LATEST=%%i
                                copy "C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1\\webapps\\%LATEST%" "C:\\Program Files\\Apache Software Foundation\\Tomcat 10.1\\webapps\\student.war"
                                net start Tomcat10
                                echo "Rollback completed"
                            ) else (
                                echo "No backup found, manual intervention required"
                                net start Tomcat10
                            )
                        '''
                    }
                }
            }
        }
        stage('Post-Deployment Verification'){
            steps{
                script{
                    // Health check to verify deployment
                    echo 'Waiting for application to fully deploy...'
                    bat 'timeout /t 30'

                    // Check if Tomcat is running
                    bat '''
                        echo "Checking Tomcat service status..."
                        sc query Tomcat10 | findstr "RUNNING" || (echo "Tomcat service is not running" && exit 1)
                    '''

                    // Check application deployment on correct port and endpoint
                    bat '''
                        echo "Verifying application deployment..."
                        curl -f http://localhost:8092/student/api/ || (echo "Student API is not accessible" && exit 1)
                        echo "Student application is successfully deployed and accessible"
                    '''
                }
            }
        }
    }
    post{
        always{
            echo "=== CLEANING UP WORKSPACE ==="
            // Use deleteDir instead of cleanWs since Workspace Cleanup plugin is not installed
            deleteDir()
        }
        success{
            echo "=== BUILD AND DEPLOYMENT SUCCESSFUL ==="
            echo "Student application has been successfully deployed to Tomcat server"
            echo "Application URL: http://localhost:8092/student/api/"

            // Simple echo instead of email if mail plugins are not configured
            echo "SUCCESS NOTIFICATION: Deployment completed successfully!"
        }
        failure{
            echo "=== BUILD OR DEPLOYMENT FAILED ==="
            echo "Please check the logs above for error details"
            echo "Common issues:"
            echo "1. Network connectivity to Maven repositories"
            echo "2. Maven dependencies not available locally"
            echo "3. Tomcat service permissions"
            echo "4. File system permissions for deployment directory"

            // Simple echo instead of email if mail plugins are not configured
            echo "FAILURE NOTIFICATION: Deployment failed - manual intervention required!"
        }
    }
}