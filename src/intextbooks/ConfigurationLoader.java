package intextbooks;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import intextbooks.ontologie.LanguageEnum;
import intextbooks.ontologie.Mediator;

public class ConfigurationLoader {

	private Document configDoc;
	private String configPath = "/config.xml";
	
	private String contentFolder;
	private String modelFolder;
	private String ontologyNS;
	private String ontologyPath;
	private String lineSeparator;
	private String extractionTmpFolder;
	private String extractedSegmentsTXT;
	private String extractedSegmentsPDF;
	private boolean silentLogging;
	private String logFolder;
	private String userLogFolder;
	private String dbURL;
	private String dbUser;
	private String dbPasswd;
	private String modelFolderLT;
	private String customAnnotationIcon;
	private String uploadPwd;
	private String repositoryURL;
	private String linkingFolder;	
	
	private String luceneVersion;	
	private int assumedMaxDocumentCount;
	private boolean logLinkingProcess;
	private int VsmCollectionSimilarityMaximumListedMatchingDocuments;
	private String nameDividorSymbol;
	private String markAsIntroductionString;
	private int fileNameHashingReadablePartLength;
//<!-- Directories -->
	private String linkingInputDirectory;
	private String linkingOutputDirectory;
	private String documentsDirectory;
	private String glossariesDirectory;
	private String resultsDirectory;
	private String indicesDirectory;
	private String documentVsmCollectionsDirectory;
	private String collectionSimilaritiesDirectory;
	private String humanFeedbackDataDirectory;
	private String evaluationResultsDirectory;
	private String basicDocumentsDirectory;
	private String testDocumentsDirectory;
	private String unorderedDocumentsDirectory;
	private String testDataDirectory;
	private String testIndicesDirectory;
	private String testDocumentVsmCollectionsDirectory;
	private String testCollectionSimilaritiesDirectory;
	private boolean alsoCreateHumanReadableVersionOfFiles;
	private String glossaryEffectivenessTest;
	private String indexVsGlossary;
	private String glossaryIndexOverlap;
	private int collectionSimilaritySanityTestNumberOfSvdDimensions;
	private LanguageEnum basicIndexingTestLanguage;
	private int occurrenceCountForIntroduction;
	
	private double findAllBodyWeight;
	private double findAllTitleWeight;
	private double findAnyBodyWeight;
	private double findAnyTitleWeight;
	private double phraseBodyWeight;
	private double phraseTitleWeight;
	
	private double glossaryIndexBodyWeight;
	private double glossaryIndexTitleWeight;
	
	private double termIntroductionWeight;
	
	private String englishPosModel;
	private String germanPosModel;
	private String frenchPosModel;
	private String spanishPosModel;
	private String dutchPosModel;
	
	private String englishNERModel;
	private String germanNERModel;
	private String frenchNERModel;
	private String spanishNERModel;
	private String dutchNERModel;
	private String NERTags;
	
	private String englishTexPattern;
	private String germanTexPattern;
	private String frenchTexPattern;
	private String spanishTexPattern;
	private String dutchTexPattern;
	
	private String ontologyBlacklistsPath;
	private String usedOntologyBlacklist;
	
	private String pythonScriptPath;
	
	private String tdbDirectory;
	
	private String jepLibraryPath;
	
	private int numberOfJumps;
	private double similarityScoreThreshold;
	
	/**
	 * @param args
	 */
	public ConfigurationLoader() {
		try {
		 this.configDoc = loadXML();
		 if(this.configDoc == null)
			 System.out.println("Doc is null");
		} catch (Exception e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			System.out.println("Unable to load configuration file");
		}

	}
	
