package fr.eql.ai116.duflot.backend.entity;

public class ResumeTextItemEntity {

    private String text;
    private float x;
    private float y;
    private float width;
    private float height;
    private String fontName;
    private float fontSize;
    private boolean bold;
    private boolean hasEOL;

    public ResumeTextItemEntity(String text, float x, float y, float width, float height, String fontName, float fontSize, boolean bold) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.fontName = fontName;
        this.fontSize = fontSize;
        this.bold = bold;
        this.hasEOL = false;
    }

    public String getText() { return text; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public String getFontName() { return fontName; }
    public float getFontSize() { return fontSize; }
    public boolean isBold() { return bold; }
    public boolean isHasEOL() {
        return hasEOL;
    }

    public void setHasEOL(boolean hasEOL) {
        this.hasEOL = hasEOL;
    }

    @Override
    public String toString() {
        return "TextItem{" +
                "text='" + text + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                ", fontName='" + fontName + '\'' +
                ", fontSize=" + fontSize +
                ", bold=" + bold +
                '}';
    }
}
