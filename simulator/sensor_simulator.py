import paho.mqtt.client as mqtt
import json
import time
import random
from datetime import datetime

# MQTT Configuration
MQTT_BROKER = "localhost"
MQTT_PORT = 1883
MQTT_USERNAME = "admin"
MQTT_PASSWORD = "admin123"

# ✅ THÊM PUMP-0001 VÀO DANH SÁCH DEVICES
DEVICES = {
    "TEMP-0001": {
        "type": "temperature",
        "topic": "device/TEMP-0001/data",
        "farm_id": 1
    },
    "HUM-0001": {
        "type": "humidity", 
        "topic": "device/HUM-0001/data",
        "farm_id": 1
    },
    "SOIL-0001": {
        "type": "soil_moisture",
        "topic": "device/SOIL-0001/data",
        "farm_id": 1
    },
    "LIGHT-0001": {
        "type": "light_intensity",
        "topic": "device/LIGHT-0001/data",
        "farm_id": 1
    },
    # ✅ THÊM MỚI: Máy bơm với soil_moisture thấp để test rule
    "PUMP-0001": {
        "type": "soil_moisture",  # ⬅️ Giả lập độ ẩm đất thấp
        "topic": "device/PUMP-0001/data",
        "farm_id": 1
    }
}

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("✅ Connected to MQTT Broker!")
    else:
        print(f"❌ Failed to connect, return code {rc}")

def generate_sensor_data(device_id, device_info):
    """Tạo dữ liệu sensor ngẫu nhiên"""
    sensor_type = device_info["type"]
    
    # ✅ THÊM: Giả lập PUMP-0001 có độ ẩm đất thấp (< 30%) để kích hoạt rule
    if device_id == "PUMP-0001":
        # Độ ẩm đất thấp để kích hoạt rule "Tưới nước tự động"
        soil_moisture = round(random.uniform(15.0, 28.0), 2)  # ⬅️ < 30%
        return {
            "device_id": device_id,
            "farm_id": device_info["farm_id"],
            "sensor_type": sensor_type,
            "soil_moisture": soil_moisture,
            "timestamp": int(datetime.now().timestamp() * 1000)
        }
    
    # ✅ GIỮ NGUYÊN: Logic cũ cho các sensor khác
    if sensor_type == "temperature":
        value = round(random.uniform(20.0, 35.0), 2)
        return {
            "device_id": device_id,
            "farm_id": device_info["farm_id"],
            "sensor_type": sensor_type,
            "temperature": value,
            "timestamp": int(datetime.now().timestamp() * 1000)
        }
    
    elif sensor_type == "humidity":
        value = round(random.uniform(40.0, 80.0), 2)
        return {
            "device_id": device_id,
            "farm_id": device_info["farm_id"],
            "sensor_type": sensor_type,
            "humidity": value,
            "timestamp": int(datetime.now().timestamp() * 1000)
        }
    
    elif sensor_type == "soil_moisture":
        value = round(random.uniform(25.0, 65.0), 2)
        return {
            "device_id": device_id,
            "farm_id": device_info["farm_id"],
            "sensor_type": sensor_type,
            "soil_moisture": value,
            "timestamp": int(datetime.now().timestamp() * 1000)
        }
    
    elif sensor_type == "light_intensity":
        value = round(random.uniform(1000.0, 5000.0), 2)
        return {
            "device_id": device_id,
            "farm_id": device_info["farm_id"],
            "sensor_type": sensor_type,
            "light_intensity": value,
            "timestamp": int(datetime.now().timestamp() * 1000)
        }

def main():
    client = mqtt.Client()
    client.username_pw_set(MQTT_USERNAME, MQTT_PASSWORD)
    client.on_connect = on_connect
    
    print(f"🔄 Connecting to MQTT Broker at {MQTT_BROKER}:{MQTT_PORT}...")
    client.connect(MQTT_BROKER, MQTT_PORT, 60)
    client.loop_start()
    
    print("\n📡 Starting sensor data simulation...")
    print(f"📊 Simulating {len(DEVICES)} devices: {', '.join(DEVICES.keys())}")
    print("⏱️  Sending data every 10 seconds\n")
    
    try:
        while True:
            for device_id, device_info in DEVICES.items():
                data = generate_sensor_data(device_id, device_info)
                topic = device_info["topic"]
                
                # Publish data
                result = client.publish(topic, json.dumps(data))
                
                # Log
                if result.rc == 0:
                    print(f"✅ [{datetime.now().strftime('%H:%M:%S')}] {device_id}: {data}")
                    
                    # ✅ THÊM: Highlight khi PUMP-0001 gửi dữ liệu độ ẩm thấp
                    if device_id == "PUMP-0001" and data.get("soil_moisture", 100) < 30:
                        print(f"   ⚠️  Độ ẩm thấp! Rule 'Tưới nước tự động' sẽ được kích hoạt!")
                else:
                    print(f"❌ Failed to send data for {device_id}")
            
            time.sleep(10)  # Gửi mỗi 10 giây
            
    except KeyboardInterrupt:
        print("\n⏹️  Stopping simulation...")
        client.loop_stop()
        client.disconnect()
        print("✅ Disconnected from MQTT Broker")

if __name__ == "__main__":
    main()