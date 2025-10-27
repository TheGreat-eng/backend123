package com.example.iotserver.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rule_conditions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Điều kiện thuộc quy tắc nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private Rule rule;

    // Loại điều kiện
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConditionType type;

    // Trường cần kiểm tra (temperature, humidity, soil_moisture, time, ...)
    @Column(nullable = false)
    private String field;

    // Toán tử so sánh
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Operator operator;

    // Giá trị để so sánh
    @Column(nullable = false)
    private String value;

    // Thiết bị liên quan (nếu có)
    @Column(name = "device_id")
    private String deviceId;

    // Logic kết hợp với điều kiện tiếp theo (AND / OR)
    @Enumerated(EnumType.STRING)
    @Column(name = "logical_operator")
    private LogicalOperator logicalOperator = LogicalOperator.AND;

    // Thứ tự ưu tiên
    @Column(name = "order_index")
    private Integer orderIndex = 0;

    // Các enum
    public enum ConditionType {
        SENSOR_VALUE, // Kiểm tra giá trị cảm biến
        TIME_RANGE, // Kiểm tra khoảng thời gian
        DEVICE_STATUS, // Kiểm tra trạng thái thiết bị
        WEATHER // Kiểm tra thời tiết (mở rộng sau)
    }

    public enum Operator {
        EQUALS, // ==
        NOT_EQUALS, // !=
        GREATER_THAN, // >
        GREATER_THAN_OR_EQUAL, // >=
        LESS_THAN, // <
        LESS_THAN_OR_EQUAL, // <=
        BETWEEN, // BETWEEN min AND max
        IN_RANGE // Trong khoảng
    }

    public enum LogicalOperator {
        AND, // VÀ
        OR // HOẶC
    }
}