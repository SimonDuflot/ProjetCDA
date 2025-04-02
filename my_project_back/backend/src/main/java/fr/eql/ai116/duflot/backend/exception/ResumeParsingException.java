package fr.eql.ai116.duflot.backend.exception;

public class ResumeParsingException extends Exception {
    public ResumeParsingException(String message) {
        super(message);
    }

    public ResumeParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
