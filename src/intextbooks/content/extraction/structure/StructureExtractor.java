package intextbooks.content.extraction.structure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.semanticweb.skos.SKOSAnnotation;
import org.semanticweb.skos.SKOSConcept;
import org.semanticweb.skos.SKOSCreationException;
import org.semanticweb.skos.SKOSDataset;
import org.semanticweb.skos.SKOSUntypedLiteral;

import intextbooks.RandomAccessToElements;
import intextbooks.SystemLogger;
import intextbooks.content.ContentManager;
import intextbooks.content.extraction.ExtractorController.resourceType;
import intextbooks.content.extraction.Utilities.BoundSimilarity;
import intextbooks.content.extraction.Utilities.CorpusSearcher;
import intextbooks.content.extraction.Utilities.GenericStatisticsMapHandle;
import intextbooks.content.extraction.Utilities.IntroductionFinder;
import intextbooks.content.extraction.Utilities.Stemming;
import intextbooks.content.extraction.Utilities.StringOperations;
import intextbooks.content.extraction.buildingBlocks.format.Line;
import intextbooks.content.extraction.buildingBlocks.format.Page;
import intextbooks.content.extraction.buildingBlocks.format.ResourceUnit;
import intextbooks.content.extraction.buildingBlocks.structure.BookStructure;
import intextbooks.content.extraction.buildingBlocks.structure.IndexElement;
import intextbooks.content.extraction.buildingBlocks.structure.IndexTerm;
import intextbooks.content.extraction.buildingBlocks.structure.TOC;
import intextbooks.content.extraction.buildingBlocks.structure.TOCLogical;
import intextbooks.content.extraction.buildingBlocks.structure.TOCResource;
import intextbooks.content.extraction.buildingBlocks.structure.TOCResourceType;
import intextbooks.content.extraction.format.FormatExtractor;
import intextbooks.content.extraction.format.PdfIntraLinkRemover;
import intextbooks.content.extraction.structure.TableOfContentsExtractor.tocType;
import intextbooks.content.models.formatting.FormattingDictionary;
import intextbooks.exceptions.BookWithoutPageNumbersException;
import intextbooks.exceptions.EarlyInterruptionException;
import intextbooks.exceptions.NoIndexException;
import intextbooks.exceptions.TOCNotFoundException;
import intextbooks.ontologie.LanguageEnum;
import intextbooks.ontologie.Mediator;
import intextbooks.persistence.Persistence;
import intextbooks.persistence.TEIBuilder;
import intextbooks.tools.utility.ListOperations;
import uk.ac.manchester.cs.skos.SKOSRDFVocabulary;


public class StructureExtractor {

	private List<String> titles = new Vector <String> ();
	private List<IndexElement> index = new Vector <IndexElement> ();
	private TOCLogical tocLogical = new TOCLogical();
	private BookStructure bookStructure =  new BookStructure();

	private StructureBuilder listToModel;
	private ContentManager cm = ContentManager.getInstance();
	private resourceType type;
	
	private String bookID;
	
