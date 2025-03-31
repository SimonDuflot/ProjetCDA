package fr.eql.ai116.duflot.backend.controller; // Use dots for package name

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest // Loads the full application context for the test
@AutoConfigureMockMvc // Auto-configures MockMvc for web layer testing
@ActiveProfiles("test") // Ensures the 'test' profile (using H2) is active
class TestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc; // Injects MockMvc instance

    @Test
    void testTestEndpoint() throws Exception {
        mockMvc.perform(get("/api/test")) // Perform GET request to the endpoint
               .andExpect(status().isOk()) // Assert HTTP status is 200 OK
               .andExpect(content().string("Hello from Backend!")); // Assert response body matches
    }
}
