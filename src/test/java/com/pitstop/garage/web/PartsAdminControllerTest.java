package com.pitstop.garage.web;

import com.pitstop.garage.parts.PartsAdminService;
import com.pitstop.garage.web.dto.AddPartRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartsAdminControllerTest {

    @Mock
    private PartsAdminService partsAdminService;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private PartsAdminController controller;

    @Test
    void listParts_returnsView() {
        when(partsAdminService.getAllParts()).thenReturn(List.of());
        assertEquals("admin-parts", controller.listParts().getViewName());
    }

    @Test
    void addPartForm_returnsForm() {
        assertEquals("admin-parts-add", controller.addPartForm().getViewName());
    }

    @Test
    void createPart_whenValid_redirects() {
        AddPartRequest request = new AddPartRequest();
        request.setName("Oil");
        request.setSku("OIL-1");
        request.setUnitPrice(new BigDecimal("10.00"));
        request.setQuantityInStock(3);
        when(bindingResult.hasErrors()).thenReturn(false);

        ModelAndView mav = controller.createPart(request, bindingResult, redirectAttributes);

        verify(partsAdminService).createPart(request);
        assertEquals("redirect:/admin/parts", mav.getViewName());
    }

    @Test
    void createPart_whenInvalid_staysOnForm() {
        AddPartRequest request = new AddPartRequest();
        when(bindingResult.hasErrors()).thenReturn(true);

        ModelAndView mav = controller.createPart(request, bindingResult, redirectAttributes);

        assertEquals("admin-parts-add", mav.getViewName());
        verify(partsAdminService, never()).createPart(any());
    }

    @Test
    void deletePart_redirects() {
        UUID id = UUID.randomUUID();
        ModelAndView mav = controller.deletePart(id, redirectAttributes);
        verify(partsAdminService).deletePart(id);
        assertEquals("redirect:/admin/parts", mav.getViewName());
    }
}
