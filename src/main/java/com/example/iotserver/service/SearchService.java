package com.example.iotserver.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.iotserver.dto.DeviceSearchResultDTO;
import com.example.iotserver.dto.FarmSearchResultDTO;
import com.example.iotserver.dto.GlobalSearchResultDTO;
import com.example.iotserver.entity.Device;
import com.example.iotserver.entity.Farm;
import com.example.iotserver.entity.User;
import com.example.iotserver.repository.DeviceRepository;
import com.example.iotserver.repository.FarmRepository;

import lombok.RequiredArgsConstructor;

// SearchService.java
@Service
@RequiredArgsConstructor
public class SearchService {

    private final FarmRepository farmRepository;
    private final DeviceRepository deviceRepository;
    private final AuthenticationService authenticationService; // Để lấy user hiện tại

    public GlobalSearchResultDTO search(String query) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        String keyword = "%" + query.toLowerCase() + "%";

        // Chỉ tìm trong các farm/thiết bị của user đó
        List<Farm> userFarms = farmRepository.findByOwner(currentUser);
        List<Long> userFarmIds = userFarms.stream().map(Farm::getId).collect(Collectors.toList());

        // Tìm kiếm Farm
        List<FarmSearchResultDTO> foundFarms = userFarms.stream()
                .filter(farm -> farm.getName().toLowerCase().contains(query.toLowerCase()) ||
                        (farm.getLocation() != null && farm.getLocation().toLowerCase().contains(query.toLowerCase())))
                .map(farm -> new FarmSearchResultDTO(farm.getId(), farm.getName(), farm.getLocation()))
                .collect(Collectors.toList());

        // Tìm kiếm Device
        List<Device> foundDevicesRaw = deviceRepository.searchDevicesInFarms(userFarmIds, keyword);
        List<DeviceSearchResultDTO> foundDevices = foundDevicesRaw.stream()
                .map(device -> new DeviceSearchResultDTO(
                        device.getId(),
                        device.getDeviceId(),
                        device.getName(),
                        device.getFarm().getId(),
                        device.getFarm().getName()))
                .collect(Collectors.toList());

        return new GlobalSearchResultDTO(foundFarms, foundDevices);
    }
}
