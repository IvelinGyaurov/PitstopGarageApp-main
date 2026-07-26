package com.pitstop.garage;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.Mockito.mockStatic;

@SpringBootTest
class PitstopGarageApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void main_invokesSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            springApplication.when(() -> SpringApplication.run(PitstopGarageApplication.class, new String[]{}))
                    .thenReturn(null);

            PitstopGarageApplication.main(new String[]{});

            springApplication.verify(() -> SpringApplication.run(PitstopGarageApplication.class, new String[]{}));
        }
    }
}
