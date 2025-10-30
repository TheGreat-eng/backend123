# File: ctest.data/test_sensor.py

import paho.mqtt.client as mqtt
import json
import time
from datetime import datetime

BROKER = "localhost"
PORT = 1883
DEVICE_ID = "SOIL-0001" # Thiết bị cần kích hoạt rule
TOPIC = f"sensor/{DEVICE_ID}/data"

client = mqtt.Client()

def connect_mqtt():
    client.connect(BROKER, PORT, 60)
    print(f"Connected to MQTT Broker at {BROKER}:{PORT}")

def publish_data():
    # ✅ SỬA LẠI PAYLOAD CHO ĐÚNG CHUẨN camelCase
    payload = {
        "deviceId": DEVICE_ID,
        "sensorType": "SOIL_MOISTURE",
        "soilMoisture": 19.0, # Giá trị để kích hoạt rule (< 25)
        # Thêm các giá trị khác nếu cần, nhưng không bắt buộc cho test này
        "timestamp": datetime.now().isoformat()
    }
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
    
    print("\n!!! KÍCH HOẠT RULE ENGINE TRONG 30 GIÂY TỚI. HÃY QUAN SÁT LOG BACKEND !!!\n")
    publish_data()
    
    time.sleep(1)
    client.loop_stop()