package com.pitstop.garage.user.service;

import com.pitstop.garage.exceptions.PrimaryUserException;
import com.pitstop.garage.exceptions.UserAlreadyExistException;
import com.pitstop.garage.exceptions.UserInactiveException;
import com.pitstop.garage.exceptions.UserNotFoundException;
import com.pitstop.garage.repair.model.RepairStatus;
import com.pitstop.garage.repair.repository.ServiceRepairRepository;
import com.pitstop.garage.security.PitstopUserDetails;
import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.model.UserRole;
import com.pitstop.garage.user.repository.UserRepository;
import com.pitstop.garage.web.dto.EditProfileRequest;
import com.pitstop.garage.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServiceRepairRepository serviceRepairRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void registerUser_whenNoUsersExist_createsAdmin() {
        RegisterRequest request = RegisterRequest.builder()
                .username("admin1")
                .password("pass")
                .email("Admin1@Mail.com")
                .build();

        when(userRepository.findByUsername("admin1")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("admin1@mail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(userRepository.findAll()).thenReturn(List.of());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean firstUser = userService.registerUser(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertTrue(firstUser);
        assertEquals("admin1", saved.getUsername());
        assertEquals("admin1@mail.com", saved.getEmail());
        assertEquals("encoded", saved.getPassword());
        assertEquals(UserRole.ADMIN, saved.getRole());
        assertTrue(saved.isActive());
    }

    @Test
    void registerUser_whenUsersExist_createsUserRole() {
        RegisterRequest request = RegisterRequest.builder()
                .username("user1")
                .password("pass")
                .email("user1@mail.com")
                .build();

        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("user1@mail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(userRepository.findAll()).thenReturn(List.of(User.builder().username("existing").build()));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean firstUser = userService.registerUser(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertFalse(firstUser);
        assertEquals(UserRole.USER, captor.getValue().getRole());
    }

    @Test
    void registerUser_whenUsernameExists_throws() {
        RegisterRequest request = RegisterRequest.builder()
                .username("taken")
                .password("pass")
                .email("new@mail.com")
                .build();

        when(userRepository.findByUsername("taken"))
                .thenReturn(Optional.of(User.builder().username("taken").build()));

        assertThrows(UserAlreadyExistException.class, () -> userService.registerUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_whenEmailExists_throws() {
        RegisterRequest request = RegisterRequest.builder()
                .username("fresh")
                .password("pass")
                .email("taken@mail.com")
                .build();

        when(userRepository.findByUsername("fresh")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("taken@mail.com"))
                .thenReturn(Optional.of(User.builder().email("taken@mail.com").build()));

        assertThrows(UserAlreadyExistException.class, () -> userService.registerUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeRole_whenSoleAdmin_throws() {
        UUID id = UUID.randomUUID();
        User admin = activeUser(id, "admin", UserRole.ADMIN);

        when(userRepository.findById(id)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndIsActiveTrue(UserRole.ADMIN)).thenReturn(1L);

        assertThrows(PrimaryUserException.class,
                () -> userService.changeRole(id, UserRole.USER));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeRole_whenSoleAdminKeepsAdmin_updates() {
        UUID id = UUID.randomUUID();
        User admin = activeUser(id, "admin", UserRole.ADMIN);

        when(userRepository.findById(id)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndIsActiveTrue(UserRole.ADMIN)).thenReturn(1L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.changeRole(id, UserRole.ADMIN);

        assertEquals(UserRole.ADMIN, admin.getRole());
        verify(userRepository).save(admin);
    }

    @Test
    void changeRole_whenMultipleAdmins_updatesRole() {
        UUID id = UUID.randomUUID();
        User admin = activeUser(id, "admin", UserRole.ADMIN);

        when(userRepository.findById(id)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndIsActiveTrue(UserRole.ADMIN)).thenReturn(2L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.changeRole(id, UserRole.MECHANIC);

        assertEquals(UserRole.MECHANIC, admin.getRole());
        verify(userRepository).save(admin);
    }

    @Test
    void changeRole_whenUserInactive_throws() {
        UUID id = UUID.randomUUID();
        User inactive = activeUser(id, "user", UserRole.USER);
        inactive.setActive(false);

        when(userRepository.findById(id)).thenReturn(Optional.of(inactive));

        assertThrows(UserInactiveException.class,
                () -> userService.changeRole(id, UserRole.MECHANIC));
    }

    @Test
    void changeActiveStatus_whenDeactivatingSoleAdmin_throws() {
        UUID id = UUID.randomUUID();
        User admin = activeUser(id, "admin", UserRole.ADMIN);

        when(userRepository.findById(id)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndIsActiveTrue(UserRole.ADMIN)).thenReturn(1L);

        assertThrows(PrimaryUserException.class,
                () -> userService.changeActiveStatus(id, false));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeActiveStatus_whenDeactivatingNonSoleAdmin_updates() {
        UUID id = UUID.randomUUID();
        User admin = activeUser(id, "admin", UserRole.ADMIN);

        when(userRepository.findById(id)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndIsActiveTrue(UserRole.ADMIN)).thenReturn(2L);
        when(serviceRepairRepository.existsByMechanicAndStatusIn(
                eq(admin), eq(List.of(RepairStatus.ACCEPTED, RepairStatus.IN_PROGRESS))))
                .thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.changeActiveStatus(id, false);

        assertFalse(admin.isActive());
        verify(userRepository).save(admin);
    }

    @Test
    void changeActiveStatus_whenDeactivatingMechanicWithOpenRepairs_throws() {
        UUID id = UUID.randomUUID();
        User mechanic = activeUser(id, "mech", UserRole.MECHANIC);

        when(userRepository.findById(id)).thenReturn(Optional.of(mechanic));
        when(serviceRepairRepository.existsByMechanicAndStatusIn(
                eq(mechanic), eq(List.of(RepairStatus.ACCEPTED, RepairStatus.IN_PROGRESS))))
                .thenReturn(true);

        assertThrows(PrimaryUserException.class,
                () -> userService.changeActiveStatus(id, false));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeActiveStatus_whenDeactivatingMechanicWithoutOpenRepairs_updates() {
        UUID id = UUID.randomUUID();
        User mechanic = activeUser(id, "mech", UserRole.MECHANIC);

        when(userRepository.findById(id)).thenReturn(Optional.of(mechanic));
        when(serviceRepairRepository.existsByMechanicAndStatusIn(
                eq(mechanic), eq(List.of(RepairStatus.ACCEPTED, RepairStatus.IN_PROGRESS))))
                .thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.changeActiveStatus(id, false);

        assertFalse(mechanic.isActive());
        verify(userRepository).save(mechanic);
    }

    @Test
    void changeActiveStatus_whenActivatingSoleAdmin_updates() {
        UUID id = UUID.randomUUID();
        User admin = activeUser(id, "admin", UserRole.ADMIN);

        when(userRepository.findById(id)).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.changeActiveStatus(id, true);

        assertTrue(admin.isActive());
        verify(userRepository).save(admin);
    }

    @Test
    void isSoleActiveAdmin_whenUserRole_returnsFalse() {
        User user = activeUser(UUID.randomUUID(), "user", UserRole.USER);
        assertFalse(userService.isSoleActiveAdmin(user));
    }

    @Test
    void isSoleActiveAdmin_whenInactiveAdmin_returnsFalse() {
        User admin = activeUser(UUID.randomUUID(), "admin", UserRole.ADMIN);
        admin.setActive(false);
        assertFalse(userService.isSoleActiveAdmin(admin));
    }

    @Test
    void changeActiveStatus_whenActivating_updatesStatus() {
        UUID id = UUID.randomUUID();
        User user = activeUser(id, "user", UserRole.USER);
        user.setActive(false);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.changeActiveStatus(id, true);

        assertTrue(user.isActive());
        verify(userRepository).save(user);
    }

    @Test
    void getById_whenMissing_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getById(id));
    }

    @Test
    void getSoleActiveAdminId_whenExactlyOne_returnsId() {
        UUID id = UUID.randomUUID();
        User admin = activeUser(id, "admin", UserRole.ADMIN);

        when(userRepository.countByRoleAndIsActiveTrue(UserRole.ADMIN)).thenReturn(1L);
        when(userRepository.findFirstByRoleAndIsActiveTrue(UserRole.ADMIN))
                .thenReturn(Optional.of(admin));

        Optional<UUID> result = userService.getSoleActiveAdminId();

        assertTrue(result.isPresent());
        assertEquals(id, result.get());
    }

    @Test
    void getSoleActiveAdminId_whenZeroOrMany_returnsEmpty() {
        when(userRepository.countByRoleAndIsActiveTrue(UserRole.ADMIN)).thenReturn(2L);

        assertTrue(userService.getSoleActiveAdminId().isEmpty());
    }

    @Test
    void updateProfile_updatesFields() {
        UUID id = UUID.randomUUID();
        User user = activeUser(id, "user", UserRole.USER);
        EditProfileRequest request = EditProfileRequest.builder()
                .firstName("Ada")
                .lastName("Lovelace")
                .phoneNumber("+359888123456")
                .profilePictureURL("https://example.com/a.png")
                .build();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateProfile(id, request);

        assertEquals("Ada", user.getFirstName());
        assertEquals("Lovelace", user.getLastName());
        assertEquals("+359888123456", user.getPhoneNumber());
        assertEquals("https://example.com/a.png", user.getProfilePicture());
        verify(userRepository).save(user);
    }

    @Test
    void loadUserByUsername_returnsDetails() {
        UUID id = UUID.randomUUID();
        User user = activeUser(id, "mechanic1", UserRole.MECHANIC);
        user.setPassword("secret");

        when(userRepository.findByUsername("mechanic1")).thenReturn(Optional.of(user));

        UserDetails details = userService.loadUserByUsername("mechanic1");

        assertInstanceOf(PitstopUserDetails.class, details);
        PitstopUserDetails pitstop = (PitstopUserDetails) details;
        assertEquals(id, pitstop.getUserId());
        assertEquals("mechanic1", pitstop.getUsername());
        assertEquals(UserRole.MECHANIC, pitstop.getRole());
        assertTrue(pitstop.isEnabled());
    }

    @Test
    void loadUserByUsername_whenMissing_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.loadUserByUsername("ghost"));
    }

    @Test
    void getAll_and_countUsers_delegateToRepository() {
        when(userRepository.findAllByOrderByUsernameAsc()).thenReturn(List.of());
        when(userRepository.count()).thenReturn(3L);

        assertTrue(userService.getAll().isEmpty());
        assertEquals(3L, userService.countUsers());
    }

    private User activeUser(UUID id, String username, UserRole role) {
        return User.builder()
                .id(id)
                .username(username)
                .email(username + "@mail.com")
                .password("encoded")
                .role(role)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }
}
