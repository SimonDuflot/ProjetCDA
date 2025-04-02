package fr.eql.ai116.duflot.backend.service;

import fr.eql.ai116.duflot.backend.entity.dto.ParsingTraceDTO;
import fr.eql.ai116.duflot.backend.entity.ProfileEntity;
import fr.eql.ai116.duflot.backend.entity.ResumeLineEntity;
import fr.eql.ai116.duflot.backend.entity.ResumeSectionEntity;
import fr.eql.ai116.duflot.backend.entity.ResumeTextItemEntity;
import fr.eql.ai116.duflot.backend.entity.SectionType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeParsingHelper {
    // Expanded keyword sets with more variations in both languages
    private static final Map<SectionType, Set<String>> SECTION_KEYWORDS = new HashMap<>();

    static {
        // Profile/Summary section keywords
        SECTION_KEYWORDS.put(SectionType.PROFILE, Set.of(
                "PROFIL", "PROFILE", "CONTACT", "CONTACT INFORMATION", "COORDONNÉES", "COORDONNEES",
                "PERSONAL INFORMATION", "INFORMATIONS PERSONNELLES", "DETAILS PERSONNELS"
        ));

        SECTION_KEYWORDS.put(SectionType.SUMMARY, Set.of(
                "SUMMARY", "SOMMAIRE", "RÉSUMÉ", "RESUME", "OBJECTIF", "OBJECTIVE",
                "PROFESSIONAL SUMMARY", "SOMMAIRE PROFESSIONNEL", "ABOUT ME", "À PROPOS DE MOI",
                "CAREER GOAL", "OBJECTIF DE CARRIÈRE", "OBJECTIF PROFESSIONNEL", "PROFESSIONAL OBJECTIVE"
        ));

        SECTION_KEYWORDS.put(SectionType.EDUCATION, Set.of(
                "EDUCATION", "FORMATION", "DIPLOMES", "DIPLÔMES", "CERTIFICATIONS", "ACADEMIC BACKGROUND",
                "PARCOURS ACADÉMIQUE", "PARCOURS ACADEMIQUE", "SCOLARITÉ", "SCOLARITE", "ÉTUDES", "ETUDES",
                "QUALIFICATIONS", "TRAINING", "FORMATIONS", "ACADEMIC QUALIFICATIONS", "DEGREES"
        ));

        SECTION_KEYWORDS.put(SectionType.EXPERIENCE, Set.of(
                "EXPERIENCE", "EXPÉRIENCE", "EXPÉRIENCES", "EXPERIENCES", "WORK EXPERIENCE", "PROFESSIONAL EXPERIENCE",
                "EXPÉRIENCE PROFESSIONNELLE", "EXPERIENCE PROFESSIONNELLE", "WORK HISTORY", "EMPLOYMENT",
                "CAREER HISTORY", "PARCOURS PROFESSIONNEL", "HISTORIQUE PROFESSIONNEL", "EMPLOIS", "JOBS"
        ));

        SECTION_KEYWORDS.put(SectionType.PROJECTS, Set.of(
                "PROJECTS", "PROJETS", "PERSONAL PROJECTS", "PROJETS PERSONNELS", "PROJECT EXPERIENCE",
                "EXPERIENCE DE PROJET", "RÉALISATIONS", "REALISATIONS", "ACHIEVEMENTS", "ACCOMPLISHMENTS"
        ));

        SECTION_KEYWORDS.put(SectionType.SKILLS, Set.of(
                "SKILLS", "COMPETENCES", "COMPÉTENCES", "TECHNOLOGIES", "TECHNICITÉS", "TECHNICAL SKILLS",
                "COMPÉTENCES TECHNIQUES", "COMPETENCES TECHNIQUES", "ABILITIES", "EXPERTISE", "EXPERTISES",
                "SAVOIR-FAIRE", "SAVOIR FAIRE", "OUTILS", "TOOLS", "CORE COMPETENCIES", "KEY SKILLS",
                "COMPÉTENCES CLÉS", "COMPETENCES CLES", "PROGRAMMING LANGUAGES", "LANGAGES DE PROGRAMMATION"
        ));

        SECTION_KEYWORDS.put(SectionType.LANGUAGES, Set.of(
                "LANGUAGES", "LANGUES", "FOREIGN LANGUAGES", "LANGUES ÉTRANGÈRES", "LANGUES ETRANGERES",
                "LANGUAGE SKILLS", "COMPÉTENCES LINGUISTIQUES", "COMPETENCES LINGUISTIQUES", "MULTILINGUAL SKILLS"
        ));

        SECTION_KEYWORDS.put(SectionType.HOBBIES, Set.of(
                "HOBBIES", "INTERESTS", "CENTRES D'INTERET", "CENTRES D'INTÉRÊT", "LOISIRS", "ACTIVITIES",
                "ACTIVITÉS", "PERSONAL INTERESTS", "INTÉRÊTS PERSONNELS", "INTERETS PERSONNELS"
        ));

        SECTION_KEYWORDS.put(SectionType.CERTIFICATIONS, Set.of(
                "CERTIFICATIONS", "CERTIFICATES", "CERTIFICATS", "LICENSES", "ACCREDITATIONS",
                "PROFESSIONAL CERTIFICATIONS", "CERTIFICATIONS PROFESSIONNELLES"
        ));

        SECTION_KEYWORDS.put(SectionType.AWARDS, Set.of(
                "AWARDS", "HONORS", "PRIX", "DISTINCTIONS", "RÉCOMPENSES", "RECOMPENSES", "ACHIEVEMENTS",
                "HONOURS", "ACCOLADES", "RECOGNITION", "RECONNAISSANCES"
        ));
    }

    private static final Pattern ALL_CAPS_PATTERN = Pattern.compile("^[A-ZÀ-ÖØ-Þ\\s\\W_0-9]+$");
    private static final Pattern URL_PATTERN = Pattern.compile("(www\\.|http://|https://|linkedin\\.com).*");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"
    );

    // Flexible Phone Pattern (adapt as needed for French/International formats)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            // Matches common formats like +33 6..., 06..., (XXX) XXX-XXXX, XXX.XXX.XXXX etc.
            "(?:\\+\\d{1,3}[-.\\s]?)?(?:\\(?\\d{1,4}\\)?[-.\\s]?)?\\d{1,4}[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,9}\\b"
            // Filter out things that are clearly not phone numbers (e.g., just years)
            // This part is tricky and might need refinement based on false positives
            // "(?!^\\d{4}$)" // Example negative lookahead - might be too restrictive
    );

    // LinkedIn Profile URL Pattern
    private static final Pattern LINKEDIN_PATTERN = Pattern.compile(
            "linkedin\\.com/in/[\\w-]+/?", // Optional trailing slash
            Pattern.CASE_INSENSITIVE // Ignore case for domain
    );

    // GitHub Profile URL Pattern
    private static final Pattern GITHUB_PATTERN = Pattern.compile(
            "github\\.com/[\\w-]+/?", // Optional trailing slash
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern WEBSITE_PATTERN = Pattern.compile(
            // Starts with www. (word boundary before it)
            "\\b(?:www.)?" +
                    // Negative lookahead: Ensure the domain part right after www. isn't linkedin.com or github.com
                    "(?!(?:linkedin|github)\\.com\\b)" +
                    // Match the main domain name part (letters, numbers, hyphen)
                    "[\\w-]+" +
                    // Match the dot before the TLD
                    "\\." +
                    // Match the TLD: 2 or more letters (e.g., com, org, io, dev, fr, uk, ai, etc.)
                    "[a-z]{2,}" +
                    // Word boundary to ensure the TLD ends cleanly
                    "\\b" +
                    // Optionally match a path afterwards (slash followed by non-space characters)
                    "(?:/[^\\s]*)?",
            Pattern.CASE_INSENSITIVE
    );
    private static final float MIN_GAP_FACTOR = 1.5f;
    private static final float Y_TOLERANCE = 2.0f;
    private static final float X_MERGE_TOLERANCE = 1.0f;

    public List<ResumeLineEntity> groupItemsIntoLines(List<ResumeTextItemEntity> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        // Sort items: Top-down (Ascending Y), then Left-right (Ascending X)
        items.sort(Comparator.<ResumeTextItemEntity, Float>comparing(ResumeTextItemEntity::getY)
                .thenComparing(ResumeTextItemEntity::getX));

        List<ResumeLineEntity> lines = new ArrayList<>();
        if (items.isEmpty()) return lines;

        ResumeLineEntity currentLine = new ResumeLineEntity(items.get(0));
        lines.add(currentLine);

        for (int i = 1; i < items.size(); i++) {
            ResumeTextItemEntity currentItem = items.get(i);
            float lastItemY = currentLine.getItems().get(currentLine.getItems().size() - 1).getY();

            // Check Vertical Alignment
            if (Math.abs(currentItem.getY() - lastItemY) < Y_TOLERANCE) {
                // Add to current line
                currentLine.addItem(currentItem);
            } else {
                // Not vertically aligned - start a new line
                currentLine = new ResumeLineEntity();
                currentLine.addItem(currentItem);
                lines.add(currentLine);
            }
        }

        // Ensure all lines have items sorted by X
        for (ResumeLineEntity line : lines) {
            line.getItems().sort(Comparator.comparingDouble(ResumeTextItemEntity::getX));
            if (!line.getItems().isEmpty()) {
                line.setY(line.getItems().get(0).getY());
            }
        }

        return lines;
    }

    public List<ResumeSectionEntity> groupLinesIntoSections(List<ResumeLineEntity> lines, ParsingTraceDTO traceData) {
        if (lines == null || lines.isEmpty()) {
            return new ArrayList<>();
        }

        // Assuming Step 2 sorted lines top-down (ascending Y)
        List<ResumeSectionEntity> sections = new ArrayList<>();
        ResumeSectionEntity currentSection = null;
        LineProperties[] lineProperties = calculateLineProperties(lines); // Calculate properties once

        // --- Pass 1: Handle Initial Profile Section ---
        int firstRealSectionTitleIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            String lineText = lines.get(i).getLineText().trim();
            if(lineText.isEmpty()) continue;

            SectionType detectedType = detectSectionTypeFromKeywords(lineText);
            // Use a slightly stricter check maybe for the *first* title detection
            if (isPotentialSectionTitle(lineText, lineProperties[i], lineProperties, i) &&
                    detectedType != SectionType.UNKNOWN && detectedType != SectionType.PROFILE && detectedType != SectionType.SUMMARY) {
                firstRealSectionTitleIndex = i;
                break; // Found the first non-profile/summary title
            }
        }

        // If no clear section titles found, assume everything is PROFILE (or UNKNOWN)
        if (firstRealSectionTitleIndex == -1) {
            firstRealSectionTitleIndex = lines.size(); // Process all lines as the first section
        }

        // Create the initial section (likely PROFILE)
        List<ResumeLineEntity> initialLines = new ArrayList<>();
        String firstTitleText = null;
        SectionType initialType = SectionType.PROFILE; // Default guess

        // Check if the very first line itself might be a PROFILE/SUMMARY title
        if (firstRealSectionTitleIndex > 0) {
            ResumeLineEntity potentialFirstTitle = lines.get(0);
            String potentialFirstTitleText = potentialFirstTitle.getLineText().trim();
            SectionType firstLineType = detectSectionTypeFromKeywords(potentialFirstTitleText);
            if (!potentialFirstTitleText.isEmpty() &&
                    (firstLineType == SectionType.PROFILE || firstLineType == SectionType.SUMMARY) &&
                    isPotentialSectionTitle(potentialFirstTitleText, lineProperties[0], lineProperties, 0))
            {
                initialType = firstLineType;
                firstTitleText = potentialFirstTitleText;
                // Add lines from index 1 up to the first real section title
                for (int i = 1; i < firstRealSectionTitleIndex; i++) {
                    if (!lines.get(i).getLineText().trim().isEmpty()) {
                        initialLines.add(lines.get(i));
                    }
                }
            } else {
                // First line wasn't a PROFILE/SUMMARY title, add all lines up to the first real title
                for (int i = 0; i < firstRealSectionTitleIndex; i++) {
                    if (!lines.get(i).getLineText().trim().isEmpty()) {
                        initialLines.add(lines.get(i));
                    }
                }
            }
        } else { // No section titles found at all after potential profile
            for (int i = 0; i < lines.size(); i++) { // Add all lines
                if (!lines.get(i).getLineText().trim().isEmpty()) {
                    initialLines.add(lines.get(i));
                }
            }
        }


        if (!initialLines.isEmpty() || initialType != SectionType.PROFILE) { // Add if contains lines or if it was explicitly SUMMARY etc.
            currentSection = new ResumeSectionEntity(initialType, firstTitleText);
            initialLines.forEach(currentSection::addLine);
            sections.add(currentSection);
        }


        // --- Pass 2: Process Remaining Sections ---
        for (int i = firstRealSectionTitleIndex; i < lines.size(); i++) {
            ResumeLineEntity currentLine = lines.get(i);
            String lineText = currentLine.getLineText().trim();

            if (lineText.isEmpty()) {
                continue;
            }

            boolean isLikelyTitle = isPotentialSectionTitle(lineText, lineProperties[i], lineProperties, i);
            SectionType detectedType = isLikelyTitle ? detectSectionTypeFromKeywords(lineText) : SectionType.UNKNOWN;

            // --- Stricter condition to start a NEW section ---
            // Must be a likely title AND a known type different from current section
            // OR be the very first section being processed in this loop.
            boolean startNewSection = (isLikelyTitle &&
                    detectedType != SectionType.UNKNOWN &&
                    (currentSection == null || detectedType != currentSection.getType()));


            if (startNewSection) {
                currentSection = new ResumeSectionEntity(detectedType, lineText);
                sections.add(currentSection);
                // Title line is NOT added to content
            } else if (currentSection != null) {
                // Add content line to the *existing* current section
                currentSection.addLine(currentLine);
            }
            // If it's not a title and currentSection is somehow null here,
            // it means the very first detected title wasn't processed correctly.
            // We might need an UNKNOWN section as a fallback, but let's see if this works first.
            else if (currentSection == null) {
                // Fallback: If no section is active (shouldn't happen often after Pass 1),
                // create an UNKNOWN section.
                currentSection = new ResumeSectionEntity(SectionType.UNKNOWN, "Unknown Section Start");
                sections.add(currentSection);
                currentSection.addLine(currentLine); // Add the line that triggered this
            }
        }

        // Final cleanup
        sections.removeIf(section -> section.getLines().isEmpty() && section.getType() != SectionType.PROFILE); // Allow empty profile

        return sections;
    }

    public ProfileEntity extractProfileData(ResumeSectionEntity profileSection) {
        if (profileSection == null || profileSection.getType() != SectionType.PROFILE) {
            // Return empty or throw exception if the section is wrong/null
            return new ProfileEntity();
        }

        ProfileEntity profileEntity = new ProfileEntity();
        List<ResumeLineEntity> lines = profileSection.getLines();

        // --- Heuristic for Name (Often the first non-contact line) ---
        // This is basic and needs improvement for real-world cases
        if (!lines.isEmpty()) {
            String firstLineText = lines.get(0).getLineText().trim();
            // Simple check: if it doesn't look like contact info, assume it's the name
            if (!containsContactInfo(firstLineText) && firstLineText.length() < 50) {
                String[] nameParts = firstLineText.split("\\s+");
                if (nameParts.length > 0) {
                    profileEntity.setLastName(nameParts[nameParts.length - 1]); // Assume last part is last name
                    if (nameParts.length > 1) {
                        profileEntity.setFirstName(String.join(" ", Arrays.copyOfRange(nameParts, 0, nameParts.length - 1)));
                    }
                }
                // Consider removing the name line from further processing if desired
                // lines = lines.subList(1, lines.size()); // Be careful with modifying the list
            }
            // Handle case where name might be on second line if first is contact
            else if (lines.size() > 1) {
                String secondLineText = lines.get(1).getLineText().trim();
                if (!containsContactInfo(secondLineText) && secondLineText.length() < 50) {
                    String[] nameParts = secondLineText.split("\\s+");
                    if (nameParts.length > 0) {
                        profileEntity.setLastName(nameParts[nameParts.length - 1]);
                        if (nameParts.length > 1) {
                            profileEntity.setFirstName(String.join(" ", Arrays.copyOfRange(nameParts, 0, nameParts.length - 1)));
                        }
                    }
                }
            }
        }


        // --- Iterate through lines to find contact details ---
        for (ResumeLineEntity line : lines) {
            String lineText = line.getLineText(); // Use the full line text for matching

            // Extract Email (only if not already found)
            if (profileEntity.getEmail() == null) {
                Matcher emailMatcher = EMAIL_PATTERN.matcher(lineText);
                if (emailMatcher.find()) {
                    profileEntity.setEmail(emailMatcher.group());
                }
            }

            // Extract Phone (only if not already found)
            if (profileEntity.getPhone() == null) {
                Matcher phoneMatcher = PHONE_PATTERN.matcher(lineText);
                if (phoneMatcher.find()) {
                    // Extract the matched part, maybe do some basic cleanup
                    String potentialPhone = phoneMatcher.group();
                    // Add checks to avoid matching things like years "2024" if needed
                    if (potentialPhone.replaceAll("[^0-9]","").length() >= 7) { // Basic check for minimum digits
                        profileEntity.setPhone(potentialPhone.trim());
                    }
                }
            }

            // Extract LinkedIn (only if not already found)
            if (profileEntity.getLinkedInProfile() == null) {
                Matcher linkedinMatcher = LINKEDIN_PATTERN.matcher(lineText);
                if (linkedinMatcher.find()) {
                    // Prepend https:// if missing for consistency
                    String url = linkedinMatcher.group();
                    if (!url.toLowerCase().startsWith("http")) {
                        url = "https://" + url;
                    }
                    profileEntity.setLinkedInProfile(url);
                }
            }

            // Extract GitHub (only if not already found)
            if (profileEntity.getGithubProfile() == null) {
                Matcher githubMatcher = GITHUB_PATTERN.matcher(lineText);
                if (githubMatcher.find()) {
                    String url = githubMatcher.group();
                    if (!url.toLowerCase().startsWith("http")) {
                        url = "https://" + url;
                    }
                    profileEntity.setGithubProfile(url);
                }
            }

            // Extract Website/Portfolio (only if not already found AND not LinkedIn/GitHub)
            if (profileEntity.getWebsite() == null) {
                Matcher websiteMatcher = WEBSITE_PATTERN.matcher(lineText);
                if (websiteMatcher.find()) {
                    String url = websiteMatcher.group();
                    // Double check it's not one we already captured
                    if ((profileEntity.getLinkedInProfile() == null || !url.contains("linkedin.com")) &&
                            (profileEntity.getGithubProfile() == null || !url.contains("github.com")))
                    {
                        if (!url.toLowerCase().startsWith("http")) {
                            url = "http://" + url; // Default to http if protocol missing
                        }
                        profileEntity.setWebsite(url);
                    }
                }
            }
        }

        return profileEntity;
    }

    // Helper class to store line properties for better heuristic analysis
    private static class LineProperties {
        boolean isAllCaps;
        boolean isBold;
        float fontSize;
        boolean isGapAbove;
        float lineHeight;
        float distanceToNextLine;
        float averageFontSize;

        LineProperties(boolean isAllCaps, boolean isBold, float fontSize, boolean isGapAbove,
                       float lineHeight, float distanceToNextLine, float averageFontSize) {
            this.isAllCaps = isAllCaps;
            this.isBold = isBold;
            this.fontSize = fontSize;
            this.isGapAbove = isGapAbove;
            this.lineHeight = lineHeight;
            this.distanceToNextLine = distanceToNextLine;
            this.averageFontSize = averageFontSize;
        }
    }

    // Calculate line properties for better heuristics
    private LineProperties[] calculateLineProperties(List<ResumeLineEntity> lines) {
        LineProperties[] properties = new LineProperties[lines.size()];

        // Calculate average font size across document
        float totalFontSize = 0f;
        int fontSizeCount = 0;

        for (ResumeLineEntity line : lines) {
            for (ResumeTextItemEntity item : line.getItems()) {
                totalFontSize += item.getFontSize();
                fontSizeCount++;
            }
        }

        float averageFontSize = fontSizeCount > 0 ? totalFontSize / fontSizeCount : 12f; // Default if no items

        // Calculate line gaps
        for (int i = 0; i < lines.size(); i++) {
            ResumeLineEntity line = lines.get(i);
            String text = line.getLineText().trim();

            // Calculate if all caps
            boolean isAllCaps = !text.isEmpty() && ALL_CAPS_PATTERN.matcher(text).matches();

            // Calculate if mostly bold
            boolean isMostlyBold = false;
            float maxFontSize = 0f;

            List<ResumeTextItemEntity> items = line.getItems();
            if (!items.isEmpty()) {
                long boldCount = items.stream().filter(ResumeTextItemEntity::isBold).count();
                isMostlyBold = boldCount > 0 && boldCount >= (items.size() / 2.0);

                // Find max font size in line
                for (ResumeTextItemEntity item : items) {
                    maxFontSize = Math.max(maxFontSize, item.getFontSize());
                }
            }

            // Calculate distance to next line (for gap detection)
            float distanceToNextLine = 0f;
            if (i < lines.size() - 1) {
                distanceToNextLine = lines.get(i + 1).getY() - line.getY();
            }

            // Calculate if there's a significant gap above this line
            boolean isGapAbove = false;
            if (i > 0) {
                float distanceFromPrevLine = line.getY() - lines.get(i - 1).getY();
                isGapAbove = distanceFromPrevLine > MIN_GAP_FACTOR * averageFontSize;
            }

            // Store properties
            properties[i] = new LineProperties(
                    isAllCaps,
                    isMostlyBold,
                    maxFontSize,
                    isGapAbove,
                    items.isEmpty() ? 0 : items.get(0).getHeight(),
                    distanceToNextLine,
                    averageFontSize
            );
        }

        return properties;
    }

    // Improved section title detection with scoring
    private boolean isPotentialSectionTitle(String line, LineProperties props, LineProperties[] allProps, int lineIndex) {
        // Skip empty lines or overly long lines
        if (line.isEmpty() || line.length() > 60) {
            return false;
        }

        // --- Strong Negative Indicators ---
        String trimmedLine = line.trim();
        // Starts with bullet or common list marker
        if (trimmedLine.startsWith("•") || trimmedLine.startsWith("*") || trimmedLine.startsWith("- ")) {
            return false;
        }
        // Contains email/URL (already checked by containsContactInfo)
        if (containsContactInfo(line)) {
            return false;
        }
        // Ends with typical sentence punctuation (less likely for titles)
        if (trimmedLine.endsWith(".") || trimmedLine.endsWith(":") || trimmedLine.endsWith(";")) {
            // Allow exceptions for short acronyms like "...", but generally exclude
            if (trimmedLine.length() > 5) return false;
        }
        // Contains date ranges common in entries (simple check, can be improved)
        if (line.matches(".*\\d{4}\\s*–\\s*(Present|\\d{4}).*")) { // e.g., "2022 – Present", "2021 – 2022"
            return false;
        }

        // --- Scoring ---
        int score = 0;

        // Format indicators
        if (props.isAllCaps) score += 3;
        if (props.isBold) score += 2;
        // Slightly larger font is a weaker indicator, maybe reduce points or increase factor
        if (props.fontSize > props.averageFontSize * 1.2) score += 1; // Reduced points, increased factor

        // Significant gap indicators
        if (props.isGapAbove) score += 2;

        // Content indicators
        if (line.length() < 35) score += 1; // Adjusted length
        // Check for multiple words (titles usually have > 1 word)
        if (line.contains(" ")) score += 1;
        // Lack of common punctuation within the line
        if (!line.matches(".*[,;:•].*")) score += 1;

        // Keyword match is very strong evidence
        SectionType sectionType = detectSectionTypeFromKeywords(line);
        if (sectionType != SectionType.UNKNOWN) score += 5;

        // Final decision threshold
        return score >= 5; // Adjust threshold based on testing
    }

    // Check if a line has a significant gap above it
    private boolean hasSignificantGapAbove(int lineIndex, LineProperties[] properties) {
        return lineIndex > 0 && properties[lineIndex].isGapAbove;
    }

    // Improved section type detection using expanded keyword sets
    private SectionType detectSectionTypeFromKeywords(String text) {
        String upperText = text.toUpperCase();

        // Check each section type's keywords
        for (Map.Entry<SectionType, Set<String>> entry : SECTION_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (upperText.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }

        return SectionType.UNKNOWN;
    }

    // Helper to check if a line contains contact information
    private boolean containsContactInfo(String text) {
        return EMAIL_PATTERN.matcher(text).find() ||
                PHONE_PATTERN.matcher(text).find() ||
                URL_PATTERN.matcher(text).find();
    }
}