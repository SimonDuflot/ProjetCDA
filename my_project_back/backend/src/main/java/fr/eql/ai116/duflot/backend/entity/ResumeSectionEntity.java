package fr.eql.ai116.duflot.backend.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ResumeSectionEntity {

    private final SectionType type; // Make final if type doesn't change after creation
    private final String titleFound; // Make final if title doesn't change
    private final List<ResumeLineEntity> lines = new ArrayList<>();

    public ResumeSectionEntity(SectionType type, String titleFound) {
        this.type = Objects.requireNonNull(type, "Section type cannot be null"); // Add null check
        this.titleFound = titleFound; // Title can be null for PROFILE
    }

    public void addLine(ResumeLineEntity line) {
        if (line != null) { // Add null check for line
            this.lines.add(line);
        }
    }

    // --- Getters ---
    public SectionType getType() {
        return type;
    }

    public String getTitleFound() {
        return titleFound;
    }

    public List<ResumeLineEntity> getLines() {
        // Return an unmodifiable list if you want to prevent external modification
        // return Collections.unmodifiableList(lines);
        return lines; // Or return the mutable list if modification is needed elsewhere (less common)
    }

    @Override
    public String toString() {
        return "ResumeSectionEntity{" +
                "type=" + type +
                ", titleFound='" + (titleFound != null ? titleFound : "N/A") + '\'' +
                ", lineCount=" + lines.size() +
                '}';
    }

    // Optional: Implement equals() and hashCode() if you plan to store these in Sets or use them as Map keys
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResumeSectionEntity that = (ResumeSectionEntity) o;
        return type == that.type && Objects.equals(titleFound, that.titleFound) && Objects.equals(lines, that.lines);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, titleFound, lines);
    }
}
