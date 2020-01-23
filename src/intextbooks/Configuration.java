package intextbooks;

import intextbooks.ontologie.LanguageEnum;


public class Configuration {
	private static Configuration instance = null;
	
	protected Configuration() {
		ConfigurationLoader cLoader = new ConfigurationLoader();
		this.ontologyNS = cLoader.getOntologyNS();
		this.ontologyPath = cLoader.getOntologyPath();
		this.contentFolder = cLoader.getUploadFolder();
		this.modelFolder = cLoader.getModelFolder();
		this.lineSeparator = cLoader.getLineSeparator();
		this.extractionTmpFolder = cLoader.getExtrationTmpFolder();
		this.extracedSegmentsTXT = cLoader.getExtractedSegmentsFolderTXT();
		this.extracedSegmentsPDF = cLoader.getExtractedSegmentsFolderPDF();
		this.logFolder = cLoader.getLogFolder();
		this.userLogFolder = cLoader.getUserLogFolder();
		this.silentLog = cLoader.isSilentLogging();
		this.dbURL = cLoader.getDbURL();
		this.dbUser = cLoader.getDbUser();
		this.dbPasswd = cLoader.getDbPasswd();
		this.modelFolderLT = cLoader.getModelFolderLinkgTests();
		this.modelFolderBuffer = modelFolder;
		this.cAnnotationIcon = cLoader.getCustomAnnotationIcon();
		this.uploadPwd = cLoader.getUploadPwd();
		this.repositoryURL = cLoader.getRepositoryURL();
		this.linkingFolder = cLoader.getLinkingFolder();
		
		this.luceneVersion = cLoader.getLuceneVersion();
		this.assumedMaxDocumentCount = cLoader.getAssumedMaxDocumentCount();
		this.logLinkingProcess = cLoader.getLogLinkingProcess();
		this.VsmCollectionSimilarityMaximumListedMatchingDocuments = cLoader.getVsmCollectionSimilarityMaximumListedMatchingDocuments();
		this.nameDividorSymbol = cLoader.getNameDividorSymbol();
		this.markAsIntroductionString = cLoader.getMarkAsIntroductionString();
		this.fileNameHashingReadablePartLength = cLoader.getFileNameHashingReadablePartLength();
		this.linkingInputDirectory = cLoader.getLinkingInputDirectory();
		this.linkingOutputDirectory = cLoader.getLinkingOutputDirectory();
		this.documentsDirectory = cLoader.getDocumentsDirectory();
		this.glossariesDirectory = cLoader.getGlossariesDirectory();
		this.resultsDirectory = cLoader.getResultsDirectory();
		this.indicesDirectory = cLoader.getIndicesDirectory();
		this.documentVsmCollectionsDirectory = cLoader.getDocumentVsmCollectionsDirectory();
		this.collectionSimilaritiesDirectory = cLoader.getCollectionSimilaritiesDirectory();
		this.humanFeedbackDataDirectory = cLoader.getHumanFeedbackDataDirectory();
		this.evaluationResultsDirectory = cLoader.getEvaluationResultsDirectory();
		this.basicDocumentsDirectory = cLoader.getBasicDocumentsDirectory();
		this.testDocumentsDirectory = cLoader.getTestDocumentsDirectory();
		this.unorderedDocumentsDirectory = cLoader.getUnorderedDocumentsDirectory();
		this.testDataDirectory = cLoader.getTestDataDirectory();
		this.testIndicesDirectory = cLoader.getTestIndicesDirectory();
		this.testDocumentVsmCollectionsDirectory = cLoader.getTestDocumentVsmCollectionsDirectory();
		this.testCollectionSimilaritiesDirectory = cLoader.getTestCollectionSimilaritiesDirectory();
		this.alsoCreateHumanReadableVersionOfFiles = cLoader.getAlsoCreateHumanReadableVersionOfFiles();
		this.glossaryEffectivenessTest = cLoader.getGlossaryEffectivenessTest();
		this.indexVsGlossary = cLoader.getIndexVsGlossary();
		this.glossaryIndexOverlap = cLoader.getGlossaryIndexOverlap();
		this.collectionSimilaritySanityTestNumberOfSvdDimensions = cLoader.getCollectionSimilaritySanityTestNumberOfSvdDimensions();
		this.basicIndexingTestLanguage = cLoader.getBasicIndexingTestLanguage();
		this.occurrenceCountForIntroduction = cLoader.getOccurrenceCountForIntroduction();
		
		this.findAllBodyWeight = cLoader.getFindAllBodyWeight();
		this.findAllTitleWeight = cLoader.getFindAllTitleWeight();
		this.findAnyBodyWeight = cLoader.getFindAnyBodyWeight();
		this.findAnyTitleWeight = cLoader.getFindAnyTitleWeight();
		this.phraseBodyWeight = cLoader.getPhraseBodyWeight();
		this.phraseTitleWeight = cLoader.getPhraseTitleWeight();
		
		this.glossaryIndexBodyWeight = cLoader.getGlossaryIndexBodyWeight();
		this.glossaryIndexTitleWeight = cLoader.getGlossaryIndexTitleWeight();
		
		this.termIntroductionWeight = cLoader.getTermIntroductionWeight();
		
		this.englishPosModel = cLoader.getEnglishPosModel();
		this.germanPosModel = cLoader.getGermanPosModel();
		this.frenchPosModel = cLoader.getFrenchPosModel();
		this.spanishPosModel = cLoader.getSpanishPosModel();
		this.dutchPosModel = cLoader.getDutchPosModel();
		
		this.englishNERModel = cLoader.getEnglishNERModel();
		this.germanNERModel = cLoader.getGermanNERModel();
		this.frenchNERModel = cLoader.getFrenchNERModel();
		this.spanishNERModel = cLoader.getSpanishNERModel();
		this.dutchNERModel = cLoader.getDutchNERModel();
		this.NERTags = cLoader.getNERTags();
		
		this.ontologyBlacklistsPath = cLoader.getOntologyBlacklistsPath();
		this.usedOntologyBlacklist = cLoader.getUsedOntologyBlacklist();
		
		this.pythonScriptPath = cLoader.getPythonScriptPath();
		
		this.tdbDirectory = cLoader.getTdbDirectory();
		
		this.englishTexPattern = cLoader.getEnglishTexPattern();
		this.germanTexPattern = cLoader.getGermanTexPattern();
		this.frenchTexPattern = cLoader.getFrenchTexPattern();
		this.spanishTexPattern = cLoader.getSpanishTexPattern();
		this.dutchTexPattern = cLoader.getDutchTexPattern();
		
		this.numberOfJumps = cLoader.getNumberOfJumps();
		this.similarityScoreThreshold = cLoader.getSimilarityScoreThreshold();
		
	}
	
