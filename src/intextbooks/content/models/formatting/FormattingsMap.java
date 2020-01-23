package intextbooks.content.models.formatting;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import intextbooks.SystemLogger;
import intextbooks.persistence.Persistence;

public class FormattingsMap {
	
	private Persistence map = Persistence.getInstance();
	private String parentBook;
	
	/**
	 * @param map
	 */
	public FormattingsMap(String parentBookID) {	
		this.parentBook = parentBookID;
	}
	
	public void createFreshFormattingsMap(){
		try {
				map.createFormatMap(this.parentBook);
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}		
	}
		

	public Integer getWordFormatting(int pageNumber, int lineNumber, int wordPos) {	
		return this.map.getWordFormatting(parentBook, pageNumber, lineNumber, wordPos);
		
	}

	public ArrayList<Integer> getFormatKeysInLine(int pageNumber, int lineNumber) {
		return this.map.getFormatKeysInLine(parentBook, pageNumber, lineNumber);
	}

	public CoordinatesContainer getWordCoordninates(int pageNumber, int lineNumber, int wordPos){
		return this.map.getWordCoordinates(parentBook, pageNumber, lineNumber, wordPos);
	}

	public void addMap(String parentBook, String pageIndex,
			ArrayList<ArrayList<Integer>> formatMap,
			ArrayList<ArrayList<CoordinatesContainer>> coordMap) {
		
		try {

			this.map.addPageFormatting(parentBook, pageIndex, formatMap, coordMap);
		} catch (Exception e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}
		
	}

	public ArrayList<FormattingContainer> getFormatMapOfAPage(int pageNumber) {
		//return this.map.getFormatMapOfAPage(parentBook, pageNumber);
		return new ArrayList<FormattingContainer>();
	}
	
	
	
	
}
