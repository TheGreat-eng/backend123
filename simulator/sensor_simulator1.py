#!/usr/bin/env python3
"""
Smart Farm – IoT Sensor Simulator (match device list in UI)
Devices:
  - DHT22-0001 (DHT22)
  - DHT22-0002 (DHT22)
  - SOIL-0001  (SOIL_MOISTURE)
  - SOIL-0002  (SOIL_MOISTURE)
  - LIGHT-0001 (LIGHT)
  - PH-0001    (PH)
"""

import json
import time
import random
import math
from datetime import datetime
from typing import Dict, Any
import paho.mqtt.client as mqtt
import os

FARM_ID = 1  # khớp với backend đang filter farm_id=1

class SensorSimulator:
    def __init__(self, broker_host="localhost", broker_port=1883, username=None, password=None):
        self.client = mqtt.Client(client_id=f"simulator_{random.randint(1000, 9999)}")
        if username:
            self.client.username_pw_set(username=username, password=password)
        self.broker_host = broker_host
        self.broker_port = broker_port
        self.connected = False

        # trạng thái mô phỏng
        self.base_temperature = 28.0
        self.base_humidity = 65.0
        self.soil_moisture = 50.0
        self.light_intensity = 10000.0
        self.ph_level = 6.5

        self.start_time = time.time()

        # callbacks
        self.client.on_connect = self.on_connect
        self.client.on_disconnect = self.on_disconnect

    # ================= MQTT =================
    def on_connect(self, client, userdata, flags, rc):
        if rc == 0:
            print("✅ Connected to MQTT Broker!")
            self.connected = True
        else:
            print(f"❌ Failed to connect, return code {rc}")

    def on_disconnect(self, client, userdata, rc):
        print("⚠️  Disconnected from MQTT Broker")
        self.connected = False

    def connect(self):
        try:
            self.client.connect(self.broker_host, self.broker_port, 60)
            self.client.loop_start()
            # đợi kết nối
            deadline = time.time() + 10
            while not self.connected and time.time() < deadline:
                time.sleep(0.1)
            return self.connected
        except Exception as e:
            print(f"❌ Connection error: {e}")
            return False

    def disconnect(self):
        self.client.loop_stop()
        self.client.disconnect()

    # =============== SIM LOGIC ===============
    def get_time_factor(self) -> float:
        """Giả lập 24 giờ trong 24 phút (1 phút ~ 1 giờ)."""
        elapsed = time.time() - self.start_time
        hour_of_day = (elapsed / 60) % 24
        return hour_of_day

    def simulate_dht22(self, device_id: str) -> Dict[str, Any]:
        hour = self.get_time_factor()
        # Nhiệt độ tăng vào ban ngày, giảm ban đêm
        temp_variation = 5 * math.sin((hour - 6) * math.pi / 12)
        temperature = self.base_temperature + temp_variation + random.uniform(-1, 1)
        # Độ ẩm nghịch pha với nhiệt độ
        humidity = self.base_humidity - (temp_variation * 2) + random.uniform(-3, 3)
        humidity = max(30, min(95, humidity))
        return {
            "farmId": FARM_ID,
            "deviceId": device_id,
            "sensorType": "DHT22",
            "temperature": round(temperature, 2),
            "humidity": round(humidity, 2),
            "timestamp": datetime.now().isoformat()
        }

    def simulate_soil_moisture(self, device_id: str) -> Dict[str, Any]:
        # Ẩm đất giảm dần theo thời gian
        self.soil_moisture -= random.uniform(0.05, 0.15)
        # Sự kiện tưới ngẫu nhiên
        if random.random() < 0.02:
            self.soil_moisture += random.uniform(15, 25)
            print(f"💧 Irrigation event! Moisture -> {self.soil_moisture:.1f}%")
        self.soil_moisture = max(20, min(70, self.soil_moisture))
        return {
            "farmId": FARM_ID,
            "deviceId": device_id,
            "sensorType": "SOIL_MOISTURE",
            "soilMoisture": round(self.soil_moisture, 2),
            "timestamp": datetime.now().isoformat()
        }

    def simulate_light_sensor(self, device_id: str) -> Dict[str, Any]:
        hour = self.get_time_factor()
        if 6 <= hour <= 18:
            # sáng mạnh nhất khoảng 12h
            light_factor = math.sin((hour - 6) * math.pi / 12)
            self.light_intensity = 50000 * light_factor + random.uniform(-2000, 2000)
        else:
            self.light_intensity = random.uniform(0, 100)
        self.light_intensity = max(0, self.light_intensity)
        return {
            "farmId": FARM_ID,
            "deviceId": device_id,
            "sensorType": "LIGHT",
            "lightIntensity": round(self.light_intensity, 2),
            "timestamp": datetime.now().isoformat()
        }

    # def simulate_ph_sensor(self, device_id: str) -> Dict[str, Any]:
    #     self.ph_level += random.uniform(-0.02, 0.02)
    #     self.ph_level = max(5.5, min(7.5, self.ph_level))
    #     return {
    #         "farmId": FARM_ID,
    #         "deviceId": device_id,
    #         "sensorType": "PH",
    #         "ph": round(self.ph_level, 2),
    #         "timestamp": datetime.now().isoformat()
    #     }
    def simulate_ph_sensor(self, device_id: str) -> Dict[str, Any]:
    # pH dao động chậm trong khoảng 5.5–7.5
        self.ph_level += random.uniform(-0.02, 0.02)
        self.ph_level = max(5.5, min(7.5, self.ph_level))
        ph_val = round(self.ph_level, 2)

        return {
        # farmId không bắt buộc vì backend tự tìm từ Device → Farm
        "deviceId": device_id,
        "sensorType": "PH",     # không bắt buộc nhưng nên có
        "soilPH": ph_val,       # ✅ ĐÚNG TÊN TRƯỜNG BACKEND ĐANG ĐỌC
        "timestamp": datetime.now().isoformat()
    }

    # =============== PUBLISH ===============
    def publish_sensor_data(self, device_id: str, data: Dict[str, Any]):
        """
        Topic khớp kiểu: sensor/<DEVICE_ID>/data
        Payload: JSON (có farmId, deviceId, sensorType, ...).
        """
        topic = f"sensor/{device_id}/data"
        payload = json.dumps(data)
        res = self.client.publish(topic, payload, qos=1)
        if res.rc == mqtt.MQTT_ERR_SUCCESS:
            print(f"📤 {device_id}: {data.get('sensorType')} sent")
        else:
            print(f"❌ Publish failed for {device_id}")

    def publish_device_status(self, device_id: str, status: str):
        topic = f"device/{device_id}/status"
        payload = json.dumps({
            "farmId": FARM_ID,
            "deviceId": device_id,
            "status": status,
            "timestamp": datetime.now().isoformat()
        })
        self.client.publish(topic, payload, qos=1, retain=False)
        print(f"📡 {device_id} status -> {status}")

    # =============== RUN LOOP ===============
    def run_simulation(self, devices: list, interval: int = 10):
        print("\n" + "="*64)
        print("🌾 Smart Farm IoT Simulator (UI-matched device IDs)")
        print("="*64)
        print(f"Devices: {len(devices)} | Interval: {interval}s | Broker: {self.broker_host}:{self.broker_port}\n")

        if not self.connect():
            print("❌ Failed to connect to MQTT broker. Exiting…")
            return

        # gửi trạng thái ONLINE ban đầu
        for d in devices:
            self.publish_device_status(d["id"], "ONLINE")

        try:
            it = 0
            while True:
                it += 1
                hour = self.get_time_factor()
                print(f"\n--- Iteration {it} | Simulated {int(hour):02d}:00 ---")

                for d in devices:
                    t = d["type"]
                    if t == "DHT22":
                        data = self.simulate_dht22(d["id"])
                    elif t == "SOIL_MOISTURE":
                        data = self.simulate_soil_moisture(d["id"])
                    elif t == "LIGHT":
                        data = self.simulate_light_sensor(d["id"])
                    elif t == "PH":
                        data = self.simulate_ph_sensor(d["id"])
                    else:
                        continue
                    self.publish_sensor_data(d["id"], data)

                print(f"💤 Sleep {interval}s…")
                time.sleep(interval)

        except KeyboardInterrupt:
            print("\n🛑 Stopping simulator…")
            for d in devices:
                self.publish_device_status(d["id"], "OFFLINE")
            self.disconnect()
            print("✅ Stopped.")

