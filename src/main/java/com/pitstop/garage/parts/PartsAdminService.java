package com.pitstop.garage.parts;

import com.pitstop.garage.client.PartsClient;
import com.pitstop.garage.client.dto.CreatePartRequest;
import com.pitstop.garage.client.dto.PartResponse;
import com.pitstop.garage.exceptions.PartSkuAlreadyExistsException;
import com.pitstop.garage.exceptions.PartSkuAlreadyExistsExceptionMessage;
import com.pitstop.garage.web.dto.AddPartRequest;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PartsAdminService {

    private final PartsClient partsClient;

    public PartsAdminService(PartsClient partsClient) {
        this.partsClient = partsClient;
    }

    public List<PartResponse> getAllParts() {
        return partsClient.getAllParts();
    }

    public void createPart(AddPartRequest form) {
        CreatePartRequest request = toCreatePartRequest(form);
        try {
            partsClient.createPart(request);
            log.info("Admin created part with sku={}", request.getSku());
        } catch (FeignException.BadRequest ex) {
            throw new PartSkuAlreadyExistsException(
                    PartSkuAlreadyExistsExceptionMessage.SKU_ALREADY_EXISTS);
        }
    }

    public void deletePart(UUID partId) {
        partsClient.deletePart(partId);
        log.info("Admin deleted part id={}", partId);
    }

    private CreatePartRequest toCreatePartRequest(AddPartRequest form) {
        CreatePartRequest request = new CreatePartRequest();
        request.setName(form.getName().trim());
        request.setSku(form.getSku().trim());
        request.setUnitPrice(form.getUnitPrice());
        request.setQuantityInStock(form.getQuantityInStock());
        return request;
    }
}