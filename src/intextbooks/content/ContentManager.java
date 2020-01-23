package intextbooks.content;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.skosapibinding.SKOSManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import intextbooks.Configuration;
import intextbooks.SystemLogger;
import intextbooks.content.enrichment.Enricher;
import intextbooks.content.extraction.ExtractorController;
import intextbooks.content.extraction.ExtractorController.resourceType;
import intextbooks.content.extraction.buildingBlocks.structure.BookStructure;
import intextbooks.content.models.Book;
import intextbooks.content.models.BookStatus;
import intextbooks.content.models.formatting.CoordinatesContainer;
import intextbooks.content.models.formatting.FormattingContainer;
import intextbooks.content.models.formatting.FormattingDictionary;
import intextbooks.content.models.formatting.PageMetadataEnum;
import intextbooks.content.models.formatting.lists.ListingContainer;
import intextbooks.content.utilities.ConceptContainer;
import intextbooks.exceptions.BookWithoutPageNumbersException;
import intextbooks.exceptions.EarlyInterruptionException;
import intextbooks.exceptions.EnrichedModelWIthProblems;
import intextbooks.exceptions.NoIndexException;
import intextbooks.exceptions.TOCNotFoundException;
import intextbooks.ontologie.LanguageEnum;
import intextbooks.ontologie.Mediator;
import intextbooks.persistence.Persistence;
import intextbooks.content.enrichment.Concept;
import intextbooks.tools.utility.MD5Utils;

public class ContentManager {
	private static ContentManager instance = null;
	HashMap<String,Book> content;
	HashMap<String,ArrayList<String>> contentGroups;
	private	int revision;
	private SystemLogger logger = SystemLogger.getInstance();
	private ExtractorController extractor = null;


	/*********************************************************************
	 ********************** CONSTRUCTOR AND INSTANCE GETTER **************
	 *********************************************************************/
	protected ContentManager() {
		this.content = new HashMap<String,Book>();
		this.contentGroups = new HashMap<String, ArrayList<String>>();
		this.revision = 0;
	}

	public void dropBooks() {
		content = new HashMap<String,Book>();
	}

	public void dropBook(String bookID) {
		this.content.remove(bookID);
	}

	public static ContentManager getInstance() {
		if(instance == null) {
			instance = new ContentManager();
		}
		return instance;
	}
	
	public String createNewBook(String bookID, String fileName, LanguageEnum lang, String bookName, String type, String group) {
		createNewBook(bookID, fileName, lang, bookName, "empty", type, group, null);

		return bookID;
	}


	public String createNewBook(String bookID, String fileName, LanguageEnum lang, String bookName, String checksum, String type, String group, String senderEmail) {
		try {
			SystemLogger.getInstance().log("*** creating new Book");
			Persistence.getInstance().createBook(bookID, fileName, Configuration.getInstance().getOntologyNS()+"/"+fileName, lang, bookName, checksum, type, group, senderEmail);
			//clean = false -> do not create a table to store the formatting of each word of the textbook
			this.content.put(bookID, new Book(bookID, bookName, fileName, lang, false, type));
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}

		return bookID;
	}

	public boolean removeBookModels(String bookID) {
		boolean flag = false;
		try {

			flag = Persistence.getInstance().removeContent(bookID, this.content.keySet());
			this.content.remove(bookID);
			SystemLogger.getInstance().log("Book " + bookID + " removed");
		} catch (IOException | SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
			return false;
		}

		//TODO: Implement removal from groupings

		return flag;
	}


	public int getRevision() {
		return revision;
	}


