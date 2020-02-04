package intextbooks.content.models;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.semanticweb.skosapibinding.SKOSManager;
import org.w3c.dom.Document;

import intextbooks.Configuration;
import intextbooks.SystemLogger;
import intextbooks.content.ContentManager;
import intextbooks.content.extraction.ExtractorController.resourceType;
import intextbooks.content.extraction.buildingBlocks.format.Page;
import intextbooks.content.models.formatting.CoordinatesContainer;
import intextbooks.content.models.formatting.FormatModel;
import intextbooks.content.models.formatting.FormattingContainer;
import intextbooks.content.models.formatting.FormattingDictionary;
import intextbooks.content.models.formatting.PageMetadataEnum;
import intextbooks.content.models.formatting.lists.ListingContainer;
import intextbooks.content.models.formatting.lists.ListingModel;
import intextbooks.content.models.structure.Segment;
import intextbooks.content.models.structure.StructureModel;
import intextbooks.ontologie.LanguageEnum;
import intextbooks.persistence.Disc;

public class Book {
	private FormatModel formatModel;
	private StructureModel structureModel;
	private ListingModel listingModel;
	private String bookID; 
	private LanguageEnum language;
	private String fileName;
	private String bookName;
	private String URI;
	private String type;
	private List<Page> pages;
	private List<Integer> pageNumbers;

	/**
	 * @param bookID
	 */
	public Book(String bookID, String bookName, String fileName, LanguageEnum lang, boolean clean, String type) {
		
		super();
		System.out.println("====================new Book=========================");
		this.bookID = cleanID(bookID);
		this.bookName = bookName;
		this.fileName = fileName;
		this.URI = "repository/content/books/"+fileName;
		this.type = type;
		
		this.formatModel = new FormatModel(this.bookID, this.fileName, clean);
		this.structureModel = new StructureModel(this.bookID, this.fileName);
		//this.relationModel = new RelationModel(this.bookID, this.fileName, clean);
		//this.highlightModel = new HighlightModel(this.bookID, clean);
		//this.listingModel = new ListingModel(this.bookID, clean);
		
		this.language = lang;
	}
	
	public void setPages(List<Page> pages) {
		this.pages = pages;
	}
	
	public List<Page> getPages(){
		return this.pages;
	}
	
	public void setPageNumbers(List<Integer> pageNumbers) {
		this.pageNumbers = pageNumbers;
	}
	
	public List<Integer> getPageNumbers(){
		return this.pageNumbers;
	}
	
	public void storePages() {
		 Disc disc =  Disc.getInstance();
		 disc.storePages(bookID, pages);
	}

	public String getBookID(){
		return this.bookID;
	}
	
	public String getURI() {
		return this.URI;
	}
		
	private String cleanID(String ID){
		String cleanedID = ID;
		cleanedID = cleanedID.replace(".", "_");
		
		return cleanedID;
	}
	
	public String getFileName() {
		return this.fileName;
	}
	
	public LanguageEnum getLanguageOfBook() {
		return this.language;
	}
	
	public String getBookName(){
		return this.bookName;
	}
	
	public String getType(){
		return this.type;
	}
	
	public void  storeBookModel() {
		storeModels();
	}
	
	public void loadBookModels() {
		System.out.println("====================Book.loadBookModels=========================");
//		try {
//			SystemLogger.getInstance().log("Waiting 1 minute");
//			TimeUnit.MINUTES.sleep(1);
//		} catch (InterruptedException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		this.formatModel.loadModel(this.bookID);
		this.structureModel.loadModel(this.bookID);
		
	}
	
	public int getStructureModelIndexCounter(){
		return this.structureModel.getIndexCounter();
	}
	
	/*********************************************************************
	 ********************** FORMATTING METHODS ***************************
	 *********************************************************************/
	
