package intextbooks.persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.skos.SKOSCreationException;
import org.semanticweb.skos.SKOSDataset;
import org.semanticweb.skos.SKOSStorageException;
import org.semanticweb.skosapibinding.SKOSFormatExt;
import org.semanticweb.skosapibinding.SKOSManager;
import org.w3c.dom.Document;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import intextbooks.Configuration;
import intextbooks.ConfigurationLoader;
import intextbooks.SystemLogger;
import intextbooks.content.models.Book;
import intextbooks.content.models.BookStatus;
import intextbooks.content.models.formatting.CoordinatesContainer;
import intextbooks.content.models.formatting.FormattingContainer;
import intextbooks.content.models.formatting.FormattingDictionary;
import intextbooks.content.models.formatting.Page;
import intextbooks.content.models.formatting.PageMetadata;
import intextbooks.content.models.formatting.PageMetadataEnum;
import intextbooks.content.models.formatting.lists.ListingContainer;
import intextbooks.content.models.structure.Segment;
import intextbooks.ontologie.LanguageEnum;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

public class Persistence {
	private static Persistence instance = null;
	private static Disc disc;
	private static Database db;
	
	
	protected Persistence() {
		disc = Disc.getInstance();
		db = Database.getInstance();
	}
	
	public static Persistence getInstance() {
		if(instance == null) {
			instance = new Persistence();
		}
		return instance;
	}
	
	public boolean removeContent(String bookID, Set<String> bookList) throws SQLException, IOException{
		disc.removeBook(bookID);
		
		return db.removeBook(bookID, bookList);
	}

	/*********************************************************
	 ****************** DATABASE OPERATIONS ******************
	 ********************************************************* 
	 */
	
	public String getFileName(String bookID) {
		return db.getFileName(bookID);
	}
	
	public String getType(String bookID) {
		return db.getType(bookID);
	}
	
	public String getLanguage(String bookID) {		
		return db.getLanguage(bookID);
	}
	
	public String getBookName(String bookID) {
		return db.getBookName(bookID);
	}
	
	public ArrayList<String> getBookList() throws SQLException {
		return db.getBookList();
	}
	
	public ArrayList<String> getGroupings() throws SQLException {
		return db.getGroupList();
	}
	
	public String getGroupName(String groupID) {
		return db.getGroupingName(groupID);
	}
	
	public String getGroupLanguage(String groupID) {
		return db.getGroupingLanguage(groupID);
	}
	
	/*public ArrayList<String> getGroup(String groupID) throws SQLException {
		return db.getGroup(groupID);
	}*/
	
	public ArrayList<String> getGroupMembers(String groupID) throws SQLException {
		return db.getGroupMembers(groupID);
	}
	
	public void createGrouping(String id, String name, LanguageEnum language) {
		db.createGrouping(id,name, language);
	}

	public void createFormatMap(String parentBook) throws SQLException{
		db.createFormatMap(parentBook);
	}
	
	public void createRelationMap(String parentBook) throws SQLException {
		db.createRelationMap(parentBook);
	}

	public void createIndexMap(String parentBook) throws SQLException{
		db.createIndexMap(parentBook);
	}
	
	public void createHighlightMap(String parentBook) throws SQLException{
		db.createHighlightMap(parentBook);
	}
	
	public void createListingMap(String parentBook) throws SQLException{
		db.createListingMap(parentBook);
	}
	
	public void addListing(String bookID, ListingContainer listing) throws SQLException {
		db.addListing(bookID, listing);
	}
	
	public ArrayList<ListingContainer> getListingsInSegment(String bookID, int segmentID) throws SQLException {
		return db.getListingsInSegment(bookID, segmentID);
	}
	
	public ArrayList<ListingContainer> getListingsOnPage(String bookID, int pageIndex) throws SQLException {
		return db.getListingsOnPage(bookID, pageIndex);
	}
	
	public void addIndex(String bookID, String indexName, List<Integer> segments, ArrayList<Integer> indices) throws SQLException {
		db.addIndex(bookID, indexName, segments, indices);
	}
	
	public void addIndex(String bookID, String indexName, ArrayList<Integer> indices) throws SQLException {
		db.addIndex(bookID, indexName, indices);
	}
	
	public void addConceptToIndexTerm(String parentBook, String indexTerm,
			String conceptName) throws SQLException{
		 db.addConceptToIndexElement(parentBook, indexTerm, conceptName);
	}
	
	public void addConceptToIndexElement(String parentBook, String indexTerm,
			String conceptName) throws SQLException{
		 db.addConceptToIndexElement(parentBook, indexTerm, conceptName);
	}

