#!/usr/bin/env groovy

/*
    Settings
*/

def host = "192.168.0.3"
def port = "1337"
def username = "guest"
def password = "guest"
def nodeLabel = "utility-slave"


class RabbitMQ {

   def dslFactory
   def host
   def port
   def username
   def password
   def nodeLabel

   private def listJobTemplate(pipelineJobName, api) {
       dslFactory.pipelineJob("${pipelineJobName}") {
           definition {
               cps {
                   sandbox()

                   script("""
                        def api = "${api}"
                        def addess = "http://${host}:${port}/\${api}"
                        def credentials = "${username}:${password}"

                        node("${nodeLabel}") {
                            stage("Conduction listings") {
                                withEnv (["ADRESS=\${addess}", "CREDENTIALS=\${credentials}"]) {
                                    sh script: '''
                                        curl -i -u "\$CREDENTIALS" "\$ADRESS"
                                    '''
                                }
                            }
                        }
                   """)
               }
           }
       }
   }

   private def createJobTemplate(pipelineJobName, parameterMap, api, json) {
       dslFactory.pipelineJob("${pipelineJobName}") {
          parameters {
             if(parameterMap['vhost'] == true)
             {
                stringParam('VHOST_NAME', '', 'Virtual Host Name' )
             }
             if(parameterMap['exchangename'] == true)
             {
                stringParam('EXCHANGE_NAME', '', 'Exchange Name' )
             }
             //if(parameterMap['exchangetype'] == true)
             //{
                // TODO: A Switch case below is dependent on that EXCHANGE_TYPE exists, however not all jobs needs this variable
                choiceParam('EXCHANGE_TYPE', ['Direct exchange', 'Fanout exchange', 'Topic exchange', 'Headers exchange'], 'Type of Exchange')
             //}
             if(parameterMap['queue'] == true)
             {
                stringParam('QUEUE_NAME', '', 'Queue Name')
             }
         }

         definition {
            cps {
               sandbox()

               script("""

                  def exchangeType = ''

                  switch ("\${EXCHANGE_TYPE}") {
                     case 'Direct exchange':
                        exchangeType = 'direct'
                        break
                     case 'Fanout exchange':
                        exchangeType = 'fanout'
                        break
                     case 'Topic exchange':
                        exchangeType = 'topic'
                        break
                     case 'Headers exchange':
                        exchangeType = 'headers'
                        break
                     default:
                        break
                  }

                  def api = "${api}"
                  def addess = "http://${host}:${port}/\$api"
                  def credentials = "${username}:${password}"
                  def contentType = "content-type:application/json"
                  def json = ""

                  if("${json}") {
                     json = "-d${json}"
                  }

                  node("${nodeLabel}") {
                     stage('Creates a new Virtual Host') {
                        withEnv (["ADRESS=\${addess}", "CREDENTIALS=\${credentials}", "CONTENTTYPE=\${contentType}", "JSON=\${json}"]) {
                           sh script: '''
                              curl -i -u "\$CREDENTIALS" -H "\$CONTENTTYPE" -XPUT "\$JSON" "\$ADRESS"
                           '''
                        }
                     }
                  }
                """)
               }
            }
         }
   }

   def listOverviewJob() {
       this.listJobTemplate("RabbitMQ - list overview (${host})", "api/cluster-name")
   }

   def listClusterNameJob() {
       this.listJobTemplate("RabbitMQ - list cluster-name (${host})", "api/cluster-name")
   }

   def listNodeJob() {
       this.listJobTemplate("RabbitMQ - list all nodes (${host})", "api/nodes")
    }

   def listVhostsJob() {
      this.listJobTemplate("RabbitMQ - list of Virtual Hosts (${host})", "api/vhosts")
   }

   def listChannelJob() {
      def api = "api/channels?sort=message_stats.publish_details.rate&sort_reverse=true&columns=name,message_stats.publish_details.rate,message_stats.deliver_get_details.rate"
      this.listJobTemplate("RabbitMQ - list of Channels (${host})", api)
   }

   def createVhostJob() {
      def vhostMap = [
         vhost: true, 
         exchangename: false, 
         exchangetype: false, 
         queue: false
         ]

      def vhostApi = "api/vhosts/\${VHOST_NAME}"
      def vhostJson = ""

     this.createJobTemplate("RabbitMQ - create new Vhost (${host})", vhostMap, vhostApi, vhostJson)
   }

   def createExchangeJob() {
      def exchangeMap = [
         vhost: true, 
         exchangename: true, 
         exchangetype: true, 
         queue: false
         ]
         
      def exchangeApi = "api/exchanges/\${VHOST_NAME}/\${EXCHANGE_NAME}"
      def exchangeJson = "{\\"type\\":\\"\${exchangeType}\\",\\"auto_delete\\":false,\\"durable\\":true,\\"internal\\":false,\\"arguments\\":{}}"

      this.createJobTemplate("RabbitMQ - create new Exchange (${host})", exchangeMap, exchangeApi, exchangeJson)
   }

   def createQueueJob() {} 

   def createPublishJob() {} 
}

// create class
def RabbitMQ = new RabbitMQ(
    dslFactory     : this,
    host           : host,
    port           : port,
    username       : username,
    password       : password,
    nodeLabel      : nodeLabel,
)

// create jobs
RabbitMQ.listOverviewJob()
RabbitMQ.listClusterNameJob()
RabbitMQ.listNodeJob()
RabbitMQ.listVhostsJob()
RabbitMQ.listChannelJob()
RabbitMQ.createVhostJob()
RabbitMQ.createExchangeJob()
