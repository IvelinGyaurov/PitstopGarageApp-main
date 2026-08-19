package com.pitstop.garage.car.vin;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NhtsaVinDecodeClient {

    private static final String BASE_URL = "https://vpic.nhtsa.dot.gov";

    private final RestClient restClient;

    public NhtsaVinDecodeClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
    }

    public JsonNode decodeVin(String vin) {
        try {
            return restClient.get()
                    .uri("/api/vehicles/DecodeVin/{vin}?format=json", vin)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new VinDecodeUnavailableException(ex);
        }
    }
}