	public ArrayList<String> getIndexTermsOnPage(String parentBook, int pageIndex)  throws SQLException {
		return db.getIndexTermsOnPage(parentBook, pageIndex);
		
	}
	
	public ArrayList<String> getIndexTermsOfSegment(String parentBook, int segmentID) throws SQLException {
		return db.getIndexTermsOfSegment(parentBook, segmentID);
	}
	
	public Set<String>[] getIndexEntriesOfSegment(String parentBook, int segmentID) throws SQLException {
		return db.getIndexEntriesOfSegment(parentBook, segmentID);
	}
	
	public ArrayList<Integer> getSegmentsIdOfIndexTerm(String bookID, String indexName) throws SQLException { 
		return db.getSegmentsIdOfIndexTerm(bookID, indexName);
	}
	
	public ArrayList<Integer> getOccurrencesOfIndex(String bookID, String indexName) throws SQLException {
		return db.getOccurrencesOfIndex(bookID, indexName);
	}
	
	public ArrayList<Integer> getOccurrencesOfIndexAsSegmentID(String bookID, String indexName) throws SQLException {
		return db.getSegmentsIdOfIndexTerm(bookID, indexName);
	}

	public ArrayList<Integer> getOccurrencesOfIndexByConcept(String bookID, String conceptName) throws SQLException {
		return db.getOccurrencesOfIndexByConcept(bookID, conceptName);
	}
	
	public String getConceptOfIndexTerm(String bookID, String indexTerm) throws SQLException {
		return db.getConceptOfIndexTerm(indexTerm, bookID);
	}
	
	public String getConceptOfIndexElement(String keyName, String bookID) throws SQLException {
		return db.getConceptOfIndexElement(keyName, bookID);
	}
	
	public List<String> getIndexTermsOfConcept(String bookID, String conceptName) throws SQLException {
		return db.getIndexTermsOfConcept(conceptName, bookID);
	}
	
	public List<Integer> getIndexConceptMappingStatistics(String bookID) throws SQLException {
		return db.getIndexConceptMappingStatistics(bookID);
	}
	
	public void updateConceptNameOfIndexElement(String bookID, String key, String conceptName) {
		db.updateConceptNameOfIndexElement(bookID, key, conceptName);
	}

	public ArrayList<String> getListOfIndicesForBook(String bookID) throws SQLException {
		return db.getListOfIndicesForBook(bookID);
	}
	
	public ArrayList<String> getListOfIndicesWithPageForBook(String bookID) throws SQLException {
		return db.getListOfIndicesWithPageForBook(bookID);
	}
	
	public List<String[]> getListOfIndicesWithPageForBookV2(String bookID) {
		return db.getListOfIndicesWithPageForBookV2(bookID);
	}
	
	public List<String> getListOfUsedConceptNames(String bookID) throws SQLException {
		return db.getListOfUsedConceptNames(bookID);
	}
	
	public List<String> getListOfIndexParts(String index_id) {
		return db.getListOfIndexParts(index_id);
	}
	
	public void createBook(String bookID, String fileName, String uri, LanguageEnum language, String bookName, String type, String group) throws SQLException{
		createBook(bookID, fileName, uri, language, bookName, "empty", type, group, null);
	} 
	
	public void createBook(String bookID, String fileName, String uri, LanguageEnum language, String bookName, String checksum, String type, String group, String senderEmail) throws SQLException{
		db.createBook(bookID, fileName, uri, language, bookName, checksum, type, group, senderEmail);
	} 
	
	public void updateBookStatus(String bookID, BookStatus status){
		try {
			db.updateBookStatus(bookID, status);
		} catch (Exception e) {
			e.printStackTrace();
		}
	} 
	
	public boolean doesRelationExist(String parentBook, Integer segmentID, String referedBookID, Integer referedSegment) throws SQLException {
		return db.doesRelationExist(parentBook, segmentID, referedBookID, referedSegment);
	}	
	
	public Integer getWordFormatting(String parentBook, int pageNumber, int lineNumber, int wordPos) {
		return db.getWordFormatting(parentBook, pageNumber, lineNumber, wordPos);
	}
	
	public ArrayList<Integer> getFormatKeysInLine(String parentBook, int pageNumber, int lineNumber) {
		return db.getFormatKeysInLine(parentBook, pageNumber, lineNumber);
	}
	
	public CoordinatesContainer getWordCoordinates(String parentBook,int pageNumber, int lineNumber, int wordPos) {
		return db.getWordCoordinates(parentBook, pageNumber, lineNumber, wordPos);
	}
	
