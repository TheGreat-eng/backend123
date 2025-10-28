package com.example.iotserver.controller;

import java.util.Collections;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.iotserver.dto.GlobalSearchResultDTO;
import com.example.iotserver.dto.response.ApiResponse;
import com.example.iotserver.service.SearchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<ApiResponse<GlobalSearchResultDTO>> globalSearch(
            @RequestParam(value = "q", required = false, defaultValue = "") String query) {

        log.info("🔍 Search request received with query: {}", query);

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(
                    ApiResponse.<GlobalSearchResultDTO>builder()
                            .success(true)
                            .message("Search query is empty.")
                            .data(new GlobalSearchResultDTO(Collections.emptyList(), Collections.emptyList()))
                            .build());
        }

        GlobalSearchResultDTO results = searchService.search(query);
        log.info("✅ Search completed. Found {} farms and {} devices",
                results.getFarms().size(), results.getDevices().size());

        return ResponseEntity.ok(
                ApiResponse.<GlobalSearchResultDTO>builder()
                        .success(true)
                        .message("Search successful.")
                        .data(results)
                        .build());
    }
}
