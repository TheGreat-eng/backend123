#!/usr/bin/env python3
"""
IoT Sensor Simulator for Smart Farm
Simulates multiple sensor types sending data via MQTT
"""

import json
import time
import random
import math
from datetime import datetime
from typing import Dict, Any
import paho.mqtt.client as mqtt

class SensorSimulator:
    def __init__(self, broker_host="localhost", broker_port=1883):
        self.client = mqtt.Client(client_id=f"simulator_{random.randint(1000, 9999)}")
        self.broker_host = broker_host
        self.broker_port = broker_port
        self.connected = False
        
        # Simulation state
        self.base_temperature = 28.0
        self.base_humidity = 65.0
        self.soil_moisture = 50.0
        self.light_intensity = 10000.0
        self.ph_level = 6.5
        
        # Time tracking
        self.start_time = time.time()
        
        # Setup MQTT callbacks
        self.client.on_connect = self.on_connect
        self.client.on_disconnect = self.on_disconnect
        
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
        """Connect to MQTT broker"""
        try:
            self.client.connect(self.broker_host, self.broker_port, 60)
            self.client.loop_start()
            
            # Wait for connection
            timeout = 10
            start = time.time()
            while not self.connected and (time.time() - start) < timeout:
                time.sleep(0.1)
                
            return self.connected
        except Exception as e:
            print(f"❌ Connection error: {e}")
            return False
            
    def disconnect(self):
        """Disconnect from MQTT broker"""
        self.client.loop_stop()
        self.client.disconnect()
        
    def get_time_factor(self) -> float:
        """Get time-based factor for day/night simulation"""
        elapsed = time.time() - self.start_time
        # Simulate 24 hours in 24 minutes (1 minute = 1 hour)
        hour_of_day = (elapsed / 60) % 24
        return hour_of_day
        
    def simulate_dht22(self, device_id: str) -> Dict[str, Any]:
        """Simulate DHT22 sensor (Temperature + Humidity)"""
        hour = self.get_time_factor()
        
        # Temperature varies with time of day
        temp_variation = 5 * math.sin((hour - 6) * math.pi / 12)
        temperature = self.base_temperature + temp_variation + random.uniform(-1, 1)
        
        # Humidity inversely correlated with temperature
        humidity = self.base_humidity - (temp_variation * 2) + random.uniform(-3, 3)
        humidity = max(30, min(95, humidity))
        
        return {
            "deviceId": device_id,
            "sensorType": "DHT22",
            "temperature": round(temperature, 2),
            "humidity": round(humidity, 2),
            "timestamp": datetime.now().isoformat()
        }
        
    def simulate_soil_moisture(self, device_id: str) -> Dict[str, Any]:
        """Simulate soil moisture sensor"""
        # Soil moisture gradually decreases
        self.soil_moisture -= random.uniform(0.05, 0.15)
        
        # Simulate irrigation events (random spikes)
        if random.random() < 0.02:  # 2% chance per reading
            self.soil_moisture += random.uniform(15, 25)
            print(f"💧 Irrigation event! Moisture increased to {self.soil_moisture:.1f}%")
            
        # Keep within realistic bounds
        self.soil_moisture = max(20, min(70, self.soil_moisture))
        
        return {
            "deviceId": device_id,
            "sensorType": "SOIL_MOISTURE",
            "soilMoisture": round(self.soil_moisture, 2),
            "timestamp": datetime.now().isoformat()
        }
        
    def simulate_light_sensor(self, device_id: str) -> Dict[str, Any]:
        """Simulate light intensity sensor"""
        hour = self.get_time_factor()
        
        # Light intensity based on time of day
        if 6 <= hour <= 18:  # Daytime
            # Peak at noon (hour 12)
            light_factor = math.sin((hour - 6) * math.pi / 12)
            self.light_intensity = 50000 * light_factor + random.uniform(-2000, 2000)
        else:  # Nighttime
            self.light_intensity = random.uniform(0, 100)
            
        self.light_intensity = max(0, self.light_intensity)
        
        return {
            "deviceId": device_id,
            "sensorType": "LIGHT",
            "lightIntensity": round(self.light_intensity, 2),
            "timestamp": datetime.now().isoformat()
        }
        
    def simulate_ph_sensor(self, device_id: str) -> Dict[str, Any]:
        """Simulate pH sensor"""
        # pH changes slowly
        self.ph_level += random.uniform(-0.02, 0.02)
        self.ph_level = max(5.5, min(7.5, self.ph_level))
        
        return {
            "deviceId": device_id,
            "sensorType": "PH",
            "ph": round(self.ph_level, 2),
            "timestamp": datetime.now().isoformat()
        }
        
    def publish_sensor_data(self, device_id: str, data: Dict[str, Any]):
        """Publish sensor data to MQTT"""
        topic = f"sensor/{device_id}/data"
        payload = json.dumps(data)
        
        result = self.client.publish(topic, payload, qos=1)
        
        if result.rc == mqtt.MQTT_ERR_SUCCESS:
            sensor_type = data.get("sensorType", "UNKNOWN")
            print(f"📤 Published {sensor_type} data from {device_id}")
        else:
            print(f"❌ Failed to publish data for {device_id}")
            
    def publish_device_status(self, device_id: str, status: str):
        """Publish device status"""
        topic = f"device/{device_id}/status"
        payload = json.dumps({
            "deviceId": device_id,
            "status": status,
            "timestamp": datetime.now().isoformat()
        })
        
        self.client.publish(topic, payload, qos=1)
        print(f"📡 Published status for {device_id}: {status}")
        
    def run_simulation(self, devices: list, interval: int = 10):
        """Run continuous simulation"""
        print("\n" + "="*60)
        print("🌾 Smart Farm IoT Simulator Started")
        print("="*60)
        print(f"Devices: {len(devices)}")
        print(f"Interval: {interval} seconds")
        print(f"Broker: {self.broker_host}:{self.broker_port}")
        print("="*60 + "\n")
        
        if not self.connect():
            print("❌ Failed to connect to MQTT broker. Exiting...")
            return
            
        # Send initial status for all devices
        for device in devices:
            self.publish_device_status(device["id"], "ONLINE")
            
        try:
            iteration = 0
            while True:
                iteration += 1
                print(f"\n--- Iteration {iteration} ---")
                hour = self.get_time_factor()
                print(f"🕐 Simulated time: {int(hour):02d}:00 (Hour {int(hour)} of 24)")
                
                for device in devices:
                    device_id = device["id"]
                    device_type = device["type"]
                    
                    # Generate sensor data based on type
                    if device_type == "DHT22":
                        data = self.simulate_dht22(device_id)
                    elif device_type == "SOIL_MOISTURE":
                        data = self.simulate_soil_moisture(device_id)
                    elif device_type == "LIGHT":
                        data = self.simulate_light_sensor(device_id)
                    elif device_type == "PH":
                        data = self.simulate_ph_sensor(device_id)
                    else:
                        continue
                        
                    # Publish to MQTT
                    self.publish_sensor_data(device_id, data)
                    
                print(f"\n💤 Waiting {interval} seconds...\n")
                time.sleep(interval)
                
        except KeyboardInterrupt:
            print("\n\n🛑 Stopping simulator...")
            
            # Send offline status for all devices
            for device in devices:
                self.publish_device_status(device["id"], "OFFLINE")
                
            self.disconnect()
            print("✅ Simulator stopped gracefully")