	public ArrayList<Integer> getRelatedSegmentsInBook(String slideID, int segmentID, String bookID) {
		return db.getRelatedSegmentsInBook(slideID, segmentID, bookID);
	}
	
	public HashMap<Integer, ArrayList<Integer>> getSemanticMatchingSegments(String sourceBookID, String targetBookID) {
		return db.getSemanticMatchingSegments(sourceBookID, targetBookID);
	}
	
	public ArrayList<String> getAggregatedConcepts(String sourceBookID, ArrayList<Integer> allSegments) {
		return db.getAggregatedConcepts(sourceBookID, allSegments);
	}
	
	public ArrayList<Pair<String,Integer>> getAggregatedConceptsWithFreq(String sourceBookID, ArrayList<Integer> allSegments) {
		return db.getAggregatedConceptsWithFreq(sourceBookID, allSegments);
	}
	
	//new Index operations
	public int addIndexCatalogEntry(String content_id, Integer parent_id, String key, String label, boolean full_name, boolean artificial) {
		return db.addIndexCatalogEntry(content_id, parent_id, key, label, full_name, artificial);
	}
	
	public void addIndexLocationEntry(Integer index_id, Integer page_index, Integer page_number, Integer segment) {
		db.addIndexLocationEntry(index_id, page_index, page_number, segment);
	}
	
	public void addIndexNounEntry(Integer index_id, String noun_text) {
		db.addIndexNounEntry(index_id, noun_text);
	}
	
	public void addIndexPartEntry(Integer index_id, String part_text) {
		db.addIndexPartEntry(index_id, part_text);
	}
	
	public void addIndexSentenceEntry(Integer index_id, Integer page_index, String sentence_text) {
		db.addIndexSentenceEntry(index_id, page_index, sentence_text);
	}
	
	public Map<Integer, Set<String>> getPagesAndSentecesForIndex(String content_id) {
		return db.getPagesAndSentecesForIndex(content_id);
	}
	
	public void addIndexElement(String bookID, String indexName,List<Integer> segments, List<Integer> indices, List<Integer> pages, boolean artificial) {
		db.addIndexElement(bookID, indexName, segments, indices, pages, artificial);
	}
	
	/*------------ USER MANAGEMENT ---------- */
	public void createUser(String userName, String password,LanguageEnum originLang, LanguageEnum targetLang,
			String email, String gender, int age, String majorSubject) {
		
		db.createUser(userName, password, originLang, targetLang, email, gender, age, majorSubject);
	}
	
	public void editUser(String userName, LanguageEnum originLang, LanguageEnum targetLang,
			String email, String gender, int age, String majorSubject) {
		db.editUser(userName, originLang, targetLang, email, gender, age, majorSubject);
	}

	
	public ArrayList<String> getAccountList() {
		return db.getAccountList();
	}
	
	public HashMap<String, String> loadUser(String userName) {
		return db.loadUser(userName);
	}
	
	public boolean dropAccount(String login) {
		return db.dropUserAccount(login);
	}
	
	public void logUserActivity(String userName, String timestamp,
			String action, String object, String book, String description) {
			db.createUserLogEntry(userName, timestamp, action, object, book, description);
	}
	
	/*------------ ASSESSMENT MANAGEMENT ---------- */
	public void createTestItem(LanguageEnum lang, String concept, String label) {
		db.createTestItem(lang, concept, label);
	}
	

	/*********************************************************
	 ******************** DISC OPERATIONS ********************
	 ********************************************************* 
	 */
	public File checkFolder(String path) {
		return disc.checkFolder(path);
		
	}
	
	public void addPageFormatting(String parentBook, String pageIndex, 
			ArrayList<ArrayList<Integer>> formatMap, ArrayList<ArrayList<CoordinatesContainer>> coordMap) throws Exception{
		disc.addPageFormatting(parentBook, pageIndex, formatMap, coordMap);
	}
	
	public void storeEnrichedModel(String parentBook, SKOSDataset model) {
		disc.storeEnrichedModel(parentBook, model);
	}
	
	public void storeFormatModel(String parentBook, HashMap<String, Page> pageMap, FormattingDictionary dict) {
		disc.storeFormatModel(parentBook, pageMap, dict);
	}

	public void storeStructureModel(String parentBook, Segment hierarchy, int indexCounter, int maxDepth, SKOSManager skosModel){
		disc.storeStructureModel(parentBook, hierarchy, indexCounter, maxDepth, skosModel);
	}
	
