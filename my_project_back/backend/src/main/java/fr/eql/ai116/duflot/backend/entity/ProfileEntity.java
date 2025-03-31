package fr.eql.ai116.duflot.backend.entity;

import java.util.ArrayList;
import java.util.List;

public class ProfileEntity {
    private String firstName;
    private String lastName;
    private String location; // Not extracted yet
    private String phone;    // Changed from ContactEntity for simplicity
    private String email;    // Changed from ContactEntity for simplicity
    private String summary;  // Not extracted yet (belongs in SUMMARY section)
    // private boolean hasHeadshot; // Cannot determine from text
    // private List<String> keywords; // Usually part of Summary/Skills

    // New fields based on request
    private String website;
    private String githubProfile;
    private String linkedInProfile;

    // --- Default Constructor ---
    public ProfileEntity() {}

    // --- Getters and Setters for all fields ---
    // (Example for firstName, add for all others)
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getGithubProfile() { return githubProfile; }
    public void setGithubProfile(String githubProfile) { this.githubProfile = githubProfile; }

    public String getLinkedInProfile() { return linkedInProfile; }
    public void setLinkedInProfile(String linkedInProfile) { this.linkedInProfile = linkedInProfile; }

    @Override
    public String toString() {
        return "ProfileEntity{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", website='" + website + '\'' +
                ", githubProfile='" + githubProfile + '\'' +
                ", linkedInProfile='" + linkedInProfile + '\'' +
                // Add other fields as needed
                '}';
    }
}
