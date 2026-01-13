package com.codenames;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration test for the main application class.
 * Verifies that the Spring application context loads successfully
 * with all configured beans and components.
 */
@SpringBootTest
class CodenamesApplicationTest {

    /**
     * Test that Spring context loads without errors.
     * This verifies:
     * - All configuration classes are valid
     * - All beans can be created
     * - No circular dependencies exist
     * - Application properties are valid
     */
    @Test
    void contextLoads() {
        // If this test passes, the Spring context loaded successfully
        // No assertion needed - failure to load would throw an exception
    }
}