	private Document loadXML() throws ParserConfigurationException, SAXException, IOException{
	
		InputStream xmlFileStream = ConfigurationLoader.class.getResourceAsStream(this.configPath);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(xmlFileStream);
		
		doc.getDocumentElement().normalize();
		 
		
		loadContentFolder(doc);
		loadFormatModelFolder(doc);
		loadOntologyPath(doc);
		loadOntologyNS(doc);
		loadLineSeperator(doc);
		loadExtractionTmpFolder(doc);
		loadExtractedSegmentsTXTFolder(doc);
		loadExtractedSegmentsPDFFolder(doc);
		loadSilentLog(doc);
		loadLogFolder(doc);
		loadUserLogFolder(doc);
		loadDbUrl(doc);
		loadDbUser(doc);
		loadDbPasswd(doc);
		loadModelFolderLT(doc);
		loadCustomAnnotationIcon(doc);
		loadUploadPwd(doc);
		loadRepositoryURL(doc);
		loadLinkingFolder(doc);
		loadAssumedMaxDocumentCount(doc);
		loadBasicDocumentsDirectory(doc);
		loadCollectionSimilaritiesDirectory(doc);
		loadDocumentsDirectory(doc);
		loadDocumentVsmCollectionsDirectory(doc);
		loadEvaluationResultsDirectory(doc);
		loadfFileNameHashingReadablePartLength(doc);
		loadGlossariesDirectory(doc);
		loadHumanFeedbackDataDirectory(doc);
		loadIndicesDirectory(doc);
		loadLinkingInputDirectory(doc);
		loadLinkingOutputDirectory(doc);
		loadLogLinkingProcess(doc);
		loadLuceneVersion(doc);
		loadMarkAsIntroductionString(doc);
		loadNameDividorSymbol(doc);
		loadResultsDirectory(doc);
		loadVsmCollectionSimilarityMaximumListedMatchingDocuments(doc);
		loadUnorderedDocumentsDirectory(doc);
		loadTestDocumentsDirectory(doc);
		loadTestDataDirectory(doc);
		loadTestIndicesDirectory(doc);
		loadTestCollectionSimilaritiesDirectory(doc);
		loadTestDocumentVsmCollectionsDirectory(doc);
		loadAlsoCreateHumanReadableVersionOfFiles(doc);
		loadGlossaryEffectivenessTest(doc);
		loadIndexVsGlossary(doc);
		loadGlossaryIndexOverlap(doc);
		loadCollectionSimilaritySanityTestNumberOfSvdDimensions(doc);
		loadBasicIndexingTestLanguage(doc);
		loadOccurrenceCountForIntroduction(doc);
		loadFindAllBodyWeight(doc);
		loadFindAllTitleWeight(doc);
		loadFindAnyBodyWeight(doc);
		loadFindAnyTitleWeight(doc);
		loadPhraseBodyWeight(doc);
		loadPhraseTitleWeight(doc);
		loadTermIntroductionWeight(doc);
		loadEnglishPosModel(doc);
		loadGermanPosModel(doc);
		loadFrenchPosModel(doc);
		loadSpanishPosModel(doc);
		loadDutchPosModel(doc);
		loadEnglishNERModel(doc);
		loadGermanNERModel(doc);
		loadFrenchNERModel(doc);
		loadSpanishNERModel(doc);
		loadDutchNERModel(doc);
		loadNERTags(doc);
		loadOntologyBlacklistsPath(doc);
		loadUsedOntologyBlacklist(doc);
		loadPythonScriptPathPath(doc);
		loadTdbDirectory(doc);
		loadJepLibraryPath(doc);
		loadEnglishTexPattern(doc);
		loadGermanTexPattern(doc);
		loadFrenchTexPattern(doc);
		loadSpanishTexPattern(doc);
		loadDutchTexPattern(doc);
		loadNumberOfJumps(doc);
		loadSimilarityScoreThreshold(doc);
		return doc;
	}
	
	private void loadNumberOfJumps(Document doc) {
		NodeList weight = doc.getElementsByTagName("numberOfJumps");
		for (int iter=0;iter<weight.getLength();iter++)
			this.numberOfJumps = Integer.parseInt(weight.item(iter).getTextContent());
	}
	
	private void loadSimilarityScoreThreshold(Document doc) {
		NodeList weight = doc.getElementsByTagName("similarityScoreThreshold");
		for (int iter=0;iter<weight.getLength();iter++)
			this.similarityScoreThreshold = Double.parseDouble(weight.item(iter).getTextContent());
	}
	
	private void loadEnglishTexPattern(Document doc) {
		NodeList model = doc.getElementsByTagName("englishTexPattern");
		for(int iter=0;iter<model.getLength();iter++)
			this.englishTexPattern = model.item(iter).getTextContent();
	}
	
	private void loadGermanTexPattern(Document doc) {
		NodeList model = doc.getElementsByTagName("germanTexPattern");
		for(int iter=0;iter<model.getLength();iter++)
			this.germanTexPattern = model.item(iter).getTextContent();
	}
	
	private void loadFrenchTexPattern(Document doc) {
		NodeList model = doc.getElementsByTagName("frenchTexPattern");
		for(int iter=0;iter<model.getLength();iter++)
			this.frenchTexPattern = model.item(iter).getTextContent();
	}
	
