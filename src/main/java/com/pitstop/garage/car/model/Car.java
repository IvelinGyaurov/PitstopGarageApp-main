package com.pitstop.garage.car.model;

import com.pitstop.garage.repair.model.ServiceRepair;
import com.pitstop.garage.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cars")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(min = 17, max = 17)
    @Column(nullable = false, unique = true, length = 17)
    private String vin;

    @NotBlank
    @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String plateNumber;

    @NotBlank
    @Size(min = 2, max = 50)
    @Column(nullable = false, length = 50)
    private String brand;

    @NotBlank
    @Size(min = 1, max = 50)
    @Column(nullable = false, length = 50)
    private String model;

    @NotNull
    @Min(1900)
    @Max(2030)
    private Integer year;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String engineType;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String transmission;

    @NotNull
    @Min(0)
    @Max(2000000)
    private Integer mileage;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "car", fetch = FetchType.EAGER)
    @Builder.Default
    private List<ServiceRepair> serviceRepairs = new ArrayList<>();

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
