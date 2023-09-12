package intextbooks.content.models.formatting;



import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import intextbooks.Configuration;
import intextbooks.SystemLogger;
import intextbooks.persistence.Persistence;

public class FormatModel {
	private HashMap<String,Page> pages; 
	private FormattingDictionary dict; 
	private String parentBookID;
	private String parentBookFileName;
	private FormattingsMap formatMap;
	
	
	public FormatModel(String bookID, String fileName, boolean clean){
		this.pages = new HashMap<String,Page>();
		this.dict = new FormattingDictionary();
		this.parentBookID = bookID;
		this.parentBookFileName = fileName;
		this.formatMap = new FormattingsMap(bookID);
		
		if(clean)
			this.formatMap.createFreshFormattingsMap();
	}
	
	
	public void addPageFormatting(ArrayList<ArrayList<Integer>> formatMap, ArrayList<ArrayList<CoordinatesContainer>> coordMap, 
			HashMap<PageMetadataEnum,String> pageMetadata, HashMap<String,FormattingContainer> potentialNewDictEntries) throws Exception{
		
		this.pages.put(pageMetadata.get(PageMetadataEnum.PageIndex),new Page(this.parentBookID, pageMetadata, formatMap.size()));
		//add to database
		//this.formatMap.addMap(this.parentBookID, pageMetadata.get(PageMetadataEnum.PageIndex), formatMap, coordMap);
		
		updateDictionary(potentialNewDictEntries);
		
	}
	
	
	public Integer dictAddFormat(FormattingContainer format){
		return this.dict.containsFormat(format);
	}

	public Integer getWordFormatting(int pNumber, int lineNumber, int wordPos) {
		String pageNumber = String.valueOf(pNumber);
		
		if(this.pages.containsKey(pageNumber) && this.pages.get(pageNumber).getLineCount() >= lineNumber)
			return this.formatMap.getWordFormatting(pNumber, lineNumber, wordPos);

		return null;
	}
	
	
	private void updateDictionary(HashMap<String,FormattingContainer> potentialNewDictEntries){
		
		  Iterator it = potentialNewDictEntries.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry format = (Map.Entry)it.next();		        
		        this.dict.containsFormat((FormattingContainer)format.getValue());
		    }

	}

	public int getDictSize() {
		return this.dict.getSize();
	}

	public ArrayList<Integer> getFormatKeysInLine(int pNumber, int lineNumber) {
		String pageNumber = String.valueOf(pNumber);
		
		if(this.pages.containsKey(pageNumber) && this.pages.get(pageNumber).getLineCount() >= lineNumber)
			return this.formatMap.getFormatKeysInLine(pNumber, lineNumber);
		
		return new ArrayList<Integer>();
		
	}
	
	public ArrayList<FormattingContainer> getFormatMapOfAPage(int pageNumber) {
		// TODO Auto-generated method stub
		String pageNum = String.valueOf(pageNumber);
		
		if(this.pages.containsKey(pageNum) )
			return this.formatMap.getFormatMapOfAPage(pageNumber);
		
		return new ArrayList<FormattingContainer>();
	}

	public String getFormatName(Integer keySum) {
		return this.dict.getFormatName(keySum);
	}

	public boolean containsFormat(Integer formatKey) {
		return this.dict.checkForFormat(formatKey);
	}

	public void store() {
		SystemLogger.getInstance().log("persisting format model");
		Persistence.getInstance().storeFormatModel(parentBookID,pages,dict);
	}


	public String getPageMetadata(int pIndex,PageMetadataEnum metaName) {
		String pageIndex = String.valueOf(pIndex);
		
		if(this.pages.containsKey(pageIndex))
			return this.pages.get(pageIndex).getMetadata(metaName);
		
		return null;
	}


	public void loadModel(String bookID) {
		this.parentBookID = bookID;
		this.parentBookFileName = Persistence.getInstance().getFileName(bookID);
		this.formatMap = new FormattingsMap(bookID);
		
		if(this.parentBookFileName != null){
			try {
				this.pages = Persistence.getInstance().loadPageMap(this.parentBookID);
				this.dict = Persistence.getInstance().loadFormatDictionary(this.parentBookID);
			} catch (Exception e) {
				e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			}
		}else
			SystemLogger.getInstance().log("Unable to load format model of book " + bookID);
		
	}


	public CoordinatesContainer getCoordsOfWord(int pNumber, int lineNumber, int wordPos) {
		String pageNumber = String.valueOf(pNumber);
		
		if(this.pages.containsKey(pageNumber) && this.pages.get(pageNumber).getLineCount() >= lineNumber)
			return this.formatMap.getWordCoordninates(pNumber, lineNumber, wordPos);

		return null;
	}
	
	public FormattingDictionary getStyleLibrary() {
		return this.dict;
	}





	
	
	
}
