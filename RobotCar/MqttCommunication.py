import paho.mqtt.client as mqtt
import serial
import threading


# The callback for when the client receives a CONNACK response from the server.
def on_connect(client, userdata, flags, rc):
    print("Connected with result code " + str(rc))

    # Subscribing in on_connect() means that if we lose the connection and
    # reconnect then subscriptions will be renewed.
    client.subscribe("lap089/motor", qos=2)


# The callback for when a PUBLISH message is received from the server.
def on_message(client, userdata, msg):
    print(msg.topic + " " + str(msg.payload))
    if ser.isOpen():
        print('Write command...')
        ser.write(msg.payload + '#')
    else:
        print("Cannot open port")

    client.publish("lap089/test", msg.payload + " - Received!", qos=2)


def on_disconnect(client, userdata, rc):
    if rc != 0:
        print("Unexpected disconnection.")


def handle_data(data, client):
    print('Receive: ', data)
    client.publish("lap089/speed", data, qos=2)


def read_from_port(ser, client):
    print("Start reading from arduino...")
    while True:
        line = ser.readline()
        if line:
            handle_data(line, client)



ser = serial.Serial('/dev/ttyUSB0', baudrate=115200, xonxoff=True, timeout=0)
ser.open()

client = mqtt.Client()
client.on_connect = on_connect
client.on_message = on_message
client.on_disconnect = on_disconnect

client.connect("broker.mqttdashboard.com", 1883, 60)

thread = threading.Thread(target=read_from_port, args=(ser, client))
thread.start()

# Blocking call that processes network traffic, dispatches callbacks and
# handles reconnecting.
# Other loop*() functions are available that give a threaded interface and a
# manual interface.
client.loop_forever()