def create_test_scenario():
    """Create a test scenario with extreme conditions"""
    scenarios = [
        {
            "name": "Normal Operation",
            "duration": 60,  # seconds
            "description": "Normal farming conditions"
        },
        {
            "name": "Heat Wave",
            "duration": 30,
            "description": "Temperature rises to 38°C",
            "temperature_boost": 10
        },
        {
            "name": "Drought",
            "duration": 45,
            "description": "Soil moisture drops rapidly",
            "moisture_drain": 0.5
        }
    ]
    
    return scenarios


def main():
    """Main function"""
    
    # Configuration
    BROKER_HOST = "localhost"  # Change to your MQTT broker
    BROKER_PORT = 1883
    INTERVAL = 10  # seconds between readings
    
    # Define virtual devices
    devices = [
        {"id": "DHT22-001", "type": "DHT22", "location": "Garden A"},
        {"id": "DHT22-002", "type": "DHT22", "location": "Garden B"},
        {"id": "SOIL-001", "type": "SOIL_MOISTURE", "location": "Garden A"},
        {"id": "SOIL-002", "type": "SOIL_MOISTURE", "location": "Garden B"},
        {"id": "LIGHT-001", "type": "LIGHT", "location": "Garden A"},
        {"id": "PH-001", "type": "PH", "location": "Garden A"},
    ]
    
    # Create and run simulator
    simulator = SensorSimulator(BROKER_HOST, BROKER_PORT)
    simulator.run_simulation(devices, INTERVAL)


if __name__ == "__main__":
    main()