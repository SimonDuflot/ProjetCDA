package fr.eql.ai116.duflot.backend.controller;

import fr.eql.ai116.duflot.backend.service.ResumeParsingService;
import fr.eql.ai116.duflot.backend.service.impl.PositionalTextStripperImpl;
import fr.eql.ai116.duflot.backend.entity.ProfileEntity;
import fr.eql.ai116.duflot.backend.entity.ResumeLineEntity;
import fr.eql.ai116.duflot.backend.entity.ResumeSectionEntity;
import fr.eql.ai116.duflot.backend.entity.ResumeTextItemEntity;
import fr.eql.ai116.duflot.backend.entity.SectionType;
import fr.eql.ai116.duflot.backend.service.ResumeParsingHelper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.apache.pdfbox.pdmodel.PDDocument;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resume")
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class ResumeController {

    @Autowired
    ResumeParsingService resumeParsingService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadResume(@RequestParam("file")MultipartFile file) {

        List<ResumeTextItemEntity> extractedItems;

        try {
            InputStream inputStream = file.getInputStream();
            byte[] decodedBytes = IOUtils.toByteArray(inputStream);
            RandomAccessRead randomAccessRead = new RandomAccessReadBuffer(decodedBytes);

            PDDocument document = Loader.loadPDF(randomAccessRead);

            // Extract text content
            PositionalTextStripperImpl textStripper = new PositionalTextStripperImpl();
            textStripper.getText(document);

            extractedItems = textStripper.getTextItems();

            System.out.println("Extracted Items with Positions: " + extractedItems.size());
            // extractedItems.stream().limit(10).forEach(System.out::println);

            // Perform Step 2: group text into lines
            ResumeParsingHelper parsingHelper = new ResumeParsingHelper();
            List<ResumeLineEntity> lines = parsingHelper.groupItemsIntoLines(extractedItems);

            System.out.println("\nGrouped Lines: " + lines.size());
            // lines.stream().limit(20).forEach(System.out::println);

            // Perform Step 3: group lines into sections
            List<ResumeSectionEntity> sections = parsingHelper.groupLinesIntoSections(lines);
            System.out.println("\nGrouped Sections (" + sections.size() + "): ");
            sections.forEach(section -> {
                System.out.println("Section Type: " + section.getType() + ", Title: '" + section.getTitleFound() + "', Lines: " + section.getLines().size());
                // Optionally print the first few lines of each section for detail
                section.getLines().stream().limit(3).forEach(line -> System.out.println("  - " + line.getLineText()));
            });            // --- Perform NEXT STEPS of the algorithm using extractedItems ---

            // --- Perform Step 4: Extract Attributes ---
            ProfileEntity profileData = null;
            // Find the PROFILE section
            for (ResumeSectionEntity section : sections) {
                if (section.getType() == SectionType.PROFILE) {
                    profileData = parsingHelper.extractProfileData(section);
                    break;
                }
            }

            if (profileData != null) {
                System.out.println("\nExtracted Profile Data:");
                System.out.println(profileData);
            } else {
                System.out.println("\nProfile section not found or no data extracted.");
            }

            document.close();
            randomAccessRead.close();
        } catch (IOException e) {
            System.err.println("Error processing PDF: " + e.getMessage());
            return new ResponseEntity<>(Map.of("message", "Error processing PDF"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(Map.of("message", "File received and items extracted successfully", "itemCount", extractedItems.size()), HttpStatus.OK);
    }
}
