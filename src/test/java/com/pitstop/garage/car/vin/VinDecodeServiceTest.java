package com.pitstop.garage.car.vin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pitstop.garage.web.dto.AddCarRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VinDecodeServiceTest {

    private static final String SAMPLE_VIN = "1HGCM82633A004352";

    @Mock
    private NhtsaVinDecodeClient nhtsaVinDecodeClient;

    @InjectMocks
    private VinDecodeService vinDecodeService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void decode_whenValidResponse_returnsBrandModelYear() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN)).thenReturn(sampleResponse("HONDA", "Accord", "2003"));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isPresent());
        assertEquals("HONDA", result.get().brand());
        assertEquals("Accord", result.get().model());
        assertEquals(2003, result.get().year());
        verify(nhtsaVinDecodeClient).decodeVin(SAMPLE_VIN);
    }

    @Test
    void decode_whenVinHasLowercase_normalizesBeforeLookup() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN)).thenReturn(sampleResponse("HONDA", "Accord", "2003"));

        Optional<VinDecodeResult> result = vinDecodeService.decode("1hgcm82633a004352");

        assertTrue(result.isPresent());
        verify(nhtsaVinDecodeClient).decodeVin(SAMPLE_VIN);
    }

    @Test
    void decode_whenVinInvalid_returnsEmpty() {
        Optional<VinDecodeResult> result = vinDecodeService.decode("INVALID");

        assertTrue(result.isEmpty());
    }

    @Test
    void decode_whenMissingFields_returnsEmpty() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN)).thenReturn(sampleResponse(null, "Accord", "2003"));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isEmpty());
    }

    @Test
    void decode_whenVinNull_returnsEmpty() {
        Optional<VinDecodeResult> result = vinDecodeService.decode(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void decode_whenGenericRuntimeException_returnsEmpty() {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN)).thenThrow(new RuntimeException("boom"));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isEmpty());
    }

    @Test
    void decode_whenResponseNull_returnsEmpty() {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN)).thenReturn(null);

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isEmpty());
    }

    @Test
    void decode_whenResponseHasNoResults_returnsEmpty() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN)).thenReturn(objectMapper.readTree("{}"));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isEmpty());
    }

    @Test
    void decode_whenResultsNotArray_returnsEmpty() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN))
                .thenReturn(objectMapper.readTree("{\"Results\":\"invalid\"}"));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isEmpty());
    }

    @Test
    void decode_whenRowMissingVariable_skipsRow() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN)).thenReturn(objectMapper.readTree("""
                {
                  "Results": [
                    {"Value": "IGNORED"},
                    {"Variable": "Make", "Value": "HONDA"},
                    {"Variable": "Model", "Value": "Accord"},
                    {"Variable": "Model Year", "Value": "2003"}
                  ]
                }
                """));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isPresent());
        assertEquals("HONDA", result.get().brand());
        assertEquals("Accord", result.get().model());
        assertEquals(2003, result.get().year());
    }

    @Test
    void decode_whenRowMissingValue_skipsRow() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN)).thenReturn(objectMapper.readTree("""
                {
                  "Results": [
                    {"Variable": "Make"},
                    {"Variable": "Model", "Value": "Accord"},
                    {"Variable": "Model Year", "Value": "2003"}
                  ]
                }
                """));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isEmpty());
    }

    @Test
    void decode_whenMakeValueEmpty_returnsEmpty() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN))
                .thenReturn(sampleResponse("", "Accord", "2003"));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isEmpty());
    }

    @Test
    void decode_whenMakeValueNullString_returnsEmpty() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN))
                .thenReturn(sampleResponse("NULL", "Accord", "2003"));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isEmpty());
    }

    @Test
    void decode_whenUnknownVariable_isIgnored() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN)).thenReturn(objectMapper.readTree("""
                {
                  "Results": [
                    {"Variable": "Trim", "Value": "EX-V6"},
                    {"Variable": "Make", "Value": "HONDA"},
                    {"Variable": "Model", "Value": "Accord"},
                    {"Variable": "Model Year", "Value": "2003"}
                  ]
                }
                """));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isPresent());
        assertEquals("HONDA", result.get().brand());
    }

    @Test
    void decode_whenYearInvalid_returnsEmpty() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN))
                .thenReturn(sampleResponse("HONDA", "Accord", "not-a-year"));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isEmpty());
    }

    @Test
    void decode_whenYearTooLow_returnsEmpty() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN))
                .thenReturn(sampleResponse("HONDA", "Accord", "1899"));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isEmpty());
    }

    @Test
    void decode_whenYearTooHigh_returnsEmpty() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN))
                .thenReturn(sampleResponse("HONDA", "Accord", "2051"));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isEmpty());
    }

    @Test
    void decode_whenYearAtLowerBound_returnsResult() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN))
                .thenReturn(sampleResponse("HONDA", "Accord", "1900"));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isPresent());
        assertEquals(1900, result.get().year());
    }

    @Test
    void decode_whenYearAtUpperBound_returnsResult() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN))
                .thenReturn(sampleResponse("HONDA", "Accord", "2050"));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isPresent());
        assertEquals(2050, result.get().year());
    }

    @Test
    void decode_whenValueNodeNull_skipsField() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN)).thenReturn(objectMapper.readTree("""
                {
                  "Results": [
                    {"Variable": "Make", "Value": null},
                    {"Variable": "Model", "Value": "Accord"},
                    {"Variable": "Model Year", "Value": "2003"}
                  ]
                }
                """));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isEmpty());
    }

    @Test
    void decode_whenVariableNull_skipsRow() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN)).thenReturn(objectMapper.readTree("""
                {
                  "Results": [
                    {"Variable": null, "Value": "HONDA"},
                    {"Variable": "Make", "Value": "HONDA"},
                    {"Variable": "Model", "Value": "Accord"},
                    {"Variable": "Model Year", "Value": "2003"}
                  ]
                }
                """));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isPresent());
        assertEquals("HONDA", result.get().brand());
    }

    @Test
    void decode_whenMakeNotApplicable_returnsEmpty() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN)).thenReturn(objectMapper.readTree("""
                {
                  "Results": [
                    {"Variable": "Make", "Value": "Not Applicable"},
                    {"Variable": "Model", "Value": "Accord"},
                    {"Variable": "Model Year", "Value": "2003"}
                  ]
                }
                """));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isEmpty());
    }

    @Test
    void decode_whenVinHasSurroundingWhitespace_normalizesBeforeLookup() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN)).thenReturn(sampleResponse("HONDA", "Accord", "2003"));

        Optional<VinDecodeResult> result = vinDecodeService.decode("  " + SAMPLE_VIN + "  ");

        assertTrue(result.isPresent());
        verify(nhtsaVinDecodeClient).decodeVin(SAMPLE_VIN);
    }

    @Test
    void decode_whenModelMissing_returnsEmpty() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN)).thenReturn(objectMapper.readTree("""
                {
                  "Results": [
                    {"Variable": "Make", "Value": "HONDA"},
                    {"Variable": "Model Year", "Value": "2003"}
                  ]
                }
                """));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isEmpty());
    }

    @Test
    void decode_whenYearMissing_returnsEmpty() throws Exception {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN)).thenReturn(objectMapper.readTree("""
                {
                  "Results": [
                    {"Variable": "Make", "Value": "HONDA"},
                    {"Variable": "Model", "Value": "Accord"}
                  ]
                }
                """));

        Optional<VinDecodeResult> result = vinDecodeService.decode(SAMPLE_VIN);

        assertTrue(result.isEmpty());
    }

    @Test
    void textValue_whenNodeReferenceIsNull_returnsNull() throws Exception {
        var method = VinDecodeService.class.getDeclaredMethod("textValue", JsonNode.class);
        method.setAccessible(true);

        assertNull(method.invoke(null, (JsonNode) null));
    }

    @Test
    void decode_whenClientUnavailable_propagatesException() {
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN))
                .thenThrow(new VinDecodeUnavailableException(new RuntimeException("timeout")));

        assertThrows(VinDecodeUnavailableException.class, () -> vinDecodeService.decode(SAMPLE_VIN));
    }

    @Test
    void applyToAddCarRequest_whenSuccessful_fillsFields() throws Exception {
        AddCarRequest request = new AddCarRequest();
        request.setVin(SAMPLE_VIN);
        request.setPlateNumber("CB1234AB");
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN)).thenReturn(sampleResponse("HONDA", "Accord", "2003"));

        VinDecodeOutcome outcome = vinDecodeService.applyToAddCarRequest(request);

        assertEquals(VinDecodeOutcome.success(), outcome);
        assertEquals("HONDA", request.getBrand());
        assertEquals("Accord", request.getModel());
        assertEquals(2003, request.getYear());
        assertEquals("CB1234AB", request.getPlateNumber());
    }

    @Test
    void applyToAddCarRequest_whenLookupFails_returnsFailedOutcome() {
        AddCarRequest request = new AddCarRequest();
        request.setVin("INVALID");

        VinDecodeOutcome outcome = vinDecodeService.applyToAddCarRequest(request);

        assertEquals(VinDecodeOutcome.failed(), outcome);
    }

    @Test
    void applyToAddCarRequest_whenServiceUnavailable_returnsUnavailableOutcome() {
        AddCarRequest request = new AddCarRequest();
        request.setVin(SAMPLE_VIN);
        when(nhtsaVinDecodeClient.decodeVin(SAMPLE_VIN))
                .thenThrow(new VinDecodeUnavailableException(new RuntimeException("timeout")));

        VinDecodeOutcome outcome = vinDecodeService.applyToAddCarRequest(request);

        assertEquals(VinDecodeOutcome.unavailable(), outcome);
    }

    private JsonNode sampleResponse(String brand, String model, String year) throws Exception {
        String brandValue = brand == null ? "Not Applicable" : brand;
        String modelValue = model == null ? "Not Applicable" : model;
        String yearValue = year == null ? "Not Applicable" : year;
        String json = """
                {
                  "Results": [
                    {"Variable": "Make", "Value": "%s"},
                    {"Variable": "Model", "Value": "%s"},
                    {"Variable": "Model Year", "Value": "%s"}
                  ]
                }
                """.formatted(brandValue, modelValue, yearValue);
        return objectMapper.readTree(json);
    }
}
