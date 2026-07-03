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

    @Column(nullable = false, unique = true, length = 17)
    private String vin;

    @Column(nullable = false, length = 10)
    private String plateNumber;

    @Column(nullable = false, length = 50)
    private String brand;

    @Column(nullable = false, length = 50)
    private String model;

    private Integer year;

    @Column(nullable = false, length = 30)
    private String engineType;

    @Column(nullable = false, length = 30)
    private String transmission;

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