	/*********************************************************************
	 ********************** RETRIEVING METHODS **************
	 *********************************************************************/
	public String getBookName(String bookID) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getBookName();
		else
			return null;
	}
	public String getBookType(String bookID) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getType();
		else
			return null;
	}
	public String getBookURI(String bookID) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getURI();
		else
			return null;
	}
	public LanguageEnum getBookLanguage(String bookID) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getLanguageOfBook();
		else
			return null;
	}
	public String getBookFileName(String bookID) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getFileName();
		else
			return null;
	}

	/*********************************************************************
	 ***************** GETTER METHODS ****************
	 *********************************************************************/
	public ArrayList<String> getListOfAllBooks(){
		ArrayList<String> IDs = new ArrayList<String>();
		ArrayList<String> buffer = new ArrayList<String>();

		//--- hot Fix to sort the books by name
		Iterator<String> iterContent = content.keySet().iterator();
		while(iterContent.hasNext()){
			Book currentContent = content.get(iterContent.next());
			buffer.add(currentContent.getBookName()+"#"+currentContent.getBookID());
		}

		Collections.sort(buffer);
		//-- end hot fix

		Iterator<String> it = buffer.iterator();
		  while (it.hasNext())
			  IDs.add(it.next().split("#")[1]);

		return IDs;
	}

	public ArrayList<String> getListOfAllBooks(String type){
		ArrayList<String> IDs = new ArrayList<String>();

		Iterator it = content.entrySet().iterator();
		  while (it.hasNext()) {
			  Map.Entry<String,Book> entry = (Map.Entry)it.next();
		       if(entry.getValue().getType().equals(type))
		    	   IDs.add((String) entry.getKey());
		  }

		return IDs;
	}

	public ArrayList<String> getListOfAllBooks(LanguageEnum lang){
		ArrayList<String> IDs = new ArrayList<String>();

		Iterator it = content.entrySet().iterator();
		  while (it.hasNext()) {
		       Map.Entry<String,Book> entry = (Map.Entry)it.next();
		       if(entry.getValue().getLanguageOfBook() == lang)
		    	   IDs.add(0, entry.getKey());
		  }

		return IDs;
	}

	public ArrayList<String> getListOfAllBooksInGrouping(String groupingID){
		if(contentGroups.containsKey(groupingID))
			return contentGroups.get(groupingID);

		return new ArrayList<String>();
	}

	public ArrayList<String> getListOfAllBooks(String type, LanguageEnum lang) {
		ArrayList<String> IDs = new ArrayList<String>();

		Iterator it = content.entrySet().iterator();
		  while (it.hasNext()) {
			  Map.Entry<String,Book> entry = (Map.Entry)it.next();
			  if(entry.getValue().getType().equalsIgnoreCase(type) && entry.getValue().getLanguageOfBook() == lang)
				  IDs.add(entry.getKey());
		  }

		return IDs;
	}

	public ArrayList<String> getGroupingsList(){
		ArrayList<String> IDs = new ArrayList<String>();

		Iterator it = this.contentGroups.entrySet().iterator();
		  while (it.hasNext()) {
		       Map.Entry entry = (Map.Entry)it.next();
		       IDs.add((String) entry.getKey());
		  }

		return IDs;
	}

	public ArrayList<LanguageEnum> getLanguagesInUse(){
		ArrayList<LanguageEnum> languages = new ArrayList<LanguageEnum>();

		for (LanguageEnum currentLang : LanguageEnum.values()) {
			if(getListOfAllBooks(currentLang).size() > 0)
				languages.add(currentLang);
		}

		return languages;
	}


	public Book getInstanceOfBookByName(String bookId){

		return content.get(bookId);

	}

	/*********************************************************************
	 ***************** EXTRACTION/LOADING/STORAGE METHODS 
	 * @throws BookWithoutPageNumbersException 
	 * @throws EarlyInterruptionException 
	 * @throws TOCNotFoundException 
	 * @throws NullPointerException 
	 * @throws NoIndexException ****************
	 *********************************************************************/
	/*
	 * Option 1: Create the textbook using one method, and the process the textbook using other method
	 */
	public String createBook(String fileName, LanguageEnum lang, String bookName, String checksum, String type, String group, String senderEmail) {
		try {
			logger.log("Creating textbook " + fileName);
			String bookId = createNewBook(createBookID(fileName), fileName, lang, bookName, checksum, type, group, senderEmail);
			return bookId;
			
		} catch (Exception e) {
			return null;
		}
	}
	
	public BookStatus processBook(String bookId, String fileName, LanguageEnum lang, String bookName, boolean link, String type, String group, boolean processReadingLabels, boolean linkWithDBpedia, String dbpediaCat, boolean linkToExternalGlossary, boolean splitTextbook, String senderEmail) throws NullPointerException, TOCNotFoundException, BookWithoutPageNumbersException, NoIndexException {
		
		logger.log("Processing textbook " + bookId);
		boolean DBPediaError = false;
		String DBPediaErrorMsj = "";
		String filePath = Configuration.getInstance().getContentFolder()+File.separator+fileName;

		//Create basic model of the book
		//@ialpizar: change-> type comes from post request as BOOK (not book)
		if(type.equals("book") || type.equals("BOOK") )
			this.extractor = ExtractorController.getInstance(bookId, filePath,lang, resourceType.BOOK, processReadingLabels, linkToExternalGlossary, splitTextbook);
		
		
		logger.log("Extraction done");
		
		//Link to DBpedia
		if(linkWithDBpedia && dbpediaCat != null && dbpediaCat != "") {
			logger.log("Starting linking to DBPedia");
			try {
				Enricher enricher = new Enricher(bookId, dbpediaCat, lang);
				logger.log("Enrich with Dbpedia resoruces");
				enricher.enrichWithDBpedia();
				enricher.storeModel();
				logger.log("Linking to DBPedia done");
			} catch (Exception e) {
				DBPediaError = true;
				DBPediaErrorMsj = e.getMessage();
				logger.log("Linking to DBPedia could not be completed: "+ DBPediaErrorMsj);
			}	
		}

		//Store the models
		this.content.get(bookId).storeBookModel();
		
		//Split the PDF
		if(splitTextbook)
			this.content.get(bookId).splitCleanPDF();

		//Add to group
		if(group != null && this.contentGroups.containsKey(group))
			this.contentGroups.get(group).add(bookId);

		//Finish
		if(linkWithDBpedia && DBPediaError) {
			logger.log("Textbook " + bookId + " processing is finished with a DBPEDIA warning: " + DBPediaErrorMsj);
			return BookStatus.ProcessedNoDBpedia;
		} else {
			logger.log("Textbook " + bookId + " processing is finished");
			return BookStatus.Processed;
		}
		
	}
	
	/*
	 * Option 2: Create and process the textbook in one method
	 */
	public String processBookFile(String fileName, LanguageEnum lang, String bookName, boolean link, String type, String group) throws NullPointerException, TOCNotFoundException, EarlyInterruptionException, BookWithoutPageNumbersException, NoIndexException {

		return processBookFile(fileName, lang, bookName, link, type, group, true, true, true, true, null);
	}
	
	public String processBookFile(String fileName, LanguageEnum lang, String bookName, boolean link, String type, String group, boolean processReadingLabels, boolean linkWithDBpedia, boolean linkToExternalGlossary, boolean splitTextbook, String senderEmail) throws NullPointerException, TOCNotFoundException, EarlyInterruptionException, BookWithoutPageNumbersException, NoIndexException {

		if(fileName != null){
			logger.log("Processing file " + fileName);
			String bookId = createNewBook(createBookID(fileName), fileName, lang, bookName, "empty", type, group, senderEmail);
			String filePath = Configuration.getInstance().getContentFolder()+File.separator+fileName;

			//@ialpizar: change-> type comes from post request as BOOK (not book)
			if(type.equals("book") || type.equals("BOOK") )
				this.extractor = ExtractorController.getInstance(bookId, filePath,lang, resourceType.BOOK, processReadingLabels, linkToExternalGlossary, splitTextbook);
			else if(type.equals("slides"))
				this.extractor = ExtractorController.getInstance(bookId, filePath,lang, resourceType.SLIDE, processReadingLabels, linkToExternalGlossary, splitTextbook);

			logger.log("Extraction done");

			this.content.get(bookId).storeBookModel();
			//commented: highlight PDF are not fully implemeted yet 
			//this.content.get(bookId).splitPDF();
			this.content.get(bookId).splitCleanPDF();
			this.revision++;

			if(group != null && this.contentGroups.containsKey(group))
				this.contentGroups.get(group).add(bookId);

			logger.log("Textbook ID is " + bookId);
			return bookId;
			
		}

		return null;
	}
	
	/*********
	 * 
	 * EVALUATION SPECIFIC
	 * 
	 *********/
	public BookStructure processBookFileForEvaluation01(String fileName, String originalFilePath, LanguageEnum lang, String bookName) {

		if(fileName != null){
			logger.log("Processing file ForEvaluation01 " + fileName);
			String bookId = createNewBook(createBookID(fileName), fileName, lang, bookName, "group", null);
			String filePath = Configuration.getInstance().getContentFolder()+File.separator+fileName;
			try {
				FileUtils.copyFile(new File(originalFilePath), new File(filePath));
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}

			try {
				this.extractor = ExtractorController.getInstance(bookId, filePath,lang, resourceType.BOOK);
			} catch (NullPointerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TOCNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (EarlyInterruptionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BookWithoutPageNumbersException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoIndexException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			logger.log("Extraction done ForEvaluation01");


			/*this.content.get(bookId).storePages();
			this.content.get(bookId).storeBookModel();
			//this.content.get(bookId).splitPDF();
			this.content.get(bookId).splitCleanPDF();
			this.revision++;*/

			return this.extractor.getBookStructure();
		}

		return null;
	}
	
	public String processBookFileForEvaluation02(String fileName, String originalFilePath, LanguageEnum lang, String bookName) {

		if(fileName != null){
			logger.log("Processing file ForEvaluation01 " + fileName);
			String bookId = createNewBook(createBookID(fileName), fileName, lang, bookName, "group", null);
			String filePath = Configuration.getInstance().getContentFolder()+File.separator+fileName;
			try {
				FileUtils.copyFile(new File(originalFilePath), new File(filePath));
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}

			try {
				this.extractor = ExtractorController.getInstance(bookId, filePath,lang, resourceType.BOOK);
			} catch (NullPointerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TOCNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (EarlyInterruptionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BookWithoutPageNumbersException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoIndexException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			logger.log("Extraction done ForEvaluation01");


			/*this.content.get(bookId).storePages();
			this.content.get(bookId).storeBookModel();
			//this.content.get(bookId).splitPDF();
			this.content.get(bookId).splitCleanPDF();
			this.revision++;*/

			return bookId;
		}

		return null;
	}
	
	public String processBookFileForEvaluationDBpedia(String fileName, String originalFilePath, LanguageEnum lang, String bookName) {

		if(fileName != null){
			logger.log("Processing file ForEvaluation01 " + fileName);
			String bookId = createNewBook(createBookID(fileName), fileName, lang, bookName, "group", null);
			String filePath = Configuration.getInstance().getContentFolder()+File.separator+fileName;
			try {
				FileUtils.copyFile(new File(originalFilePath), new File(filePath));
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}

			try {
				this.extractor = ExtractorController.getInstance(bookId, filePath,lang, resourceType.BOOK);
			} catch (NullPointerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TOCNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (EarlyInterruptionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BookWithoutPageNumbersException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoIndexException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			logger.log("Extraction done ForEvaluation01");


			//this.content.get(bookId).storeBookModel();
			//this.content.get(bookId).splitPDF();
			//this.content.get(bookId).splitCleanPDF();
			this.revision++;

			return bookId;
		}

		return null;
	}

	public String processBookFile(String bookID, String fileName, LanguageEnum lang, String bookName, String type, String group) {
		String filePath = Configuration.getInstance().getContentFolder()+File.separator+fileName;

		if(fileName != null){
			logger.log("Processing file " + fileName);

			String bookId = createNewBook(bookID, fileName, lang, bookName, type, group);
			logger.log("New Book "+ bookName +" with ID " + bookID + " created");
			logger.log("Model extraction started");


			try {
				if(type.equals("book"))
					this.extractor = ExtractorController.getInstance(bookId, filePath,lang, resourceType.BOOK);
				else if(type.equals("slides"))
					this.extractor = ExtractorController.getInstance(bookId, filePath,lang, resourceType.SLIDE);
			} catch (NullPointerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TOCNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (EarlyInterruptionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BookWithoutPageNumbersException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoIndexException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			logger.log("Extraction done");

			this.content.get(bookId).storeBookModel();
			this.content.get(bookId).splitPDF();
			this.revision++;

			this.contentGroups.get(group).add(bookId);

			logger.log("Book " + bookId + " SUCCESSFULLY processed");

			return bookId;
		}

		return null;
	}

	private String createBookID(String fileName){
		Calendar calendar = Calendar.getInstance();
		java.util.Date now = calendar.getTime();
			
		String valForHash = fileName + String.valueOf(now.getTime());	
		return MD5Utils.getDigest(valForHash);
		
	}

	public void loadBookModels(){
		Persistence pLayer = Persistence.getInstance();
		ArrayList<String> bookList;
		ArrayList<String> groupings;

		try {
			bookList = pLayer.getBookList();
			Iterator<String> iter = bookList.iterator();
			while(iter.hasNext())
				loadBookModel(iter.next());

			groupings = pLayer.getGroupings();
			Iterator<String> iterGroups = groupings.iterator();
			while(iterGroups.hasNext()){
				String currentGroup = iterGroups.next();
				contentGroups.put(currentGroup, loadGroup(currentGroup));
			}

		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}
	}

	public void loadBookModel(String bookID) {
		logger.log("Loading book model " + bookID);
		LanguageEnum lang = LanguageEnum.valueOf(Persistence.getInstance().getLanguage(bookID));
		String bookName = Persistence.getInstance().getBookName(bookID);
		String bookFile = Persistence.getInstance().getFileName(bookID);
		String bookType = Persistence.getInstance().getType(bookID);

		Book newBook = new Book(bookID, bookName, bookFile, lang, false, bookType);
		newBook.loadBookModels();

		this.content.put(bookID, newBook);
		logger.log("done....");

	}

	public ArrayList<String> loadGroup(String groupID){
		try {
			return Persistence.getInstance().getGroupMembers(groupID);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return new ArrayList<String>();
	}

	public void storeBookModels() {
		ArrayList<String> IDs = new ArrayList<String>();

		Iterator it = content.entrySet().iterator();
		  while (it.hasNext()) {
		       Map.Entry<String,Book> entry = (Map.Entry)it.next();
		       entry.getValue().storeBookModel();
		  }


	}

	public void storeBookModels(String bookID) {
		if(this.content.containsKey(bookID))
			this.content.get(bookID).storeBookModel();

	}

	public ArrayList<String> getListOfStoredBooks(){
		try {
			return Persistence.getInstance().getBookList();
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
			return new ArrayList<String>();
		}
	}

	public void clearUserPDFfolder(String userName){
		Persistence.getInstance().clearUserPDFfolder(userName);
	}

	/*********************************************************************
	 ********************** FORMATTING METHODS ***************************
	 *********************************************************************/

	/**
	 * Methods to add Formatting Informations
	 * @throws Exception
	 */
	public void addPageFormatting(String bookID, ArrayList<ArrayList<Integer>> formatMap, ArrayList<ArrayList<CoordinatesContainer>> coordMap,
			HashMap<PageMetadataEnum,String> pageMetadata, HashMap<String, FormattingContainer> newDictEntries) throws Exception{

		if(this.content.containsKey(bookID))
			this.content.get(bookID).addPageFormatting(formatMap, coordMap, pageMetadata, newDictEntries);
	}



	/**
	 * Methods to get Formatting Informations
	 *
	 */
	public int formatDictSize(String bookID) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).formatDictSize();
		return 0;
	}

	public ArrayList<Integer> getFormatKeysInLine(String bookID,int pageNumber, int lineNumber) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getFormatKeysInLine(pageNumber, lineNumber);

		return new ArrayList<Integer>();
	}

	public String getFormatName(String bookID, Integer keySum) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getFormatName(keySum);

		return null;
	}

	public boolean bookContainsFormat(String bookID, int formatKey) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).containsFormat(formatKey);

		return false;
	}

	public Integer getFormatting(String bookID, int pageNumber, int lineNumber,int wordPos) {

		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getWordFormatting(pageNumber, lineNumber, wordPos);

		return null;
	}

	public ArrayList<FormattingContainer> getFormatMapOfAPage(String bookID,int pageNumber){
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getFormatMapOfAPage(pageNumber);

		return new ArrayList<FormattingContainer>();
	}

	public ArrayList<FormattingContainer> getFormatIntersectionInBooks(ArrayList<String> bookIDs) {
		//TODO
		return new ArrayList<FormattingContainer>();
	}

	public ArrayList<Integer> intersectingFormatesInLines(String bookID, ArrayList<Integer> lines) {
		//TODO
		return new ArrayList<Integer>();
	}

	public List<Integer> getPageNumbersOfBook(String bookID) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getPageNumbers();

		return new ArrayList<Integer>();
	}
	
	public void setPageNumbersOfBook(String bookID, List<Integer> pageNumbers) {
		if(this.content.containsKey(bookID))
			this.content.get(bookID).setPageNumbers(pageNumbers);
	}

	public CoordinatesContainer getCoordinates(String bookID, int pageNumber, int lineNumber,int wordPos) {

		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getCoordsOfWord(pageNumber, lineNumber, wordPos);

		return null;
	}



	/*********************************************************************
	 ********************** STRUCTURE METHODS ****************************
	 *********************************************************************/

	public Integer addChapter(String bookID, Integer parentIndex, Integer after,
			String title, int pageNumber, int startPage, int endPage, float coordLB, float coordLT, float coordRB, float coordRT) {

		if(this.content.containsKey(bookID))
			return this.content.get(bookID).addChapter(parentIndex, after, title, pageNumber, startPage, endPage, coordLB, coordLT, coordRB, coordRT);

		return -1;
	}

	public Integer addParagraph(String bookID, Integer parentIndex, Integer after,
			String reference, int pageNumber, int startPage, int endPage, float coordLB, float coordLT, float coordRB, float coordRT) {

		if(this.content.containsKey(bookID))
			return this.content.get(bookID).addParagraph(parentIndex, after, reference, pageNumber, startPage, endPage,"",coordLB, coordLT, coordRB, coordRT, true);

		return -1;
	}

	public ArrayList<Integer> getDescendingSegments(String bookID, int parentIndex) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getDescendingSegments(parentIndex);

		return new ArrayList<Integer>();
	}

	public ArrayList<Integer> getSubChaptersInBook(String bookID, int parentIndex) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getSubChapters(parentIndex);

		return new ArrayList<Integer>();
	}
	
	public ArrayList<Integer> getSiblingSegments(String bookID, int parentIndex) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getSiblingSegments(parentIndex);

		return new ArrayList<Integer>();
	}


	public ArrayList<Integer> getChildrenOfChapterInBook(String bookID, int parentIndex) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getChildrenOfChapter(parentIndex);

		return new ArrayList<Integer>();

	}

	public int getNextSiblingOfChapterInBook(String bookID,	int index) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getNextSiblingOfChapter(index);

		return -1;
	}

	public int getLevelOfSegment(String bookID, Integer index) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getLevelOfSegment(index);

		return -99;
	}

	public int getPreviousSiblingOfChapterInBook(String bookID, int index) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getPreviousSiblingOfChapter(index);

		return -1;
	}

	public int getParentOfSegment(String bookID, int index) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getParentOfSegment(index);

		return -1;
	}

	public String getContentOfSegmentInBook(String bookID, int index) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getContentOfSegment(index);

		return null;
	}

	public String getTitleOfSegmentInBook(String bookID, int index) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getSegmentTitle(index);

		return null;
	}

	public ArrayList<Integer> getAllParagraphsInSubStructure(String bookID, int parentIndex) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getAllLeavesInSubtree(parentIndex);

		return new ArrayList<Integer>();
	}

	public ArrayList<Integer> getAllParagraphsOnSameLevel(String bookID, int parentIndex) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getAllLeavesOnSameLevel(parentIndex);

		return new ArrayList<Integer>();
	}

	public void outputStructure(String bookID){
		if(this.content.containsKey(bookID))
			this.content.get(bookID).debugOutputStructure();
		else
			System.out.println("book not found");
	}

	public String getSegmentType(String bookID, int index){
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getSegmentType(index);

		return "";
	}

	public boolean isSegmentParagraph(String bookID, int index) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).isSegmentParagraph(index);

		return false;
	}

	public int getStartIndexOfSegment(String bookID, int segmentID) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getStartIndexOfSegment(segmentID);

		return -1;

	}

	public int getEndIndexOfSegment(String bookID, int segmentID) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getEndIndexOfSegment(segmentID);

		return -1;

	}
	
	public void addIndex(String bookID, String indexName, List<Integer> segments, ArrayList<Integer> indices, ArrayList<Integer> pages, boolean artificial){
		if(this.content.containsKey(bookID))
			this.content.get(bookID).addIndex(indexName, segments,  indices, pages, artificial);
	}

	/*OLD*/
