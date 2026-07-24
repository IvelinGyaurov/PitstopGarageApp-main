package com.pitstop.garage.api;

import com.pitstop.garage.security.PitstopUserDetails;
import com.pitstop.garage.user.model.User;
import com.pitstop.garage.user.repository.UserRepository;
import com.pitstop.garage.user.service.UserService;
import com.pitstop.garage.web.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IndexApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void getIndex_returnsOk() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void getLogin_returnsOk() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("loginRequest"));
    }

    @Test
    void getRegister_returnsOk() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void postRegister_withValidData_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("password", "pass")
                        .param("email", "newuser@mail.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void postRegister_withInvalidData_staysOnRegister() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "ab")
                        .param("password", "1")
                        .param("email", "bad"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void getHome_whenAuthenticated_returnsHome() throws Exception {
        userService.registerUser(RegisterRequest.builder()
                .username("homeuser")
                .password("pass")
                .email("homeuser@mail.com")
                .build());
        User saved = userRepository.findByUsername("homeuser").orElseThrow();

        PitstopUserDetails principal = new PitstopUserDetails(
                saved.getId(),
                saved.getUsername(),
                saved.getPassword(),
                saved.getRole(),
                saved.isActive()
        );

        mockMvc.perform(get("/home").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(containsString("homeuser")));
    }

    @Test
    void getCars_whenUnauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/cars"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
