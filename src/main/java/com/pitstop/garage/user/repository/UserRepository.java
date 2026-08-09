package com.pitstop.garage.user.repository;

import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    List<User> findAllByOrderByUsernameAsc();

    long countByRoleAndIsActiveTrue(UserRole role);

    Optional<User> findFirstByRoleAndIsActiveTrue(UserRole role);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsername(String username);

}