	public StructureExtractor(String bookID, String filePath, LanguageEnum lang, resourceType type, boolean processReadingLabels, boolean linkToExternalGlossary, boolean splitTextbook) throws TOCNotFoundException, NullPointerException, BookWithoutPageNumbersException, NoIndexException{
		
		this.type = type;
		this.bookID = bookID;
		
		PdfIntraLinkRemover cleaner = new PdfIntraLinkRemover(filePath);
		cleaner.clearAll();
		
		switch(type){

		case BOOK:
			bookExtractor(bookID, filePath, lang, type, processReadingLabels, linkToExternalGlossary, splitTextbook);
			break;
		default:
			break;
		}
	}

/**
 * 
 * @param bookID
 * @param filePath
 * @param lang
 * @param type
 * @throws TOCNotFoundException 
 * @throws EarlyInterruptionException 
 * @throws NoIndexException 
 */
	private void bookExtractor(String bookID, String filePath, LanguageEnum lang, resourceType type, boolean processReadingLabels, boolean linkToExternalGlossary, boolean splitTextbook) throws TOCNotFoundException, NullPointerException, BookWithoutPageNumbersException, NoIndexException{
		SystemLogger.getInstance().log("Start: Book Extaction (bookExtractor)");
		FormatExtractor parse;
		IndexExtractor indexExtractor = new IndexExtractor(bookID);
		SegmentExtractor segmentExtractor = new SegmentExtractor(lang);
		FormattingDictionary styleLibrary;

		try {

			this.listToModel = new StructureBuilder(bookID, type);

			SystemLogger.getInstance().log("Start: Parsing (FormatExtractor)");
			
			//Parsing of the textbook
			parse = new FormatExtractor(bookID, filePath, type);
			styleLibrary = this.cm.getStyleLibrary(bookID);
			bookStructure.setBookContent(parse.getBookContent());	
			
//			/* TESTING */
//			System.out.println(parse.getBookContent());
//			for(int n = 14; n <= 14; n++) {
//				System.out.println("****************************** " + n);
//				for(Line l: parse.getPages().get(n).getLines()) {
//					System.out.println(l.getPositionY() + " - " + l.getText());
//				}
//			}
//			System.exit(0);
//			/* TESTING */

			SystemLogger.getInstance().log("Start: Index Extraction (indexExtractor.extractIndex)");
			try {
				this.index = indexExtractor.extractIndex(parse.getPages(),parse.getTableOfContents(), bookID, styleLibrary, parse.getFonts(), processReadingLabels);
			} catch (NoIndexException e) {
				SystemLogger.getInstance().log("ERROR while extracting the index: " + e.getMessage());
			}
			
			//Extract TOC according to its type
			if(TableOfContentsExtractor.getType().equals(tocType.INDENTED)){

				SystemLogger.getInstance().log("Start: TOC Extraction (modelOverIndentation)");

//				/*TESTING*/
//				Iterator<IndexTerm> it = index.iterator();
//				System.out.println("@@@INDEX TERMS");
//				while(it.hasNext()) {
//					IndexTerm t = it.next();
//					System.out.println("^ID " + t.getID());
//					System.out.println("^Parent" + t.getParent());
//					System.out.println("^PageIndeces" + t.getPageIndicies());
//					System.out.println("^PageNumbers" + t.getPageNumbers());
//					System.out.println(".......");
//				}
//				System.exit(0);
//				/*TESTING*/
				
//				/*FIX*/
//				Iterator<TOC> it0 = parse.getTableOfContents().iterator();
//				System.out.println("@@@TOC ENTRIES");
//				while(it0.hasNext()) {
//					TOC t = it0.next();
//					if(t.getTitleText().equals("Erratum to: Statistics and Sampling Distributions E1") || t.getTitleText().equals("Erratum to: Statistics and Sampling Distributions E1")
//							|| t.getTitleText().equals("appendix tables") || t.getTitleText().equals("answers to odd-numbered exercises") 
//							|| t.getTitleText().equals("index")){
//						t.setPosX(88.8945f);
//					}
//				}
				
//				/*TESTING*/
//				Iterator<TOC> it = parse.getTableOfContents().iterator();
//				System.out.println("@@@TOC ENTRIES");
//				while(it.hasNext()) {
//					TOC t = it.next();
//					System.out.println("^T: " + t.getTitleText());
//					System.out.println("^^PosX: " + t.getPosX());
//					System.out.println(".......");
//				}
//				System.out.println(this.tocLogical.toString());
//				System.exit(0);
//				/*TESTING*/
				
				this.tocLogical = modelOverIndentation(bookID, parse.getTableOfContents(),parse,index, type);
				this.tocLogical.setTitle(parse.getTOCTitle());
				this.tocLogical.setPages(parse.getTOCPages());

			}
			else if (TableOfContentsExtractor.getType().equals(tocType.ORDERED)){
				SystemLogger.getInstance().log("Start: TOC Extraction (modelOverOrder)");
				modelOverOrder(bookID, parse.getTableOfContents(),parse,index, type);
			}
			else{
				SystemLogger.getInstance().log("Start: TOC Extraction (modelOverFlat)");
				this.tocLogical = modelOverFlat(bookID, parse.getTableOfContents(),parse,index, type);
				this.tocLogical.setTitle(parse.getTOCTitle());
				this.tocLogical.setPages(parse.getTOCPages());
				//System.out.println(this.tocLogical.toString());
			}
			
//			/*TESTING*/
//			for(Page p: parse.getPages()) {
//				System.out.println("PI: " + p.getPageIndex() + "/ PN: " + p.getPageNumber());
//			}
//			System.exit(0);
//			/*TESTING*/		
			
			SystemLogger.getInstance().log("Start: Create SKOS model");
			this.listToModel.produceSKOSModelOfBook(bookID,parse.getPageNumbers(), index, parse, type);
			
			SystemLogger.getInstance().log("Start: Separate Chapters into segments (segmentExtractor.seperatedChapterExtractions)");
			/*
			 * Extracts start and end page, and start and end line inside the page for each sub/chapter.
			 * It also creates the chapter and paragraphs segments and adds that information to the book model (Book).
			 * It also creates the chapter and paragraphs schemes elements in the SKOS MODEL.
			 */
			ArrayList<SegmentData> segmentsData = segmentExtractor.seperatedChapterExtractions(bookID, parse, this.listToModel, this.tocLogical, styleLibrary, splitTextbook);
			segmentExtractor.addSegmentsToSKOSModel(this.listToModel);
			
			//rule: Label Of Styles Cross Check
			SystemLogger.getInstance().log("Start: Label of Styles Cross Check");
			labelOfStylesCrossCheck(segmentsData, parse.getPages(), parse.getLabelOfStyles());
			parse.identifyTextBlocksV2(bookID, parse.getLabelOfStyles());
			
			SystemLogger.getInstance().log("Start: matching Index Terms to Segments");
			indexExtractor.matchIndexTermsToSegments(parse.getPagesAsLines(), bookID, index);
			
//			for(IndexElement i: this.index) {
//				System.out.println(i);
//			}
//			System.exit(0);
			
			SystemLogger.getInstance().log("Start: TEI Model Construction");
			TEIBuilder tei = new TEIBuilder(bookID, lang, segmentsData, parse.getPages(), segmentExtractor, indexExtractor.getFirstIndexPage(), tocLogical, index, parse.getMetadata(), parse.getLabelOfStyles());
			tei.construct();
			cm.setTEIModel(bookID, tei.getModel());

			SystemLogger.getInstance().log("Start: Textual Extraction");
			String allCOntent = extractBookText(segmentsData, parse.getPages(), segmentExtractor, indexExtractor.getFirstIndexPage(), tocLogical);
			
			//SystemLogger.getInstance().log("Start: Store Index to DB (indexExtractor.storeIndexToDatabase)");
			indexExtractor.storeIndexToDatabase(parse.getPagesAsLines(), bookID, index);
			
//			/*TESTING*/	
//			for(IndexElement i: index) {
//				System.out.println(i);
//			}
//			System.exit(0);
//			/*TESTING*/	

			/**
			 * Method searchWords highlights the words. Color blue is used for index terms in the pages where the terms are introduced 
			 * (according to the index, all the index pages for the term).
			 * Color green is used when the index term appears in the text, but that page is not listed in the index entry of the term
			 * in the index section
			 */
			//SystemLogger.getInstance().log("Start: Annotation #1 (annotate)");
			//annotate(parse,filePath,bookID, highlightList);

			//SystemLogger.getInstance().log("Start: Annotation #2 (annotate)");
			//annotate(parse,filePath,bookID);
		
			bookStructure.setIndex(this.index);
			bookStructure.setFirstIndexPage(indexExtractor.getFirstIndexPage());
			bookStructure.setToc(this.tocLogical);
			bookStructure.setFormattingDictionary(this.cm.getStyleLibrary(bookID));
			bookStructure.setBookContent(parse.getBookContent());
			bookStructure.setRawText(allCOntent);
			bookStructure.setNumberPages(parse.getPages().size());
			
			this.cm.getInstanceOfBookByName(bookID).setPages(parse.getPages());
			
			
			SystemLogger.getInstance().log("End: Book Extaction (bookExtractor)");
			
			/*TESTING*/		
//			SystemLogger.getInstance().log("Printing segments information (printSegmentsInformation)");
//			printSegmentsInformation(segmentsData);
			/*TESTING*/		
			
		} catch (IOException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		} catch (TOCNotFoundException e) {
			throw e;
		} catch (NullPointerException e) {
			throw e;
		} catch (BookWithoutPageNumbersException e) {
			throw e;
		} 
	}
	