	private void loadSpanishTexPattern(Document doc) {
		NodeList model = doc.getElementsByTagName("spanishTexPattern");
		for(int iter=0;iter<model.getLength();iter++)
			this.spanishTexPattern = model.item(iter).getTextContent();
	}
	
	private void loadDutchTexPattern(Document doc) {
		NodeList model = doc.getElementsByTagName("dutchTexPattern");
		for(int iter=0;iter<model.getLength();iter++)
			this.dutchTexPattern = model.item(iter).getTextContent();
	}
	
	
	private void loadPythonScriptPathPath(Document doc) {
		NodeList path = doc.getElementsByTagName("pythonScriptPath");
		for(int iter=0;iter<path.getLength();iter++)
			this.pythonScriptPath = path.item(iter).getTextContent();
	}
	
	private void loadTdbDirectory(Document doc) {
		NodeList path = doc.getElementsByTagName("tdbDirectory");
		for(int iter=0;iter<path.getLength();iter++)
			this.tdbDirectory = path.item(iter).getTextContent();
	}
	
	private void loadJepLibraryPath(Document doc) {
		NodeList path = doc.getElementsByTagName("jepLibraryPath");
		for(int iter=0;iter<path.getLength();iter++)
			this.jepLibraryPath = path.item(iter).getTextContent();
	}

	private void loadOntologyBlacklistsPath(Document doc) {
		NodeList path = doc.getElementsByTagName("ontologyBlacklistsPath");
		for(int iter=0;iter<path.getLength();iter++)
			this.ontologyBlacklistsPath = path.item(iter).getTextContent();
	}
	
	private void loadUsedOntologyBlacklist(Document doc) {
		NodeList list = doc.getElementsByTagName("usedOntologyBlacklist");
		for(int iter=0;iter<list.getLength();iter++)
			this.usedOntologyBlacklist = list.item(iter).getTextContent();
	}
	
	private void loadDutchPosModel(Document doc) {
		NodeList model = doc.getElementsByTagName("dutchPosModel");
		for(int iter=0;iter<model.getLength();iter++)
			this.dutchPosModel = model.item(iter).getTextContent();
	}
	
	private void loadSpanishPosModel(Document doc) {
		NodeList model = doc.getElementsByTagName("spanishPosModel");
		for(int iter=0;iter<model.getLength();iter++)
			this.spanishPosModel = model.item(iter).getTextContent();
	}

	private void loadFrenchPosModel(Document doc) {
		NodeList model = doc.getElementsByTagName("frenchPosModel");
		for(int iter=0;iter<model.getLength();iter++)
			this.frenchPosModel = model.item(iter).getTextContent();
	}

	private void loadGermanPosModel(Document doc) {
		NodeList model = doc.getElementsByTagName("germanPosModel");
		for(int iter=0;iter<model.getLength();iter++)
			this.germanPosModel = model.item(iter).getTextContent();
	}

	private void loadEnglishPosModel(Document doc) {
		NodeList model = doc.getElementsByTagName("englishPosModel");
		for(int iter=0;iter<model.getLength();iter++)
			this.englishPosModel = model.item(iter).getTextContent();
	}
	
	private void loadDutchNERModel(Document doc) {
		NodeList model = doc.getElementsByTagName("dutchNERModel");
		for(int iter=0;iter<model.getLength();iter++)
			this.dutchNERModel = model.item(iter).getTextContent();
	}
	
	private void loadSpanishNERModel(Document doc) {
		NodeList model = doc.getElementsByTagName("spanishNERModel");
		for(int iter=0;iter<model.getLength();iter++)
			this.spanishNERModel = model.item(iter).getTextContent();
	}

	private void loadFrenchNERModel(Document doc) {
		NodeList model = doc.getElementsByTagName("frenchNERModel");
		for(int iter=0;iter<model.getLength();iter++)
			this.frenchNERModel = model.item(iter).getTextContent();
	}

	private void loadGermanNERModel(Document doc) {
		NodeList model = doc.getElementsByTagName("germanNERModel");
		for(int iter=0;iter<model.getLength();iter++)
			this.germanNERModel = model.item(iter).getTextContent();
	}

	private void loadEnglishNERModel(Document doc) {
		NodeList model = doc.getElementsByTagName("englishNERModel");
		for(int iter=0;iter<model.getLength();iter++)
			this.englishNERModel = model.item(iter).getTextContent();
	}
	
	private void loadNERTags(Document doc) {
		NodeList model = doc.getElementsByTagName("NERTags");
		for(int iter=0;iter<model.getLength();iter++)
			this.NERTags = model.item(iter).getTextContent();
	}

