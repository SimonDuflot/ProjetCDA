package fr.eql.ai116.duflot.backend.entity.dto;

import fr.eql.ai116.duflot.backend.entity.EducationEntity;
import fr.eql.ai116.duflot.backend.entity.ExperienceEntity;
import fr.eql.ai116.duflot.backend.entity.LanguageEntity;
import fr.eql.ai116.duflot.backend.entity.LinkEntity;
import fr.eql.ai116.duflot.backend.entity.ProfileEntity;
import fr.eql.ai116.duflot.backend.entity.ProjectEntity;
import fr.eql.ai116.duflot.backend.entity.SkillEntity;

import java.util.List;

/**
 * Data Transfer Object representing a fully parsed resume.
 * Contains all structured data extracted from the resume file.
 */
public class ResumeDTO {

    private ProfileEntity profile;
    private List<LinkEntity> links;
    private List<SkillEntity> skills;
    private List<LanguageEntity> languages;
    private List<ProjectEntity> projects;
    private List<ExperienceEntity> experiences;
    private List<EducationEntity> educations;

    private String fileName;
    private int pageCount;
    private long parseTime;

    public ResumeDTO() {
    }

    public void setParseTime(long parseTime) {
        this.parseTime = parseTime;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public void setProfile(ProfileEntity profile) {
        this.profile = profile;
    }

}
