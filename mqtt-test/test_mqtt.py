#!/usr/bin/env python3
"""
Script kiểm tra linh hoạt các kịch bản gửi email.
Chỉ cần bỏ comment kịch bản bạn muốn chạy.
"""

import paho.mqtt.client as mqtt
import json
import time
from datetime import datetime

# ==================== CONFIG ====================
BROKER = "localhost"
PORT = 1883

# ==================== CHỌN KỊCH BẢN TEST ====================
# Bỏ comment (xóa dấu #) ở kịch bản bạn muốn chạy
# và comment lại các kịch bản khác.

# --- Kịch bản 1: Test Email theo Quy tắc (Đất khô) ---
# SCENARIO = {
#     "name": "Kịch bản 1: Test Email theo Quy tắc Tự động",
#     "device_id": "SOIL-TEST-01",
#     "payload": {
#         "deviceId": "SOIL-TEST-01",
#         "sensorType": "SOIL_MOISTURE",
#         "soilMoisture": 19.5,  # < 25 -> Kích hoạt rule
#         "temperature": 28.0
#     }
# }

# --- Kịch bản 2: Test Email Cảnh báo Sức khỏe Cây (Nguy cơ nấm) ---
# SCENARIO = {
#     "name": "Kịch bản 2: Test Email Cảnh báo Sức khỏe Cây",
#     "device_id": "DHT-PLANT-TEST",
#     "payload": {
#         "deviceId": "DHT-PLANT-TEST",
#         "sensorType": "DHT22",
#         "temperature": 26.5,
#         "humidity": 92.8 # > 85 -> Kích hoạt cảnh báo
#     }
# }

# --- Kịch bản 4: Test Email Cảnh báo Tức thời (Nhiệt độ cao) ---
SCENARIO = {
    "name": "Kịch bản 4: Test Email Cảnh báo Tức thời từ Cảm biến",
    "device_id": "DHT-REALTIME-TEST",
    "payload": {
        "deviceId": "DHT-REALTIME-TEST",
        "sensorType": "DHT22",
        "temperature": 41.0, # > 38 -> Kích hoạt cảnh báo
        "humidity": 35.0
    }
}

# ==================== LOGIC GỬI TIN ====================
def run_test(scenario):
    print("\n" + "="*70)
    print(f"🔧 Đang chạy: {scenario['name']}")
    print("="*70)

    device_id = scenario['device_id']
    topic = f"sensor/{device_id}/data"
    payload = scenario['payload']
    
    # Thêm timestamp nếu chưa có
    if 'timestamp' not in payload:
        payload['timestamp'] = datetime.now().isoformat()

    try:
        client = mqtt.Client(callback_api_version=mqtt.CallbackAPIVersion.VERSION1)
        client.connect(BROKER, PORT, 60)
        print(f"✅ Đã kết nối tới MQTT Broker: {BROKER}:{PORT}")

        client.loop_start()
        time.sleep(1)
        
        print(f"📤 Đang gửi dữ liệu tới topic: {topic}")
        print("   Payload:", json.dumps(payload, indent=2))
        client.publish(topic, json.dumps(payload), qos=1)
        
        time.sleep(1)
        client.loop_stop()
        client.disconnect()
        print("✅ Đã gửi tin nhắn và ngắt kết nối.")
        print("--------------------------------------------------")

    except Exception as e:
        print(f"\n❌ LỖI: {e}")
        print("\n🔧 KHẮC PHỤC:")
        print("   1. Đảm bảo Docker và container 'smartfarm-mosquitto' đang chạy.")
        print("   2. Kiểm tra lại địa chỉ Broker và Port.\n")

if __name__ == '__main__':
    run_test(SCENARIO)