	public void storeTEIModel(String parentBook,Document teiModel){
		disc.storeTEIModel(parentBook, teiModel);
	}

	public FormattingDictionary loadFormatDictionary(String bookID) {
		return disc.loadFormatDictionary(bookID);
	}

	public HashMap<String,Page> loadPageMap(String bookID) throws Exception {
		return disc.loadPageMap(bookID);		
	}

	public SKOSManager loadStructureModel(String bookID) {
		return disc.loadStructureModel(bookID);
	}
	
	public Segment loadStructure(String bookID) {
		return disc.loadStructure(bookID);
	}
	
	public File loadParagraphFile(String bookID, int index) {
		return disc.loadParagraphFile(bookID, index);
	}
	
	public int[] loadCounters(String bookID) {
		return disc.loadCounters(bookID);
	}

	public void storeSegment(String bookID, int index, String content){
		disc.storeSegment(bookID, index, content);
	}
	
	public void storeExtractonTempInfo(String bookID, String fileName, String content){		
		disc.storeExtractonTempInfo(bookID, fileName, content);
	}
	
	public void storeContentFilePersonalized(String filePath, String fileName, String content){		
		disc.storeContentFilePersonalized(filePath, fileName, content);
	}

	public String storeContentFile(String fileName, FileItem item) throws IOException {
		return disc.storeContentFile(fileName, item);
	}

	public void performPDFsplit(String parentBook, String parentBookFileName, Segment segment) {
		disc.performPDFsplit(parentBook, parentBookFileName, segment);
	}
	
	public void performCleanPDFsplit(String parentBook, String parentBookFileName, Segment segment) {
		disc.performCleanPDFsplit(parentBook, parentBookFileName, segment);
	}

	public String performPDFsplit(String parentBookID,
			String parentBookFileName, Segment segment, String userName, LanguageEnum userLang, HashMap<String, Boolean> explainPresent) {
		return 	disc.performPDFsplit(parentBookID, parentBookFileName, segment, userName, userLang,explainPresent);
	}
    
	public void clearUserPDFfolder(String userName) {
		
		disc.clearUserPDFfolder(userName);
	}

	
	
	/*********************************************************
	 ****************** ONTOLOGY OPERATIONS ******************
	 ********************************************************* 
	 */

    
    public int getOntologyVersion() throws SQLException {
		return db.getOntologyVersion();
    }
    
    public void versionizeOntology() throws SQLException {
    	db.versionizeOntology();
    }

	public Element getStructureXML(String bookID) {
		return disc.getStructureXML(bookID);
	}

	public void removePersonalizedPDF(String userName, String bookID) {
		disc.removePersonalizedPDF(userName, bookID);
		
	}

	
	/*********************************************************
	 ****************** ASSESSMENTS ******************
	 ********************************************************* 
	 */
	
	
	public void createAssessmentRun(String testId, String userName,
			String timestamp, String sourceLanguage, String targetLanguage) {
		db.createAssessmentRun(testId, userName, timestamp, sourceLanguage, targetLanguage);	
		
	}

	public void createAssessmentRunQuestion(String testId, String questionId,
			String userName, String type, String sourceLanguage, String targetLanguage) {
		db.createAssessmentRunQuestion(testId, questionId, userName, type, sourceLanguage, targetLanguage);
		
	}

	public void createAssessmentRunItem(String testId, String questionId,
			String concept, String label) {
		db.createAssessmentRunItem(testId, questionId, concept, label);
		
	}

	public void createAssessmentRunSelectedItem(String testId,
			String questionId, String concept, String label, String correct) {
		db.createAssessmentRunSelectedItem(testId, questionId, concept, label, correct);
		
	}

	public void createAssessmentRunCorrectItem(String testId,
			String questionId, String concept, String label) {
		db.createAssessmentRunCorrectItem(testId, questionId, concept, label);
		
	}

	public ArrayList<String> getAssessmentRuns(String userName) {
		return db.getAssessmentRuns(userName);
	}

	public ArrayList<String> getQuestionsOfRun(String runId) {
		return db.getQuestionsOfRun(runId);
	}

	public ArrayList<String> getCorrectAnsweredItemsInRun(String questionId) {
		return db.getSelectedItemsInRun(questionId, true);
	}
	
	public ArrayList<String> getIncorrectAnsweredItemsInRun(String questionId) {
		return db.getSelectedItemsInRun(questionId, false);
	}
	
	public ArrayList<String[]> getConceptsInQuestion(String runId){
		return db.getItemsInQuestion(runId);
	}



	



	

	

	
	

	
	

	

	

	



	

	
	
	

	
}
