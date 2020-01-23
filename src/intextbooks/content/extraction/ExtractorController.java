package intextbooks.content.extraction;


import java.util.Enumeration;

import intextbooks.content.extraction.buildingBlocks.structure.BookStructure;
import intextbooks.content.extraction.structure.StructureExtractor;
import intextbooks.exceptions.BookWithoutPageNumbersException;
import intextbooks.exceptions.EarlyInterruptionException;
import intextbooks.exceptions.NoIndexException;
import intextbooks.exceptions.TOCNotFoundException;
import intextbooks.ontologie.LanguageEnum;

public class ExtractorController {
	
	public static enum resourceType {BOOK, SLIDE};
	
	private StructureExtractor sE;
	private static ExtractorController instance;
	
	
   public static ExtractorController getInstance(String bookID,String filePath, LanguageEnum lang, resourceType type) throws NullPointerException, TOCNotFoundException, EarlyInterruptionException, BookWithoutPageNumbersException, NoIndexException {
		
		instance = new ExtractorController(bookID,filePath, lang,type, false, false, false);
		
		return instance;
	}
	
	public static ExtractorController getInstance(String bookID,String filePath, LanguageEnum lang, resourceType type, boolean processReadingLabels , boolean linkToExternalGlossary, boolean splitTextbook) throws NullPointerException, TOCNotFoundException, BookWithoutPageNumbersException, NoIndexException {
		
		instance = new ExtractorController(bookID,filePath, lang, type, processReadingLabels, linkToExternalGlossary, splitTextbook);
		
		return instance;
	}
	
	public ExtractorController(String bookID,String filePath, LanguageEnum lang, resourceType type, boolean processReadingLabels, boolean linkToExternalGlossary, boolean splitTextbook) throws NullPointerException, TOCNotFoundException, BookWithoutPageNumbersException, NoIndexException{	
		sE = new StructureExtractor(bookID,filePath,lang, type, processReadingLabels, linkToExternalGlossary, splitTextbook);			
	}
	
	public BookStructure getBookStructure() {
		return sE.getBookStructure();
	}
	
}