	private void loadTermIntroductionWeight(Document doc) {
		NodeList weight = doc.getElementsByTagName("termIntroductionWeight");
		for(int iter=0;iter<weight.getLength();iter++)
			this.termIntroductionWeight = Double.parseDouble(weight.item(iter).getTextContent());
	}

	private void loadLinkingFolder(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("linkingFolder");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.linkingFolder = linkingFolderNode.item(iter).getTextContent();
		
	}

	private void loadUploadPwd(Document doc) {
		NodeList uploadPwd = doc.getElementsByTagName("uploadPwd");
		for(int iter=0;iter<uploadPwd.getLength();iter++)
			this.uploadPwd = uploadPwd.item(iter).getTextContent();
		
	}
	
	private void loadRepositoryURL(Document doc) {
		NodeList repositoryURL = doc.getElementsByTagName("repositoryURL");
		for(int iter=0;iter<repositoryURL.getLength();iter++)
			this.repositoryURL = repositoryURL.item(iter).getTextContent();
		
	}

	private void loadOntologyNS(Document doc) {
		NodeList interlingaNS = doc.getElementsByTagName("ontologyNS");
		for(int iter=0;iter<interlingaNS.getLength();iter++)
			this.ontologyNS = interlingaNS.item(iter).getTextContent();
		
	}

	private void loadOntologyPath(Document doc) {
		NodeList ontologyPath = doc.getElementsByTagName("ontologyPath");
		for(int iter=0;iter<ontologyPath.getLength();iter++)
			this.ontologyPath = ontologyPath.item(iter).getTextContent();		
		
	}

	private void loadContentFolder(Document doc) {
		NodeList uploadFolders = doc.getElementsByTagName("contentFolder");
		for(int iter=0;iter<uploadFolders.getLength();iter++)
			this.contentFolder = uploadFolders.item(iter).getTextContent();
		
	}
	
	private void loadFormatModelFolder(Document doc) {
		NodeList formatModelFolders = doc.getElementsByTagName("modelFolder");
		for(int iter=0;iter<formatModelFolders.getLength();iter++)
			this.modelFolder = formatModelFolders.item(iter).getTextContent();
		
	}
	
	private void loadLineSeperator(Document doc) {
		NodeList lineSperatorNodes = doc.getElementsByTagName("lineSeparator");
		for(int iter=0;iter<lineSperatorNodes.getLength();iter++)
			this.lineSeparator = lineSperatorNodes.item(iter).getTextContent();
		
	}
	
	private void loadExtractionTmpFolder(Document doc) {
		NodeList exractionTmpFolderNodes = doc.getElementsByTagName("extractionTmpFolder");
		for(int iter=0;iter<exractionTmpFolderNodes.getLength();iter++)
			this.extractionTmpFolder = exractionTmpFolderNodes.item(iter).getTextContent();
		
	}
	
	private void loadExtractedSegmentsTXTFolder(Document doc) {
		NodeList extractedSegmentsNodes = doc.getElementsByTagName("extractedSegmentsTXT");
		for(int iter=0;iter<extractedSegmentsNodes.getLength();iter++)
			this.extractedSegmentsTXT = extractedSegmentsNodes.item(iter).getTextContent();
		
	}
	
	private void loadExtractedSegmentsPDFFolder(Document doc) {
		NodeList extractedSegmentsNodes = doc.getElementsByTagName("extractedSegmentsPDF");
		for(int iter=0;iter<extractedSegmentsNodes.getLength();iter++)
			this.extractedSegmentsPDF = extractedSegmentsNodes.item(iter).getTextContent();
		
	}
	
	private void loadLogFolder(Document doc) {
		NodeList logFolderNodes = doc.getElementsByTagName("logFolder");
		for(int iter=0;iter<logFolderNodes.getLength();iter++)
			this.logFolder = logFolderNodes.item(iter).getTextContent();
		
	}
	
	private void loadUserLogFolder(Document doc) {
		NodeList logFolderNodes = doc.getElementsByTagName("userLogFolder");
		for(int iter=0;iter<logFolderNodes.getLength();iter++)
			this.userLogFolder = logFolderNodes.item(iter).getTextContent();
		
	}
	
	private void loadDbUrl(Document doc) {
		NodeList dbNodes = doc.getElementsByTagName("database");
		for(int iter=0;iter<dbNodes.getLength();iter++)
			this.dbURL = dbNodes.item(iter).getTextContent();
		
	}
	
