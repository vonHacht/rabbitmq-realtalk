#!/usr/bin/env python
import pika
import requests


r = requests.get('http://localhost:1337/')
print("Status code from request: {0}".format(r.status_code))

credentials = pika.PlainCredentials('guest', 'guest')
parameters = pika.ConnectionParameters('localhost', , '/', credentials)

connection = pika.BlockingConnection(parameters)
#channel = connection.channel()

#channel.queue_declare(queue='hello')

#channel.basic_publish(exchange='',
#                      routing_key='hello',
#                      body='Hello World!')
#print(" [x] Sent 'Hello World!'")

#connection.close()