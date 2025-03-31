package fr.eql.ai116.duflot.backend.controller; // Use dots for package name

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.http.ResponseEntity; // Import ResponseEntity
import org.springframework.http.HttpStatus; // Import HttpStatus

@RestController
@RequestMapping("/api") // Base path for all API endpoints in this controller
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class TestController {

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() { // Return ResponseEntity
        // Simple response with OK status
        return new ResponseEntity<>("Hello from Backend!", HttpStatus.OK);
    }
}
