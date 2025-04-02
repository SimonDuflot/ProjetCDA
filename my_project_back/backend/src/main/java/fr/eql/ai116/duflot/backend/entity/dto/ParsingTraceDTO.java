package fr.eql.ai116.duflot.backend.entity.dto;

import fr.eql.ai116.duflot.backend.entity.Status;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Data Transfer Object responsible for the logging and tracing of the parsing mechanisms.
 * Contains methods responsible for storing logs and traces.
 */
public class ParsingTraceDTO {

    private final String jobId;
    private final String originalFilename;
    private final long startTime;
    private long endTime;
    private Status overallStatus;
    private final List<LogEntry> logEntries;

    // Optional: Store intermediate results directly (can make object large)
    // private List<ResumeTextItemEntity> rawTextItems;
    // private List<ResumeLineEntity> groupedLines;
    // private List<ResumeSectionEntity> identifiedSections;
    // private ResumeEntity finalResult; // Or whatever your final parsed structure is

    public ParsingTraceDTO(String jobId, String originalFilename) {
        this.jobId = jobId;
        this.originalFilename = originalFilename;
        this.startTime = System.currentTimeMillis();
        this.overallStatus = Status.RUNNING;
        this.logEntries = new ArrayList<>();
    }

    /**
     * Represents a single step or event during the parsing process.
     */
    public static class LogEntry {
        private final long timestamp;
        private final Step step; // Enum defining the parsing stage
        private final Status status; // Outcome of this step/log
        private final String message; // Human-readable description
        // Using LinkedHashMap to preserve insertion order if needed
        private Map<String, Object> details; // Structured data (counts, scores, errors, etc.)

        public enum Step {
            PREPARATION,
            VECTOR_EXTRACTION,
            TEXT_EXTRACTION,
            LINE_GROUPING,
            SECTION_GROUPING, // Covers title detection heuristics
            ATTRIBUTE_EXTRACTION,
            COMPLETION,
            UNKNOWN
        }

        // Private constructor, use factory method in outer class
        private LogEntry(Step step, Status status, String message, Map<String, Object> details) {
            this.timestamp = System.currentTimeMillis();
            this.step = step;
            this.status = status;
            this.message = message;
            this.details = (details != null) ? new LinkedHashMap<>(details) : new LinkedHashMap<>();
        }

        // Getters for Jackson serialization to JSON
        public long getTimestamp() { return timestamp; }
        public Step getStep() { return step; }
        public Status getStatus() { return status; }
        public String getMessage() { return message; }
        public Map<String, Object> getDetails() { return details; }
    }

    /**
     * Adds a log entry to the trace.
     * @return The created LogEntry.
     */
    public LogEntry addLogEntry(LogEntry.Step step, Status status, String message, Map<String, Object> details) {
        LogEntry entry = new LogEntry(step, status, message, details);
        this.logEntries.add(entry);
        // Update overall status if a failure occurs
        if (status == Status.FAILURE) {
            this.overallStatus = Status.FAILURE;
        }
        return entry;
    }

    public LogEntry addLogEntry(LogEntry.Step step, Status status, String message) {
        return addLogEntry(step, status, message, null);
    }

    /**
     * Gets the most recently added log entry.
     * @return The last log entry or null if no entries exist.
     */
    public LogEntry getLastEntry() {
        if (logEntries.isEmpty()) {
            return null;
        }
        return logEntries.get(logEntries.size() - 1);
    }

    // --- Getters for main trace object ---
    public String getJobId() { return jobId; }
    public String getOriginalFilename() { return originalFilename; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public Status getOverallStatus() { return overallStatus; }
    public List<LogEntry> getLogEntries() { return logEntries; }

    public void setEndTime(long endTime) { this.endTime = endTime; }
    public void setOverallStatus(Status overallStatus) { this.overallStatus = overallStatus; }


    /**
     * Provides a summary of the trace steps and their status.
     */
    public Map<String, String> getSummary() {
        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("Job ID", jobId);
        summary.put("Filename", originalFilename);
        summary.put("Overall Status", overallStatus.name());
        summary.put("Duration (ms)", endTime > 0 ? String.valueOf(endTime - startTime) : "N/A");
        summary.put("Steps", logEntries.stream()
                .map(entry -> entry.getStep() + ": " + entry.getStatus())
                .collect(Collectors.joining(", ")));
        return summary;
    }
}
