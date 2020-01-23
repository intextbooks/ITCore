package intextbooks.content.models.formatting;

public enum PageMetadataEnum {
	
	PageNumber("PageNumber"), 
	PageIndex("PageIndex"), 
	ChapterIndentation("ChapterIndentation"),
	TextIndentation("TextIndentation"),
	SubChapterIndentation("SubChapterIndentation"),
	LineCount("LineCount");
	
    private final String value;

    private PageMetadataEnum(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getValue();
    }
    
   
	
	
}