	private void loadDbUser(Document doc) {
		NodeList dbNodes = doc.getElementsByTagName("dbUser");
		for(int iter=0;iter<dbNodes.getLength();iter++)
			this.dbUser = dbNodes.item(iter).getTextContent();
		
	}
	
	private void loadDbPasswd(Document doc) {
		NodeList dbNodes = doc.getElementsByTagName("dbPasswd");
		for(int iter=0;iter<dbNodes.getLength();iter++)
			this.dbPasswd = dbNodes.item(iter).getTextContent();
		
	}
	
	
	private void loadSilentLog(Document doc) {
		NodeList silentLogNodes = doc.getElementsByTagName("silentLogging");
		for(int iter=0;iter<silentLogNodes.getLength();iter++) {
			this.silentLogging = Boolean.parseBoolean(silentLogNodes.item(iter).getTextContent());
		}
	}
	
	private void loadModelFolderLT(Document doc) {
		NodeList modelLTNodes = doc.getElementsByTagName("modelFolderLinkingTests");
		for(int iter=0;iter<modelLTNodes.getLength();iter++)
			this.modelFolderLT = modelLTNodes.item(iter).getTextContent();
		
	}
	
	private void loadCustomAnnotationIcon(Document doc){
		NodeList cAnnoNodes = doc.getElementsByTagName("customAnnotationIcon");
		for(int iter=0;iter<cAnnoNodes.getLength();iter++)
			this.customAnnotationIcon = cAnnoNodes.item(iter).getTextContent();
		
	}

	private void loadLuceneVersion(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("luceneVersion");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.linkingFolder = linkingFolderNode.item(iter).getTextContent();
		
	}
	
	private void loadAssumedMaxDocumentCount(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("assumedMaxDocumentCount");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.assumedMaxDocumentCount = Integer.parseInt(linkingFolderNode.item(iter).getTextContent());	
	}
	
	private void loadVsmCollectionSimilarityMaximumListedMatchingDocuments(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("VsmCollectionSimilarityMaximumListedMatchingDocuments");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.VsmCollectionSimilarityMaximumListedMatchingDocuments = Integer.parseInt(linkingFolderNode.item(iter).getTextContent());	
	}
	
	private void loadNameDividorSymbol(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("nameDividorSymbol");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.nameDividorSymbol = linkingFolderNode.item(iter).getTextContent();	
	}
	
	private void loadMarkAsIntroductionString(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("markAsIntroductionString");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.markAsIntroductionString = linkingFolderNode.item(iter).getTextContent();	
	}
	
	private void loadfFileNameHashingReadablePartLength(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("fileNameHashingReadablePartLength");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.fileNameHashingReadablePartLength = Integer.parseInt(linkingFolderNode.item(iter).getTextContent());	
	}
	
	private void loadLinkingInputDirectory(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("linkingInputDirectory");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.linkingInputDirectory = linkingFolderNode.item(iter).getTextContent();	
	}
	
	private void loadLinkingOutputDirectory(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("linkingOutputDirectory");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.linkingOutputDirectory = linkingFolderNode.item(iter).getTextContent();	
	}
	
	private void loadDocumentsDirectory(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("documentsDirectory");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.documentsDirectory = linkingFolderNode.item(iter).getTextContent();	
	}
	
	private void loadGlossariesDirectory(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("glossariesDirectory");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.glossariesDirectory = linkingFolderNode.item(iter).getTextContent();	
	}
	
	private void loadResultsDirectory(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("resultsDirectory");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.resultsDirectory = linkingFolderNode.item(iter).getTextContent();	
	}
	
	private void loadIndicesDirectory(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("indicesDirectory");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.indicesDirectory = linkingFolderNode.item(iter).getTextContent();	
	}
	
	private void loadLogLinkingProcess(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("logLinkingProcess");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.logLinkingProcess =Boolean.parseBoolean( linkingFolderNode.item(iter).getTextContent());	
	}

	private void loadDocumentVsmCollectionsDirectory(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("documentVsmCollectionsDirectory");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.documentVsmCollectionsDirectory = linkingFolderNode.item(iter).getTextContent();	
	}
	
	private void loadCollectionSimilaritiesDirectory(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("collectionSimilaritiesDirectory");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.collectionSimilaritiesDirectory = linkingFolderNode.item(iter).getTextContent();	
	}
	
	private void loadHumanFeedbackDataDirectory(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("humanFeedbackDataDirectory");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.humanFeedbackDataDirectory = linkingFolderNode.item(iter).getTextContent();	
	}
	
