package com.gymcrm.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("DiscoveryServerApplication")
class DiscoveryServerApplicationTests {

    @Test
    @DisplayName("the Eureka server context loads successfully")
    void contextLoads() {
    }
}
