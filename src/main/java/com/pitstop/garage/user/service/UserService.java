package com.pitstop.garage.user.service;

import com.pitstop.garage.exceptions.*;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.pitstop.garage.exceptions.UserAlreadyExistExceptionMessage.*;

@Slf4j
@Service
public class UserService implements UserDetailsService  {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
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

        if (isFirstRegisteredUser(id)) {
            throw new PrimaryUserException(PrimaryUserExceptionMessage.CANNOT_CHANGE_PRIMARY_USER_ROLE);
        }

        user.setRole(newRole);
        user.setUpdatedOn(LocalDateTime.now());
        userRepository.save(user);
    }

    public Optional<UUID> getFirstRegisteredUserId() {
        return userRepository.findTopByOrderByCreatedOnAsc()
                .map(User::getId);
    }

    public boolean isFirstRegisteredUser(UUID id) {
        return getFirstRegisteredUserId()
                .map(firstId -> firstId.equals(id))
                .orElse(false);
    }

    @CacheEvict(value = "users", allEntries = true)
    public void changeActiveStatus(UUID id, boolean active) {
        User user = getById(id);

        if (isFirstRegisteredUser(id)) {
            throw new PrimaryUserException(PrimaryUserExceptionMessage.CANNOT_CHANGE_PRIMARY_USER_STATUS);
        }

        user.setActive(active);
        user.setUpdatedOn(LocalDateTime.now());
        userRepository.save(user);
    }

    @Cacheable(value = "users")
    public List<User> getAll() {

        return userRepository.findAll();
    }

    public User getById(UUID id) {

        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(UserNotFoundExceptionMessage.USER_NOT_FOUND));
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
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new PitstopUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                user.isActive()
        );
    }
}