	private void loadEvaluationResultsDirectory(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("evaluationResultsDirectory");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.evaluationResultsDirectory = linkingFolderNode.item(iter).getTextContent();	
	}
	
	private void loadBasicDocumentsDirectory(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("basicDocumentsDirectory");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.basicDocumentsDirectory = linkingFolderNode.item(iter).getTextContent();	
	}
	
	private void loadTestDocumentsDirectory(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("testDocumentsDirectory");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.testDocumentsDirectory = linkingFolderNode.item(iter).getTextContent();	
	}

	private void loadTestDataDirectory(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("testDataDirectory");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.testDataDirectory = linkingFolderNode.item(iter).getTextContent();	
	}
	
	private void loadUnorderedDocumentsDirectory(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("unorderedDocumentsDirectory");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.unorderedDocumentsDirectory = linkingFolderNode.item(iter).getTextContent();	
	}
	
	private void loadTestIndicesDirectory(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("testIndicesDirectory");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.testIndicesDirectory = linkingFolderNode.item(iter).getTextContent();	
	}
	
	private void loadTestDocumentVsmCollectionsDirectory(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("testDocumentVsmCollectionsDirectory");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.testDocumentVsmCollectionsDirectory = linkingFolderNode.item(iter).getTextContent();	
	}
	
	private void loadTestCollectionSimilaritiesDirectory(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("testCollectionSimilaritiesDirectory");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.testCollectionSimilaritiesDirectory = linkingFolderNode.item(iter).getTextContent();	
	}
	
	private void loadAlsoCreateHumanReadableVersionOfFiles(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("alsoCreateHumanReadableVersionOfFiles");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.alsoCreateHumanReadableVersionOfFiles = Boolean.parseBoolean(linkingFolderNode.item(iter).getTextContent());
		
	}
	
	private void loadGlossaryEffectivenessTest(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("glossaryEffectivenessTest");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.glossaryEffectivenessTest = linkingFolderNode.item(iter).getTextContent();
		
	}

	private void loadIndexVsGlossary(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("indexVsGlossary");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.indexVsGlossary = linkingFolderNode.item(iter).getTextContent();
		
	}
	
	private void loadGlossaryIndexOverlap(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("glossaryIndexOverlap");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.glossaryIndexOverlap = linkingFolderNode.item(iter).getTextContent();
	}


	private void loadCollectionSimilaritySanityTestNumberOfSvdDimensions(
			Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("collectionSimilaritySanityTestNumberOfSvdDimensions");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.collectionSimilaritySanityTestNumberOfSvdDimensions = Integer.parseInt(linkingFolderNode.item(iter).getTextContent());		
	}
	
	private void loadBasicIndexingTestLanguage(Document doc) {
		NodeList linkingFolderNode = doc.getElementsByTagName("basicIndexingTestLanguage");
		for(int iter=0;iter<linkingFolderNode.getLength();iter++)
			this.basicIndexingTestLanguage = LanguageEnum.valueOf(linkingFolderNode.item(iter).getTextContent());		
	}
	
	private void loadOccurrenceCountForIntroduction(Document doc) {
		NodeList count = doc.getElementsByTagName("occurrenceCountForIntroduction");
		for(int iter=0;iter<count.getLength();iter++)
			this.occurrenceCountForIntroduction = Integer.parseInt(count.item(iter).getTextContent());
	}
	
	private void loadPhraseTitleWeight(Document doc) {
		NodeList weight = doc.getElementsByTagName("phraseTitleWeight");
		for(int iter=0;iter<weight.getLength();iter++)
			this.phraseTitleWeight = Double.parseDouble(weight.item(iter).getTextContent());
	}

	private void loadPhraseBodyWeight(Document doc) {
		NodeList weight = doc.getElementsByTagName("phraseBodyWeight");
		for(int iter=0;iter<weight.getLength();iter++)
			this.phraseBodyWeight = Double.parseDouble(weight.item(iter).getTextContent());
	}

	private void loadFindAnyTitleWeight(Document doc) {
		NodeList weight = doc.getElementsByTagName("findAnyTitleWeight");
		for(int iter=0;iter<weight.getLength();iter++)
			this.findAnyTitleWeight = Double.parseDouble(weight.item(iter).getTextContent());
	}

	private void loadFindAnyBodyWeight(Document doc) {
		NodeList weight = doc.getElementsByTagName("findAnyBodyWeight");
		for(int iter=0;iter<weight.getLength();iter++)
			this.findAnyBodyWeight = Double.parseDouble(weight.item(iter).getTextContent());
	}