	/**
	 * Methods to add Formatting Informations
	 * @throws Exception 
	 */
	public void addPageFormatting(ArrayList<ArrayList<Integer>> formatMap, ArrayList<ArrayList<CoordinatesContainer>> coordMap,
			HashMap<PageMetadataEnum, String> pageMetadata, HashMap<String, FormattingContainer> newDictEntries) throws Exception {
		this.formatModel.addPageFormatting(formatMap, coordMap, pageMetadata, newDictEntries);
	}

	
	/**
	 * Methods to get Formatting Informations
	 * 
	 */
	public Integer getWordFormatting(int pageNumber, int lineNumber, int wordPos) {
		return this.formatModel.getWordFormatting(pageNumber, lineNumber, wordPos);
	}
	
	public ArrayList<Integer> getFormatKeysInLine(int pageNumber, int lineNumber){
		return this.formatModel.getFormatKeysInLine(pageNumber, lineNumber);
	}

	public int formatDictSize() {
		return this.formatModel.getDictSize();
	}



	public String getFormatName(Integer keySum) {
		return this.formatModel.getFormatName(keySum);
	}

	public boolean containsFormat(Integer formatKey) {
		return this.formatModel.containsFormat(formatKey);
	}

	public ArrayList<FormattingContainer> getFormatMapOfAPage(int pageNumber) {
		// TODO Auto-generated method stub
		return this.formatModel.getFormatMapOfAPage(pageNumber);
	}

	
	/**
	 * 
	 * @param string
	 * @param textindentation
	 * @return
	 */
	public String getPageMetadata(int pageIndex,
			PageMetadataEnum metaName) {
		return this.formatModel.getPageMetadata(pageIndex, metaName);
	}

	
	private void storeModels(){
		//this.formatModel.store();
		this.structureModel.store();
	}
	

	/*********************************************************************
	 ********************** STRUCTURE METHODS ***************************
	 *********************************************************************/
	
	
	public int addChapter(Integer parentIndex, Integer after, String reference, int pageNumber, int startPage, int endPage, 
			double coordLB, double coordLT, double coordRB, double coordRT) {
		return this.structureModel.addChapter(parentIndex, after, reference, pageNumber, startPage, endPage, coordLB, coordLT, coordRB, coordRT, startPage != endPage, reference);
		
	}

	public int addParagraph(Integer parentIndex, Integer after, String reference, int pageNumber, int startPage, int endPage, String fileContent,
			 double coordLB, double coordLT, double coordRB, double coordRT, boolean splitTextbook) {
		
		String parentTitle = this.getSegmentTitle(parentIndex) + " - paragraph " + reference.replace(".txt", "");
		
		return this.structureModel.addParagraph(parentIndex, after, reference, pageNumber, startPage, endPage, fileContent, coordLB, coordLT, coordRB, coordRT, startPage != endPage, parentTitle, splitTextbook);
		
	}

	public ArrayList<Integer> getDescendingSegments(int parentIndex) {
		return this.structureModel.getDescendingSegments(parentIndex);
	}
	
	public ArrayList<Integer> getSiblingSegments(int parentIndex) {
		return this.structureModel.getSiblingSegments(parentIndex);
	}
	
	public ArrayList<Integer> getSubChapters(int parentIndex) {
		return this.structureModel.getSubChapters(parentIndex);
	}
	
	public ArrayList<Integer> getChildrenOfChapter(int parentIndex){ 
		return this.structureModel.getChildrenOfChapter(parentIndex);
	}

	public int getNextSiblingOfChapter(int index) {
		return this.structureModel.getNextSiblingOfChapter(index);
	}

	public int getPreviousSiblingOfChapter(int index) {
		return this.structureModel.getPreviousSiblingOfChapter(index);
	}

	public int getParentOfSegment(int index) {
		return this.structureModel.getParentOfSegment(index);
	}

	public int getLevelOfSegment(Integer index) {
		return this.structureModel.getLevelOfSegment(index);
	}

	
	public void debugOutputStructure(){
		this.structureModel.debuggOutput();
	}

	public String getContentOfSegment(int index) {
		return this.structureModel.getContentOfSegment(index);
	}

	public ArrayList<Integer> getLeaves(int index) {
		return this.structureModel.getLeaves(index);
	}
	
	public ArrayList<Integer> getAllLeavesInSubtree(int index) {
		return this.structureModel.getAllLeaves(index);
	}

