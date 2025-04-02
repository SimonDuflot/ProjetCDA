package fr.eql.ai116.duflot.backend.controller;

import fr.eql.ai116.duflot.backend.entity.ResumeTextItemEntity;
import fr.eql.ai116.duflot.backend.service.ResumeParsingOrchestrator;
import fr.eql.ai116.duflot.backend.service.ResumeParsingService;
import fr.eql.ai116.duflot.backend.util.SseService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/resume")
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class ResumeController {

    private static final Logger logger = LogManager.getLogger(ResumeController.class);

    @Autowired
    private ResumeParsingService resumeParsingService; // For synchronous processing

    @Autowired
    private ResumeParsingOrchestrator resumeParsingOrchestrator; // For asynchronous processing

    @Autowired
    private SseService sseService;

    /**
     * Simple synchronous parsing endpoint.
     * Extracts text items and returns their count.
     * Consider changing the return type/logic if full parsing results are needed synchronously.
     *
     * @param file The uploaded PDF file.
     * @return ResponseEntity with success message and item count, or an error.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadResume(@RequestParam("file") MultipartFile file) {
        if (!isValidPdf(file)) {
            logger.warn("Invalid file uploaded to /upload endpoint. Type: {}, Empty: {}", file.getContentType(), file.isEmpty());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid or empty PDF file provided."));
        }

        try {
            // Assuming this service method performs the required synchronous steps
            // and returns the necessary data (here, just items for counting).
            List<ResumeTextItemEntity> extractedItems = resumeParsingService.extractTextItemsWithPositions(file); // Or parseResumeSynchronously(file) if it does more

            logger.info("Synchronous processing complete for file: {}, Items extracted: {}", file.getOriginalFilename(), extractedItems.size());

            return ResponseEntity.ok(Map.of(
                    "message", "File processed successfully (synchronously)",
                    "itemCount", extractedItems.size()
                    // Add other relevant data if parseResumeSynchronously returns more
            ));

        } catch (Exception e) { // Consider catching more specific exceptions from the service
            logger.error("Error during synchronous processing of resume: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process resume due to an internal error."));
        }
    }

    /**
     * Initiates asynchronous parsing of the resume.
     * Returns a Job ID and the URL to poll for status updates via SSE.
     *
     * @param file The uploaded PDF file.
     * @return ResponseEntity with Job ID and Status URL, or an error.
     */
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> parseResumeAsync(@RequestParam("file") MultipartFile file) {
        if (!isValidPdf(file)) {
            logger.warn("Invalid file uploaded to /parse endpoint. Type: {}, Empty: {}", file.getContentType(), file.isEmpty());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid or empty PDF file provided."));
        }

        String jobId = UUID.randomUUID().toString();
        logger.info("Initiating asynchronous parsing job with ID: {} for file: {}", jobId, file.getOriginalFilename());

        try {
            // Delegate the actual parsing to the async orchestrator
            resumeParsingOrchestrator.parseResumeAsync(jobId, file);

            // Return the Job ID and the status URL immediately
            return ResponseEntity.accepted() // 202 Accepted is suitable for async initiation
                    .body(Map.of(
                            "jobId", jobId,
                            "message", "Parsing job initiated successfully.",
                            "statusUrl", "/api/resumes/status/" + jobId // Relative or absolute URL
                    ));
        } catch (Exception e) {
            // Catch potential immediate errors during job kickoff
            logger.error("Failed to initiate async parsing for Job ID: {} and file: {}", jobId, file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to start parsing job: " + e.getMessage()));
        }
    }

    /**
     * Endpoint for clients to connect via Server-Sent Events (SSE)
     * to receive status updates for an ongoing asynchronous parsing job.
     *
     * @param jobId The ID of the parsing job.
     * @return An SseEmitter instance for the client connection.
     */
    @GetMapping("/status/{jobId}")
    public SseEmitter getParsingStatus(@PathVariable String jobId) {
        logger.info("Client requesting SSE connection for Job ID: {}", jobId);
        try {
            // Delegate emitter creation/retrieval to the SseService
            // Consider adding a check here: if (!orchestrator.isJobActiveOrKnown(jobId)) throw new JobNotFoundException();
            return sseService.createEmitter(jobId);
        } catch (Exception e) { // Catch potential exceptions during emitter creation/retrieval
            logger.error("Error creating or retrieving SseEmitter for Job ID {}: {}", jobId, e.getMessage(), e);
            // Let Spring's default exception handling turn this into a 500,
            // or throw ResponseStatusException for more control:
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error setting up status stream for job " + jobId, e);
        }
    }

    /**
     * Helper method to validate the uploaded file.
     * Checks if the file is not null, not empty, and has the PDF MIME type.
     *
     * @param file The MultipartFile to validate.
     * @return true if the file is a valid PDF, false otherwise.
     */
    private boolean isValidPdf(MultipartFile file) {
        // Basic checks
        if (file == null || file.isEmpty() || file.getContentType() == null) {
            return false;
        }

        // MIME type check (allow common PDF MIME variants)
        boolean hasPdfMimeType = "application/pdf".equals(file.getContentType()) ||
                "application/x-pdf".equals(file.getContentType()) ||
                "application/acrobat".equals(file.getContentType());
        if (!hasPdfMimeType) {
            return false;
        }

        // File extension check
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            return false;
        }

        // Magic bytes check (PDF starts with "%PDF-")
        try (InputStream is = file.getInputStream()) {
            byte[] pdfHeader = new byte[5];
            int bytesRead = is.read(pdfHeader, 0, 5);
            if (bytesRead != 5 || !new String(pdfHeader).equals("%PDF-")) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        // File size check
        long maxSizeBytes = 10 * 1024 * 1024; // 10MB example limit
        if (file.getSize() > maxSizeBytes) {
            return false;
        }

        // Optional: Basic PDF structure verification
        try (InputStream is = file.getInputStream()) {
            // You could use a library like Apache PDFBox, iText, or PdfRenderer to verify PDF structure
            // Example with PDFBox: PDDocument doc = PDDocument.load(is);

            // For now, we'll just do a simple check for EOF marker
            byte[] buffer = new byte[1024];
            byte[] eofMarker = "%%EOF".getBytes();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] fileContent = baos.toByteArray();

            // Check for EOF marker somewhere in the last 1024 bytes
            if (fileContent.length >= 1024) {
                byte[] lastBytes = Arrays.copyOfRange(fileContent, fileContent.length - 1024, fileContent.length);
                String lastBytesStr = new String(lastBytes);
                if (!lastBytesStr.contains("%%EOF")) {
                    return false;
                }
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
