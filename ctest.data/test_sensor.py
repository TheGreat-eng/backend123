import paho.mqtt.client as mqtt
import json
import time
from datetime import datetime

BROKER = "localhost"
PORT = 1883
# DEVICE_ID = "DHT-001" # Phải khớp với deviceId bạn đã tạo ở Luồng 2
# TOPIC = f"sensor/{DEVICE_ID}/data"
DEVICE_ID = "SOIL-001" # Thay đổi
TOPIC = f"sensor/{DEVICE_ID}/data"

client = mqtt.Client()

def connect_mqtt():
    client.connect(BROKER, PORT, 60)
    print(f"Connected to MQTT Broker at {BROKER}:{PORT}")

def publish_data():
    payload = {
        "deviceId": DEVICE_ID,
        "sensorType": "SOIL_MOISTURE",
        "lightIntensity": 310,  # ✅ THÊM lightIntensity
        "soilMoisture": 25.0,
        "temperature": 28.5,
        "humidity": 67.0,
        "soilPH": 5,  # ✅ THÊM soilPH BẤT THƯỜNG
        "timestamp": datetime.now().isoformat()
    }
    # payload = {
    #     "deviceId": DEVICE_ID,
    #     "sensorType": "SOIL_MOISTURE",
    #     "soilMoisture": 19.5, # Giá trị để kích hoạt rule
    #     "temperature": 28.5,
    #     "humidity": 67.0,
    #     "timestamp": datetime.now().isoformat()
    # }
    payload_json = json.dumps(payload)
    result = client.publish(TOPIC, payload_json)
    
    if result[0] == 0:
        print(f"Sent `{payload_json}` to topic `{TOPIC}`")
    else:
        print(f"Failed to send message to topic {TOPIC}")

if __name__ == '__main__':
    connect_mqtt()
    client.loop_start()
    time.sleep(1)
    publish_data()
    time.sleep(1)
    client.loop_stop()