	public static Configuration getInstance() {
		if(instance == null) {
			instance = new Configuration();
		}
		return instance;
	}
	
	private String ontologyPath;
	private String contentFolder;
	private String modelFolder;
	private String ontologyNS;
	private String lineSeparator;
	private String extractionTmpFolder;
	private String extracedSegmentsTXT;
	private String extracedSegmentsPDF;
	private String logFolder;
	private String userLogFolder;
	private boolean silentLog;
	private String dbURL;
	private String dbUser;
	private String dbPasswd;
	private String modelFolderLT;
	private String modelFolderBuffer;
	private String cAnnotationIcon;
	private String uploadPwd;
	private String repositoryURL;
	private String linkingFolder;
	
	
//--- Linking Config Info ---
	
//	<!-- Parameters -->
	private String luceneVersion;	
	private int assumedMaxDocumentCount;
	private boolean logLinkingProcess;
	private int VsmCollectionSimilarityMaximumListedMatchingDocuments;
	private String nameDividorSymbol;
	private String markAsIntroductionString;
	private int fileNameHashingReadablePartLength;
	private boolean alsoCreateHumanReadableVersionOfFiles;
	private int collectionSimilaritySanityTestNumberOfSvdDimensions;
	private LanguageEnum basicIndexingTestLanguage;
	private int occurrenceCountForIntroduction;
	
//	<!-- Directories -->
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
	private String glossaryEffectivenessTest;
	private String indexVsGlossary;
	private String glossaryIndexOverlap;
	private String tdbDirectory;
	
	private double findAllBodyWeight;
	private double findAllTitleWeight;
	private double findAnyBodyWeight;
	private double findAnyTitleWeight;
	private double phraseBodyWeight;
	private double phraseTitleWeight;
	
	private double glossaryIndexBodyWeight;
	private double glossaryIndexTitleWeight;
	
	private double termIntroductionWeight;
	
// <!-- POS models -->
	private String englishPosModel;
	private String germanPosModel;
	private String frenchPosModel;
	private String spanishPosModel;
	private String dutchPosModel;
	
	// <!-- NER models -->
	private String englishNERModel;
	private String germanNERModel;
	private String frenchNERModel;
	private String spanishNERModel;
	private String dutchNERModel;
	private String NERTags;
	
// <!-- ontology blacklists -->
	private String ontologyBlacklistsPath;
	private String usedOntologyBlacklist;

// <!-- ontology blacklists -->
	private String pythonScriptPath;
	
	// <!-- Tex Patterns -->
	private String englishTexPattern;
	private String germanTexPattern;
	private String frenchTexPattern;
	private String spanishTexPattern;
	private String dutchTexPattern;
	
	// <!-- KB Enrichment -->
	private int numberOfJumps;
	private double similarityScoreThreshold;
	
	public String getOntologyPath() {
		return ontologyPath;
	}

	public String getContentFolder() {
		return contentFolder;
	}

	public String getModelFolder() {
		return modelFolder;
	}
	
	public String getOntologyNS() {
		return ontologyNS;
	}
	
	public String getLineSeparator() {
		return lineSeparator;
	}
	
	public String getExtractionTmpFolder(){
		return extractionTmpFolder;
	};
	
	public String getExtractedSegmentsFolderTXT(){
		return extracedSegmentsTXT;
	}
	
	public String getExtractedSegmentsFolderPDF(){
		return extracedSegmentsPDF;
	}
	
