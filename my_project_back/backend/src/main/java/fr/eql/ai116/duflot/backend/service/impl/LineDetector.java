package fr.eql.ai116.duflot.backend.service.impl;

import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.state.PDGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;
import org.apache.pdfbox.Loader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class LineDetector extends PDFStreamEngine {

    public static class LineInfo {
        Vector start;
        Vector end;
        String type; // "H" or "V"

        LineInfo(Vector start, Vector end, String type) {
            this.start = start;
            this.end = end;
            this.type = type;
        }

        @Override
        public String toString() {
            return String.format("Type: %s, Start: (%.2f, %.2f), End: (%.2f, %.2f)",
                    type, start.getX(), start.getY(), end.getX(), end.getY());
        }
    }

    private final List<LineInfo> lines = new ArrayList<>();
    private Vector currentPoint = null; // Track current point in the path
    private final List<Vector> currentPathPoints = new ArrayList<>(); // Track points in the current subpath

    // Define a tolerance for floating point comparisons
    private static final double TOLERANCE = 0.1;

    @Override
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
        // Optional: You might log operators here for debugging
        // System.out.println("Operator: " + operator.getName());
        super.processOperator(operator, operands);
    }

    // --- Path Construction Operators ---

    public void moveTo(float x, float y) throws IOException {
        currentPathPoints.clear(); // Start new subpath
        Vector pos = transform(x, y);
        currentPathPoints.add(pos);
        currentPoint = pos;
        System.out.println("moveTo: " + pos);
    }

    public void lineTo(float x, float y) throws IOException {
        if (currentPoint == null) {
            // Should not happen if PDF is valid and moveTo was called first
            System.err.println("WARN: lineTo called without current point.");
            moveTo(x, y); // Try to recover? Or just log error.
            return;
        }
        Vector newPoint = transform(x, y);
        currentPathPoints.add(newPoint);
        currentPoint = newPoint;
        System.out.println("lineTo: " + newPoint);
    }

    public void appendRectangle(float x, float y, float w, float h) throws IOException {
        Vector p0 = transform(x, y);
        Vector p1 = transform(x + w, y);
        Vector p2 = transform(x + w, y + h);
        Vector p3 = transform(x, y + h);

        // Add the segments explicitly for analysis later if needed,
        // or analyze directly here. Let's analyze directly:
        checkAndAddLine(p0, p1); // Bottom
        checkAndAddLine(p1, p2); // Right
        checkAndAddLine(p2, p3); // Top
        checkAndAddLine(p3, p0); // Left

        // Add points to current path so closePath works if called
        currentPathPoints.clear();
        currentPathPoints.add(p0);
        currentPathPoints.add(p1);
        currentPathPoints.add(p2);
        currentPathPoints.add(p3);
        currentPoint = p0; // Rect ends where it started for path closing
    }

    public void closePath() throws IOException {
        // Connects the last point back to the start point of the current subpath
        if (!currentPathPoints.isEmpty()) {
            Vector firstPoint = currentPathPoints.get(0);
            Vector lastPoint = currentPathPoints.get(currentPathPoints.size() - 1);

            // Check if path is already closed or consists of a single point
            if (currentPathPoints.size() > 1 && !pointsNearlyEqual(firstPoint, lastPoint)) {
                // Add the closing segment (conceptually, strokePath will draw it)
                currentPathPoints.add(firstPoint);
                currentPoint = firstPoint; // Update current point
            }
        }
        // System.out.println("closePath");
    }

    public void endPath() throws IOException {
        currentPathPoints.clear();
        currentPoint = null;
        System.out.println("endPath");
    }

    // --- Path Painting Operators ---

    public void strokePath() throws IOException {
        // System.out.println("Stroking path with " + currentPathPoints.size() + " points");
        if (currentPathPoints.size() >= 2) {
            for (int i = 0; i < currentPathPoints.size() - 1; i++) {
                Vector start = currentPathPoints.get(i);
                Vector end = currentPathPoints.get(i + 1);
                checkAndAddLine(start, end);
            }
        }
        currentPathPoints.clear(); // Path is consumed after stroking
        currentPoint = null; // Current point is undefined after stroke? (Check PDF spec)
    }

    // Optional: You might need to handle fillPath, eoFillPath, fillAndStrokePath etc.
    // if lines could be drawn implicitly by filled shapes. For simple lines, strokePath is key.
    // @Override public void fillPath(int windingRule) throws IOException { ... }
    // @Override public void fillAndStrokePath(int windingRule) throws IOException { strokePath(); fillPath(windingRule); }


    // --- Helper Methods ---

    private Vector transform(float x, float y) {
        PDGraphicsState state = getGraphicsState();
        Matrix ctm = state.getCurrentTransformationMatrix();
        Vector sourceVector = new Vector(x, y);
        return ctm.transform(sourceVector);
    }

    /**
     * Checks if two points represented by Vectors are nearly equal within TOLERANCE.
     */
    private boolean pointsNearlyEqual(Vector p1, Vector p2) {
        return Math.abs(p1.getX() - p2.getX()) < TOLERANCE &&
                Math.abs(p1.getY() - p2.getY()) < TOLERANCE;
    }

    /**
     * Checks if the line segment between start and end is horizontal or vertical
     * and adds it to the lines list if it is. Ignores zero-length segments.
     * @param start Start Vector of the segment (in page space).
     * @param end End Vector of the segment (in page space).
     */
    private void checkAndAddLine(Vector start, Vector end) {
        // Ignore zero-length segments (often artifacts or redundant points)
        if (pointsNearlyEqual(start, end)) {
            return;
        }

        // Check for Horizontal line (Y coordinates are nearly the same)
        if (Math.abs(start.getY() - end.getY()) < TOLERANCE) {
            lines.add(new LineInfo(start, end, "H"));
            // System.out.println("Detected H Line: " + start + " -> " + end);

            // Check for Vertical line (X coordinates are nearly the same)
        } else if (Math.abs(start.getX() - end.getX()) < TOLERANCE) {
            lines.add(new LineInfo(start, end, "V"));
            // System.out.println("Detected V Line: " + start + " -> " + end);
        }
        // else: It's a diagonal line, ignore based on requirements
    }

    /**
     * Returns the list of detected horizontal and vertical lines.
     * @return List of LineInfo objects.
     */
    public List<LineInfo> getLines() {
        return lines;
    }


    // --- Main Execution Logic (Example) ---
    public static void main(String[] args) throws IOException {
        // Ensure you provide the correct path to your PDF
        File pdfFile = new File("path/to/your/document.pdf");
        if (!pdfFile.exists()) {
            System.err.println("PDF file not found: " + pdfFile.getAbsolutePath());
            return;
        }


        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            LineDetector lineDetector = new LineDetector();
            int pageNum = 0;
            for (PDPage page : document.getPages()) {
                pageNum++;
                System.out.println("Processing Page: " + pageNum);
                lineDetector.processPage(page);
            }

            List<LineInfo> detectedLines = lineDetector.getLines();
            System.out.println("\n--- Detected Lines ---");
            System.out.println("Total lines found: " + detectedLines.size());
            int hCount = 0;
            int vCount = 0;
            for (LineInfo line : detectedLines) {
                System.out.println(line);
                if ("H".equals(line.type)) hCount++;
                if ("V".equals(line.type)) vCount++;
            }
            System.out.println("\nHorizontal lines: " + hCount);
            System.out.println("Vertical lines: " + vCount);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
