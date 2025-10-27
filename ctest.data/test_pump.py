import paho.mqtt.client as mqtt
import json

BROKER = "localhost"
PORT = 1883
DEVICE_ID = "PUMP-001" # Phải khớp với deviceId trong Rule
TOPIC_CONTROL = f"device/{DEVICE_ID}/control"

def on_connect(client, userdata, flags, rc):
    print("Pump connected to MQTT.")
    client.subscribe(TOPIC_CONTROL)
    print(f"Pump is listening on topic: {TOPIC_CONTROL}")

def on_message(client, userdata, msg):
    print(f"\n>>>> PUMP RECEIVED COMMAND <<<<")
    print(f"From topic: {msg.topic}")
    payload = json.loads(msg.payload.decode())
    print(f"Payload: {json.dumps(payload, indent=2)}")
    print("================================\n")

client = mqtt.Client()
client.on_connect = on_connect
client.on_message = on_message
client.connect(BROKER, PORT, 60)
client.loop_forever()