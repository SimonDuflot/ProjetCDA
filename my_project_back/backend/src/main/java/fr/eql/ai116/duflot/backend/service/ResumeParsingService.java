package fr.eql.ai116.duflot.backend.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

import static org.apache.pdfbox.Loader.loadPDF;

@Service
public class ResumeParsingService {

    public String extractTextFromPdf(File pdfFile) throws IOException {
        PDDocument document = loadPDF(pdfFile);
        PDFTextStripper textStripper = new PDFTextStripper();
        String text = textStripper.getText(document);
        document.close();
        return text;
    }
}
