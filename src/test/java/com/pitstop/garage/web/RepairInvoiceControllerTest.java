package com.pitstop.garage.web;

import com.pitstop.garage.repair.model.ServiceRepair;
import com.pitstop.garage.repair.service.RepairInvoicePdfService;
import com.pitstop.garage.repair.service.RepairService;
import com.pitstop.garage.security.PitstopUserDetails;
import com.pitstop.garage.user.model.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepairInvoiceControllerTest {

    @Mock
    private RepairService repairService;

    @Mock
    private RepairInvoicePdfService invoicePdfService;

    @InjectMocks
    private RepairInvoiceController controller;

    @Test
    void downloadClientInvoice_returnsPdfAttachment() {
        UUID repairId = UUID.randomUUID();
        PitstopUserDetails user = principal(UserRole.USER);
        ServiceRepair repair = repair(repairId);
        byte[] pdf = "%PDF-client".getBytes();

        when(repairService.getCompletedRepairForClientInvoice(user.getUserId(), repairId)).thenReturn(repair);
        when(invoicePdfService.generate(eq(repair), any(Locale.class))).thenReturn(pdf);

        ResponseEntity<byte[]> response = controller.downloadClientInvoice(repairId, user);

        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .contains("pitstop-invoice-" + repairId.toString().substring(0, 8)));
        assertArrayEquals(pdf, response.getBody());
        verify(repairService).getCompletedRepairForClientInvoice(user.getUserId(), repairId);
    }

    @Test
    void downloadMechanicInvoice_returnsPdfAttachment() {
        UUID repairId = UUID.randomUUID();
        PitstopUserDetails user = principal(UserRole.MECHANIC);
        ServiceRepair repair = repair(repairId);
        byte[] pdf = "%PDF-mech".getBytes();

        when(repairService.getCompletedRepairForMechanicInvoice(user.getUserId(), repairId)).thenReturn(repair);
        when(invoicePdfService.generate(eq(repair), any(Locale.class))).thenReturn(pdf);

        ResponseEntity<byte[]> response = controller.downloadMechanicInvoice(repairId, user);

        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertArrayEquals(pdf, response.getBody());
    }

    @Test
    void downloadAdminInvoice_returnsPdfAttachment() {
        UUID repairId = UUID.randomUUID();
        ServiceRepair repair = repair(repairId);
        byte[] pdf = "%PDF-admin".getBytes();

        when(repairService.getCompletedRepairForAdminInvoice(repairId)).thenReturn(repair);
        when(invoicePdfService.generate(eq(repair), any(Locale.class))).thenReturn(pdf);

        ResponseEntity<byte[]> response = controller.downloadAdminInvoice(repairId);

        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertArrayEquals(pdf, response.getBody());
    }

    private PitstopUserDetails principal(UserRole role) {
        return new PitstopUserDetails(UUID.randomUUID(), "user", "pass", role, true);
    }

    private ServiceRepair repair(UUID id) {
        ServiceRepair repair = new ServiceRepair();
        repair.setId(id);
        return repair;
    }
}
