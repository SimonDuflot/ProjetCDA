package fr.eql.ai116.duflot.backend.entity;

public enum SectionType {
    PROFILE, // For initial contact info, often untitled
    SUMMARY, // Objective or Summary section
    EDUCATION,
    EXPERIENCE,
    PROJECTS,
    SKILLS,
    LANGUAGES,
    HOBBIES, // Or Interests
    CERTIFICATIONS,
    AWARDS, // Could be separate or part of Education/Experience
    PUBLICATIONS, // For later expansion
    UNKNOWN // For blocks that couldn't be classified
}
