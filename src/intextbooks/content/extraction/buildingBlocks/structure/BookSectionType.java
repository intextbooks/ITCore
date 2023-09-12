package intextbooks.content.extraction.buildingBlocks.structure;

public enum BookSectionType {
	CHAPTER("CHAPTER"), 
	SUBCHAPTER("SUBCHAPTER"),
	PARAGRAPH("PARAGRAPH");
	
	private final String value;

    private BookSectionType(final String value) {
        this.value = value;
    }
}