//	public void addIndex(String bookID, String indexName, List<Integer> segments, ArrayList<Integer> indices){
//		if(this.content.containsKey(bookID))
//			this.content.get(bookID).addIndex(indexName, segments,  indices);
//	}
//
//	public void addIndex(String bookID, String indexName,  ArrayList<Integer> indices){
//		if(this.content.containsKey(bookID))
//			this.content.get(bookID).addIndex(indexName, indices);
//	}

	public void addConceptToIndex(String bookID, String indexName, String conceptName){
		if(this.content.containsKey(bookID))
			this.content.get(bookID).addConceptToIndex(indexName, conceptName);
	}

	public ArrayList<Integer> getOccurrencesOfIndex(String bookID, String indexName){
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getOccurrencesOfIndex(indexName);
		else
			return new ArrayList<Integer>();
	}

	public Integer getFirstOccurrenceOfIndex(String bookID, String indexName){
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getFirstOccurrenceOfIndex(indexName);
		else
			return -1;
	}


	public Integer getFirstOccurrenceOfIndexByConcept(String bookID, String conceptName){
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getFirstOccurrenceOfIndexByConcept(conceptName);
		else
			return -1;
	}

	public ArrayList<String> IndexTermsOnPage(String bookID, int pageIndex) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getIndexTermsOnPage(pageIndex);
		else
			return new ArrayList<String>();
	}

	public ArrayList<Integer> getSegmentsIdOfIndexTerm(String bookID, String indexName){
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getSegmentsIdOfIndexTerm(bookID, indexName);
		else
			return new ArrayList<Integer>();
	}

	/*OLD*/
