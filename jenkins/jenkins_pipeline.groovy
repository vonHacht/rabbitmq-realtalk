#!/usr/bin/env groovy

/*
    Settings
*/

def host = "192.168.0.4"
def port = "1337"
def username = "guest"
def password = "guest"
def nodeLabel = "utility-slave"
def clusterName = "rabbit@my-rabbit"


class RabbitMQ {

   def dslFactory
   def host
   def port
   def username
   def password
   def clusterName
   def nodeLabel

   enum httpAction {
      GET, PUT, DELETE, POST
   }

   private def actionToCurlHttp(action) {

      def value = '-XPUT'

      switch (action) {
         case httpAction.GET:
            value = '-XGET'
            break
         case httpAction.DELETE:
            value = '-XDELETE'
            break
         case httpAction.POST:
            value = '-XPOST'
            break
         case httpAction.PUT:
         default:
            break
      }

      return value
   }

   private def getClusterName() {
      /*
         TODO: It should be possible to figure out the cluster name by calling RabbitMQ
         def name='curl -i -u ${username}:${password} http://${host}:${port}/api/cluster-name'.execute().text 
      */
      return "${clusterName}"
   }

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
                                        curl --silent -i -u "\$CREDENTIALS" "\$ADRESS"
                                    '''
                                }
                            }
                        }
                   """)
               }
           }
       }
   }

   private def createJobTemplate( pipelineJobName, parameterMap, api, json, action ) {
       dslFactory.pipelineJob("${pipelineJobName}") {

          def clusterName = this.getClusterName()
          def actionToCurl = this.actionToCurlHttp(action)

          json =  """def json = """ + """\'${json}\'""" + """\n"""
          json += """json = json.replaceAll(/("node"):(".*?")/, /"node":"\${clusterName}"/)\n"""

          parameters {
             if(parameterMap['vhost'] == true)
             {
                stringParam('VHOST_NAME', '', 'Virtual Host Name' )
             }
             if(parameterMap['exchangename'] == true)
             {
                stringParam('EXCHANGE_NAME', '', 'Exchange Name' )
             }
             if(parameterMap['exchangetype'] == true)
             {
                choiceParam('EXCHANGE_TYPE', ['direct', 'fanout', 'topic', 'headers'], 'Type of Exchange')
                json += """json = json.replaceAll(/("type"):(".*?")/, /"type":"\${EXCHANGE_TYPE}"/)\n"""
             }
             if(parameterMap['queue'] == true)
             {
                stringParam('QUEUE_NAME', '', 'Queue Name')
                json += """json = json.replaceAll(/("routing_key"):(".*?")/, /"routing_key":"\${QUEUE_NAME}"/)\n"""
             }
             if(parameterMap['message'] == true)
             {
                stringParam('MESSAGE', '', 'Message to Publish on Exchange')
                json += """json = json.replaceAll(/("payload"):(".*?")/, /"payload":"\${MESSAGE}"/)"""
             }
         }

         definition {
            cps {
               sandbox()

               script("""
                  def api = "${api}"
                  def addess = "http://${host}:${port}/\${api}"
                  def credentials = "${username}:${password}"
                  def contentType = "content-type:application/json"
                  def clusterName = "${clusterName}"
                  def action = ${action} 
                  ${json}

                  if("\${json}" != "") {
                     json = "-d\${json}"
                     println("JSON: \${json}")
                  }

                  node("${nodeLabel}") {
                     stage('Executes Curl') {
                        withEnv (["ADRESS=\${addess}", "CREDENTIALS=\${credentials}", "CONTENTTYPE=\${contentType}", "JSON=\${json}"]) {
                           sh script: '''
                              curl --silent -i -u "\$CREDENTIALS" -H "\$CONTENTTYPE" ${actionToCurl} "\$JSON" "\$ADRESS"
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
         queue: false,
         message: false
         ]

      def vhostApi = "api/vhosts/\${VHOST_NAME}"
      def vhostJson = ""

     this.createJobTemplate("RabbitMQ - create new Vhost (${host})", vhostMap, vhostApi, vhostJson, httpAction.PUT)
   }

   def createExchangeJob() {
      def exchangeMap = [
         vhost: true, 
         exchangename: true, 
         exchangetype: true, 
         queue: false,
         message: false
         ]

      def exchangeApi = "api/exchanges/\${VHOST_NAME}/\${EXCHANGE_NAME}"
      def exchangeJson = "{\"type\":\"\",\"auto_delete\":false,\"durable\":true,\"internal\":false,\"arguments\":{}}"

      this.createJobTemplate("RabbitMQ - create new Exchange (${host})", exchangeMap, exchangeApi, exchangeJson, httpAction.PUT)
   }

   def createQueueJob() {
      def queueMap = [
         vhost: true, 
         exchangename: false, 
         exchangetype: false, 
         queue: true,
         message: false
         ]

      def queueApi = "api/queues/\${VHOST_NAME}/\${QUEUE_NAME}"
      def queueJson = "{\"auto_delete\":false,\"durable\":true,\"arguments\":{},\"node\":\"\"}"

      this.createJobTemplate("RabbitMQ - create new Queue (${host})", queueMap, queueApi, queueJson, httpAction.PUT)
   }

   def createPublishJob() {
      def publishMap = [
         vhost: true, 
         exchangename: true, 
         exchangetype: false, 
         queue: true,
         message: true
         ]

      def publishApi = "api/exchanges/\${VHOST_NAME}/\${EXCHANGE_NAME}/publish"
      def publishJson = "{\"properties\":{},\"routing_key\":\"\",\"payload\":\"\",\"payload_encoding\":\"string\"}"

      this.createJobTemplate("RabbitMQ - Publish Message On Exchange (${host})", publishMap, publishApi, publishJson, httpAction.POST )
   }
}

// create class
def RabbitMQ = new RabbitMQ(
    dslFactory     : this,
    host           : host,
    port           : port,
    username       : username,
    password       : password,
    clusterName    : clusterName,
    nodeLabel      : nodeLabel
)

// create jobs
RabbitMQ.listOverviewJob()
RabbitMQ.listClusterNameJob()
RabbitMQ.listNodeJob()
RabbitMQ.listVhostsJob()
RabbitMQ.listChannelJob()
RabbitMQ.createVhostJob()
RabbitMQ.createExchangeJob()
RabbitMQ.createQueueJob()
RabbitMQ.createPublishJob()
