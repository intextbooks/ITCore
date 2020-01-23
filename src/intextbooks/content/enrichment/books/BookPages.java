package intextbooks.content.enrichment.books;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import intextbooks.content.extraction.buildingBlocks.format.Page;

public class BookPages implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Map<Integer, String> pages;
	
	public BookPages(List<Page> resourcePages) {
		this.pages = new HashMap<Integer, String>();
		String allText;
		for(int i = 0; i < resourcePages.size();i++) {
			if(resourcePages.get(i) != null) {
				allText = resourcePages.get(i).getText().toLowerCase().replaceAll("\n", " ");
				this.pages.put(i, allText);
			} 
		}
	}
	
	public String getTextFromPage(int page) {
		return this.pages.get(page);
	}

}
