package com.pitstop.garage.repair.repository;

import com.pitstop.garage.repair.model.RepairStatus;
import com.pitstop.garage.repair.model.ServiceRepair;
import com.pitstop.garage.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRepairRepository extends JpaRepository<ServiceRepair, UUID> {

    Optional<ServiceRepair> findByIdAndClient(UUID id, User client);

    List<ServiceRepair> findAllByStatusAndMechanicIsNullOrderByCreatedOnDesc(RepairStatus status);

    List<ServiceRepair> findAllByMechanicAndStatusInOrderByCreatedOnDesc(
            User mechanic,
            List<RepairStatus> statuses
    );

    List<ServiceRepair> findAllByStatusAndCreatedOnBefore(
            RepairStatus status,
            LocalDateTime createdOn
    );

    List<ServiceRepair> findAllByClientAndStatusIn(User client, List<RepairStatus> statuses);
}
