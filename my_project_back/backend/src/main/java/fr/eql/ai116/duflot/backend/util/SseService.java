package fr.eql.ai116.duflot.backend.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {

    private static final Logger logger = LogManager.getLogger();
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private static final long SSE_EMITTER_TIMEOUT = 300_000L; // (5min)

    public SseEmitter createEmitter(String jobId) {
        SseEmitter emitter = new SseEmitter(SSE_EMITTER_TIMEOUT);
        this.emitters.put(jobId, emitter);
        logger.info("SSE Emitter created and registered for Job ID: {}", jobId);

        // Define actions on completion, timeout, or error
        emitter.onCompletion(() -> {
            logger.info("SSE Emitter completed for Job ID: {}", jobId);
            this.emitters.remove(jobId);
        });
        emitter.onTimeout(() -> {
            logger.warn("SSE Emitter timed out for Job ID: {}", jobId);
            emitter.complete();
            // Ensure completion on timeout
            // Removal happens in onCompletion callback
        });
        emitter.onError(e -> {
            logger.error("SSE Emitter error for Job ID: {}: {}", jobId, e.getMessage());
            this.emitters.remove(jobId);
        });
        // Optional: Send an initial "connected" event
        sendUpdate(jobId, "statusUpdate", Map.of("message", "SSE Connection Established", "type", "info"));
        return emitter;
    }

    /**
     * Sends an update to the client associated with the given job ID.
     * @param jobId The job ID.
     * @param eventName The name of the SSE event (e.g., "statusUpdate", "parsingComplete").
     * @param data The data object to send (will be serialized to JSON).
     */
    public void sendUpdate(String jobId, String eventName, Object data) {
        SseEmitter emitter = this.emitters.get(jobId);
        if (emitter != null) {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                        .name(eventName)
                        .data(data) // Spring handles JSON serialization
                        .id(String.valueOf(System.currentTimeMillis())); // Optional event ID

                emitter.send(event);
                logger.debug("Sent SSE event '{}' for Job ID: {}", eventName, jobId);
            } catch (IOException | IllegalStateException e) {
                logger.error("Failed to send SSE event for Job ID: {}. Removing emitter. Error: {}", jobId, e.getMessage());
                // Assume client disconnected or error, complete and remove
                emitter.complete(); // Trigger completion callbacks
                this.emitters.remove(jobId); // Explicit removal
            }
        } else {
            logger.warn("No active SSE Emitter found for Job ID: {}. Update skipped.", jobId);
        }
    }

    /**
     * Completes the SSE connection for a specific job ID, optionally sending a final event.
     * @param jobId The job ID.
     * @param finalEventName The name of the final event (e.g., "parsingComplete").
     * @param finalData The data for the final event.
     */
    public void completeEmitter(String jobId, String finalEventName, Object finalData) {
        sendUpdate(jobId, finalEventName, finalData); // Send final update first
        SseEmitter emitter = this.emitters.get(jobId);
        if (emitter != null) {
            logger.info("Completing SSE Emitter for Job ID: {}", jobId);
            emitter.complete(); // This will trigger the onCompletion callback for removal
        }
    }

    /**
     * Completes the SSE connection due to an error, optionally sending a final error event.
     * @param jobId The job ID.
     * @param errorEventName The name of the error event (e.g., "parsingError").
     * @param errorData The data for the error event.
     */
    public void errorEmitter(String jobId, String errorEventName, Object errorData) {
        sendUpdate(jobId, errorEventName, errorData); // Send error update first
        SseEmitter emitter = this.emitters.get(jobId);
        if (emitter != null) {
            logger.warn("Completing SSE Emitter with error for Job ID: {}", jobId);
            emitter.complete(); // Or completeExceptionally if preferred, but complete works
        }
    }

    // Optional: Method to check if an emitter exists
    public boolean hasEmitter(String jobId) {
        return this.emitters.containsKey(jobId);
    }
}
