// Trusted repository builds on a node with Java 21, Git, Docker and a browser.
def command(String value) {
    if (isUnix()) { sh value } else { bat value }
}

def maven(String arguments) {
    command(isUnix() ? "sh ./mvnw -B ${arguments}" : "mvnw.cmd -B ${arguments}")
}

pipeline {
    agent { label 'qa-local' }
    options {
        disableConcurrentBuilds()
        timeout(time: 25, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '5'))
    }
    parameters {
        choice(name: 'BROWSER', choices: ['chrome', 'firefox'], description: 'Headless Web browser')
    }
    environment {
        HEADLESS = 'true'
        // Separate from the normal local fixture on 55432.
        DB_PORT = '55433'
        DB_HOST = '127.0.0.1'
        COMPOSE_PROJECT_NAME = "qa-jenkins-${BUILD_NUMBER}"
    }
    stages {
        stage('Check environment') {
            steps {
                script {
                    command('java -version')
                    command('docker info')
                    command('docker compose version')
                }
                dir('test-output/screenshots') { deleteDir() }
                script { maven('clean test-compile') }
            }
        }
        stage('Start database') {
            steps {
                script { command('docker compose -p ' + env.COMPOSE_PROJECT_NAME +
                    ' -f docker/docker-compose.yml up -d --wait --wait-timeout 120') }
            }
        }
        stage('API tests') {
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE', catchInterruptions: false) {
                    script { maven('-Dgroups=api -Dsurefire.reportsDirectory=target/api-reports test') }
                }
            }
        }
        stage('Database tests') {
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE', catchInterruptions: false) {
                    script { maven('-Dgroups=database -Dsurefire.reportsDirectory=target/database-reports test') }
                }
            }
        }
        stage('Web tests') {
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE', catchInterruptions: false) {
                    script { maven('-Pparallel-web -Dbrowser=' + params.BROWSER +
                        ' -Dheadless=true -Dsurefire.reportsDirectory=target/web-reports test') }
                }
            }
        }
    }
    post {
        always {
            // Keep running other cleanup/report steps while preserving any failure.
            catchError(buildResult: 'FAILURE', catchInterruptions: false) {
                script { maven('allure:report') }
            }
            catchError(buildResult: 'FAILURE', catchInterruptions: false) {
                junit testResults: 'target/api-reports/TEST-*.xml,target/database-reports/TEST-*.xml,target/web-reports/TEST-*.xml',
                    allowEmptyResults: false
            }
            catchError(buildResult: 'FAILURE', catchInterruptions: false) {
                script { command('docker compose -p ' + env.COMPOSE_PROJECT_NAME +
                    ' -f docker/docker-compose.yml logs --no-color > postgres.log') }
            }
            catchError(buildResult: 'FAILURE', catchInterruptions: false) {
                archiveArtifacts artifacts: 'target/*-reports/**,target/allure-results/**,target/site/allure-maven-plugin/**,test-output/screenshots/**,postgres.log',
                    allowEmptyArchive: true
            }
        }
        cleanup {
            // This uniquely named build fixture is disposable; normal local data is untouched.
            script { command('docker compose -p ' + env.COMPOSE_PROJECT_NAME +
                ' -f docker/docker-compose.yml down -v --remove-orphans') }
        }
    }
}
