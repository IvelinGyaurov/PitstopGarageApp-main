package com.pitstop.garage.repair.service;

import com.pitstop.garage.car.model.Car;
import com.pitstop.garage.repair.model.RepairStatus;
import com.pitstop.garage.repair.model.ServiceRepair;
import com.pitstop.garage.repair.model.UsedPart;
import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepairInvoicePdfServiceTest {

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private RepairInvoicePdfService invoicePdfService;

    @BeforeEach
    void stubMessages() {
        when(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
                .thenAnswer(invocation -> {
                    Object[] args = invocation.getArgument(1);
                    String code = invocation.getArgument(0);
                    if (args != null && args.length > 0 && "common.eur".equals(code)) {
                        return args[0] + " EUR";
                    }
                    return code;
                });
    }

    @Test
    void generate_withParts_returnsPdfBytes() {
        byte[] pdf = invoicePdfService.generate(completedRepair(true), Locale.ENGLISH);

        assertTrue(pdf.length > 100);
        assertTrue(new String(pdf, 0, 4).startsWith("%PDF"));
    }

    @Test
    void generate_withoutParts_returnsPdfBytes() {
        byte[] pdf = invoicePdfService.generate(completedRepair(false), new Locale("bg"));

        assertTrue(pdf.length > 100);
        assertTrue(new String(pdf, 0, 4).startsWith("%PDF"));
    }

    @Test
    void generate_withSparseOptionalFields_returnsPdfBytes() {
        ServiceRepair repair = completedRepair(false);
        repair.setProblemDescription(null);
        repair.setLaborCost(null);
        repair.setMechanic(null);
        repair.setAcceptedAt(null);
        repair.setStartedAt(null);
        repair.setUsedParts(null);
        repair.getClient().setFirstName(null);
        repair.getClient().setLastName(null);
        repair.getCar().setYear(null);

        byte[] pdf = invoicePdfService.generate(repair, null);

        assertTrue(pdf.length > 100);
        assertTrue(new String(pdf, 0, 4).startsWith("%PDF"));
    }

    @Test
    void generate_whenRepairGraphBroken_throwsIllegalState() {
        ServiceRepair repair = completedRepair(false);
        repair.setCar(null);

        assertThrows(IllegalStateException.class,
                () -> invoicePdfService.generate(repair, Locale.ENGLISH));
    }

    private ServiceRepair completedRepair(boolean withParts) {
        User client = User.builder()
                .id(UUID.randomUUID())
                .username("client1")
                .email("client@test.com")
                .password("pass")
                .firstName("Ivan")
                .lastName("Petrov")
                .role(UserRole.USER)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
        User mechanic = User.builder()
                .id(UUID.randomUUID())
                .username("mech1")
                .email("mech@test.com")
                .password("pass")
                .role(UserRole.MECHANIC)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
        Car car = Car.builder()
                .id(UUID.randomUUID())
                .vin("WVWZZZ1JZXW000001")
                .plateNumber("CA1234AB")
                .brand("VW")
                .model("Golf")
                .year(2018)
                .engineType("PETROL")
                .transmission("MANUAL")
                .owner(client)
                .build();

        ServiceRepair repair = ServiceRepair.builder()
                .id(UUID.randomUUID())
                .problemDescription("Brake noise")
                .status(RepairStatus.COMPLETED)
                .laborCost(new BigDecimal("80.00"))
                .createdOn(LocalDateTime.now().minusDays(3))
                .acceptedAt(LocalDateTime.now().minusDays(2))
                .startedAt(LocalDateTime.now().minusDays(1))
                .completedAt(LocalDateTime.now())
                .client(client)
                .mechanic(mechanic)
                .car(car)
                .usedParts(new ArrayList<>())
                .build();

        if (withParts) {
            UsedPart part = UsedPart.builder()
                    .id(UUID.randomUUID())
                    .partId(UUID.randomUUID())
                    .partName("Brake pads")
                    .quantity(2)
                    .unitPrice(new BigDecimal("45.00"))
                    .serviceRepair(repair)
                    .build();
            repair.setUsedParts(List.of(part));
        }

        return repair;
    }
}
