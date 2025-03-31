package fr.eql.ai116.duflot.backend.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ResumeLineEntity {

    private List<ResumeTextItemEntity> items = new ArrayList<>();
    private float y; // Store the approximate Y-coordinate of the line

    public ResumeLineEntity() {
    }

    public ResumeLineEntity(ResumeTextItemEntity firstItem) {
        this.items.add(firstItem);
        this.y = firstItem.getY(); // Initialize with the first item's Y
    }

    public void addItem(ResumeTextItemEntity item) {
        this.items.add(item);
    }

    public List<ResumeTextItemEntity> getItems() {
        return items;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public ResumeTextItemEntity getLastItem() {
        if (items.isEmpty()) {
            return null;
        }
        return items.get(items.size() - 1);
    }

    // Helper to get combined text of the line
    public String getLineText() {
        // Add defensive check? Or trust the process.
        if (items == null) return ""; // Handle null list case
        return items.stream()
                .map(ResumeTextItemEntity::getText)
                .filter(Objects::nonNull) // Avoid NPE if getText() can return null
                .collect(Collectors.joining(" "));
    }

    @Override
    public String toString() {
        return "Line{y=" + y + ", text='" + getLineText() + "'}";
    }
}
