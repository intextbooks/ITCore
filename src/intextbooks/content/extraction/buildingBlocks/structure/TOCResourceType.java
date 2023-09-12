package intextbooks.content.extraction.buildingBlocks.structure;

public enum TOCResourceType {
	CHAPTER("CHAPTER"), 
	SECTION("SECTION"),
	BACK("BACK");
	
	private final String value;

    private TOCResourceType(final String value) {
        this.value = value;
    }
}
