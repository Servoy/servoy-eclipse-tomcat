pipeline {
    agent any
    
    options {
        quietPeriod(120)
        // Log-rotator instellingen overgenomen uit de oude XML (max 10 dagen/10 builds)
        buildDiscarder(logRotator(daysToKeepStr: '10', numToKeepStr: '10'))
    }
    
   triggers {
        GenericTrigger(
            genericVariables: [
                [key: 'ref', value: '$.ref']
            ],
            token: 'tomcat',
            regexpFilterText: '$ref',
            regexpFilterExpression: "^refs/heads/${env.BRANCH}\$"
        )
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
        stage('Build with Tycho') {
            steps {
                configFileProvider([
                    configFile(fileId: 'master_mvn_repo', variable: 'MAVEN_SETTINGS'),
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
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Failed', adaptiveCards: true
        }
        
        unstable {
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Unstable', adaptiveCards: true
        }
        
        fixed {
            office365ConnectorSend webhookUrl: TEAMS_WEBHOOK, status: 'Back to Normal', adaptiveCards: true
        }
    }
}