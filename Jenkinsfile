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
                bat 'mvn clean package -DskipTests=true'
            }
            post{
                success{
                    echo 'Now Archiving the Artifacts'
                    archiveArtifacts artifacts: '**/target/*.war'
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

                    // Check application deployment
                    bat '''
                        echo "Verifying application deployment..."
                        curl -f http://localhost:8092/student/api/ || (echo "Student application is not accessible" && exit 1)
                        echo "Student application is successfully deployed and accessible"
                    '''
                }
            }
        }
    }
    post{
        always{
            cleanWs()
        }
        success{
            emailext(
                subject: "Deployment Successful: ${env.JOB_NAME} - Build ${env.BUILD_NUMBER}",
                body: "The application has been successfully deployed to Tomcat server.",
                to: "taran.duggal@teleperformancedibs.com"
            )
        }
        failure{
            emailext(
                subject: "Deployment Failed: ${env.JOB_NAME} - Build ${env.BUILD_NUMBER}",
                body: "The deployment to Tomcat server has failed. Please check the logs.",
                to: "taran.duggal@teleperformancedibs.com"
            )
        }
    }
}