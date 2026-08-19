package com.pitstop.garage.car.vin;

import com.fasterxml.jackson.databind.JsonNode;
import com.pitstop.garage.web.dto.AddCarRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
public class VinDecodeService {

    private static final Pattern VIN_PATTERN = Pattern.compile("^[A-HJ-NPR-Z0-9]{17}$");

    private final NhtsaVinDecodeClient nhtsaVinDecodeClient;

    public VinDecodeService(NhtsaVinDecodeClient nhtsaVinDecodeClient) {
        this.nhtsaVinDecodeClient = nhtsaVinDecodeClient;
    }

    public Optional<VinDecodeResult> decode(String vin) {
        if (vin == null || !VIN_PATTERN.matcher(vin.trim().toUpperCase()).matches()) {
            return Optional.empty();
        }

        String normalizedVin = vin.trim().toUpperCase();
        try {
            JsonNode response = nhtsaVinDecodeClient.decodeVin(normalizedVin);
            return mapResponse(response);
        } catch (VinDecodeUnavailableException ex) {
            log.warn("NHTSA VIN lookup unavailable for {}: {}", normalizedVin, ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("NHTSA VIN lookup failed for {}: {}", normalizedVin, ex.getMessage());
            return Optional.empty();
        }
    }

    public VinDecodeOutcome applyToAddCarRequest(AddCarRequest addCarRequest) {
        try {
            Optional<VinDecodeResult> decoded = decode(addCarRequest.getVin());
            if (decoded.isEmpty()) {
                return VinDecodeOutcome.failed();
            }

            VinDecodeResult result = decoded.get();
            addCarRequest.setBrand(result.brand());
            addCarRequest.setModel(result.model());
            addCarRequest.setYear(result.year());
            return VinDecodeOutcome.success();
        } catch (VinDecodeUnavailableException ex) {
            return VinDecodeOutcome.unavailable();
        }
    }

    private Optional<VinDecodeResult> mapResponse(JsonNode response) {
        if (response == null || !response.has("Results") || !response.get("Results").isArray()) {
            return Optional.empty();
        }

        String brand = null;
        String model = null;
        Integer year = null;

        for (JsonNode row : response.get("Results")) {
            if (!row.hasNonNull("Variable") || !row.has("Value")) {
                continue;
            }
            String variable = row.get("Variable").asText();
            String value = textValue(row.get("Value"));
            if (value == null) {
                continue;
            }
            switch (variable) {
                case "Make" -> brand = value;
                case "Model" -> model = value;
                case "Model Year" -> year = parseYear(value);
                default -> { }
            }
        }

        if (brand == null || model == null || year == null) {
            return Optional.empty();
        }

        return Optional.of(new VinDecodeResult(brand, model, year));
    }

    private static String textValue(JsonNode valueNode) {
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        String value = valueNode.asText().trim();
        if (value.isEmpty()
                || "Not Applicable".equalsIgnoreCase(value)
                || "NULL".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }

    private static Integer parseYear(String value) {
        try {
            int year = Integer.parseInt(value);
            if (year >= 1900 && year <= 2050) {
                return year;
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return null;
    }
}
