#!/usr/bin/env python3
"""
Fake Pump Device - Giả lập máy bơm nhận lệnh MQTT
"""

import paho.mqtt.client as mqtt
import json
import time
from datetime import datetime

BROKER = "localhost"
PORT = 1883
DEVICE_ID = "PUMP-0001"

pump_state = "OFF"

# ✅ Callback kiểu cũ (API Version 1)
def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("✅ Kết nối thành công")
        client.subscribe(f"device/{DEVICE_ID}/control")
        print(f"📡 Đang lắng nghe topic: device/{DEVICE_ID}/control")
        
        feedback = {
            "deviceId": DEVICE_ID,
            "status": "ONLINE",
            "state": pump_state,
            "timestamp": datetime.now().isoformat()
        }
        client.publish(f"device/{DEVICE_ID}/status", json.dumps(feedback))
        print(f"✅ Đã gửi status: ONLINE, state: {pump_state}\n")
    else:
        print(f"❌ Kết nối thất bại: {rc}")

def on_message(client, userdata, msg):  # ✅ Không có 'properties'
    global pump_state
    print(f"\n{'='*60}")
    print(f"📥 NHẬN LỆNH TỪ BACKEND")
    print(f"{'='*60}")
    print(f"📍 Topic: {msg.topic}")
    
    try:
        payload = json.loads(msg.payload.decode())
        print(f"📦 Payload:")
        print(json.dumps(payload, indent=2, ensure_ascii=False))
        
        action = payload.get("action", "").upper()
        
        if action == "TURN_ON" or action == "ON":
            duration = payload.get("duration", 60)
            pump_state = "ON"
            print(f"\n💧 BẬT MÁY BƠM")
            print(f"⏱️  Thời gian: {duration} giây")
            
            feedback = {
                "deviceId": DEVICE_ID,
                "status": "ONLINE",
                "state": "ON",
                "duration": duration,
                "timestamp": datetime.now().isoformat()
            }
            client.publish(f"device/{DEVICE_ID}/status", json.dumps(feedback))
            print(f"✅ Đã gửi trạng thái: MÁY BƠM ĐANG BẬT\n")
            
        elif action == "TURN_OFF" or action == "OFF":
            pump_state = "OFF"
            print(f"\n🛑 TẮT MÁY BƠM")
            
            feedback = {
                "deviceId": DEVICE_ID,
                "status": "ONLINE",
                "state": "OFF",
                "timestamp": datetime.now().isoformat()
            }
            client.publish(f"device/{DEVICE_ID}/status", json.dumps(feedback))
            print(f"✅ Đã gửi trạng thái: MÁY BƠM ĐÃ TẮT\n")
        else:
            print(f"⚠️  Lệnh không xác định: {action}\n")
            
    except Exception as e:
        print(f"❌ Lỗi xử lý message: {e}\n")

# ✅ Sử dụng API Version 1
client = mqtt.Client(callback_api_version=mqtt.CallbackAPIVersion.VERSION1)
client.on_connect = on_connect
client.on_message = on_message

print(f"{'='*60}")
print(f"🔌 FAKE PUMP DEVICE - {DEVICE_ID}")
print(f"{'='*60}")
print(f"🔗 Đang kết nối tới: {BROKER}:{PORT}...")

try:
    client.connect(BROKER, PORT, 60)
    print(f"⏳ Đang chờ lệnh điều khiển...\n")
    print(f"{'='*60}\n")
    client.loop_forever()
except KeyboardInterrupt:
    print(f"\n\n{'='*60}")
    print(f"👋 Dừng Fake Pump Device")
    print(f"{'='*60}\n")
    client.disconnect()
except Exception as e:
    print(f"❌ Lỗi: {e}")