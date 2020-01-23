package intextbooks.content.models.formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import intextbooks.persistence.Persistence;

public class Page {
	private PageMetadata metadata;
	private String parentBook;
	private int lineCount;
	
	
	/**
	 * @param metadata
	 * @param map
	 * @throws Exception 
	 */
	public Page(String parentBookID, HashMap<PageMetadataEnum,String> metadata, int lineCount) {
		
		this.parentBook = parentBookID;
		this.metadata = new PageMetadata(metadata);
		this.lineCount = lineCount;
		
		
	}

	public HashMap<String, String> getMetadataAsMap() {
		return this.metadata.getMetadataAsMap();
	}

	public int getLineCount() {
		return lineCount;
	}

	public String getMetadata(PageMetadataEnum metadataName) {
		if(this.metadata.containsMetadata(metadataName))
			return this.metadata.getMetadataValue(metadataName);
		
		return null;
	}
	
	
	

	
	
	
	
}
