package fr.eql.ai116.duflot.backend.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import fr.eql.ai116.duflot.backend.service.ResumeParsingOrchestrator;
import fr.eql.ai116.duflot.backend.util.SseService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/parse")
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class ResumeParseEventsController {

    private static final Logger logger = LogManager.getLogger(ResumeParseEventsController.class);

    @Autowired
    private SseService sseService;

    @Autowired
    private ResumeParsingOrchestrator resumeParsingOrchestrator;

    /**
     * Endpoint to initiate the parsing job.
     * Accepts a PDF file, starts the async parsing, and returns a job ID.
     * @param file The uploaded PDF file.
     * @return A ResponseEntity containing the jobId.
     */
    @PostMapping("/initiate")
    public ResponseEntity<Map<String, String>> initializeJob(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            logger.warn("Received empty file upload attempt.");
            return ResponseEntity.badRequest().body(Map.of("error", "File cannot be empty"));
        }
        // Optional: Add more validation (e.g., file type, size)
        if (file.getContentType() != null && !file.getContentType().equals("application/pdf")) {
            logger.warn("Received non-PDF file upload attempt: {}", file.getContentType());
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(Map.of("error", "Only PDF files are allowed"));
        }

        String jobId = UUID.randomUUID().toString();
        logger.info("Initiating parsing job with ID: {}", jobId);

        try {
            // Start the background parsing task
            resumeParsingOrchestrator.parseResumeAsync(jobId, file);

            // Return the Job ID immediately
            return ResponseEntity.ok(Map.of("jobId", jobId));

        } catch (Exception e) {
            // Catch potential immediate errors during job kickoff (less likely with @Async)
            logger.error("Failed to initiate async parsing for Job ID: {}", jobId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to start parsing job: " + e.getMessage()));
        }
    }

    /**
     * Endpoint for clients to connect and receive status updates via SSE.
     * @param jobId The ID of the job to get status for.
     * @return An SseEmitter instance for the client connection.
     */
    @GetMapping("/status/{jobId}")
    public SseEmitter streamUpdates(@PathVariable String jobId) {
        logger.info("Client connected for SSE updates for Job ID: {}", jobId);
        try {
            // Create or retrieve emitter for this job ID
            // SseService handles storing and managing the emitter lifecycle
            return sseService.createEmitter(jobId);
        } catch (Exception e) {
            // Handle cases where emitter creation fails immediately
            logger.error("Error creating SseEmitter for Job ID {}: {}", jobId, e.getMessage());
            // Throwing ResponseStatusException translates to an HTTP error response
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error setting up status stream", e);
        }
        // Note: If the jobId relates to a job that *never* started, the emitter will
        // likely just timeout eventually, or the async task won't send updates.
        // You could add a check: if (!jobExists(jobId)) return 404; but that requires
        // tracking job *initiation* status separately if the async task hasn't run yet.
    }
}