	private void loadFindAllTitleWeight(Document doc) {
		NodeList weight = doc.getElementsByTagName("findAllTitleWeight");
		for(int iter=0;iter<weight.getLength();iter++)
			this.findAllTitleWeight = Double.parseDouble(weight.item(iter).getTextContent());
	}

	private void loadFindAllBodyWeight(Document doc) {
		NodeList weight = doc.getElementsByTagName("findAllBodyWeight");
		for(int iter=0;iter<weight.getLength();iter++)
			this.findAllBodyWeight = Double.parseDouble(weight.item(iter).getTextContent());
	}
	
	private void loadGlossaryIndexBodyWeight(Document doc) {
		NodeList weight = doc.getElementsByTagName("glossaryIndexBodyWeight");
		for (int iter=0;iter<weight.getLength();iter++)
			this.glossaryIndexBodyWeight = Double.parseDouble(weight.item(iter).getTextContent());
	}
	
	private void loadGlossaryIndexTitleWeight(Document doc) {
		NodeList weight = doc.getElementsByTagName("glossaryIndexTitleWeight");
		for (int iter=0;iter<weight.getLength();iter++)
			this.glossaryIndexTitleWeight = Double.parseDouble(weight.item(iter).getTextContent());
	}

	
	public String getOntologyNS() {
		return this.ontologyNS;
	}

	public String getOntologyPath() {
		return this.ontologyPath;
	}

	public String getUploadFolder() {
		return this.contentFolder;
	}
	
	public String getModelFolder() {
		return this.modelFolder;
	}
	
	public String getLineSeparator() {
		return this.lineSeparator;
	}
	
	public String getExtrationTmpFolder(){
		return this.extractionTmpFolder;
	}
	
	public String getExtractedSegmentsFolderTXT(){
		return this.extractedSegmentsTXT;
	}
	
	public String getExtractedSegmentsFolderPDF(){
		return this.extractedSegmentsPDF;
	}

	public boolean isSilentLogging() {
		return this.silentLogging;
	}

	public String getLogFolder() {
		return this.logFolder;
	}
	
	public String getUserLogFolder() {
		return this.userLogFolder;
	}

	public String getDbURL() {
		return this.dbURL;
	}

	public String getDbUser() {
		return this.dbUser;
	}

	public String getDbPasswd() {
		return this.dbPasswd;
	}
	
	public String getModelFolderLinkgTests() {
		return this.modelFolderLT;
	}
	
	public String getCustomAnnotationIcon(){
		return this.customAnnotationIcon;
	}
	
	public String getUploadPwd() {
		return uploadPwd;
	}

	public String getRepositoryURL() {
		return repositoryURL;
	}
	
	public String getLinkingFolder() {
		return linkingFolder;
	}

	public String getLuceneVersion() {
		return luceneVersion;
	}

	public int getAssumedMaxDocumentCount() {
		return assumedMaxDocumentCount;
	}

	public int getVsmCollectionSimilarityMaximumListedMatchingDocuments() {
		return VsmCollectionSimilarityMaximumListedMatchingDocuments;
	}

	public boolean getLogLinkingProcess() {
		return logLinkingProcess;
	}

	public int getFileNameHashingReadablePartLength() {
		return fileNameHashingReadablePartLength;
	}

	public String getLinkingOutputDirectory() {
		return linkingOutputDirectory;
	}

	public String getNameDividorSymbol() {
		return nameDividorSymbol;
	}

	public String getMarkAsIntroductionString() {
		return markAsIntroductionString;
	}

	public String getLinkingInputDirectory() {
		return linkingInputDirectory;
	}

	public String getGlossariesDirectory() {
		return glossariesDirectory;
	}

	public String getUnorderedDocumentsDirectory() {
		return unorderedDocumentsDirectory;
	}


	public String getTestDocumentsDirectory() {
		return testDocumentsDirectory;
	}

	public String getBasicDocumentsDirectory() {
		return basicDocumentsDirectory;
	}

	public String getEvaluationResultsDirectory() {
		return evaluationResultsDirectory;
	}


	public String getIndicesDirectory() {
		return indicesDirectory;
	}

	public String getDocumentsDirectory() {
		return documentsDirectory;
	}

	public String getDocumentVsmCollectionsDirectory() {
		return documentVsmCollectionsDirectory;
	}


	public String getCollectionSimilaritiesDirectory() {
		return collectionSimilaritiesDirectory;
	}

	public String getResultsDirectory() {
		return resultsDirectory;
	}

