package intextbooks.content.extraction.format.lists;

import intextbooks.content.extraction.ExtractorController.resourceType;

public class ListExtractorFactory {
	
	public static ListExtractor createListExtractor(String bookID, resourceType type) {
		
		if (type == resourceType.SLIDE)
			return new SlideListExtractor(bookID);
		
		if (type == resourceType.BOOK)
			return null;
		
		return null;
		
	}

}
