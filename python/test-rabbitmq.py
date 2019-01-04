#!/usr/bin/env python

"""A python Script for Testing the Functionality of RabbitMQ

Usage:
   test-rabbitmq.py --message=message --host=host --port=port --vhost=vhost --username=username --password=password
   test-rabbitmq.py (-h | --help)

Options:
   --message=message        The Message to be sent and received
   --host=host              The RabbitMQ host
   --port=port              The RabbitMQ port
   --vhost=vhost            The RabbitMQ Virtual Host
   --username=username      The RabbitMQ Username
   --password=password      The RabbitMQ Password
   -h --help                Show this screen.
"""

from docopt import docopt
import pika
import requests
import sys
import multiprocessing
import time

# TODO ...
# def receive(ch, method, properties, body):
#    print("Message Received: {0}".format(body))

# TODO ...
def receive(host, port, vhost, username, password):

    credentials = pika.PlainCredentials(username, password)
    parameters = pika.ConnectionParameters(
       host=host,
       port=port,
       virtual_host=vhost,
       credentials=credentials)

    rabbit_queue = 'my-rabbit-queue'  # now-forced

    connection = pika.BlockingConnection(parameters)
    channel = connection.channel()
    channel.queue_declare(queue=rabbit_queue)

#    channel.basic_consume(receive,
#                          queue=rabbit_queue,
#                          no_ack=True)

#    channel.start_consuming()
#    connection.close()


def send(message, host, port, vhost, username, password):

    credentials = pika.PlainCredentials(username, password)
    parameters = pika.ConnectionParameters(
       host=host,
       port=port,
       virtual_host=vhost,
       credentials=credentials)

    rabbit_queue = 'my-rabbit-queue' # now-forced

    connection = pika.BlockingConnection(parameters)
    channel = connection.channel()
    channel.queue_declare(queue=rabbit_queue)

    channel.basic_publish(
       exchange='',
       routing_key=rabbit_queue,
       body=message)

    connection.close()


if __name__ == '__main__':
    arguments = docopt(__doc__)

    print("Running: {0}".format(sys.argv[0]))
    print("These Are Your Arguments: {}".format(arguments))

    #print("Testing HTTP Get (200=OK) Via Request On http://{0}:{1}".format(
    #    arguments['--host'],
    #    arguments['--port'])
    #)
    #r = requests.get("http://{0}:{1}/".format(
    #    arguments['--host'],
    #    arguments['--port']))
    #print("Status via HTTP: {0}".format(r.status_code))

    print("Sending Message {0} to RabbitMQ Via Advanced Message Queueing Protocol".format(arguments['--message']))
    send(arguments['--message'],
         arguments['--host'],
         arguments['--port'],
         arguments['--vhost'],
         arguments['--username'],
         arguments['--password'])

