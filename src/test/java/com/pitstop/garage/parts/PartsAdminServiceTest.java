package com.pitstop.garage.parts;

import com.pitstop.garage.client.PartsClient;
import com.pitstop.garage.client.dto.CreatePartRequest;
import com.pitstop.garage.client.dto.PartResponse;
import com.pitstop.garage.client.dto.RestockPartRequest;
import com.pitstop.garage.exceptions.PartSkuAlreadyExistsException;
import com.pitstop.garage.web.dto.AddPartRequest;
import com.pitstop.garage.web.dto.RestockPartForm;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartsAdminServiceTest {

    @Mock
    private PartsClient partsClient;

    @InjectMocks
    private PartsAdminService partsAdminService;

    @Test
    void getAllParts_delegatesToClient() {
        PartResponse part = new PartResponse();
        part.setSku("OIL-1");
        when(partsClient.getAllParts()).thenReturn(List.of(part));

        List<PartResponse> result = partsAdminService.getAllParts();

        assertEquals(1, result.size());
        assertEquals("OIL-1", result.get(0).getSku());
    }

    @Test
    void createPart_whenOk_callsClient() {
        AddPartRequest form = new AddPartRequest();
        form.setName("  Oil Filter ");
        form.setSku(" OIL-1 ");
        form.setUnitPrice(new BigDecimal("12.50"));
        form.setQuantityInStock(5);

        when(partsClient.createPart(any(CreatePartRequest.class))).thenReturn(new PartResponse());

        partsAdminService.createPart(form);

        ArgumentCaptor<CreatePartRequest> captor = ArgumentCaptor.forClass(CreatePartRequest.class);
        verify(partsClient).createPart(captor.capture());
        assertEquals("Oil Filter", captor.getValue().getName());
        assertEquals("OIL-1", captor.getValue().getSku());
        assertEquals(new BigDecimal("12.50"), captor.getValue().getUnitPrice());
        assertEquals(5, captor.getValue().getQuantityInStock());
    }

    @Test
    void createPart_whenBadRequest_throwsSkuException() {
        AddPartRequest form = new AddPartRequest();
        form.setName("Oil Filter");
        form.setSku("OIL-1");
        form.setUnitPrice(new BigDecimal("12.50"));
        form.setQuantityInStock(5);

        Request request = Request.create(Request.HttpMethod.POST, "/api/parts",
                Collections.emptyMap(), null, StandardCharsets.UTF_8);
        when(partsClient.createPart(any(CreatePartRequest.class)))
                .thenThrow(new FeignException.BadRequest("exists", request, null, Collections.emptyMap()));

        assertThrows(PartSkuAlreadyExistsException.class, () -> partsAdminService.createPart(form));
    }

    @Test
    void deletePart_delegatesToClient() {
        UUID id = UUID.randomUUID();

        partsAdminService.deletePart(id);

        verify(partsClient).deletePart(id);
    }

    @Test
    void getPartById_delegatesToClient() {
        UUID id = UUID.randomUUID();
        PartResponse part = new PartResponse();
        part.setId(id);
        when(partsClient.getPartById(id)).thenReturn(part);

        assertEquals(id, partsAdminService.getPartById(id).getId());
    }

    @Test
    void restockPart_delegatesToClient() {
        UUID id = UUID.randomUUID();
        RestockPartForm form = new RestockPartForm();
        form.setQuantityToAdd(12);

        partsAdminService.restockPart(id, form);

        ArgumentCaptor<RestockPartRequest> captor = ArgumentCaptor.forClass(RestockPartRequest.class);
        verify(partsClient).restockPart(eq(id), captor.capture());
        assertEquals(12, captor.getValue().getQuantityToAdd());
    }
}
