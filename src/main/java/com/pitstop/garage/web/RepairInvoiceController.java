package com.pitstop.garage.web;

import com.pitstop.garage.repair.model.ServiceRepair;
import com.pitstop.garage.repair.service.RepairInvoicePdfService;
import com.pitstop.garage.repair.service.RepairService;
import com.pitstop.garage.security.PitstopUserDetails;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Controller
public class RepairInvoiceController {

    private final RepairService repairService;
    private final RepairInvoicePdfService invoicePdfService;

    public RepairInvoiceController(RepairService repairService,
                                   RepairInvoicePdfService invoicePdfService) {
        this.repairService = repairService;
        this.invoicePdfService = invoicePdfService;
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/repairs/{id}/invoice")
    public ResponseEntity<byte[]> downloadClientInvoice(@PathVariable UUID id,
                                                        @AuthenticationPrincipal PitstopUserDetails userData) {
        ServiceRepair repair = repairService.getCompletedRepairForClientInvoice(userData.getUserId(), id);
        return pdf(repair);
    }

    @PreAuthorize("hasRole('MECHANIC')")
    @GetMapping("/mechanic/repairs/{id}/invoice")
    public ResponseEntity<byte[]> downloadMechanicInvoice(@PathVariable UUID id,
                                                          @AuthenticationPrincipal PitstopUserDetails userData) {
        ServiceRepair repair = repairService.getCompletedRepairForMechanicInvoice(userData.getUserId(), id);
        return pdf(repair);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/repairs/{id}/invoice")
    public ResponseEntity<byte[]> downloadAdminInvoice(@PathVariable UUID id) {
        ServiceRepair repair = repairService.getCompletedRepairForAdminInvoice(id);
        return pdf(repair);
    }

    private ResponseEntity<byte[]> pdf(ServiceRepair repair) {
        byte[] content = invoicePdfService.generate(repair, LocaleContextHolder.getLocale());
        String filename = "pitstop-invoice-" + repair.getId().toString().substring(0, 8) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }
}
