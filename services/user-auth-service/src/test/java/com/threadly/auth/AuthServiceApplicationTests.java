package com.threadly.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

// Context load is deferred to Docker/integration; unit tests cover password hashing.
class AuthServiceApplicationTests {
    @Test
    void placeholder() {
        // See PasswordEncodingTest for fast unit coverage without a database.
    }
}
