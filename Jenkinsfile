pipeline {
    agent any
    
    options {
        quietPeriod(120)
        // Log-rotator instellingen overgenomen uit de oude XML (max 10 dagen/10 builds)
        buildDiscarder(logRotator(daysToKeepStr: '10', numToKeepStr: '10'))
    }
    
    triggers {
        githubPush()
    }
    
    parameters {
        string(name: 'goals', defaultValue: 'install', trim: false)
    }
    
    environment {
        TEAMS_WEBHOOK = credentials('servoy-teams-webhook')
    }
    
    tools {
        jdk 'Java 21' // Uniform rechtgetrokken naar Java 21
        maven 'Maven 3.9.16'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build with Tycho 5') {
            steps {
                configFileProvider([
                    configFile(fileId: 'ba7b9372-76e5-4898-a2be-1dde60a0d6e3', variable: 'MAVEN_SETTINGS'),
                    configFile(fileId: 'maven_toolchain', variable: 'TOOLCHAIN')
                ]) {
                    sh 'mvn -B -s "$MAVEN_SETTINGS" -t "$TOOLCHAIN" $goals'
                }
            }
        }
    }
    
    post {
        failure {
            // Teams notificatie zonder quotes om security-waarschuwingen te voorkomen
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Failed'
        }
        
        unstable {
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Unstable'
        }
        
        fixed {
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Back to Normal'
        }
    }
}