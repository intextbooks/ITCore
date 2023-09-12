package intextbooks.content.models.structure;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.util.URIref;

import intextbooks.SystemLogger;
import intextbooks.persistence.Persistence;

public class IndexMap {

	private Persistence map = Persistence.getInstance();
	private String parentBook;
	
	/**
	 * @param map
	 */
	public IndexMap(String parentBookID) {
		this.parentBook = parentBookID;
	}
	
	public void addIndex(String indexName, List<Integer> segments, List<Integer> indices, List<Integer> pages, boolean artificial){
		try {
			map.addIndexElement(parentBook, indexName, segments, indices, pages, artificial);
		} catch (Exception e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}
	}
	
	public ArrayList<Integer> getOccurrencesOfIndex(String indexName) {
		try {
			return map.getOccurrencesOfIndex(parentBook, indexName);
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return new ArrayList<Integer>();
		}
	}
	
	public ArrayList<Integer> getOccurrencesOfIndexByConcept(String conceptName) {
		try {
			return map.getOccurrencesOfIndexByConcept(parentBook, conceptName);
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return new ArrayList<Integer>();
		}
	}
	
	public void updateConceptNameOfIndexElement(String key, String conceptName) {
		map.updateConceptNameOfIndexElement(parentBook, key, conceptName);
	}
	
	public ArrayList<String> getFullList(){
		try {
			return map.getListOfIndicesForBook(parentBook);
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return new ArrayList<String>();
		}
	}
	
	public ArrayList<String> getWithPagesList(){
		try {
			return map.getListOfIndicesWithPageForBook(parentBook);
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return new ArrayList<String>();
		}
	}

	/*OLD*/
//	public String getConceptOfIndexTerm(String indexTerm) {
//		try {
//			String conceptName = map.getConceptOfIndexTerm(parentBook, indexTerm);
//			if(conceptName != null)
//				return URLEncoder.encode(conceptName, "UTF-8");
//			
//			return conceptName;
//		} catch (SQLException | UnsupportedEncodingException e) {
//			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
//			return null;
//		}
//	}
	
	public String getConceptOfIndexElement(String indexElement) {
		try {
			String conceptName = map.getConceptOfIndexElement(indexElement, parentBook);
			if(conceptName != null)
				return URLEncoder.encode(conceptName, "UTF-8");
			
			return conceptName;
		} catch (SQLException | UnsupportedEncodingException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return null;
		}
	}
	
	public List<String> getIndexTermsOfConcept(String conceptName) {
		try {
			return map.getIndexTermsOfConcept(parentBook, conceptName);
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return null;
		}
	}
	
	public void addConceptToIndexIndex(String indexTerm, String conceptName) {
		try {
			map.addConceptToIndexElement(parentBook, indexTerm, conceptName);
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}
		
	}
	
//	public void addConceptToIndexIndexElement(String indexTerm, String conceptName) {
//		try {
//			map.addConceptToIndexElement(parentBook, indexTerm, conceptName);
//		} catch (SQLException e) {
//			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
//		}
//		
//	}

	public ArrayList<String> getIndexTermsOnPage(int pageIndex) {
		try {
			return map.getIndexTermsOnPage(parentBook, pageIndex);
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return new ArrayList<String>();
		} 
	}
	
	public ArrayList<String> getIndexTermsOfSegment(int segmentID) {
		try {
			return map.getIndexTermsOfSegment(parentBook, segmentID);
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return new ArrayList<String>();
		} 
	}
	
	public Set<String>[] getIndexEntriesOfSegment(int segmentID) {
		try {
			return map.getIndexEntriesOfSegment(parentBook, segmentID);
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			Set<String>[] array = (Set<String>[]) new Set[3];
			array[0] = new HashSet<String>();
			array[1] = new HashSet<String>();
			array[2] = new HashSet<String>();
			return array;
		} 
	}
	
	public ArrayList<Integer> getSegmentsIdOfIndexTerm(String bookID, String indexName){
		try {
			return map.getSegmentsIdOfIndexTerm(bookID, indexName);
		}
		catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return new ArrayList<Integer>();
		} 
	}
}
