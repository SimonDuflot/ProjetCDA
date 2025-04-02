package fr.eql.ai116.duflot.backend.service;

import fr.eql.ai116.duflot.backend.entity.dto.ParsingTraceDTO;
import fr.eql.ai116.duflot.backend.entity.dto.ParsingTraceDTO.LogEntry;
import fr.eql.ai116.duflot.backend.entity.ProfileEntity;
import fr.eql.ai116.duflot.backend.entity.ResumeLineEntity;
import fr.eql.ai116.duflot.backend.entity.ResumeSectionEntity;
import fr.eql.ai116.duflot.backend.entity.ResumeTextItemEntity;
import fr.eql.ai116.duflot.backend.entity.SectionType;
import fr.eql.ai116.duflot.backend.entity.Status;
import fr.eql.ai116.duflot.backend.entity.dto.ResumeDTO;
import fr.eql.ai116.duflot.backend.service.impl.ResumeParsingServiceImpl;
import fr.eql.ai116.duflot.backend.util.SseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service responsible for asynchronously processing resume parsing jobs.
 * Handles the orchestration, SSE updates, and error management.
 */
@Service
public class ResumeParsingOrchestrator {

    private static final Logger logger = LogManager.getLogger(ResumeParsingOrchestrator.class);

    @Autowired
    private SseService sseService;

    @Autowired
    private ResumeParsingService resumeParsingService;

    /**
     * Asynchronously parses the resume file and sends updates via SseService.
     * @param jobId The unique ID for this parsing job.
     * @param file The uploaded multipart file.
     */
    @Async // Marks this method to run in a separate thread
    public void parseResumeAsync(String jobId, MultipartFile file) {
        logger.info("Starting async parsing for Job ID: {}", jobId);
        Path tempFile = null;
        ParsingTraceDTO traceData = new ParsingTraceDTO(jobId, file.getOriginalFilename());
        ResumeDTO parsedResume = null;
        long startTime = System.currentTimeMillis();

        try {
            // --- Preparation ---
            sendStatusUpdate(jobId, traceData, LogEntry.Step.PREPARATION, Status.INFO, "Preparing file...");

            // Save multipart file to a temporary location
            tempFile = Files.createTempFile("resume_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile.toFile());
            logger.info("File saved temporarily to: {}", tempFile.toString());

            sendStatusUpdate(jobId, traceData, LogEntry.Step.PREPARATION, Status.SUCCESS,
                    "File prepared.", Map.of("tempPath", tempFile.toString()));

            // --- Perform the actual parsing using the service ---
            File pdfFile = tempFile.toFile();

            // Call the high-level parse method
            parsedResume = resumeParsingService.parseResume(pdfFile, traceData);

            // Add metadata to the result
            if (parsedResume != null) {
                parsedResume.setFileName(file.getOriginalFilename());
                parsedResume.setParseTime(System.currentTimeMillis() - startTime);
                // Could also set page count here if available from PDDocument
            }

            // --- Completion ---
            logger.info("Async parsing processing finished successfully for Job ID: {}", jobId);
            traceData.setOverallStatus(Status.SUCCESS);

            Map<String, Object> finalPayload = Map.of(
                    "summary", "Parsing finished successfully.",
                    "data", parsedResume,
                    "traceSummary", traceData.getSummary(),
                    "parseTimeMs", System.currentTimeMillis() - startTime
            );

            sseService.completeEmitter(jobId, "parsingComplete", finalPayload);

        } catch (Exception e) {
            // Handle exceptions from any step
            logger.error("Error during async parsing for Job ID: {}", jobId, e);
            traceData.setOverallStatus(Status.FAILURE);

            Map<String, Object> errorPayload = Map.of(
                    "errorMessage", "A critical error occurred during parsing.",
                    "details", e.getMessage(),
                    "traceSummary", traceData.getSummary()
            );

            // Ensure emitter is closed if an exception occurs
            if (sseService.hasEmitter(jobId)) {
                sseService.errorEmitter(jobId, "parsingError", errorPayload);
            }

        } finally {
            // --- Cleanup ---
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                    logger.info("Deleted temporary file: {}", tempFile.toString());
                } catch (IOException e) {
                    logger.error("Failed to delete temporary file: {}", tempFile.toString(), e);
                }
            }

            traceData.setEndTime(System.currentTimeMillis());
            logger.info("Finished processing Job ID: {}. Overall Status: {}", jobId, traceData.getOverallStatus());

            // Optional: Persist the traceData here if needed
            // tracePersistenceService.save(traceData);
        }
    }

    /**
     * Helper method to add a log entry to the trace data and send an SSE update.
     */
    private void sendStatusUpdate(String jobId, ParsingTraceDTO traceData, LogEntry.Step step, Status status, String message) {
        sendStatusUpdate(jobId, traceData, step, status, message, null);
    }

    private void sendStatusUpdate(String jobId, ParsingTraceDTO traceData, LogEntry.Step step, Status status, String message, Map<String, Object> details) {
        LogEntry entry = traceData.addLogEntry(step, status, message, details);
        // Send the LogEntry object itself as the SSE data payload
        sseService.sendUpdate(jobId, "statusUpdate", entry);

        // Send specific event type for heuristic results if applicable
        if (details != null && step == LogEntry.Step.SECTION_GROUPING && details.containsKey("heuristicScore")) {
            sseService.sendUpdate(jobId, "heuristicResult", details);
        }
    }
}