//	public String getConceptOfIndexTerm(String bookID, String indexTerm) {
//		if(this.content.containsKey(bookID))
//			return this.content.get(bookID).getConceptOfIndexTerm(indexTerm);
//		else
//			return null;
//	}
	
	public String getConceptOfIndexElement(String bookID, String indexElement) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getConceptOfIndexElement(indexElement);
		else
			return null;
	}

	public List<String> getIndexTermsOfConcept(String bookID, String conceptName) {
		if (this.content.containsKey(bookID))
			return this.content.get(bookID).getIndexTermsOfConcept(conceptName);
		else
			return null;
	}
	
	public List<String> getIndexTermsOfIndexTerms(String bookID, List<String> labels) {
		if (this.content.containsKey(bookID)) {
			List<String> results = new ArrayList<String>();
			List<String> keyIndices = this.content.get(bookID).getListOfIndices();
			for(String keyIndex: keyIndices) {
				//if(keyIndex.equals("Analysis of variance (ANOVA)")) {
				//	System.out.println(" >>>> Analysis of variance (ANOVA)");
				//}
				String keyIndexToCompare= keyIndex.replace(" <> ", " ");
				keyIndexToCompare = keyIndexToCompare.toLowerCase().trim();
				keyIndexToCompare = Concept.removeChar(keyIndexToCompare);
				for(String label: labels) {
					/*if(keyIndex.equals("Analysis of variance (ANOVA)")) {
						System.out.println("label: " + label);
						System.out.println("keyIndexToCompare: " + keyIndexToCompare);
					}*/
					label = Concept.removeChar(label);
					if(keyIndexToCompare.equals(label)){
//						System.out.println("yes");
						results.add(keyIndex);
						//return results;
					}
				}
			}
			return results;
		}
		else
			return null;
	}
	
	public void updateConceptNameOfIndexElement(String bookID, String key, String conceptName) {
		this.content.get(bookID).updateConceptNameOfIndexElement(key, conceptName);
	}

	public ArrayList<String> getIndexTermsOfSegment(String bookID, int segmentID){
		if (this.content.containsKey(bookID))
			return this.content.get(bookID).getIndexTermsOfSegment(segmentID);
		else
			return new ArrayList<String>();
	}
	
	public Set<String>[] getIndexEntriesOfSegment(String bookID, int segmentID){
		if (this.content.containsKey(bookID))
			return this.content.get(bookID).getIndexEntriesOfSegment(segmentID);
		else {
			Set<String>[] array = (Set<String>[]) new Set[3];
			array[0] = new HashSet<String>();
			array[1] = new HashSet<String>();
			array[2] = new HashSet<String>();
			return array;
		}	
	}
	
	public ArrayList<String> getIndexTermsOfPageIndex(String bookID, int pageIndex){
		if (this.content.containsKey(bookID))
			return this.content.get(bookID).getIndexTermsOfPageIndex(pageIndex);
		else
			return new ArrayList<String>();
	}

	public ArrayList<ConceptContainer> getConceptsOfSegment(String bookID, int segmentID) {

		ArrayList<String> indexTerms = getIndexTermsOfSegment(bookID, segmentID);
		Iterator<String> iter = indexTerms.iterator();
		ConceptContainer currentConcept;
		String currentIndexTerm;
		ArrayList<ConceptContainer> conceptBuffer = new ArrayList<ConceptContainer>();
		ArrayList<ConceptContainer> conceptResult = new ArrayList<ConceptContainer>();
		Mediator mediator = Mediator.getInstance();

		while(iter.hasNext()){
			currentIndexTerm = iter.next();
			currentConcept = new ConceptContainer(currentIndexTerm);
			currentConcept.setConceptName(getConceptOfIndexElement(bookID, currentIndexTerm));
			conceptBuffer.add(currentConcept);

		}

		Iterator<ConceptContainer> iterContainer = conceptBuffer.iterator();

		while(iterContainer.hasNext()){
			currentConcept = iterContainer.next();
			if(currentConcept.hasConcept()){
				currentConcept.setURIs(mediator.getDBpediaURIsForConcept(currentConcept.getConceptName(), getBookLanguage(bookID)));
				if(!conceptResult.contains(currentConcept))
				//if(currentConcept.hasURIsAttached() && !currentConcept.getConceptName().equals("random") && !conceptResult.contains(currentConcept))
					conceptResult.add(currentConcept);
			}


		}

		return conceptResult;

	}


	public ArrayList<String> getDBpediaURIsOfConcept(String conceptName, String bookID){
		Mediator mediator = Mediator.getInstance();

		return mediator.getDBpediaURIsForConcept(conceptName, getBookLanguage(bookID));
	}

	public ArrayList<String> getListOfIndices(String bookID){
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getListOfIndices();
		else
			return new ArrayList<String>();
	}
	
	public ArrayList<String> getListOfIndicesWithPages(String bookID){
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getListOfIndicesWithPages();
		else
			return new ArrayList<String>();
	}

	public SKOSManager getSKOSModel(String bookID){
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getStructureModelAsSKOS();
		else
			return null;
	}

	public void setSKOSModel(String bookID, SKOSManager newModel){
		if(this.content.containsKey(bookID))
			this.content.get(bookID).setStructureSKOSModel(newModel);
	}
	
	public void setTEIModel(String bookID, Document teiModel){
		if(this.content.containsKey(bookID))
			this.content.get(bookID).setTEIModel(teiModel);
	}

	public int getBookDepth(String bookID){
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getBookDepth();
		else
			return -1;
	}

	public int getCurrentStructureIndex(String bookID) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getStructureModelIndexCounter();
		else
			return -1;
	}


	public File getParagraphText(String bookID, int index) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getParagraphText(index);
		else
			return null;
	}

	public ArrayList<Integer> getSegmentsOnPage(String bookID, int pageIndex){
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getSegmentsOnPage(pageIndex);
		else
			return new ArrayList<Integer>();
	}

	public int getSegmentOfWord(String bookID, int pageIndex, double WordCoordinateTopLeftY){
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getSegmentOfWord(pageIndex, WordCoordinateTopLeftY);
		else
			return -1;
	}

	public ArrayList<Integer> getRelatedSegmentsInBook(String slideID, int segmentID, String bookID) {
		if(this.content.containsKey(slideID) && this.content.containsKey(bookID))
			return Persistence.getInstance().getRelatedSegmentsInBook(slideID, segmentID, bookID);
		else
			return new ArrayList<Integer>();
	}

	public HashMap<Integer, ArrayList<Integer>> getSemanticMatchingSegments(String sourceBookID, String targetBookID) {
		if (this.content.containsKey(sourceBookID) && this.content.containsKey(targetBookID))
			return Persistence.getInstance().getSemanticMatchingSegments(sourceBookID, targetBookID);
		else
			return new HashMap<Integer, ArrayList<Integer>>();
	}
	
	public ArrayList<String> getAggregatedConcepts(String sourceBookID,ArrayList<Integer> allSegments) {
		if (this.content.containsKey(sourceBookID))
			return Persistence.getInstance(). getAggregatedConcepts(sourceBookID, allSegments);
		else
			return new ArrayList<String>();
	}
	
	public ArrayList<Pair<String,Integer>> getAggregatedConceptsWithFreq(String sourceBookID,ArrayList<Integer> allSegments) {
		if (this.content.containsKey(sourceBookID))
			return Persistence.getInstance(). getAggregatedConceptsWithFreq(sourceBookID, allSegments);
		else
			return new ArrayList<Pair<String,Integer>>();
	}


	/*********************************************************************
	 ********************** LISTING METHODS  *****************************
	 *********************************************************************/

	public void addListing(String bookID, ListingContainer listing) {
		if(this.content.containsKey(bookID))
			this.content.get(bookID).addListing(listing);
	}

	public ArrayList<ListingContainer> getListingsInSegment(String bookID, int segmentID) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getListingsInSegment(segmentID);
		else
			return new ArrayList<ListingContainer>();
	}

	public ArrayList<ListingContainer> getListingsOnPage(String bookID, int pageIndex) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getListingsOnPage(pageIndex);
		else
			return new ArrayList<ListingContainer>();
	}


	/***
	 * DEBUG ONLY
	 *
	 */
	public void setRevision(int revision) {
		this.revision = revision;
	}

	public void switchToLinkingTest() {
		Configuration.getInstance().switchToLT();

	}

	public void returnFromLinkingTest() {
		Configuration.getInstance().returnFromLT();

	}

	public Element getStrucutre(String bookID) {
		if(this.content.containsKey(bookID))
			return Persistence.getInstance().getStructureXML(bookID);
		else
			return null;

	}

	public HashMap<String, Double> getCoordinatesOfSegmentInBook(String bookID,
			int segmentID) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getCoordinatesOfSegment(segmentID);
		else
			return null;
	}

	public ArrayList<Integer> getAllLeavesInSubtree(String bookID, int segmentID) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getAllLeavesInSubtree(segmentID);
		else
			return null;
	}

	public ArrayList<Integer> getAllLeavesOnSameLevel(String bookID, int segmentID) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getAllLeavesOnSameLevel(segmentID);
		else
			return null;
	}

	public ArrayList<Integer> getLeaves(String bookID, int segmentID) {
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getLeaves(segmentID);
		else
			return null;
	}

	public String createPersonalizedPDFsegment(String bookID,String segmentID, String userName, LanguageEnum userLang) {
		if(this.content.containsKey(bookID)){

			//Get all books in the mother tongue of user
			ArrayList<String> booksToSearchthrough = getListOfAllBooks(userLang);

			HashMap<String, Boolean> explainPresent = new HashMap<String, Boolean>();

			//Get all index terms that appear in the segment of the target book
			int segmentIndx = Integer.parseInt(segmentID);
			ArrayList<String> indicesList = getIndexTermsOfSegment(bookID, segmentIndx);
			Iterator<String> indexListIterator = indicesList.iterator();

			//for each index term
			while(indexListIterator.hasNext()){
				String indexTerm = indexListIterator.next();
				//get the concept that relates to the index term
				String conceptName = getConceptOfIndexElement(bookID,  indexTerm);
				Iterator<String> iter = booksToSearchthrough.iterator();

				//for all books in the mother tongue 
				while(iter.hasNext()){
					String bookMT = iter.next();

					//get the segment where the same concept appears first
					int explainedSegmentID = getFirstOccurrenceOfIndexByConcept(bookMT, conceptName);

					//add to the Map if the term can be explained or not
					if(explainedSegmentID > 0){
						explainPresent.put(indexTerm, true);
					}
					else{
						explainPresent.put(indexTerm, false);
					}
				}
			}

			return this.content.get(bookID).createPersonalizedPDFsegment(Integer.valueOf(segmentID), userName, userLang, explainPresent);
		}else
			return null;
	}

	public void createGrouping(String name,LanguageEnum language) {
		String id = createBookID("");
		Persistence.getInstance().createGrouping(id,name, language);
		this.contentGroups.put(id, new ArrayList<String>());

	}

	public String getGroupingName(String groupID) {
		return Persistence.getInstance().getGroupName(groupID);
	}

	public String getGroupingLanguage(String groupID) {
		return Persistence.getInstance().getGroupLanguage(groupID);
	}

	public FormattingDictionary getStyleLibrary(String bookID) {	
		if(this.content.containsKey(bookID))
			return this.content.get(bookID).getStyleLibrary();
		else
			return null;
	}






















}