	private void labelOfStylesCrossCheck(ArrayList<SegmentData> segmentsData, List<Page> pages, FormattingDictionary lOS) {
		for(int hL = 1; hL< 5; hL++) {
			SystemLogger.getInstance().debug("### Level: " + hL);
			Set<Integer> keySums = new HashSet<Integer>();
			for(int i=0; i < segmentsData.size(); i++) {
				SegmentData s = segmentsData.get(i);
				if(s.getChapterMedatada().getChapterHierarchy() == hL) {
					int startPage =s.getChapterMedatada().getPageStartIndex();
					int ls = s.getChapterMedatada().getTitleLineStart();
					int le = s.getChapterMedatada().getLineStart();
					SystemLogger.getInstance().debug(s.getTitle());
					SystemLogger.getInstance().debug("s "+ ls);
					SystemLogger.getInstance().debug("e " +le);
					if(ls != -1 && le < pages.get(startPage).size()) {
						for(int j = ls; j <= le; j++) {
							int keySum = pages.get(startPage).getLineAt(j).getFCKeySum();
							keySums.add(keySum);
							SystemLogger.getInstance().debug(pages.get(startPage).getLineAt(j).getText());
							SystemLogger.getInstance().debug("\t" +keySum); ;
						}
						
					}
				}
			}
			if(keySums.size() > 0) {
				//update the role(s)
				for(Integer k: keySums) {
					lOS.updateTitleRoleLabel(k, hL);
				}
			}
			
		}
		lOS.cleanlibrary();

//		/*TESTING*/
//		for(int p =21; p < 22; p++) {
//			if(pages.get(p) == null)
//				continue;
//			//System.out.println("PPPPPPPP: " + p);
//			List<Line> lines = pages.get(p).getLines();
//			for(Line l : lines) {
//					System.out.println("> " + l.getText() + l.getFCKeySum());
//				
//			}
//		}
//		/*TETSIGN*/

	}
	
	private String extractBookText(ArrayList<SegmentData> segmentsData, List<Page> pages, SegmentExtractor segmentExtractor, int firstIndexPage, TOCLogical tocLogical) {
		String allContent = "";
		int firstPage = -1;
		
		//all the first pages of chapters, where to look up for copyright lines
		List<Integer> firstPages = new ArrayList<Integer>();
		for(int i=0; i < segmentsData.size(); i++) {
			SegmentData s = segmentsData.get(i);
			if(s.getChapterMedatada().getChapterHierarchy() == 1) {
				firstPages.add(s.getChapterMedatada().getPageStartIndex());
			}
		}
		
		for(int i=0; i < segmentsData.size(); i++) {
			SegmentData s = segmentsData.get(i);
			
			/*TESTING*/
//			System.out.println("> " + s.getTitle()+ " ID: " + s.getChapterID() + " PID: " + s.getParagraphID() + " H: " + s.getHierarchy());	
//			System.out.println("\tTitleStart: " + s.getChapterMedatada().getPageStartIndex() + " startLine: " + s.getChapterMedatada().getTitleLineStart());
//			System.out.println("\tstartP: " + s.getChapterMedatada().getPageStartIndex() + " startLine: " + s.getChapterMedatada().getLineStart());
//			System.out.println("\tendP: " + s.getChapterMedatada().getPageEndIndex() + " endLine: " + s.getChapterMedatada().getLineEnd());
			/*TESTING*/
			
			if(firstPage == -1) {
				firstPage =  s.getChapterMedatada().getPageStartIndex();
			}
			
			//check if we must stop for index
			if(s.getChapterMedatada().getPageStartIndex() >= firstIndexPage) {
				break;
			}
			
			//get title of section 
			String sectionTitle = "";
			Page currentPage = pages.get(s.getChapterMedatada().getPageStartIndex());
			if(s.getChapterMedatada().getTitleLineStart() != -1) {
				for(int lineIndex=s.getChapterMedatada().getTitleLineStart(); lineIndex <= s.getChapterMedatada().getLineStart(); lineIndex++) {
					sectionTitle += currentPage.getLineAt(lineIndex).getText();
					if(lineIndex != s.getChapterMedatada().getLineStart())
						sectionTitle += " ";
				}
			}
			
			//get content of section
			String content = sectionTitle + "\n";
			if(i != segmentsData.size() -1) {
				SegmentData nextS = segmentsData.get(i+1);
				int endPage = nextS.getChapterMedatada().getPageStartIndex();
				int endLine = nextS.getChapterMedatada().getTitleLineStart() -1;
				if(endLine <= 1) {
					endPage = getValidPreviousPage(nextS.getChapterMedatada().getPageStartIndex() -1, pages);
					endLine = pages.get(endPage).size()-1;
				}
				content += segmentExtractor.extractChapterLines(pages, s.getChapterMedatada().getPageStartIndex(), s.getChapterMedatada().getLineStart()+1, endPage, endLine, s.getChapterMedatada().getPageTitle(), s.getChapterMedatada().getPageStart(), firstPages.contains(s.getChapterMedatada().getPageStartIndex()));
			} else {
				//last section
				content += segmentExtractor.extractChapterLines(pages, s.getChapterMedatada().getPageStartIndex(), s.getChapterMedatada().getLineStart()+1, s.getChapterMedatada().getPageEndIndex(), s.getChapterMedatada().getLineEnd(),s.getChapterMedatada().getPageTitle(), s.getChapterMedatada().getPageStart(),  firstPages.contains(s.getChapterMedatada().getPageStartIndex()));
			}
			
			String path = "";
			if(i < 10)
				path += "0";
			path += (i+1);
			path += " " + sectionTitle;
			allContent += content;
			//Persistence.getInstance().storeExtractonTempInfo(this.bookID, path, content);
			Persistence.getInstance().storeSegment(this.bookID, s.getChapterID()+1, content);
		}
		
		if(firstPage != -1) {
			int endPage = getValidPreviousPage(firstPage -1, pages);
			int endLine = pages.get(endPage).size()-1;
			int endPageBeforeTOC = 0;
			int endLineBeforeTOC = 0;
			for(int i = 0; i < pages.size(); i++) {
				if(tocLogical.isTOCIndexPage(i)) {
					endPageBeforeTOC = getValidPreviousPage(i -1, pages);
					endLineBeforeTOC = pages.get(endPageBeforeTOC).size()-1;
					break;
				}
				endPageBeforeTOC = i;
			}
			int startPageAfterTOC = tocLogical.getLastTOCPage()+1;
			String frontContent = segmentExtractor.extractChapterLines(pages, 0, 0, endPageBeforeTOC, endLineBeforeTOC, "frontMatter", 0,  false);
			frontContent += tocLogical.toSectionString();
			frontContent += segmentExtractor.extractChapterLines(pages, startPageAfterTOC, 0, endPage, endLine, "frontMatter", 0,  false);
			//Persistence.getInstance().storeExtractonTempInfo(this.bookID, "00 frontMatter", frontContent);
			Persistence.getInstance().storeSegment(this.bookID, 1, frontContent);
			allContent = frontContent + allContent;
		}
		
		//Persistence.getInstance().storeExtractonTempInfo(this.bookID, "00", allContent);
		Persistence.getInstance().storeSegment(this.bookID, 0, allContent);
		
		return allContent;
	}
	
