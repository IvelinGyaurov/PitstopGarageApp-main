package com.pitstop.garage.car.vin;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NhtsaVinDecodeClientTest {

    private static final String SAMPLE_VIN = "1HGCM82633A004352";
    private static final String DECODE_URL =
            "https://vpic.nhtsa.dot.gov/api/vehicles/DecodeVin/" + SAMPLE_VIN + "?format=json";

    private MockRestServiceServer server;
    private NhtsaVinDecodeClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new NhtsaVinDecodeClient(builder);
    }

    @Test
    void decodeVin_whenSuccess_returnsJsonBody() {
        server.expect(requestTo(DECODE_URL))
                .andRespond(withSuccess("""
                        {"Results":[{"Variable":"Make","Value":"HONDA"}]}
                        """, MediaType.APPLICATION_JSON));

        JsonNode response = client.decodeVin(SAMPLE_VIN);

        assertNotNull(response);
        assertTrue(response.has("Results"));
        server.verify();
    }

    @Test
    void decodeVin_whenRestClientFails_throwsUnavailable() {
        server.expect(requestTo(DECODE_URL))
                .andRespond(withServerError());

        assertThrows(VinDecodeUnavailableException.class, () -> client.decodeVin(SAMPLE_VIN));
        server.verify();
    }
}