	public String getHumanFeedbackDataDirectory() {
		return humanFeedbackDataDirectory;
	}

	public String getTestDataDirectory() {
		// TODO Auto-generated method stub
		return testDataDirectory;
	}

	public String getTestIndicesDirectory() {
		// TODO Auto-generated method stub
		return testIndicesDirectory;
	}

	public String getTestDocumentVsmCollectionsDirectory() {
		// TODO Auto-generated method stub
		return testDocumentVsmCollectionsDirectory;
	}

	public String getTestCollectionSimilaritiesDirectory() {
		// TODO Auto-generated method stub
		return testCollectionSimilaritiesDirectory;
	}

	public boolean getAlsoCreateHumanReadableVersionOfFiles() {
		// TODO Auto-generated method stub
		return alsoCreateHumanReadableVersionOfFiles;
	}

	public String getGlossaryEffectivenessTest() {
		// TODO Auto-generated method stub
		return glossaryEffectivenessTest;
	}

	public String getIndexVsGlossary() {
		// TODO Auto-generated method stub
		return indexVsGlossary;
	}

	public String getGlossaryIndexOverlap() {
		// TODO Auto-generated method stub
		return glossaryIndexOverlap;
	}

	public int getCollectionSimilaritySanityTestNumberOfSvdDimensions() {
		// TODO Auto-generated method stub
		return collectionSimilaritySanityTestNumberOfSvdDimensions;
	}

	public LanguageEnum getBasicIndexingTestLanguage() {
		// TODO Auto-generated method stub
		return basicIndexingTestLanguage;
	}
	
	public int getOccurrenceCountForIntroduction() {
		return this.occurrenceCountForIntroduction;
	}
	
	public double getFindAllBodyWeight() {
		return this.findAllBodyWeight;
	}
	
	public double getFindAllTitleWeight() {
		return this.findAllTitleWeight;
	}
	
	public double getFindAnyBodyWeight() {
		return this.findAnyBodyWeight;
	}
	
	public double getFindAnyTitleWeight() {
		return this.findAnyTitleWeight;
	}
	
	public double getPhraseBodyWeight() {
		return this.phraseBodyWeight;
	}
	
	public double getPhraseTitleWeight() {
		return this.phraseTitleWeight;
	}
	
	public double getTermIntroductionWeight() {
		return this.termIntroductionWeight;
	}
	
	public double getGlossaryIndexBodyWeight() {
		return this.glossaryIndexBodyWeight;
	}
	
	public double getGlossaryIndexTitleWeight() {
		return this.glossaryIndexTitleWeight;
	}
	
	public String getEnglishNERModel() {
		return this.englishNERModel;
	}
	
	public String getGermanNERModel() {
		return this.germanNERModel;
	}
	
	public String getFrenchNERModel() {
		return this.frenchNERModel;
	}
	
	public String getSpanishNERModel() {
		return this.spanishNERModel;
	}
	
	public String getDutchNERModel() {
		return this.dutchNERModel;
	}
	
	public String getNERTags() {
		return this.NERTags;
	}
	
	public String getEnglishPosModel() {
		return this.englishPosModel;
	}
	
	public String getGermanPosModel() {
		return this.germanPosModel;
	}
	
	public String getFrenchPosModel() {
		return this.frenchPosModel;
	}
	
	public String getSpanishPosModel() {
		return this.spanishPosModel;
	}
	
	public String getDutchPosModel() {
		return this.dutchPosModel;
	}
	
	public String getOntologyBlacklistsPath() {
		return this.ontologyBlacklistsPath;
	}
	
	public String getUsedOntologyBlacklist() {
		return this.usedOntologyBlacklist;
	}
	
	public String getPythonScriptPath() {
		return this.pythonScriptPath;
	}
	
	public String getTdbDirectory() {
		return this.tdbDirectory;
	}
	
	public String getJepLibraryPath() {
		return this.jepLibraryPath;
	}
	
	public String getEnglishTexPattern() {
		return this.englishTexPattern;
	}
	
	public String getGermanTexPattern() {
		return this.germanTexPattern;
	}
	
	public String getFrenchTexPattern() {
		return this.frenchTexPattern;
	}
	
	public String getSpanishTexPattern() {
		return this.spanishTexPattern;
	}
	
	public String getDutchTexPattern() {
		return this.dutchTexPattern;
	}
	
	public int getNumberOfJumps() {
		return this.numberOfJumps;
	}
	
	public double getSimilarityScoreThreshold() {
		return this.similarityScoreThreshold;
	}

}
