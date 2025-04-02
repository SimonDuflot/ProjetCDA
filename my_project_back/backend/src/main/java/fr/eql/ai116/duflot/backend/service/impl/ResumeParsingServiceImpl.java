package fr.eql.ai116.duflot.backend.service.impl;

import fr.eql.ai116.duflot.backend.entity.dto.ParsingTraceDTO;
import fr.eql.ai116.duflot.backend.entity.dto.ParsingTraceDTO.LogEntry;
import fr.eql.ai116.duflot.backend.entity.ProfileEntity;
import fr.eql.ai116.duflot.backend.entity.ResumeLineEntity;
import fr.eql.ai116.duflot.backend.entity.ResumeSectionEntity;
import fr.eql.ai116.duflot.backend.entity.ResumeTextItemEntity;
import fr.eql.ai116.duflot.backend.entity.SectionType;
import fr.eql.ai116.duflot.backend.entity.Status;
import fr.eql.ai116.duflot.backend.entity.dto.ResumeDTO;
import fr.eql.ai116.duflot.backend.service.ResumeParsingHelper;
import fr.eql.ai116.duflot.backend.service.ResumeParsingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.pdfbox.Loader.loadPDF;

@Service
public class ResumeParsingServiceImpl implements ResumeParsingService {

    private static final Logger logger = LogManager.getLogger(ResumeParsingServiceImpl.class);

    @Autowired
    ResumeParsingHelper parsingHelper;

    /**
     * High-level method that executes the entire parsing pipeline.
     */
    @Override
    public ResumeDTO parseResume(File pdfFile, ParsingTraceDTO traceData) throws IOException {
        ResumeDTO resumeDTO = new ResumeDTO();

        // Step 1: Extract text items with positions
        traceData.addLogEntry(LogEntry.Step.TEXT_EXTRACTION, Status.INFO, "Starting text extraction...");
        List<ResumeTextItemEntity> textItems = extractTextItemsWithPositions(pdfFile, traceData);
        if (textItems.isEmpty()) {
            traceData.addLogEntry(LogEntry.Step.TEXT_EXTRACTION, Status.FAILURE, "Text extraction failed to produce items.");
            throw new RuntimeException("Text extraction failed to produce items.");
        }

        // Step 2: Group items into lines
        traceData.addLogEntry(LogEntry.Step.LINE_GROUPING, Status.INFO, "Starting line grouping...");
        List<ResumeLineEntity> lines = groupItemsIntoLines(textItems, traceData);
        if (lines.isEmpty()) {
            traceData.addLogEntry(LogEntry.Step.LINE_GROUPING, Status.FAILURE, "Line grouping failed to produce lines.");
            throw new RuntimeException("Line grouping failed to produce lines.");
        }

        // Step 3: Group lines into sections
        traceData.addLogEntry(LogEntry.Step.SECTION_GROUPING, Status.INFO, "Starting section grouping...");
        List<ResumeSectionEntity> sections = groupLinesIntoSections(lines, traceData);
        if (sections.isEmpty()) {
            traceData.addLogEntry(LogEntry.Step.SECTION_GROUPING, Status.FAILURE, "Section grouping failed to produce sections.");
            throw new RuntimeException("Section grouping failed to produce sections.");
        }

        // Log section types found
        List<String> sectionTypes = sections.stream().map(s -> s.getType().toString()).toList();
        traceData.addLogEntry(LogEntry.Step.SECTION_GROUPING, Status.INFO, "Identified Section Types", Map.of("types", sectionTypes));

        // Step 4: Extract data from each section type
        traceData.addLogEntry(LogEntry.Step.ATTRIBUTE_EXTRACTION, Status.INFO, "Starting attribute extraction...");

        // Process profile section if present
        Optional<ResumeSectionEntity> profileSectionOpt = sections.stream()
                .filter(s -> s.getType() == SectionType.PROFILE)
                .findFirst();

        if (profileSectionOpt.isPresent()) {
            ProfileEntity profile = extractProfileData(profileSectionOpt.get(), traceData);
            resumeDTO.setProfile(profile);
        } else {
            traceData.addLogEntry(LogEntry.Step.ATTRIBUTE_EXTRACTION, Status.INFO, "PROFILE section not found.");
            resumeDTO.setProfile(new ProfileEntity()); // Empty profile
        }

        // Process other section types (experience, education, etc.)
        // TODO: Implement extraction for other section types
        // For example:
        // sections.stream().filter(s -> s.getType() == SectionType.EXPERIENCE).forEach(section -> {
        //     ExperienceEntity experience = extractExperienceData(section, traceData);
        //     resumeDTO.addExperience(experience);
        // });

        traceData.addLogEntry(LogEntry.Step.ATTRIBUTE_EXTRACTION, Status.SUCCESS, "Attribute extraction complete.");
        return resumeDTO;
    }


