package com.pitstop.garage.web;

import com.pitstop.garage.config.MessageHelper;
import com.pitstop.garage.parts.PartsAdminService;
import com.pitstop.garage.client.dto.PartResponse;
import com.pitstop.garage.web.dto.AddPartRequest;
import com.pitstop.garage.web.dto.RestockPartForm;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartsAdminControllerTest {

    @Mock
    private PartsAdminService partsAdminService;

    @Mock
    private MessageHelper messages;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private PartsAdminController controller;

    @BeforeEach
    void stubMessages() {
        lenient().when(messages.get(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

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

    @Test
    void restockPartForm_returnsView() {
        UUID id = UUID.randomUUID();
        PartResponse part = new PartResponse();
        part.setId(id);
        when(partsAdminService.getPartById(id)).thenReturn(part);

        ModelAndView mav = controller.restockPartForm(id);

        assertEquals("admin-parts-restock", mav.getViewName());
        assertEquals(part, mav.getModel().get("part"));
    }

    @Test
    void restockPart_whenValid_redirects() {
        UUID id = UUID.randomUUID();
        RestockPartForm form = new RestockPartForm();
        form.setQuantityToAdd(5);
        when(bindingResult.hasErrors()).thenReturn(false);

        ModelAndView mav = controller.restockPart(id, form, bindingResult, redirectAttributes);

        verify(partsAdminService).restockPart(id, form);
        assertEquals("redirect:/admin/parts", mav.getViewName());
    }

    @Test
    void restockPart_whenInvalid_staysOnForm() {
        UUID id = UUID.randomUUID();
        RestockPartForm form = new RestockPartForm();
        PartResponse part = new PartResponse();
        when(bindingResult.hasErrors()).thenReturn(true);
        when(partsAdminService.getPartById(id)).thenReturn(part);

        ModelAndView mav = controller.restockPart(id, form, bindingResult, redirectAttributes);

        assertEquals("admin-parts-restock", mav.getViewName());
        verify(partsAdminService, never()).restockPart(any(), any());
    }
}