	public ArrayList<Integer> getAllLeavesOnSameLevel(int index) {
		return this.structureModel.getAllLeavesOnSameLevel(index);
	}

	public String getSegmentType(int index) {
		return this.structureModel.getSegmentType(index);
		
	}
	
	public String getSegmentTitle(int index){
		return this.structureModel.getSegmentTitle(index);
	}
	
	public boolean isSegmentParagraph(int index) {
		return this.structureModel.isSegmentParagraph(index);
		
	}

	public HashMap<String, Double> getCoordinatesOfSegment(int index) {
		return this.structureModel.getPositionOfSegment(index);
	}

	public CoordinatesContainer getCoordsOfWord(int pageNumber, int lineNumber, int wordNumber) {
		return this.formatModel.getCoordsOfWord(pageNumber, lineNumber, wordNumber);
		
	}

	public int getStartIndexOfSegment(int segmentID) {
		return this.structureModel.getStartIndexOfSegment(segmentID);
	}

	public int getEndIndexOfSegment(int segmentID) {
		return this.structureModel.getEndIndexOfSegment(segmentID);
	}
	
	public ArrayList<Integer> getSegmentsOnPage(int pageIndex) {
		return this.structureModel.getSegmentsOnPage(pageIndex);
	}

	public int getSegmentOfWord(int pageIndex, double wordCoordinateTopLeftY) {
		boolean parent = this.type.equals("slides") ? true : false;
		
		return this.structureModel.getSegmentOfWord(pageIndex, wordCoordinateTopLeftY, parent);
	}
	
	public SKOSManager getStructureModelAsSKOS(){
		return this.structureModel.getSKOSModel();
	}
	
	public void setStructureSKOSModel(SKOSManager newModel){
		this.structureModel.setSKOSModel(newModel);
	}
	
	public void setTEIModel(Document teiModel){
		this.structureModel.setTEIModel(teiModel);
	}
	
	public void splitPDF(){
		this.structureModel.splitPDF();
	}
	
	public void splitCleanPDF(){
		this.structureModel.splitCleanPDF();
	}
	
	public String createPersonalizedPDFsegment(int segmentID, String userName, LanguageEnum userLang, HashMap<String, Boolean> explainPresent) {
		return this.structureModel.createPersonalizedPDFsegment(segmentID, userName,userLang,explainPresent);
	}
	
	public void addIndex(String indexName, List<Integer> segments, List<Integer> indices, List<Integer> pages, boolean artificial) {
		this.structureModel.addIndex(indexName, segments, indices, pages, artificial);
	}
	
	/*OLD*/
//	public void addIndex(String indexName, List<Integer> segments, ArrayList<Integer> indices) {
//		this.structureModel.addIndex(indexName, segments, indices);
//	}
//	
//	public void addIndex(String indexName, ArrayList<Integer> indices) {
//		this.structureModel.addIndex(indexName, indices);
//	}
	
	public void addConceptToIndex(String indexName, String conceptName) {
		this.structureModel.addConceptToIndex(indexName, conceptName);
		
	}

	public ArrayList<Integer> getOccurrencesOfIndex(String indexName) {
		return this.structureModel.getOccurrencesOfIndex(indexName);
		
	}

	public ArrayList<String> getIndexTermsOnPage(int pageIndex) {
		return this.structureModel.getIndexTermsOnPage(pageIndex);
	}
	
	public Set<String>[] getIndexEntriesOfSegment(int segmentID) {
		Set<String> indexKeys = new HashSet<String>();
		Set<String> indexLabels = new HashSet<String>();
		Set<String> indexConcepts = new HashSet<String>();
		
		Set<String>[] array = (Set<String>[]) new Set[3];
		array[0] = indexKeys;
		array[1] = indexLabels;
		array[2] = indexConcepts;
		
		//get the parent, and then get all the childs
		//if the section does not have any subsection, it returns only the segment id of the section
		//if the section has subsections, it returnes the list of segments id of the section and its subctions
		ArrayList<Integer> segmentList = getAllLeavesInSubtree(this.structureModel.getParentOfSegment(segmentID));
		System.out.println("segList: " +segmentList);
		Iterator<Integer> iter = segmentList.iterator();
		
		while(iter.hasNext()) {
			Set<String>[] res = this.structureModel.getIndexEntriesOfSegment(iter.next());
			
			indexKeys.addAll(res[0]);
			indexLabels.addAll(res[1]);
			indexConcepts.addAll(res[2]);
		}
			
		return array;
	}
	
