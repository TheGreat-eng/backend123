package com.example.iotserver.service;

import com.example.iotserver.dto.SensorDataDTO;
import com.example.iotserver.entity.Device;
import com.example.iotserver.exception.ResourceNotFoundException;
import com.example.iotserver.repository.DeviceRepository;
import com.example.iotserver.repository.RuleExecutionLogRepository;
import com.example.iotserver.repository.RuleRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    private final DeviceRepository deviceRepository;
    private final RuleRepository ruleRepository;
    private final RuleExecutionLogRepository logRepository;
    private final SensorDataService sensorDataService;

    public Map<String, Object> getDashboardSummary(Long farmId) {
        Map<String, Object> summary = new HashMap<>();

        // Thống kê thiết bị
        long totalDevices = deviceRepository.countByFarmId(farmId);
        long onlineDevices = deviceRepository.countByFarmIdAndStatus(farmId,
                com.example.iotserver.entity.Device.DeviceStatus.ONLINE);
        summary.put("totalDevices", totalDevices);
        summary.put("onlineDevices", onlineDevices);

        // Thống kê quy tắc
        long totalRules = ruleRepository.countByFarmId(farmId);
        long enabledRules = ruleRepository.countByFarmIdAndEnabled(farmId, true);
        summary.put("totalRules", totalRules);
        summary.put("enabledRules", enabledRules);

        // Lấy dữ liệu môi trường trung bình (từ service đã có)
        Map<String, Object> avgData = new HashMap<>();
        try {
            // Lấy dữ liệu mới nhất của TẤT CẢ thiết bị trong farm
            Map<String, Map<String, Object>> latestFarmData = sensorDataService.getFarmLatestData(farmId);

            // Lọc ra các giá trị khác null để tính trung bình
            double avgTemperature = latestFarmData.values().stream()
                    .filter(data -> data.containsKey("temperature") && data.get("temperature") != null)
                    .mapToDouble(data -> ((Number) data.get("temperature")).doubleValue())
                    .average()
                    .orElse(Double.NaN); // Dùng NaN nếu không có dữ liệu

            double avgHumidity = latestFarmData.values().stream()
                    .filter(data -> data.containsKey("humidity") && data.get("humidity") != null)
                    .mapToDouble(data -> ((Number) data.get("humidity")).doubleValue())
                    .average()
                    .orElse(Double.NaN);

            double avgLightIntensity = latestFarmData.values().stream()
                    .filter(data -> data.containsKey("light_intensity") && data.get("light_intensity") != null)
                    .mapToDouble(data -> ((Number) data.get("light_intensity")).doubleValue())
                    .average()
                    .orElse(Double.NaN);
            double avgSoilMoisture = latestFarmData.values().stream()
                    .filter(data -> data.containsKey("soil_moisture") && data.get("soil_moisture") != null)
                    .mapToDouble(data -> ((Number) data.get("soil_moisture")).doubleValue())
                    .average()
                    .orElse(Double.NaN);
            double avgSoilPH = latestFarmData.values().stream()
                    .filter(data -> data.containsKey("soilPH") && data.get("soilPH") != null)
                    .mapToDouble(data -> ((Number) data.get("soilPH")).doubleValue())
                    .average()
                    .orElse(Double.NaN);

            // Đưa giá trị vào map avgData (chỉ đưa vào nếu nó là số)
            if (!Double.isNaN(avgTemperature))
                avgData.put("avgTemperature", Math.round(avgTemperature * 10) / 10.0);
            if (!Double.isNaN(avgHumidity))
                avgData.put("avgHumidity", Math.round(avgHumidity * 10) / 10.0);
            if (!Double.isNaN(avgLightIntensity))
                avgData.put("avgLightIntensity", Math.round(avgLightIntensity));
            if (!Double.isNaN(avgSoilMoisture))
                avgData.put("avgSoilMoisture", Math.round(avgSoilMoisture * 10) / 10.0);
            if (!Double.isNaN(avgSoilPH))
                avgData.put("avgSoilPH", Math.round(avgSoilPH * 100) / 100.0); // pH lấy 2 chữ số thập phân

        } catch (Exception e) {
            // Ghi log lỗi nhưng không làm crash ứng dụng
            // logger.error("Không thể tính dữ liệu môi trường trung bình", e);
        }

        summary.put("averageEnvironment", avgData);
        // ===================================

        return summary;
    }

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    /**
     * Ghi dữ liệu cảm biến vào response dưới dạng file CSV
     * 
     * @param response HttpServletResponse để ghi file
     * @param deviceId ID của thiết bị
     * @param start    Thời gian bắt đầu
     * @param end      Thời gian kết thúc
     * @throws IOException
     */
    public void writeSensorDataToCsv(HttpServletResponse response, String deviceId, Instant start, Instant end)
            throws IOException {
        log.info("Bắt đầu xuất CSV cho thiết bị {} từ {} đến {}", deviceId, start, end);
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "deviceId", deviceId));

        List<SensorDataDTO> dataList = sensorDataService.getSensorDataRange(deviceId, start, end);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"report_" + deviceId + ".csv\"");
        response.setCharacterEncoding("UTF-8"); // Đảm bảo hỗ trợ tiếng Việt

        PrintWriter writer = response.getWriter();
        // Ghi BOM để Excel nhận diện UTF-8
        writer.write('\ufeff');

        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                .withHeader("Thời gian", "Nhiệt độ (°C)", "Độ ẩm (%)", "Độ ẩm đất (%)", "Ánh sáng (lux)", "pH đất"))) {

            for (SensorDataDTO data : dataList) {
                csvPrinter.printRecord(
                        data.getTimestamp() != null ? DATE_TIME_FORMATTER.format(data.getTimestamp()) : "N/A",
                        data.getTemperature(),
                        data.getHumidity(),
                        data.getSoilMoisture(),
                        data.getLightIntensity(),
                        data.getSoilPH());
            }
            log.info("Đã xuất thành công {} dòng dữ liệu ra CSV.", dataList.size());
        } catch (IOException e) {
            log.error("Lỗi khi ghi file CSV: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Tạo báo cáo PDF từ dữ liệu cảm biến
     * 
     * @param response HttpServletResponse để ghi file
     * @param deviceId ID của thiết bị
     * @param start    Thời gian bắt đầu
     * @param end      Thời gian kết thúc
     * @throws IOException
     */
    public void createSensorDataPdf(HttpServletResponse response, String deviceId, Instant start, Instant end)
            throws IOException {
        log.info("Bắt đầu tạo PDF cho thiết bị {} từ {} đến {}", deviceId, start, end);
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "deviceId", deviceId));

        List<SensorDataDTO> dataList = sensorDataService.getSensorDataRange(deviceId, start, end);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"report_" + deviceId + ".pdf\"");

        PdfWriter writer = new PdfWriter(response.getOutputStream());
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);

        // --- Tiêu đề ---
        document.add(new Paragraph("BÁO CÁO DỮ LIỆU CẢM BIẾN")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(20));

        // --- Thông tin báo cáo ---
        document.add(new Paragraph("Thiết bị: " + device.getName() + " (" + deviceId + ")"));
        document.add(new Paragraph("Loại: " + device.getType().name()));
        document.add(new Paragraph("Nông trại: " + device.getFarm().getName()));
        document.add(new Paragraph(
                "Thời gian: từ " + DATE_TIME_FORMATTER.format(start) + " đến " + DATE_TIME_FORMATTER.format(end)));

        // --- Tạo bảng ---
        float[] columnWidths = { 3, 2, 2, 2, 2, 2 };
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginTop(20);

        // --- Header của bảng ---
        addTableHeader(table, "Thời gian", "Nhiệt độ (°C)", "Độ ẩm (%)", "Độ ẩm đất (%)", "Ánh sáng (lux)", "pH đất");

        // --- Dữ liệu của bảng ---
        for (SensorDataDTO data : dataList) {
            addTableRow(table,
                    data.getTimestamp() != null ? DATE_TIME_FORMATTER.format(data.getTimestamp()) : "N/A",
                    data.getTemperature() != null ? data.getTemperature().toString() : "",
                    data.getHumidity() != null ? data.getHumidity().toString() : "",
                    data.getSoilMoisture() != null ? data.getSoilMoisture().toString() : "",
                    data.getLightIntensity() != null ? data.getLightIntensity().toString() : "",
                    data.getSoilPH() != null ? data.getSoilPH().toString() : "");
        }

        document.add(table);
        document.close();
        log.info("Đã tạo thành công file PDF với {} dòng dữ liệu.", dataList.size());
    }

    // Helper method cho việc tạo bảng PDF
    private void addTableHeader(Table table, String... headers) {
        for (String header : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(header))
                    .setBold()
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER));
        }
    }

    private void addTableRow(Table table, String... data) {
        for (String value : data) {
            table.addCell(new Cell().add(new Paragraph(value)));
        }
    }

}