    /**
     * Extracts text items with positional information from a PDF file.
     * Step 1 of the parsing pipeline.
     *
     * @param pdfFile The PDF file to process.
     * @param traceData The trace object to log details to.
     * @return A list of ResumeTextItemEntity objects.
     * @throws IOException If there's an error reading the PDF.
     */
    @Override
    public List<ResumeTextItemEntity> extractTextItemsWithPositions(File pdfFile, ParsingTraceDTO traceData) throws IOException {
        traceData.addLogEntry(LogEntry.Step.TEXT_EXTRACTION, Status.INFO, "Loading PDF document...");
        PDDocument document = null;
        try {
            document = loadPDF(pdfFile);
            traceData.addLogEntry(LogEntry.Step.TEXT_EXTRACTION, Status.INFO, "PDF loaded. Using PositionalTextStripper...");

            PositionalTextStripperImpl textStripper = new PositionalTextStripperImpl();
            textStripper.getText(document); // Trigger processing

            List<ResumeTextItemEntity> extractedItems = textStripper.getTextItems();
            traceData.addLogEntry(ParsingTraceDTO.LogEntry.Step.TEXT_EXTRACTION, Status.INFO, "PositionalTextStripper finished.", Map.of("rawItemCount", extractedItems.size()));

            if (extractedItems.isEmpty()) {
                logger.warn("No text items extracted by PositionalTextStripper for file: {}", pdfFile.getName());
                traceData.addLogEntry(LogEntry.Step.TEXT_EXTRACTION, Status.FAILURE, "PositionalTextStripper returned no items.");
            }
            return extractedItems;

        } catch (IOException e) {
            logger.error("IOException during text extraction for file {}: {}", pdfFile.getName(), e.getMessage());
            traceData.addLogEntry(LogEntry.Step.TEXT_EXTRACTION, Status.FAILURE, "IOException during PDF loading or stripping.", Map.of("error", e.getMessage()));
            throw e; // Re-throw
        } finally {
            if (document != null) {
                try {
                    document.close();
                    traceData.addLogEntry(LogEntry.Step.TEXT_EXTRACTION, Status.INFO, "PDF document closed.");
                } catch (IOException e) {
                    logger.error("Failed to close PDF document: {}", e.getMessage());
                    traceData.addLogEntry(LogEntry.Step.TEXT_EXTRACTION, Status.FAILURE, "Failed to close PDF document.", Map.of("error", e.getMessage()));
                }
            }
        }
    }

    /**
     * Groups extracted text items into lines. Step 2.
     *
     * @param textItems List of text items from Step 1.
     * @param traceData The trace object to log details to.
     * @return A list of ResumeLineEntity objects.
     */
    @Override
    public List<ResumeLineEntity> groupItemsIntoLines(List<ResumeTextItemEntity> textItems, ParsingTraceDTO traceData) {
        traceData.addLogEntry(LogEntry.Step.LINE_GROUPING, Status.INFO, "Starting line grouping...");
        List<ResumeLineEntity> lines = parsingHelper.groupItemsIntoLines(textItems); // Delegate
        traceData.addLogEntry(LogEntry.Step.LINE_GROUPING, Status.INFO, "Line grouping finished.", Map.of("lineCount", lines.size()));
        return lines;
    }

    /**
     * Groups lines into logical sections. Step 3.
     * Requires ResumeParsingHelper.groupLinesIntoSections to accept and use traceData.
     *
     * @param lines List of lines from Step 2.
     * @param traceData The trace object to log details (including heuristic scores).
     * @return A list of ResumeSectionEntity objects.
     */
    @Override
    public List<ResumeSectionEntity> groupLinesIntoSections(List<ResumeLineEntity> lines, ParsingTraceDTO traceData) {
        traceData.addLogEntry(LogEntry.Step.SECTION_GROUPING, Status.INFO, "Starting section grouping...");
        List<ResumeSectionEntity> sections = parsingHelper.groupLinesIntoSections(lines, traceData); // Delegate
        traceData.addLogEntry(LogEntry.Step.SECTION_GROUPING, Status.INFO, "Section grouping finished.", Map.of("sectionCount", sections.size()));
        return sections;
    }
    /**
     * Extracts structured profile information. Part of Step 4.
     *
     * @param profileSection The identified PROFILE section entity.
     * @param traceData The trace object to log details to.
     * @return A ProfileEntity object.
     */
    @Override
    public ProfileEntity extractProfileData(ResumeSectionEntity profileSection, ParsingTraceDTO traceData) {
        if (profileSection == null || profileSection.getType() != SectionType.PROFILE) {
            traceData.addLogEntry(LogEntry.Step.ATTRIBUTE_EXTRACTION, Status.FAILURE, "Attempted to extract profile data from null or incorrect section type.", Map.of("sectionType", profileSection != null ? profileSection.getType() : "null"));
            return new ProfileEntity(); // Return empty
        }
        traceData.addLogEntry(LogEntry.Step.ATTRIBUTE_EXTRACTION, Status.INFO, "Extracting data for PROFILE section...");
        ProfileEntity profile = parsingHelper.extractProfileData(profileSection); // Delegate
        traceData.addLogEntry(LogEntry.Step.ATTRIBUTE_EXTRACTION, Status.INFO, "Profile data extraction finished.", Map.of("extractedName", profile.getFirstName() + " " + profile.getLastName()));
        return profile;
    }

    @Override
    public String extractTextFromPdf(File pdfFile) throws IOException {
        PDDocument document = loadPDF(pdfFile);
        PDFTextStripper textStripper = new PDFTextStripper();
        String text = textStripper.getText(document);
        document.close();
        return text;
    }
}
