#!/usr/bin/env python3
"""
Script tự động test 7 quy tắc cảnh báo sức khỏe cây
Module 9 - Plant Health Alerts
"""

import paho.mqtt.client as mqtt
import json
import time
import requests
from datetime import datetime

# Cấu hình
MQTT_BROKER = "localhost"
MQTT_PORT = 1883
API_BASE_URL = "http://localhost:8080/api/plant-health"
FARM_ID = 1

class PlantHealthTester:
    def __init__(self):
        self.mqtt_client = mqtt.Client()
        self.mqtt_client.connect(MQTT_BROKER, MQTT_PORT, 60)
        print("✅ Kết nối MQTT thành công")
        
    def send_sensor_data(self, data, description):
        """Gửi dữ liệu cảm biến qua MQTT"""
        topic = f"sensor/{data['deviceId']}/data"
        payload = json.dumps(data)
        
        print(f"\n📤 {description}")
        print(f"   Topic: {topic}")
        print(f"   Data: {json.dumps(data, indent=2)}")
        
        self.mqtt_client.publish(topic, payload)
        time.sleep(2)  # Đợi server xử lý
        
    def check_health(self):
        """Kiểm tra sức khỏe qua API"""
        url = f"{API_BASE_URL}/current?farmId={FARM_ID}"
        
        try:
            response = requests.get(url)
            if response.status_code == 200:
                data = response.json()
                print(f"\n🌿 KẾT QUẢ KIỂM TRA:")
                print(f"   Điểm sức khỏe: {data['healthScore']}/100")
                print(f"   Trạng thái: {data['status']}")
                print(f"   Số cảnh báo: {len(data['activeAlerts'])}")
                
                if data['activeAlerts']:
                    print(f"\n   📋 CẢNH BÁO:")
                    for alert in data['activeAlerts']:
                        print(f"   - [{alert['severity']}] {alert['type']}: {alert['description']}")
                        print(f"     💡 Gợi ý: {alert['suggestion']}")
                
                print(f"\n   💬 Tổng quan: {data['overallSuggestion']}")
                return data
            else:
                print(f"❌ Lỗi API: {response.status_code}")
                return None
        except Exception as e:
            print(f"❌ Lỗi kết nối API: {e}")
            return None
    
    def test_rule_1_fungus(self):
        """Test Quy tắc 1: Nguy cơ nấm"""
        print("\n" + "="*60)
        print("TEST QUY TẮC 1: NGUY CƠ NẤM 🍄")
        print("="*60)
        
        data = {
            "deviceId": "DHT-001",
            "temperature": 25.0,    # 20-28°C ✓
            "humidity": 92.0,       # > 85% ✓
            "soilMoisture": 65.0,
            "timestamp": datetime.now().isoformat()
        }
        
        self.send_sensor_data(data, "Gửi dữ liệu: Độ ẩm cao + nhiệt độ phù hợp → Nguy cơ nấm")
        result = self.check_health()
        
        # Kiểm tra kết quả
        if result and any(a['type'] == 'FUNGUS' for a in result['activeAlerts']):
            print("\n✅ PASS: Phát hiện nguy cơ nấm")
        else:
            print("\n❌ FAIL: Không phát hiện nguy cơ nấm")
    
    def test_rule_2_heat_stress(self):
        """Test Quy tắc 2: Stress nhiệt"""
        print("\n" + "="*60)
        print("TEST QUY TẮC 2: STRESS NHIỆT 🔥")
        print("="*60)
        
        data = {
            "deviceId": "DHT-002",
            "temperature": 41.0,    # > 38°C ✓
            "humidity": 55.0,
            "soilMoisture": 35.0,
            "timestamp": datetime.now().isoformat()
        }
        
        self.send_sensor_data(data, "Gửi dữ liệu: Nhiệt độ 41°C → Stress nhiệt")
        result = self.check_health()
        
        if result and any(a['type'] == 'HEAT_STRESS' for a in result['activeAlerts']):
            print("\n✅ PASS: Phát hiện stress nhiệt")
        else:
            print("\n❌ FAIL: Không phát hiện stress nhiệt")
    
    def test_rule_3_drought(self):
        """Test Quy tắc 3: Thiếu nước"""
        print("\n" + "="*60)
        print("TEST QUY TẮC 3: THIẾU NƯỚC 💧")
        print("="*60)
        
        data = {
            "deviceId": "SOIL-001",
            "temperature": 32.0,
            "humidity": 45.0,
            "soilMoisture": 18.0,   # < 30% ✓
            "timestamp": datetime.now().isoformat()
        }
        
        self.send_sensor_data(data, "Gửi dữ liệu: Độ ẩm đất 18% → Thiếu nước")
        result = self.check_health()
        
        if result and any(a['type'] == 'DROUGHT' for a in result['activeAlerts']):
            print("\n✅ PASS: Phát hiện thiếu nước")
        else:
            print("\n❌ FAIL: Không phát hiện thiếu nước")
    
    def test_rule_4_cold(self):
        """Test Quy tắc 4: Lạnh"""
        print("\n" + "="*60)
        print("TEST QUY TẮC 4: NGUY CƠ LẠNH ❄️")
        print("="*60)
        
        data = {
            "deviceId": "DHT-003",
            "temperature": 10.0,    # < 12°C ✓
            "humidity": 70.0,
            "soilMoisture": 50.0,
            "timestamp": datetime.now().isoformat()
        }
        
        print("⚠️  LƯU Ý: Quy tắc này chỉ trigger vào ban đêm (22h-6h)")
        self.send_sensor_data(data, "Gửi dữ liệu: Nhiệt độ 10°C")
        result = self.check_health()
        
        # Quy tắc này phụ thuộc thời gian
        current_hour = datetime.now().hour
        is_night = current_hour >= 22 or current_hour <= 6
        
        if is_night:
            if result and any(a['type'] == 'COLD' for a in result['activeAlerts']):
                print("\n✅ PASS: Phát hiện nguy cơ lạnh (ban đêm)")
            else:
                print("\n❌ FAIL: Không phát hiện nguy cơ lạnh")
        else:
            print(f"\n⏭️  SKIP: Hiện tại là {current_hour}h (ban ngày), quy tắc chỉ chạy 22h-6h")
    
    def test_rule_5_unstable_moisture(self):
        """Test Quy tắc 5: Độ ẩm dao động"""
        print("\n" + "="*60)
        print("TEST QUY TẮC 5: ĐỘ ẨM DAO ĐỘNG ⚡")
        print("="*60)
        
        # Gửi dữ liệu ban đầu
        data1 = {
            "deviceId": "SOIL-002",
            "temperature": 28.0,
            "humidity": 60.0,
            "soilMoisture": 40.0,
            "timestamp": datetime.now().isoformat()
        }
        self.send_sensor_data(data1, "Bước 1: Gửi dữ liệu ban đầu - Độ ẩm 40%")
        
        print("\n⏳ Đợi 3 giây (giả lập 6 giờ trong thực tế)...")
        time.sleep(3)
        
        # Gửi dữ liệu thay đổi mạnh
        data2 = {
            "deviceId": "SOIL-002",
            "temperature": 28.0,
            "humidity": 60.0,
            "soilMoisture": 75.0,   # Thay đổi 35% ✓
            "timestamp": datetime.now().isoformat()
        }
        self.send_sensor_data(data2, "Bước 2: Gửi dữ liệu mới - Độ ẩm 75% (thay đổi 35%)")
        
        print("\n⚠️  LƯU Ý: Quy tắc này so sánh với dữ liệu 6 giờ trước")
        print("   Trong test này, chúng ta chỉ chờ 3 giây nên có thể không trigger")
        
        result = self.check_health()
        
        if result and any(a['type'] == 'UNSTABLE_MOISTURE' for a in result['activeAlerts']):
            print("\n✅ PASS: Phát hiện độ ẩm dao động")
        else:
            print("\n⏭️  Có thể cần đợi lâu hơn hoặc có dữ liệu 6h trước trong DB")
    
    def test_rule_6_low_light(self):
        """Test Quy tắc 6: Thiếu ánh sáng"""
        print("\n" + "="*60)
        print("TEST QUY TẮC 6: THIẾU ÁNH SÁNG 🌥️")
        print("="*60)
        
        data = {
            "deviceId": "LIGHT-001",
            "temperature": 26.0,
            "humidity": 65.0,
            "lightIntensity": 800.0,  # < 1000 lux ✓
            "timestamp": datetime.now().isoformat()
        }
        
        print("⚠️  LƯU Ý: Quy tắc này chỉ trigger vào ban ngày (8h-18h)")
        self.send_sensor_data(data, "Gửi dữ liệu: Ánh sáng 800 lux")
        result = self.check_health()
        
        current_hour = datetime.now().hour
        is_daytime = 8 <= current_hour <= 18
        
        if is_daytime:
            if result and any(a['type'] == 'LOW_LIGHT' for a in result['activeAlerts']):
                print("\n✅ PASS: Phát hiện thiếu ánh sáng (ban ngày)")
            else:
                print("\n❌ FAIL: Không phát hiện thiếu ánh sáng")
        else:
            print(f"\n⏭️  SKIP: Hiện tại là {current_hour}h (ban đêm), quy tắc chỉ chạy 8h-18h")
    
    def test_rule_7_ph_abnormal(self):
        """Test Quy tắc 7: pH bất thường"""
        print("\n" + "="*60)
        print("TEST QUY TẮC 7: PH BẤT THƯỜNG ⚗️")
        print("="*60)
        
        # Test pH quá thấp
        data = {
            "deviceId": "PH-001",
            "temperature": 28.0,
            "humidity": 60.0,
            "soilPH": 4.2,   # < 5.0 ✓
            "timestamp": datetime.now().isoformat()
        }
        
        self.send_sensor_data(data, "Gửi dữ liệu: pH 4.2 (quá chua)")
        result = self.check_health()
        
        if result and any(a['type'] == 'PH_ABNORMAL' for a in result['activeAlerts']):
            print("\n✅ PASS: Phát hiện pH bất thường")
        else:
            print("\n❌ FAIL: Không phát hiện pH bất thường")
    
    def test_combined_issues(self):
        """Test kết hợp nhiều vấn đề"""
        print("\n" + "="*60)
        print("TEST KẾT HỢP: NHIỀU VẤN ĐỀ CÙNG LÚC 🚨")
        print("="*60)
        
        data = {
            "deviceId": "ALL-001",
            "temperature": 40.0,    # HEAT_STRESS
            "humidity": 90.0,       # FUNGUS
            "soilMoisture": 18.0,   # DROUGHT
            "soilPH": 4.2,          # PH_ABNORMAL
            "timestamp": datetime.now().isoformat()
        }
        
        self.send_sensor_data(data, "Gửi dữ liệu: Kết hợp 4 vấn đề nghiêm trọng")
        result = self.check_health()
        
        if result:
            alert_count = len(result['activeAlerts'])
            print(f"\n📊 Kết quả:")
            print(f"   - Số cảnh báo: {alert_count}")
            print(f"   - Điểm sức khỏe: {result['healthScore']}")
            print(f"   - Trạng thái: {result['status']}")
            
            if alert_count >= 3 and result['healthScore'] < 50:
                print("\n✅ PASS: Phát hiện nhiều vấn đề, điểm sức khỏe thấp")
            else:
                print("\n⚠️  Kết quả không như mong đợi")
    
    def run_all_tests(self):
        """Chạy tất cả test cases"""
        print("\n" + "🌿"*30)
        print("BẮT ĐẦU TEST MODULE 9: PLANT HEALTH ALERTS")
        print("🌿"*30)
        
        tests = [
            self.test_rule_1_fungus,
            self.test_rule_2_heat_stress,
            self.test_rule_3_drought,
            self.test_rule_4_cold,
            self.test_rule_5_unstable_moisture,
            self.test_rule_6_low_light,
            self.test_rule_7_ph_abnormal,
            self.test_combined_issues
        ]
        
        for i, test in enumerate(tests, 1):
            try:
                test()
                time.sleep(2)  # Đợi giữa các test
            except Exception as e:
                print(f"\n❌ Lỗi test {i}: {e}")
        
        print("\n" + "="*60)
        print("✅ HOÀN THÀNH TẤT CẢ TEST CASES")
        print("="*60)
        
        # Tổng kết
        final_result = self.check_health()
        if final_result:
            print("\n📊 TỔNG KẾT:")
            print(f"   Tổng số cảnh báo: {len(final_result['activeAlerts'])}")
            print(f"   Điểm sức khỏe cuối: {final_result['healthScore']}/100")
            print(f"   Trạng thái: {final_result['status']}")

def main():
    """Hàm chính"""
    print("🔧 Khởi tạo Plant Health Tester...")
    tester = PlantHealthTester()
    
    print("\n📝 Lưu ý:")
    print("   - Đảm bảo MQTT broker đang chạy (localhost:1883)")
    print("   - Đảm bảo Spring Boot app đang chạy (localhost:8080)")
    print("   - Một số quy tắc phụ thuộc thời gian (ban ngày/ban đêm)")
    
    input("\n👉 Nhấn Enter để bắt đầu test...")
    
    tester.run_all_tests()
    
    print("\n🎉 Xong! Kiểm tra kết quả ở trên.")
    print("💡 Tip: Chạy lại script này vào các khung giờ khác nhau để test đầy đủ")

if __name__ == "__main__":
    main()