package com.pitstop.garage.user.model;

import com.pitstop.garage.car.model.Car;
import com.pitstop.garage.repair.model.ServiceRepair;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(min = 3, max = 20)
    @Column(nullable = false, unique = true, length = 20)
    private String username;

    @NotBlank
    @Email
    @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank
    @Column(nullable = false, length = 60)
    private String password;

    @Size(max = 50)
    @Column(length = 50)
    private String firstName;

    @Size(max = 50)
    @Column(length = 50)
    private String lastName;

    @Size(max = 20)
    @Column(length = 20)
    private String phoneNumber;

    @Size(max = 500)
    @Column(length = 500)
    private String profilePicture;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(nullable = false)
    private LocalDateTime createdOn;

    @Column(nullable = false)
    private LocalDateTime updatedOn;

    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER)
    @Builder.Default
    private List<Car> ownedCars = new ArrayList<>();

    @OneToMany(mappedBy = "client", fetch = FetchType.EAGER)
    @Builder.Default
    private List<ServiceRepair> clientRepairs = new ArrayList<>();

    @OneToMany(mappedBy = "mechanic", fetch = FetchType.EAGER)
    @Builder.Default
    private List<ServiceRepair> mechanicRepairs = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdOn == null) {
            createdOn = LocalDateTime.now();
        }
    }
}