def main():
    # Có thể đặt qua biến môi trường nếu cần
    BROKER_HOST = os.getenv("MQTT_HOST", "localhost")
    BROKER_PORT = int(os.getenv("MQTT_PORT", "1883"))
    MQTT_USER   = os.getenv("MQTT_USER")
    MQTT_PASS   = os.getenv("MQTT_PASS")
    INTERVAL    = int(os.getenv("SIM_INTERVAL", "10"))

    # Danh sách thiết bị khớp ảnh UI
    devices = [
        {"id": "DHT22-0001", "type": "DHT22",         "location": "Zone A"},
        {"id": "DHT22-0002", "type": "DHT22",         "location": "Zone B"},
        {"id": "SOIL-0001",  "type": "SOIL_MOISTURE", "location": "Zone A"},
        {"id": "SOIL-0002",  "type": "SOIL_MOISTURE", "location": "Zone B"},
        {"id": "LIGHT-0001", "type": "LIGHT",         "location": "Zone A"},
        {"id": "PH-0001",    "type": "PH",            "location": "Zone A"},
    ]

    sim = SensorSimulator(BROKER_HOST, BROKER_PORT, username=MQTT_USER, password=MQTT_PASS)
    sim.run_simulation(devices, INTERVAL)

if __name__ == "__main__":
    main()
