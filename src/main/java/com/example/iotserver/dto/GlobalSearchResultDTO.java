package com.example.iotserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSearchResultDTO {
    private List<FarmSearchResultDTO> farms;
    private List<DeviceSearchResultDTO> devices;
}
