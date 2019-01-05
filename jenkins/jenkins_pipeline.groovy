/*
    Jenkins Pipeline
*/

pipelineJob("RabbitMQ Pipeline Job") {
    agent any 
    stages {
        stage('Get List of Vhost') { 
            steps {
                sh'''
                    curl -i -u guest:guest http://localhost:15672/api/vhosts
                ''' 
            }
        }
    }
}
