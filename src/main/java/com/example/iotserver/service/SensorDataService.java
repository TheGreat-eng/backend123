package com.example.iotserver.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.example.iotserver.config.InfluxDBConfig;
import com.example.iotserver.dto.SensorDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SensorDataService {

    private final WriteApiBlocking writeApi;
    private final InfluxDBClient influxDBClient;
    private final InfluxDBConfig influxDBConfig;

    /**
     * Save sensor data to InfluxDB
     */
    public void saveSensorData(SensorDataDTO data) {
        try {
            Point point = Point.measurement("sensor_data")
                    .addTag("device_id", data.getDeviceId())
                    .addTag("sensor_type", data.getSensorType() != null ? data.getSensorType() : "UNKNOWN") // Thêm kiểm
                                                                                                            // tra null
                    .addTag("farm_id", String.valueOf(data.getFarmId()))
                    .time(data.getTimestamp(), WritePrecision.MS);

            // VVVV--- THÊM LOG DEBUG CHI TIẾT ---VVVV
            log.info(">>>> [INFLUX WRITE] Preparing to write Point for device {}", data.getDeviceId());
            // ^^^^-------------------------------^^^^

            // VVVV--- THÊM ĐẦY ĐỦ CÁC TRƯỜNG ---VVVV
            if (data.getTemperature() != null)
                point.addField("temperature", data.getTemperature());
            if (data.getHumidity() != null)
                point.addField("humidity", data.getHumidity());
            if (data.getSoilMoisture() != null)
                point.addField("soil_moisture", data.getSoilMoisture());
            if (data.getLightIntensity() != null)
                point.addField("light_intensity", data.getLightIntensity());
            if (data.getSoilPH() != null)
                point.addField("soilPH", data.getSoilPH());
            // ^^^^-----------------------------^^^^

            // Nếu không có field nào được thêm, không ghi để tránh lỗi
            if (point.hasFields()) {
                writeApi.writePoint(point);
                log.debug("Saved sensor data for device: {}", data.getDeviceId());
            } else {
                log.warn("No fields to write for device {}, skipping InfluxDB write.", data.getDeviceId());
            }

        } catch (Exception e) {
            log.error("Error saving sensor data to InfluxDB: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save sensor data", e);
        }
    }

    // File: SensorDataService.java

    /**
     * Get latest sensor data for a device by pivoting fields into a single record.
     * ✅ SỬA: Tăng range lên 24h để đảm bảo có dữ liệu
     */
    public SensorDataDTO getLatestSensorData(String deviceId) {
        try {
            log.info("🔍 [InfluxDB] Getting latest data for device: {}", deviceId);

            // ✅ SỬA ĐỔI QUERY: Thêm pivot() để gộp các fields lại thành một hàng duy nhất
            String query = String.format(
                    "from(bucket: \"%s\")\n" +
                            "  |> range(start: -1h)\n" +
                            "  |> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\")\n" +
                            "  |> filter(fn: (r) => r[\"device_id\"] == \"%s\")\n" +
                            "  |> last()\n" +
                            "  |> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")",
                    influxDBConfig.getBucket(), deviceId);

            log.debug("🔍 [InfluxDB] Executing Pivot Query: {}", query);

            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(query, influxDBConfig.getOrg());

            if (tables.isEmpty() || tables.get(0).getRecords().isEmpty()) {
                log.warn("❌ [InfluxDB] No data found for device: {} in the last hour.", deviceId);
                return null;
            }

            // Với pivot(), chúng ta chỉ cần xử lý record đầu tiên
            FluxRecord record = tables.get(0).getRecords().get(0);
            Map<String, Object> values = record.getValues();

            SensorDataDTO sensorData = SensorDataDTO.builder()
                    .deviceId(deviceId)
                    .timestamp(record.getTime())
                    .temperature(getDoubleValue(values, "temperature"))
                    .humidity(getDoubleValue(values, "humidity"))
                    .soilMoisture(getDoubleValue(values, "soil_moisture"))
                    .lightIntensity(getDoubleValue(values, "light_intensity"))
                    .soilPH(getDoubleValue(values, "soilPH"))
                    .build();

            log.info("✅ [InfluxDB] Successfully retrieved latest data for {}: {}", deviceId, sensorData);
            return sensorData;

        } catch (Exception e) {
            log.error("❌ [InfluxDB] Error querying latest sensor data for {}: {}", deviceId, e.getMessage(), e);
            return null; // Trả về null khi có lỗi
        }
    }

    // ✅ THÊM HELPER METHOD NÀY: Lấy giá trị Double từ map một cách an toàn
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    /**
     * Get sensor data for a time range
     */
    public List<SensorDataDTO> getSensorDataRange(
            String deviceId,
            Instant start,
            Instant end) {
        String flux = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: %s, stop: %s) " +
                        "|> filter(fn: (r) => r[\"device_id\"] == \"%s\") " +
                        "|> sort(columns: [\"_time\"])",
                influxDBConfig.getBucket(),
                start.toString(),
                end.toString(),
                deviceId);

        List<Map<String, Object>> rawDataList = executeQueryList(flux);
        return rawDataList.stream()
                .map(SensorDataDTO::fromInfluxRecord)
                .collect(Collectors.toList());
    }

    /**
     * Get aggregated sensor data (for charts)
     */
    public List<SensorDataDTO> getAggregatedData(
            String deviceId,
            String field,
            String aggregation, // mean, max, min
            String window // 1m, 5m, 1h, 1d
    ) {
        String flux = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: -7d) " +
                        "|> filter(fn: (r) => r[\"device_id\"] == \"%s\") " +
                        "|> filter(fn: (r) => r[\"_field\"] == \"%s\") " +
                        "|> aggregateWindow(every: %s, fn: %s, createEmpty: false)",
                influxDBConfig.getBucket(),
                deviceId,
                field,
                window,
                aggregation);

        List<Map<String, Object>> rawDataList = executeQueryList(flux);

        // ✅ THÊM: Log debug
        log.info("🔍 [Aggregated Query] Device: {}, Field: {}, Window: {}, Results: {}",
                deviceId, field, window, rawDataList.size());

        if (rawDataList.isEmpty()) {
            log.warn("⚠️ Không có dữ liệu aggregated cho device: {}, field: {}", deviceId, field);
            return Collections.emptyList(); // ✅ Trả về list rỗng thay vì lỗi
        }

        return rawDataList.stream()
                .map(data -> {
                    SensorDataDTO dto = SensorDataDTO.fromInfluxRecord(data);

                    // ✅ SỬA: Xử lý null
                    Object valueObj = data.get("_value");
                    if (valueObj != null) {
                        if (valueObj instanceof Number) {
                            dto.setAvgValue(((Number) valueObj).doubleValue());
                        } else {
                            log.warn("⚠️ Value không phải số: {}", valueObj);
                        }
                    }

                    return dto;
                })
                .filter(dto -> dto.getAvgValue() != null) // ✅ Lọc bỏ các record null
                .collect(Collectors.toList());
    }

    /**
     * Get all devices data for a farm
     */
    public Map<String, Map<String, Object>> getFarmLatestData(Long farmId) {
        String flux = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: -1h) " +
                        "|> filter(fn: (r) => r[\"farm_id\"] == \"%s\") " +
                        "|> last()",
                influxDBConfig.getBucket(),
                farmId);

        List<Map<String, Object>> results = executeQueryList(flux);
        Map<String, Map<String, Object>> deviceDataMap = new HashMap<>();

        for (Map<String, Object> record : results) {
            String deviceId = (String) record.get("device_id");
            deviceDataMap.putIfAbsent(deviceId, new HashMap<>());

            String field = record.get("_field").toString();
            Object value = record.get("_value");

            deviceDataMap.get(deviceId).put(field, value);
            deviceDataMap.get(deviceId).put("device_id", deviceId);
            deviceDataMap.get(deviceId).put("timestamp", record.get("_time"));
        }

        return deviceDataMap;
    }

    // Helper methods
    private Map<String, Object> executeQuery(String flux) {
        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux, influxDBConfig.getOrg());

        if (tables.isEmpty() || tables.get(0).getRecords().isEmpty()) {
            return new HashMap<>();
        }

        return fluxRecordToMap(tables.get(0).getRecords().get(0));
    }

    private List<Map<String, Object>> executeQueryList(String flux) {
        try {
            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(flux, influxDBConfig.getOrg());

            // ✅ THÊM: Log debug
            log.debug("🔍 [InfluxDB] Query executed, tables count: {}", tables.size());

            if (tables.isEmpty()) {
                return Collections.emptyList(); // ✅ Trả về list rỗng
            }

            List<Map<String, Object>> results = new ArrayList<>();
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Map<String, Object> data = new HashMap<>();

                    // ✅ SỬA: Xử lý null an toàn
                    Object value = record.getValue();
                    if (value != null) {
                        data.put("_value", value);
                    } else {
                        log.warn("⚠️ Record có value null, bỏ qua");
                        continue; // Skip record này
                    }

                    data.put("_time", record.getTime());
                    data.put("_field", record.getField());
                    data.put("device_id", record.getValueByKey("device_id"));

                    results.add(data);
                }
            }

            return results;

        } catch (Exception e) {
            log.error("❌ [InfluxDB] Lỗi query: {}", e.getMessage(), e);
            return Collections.emptyList(); // ✅ Trả về list rỗng thay vì throw exception
        }
    }

    private Map<String, Object> fluxRecordToMap(FluxRecord record) {
        Map<String, Object> map = new HashMap<>();
        map.put("_time", record.getTime());
        map.put("_value", record.getValue());
        map.put("_field", record.getField());
        map.putAll(record.getValues());
        return map;
    }

    /**
     * Lấy dữ liệu cảm biến mới nhất theo farmId
     */
    public SensorDataDTO getLatestSensorDataByFarmId(Long farmId) {
        try {
            String query = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: -1h) " +
                            "|> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\") " +
                            "|> filter(fn: (r) => r[\"farm_id\"] == \"%s\") " +
                            "|> last()",
                    influxDBConfig.getBucket(),
                    farmId);

            log.debug("🔍 [InfluxDB] Query for farmId {}: {}", farmId, query);

            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(query);

            if (tables == null || tables.isEmpty()) {
                log.warn("⚠️ [InfluxDB] Không có dữ liệu cho farmId: {}", farmId);
                return null;
            }

            // Parse dữ liệu
            SensorDataDTO data = new SensorDataDTO();
            data.setFarmId(farmId);
            data.setTimestamp(Instant.now());

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    String field = (String) record.getField();
                    Object value = record.getValue();

                    switch (field) {
                        case "temperature":
                            data.setTemperature(((Number) value).doubleValue());
                            break;
                        case "humidity":
                            data.setHumidity(((Number) value).doubleValue());
                            break;
                        case "soil_moisture":
                            data.setSoilMoisture(((Number) value).doubleValue());
                            break;
                        case "light_intensity":
                            data.setLightIntensity(((Number) value).doubleValue());
                            break;
                        case "soilPH":
                            data.setSoilPH(((Number) value).doubleValue());
                            break;
                    }
                }
            }

            log.info("✅ [InfluxDB] Lấy dữ liệu thành công cho farmId: {}", farmId);
            return data;

        } catch (Exception e) {
            log.error("❌ [InfluxDB] Lỗi khi lấy dữ liệu farmId {}: {}", farmId, e.getMessage());
            return null;
        }
    }

    /**
     * Lấy dữ liệu cảm biến tại thời điểm cụ thể
     * (Dùng cho quy tắc 5: độ ẩm dao động)
     */
    public SensorDataDTO getSensorDataAt(Long farmId, LocalDateTime dateTime) {
        try {
            String query = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: %s, stop: %s) " +
                            "|> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\") " +
                            "|> filter(fn: (r) => r[\"farm_id\"] == \"%s\") " +
                            "|> last()",
                    influxDBConfig.getBucket(),
                    dateTime.minusMinutes(30).toString() + "Z",
                    dateTime.plusMinutes(30).toString() + "Z",
                    farmId);

            log.debug("🔍 [InfluxDB] Query for farmId {}: {}", farmId, query);

            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(query);

            if (tables == null || tables.isEmpty()) {
                log.warn("⚠️ [InfluxDB] Không có dữ liệu cho farmId: {}", farmId);
                return null;
            }

            // Parse dữ liệu
            SensorDataDTO data = new SensorDataDTO();
            data.setFarmId(farmId);
            data.setTimestamp(Instant.now());

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    String field = (String) record.getField();
                    Object value = record.getValue();

                    switch (field) {
                        case "temperature":
                            data.setTemperature(((Number) value).doubleValue());
                            break;
                        case "humidity":
                            data.setHumidity(((Number) value).doubleValue());
                            break;
                        case "soil_moisture":
                            data.setSoilMoisture(((Number) value).doubleValue());
                            break;
                        case "light_intensity":
                            data.setLightIntensity(((Number) value).doubleValue());
                            break;
                        case "soilPh":
                            data.setSoilPH(((Number) value).doubleValue());
                            break;
                    }
                }
            }

            log.info("✅ [InfluxDB] Lấy dữ liệu thành công cho farmId: {}", farmId);
            return data;

        } catch (Exception e) {
            log.error("❌ [InfluxDB] Lỗi khi lấy dữ liệu farmId {}: {}", farmId, e.getMessage());
            return null;
        }
    }

    /**
     * 🔍 DEBUG: Kiểm tra dữ liệu sensor có tồn tại không
     */
    public boolean hasRecentData(String deviceId, int hoursBack) {
        try {
            String query = String.format(
                    "from(bucket: \"%s\")\n" +
                            "  |> range(start: -%dh)\n" +
                            "  |> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\")\n" +
                            "  |> filter(fn: (r) => r[\"device_id\"] == \"%s\")\n" +
                            "  |> count()",
                    influxDBConfig.getBucket(), hoursBack, deviceId);

            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(query, influxDBConfig.getOrg());

            if (!tables.isEmpty() && !tables.get(0).getRecords().isEmpty()) {
                Object count = tables.get(0).getRecords().get(0).getValue();
                long recordCount = count != null ? ((Number) count).longValue() : 0;
                log.info("🔍 Device {} có {} bản ghi trong {}h qua", deviceId, recordCount, hoursBack);
                return recordCount > 0;
            }

            log.warn("⚠️ Không có dữ liệu nào cho device {} trong {}h qua", deviceId, hoursBack);
            return false;

        } catch (Exception e) {
            log.error("❌ Lỗi kiểm tra dữ liệu: {}", e.getMessage());
            return false;
        }
    }
}