	public ArrayList<String> getIndexTermsOfSegment(int segmentID) {
		

		ArrayList<String> indexList = new ArrayList<String>();
		ArrayList<String> bufferList = new ArrayList<String>();
		//get the parent, and then get all the childs
		//if the section does not have any subsection, it returns only the segment id of the section
		//if the section has subsections, it returnes the list of segments id of the section and its subctions
		ArrayList<Integer> segmentList = getAllLeavesInSubtree(this.structureModel.getParentOfSegment(segmentID));
		System.out.println("segList: " +segmentList);
		Iterator<Integer> iter = segmentList.iterator();
		
		while(iter.hasNext())
			bufferList.addAll(this.structureModel.getIndexTermsOfSegment(iter.next()));
		
		Iterator<String> iterBuffer = bufferList.iterator();
		
		while(iterBuffer.hasNext()){
			String currentIndexTerm = iterBuffer.next();
			if(!indexList.contains(currentIndexTerm))
				indexList.add(currentIndexTerm);
		}
		return indexList;
	}
	
	public ArrayList<String> getIndexTermsOfPageIndex(int pageIndex) {
		return this.structureModel.getIndexTermsOnPage(pageIndex);
	}
	
	public Integer getFirstOccurrenceOfIndexByConcept(String conceptName) {
		return this.structureModel.getFirstOccurrenceOfIndexByConcept(conceptName);
	}
	
	public ArrayList<Integer> getSegmentsIdOfIndexTerm(String bookID, String indexName){
		return this.structureModel.getSegmentsIdOfIndexTerm(bookID, indexName);
	}
	
	public Integer getFirstOccurrenceOfIndex(String indexName) {
		return this.structureModel.getFirstOccurrenceOfIndex(indexName);
	}

	/*OLD*/
//	public String getConceptOfIndexTerm(String indexTerm) {
//		return this.structureModel.getConceptOfIndexTerm(indexTerm);
//	}
	
	public String getConceptOfIndexElement(String indexElement) {
		return this.structureModel.getConceptOfIndexElement(indexElement);
	}
	
	public List<String> getIndexTermsOfConcept(String conceptName) {
		return this.structureModel.getIndexTermsOfConcept(conceptName);
	}
	
	public ArrayList<String> getListOfIndices() {
		return this.structureModel.getListOfIndices();
	}
	
	public void updateConceptNameOfIndexElement(String key, String conceptName) {
		this.structureModel.updateConceptNameOfIndexElement(key, conceptName);
	}
	
	public ArrayList<String> getListOfIndicesWithPages() {
		return this.structureModel.getListOfIndicesWithPages();
	}

	
	public int getBookDepth() {
		return this.structureModel.getBookDepth();
	}
	
	public File getParagraphText(int index) {
		return this.structureModel.getParagraphText(index);
	}
	
	/*********************************************************************
	 ********************** SEMANTIC METHODS 
	  ***************************
	 *********************************************************************/
	

	/*********************************************************************
	 ********************** LISTING METHODS 
	  ***************************
	 *********************************************************************/
	
	public void addListing(ListingContainer listing) {
		this.listingModel.addListing(listing);
	}
	
	public ArrayList<ListingContainer> getListingsInSegment(int segmentID) {
		return this.listingModel.getListingsInSegment(segmentID);
	}
	
	public ArrayList<ListingContainer> getListingsOnPage(int pageIndex) {
		return this.listingModel.getListingsOnPage(pageIndex);
	}
	
	public FormattingDictionary getStyleLibrary() {
		return this.formatModel.getStyleLibrary();
	}
	
}