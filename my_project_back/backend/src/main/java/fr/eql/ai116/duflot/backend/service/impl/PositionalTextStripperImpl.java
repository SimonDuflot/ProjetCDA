package fr.eql.ai116.duflot.backend.service.impl;

import fr.eql.ai116.duflot.backend.entity.ResumeTextItemEntity;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.interactive.action.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PositionalTextStripperImpl extends PDFTextStripper {

    private final List<ResumeTextItemEntity> textItems = new ArrayList<>();
    private ResumeTextItemEntity lastItemProcessed = null;

    public PositionalTextStripperImpl() throws IOException {
        // Constructor needed due to potential IOException in superclass
        super();
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        if (textPositions.isEmpty()) {
            return;
        }

        // --- Simplified approach: Treat the whole string from writeString as one item ---
        // You might need more granular processing (character by character) for complex layouts
        TextPosition firstPosition = textPositions.get(0);
        TextPosition lastPosition = textPositions.get(textPositions.size() - 1);

        float startX = firstPosition.getXDirAdj();
        float startY = firstPosition.getYDirAdj();
        // Calculate width based on the start of the first and end of the last character
        float totalWidth = (lastPosition.getXDirAdj() + lastPosition.getWidthDirAdj()) - startX;
        // Use height of the first character (assuming uniform height in the chunk)
        float height = firstPosition.getHeightDir();
        String fontName = firstPosition.getFont().getName();
        float fontSize = firstPosition.getFontSizeInPt();

        // Basic boldness check (might need refinement)
        PDFontDescriptor fontDescriptor = firstPosition.getFont().getFontDescriptor();
        boolean isBold = fontDescriptor != null && (fontDescriptor.getFontWeight() >= 700 || fontName.toLowerCase().contains("bold"));

        ResumeTextItemEntity item = new ResumeTextItemEntity(text, startX, startY, totalWidth, height, fontName, fontSize, isBold);
        textItems.add(item);
        this.lastItemProcessed = item;
        // Call super method if you need the default text stripping behavior as well
        // super.writeString(text, textPositions);
    }

    @Override
    protected void writeLineSeparator() throws IOException {
        if (lastItemProcessed != null) {
            this.lastItemProcessed.setHasEOL(true);
            lastItemProcessed = null;
        }
        super.writeLineSeparator();
    }

    @Override
    protected void endPage(PDPage page) throws IOException {
        if (lastItemProcessed != null && !lastItemProcessed.isHasEOL()) {
            this.lastItemProcessed.setHasEOL(true);
            lastItemProcessed = null;
        }
        super.endPage(page);
    }

    @Override
    protected void writeWordSeparator() throws IOException {
        super.writeWordSeparator();
        lastItemProcessed = null;
    }

    /**
     * Uses PDFBox PDAnnotation to extract every links present in the selected page.
     * Output used to instanciate LinkEntity and store links based on content
     * (regexes for linkedin / github and more).
     *
     * @param page
     * @throws IOException
     */
    protected void findAllLinks(PDPage page) throws IOException {
        List<PDAnnotation> links = page.getAnnotations();
        for (PDAnnotation annotation : links) {
            if (annotation instanceof PDAnnotationLink) {
                PDAnnotationLink link = (PDAnnotationLink) annotation;
                PDAction action = link.getAction();
                PDActionURI uri = (PDActionURI) action;
                String url = uri.getURI();
                System.out.println("Found URL: " + url);
            }
        }
    }

    /**
     * Call this method AFTER processing the document (e.g., after calling stripper.getText(document))
     * to get the collected text items with positions.
     *
     * @return List of TextItem objects.
     */
    public List<ResumeTextItemEntity> getTextItems() {
        return textItems;
    }
}
