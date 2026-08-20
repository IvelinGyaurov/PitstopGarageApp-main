package com.pitstop.garage.user.service;

import com.pitstop.garage.exceptions.*;
import com.pitstop.garage.repair.model.RepairStatus;
import com.pitstop.garage.repair.repository.ServiceRepairRepository;
import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.model.UserRole;
import com.pitstop.garage.user.repository.UserRepository;
import com.pitstop.garage.web.dto.EditProfileRequest;
import com.pitstop.garage.web.dto.RegisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.pitstop.garage.security.PitstopUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.pitstop.garage.exceptions.UserAlreadyExistExceptionMessage.*;

@Slf4j
@Service
public class UserService implements UserDetailsService  {

    private static final List<RepairStatus> OPEN_MECHANIC_REPAIR_STATUSES = List.of(
            RepairStatus.ACCEPTED,
            RepairStatus.IN_PROGRESS
    );

    private final UserRepository userRepository;
    private final ServiceRepairRepository serviceRepairRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository,
                       ServiceRepairRepository serviceRepairRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.serviceRepairRepository = serviceRepairRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @CacheEvict(value = "users", allEntries = true)
    public void registerUser(RegisterRequest registerRequest) {

        String username = registerRequest.getUsername().trim();
        String email = registerRequest.getEmail().trim().toLowerCase();

        if (userRepository.findByUsername(username).isPresent()) {
            log.error("Username {} already exist", username);
            throw new UserAlreadyExistException(USERNAME_ALREADY_EXIST);
        }

        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            log.error("Email {} already exist", email);
            throw new UserAlreadyExistException(EMAIL_ALREADY_EXIST);
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .email(email)
                .role(UserRole.USER)
                .isActive(Boolean.TRUE)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        int usersCount = userRepository.findAll().size();
        if (usersCount == 0) {
            user.setRole(UserRole.ADMIN);
        }

        userRepository.save(user);
    }

    @CacheEvict(value = "users", allEntries = true)
    public void changeRole(UUID id, UserRole newRole) {
        User user = getById(id);

        if (!user.isActive()) {
            throw new UserInactiveException(UserInactiveExceptionMessage.USER_INACTIVE);
        }

        if (isSoleActiveAdmin(user) && newRole != UserRole.ADMIN) {
            throw new PrimaryUserException(PrimaryUserExceptionMessage.CANNOT_CHANGE_LAST_ADMIN_ROLE);
        }

        user.setRole(newRole);
        user.setUpdatedOn(LocalDateTime.now());
        userRepository.save(user);
    }

    private long countActiveAdmins() {
        return userRepository.countByRoleAndIsActiveTrue(UserRole.ADMIN);
    }

    public boolean isSoleActiveAdmin(User user) {
        return user.getRole() == UserRole.ADMIN
                && user.isActive()
                && countActiveAdmins() == 1;
    }

    public Optional<UUID> getSoleActiveAdminId() {
        if (countActiveAdmins() != 1) {
            return Optional.empty();
        }
        return userRepository.findFirstByRoleAndIsActiveTrue(UserRole.ADMIN)
                .map(User::getId);
    }

    @CacheEvict(value = "users", allEntries = true)
    public void changeActiveStatus(UUID id, boolean active) {
        User user = getById(id);

        if (!active && isSoleActiveAdmin(user)) {
            throw new PrimaryUserException(PrimaryUserExceptionMessage.CANNOT_CHANGE_LAST_ADMIN_STATUS);
        }

        if (!active && serviceRepairRepository.existsByMechanicAndStatusIn(user, OPEN_MECHANIC_REPAIR_STATUSES)) {
            throw new PrimaryUserException(
                    PrimaryUserExceptionMessage.CANNOT_DEACTIVATE_MECHANIC_WITH_OPEN_REPAIRS);
        }

        user.setActive(active);
        user.setUpdatedOn(LocalDateTime.now());
        userRepository.save(user);
    }

    @Cacheable(value = "users", key = "'all'")
    public List<User> getAll() {
        return userRepository.findAllByOrderByUsernameAsc();
    }

    public long countUsers() {
        return userRepository.count();
    }

    @Cacheable(value = "users", key = "#id")
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(UserNotFoundExceptionMessage.USER_NOT_FOUND));
    }

    @CacheEvict(value = "users", allEntries = true)
    public void updateProfile(UUID id, EditProfileRequest editProfileRequest) {

        User user = getById(id);

        user.setFirstName(editProfileRequest.getFirstName());
        user.setLastName(editProfileRequest.getLastName());
        user.setPhoneNumber(editProfileRequest.getPhoneNumber());
        user.setProfilePicture(editProfileRequest.getProfilePictureURL());
        user.setUpdatedOn(LocalDateTime.now());

        userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UserNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(UserNotFoundExceptionMessage.USER_NOT_FOUND));
        return new PitstopUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                user.isActive()
        );
    }
}
