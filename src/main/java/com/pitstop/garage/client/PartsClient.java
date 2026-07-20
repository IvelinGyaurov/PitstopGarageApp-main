package com.pitstop.garage.client;

import com.pitstop.garage.client.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "pitstop-parts", url = "http://localhost:8081")
public interface PartsClient {

    @GetMapping("/api/parts")
    List<PartResponse> getAllParts();

    @PostMapping("/api/parts")
    PartResponse createPart(@RequestBody CreatePartRequest request);

    @DeleteMapping("/api/parts/{id}")
    void deletePart(@PathVariable("id") UUID id);

    @PostMapping("/api/parts/deduct")
    List<DeductedPartResponse> deductParts(@RequestBody DeductPartsRequest request);
}