	public String getLogFolder(){
		return logFolder;
	}
	
	public String getUserLogFolder(){
		return userLogFolder;
	}
	
	public boolean isSilentLogging(){
		return silentLog;
	}

	public String getDatabaseURL() {
		return dbURL;
	}

	public String getDbUser() {
		return dbUser;
	}

	public String getDbPasswd() {
		return dbPasswd;
	}
	
	public String getModelFolderLinkingTest() {
		return modelFolderLT;
	}

	public void switchToLT() {
		this.modelFolder = this.modelFolderLT;
	}
	
	public void returnFromLT(){
		this.modelFolder = this.modelFolderBuffer;
	}
	
	public String getCustomAnnotationIcon(){
		return this.cAnnotationIcon;
	}
	
	public String getUploadPwd(){
		return this.uploadPwd;
	}
	
	public String getRepositoryURL(){
		return this.repositoryURL;
	}
	
	public String getLinkingFolder(){
		return this.linkingFolder;
	}
	
	public void changeConfig(String contentFolder, String modelFolder, String modelFolderLT, 
			String extractionTmpFolder, String pdfFolder, String txtFolder, String logFolder, String ontologyPath){
		
		this.contentFolder = contentFolder;
		this.modelFolder = modelFolder;
		this.modelFolderLT = modelFolderLT;
		this.extractionTmpFolder = extractionTmpFolder;
		this.extracedSegmentsPDF = pdfFolder;
		this.extracedSegmentsTXT = txtFolder;
		this.logFolder = logFolder;
		this.ontologyPath = ontologyPath;
		
	}
	

	public String getLuceneVersion() {
		return luceneVersion;
	}

	public int getAssumedMaxDocumentCount() {
		return assumedMaxDocumentCount;
	}

	public boolean getLogLinkingProcess() {
		return logLinkingProcess;
	}

	public int getVsmCollectionSimilarityMaximumListedMatchingDocuments() {
		return VsmCollectionSimilarityMaximumListedMatchingDocuments;
	}

	public String getNameDividorSymbol() {
		return nameDividorSymbol;
	}

	public String getMarkAsIntroductionString() {
		return markAsIntroductionString;
	}

	public int getFileNameHashingReadablePartLength() {
		return fileNameHashingReadablePartLength;
	}

	public String getLinkingInputDirectory() {
		return linkingInputDirectory;
	}

	public String getDocumentsDirectory() {
		return documentsDirectory;
	}
	
	public String getLinkingOutputDirectory() {
		return linkingOutputDirectory;
	}

	public String getGlossariesDirectory() {
		return glossariesDirectory;
	}

	public String getResultsDirectory() {
		return resultsDirectory;
	}
	public String getIndicesDirectory() {
		return indicesDirectory;
	}

	public String getDocumentVsmCollectionsDirectory() {
		return documentVsmCollectionsDirectory;
	}

	public String getCollectionSimilaritiesDirectory() {
		return collectionSimilaritiesDirectory;
	}

	public String getEvaluationResultsDirectory() {
		return evaluationResultsDirectory;
	}
	public String getHumanFeedbackDataDirectory() {
		return humanFeedbackDataDirectory;
	}
	public String getBasicDocumentsDirectory() {
		return basicDocumentsDirectory;
	}
	public String getTestDocumentsDirectory() {
		return testDocumentsDirectory;
	}

	public String getUnorderedDocumentsDirectory() {
		return unorderedDocumentsDirectory;
	}

	public String getTestDataDirectory() {
		return testDataDirectory;
	}

	public String getTestIndicesDirectory() {
		return testIndicesDirectory;
	}

	public String getTestDocumentVsmCollectionsDirectory() {
		return testDocumentVsmCollectionsDirectory;
	}

	public String getTestCollectionSimilaritiesDirectory() {
		return testCollectionSimilaritiesDirectory;
	}

	public boolean getAlsoCreateHumanReadableVersionOfFiles() {
		return alsoCreateHumanReadableVersionOfFiles;
	}

	public String getGlossaryEffectivenessTest() {
		return glossaryEffectivenessTest;
	}

	public String getIndexVsGlossary() {
		return indexVsGlossary;
	}

	public String getGlossaryIndexOverlap() {
		return glossaryIndexOverlap;
	}

	public int getCollectionSimilaritySanityTestNumberOfSvdDimensions() {
		return collectionSimilaritySanityTestNumberOfSvdDimensions;
	}

	public LanguageEnum getBasicIndexingTestLanguage() {
		return basicIndexingTestLanguage;
	}
	
	public int getOccurrenceCountForIntroduction() {
		return occurrenceCountForIntroduction;
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
	
	public double getGlossaryIndexBodyWeight() {
		return this.glossaryIndexBodyWeight;
	}
	
	public double getGlossaryIndexTitleWeight() {
		return this.glossaryIndexTitleWeight;
	}
	
	public double getTermIntroductionWeight() {
		return this.termIntroductionWeight;
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
	
	public String getEnglishPosModel() {
		return this.englishPosModel;
	}
	
	public String getNERTags() {
		return this.NERTags;
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
