package com.example.iotserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FarmSearchResultDTO {
    private Long id;
    private String name;

    private String location;
}