	private int getValidPreviousPage(int page, List<Page> pages) {
		while(page >= 0) {
			if(pages.get(page) != null && pages.get(page).size() > 0) {
				return page;
			}
			page--;
		}
		return 0;
	}
	
	private void printSegmentsInformation(ArrayList<SegmentData> segmentsData) {
		for(SegmentData segmentData: segmentsData) {
			System.out.println("----------------------------------");
			System.out.println("Title: " + segmentData.getTitle());
			System.out.println("Hierarchy: " + segmentData.getHierarchy());
			System.out.println("ChapterID: " + segmentData.getChapterID());
			System.out.println("StartPageNumber: " + segmentData.getChapterMedatada().pageStart);
			System.out.println("StartPageIndex: " + segmentData.getChapterMedatada().pageStartIndex);
			System.out.println("EndPageNumber: " + segmentData.getChapterMedatada().pageEnd);
			System.out.println("EndPageIndex: " + segmentData.getChapterMedatada().pageEndIndex);
			System.out.println("ParagraphID: " + segmentData.getParagraphID());
			String text = segmentData.getText().length() > 50 ? segmentData.getText().substring(0, 50) : segmentData.getText();
			System.out.println("First 50 chars: " + text);
		}
	}

	/**
	 * 
	 * @param presentationID
	 * @param filePath
	 * @param lang
	 * @param type
	 * @throws EarlyInterruptionException 
	 * @throws BookWithoutPageNumbersException 
	 * @throws NullPointerException 
	 */
	private void slideExtractor(String presentationID, String filePath, LanguageEnum lang, resourceType type) throws NullPointerException, BookWithoutPageNumbersException{

		FormatExtractor parse;
		try {
			this.listToModel = new StructureBuilder(presentationID, type);	

			parse = new FormatExtractor(presentationID, filePath, type);
//			headingSlideIndexMatching(parse.getSlides());			

			
			modelOverFlat(presentationID, parse.getTableOfContents(),parse,index, type);
//			modelOverPresentation(bookID, parse, type);
			
		} catch (IOException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		} catch (TOCNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param bookID
	 * @param parse
	 * @param type
	 */
	private void modelOverPresentation(String bookID, FormatExtractor parse, resourceType type){

		listToModel.produceSKOSModelOfPresentation(bookID, parse.getPageNumbers(), type);		
	}

	/**
	 * 
	 * @param slides
	 */
/*	private void headingSlideIndexMatching(List<Slide> slides){

		Map<String, String> headingIndexMap = new HashMap<String, String> ();
		String previousHeading = "";

		for(int i = 0; i<slides.size(); i++){
			if(slides.get(i) != null){
				String currentHeading = slides.get(i).getSlideHeading();

				if(i==0){

					listToModel.add(currentHeading, 1);

					previousHeading = currentHeading;
					headingIndexMap.put(currentHeading, String.valueOf(slides.get(i).getSlideIndex()));
				}
				else{

					String[] previous = previousHeading.split(" ");
					String[] current = currentHeading.split(" ");

					int j;

					for(j=0; j<previous.length; j++){

						if(!previous[j].toLowerCase().matches(current[j].toLowerCase())   ){

							if(j==previous.length-1 && previous.length>1){
								continue;
							}						
							else
								break;						
						}
					}

					if(j<previous.length){

						listToModel.add(currentHeading, 1);

						previousHeading = currentHeading;
						headingIndexMap.put(currentHeading, String.valueOf(slides.get(i).getSlideIndex()));					
					}
					else{

						String value = headingIndexMap.get(previousHeading);
						value +=","+ String.valueOf(slides.get(i).getSlideIndex());
						headingIndexMap.put(previousHeading, value);		
					}			
				}
			}
		}
	}*/

/**
 * 
 * @param resourceID
 * @param pageIndicies
 * @param conceptNames
 * @return
 */
	private int checkParentalLinks(String resourceID, List<Integer> pageIndicies,List<String> conceptNames){

		List<String> indexTermsOfConcept;

		for(int pageIndex : pageIndicies){

			List<String> indiciesOnPage = cm.IndexTermsOnPage(resourceID, pageIndex);

			for(String conceptName : conceptNames){

				indexTermsOfConcept = cm.getIndexTermsOfConcept(resourceID, conceptName);			

				for(String indexTerm : indexTermsOfConcept ){

					if(indiciesOnPage.contains(indexTerm)){
						return pageIndex;
					}				
				}			
			}
		}
		return -1;
	}

	private String fillToContent(String contentBuffer, String incomingText){
		contentBuffer+=incomingText;	
		return contentBuffer;
	}
	
	private void identifySectionParts(List <TOC> toc) {
		SystemLogger.getInstance().log("Identifying section parts in TOC ...");
		
		Number biggestSpace = GenericStatisticsMapHandle.getBiggestKeys(TableOfContentsExtractor.lineSpacingStatistics).getLeft();
		Number smallestSpace = GenericStatisticsMapHandle.getSmallestKey(TableOfContentsExtractor.lineSpacingStatistics);
		
		
		int diffMargin = 4;
		boolean candidatesIdentified = false;
		
		//Part section without number + other lines
		for(short i=0;i<toc.size();i++){
			
			if(toc.get(i).getPageNumber() == 0) {
				SystemLogger.getInstance().debug("CANDIDATE 1: "+ toc.get(i).getTitleText());
				toc.get(i).setSection(true);
				candidatesIdentified = true;
			}
		}
		if(candidatesIdentified)
			return;
		
		//Part sections with page number: same level
		int ref = (int) toc.get(0).getPosX();
		boolean refBoolean = toc.get(0).isBold();
		boolean refItalic = toc.get(0).isItalic();
		float refFontSize = toc.get(0).getFontSize();
		for(short i=0;i<toc.size() - 1;i++){
			
			float diffLevel1 =  Math.abs((int) ref -(int)toc.get(i).getPosX());
			float diffLevel2 =  Math.abs((int) ref -(int)toc.get(i+1).getPosX());
			if(diffLevel1 < diffMargin && diffLevel2 < diffMargin) {
				//same level
				int pageDiff = toc.get(i+1).getPageNumber() - toc.get(i).getPageNumber();
				if(pageDiff <= 2) {
					boolean moreLevels = false;
					for(int j=i+1;j<toc.size() - 1;j++) {
						float diffLevelRest =  Math.abs((int) ref -(int)toc.get(j).getPosX());
						if(diffLevelRest > diffMargin) {
							if(toc.get(i).isBold() == refBoolean && toc.get(i).isItalic() == refItalic && toc.get(i).getFontSize() == refFontSize) {
								SystemLogger.getInstance().debug("CANDIDATE: "+ toc.get(i).getTitleText());
								SystemLogger.getInstance().debug("ref: " + ref);
								SystemLogger.getInstance().debug("pos: " + toc.get(i).getPosX());
								SystemLogger.getInstance().debug("diffLevel1: " + diffLevel1);
								SystemLogger.getInstance().debug("diffLevel2: " + diffLevel2);
								toc.get(i).setSection(true);
								candidatesIdentified = true;
								moreLevels = true;
								break;
							}
						} else {
							SystemLogger.getInstance().debug("NOT a CANDIDATE: "+ toc.get(i).getTitleText());
						}
					}
					if(!moreLevels) {
						break;
					}
				}
			}
		}
		if(candidatesIdentified) {
			//detect false positives
			int cant = 0;
			for(short i=0;i<toc.size() - 1;i++){
				if(toc.get(i).getSection()) {
					cant++;
				}
			}
			if(cant == 1) {
				for(short i=0;i<toc.size() - 1;i++){
					if(toc.get(i).getSection()) {	
						toc.get(i).setSection(false);
						SystemLogger.getInstance().debug("REMOVED   CANDIDATE: "+ toc.get(i).getTitleText());
					}
				}
			}
			return;
		}		
		
		float mostLeftX = 0;
		float mostLeftFontSize = 0;		
		//Part sections with page number: most-left level
		if(toc.size() > 1) {
			//1 identify most left X
			mostLeftX = toc.get(0).getPosX();
			for(short i=1;i<toc.size();i++){
				if(toc.get(i).getPosX() < mostLeftX) {
					mostLeftX = toc.get(i).getPosX();
					mostLeftFontSize = toc.get(i).getFontSize();
				}
			}
			
			SystemLogger.getInstance().debug("mostLeft: "+ mostLeftX);
			//2 get most left TOC entries
			for(short i=0;i<toc.size()-1;i++){
				if(BoundSimilarity.isInBound(toc.get(i).getPosX(),mostLeftX,
						toc.get(i).getFontSize(),mostLeftFontSize,0.2f)) {
					int pageDiff = toc.get(i+1).getPageNumber() - toc.get(i).getPageNumber();
					
					int diffYCoordinateTwoLines;
					if(toc.get(i).getConcatenatedPosY() != -1) {
						diffYCoordinateTwoLines = Math.round(toc.get(i+1).getPosY() - toc.get(i).getConcatenatedPosY());
					} else {
						diffYCoordinateTwoLines = Math.round(toc.get(i+1).getPosY() - toc.get(i).getPosY());
					}
					SystemLogger.getInstance().debug("POSIBLE CANDIDATE: "+ toc.get(i).getTitleText());
					SystemLogger.getInstance().debug("diffYCoordinateTwoLines: "+ diffYCoordinateTwoLines);
					SystemLogger.getInstance().debug("pageDiff: "+ pageDiff);
					
					//=  Math.round(toc.get(i+1).getPositionY() - lastLineYCoordinate);
					//if((pageDiff == 1 || pageDiff == 2) && !BoundSimilarity.isInBound(diffYCoordinateTwoLines, smallestSpace.floatValue(), toc.get(i).getFontSize(), toc.get(i+1).getFontSize(), 0.4f)) {
					if((pageDiff == 1 || pageDiff == 2) && !BoundSimilarity.isInYBound(toc.get(i), toc.get(i+1), 0.4)) {
						SystemLogger.getInstance().debug("L1: " +  toc.get(i).getTitleText());
						SystemLogger.getInstance().debug("L2: " +  toc.get(i+1).getTitleText());
						SystemLogger.getInstance().debug("CANDIDATE #2: "+ toc.get(i).getTitleText() + " --Y: " + toc.get(i).getPosY() + " --YC: " + toc.get(i).getConcatenatedPosY());
						toc.get(i).setSection(true);
						candidatesIdentified = true;
					}		
				}
			}
		}
		
		if(candidatesIdentified) {
			//detect false positives
			int cant = 0;
			for(short i=0;i<toc.size() - 1;i++){
				if(toc.get(i).getSection()) {
					cant++;
				}
			}
			if(cant == 1) {
				for(short i=0;i<toc.size() - 1;i++){
					if(toc.get(i).getSection()) {	
						toc.get(i).setSection(false);
						SystemLogger.getInstance().debug("REMOVED   CANDIDATE: "+ toc.get(i).getTitleText());
					}
				}
			}
			return;
		}
		
		//identify using page sequence and text
		for(short i=0;i<toc.size()-1;i++){
			List<Page> pages = RandomAccessToElements.getInstance().getPages();
			if(BoundSimilarity.isInBound(toc.get(i).getPosX(),mostLeftX,
					toc.get(i).getFontSize(),mostLeftFontSize,0.2f)) {
				int pageDiff = toc.get(i+1).getPageNumber() - toc.get(i).getPageNumber();
				if(pageDiff == 1 || pageDiff == 2){
					SystemLogger.getInstance().debug("POSIBLE CANDIDATE -page sequence- : "+ toc.get(i).getTitleText());
					int targetPageNumber = toc.get(i).getPageNumber();
					boolean exist = false;
					int indexNumber = -1;
					for(int pI=0; pI< pages.size(); pI++) {
						Page page = pages.get(pI);
						if(page != null && page.getPageNumber() == targetPageNumber) {
							exist = true;
							indexNumber = pI;
						}
					}
					//if no exact page number, check for one less
					if(!exist) {
						targetPageNumber++;
						for(int pI=0; pI< pages.size(); pI++) {
							Page page = pages.get(pI);
							if(page.getPageNumber() == targetPageNumber) {
								exist = true;
								indexNumber = pI;
							}
						}	
					}
					//now if page was found
					if(exist) {
						pages.get(indexNumber).extractText();
						String targetText = pages.get(indexNumber).getText();
						String sourceText = toc.get(i).getTitleText();
						Double sim = StringOperations.similarity(sourceText, targetText);
						if(sim > 0.9) {
							candidatesIdentified = true;
							toc.get(i).setSection(true);
							SystemLogger.getInstance().debug("Setting to PART: " + sourceText );
						}
					}
				}
			}
		}
		
		if(candidatesIdentified) {
			//detect false positives
			int cant = 0;
			for(short i=0;i<toc.size() - 1;i++){
				if(toc.get(i).getSection()) {
					cant++;
				}
			}
			if(cant == 1) {
				for(short i=0;i<toc.size() - 1;i++){
					if(toc.get(i).getSection()) {	
						toc.get(i).setSection(false);
						SystemLogger.getInstance().debug("REMOVED   CANDIDATE: "+ toc.get(i).getTitleText());
					}
				}
			}
			return;
		}
	}


	private TOCLogical modelOverIndentation(String bookID, List <TOC> toc, FormatExtractor parse, List<IndexElement> indexElements, resourceType type) throws IOException{
		//It creates the logical structure for the TOC: TableOfContentsLogical
		//TableOfContentsLogical
		TOCLogical tocLogical = new TOCLogical();
		int currentLevel = 1;
		
		//identify Section Parts
		identifySectionParts(toc);

		int sectionCounter = -1;

		String tab="	", buf="", contentBuffer="";

		//Stack
		Stack <TOC> order = new Stack<TOC>();
		Stack <TOCResource> resources = new Stack<TOCResource>();
		
		int chapLevels1 = 0;
		boolean withSections = false;
		
		//First level entry
		short startLoop = 1;
		if(toc.get(0).getSection()) {
			//add section
			listToModel.addSection(toc.get(0).getTitleText());
			sectionCounter++;
			TOCResource tmp1 = new TOCResource(TOCResourceType.SECTION, toc.get(0).getTitleText(), toc.get(0).getPageNumber());
			resources.push(tmp1);
			tocLogical.addChildren(tmp1);
			//add title
			order.push(toc.get(1));
			listToModel.add(toc.get(1).getTitleText(),toc.get(1).getPageNumber(),1, sectionCounter, toc.get(1).isErratum());
			TOCResource tmp2 = new TOCResource(TOCResourceType.CHAPTER, toc.get(1).getTitleText(), toc.get(1).getPageNumber());
			resources.push(tmp2);
			tmp1.addChildren(tmp2);
			SystemLogger.getInstance().log(toc.get(1).getTitleText()+"; Hierarchy level is : 1" + " ; Section is: " + sectionCounter);
			startLoop = 2;
			withSections = true;
		} else {
			order.push(toc.get(0));
			listToModel.add(toc.get(0).getTitleText(),toc.get(0).getPageNumber(),1, sectionCounter, toc.get(0).isErratum());
			TOCResource tmp1 = new TOCResource(TOCResourceType.CHAPTER, toc.get(0).getTitleText(), toc.get(0).getPageNumber());
			resources.push(tmp1);
			tocLogical.addChildren(tmp1);
			chapLevels1++;
			SystemLogger.getInstance().log(toc.get(0).getTitleText()+"; Hierarchy level is : 1" + " ; Section is: " + sectionCounter);
		}
	
		
//		/*TESTING*/
//		System.out.println("ORDER size: " + order.size() + ", top: " + order.peek().getTitleText());
//		/*TESTING*/

		//All other entries
		for(short i=startLoop;i<toc.size();i++){
//			/*TESTING*/
//			System.out.println(">***TOC: " + toc.get(i).getTitleText());
//			/*TESTING*/
			
			//if there are candidates for section for the first time after two level 1 chapters, then sections are false positive and should be avoided
			if(!withSections && chapLevels1 > 1) {
				
				//code to check if there is a TOC entry for a section without page numbers, then it is consider as erratum
				if(toc.get(i).getSection() && toc.get(i).getPageNumber() == 0 && (i+1) < toc.size() && toc.get(i+1).getPageNumber() != 0) {
					Integer[] pNumbers = this.cm.getPageNumbersOfBook(bookID).toArray(new Integer[1]);
					int negNumbers = 0;
					int posNumberBefore = 0;
					for(int pn = 0; pn < pNumbers.length; pn++) {
						if(pNumbers[pn] == toc.get(i+1).getPageNumber()) {
							//if there is a toc section without page number, and then there is a gap of pages between this section and the next one then
							//the section does not have page numbers and it is mark as erratum
							if(posNumberBefore + 1 == toc.get(i+1).getPageNumber() && negNumbers > 2) {
								toc.get(i).setErratum(true);
							}
						} else if (pNumbers[pn] >= 0 ) {
							posNumberBefore = pNumbers[pn];
							negNumbers = 0;
						} else {
							negNumbers++;
						}
					}
				}
				toc.get(i).setSection(false);
			}
			
			if(toc.get(i).getSection()) {
				listToModel.addSection(toc.get(i).getTitleText());
				sectionCounter++;
				TOCResource tmp1 = new TOCResource(TOCResourceType.SECTION, toc.get(i).getTitleText(), toc.get(i).getPageNumber());
				resources.clear();
				resources.push(tmp1);
				tocLogical.addChildren(tmp1);
				withSections = true;
				continue;
			}

			titles.add(toc.get(i).getTitleText());

			float diff =  (int)order.peek().getPosX() -(int)toc.get(i).getPosX();
			
			if((toc.get(i).getPageNumber() == 0 || toc.get(i).getPageNumber() == -1) && !toc.get(i).isErratum()) {
				//TODO
				SystemLogger.getInstance().debug("bad toc entry: "  + toc.get(i).getTitleText());
			}
			
			//case 1: current TOC entry is a child of TOP STACK entry
			if((int)order.peek().getPosX() < (int)toc.get(i).getPosX()
					&& diff < -5){
				
				/*TESTING*/
				SystemLogger.getInstance().debug("> CASE 1");
				/*TESTING*/

				for(byte a= 0 ; a< order.size(); a++)
					buf+=tab;

				order.push(toc.get(i));	
//				/*TESTING*/
//				System.out.println("ORDER size: " + order.size() + ", top: " + order.peek().getTitleText());
//				/*TESTING*/

				listToModel.add(toc.get(i).getTitleText(),toc.get(i).getPageNumber(), order.size(), sectionCounter, toc.get(i).isErratum());

				contentBuffer = fillToContent(contentBuffer, buf+toc.get(i).getTitleText()+"\n");

				buf="";

			} //case 2: current TOC entry is a sibling of TOP STACK entry
			else if((int)order.peek().getPosX() == (int)(toc.get(i).getPosX())
					|| diff >= -5 && diff <= 5){

				if(Math.abs(order.peek().getFontSize()) - Math.abs(toc.get(i).getFontSize()) > 1){
					//case 2.1: difference in font size PEEK is BIGGER
					/*TESTING*/
					SystemLogger.getInstance().debug("> CASE 2.1");
//					System.out.println("order.peek() " + order.peek().getTitleText());
//					System.out.println("order.peek() fs " + order.peek().getFontSize());
//					System.out.println("toc.get(i)  " + toc.get(i).getTitleText());
//					System.out.println("toc.get(i) fs " + toc.get(i).getFontSize());
//					System.out.println("> CASE 2.1");
					/*TESTING*/
					for(byte a= 0 ; a< order.size(); a++)
						buf+=tab;
			
					order.push(toc.get(i));		
					/*TESTING*/
					SystemLogger.getInstance().debug("ORDER size: " + order.size() + ", top: " + order.peek().getTitleText());
					/*TESTING*/

					listToModel.add(toc.get(i).getTitleText(),toc.get(i).getPageNumber(), order.size(), sectionCounter, toc.get(i).isErratum());
					
					contentBuffer = fillToContent(contentBuffer, buf+toc.get(i).getTitleText()+"\n");

					buf="";			
				}
				else if(Math.abs(order.peek().getFontSize()) - Math.abs(toc.get(i).getFontSize()) < -1){
					//case 2.2  difference in font size PEEK is SMALLER
					/*TESTING*/
					SystemLogger.getInstance().debug("> CASE 2.2");
					/*TESTING*/
					while(!order.isEmpty() && diff > 5  ){

						order.pop();	

						if(!order.isEmpty())
							diff= (int)order.peek().getPosX() - (int)toc.get(i).getPosX();

					}

					while(!order.isEmpty()
							&& Math.abs((int)order.peek().getFontSize()) < Math.abs((int)toc.get(i).getFontSize()) 
							&& (int)order.peek().getPosX() == (int)toc.get(i).getPosX())
						order.pop();
				

					while(!order.isEmpty() 
							&& !order.peek().isBold() && toc.get(i).isBold() 
							&& (int)order.peek().getPosX() == (int)toc.get(i).getPosX())
						order.pop();

					if( !order.isEmpty() )
						if( (int)order.peek().getPosX() == (int)toc.get(i).getPosX() 
								|| order.peek().isBold() && toc.get(i).isBold())
							order.pop();

					for(byte a= 0 ; a< order.size(); a++)
						buf+=tab;

					order.push(toc.get(i));
					/*TESTING*/
					SystemLogger.getInstance().debug("ORDER size: " + order.size() + ", top: " + order.peek().getTitleText());
					/*TESTING*/

					listToModel.add(toc.get(i).getTitleText(),toc.get(i).getPageNumber(), order.size(), sectionCounter, toc.get(i).isErratum());

					contentBuffer = fillToContent(contentBuffer, buf+toc.get(i).getTitleText()+"\n");

					buf="";				

				}
				else{
					//case 2.3 SAME FONT
					if(order.peek().isBold() && !toc.get(i).isBold()){
						//case 2.3.1 difference in BOLD TOP and CURRENT
						/*TESTING*/
						SystemLogger.getInstance().debug("> CASE 2.3.1");
						SystemLogger.getInstance().debug("> order.peek(): " + order.peek().getTitleText());
						SystemLogger.getInstance().debug("> order.peek().isBold(): " + order.peek().isBold());
						SystemLogger.getInstance().debug("> toc.get(i).isBold(): " + toc.get(i).isBold());
						/*TESTING*/
						for(byte a= 0 ; a< order.size(); a++)
							buf+=tab;
						
						order.push(toc.get(i));
						/*TESTING*/
						SystemLogger.getInstance().debug("ORDER size: " + order.size() + ", top: " + order.peek().getTitleText());
						/*TESTING*/

						listToModel.add(toc.get(i).getTitleText(),toc.get(i).getPageNumber(), order.size(), sectionCounter,toc.get(i).isErratum());

						contentBuffer = fillToContent(contentBuffer, buf+toc.get(i).getTitleText()+"\n");

						buf="";	

					}else{
						//case 2.3.2 same BOLD
//						/*TESTING*/
						SystemLogger.getInstance().debug("> CASE 2.3.2");
						while(!order.isEmpty() 
								&& !order.peek().isBold() && toc.get(i).isBold() 
								&& (int)order.peek().getPosX() == (int)toc.get(i).getPosX())
							order.pop();
						
						while(!order.isEmpty() && diff > 5 ){
							order.pop();

							if(!order.isEmpty())
								diff= (int)order.peek().getPosX() - (int)toc.get(i).getPosX();
						}

						while(!order.isEmpty()
								&& Math.abs((int)order.peek().getFontSize()) < Math.abs((int)toc.get(i).getFontSize())
								&& (int)order.peek().getPosX() == (int)toc.get(i).getPosX())
							order.pop();
					
						if( !order.isEmpty() )
							order.pop();

						for(byte a= 0 ; a< order.size(); a++)
							buf+=tab;
						
						order.push(toc.get(i));
//						/*TESTING*/
//						System.out.println("ORDER size: " + order.size() + ", top: " + order.peek().getTitleText());
//						/*TESTING*/

						listToModel.add(toc.get(i).getTitleText(),toc.get(i).getPageNumber(), order.size(), sectionCounter, toc.get(i).isErratum());

						contentBuffer = fillToContent(contentBuffer, buf+toc.get(i).getTitleText()+"\n");

						buf="";
					}
				}
			} // closes case 2
			else{
				/*TESTING*/
				SystemLogger.getInstance().debug("> CASE 3 desending ");
				/*TESTING*/
				
				while(!order.isEmpty() && diff > 5 ){
					order.pop();

					if(!order.isEmpty())
						diff= (int)order.peek().getPosX() - (int)toc.get(i).getPosX();
				}

				while(!order.isEmpty() 
						&& Math.abs((int)order.peek().getFontSize()) < Math.abs((int)toc.get(i).getFontSize()) 
						&& (int)order.peek().getPosX() == (int)toc.get(i).getPosX())
					order.pop();

				while(!order.isEmpty() 
						&& !order.peek().isBold() && toc.get(i).isBold() 
						&& (int)order.peek().getPosX() == (int)toc.get(i).getPosX())
					order.pop();
				

				if( !order.isEmpty() )
					if( (int)order.peek().getPosX() == (int)toc.get(i).getPosX() 
						|| diff <= 5 && diff >= - 5 || order.peek().isBold() && toc.get(i).isBold() )
						order.pop();
					
				for(byte a= 0 ; a< order.size(); a++)
					buf+=tab;
				
//					ldw = new LineDataContainer(toc.get(i).getWordAt(0).getFontName(),toc.get(i).getFontSize(),toc.get(i).getStartPositionX(),toc.get(i).getWordAt(0).isBold());
				order.push(toc.get(i));
//					/*TESTING*/
//					System.out.println("ORDER size: " + order.size() + ", top: " + order.peek().getTitleText());
//					/*TESTING*/

				listToModel.add(toc.get(i).getTitleText(),toc.get(i).getPageNumber() , order.size(), sectionCounter, toc.get(i).isErratum());

				contentBuffer = fillToContent(contentBuffer, buf+toc.get(i).getTitleText()+"\n");

				buf="";
			}
			
			SystemLogger.getInstance().log(buf+toc.get(i).getTitleText()+"; Hierarchy level is : "+order.size() + " ; Section is: " + sectionCounter + " X: " + toc.get(i).getPosX());
			
			//TOCLogical
			TOCResource tmp1 = new TOCResource(TOCResourceType.CHAPTER, toc.get(i).getTitleText(), toc.get(i).getPageNumber());
			if(!withSections && order.size() == 1) {
				chapLevels1++;
			}
			if(order.size() == currentLevel) {
				SystemLogger.getInstance().debug("#1: Resources size: " + resources.size());
				//current is sibling
				resources.pop();
				if(resources.size() == 0) {
					tocLogical.addChildren(tmp1);
				} else {
					resources.peek().addChildren(tmp1);
				}
			} else if(order.size() > currentLevel) {
				SystemLogger.getInstance().debug("#2: Resources size: " + resources.size());
				//current is children
				resources.peek().addChildren(tmp1);		
			} else if (order.size() < currentLevel) {
				SystemLogger.getInstance().debug("#3: Resources size: " + resources.size());
				int diffLevel = (currentLevel - order.size()) + 1;
				while(diffLevel > 0 && resources.size() > 0) {
					if(resources.peek().getType() == TOCResourceType.SECTION) {
						break;
					}
					resources.pop();
					diffLevel--;
				}
				SystemLogger.getInstance().debug("#3 after: Resources size: " + resources.size());
				if(resources.size() == 0) {
					tocLogical.addChildren(tmp1);
				} else {
					resources.peek().addChildren(tmp1);
				}
			}
			resources.push(tmp1);
			currentLevel = order.size();
			//if(toc.get(i).getTitleText().equals("11.1 some def‌initions"))	
			//	System.exit(0);
		}

		return tocLogical;
	}

/**
 * 
 * @param bookID
 * @param toc
 * @param parse
 * @param indexTerms
 * @param type
 */

	private void modelOverOrder(String bookID, List <TOC> toc, FormatExtractor parse, List<IndexElement> indexElements, resourceType type){


		Stack <String> order = new Stack<String>();
		
		int chapterOrdering = -1;

		order.push(toc.get(0).getChapterPrefix());

		listToModel.add(toc.get(0).getTitleText(),toc.get(0).getPageNumber(),1, chapterOrdering, toc.get(0).isErratum());

		for(int i = 1; i < toc.size(); i++){
			if(toc.get(i).getChapterPrefix().equals("0")){

				if(!toc.get(i-1).getChapterPrefix().equals("0")){

					listToModel.add(toc.get(i).getTitleText(),toc.get(i).getPageNumber(), toc.get(i-1).getChapterPrefix().length()+1, chapterOrdering, toc.get(i).isErratum());
					order.push(toc.get(i).getChapterPrefix());		
				}
				else{

					order.pop();

					listToModel.add(toc.get(i).getTitleText(),toc.get(i).getPageNumber(), order.peek().length()+1, chapterOrdering, toc.get(i).isErratum());
					order.push(toc.get(i).getTitleText());
				}
			}
			else if(toc.get(i).getChapterPrefix().length() == 1 ){

				listToModel.add(toc.get(i).getTitleText(),toc.get(i).getPageNumber(),1, chapterOrdering, toc.get(i).isErratum());

				order.clear();
				order.push(toc.get(i).getChapterPrefix());
				
			}				
			else if(toc.get(i).getChapterPrefix().length() > order.peek().length()){

				listToModel.add(toc.get(i).getTitleText(),toc.get(i).getPageNumber(), toc.get(i).getChapterPrefix().length(), chapterOrdering, toc.get(i).isErratum());

				order.push(toc.get(i).getChapterPrefix());

			}
			else if(toc.get(i).getChapterPrefix().length() == order.peek().length()){

				listToModel.add(toc.get(i).getTitleText(),toc.get(i).getPageNumber(), toc.get(i).getChapterPrefix().length(), chapterOrdering,toc.get(i).isErratum());

				order.pop();
				order.push(toc.get(i).getChapterPrefix());

			}
			else{

				while(order.peek().length() >= toc.get(i).getChapterPrefix().length())
					order.pop();
				
				listToModel.add(toc.get(i).getTitleText(),toc.get(i).getPageNumber(), toc.get(i).getChapterPrefix().length(), chapterOrdering,toc.get(i).isErratum());

				order.push(toc.get(i).getTitleText());			
			}
			
			SystemLogger.getInstance().log(toc.get(i).getTitleText()+"; Hierarchy level is : "+order.size());
		}	
	}

	
	/**
	 * 
	 * @param bookID
	 * @param toc
	 * @param parse
	 * @param indexTerms
	 * @param type
	 */
	private TOCLogical modelOverFlat(String bookID, List <TOC> toc, FormatExtractor parse, List<IndexElement> indexElements, resourceType type){
		TOCLogical tocLogical = new TOCLogical();
		int sectionCounter = -1;
		TOCResource topResource = null;
		
		//identify Section Parts
		identifySectionParts(toc);
		
		for(int i = 0; i < toc.size(); i++){
			
			if(toc.get(i).getSection()) {
				listToModel.addSection(toc.get(0).getTitleText());
				sectionCounter++;
				TOCResource tmp1 = new TOCResource(TOCResourceType.SECTION, toc.get(i).getTitleText(), toc.get(i).getPageNumber());
				topResource = tmp1;
				tocLogical.addChildren(tmp1);
			} else {
				listToModel.add(toc.get(i).getTitleText(),toc.get(i).getPageNumber(), 1, sectionCounter, toc.get(i).isErratum());
				TOCResource tmp1 = new TOCResource(TOCResourceType.CHAPTER, toc.get(i).getTitleText(), toc.get(i).getPageNumber());
				if(topResource != null) {
					topResource.addChildren(tmp1);
				} else {
					tocLogical.addChildren(tmp1);
				}
				SystemLogger.getInstance().log(toc.get(i).getTitleText()+"; Hierarchy level is : 1" + " ; Section is: " + sectionCounter);
			}
		}
		
		System.out.println(tocLogical);
		//System.exit(0);
		return tocLogical;
	}
	
	public BookStructure getBookStructure() {
		return this.bookStructure;
	}

}
