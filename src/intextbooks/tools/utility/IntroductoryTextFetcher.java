package intextbooks.tools.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import intextbooks.SystemLogger;
import intextbooks.content.ContentManager;
import intextbooks.ontologie.LanguageEnum;

public class IntroductoryTextFetcher {
	
	private ContentManager cm;
	private Map<LanguageEnum, List<String>> books = new HashMap<LanguageEnum, List<String>>();
	
	public IntroductoryTextFetcher() {
		
		this.cm = ContentManager.getInstance();
		ArrayList<LanguageEnum> langs = cm.getLanguagesInUse();
		
		for (LanguageEnum lang : langs) {
			
			List<String> langBooks = cm.getListOfAllBooks(lang);
			books.put(lang, langBooks);
			
		}
		
	}
	
	public String getIntroductionsForConcept(String concept, LanguageEnum lang) {
		
		//SystemLogger.getInstance().log(">> lang: " + lang);
		List<String> langBooks = books.get(lang);
		//SystemLogger.getInstance().log(">> langBooks: " + langBooks);
		StringBuilder sb = new StringBuilder();
		if (langBooks == null) {
			return sb.toString();
		}
		try {
		
			for (String bookID : langBooks) {
				
				List<String> indexTerms = cm.getIndexTermsOfConcept(bookID, URLEncoder.encode(concept, "UTF-8"));
				
				for (String index : indexTerms) {
				
					int page = cm.getFirstOccurrenceOfIndex(bookID, index);
					//@i.alpizarchacon align page with numbers on segments + 1 
					page = page+1;
					List<Integer> segments = cm.getSegmentsOnPage(bookID, page);
				
					for (int segID : segments) {
					
						if (cm.isSegmentParagraph(bookID, segID)) {
						
							File parFile = cm.getParagraphText(bookID, segID);
							BufferedReader reader = null;
							try {
								
								reader = new BufferedReader(new FileReader(parFile));
								String line = null;
								
								while ((line = reader.readLine()) != null) {
									sb.append(line);
								}
								
							} catch (FileNotFoundException e) {
								e.printStackTrace();
								SystemLogger.getInstance().log(e.toString());
							} catch (IOException e) {
								e.printStackTrace();
								SystemLogger.getInstance().log(e.toString());
							} finally {
								
								try {
									reader.close();
								} catch (IOException e) {
									e.printStackTrace();
									SystemLogger.getInstance().log(e.toString());
								}
								
							}
							
						}
						
						sb.append(" ");
					
					}
					
				}
				
			}
			
			return sb.toString();
		
		} catch (UnsupportedEncodingException e) {
			
			return "";
			
		}		
		
	}

}
