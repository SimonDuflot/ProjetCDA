package fr.eql.ai116.duflot.backend.entity;

import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PositionalTextStripper extends PDFTextStripper {

    private final List<ResumeTextItemEntity> textItems = new ArrayList<>();

    public PositionalTextStripper() throws IOException {
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

        // Call super method if you need the default text stripping behavior as well
        // super.writeString(text, textPositions);
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
