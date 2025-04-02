package fr.eql.ai116.duflot.backend.service;

import fr.eql.ai116.duflot.backend.entity.ProfileEntity;
import fr.eql.ai116.duflot.backend.entity.ResumeLineEntity;
import fr.eql.ai116.duflot.backend.entity.ResumeSectionEntity;
import fr.eql.ai116.duflot.backend.entity.ResumeTextItemEntity;
import fr.eql.ai116.duflot.backend.entity.dto.ParsingTraceDTO;
import fr.eql.ai116.duflot.backend.entity.dto.ResumeDTO;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Service responsible for parsing resumes.
 * Contains methods for each parsing step as well as a high-level method for the entire process.
 */
public interface ResumeParsingService {

    /**
     * High-level method to parse a resume file through all steps of the pipeline.
     *
     * @param pdfFile The PDF file to process
     * @param traceData The trace object for logging details (without SSE updates)
     * @return A ResumeDTO containing all parsed resume data
     * @throws IOException If there's an error reading the PDF
     */
    ResumeDTO parseResume(File pdfFile, ParsingTraceDTO traceData) throws IOException;

    /**
     * Extracts text items with positional information from a PDF file.
     * Step 1 of the parsing pipeline.
     *
     * @param pdfFile The PDF file to process
     * @param traceData The trace object for logging details
     * @return A list of ResumeTextItemEntity objects
     * @throws IOException If there's an error reading the PDF
     */
    List<ResumeTextItemEntity> extractTextItemsWithPositions(File pdfFile, ParsingTraceDTO traceData) throws IOException;

    /**
     * Groups extracted text items into lines. Step 2 of the pipeline.
     *
     * @param textItems List of text items from Step 1
     * @param traceData The trace object for logging details
     * @return A list of ResumeLineEntity objects
     */
    List<ResumeLineEntity> groupItemsIntoLines(List<ResumeTextItemEntity> textItems, ParsingTraceDTO traceData);

    /**
     * Groups lines into logical sections. Step 3 of the pipeline.
     *
     * @param lines List of lines from Step 2
     * @param traceData The trace object for logging details
     * @return A list of ResumeSectionEntity objects
     */
    List<ResumeSectionEntity> groupLinesIntoSections(List<ResumeLineEntity> lines, ParsingTraceDTO traceData);

    /**
     * Extracts structured profile information. Part of Step 4.
     *
     * @param profileSection The identified PROFILE section entity
     * @param traceData The trace object for logging details
     * @return A ProfileEntity object
     */
    ProfileEntity extractProfileData(ResumeSectionEntity profileSection, ParsingTraceDTO traceData);

    /**
     * Simple method to extract raw text from a PDF.
     *
     * @param pdfFile The PDF file
     * @return The extracted text as String
     * @throws IOException If there's an error reading the PDF
     */
    String extractTextFromPdf(File pdfFile) throws IOException;
}
