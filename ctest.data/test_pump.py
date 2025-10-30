import paho.mqtt.client as mqtt
import json
from datetime import datetime
import time

BROKER = "localhost"
PORT = 1883
DEVICE_ID = "PUMP-0001" 
TOPIC_CONTROL = f"device/{DEVICE_ID}/control"
TOPIC_STATUS = f"device/{DEVICE_ID}/status" # ✅ THÊM: Topic để gửi phản hồi trạng thái

pump_state = "OFF" # Giả lập trạng thái ban đầu của máy bơm

def on_connect(client, userdata, flags, rc):
    print("Pump connected to MQTT.")
    client.subscribe(TOPIC_CONTROL)
    print(f"Pump is listening on topic: {TOPIC_CONTROL}")
    
    # ✅ THÊM: Gửi trạng thái "ONLINE" ngay khi kết nối
    initial_status = {
        "deviceId": DEVICE_ID,
        "status": "ONLINE",
        "state": pump_state, # Báo cáo trạng thái hiện tại là OFF
        "timestamp": datetime.now().isoformat()
    }
    client.publish(TOPIC_STATUS, json.dumps(initial_status))
    print(f"Sent initial status to {TOPIC_STATUS}: ONLINE, state: {pump_state}")


def on_message(client, userdata, msg):
    global pump_state
    print(f"\n>>>> PUMP RECEIVED COMMAND <<<<")
    print(f"From topic: {msg.topic}")
    payload = json.loads(msg.payload.decode())
    print(f"Payload: {json.dumps(payload, indent=2)}")
    
    action = payload.get("action", "").lower()
    
    # ✅ THÊM LOGIC GỬI PHẢN HỒI
    feedback_payload = {
        "deviceId": DEVICE_ID,
        "status": "ONLINE", # Vì nó đang chạy nên nó online
        "timestamp": datetime.now().isoformat()
    }

    if "turn_on" in action:
        pump_state = "ON"
        feedback_payload["state"] = pump_state # Trạng thái mới là ON
        print("Action: Turning ON the pump.")
    elif "turn_off" in action:
        pump_state = "OFF"
        feedback_payload["state"] = pump_state # Trạng thái mới là OFF
        print("Action: Turning OFF the pump.")
    else:
        print("Action: Unknown command.")
        return # Không làm gì nếu lệnh không rõ

    # Gửi phản hồi về cho backend
    client.publish(TOPIC_STATUS, json.dumps(feedback_payload))
    print(f"Sent feedback to {TOPIC_STATUS}: {json.dumps(feedback_payload)}")
    print("================================\n")


client = mqtt.Client()
client.on_connect = on_connect
client.on_message = on_message
client.connect(BROKER, PORT, 60)
client.loop_forever()