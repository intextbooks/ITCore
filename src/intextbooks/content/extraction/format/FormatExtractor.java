package intextbooks.content.extraction.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import intextbooks.RandomAccessToElements;
import intextbooks.SystemLogger;
import intextbooks.content.ContentManager;
import intextbooks.content.extraction.ExtractorController.resourceType;
import intextbooks.content.extraction.Utilities.BoundSimilarity;
import intextbooks.content.extraction.Utilities.StringOperations;
import intextbooks.content.extraction.Utilities.WordListCheck;
import intextbooks.content.extraction.buildingBlocks.format.ElementBlock;
import intextbooks.content.extraction.buildingBlocks.format.Line;
import intextbooks.content.extraction.buildingBlocks.format.LineDataContainer;
import intextbooks.content.extraction.buildingBlocks.format.Page;
import intextbooks.content.extraction.buildingBlocks.format.ResourceUnit;
import intextbooks.content.extraction.buildingBlocks.format.Slide;
import intextbooks.content.extraction.buildingBlocks.format.Text;
import intextbooks.content.extraction.buildingBlocks.structure.BookSectionResource;
import intextbooks.content.extraction.buildingBlocks.structure.BookSectionType;
import intextbooks.content.extraction.buildingBlocks.structure.TOC;
import intextbooks.content.extraction.buildingBlocks.structure.TextBlock;
import intextbooks.content.extraction.format.lists.ListExtractor;
import intextbooks.content.extraction.format.lists.ListExtractorFactory;
import intextbooks.content.extraction.structure.TableOfContentsExtractor;
import intextbooks.content.models.formatting.FormattingContainer;
import intextbooks.content.models.formatting.FormattingContainer.RoleLabel;
import intextbooks.content.models.formatting.FormattingDictionary;
import intextbooks.exceptions.BookWithoutPageNumbersException;
import intextbooks.exceptions.EarlyInterruptionException;
import intextbooks.exceptions.TOCNotFoundException;
import intextbooks.ontologie.LanguageEnum;
import intextbooks.tools.utility.SimpleHTTPRequest;



public class FormatExtractor{
	
	private enum pageNumPos {TOP, BOTTOM, NONE} ;
	
	byte tableOfContentsStartPageIndx = -1;
	float tableOfContentsTitlePos = -1;
	float tableOfContentsTitleFontSize = -1;
	byte firstChapterStartIndex = -1;
	
	pageNumPos pos = pageNumPos.NONE;
	pageNumPos posContentOrder = pageNumPos.NONE;
	
	public String resourceName="";
	
	private ArrayList<Integer> pageNumbers = new ArrayList<Integer>();

	private String rawText;
	private Vector <Page> pages;
	private Vector <Page> pagesContentOrder;
	private Vector <Slide> slides;
	FormattingDictionary lOS;
	Map<String,String> metadata = new HashMap<String,String>();
	
	private	List <TOC> toc = new Vector <TOC>(); 
	private List<BookSectionResource> bookContent;
	
	private float pageHeight=-1, pageWidth=-1;
	
	private LineDataContainer pageNumLine;
	private LineDataContainer pageNumLineContentOrder;
	private LineDataContainer pageNumLineExtra;
	private Line TOCTitleLine;
	private List<Integer> tocPages;
	private byte pageNumOffset=-1;
	private float tocMostleft1=-1, tocMostleft2=9999;

	private resourceType type;
	
	private ContentManager cm = ContentManager.getInstance();	
	
	private float textBodyFontSize;
	private Map<String, PDFont> fonts = new HashMap<String, PDFont>();
	
	private boolean centerAlignmentValid = false;
	
	private final String OpenLibraryISBNService = "https://openlibrary.org/api/books?jscmd=data&format=json&bibkeys=ISBN:";

	public FormatExtractor(String resourceID, String filePath, resourceType type) throws IOException, TOCNotFoundException, NullPointerException, BookWithoutPageNumbersException {
	
		File file  = new File(filePath); 
		
		String s = file.getName();
		s = s.replace(".pdf","");
		resourceName = s;
		
		try {
			PDDocument document = PDDocument.load(file);
	           	
			this.type = type;
			
			if(type.equals(resourceType.BOOK))
				processBook(resourceID, document);
			else{
	            System.err.println( "Error: Undefined document type." );
			}
			
			ListExtractor listExtractor = ListExtractorFactory.createListExtractor(resourceID, type);
			
			if (listExtractor != null) {
				
				SystemLogger.getInstance().log("Start list extraction");
				listExtractor.extractLists(this.getPagesAsResourceUnits());
				listExtractor.persistLists();
				SystemLogger.getInstance().log("List extraction ended");
				
			}
			
			SystemLogger.getInstance().log("Format extraction ended");
			
			document.close();
			
		} catch( InvalidPasswordException e ){
            System.err.println( "Error: Document is encrypted with a non-empty password." );
            SystemLogger.getInstance().log(e.toString());
            e.printStackTrace();
        }
		catch (IOException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}
	}

	public void processBook (String bookID, PDDocument document) throws IOException, TOCNotFoundException, NullPointerException, BookWithoutPageNumbersException{

		//Lang of Textbook
		LanguageEnum lang = cm.getBookLanguage(bookID);
		
		//Parsing: Reading Order
		PdfTextExtractor textExtractor = new PdfTextExtractor();
		
		//Process each page: get words and then lines. For each page a ResourceUnit is created
		textExtractor.processText(bookID, document);
		textExtractor.closeDocument();

		//Get text body font size
		this.textBodyFontSize = textExtractor.getTextBodyFontSize();
				
		//converts ResourceUnits to Pages
		SystemLogger.getInstance().log("Converting ResourceUnits to pages");
		pages = textExtractor.convertToPage();
		RandomAccessToElements.getInstance().setPages(pages);
		
//		/* TESTING */
//		for(Line l: pages.get(18).getLines()) {
//			//System.out.println("L: " + l.getText() + " Y: " + l.getPositionY() + " Height: " + l.getLineHeight() + " FS: "  + l.getFontSize());
//			System.out.println("L: " + l.getText() + " SX: " + l.getStartPositionX() + " EX: " + l.getEndPositionX() + " Y: " + l.getPositionY() + " BOLD: " + l.isBold() + " Size: " + l.size());
//			for(Text w : l.getWords()) {
//				System.out.println("\tw: " + w.getText() + " SX: " + w.getStartPositionX() + " EX: " + w.getEndPositionX() + " BOLD: " + w.isBold());
//			}
//		}
//		System.exit(0);
//		/* TESTING */

		//Text from all pages concatenated: for evaluation, not need it for normal flow
		//rawText = textExtractor.getRawText();
		
		//General PDF data
		this.pageHeight = textExtractor.getHeight();
		this.pageWidth = textExtractor.getWidth();
		
		SystemLogger.getInstance().log("Resource text process ended");
		
		//get metadata
		SystemLogger.getInstance().log("Getting metadata");
		obtainMetadata();
		SystemLogger.getInstance().log("Getting metadata ended");	
	
		//removes a recurrent line at the top or bottom of each page
		SystemLogger.getInstance().log("Removing repeating lines through pages started");
		removeRepeatingLines();
		SystemLogger.getInstance().log("Removing repeating lines through pages ended");	
		
		//removes last pages if NULL
		trimLastPages();
		
		//get where the page numbers are at a page (top, bottom), get the index of the TOC page, and page number of first chapter (index and page number)
		SystemLogger.getInstance().log("Initial conquest process started");
		InitialConquest();	
		SystemLogger.getInstance().log("Initial conquest process ended");
		
		//setting in each page (index page, physical number) the page number (logical number). Numbers at the beginning of pages are corrected. 
		SystemLogger.getInstance().log("Numbering pages started");		
		numberingPages(bookID);
		SystemLogger.getInstance().log("Numbering pages ended");
		
		//removes the line with the page number for each page
		SystemLogger.getInstance().log("Page number removal started");	
		removePageNumLine(pages, pos, pageNumLine);
		SystemLogger.getInstance().log("Page number removal ended");

	
		//Style Library: gets information about the format of each page-line-word
		SystemLogger.getInstance().log("Bridging extracted information to backbone started");
		contentManagerBridge(bookID,type);
		lOS = this.cm.getStyleLibrary(bookID);
		lOS.constructStyleLibrary();		
		SystemLogger.getInstance().log("Bridging extracted information to backbone ended");
		
		/* TESTING */
		//sL.printData();
		//lOS.printRoleLabels();
		//System.exit(0);
		//this.bookContent = identifyTextBlocks(bookID, lOS);
//		System.out.println("#######");
//		for(BookSectionResource s: this.bookContent) {
//			System.out.println(s);
//		}
//		System.exit(0);
//		/* TESTING */
		
		//left normalization for toc pages
		SystemLogger.getInstance().log("TOC: Page most left normalization started");	
		normalizeTocStartX(tableOfContentsStartPageIndx, firstChapterStartIndex, TOCTitleLine, lOS.getBodyFontSize() );
		SystemLogger.getInstance().log("TOC: Page most left normalization ended");
		
		//extracts the TOC -- each topic has the pageNumber (logical), and main chapters are marked
		SystemLogger.getInstance().log("Table of Contents extraction started");
		Pair<List<Integer>, ArrayList<TOC>> resultTOC = TableOfContentsExtractor.extractToc(tableOfContentsStartPageIndx, pages, TOCTitleLine, textBodyFontSize, lang);
		tocPages = resultTOC.getLeft();
		toc = resultTOC.getRight();
		SystemLogger.getInstance().log("Table of Contents extraction ended");
	
//		/*TESTING*/
//		Iterator<TOC> it = toc.iterator();
//		System.out.println("@@@TOC ENTRIES ------ after extractTOC");
//		while(it.hasNext()) {
//			TOC t = it.next();
//			System.out.println("^T: " + t.getTitleText() + " P: " + t.getPageNumber() + t.getFontSize());
//			//System.out.println("^^PosX" + t.getPosX());
//			//System.out.println("");
//		}	
//		System.exit(0);
//		/*TESTING*/
//		/*TESTING*/
//		System.out.println("*****TOC******");
//		Iterator<TOC> it = toc.iterator();
//		int i = 0;
//		while(it.hasNext()) {
//			TOC t = it.next();
//			System.out.println("#:" + i++ + ": " + t.getTitleText() + " -pageNumber:" + t.getPageNumber() + " -chapter: " + t.getChapterPrefix());
////			System.out.println("*Title: " + t.getTitleText());
////			System.out.println("**PageIndex: " + t.getPageIndex());
////			System.out.println("**PageNumber: " + t.getPageNumber());
////			System.out.println("**PosX: " + t.getPosX());
////			System.out.println("**FontSize: " + t.getFontSize());
////			System.out.println("**Bold: " + t.isBold());
//		}
//		System.exit(0);
//		/*TESTING*/

	}

	private void trimLastPages() {
		if(pages!=null)			
			for(int i = pages.size()-1; i>=pages.size()/2 ; i--  ){
				
				if(pages.get(i) != null )
					break;			
				else {
					pages.remove(i);
				}
			}
	}

	public Line getTOCTitleLine() {
		return this.TOCTitleLine;
	}
	
	
	public void findExtraPageNumLine(short start, short lookupRange, pageNumPos pos) {
		for(short i= start ; i < lookupRange && i + 2 < lookupRange; i++){

			if(pages.get(i)!=null){
				
				//@i.alpizarchacon fix to the i+2 page is null, fix the next one that is not
				int j = i + 2;
				for(; j <lookupRange; j=j+2) {
					if (pages.get(j) != null) {
						break;
					}
				}
				
//				/* TESTING */
//				System.out.println("Page#, testing i= " + i +  " --j= " + j);
//				/* TESTING */

				if(pos.equals(pageNumPos.TOP)) {
					//try to locate page number at the top of the page
					if(StringUtils.isNumeric(pages.get(i).getLineAt(0).getWordAt(0).getText()) && StringUtils.isNumeric(pages.get(j).getLineAt(0).getWordAt(0).getText()) ){
						
						if(Integer.parseInt(pages.get(i).getLineAt(0).getWordAt(0).getText()) + 2 == Integer.parseInt(pages.get(j).getLineAt(0).getWordAt(0).getText())){			

//							/* TESTING */
//							System.out.println("@@@@@@@@@@@@@");
//							System.out.println("p: " + i);
//							System.out.println("line 1 size: " + pages.get(i).getLineAt(0).size());
//							System.out.println(pages.get(i).getLineAt(0).getText());
//							System.out.println(pages.get(i).getLineAt(1).getText());
//							System.out.println(pages.get(i).getLineAt(2).getText());
//							System.out.println(pages.get(i).getLineAt(pages.get(i).size()-1).getText());
//							System.out.println(pages.get(i).getLineAt(0).getWordAt(0).getText());
//							System.out.println(pages.get(i).getLineAt(0).getWordAt(0).getStartPositionX());
//							/* TESTING */
							pageNumLineExtra=new LineDataContainer(pages.get(i).getLineAt(0).getFontName(), pages.get(i).getLineAt(0).getWordAt(0).getFontSize(), pages.get(i).getLineAt(0).getStartPositionX(), pages.get(i).getLineAt(0).getPositionY());
							return;
						}
					}

					int lastWord = pages.get(i).getLineAt(0).size() -1;
					int lastWordJ = pages.get(j).getLineAt(0).size() -1;
					
					if(StringUtils.isNumeric(pages.get(i).getLineAt(0).getWordAt(lastWord).getText()) && StringUtils.isNumeric(pages.get(j).getLineAt(0).getWordAt(lastWordJ).getText()) ){
						
						if(Integer.parseInt(pages.get(i).getLineAt(0).getWordAt(lastWord).getText()) + 2 == Integer.parseInt(pages.get(j).getLineAt(0).getWordAt(lastWordJ).getText())){			

//							/* TESTING */
//							System.out.println("######################");
//							System.out.println("p: " + i);
//							System.out.println(pages.get(i).getLineAt(0).getWordAt(lastWord).getText());
//							System.out.println(pages.get(i).getLineAt(0).getWordAt(lastWord).getStartPositionX());
//							/* TESTING */
							pageNumLineExtra=new LineDataContainer(pages.get(i).getLineAt(0).getFontName(), pages.get(i).getLineAt(0).getWordAt(lastWord).getFontSize(), pages.get(i).getLineAt(0).getWordAt(lastWord).getStartPositionX(), pages.get(i).getLineAt(0).getPositionY());
							return;
						}
					}
					
				}
				
				if(pos.equals(pageNumPos.BOTTOM)) {
					//try to locate page number at the bottom of the page
					if(StringUtils.isNumeric(pages.get(i).getLineAt(pages.get(i).size()-1).getWordAt(0).getText()) && StringUtils.isNumeric(pages.get(j).getLineAt(pages.get(j).size()-1).getWordAt(0).getText()) ){
						
						if(Integer.parseInt(pages.get(i).getLineAt(pages.get(i).size()-1).getWordAt(0).getText()) + 2 == Integer.parseInt(pages.get(j).getLineAt(pages.get(j).size()-1).getWordAt(0).getText())){			

							pageNumLineExtra=new LineDataContainer(pages.get(i).getLineAt(pages.get(i).size()-1).getFontName(), pages.get(i).getLineAt(pages.get(i).size()-1).getWordAt(0).getFontSize(), pages.get(i).getLineAt(pages.get(i).size()-1).getStartPositionX(), pages.get(i).getLineAt(pages.get(i).size()-1).getPositionY());
							return;
						}
					}
					
					
					int lastWord = pages.get(i).getLineAt(pages.get(i).size()-1).size() -1;
					int lastWordJ = pages.get(j).getLineAt(pages.get(j).size()-1).size() -1;
					
					if(StringUtils.isNumeric(pages.get(i).getLineAt(pages.get(i).size()-1).getWordAt(lastWord).getText()) && StringUtils.isNumeric(pages.get(j).getLineAt(pages.get(j).size()-1).getWordAt(lastWordJ).getText()) ){
						
						if(Integer.parseInt(pages.get(i).getLineAt(pages.get(i).size()-1).getWordAt(lastWord).getText()) + 2 == Integer.parseInt(pages.get(j).getLineAt(pages.get(j).size()-1).getWordAt(lastWordJ).getText())){			

							pageNumLineExtra=new LineDataContainer(pages.get(i).getLineAt(pages.get(i).size()-1).getFontName(), pages.get(i).getLineAt(pages.get(i).size()-1).getWordAt(lastWord).getFontSize(), pages.get(i).getLineAt(pages.get(i).size()-1).getWordAt(lastWord).getStartPositionX(), pages.get(i).getLineAt(pages.get(i).size()-1).getPositionY());
							return;
						}
					}
				}
			}
		}
	}
	
	private boolean FlexibleTOCStartRecognition() {
		//variables
		int localTableOfContentsStartPageIndx = -1;
		int localTableOfContentsStartLineIndx = -1;
		
		//First: recognize title
		int endLookingForTOC = pages.size() > 20 ? 40 : pages.size();
		for(byte i=0 ; i <endLookingForTOC ; i++){
			if ( pages.elementAt(i) != null && pages.elementAt(i).size() > 5) {
				for(short l=0; l < pages.elementAt(i).size(); l++) {
					System.out.println(pages.elementAt(i).getLineAt(l).getText());
					if(WordListCheck.containstableOfContents(pages.elementAt(i).getLineAt(l).getText()) && !WordListCheck.omitTableOfContents(pages.elementAt(i).getLineAt(l).getText())
							&& pages.elementAt(i).getLineAt(l).getFontSize() != textBodyFontSize) {
						localTableOfContentsStartPageIndx = i;
						localTableOfContentsStartLineIndx = l;
						break;
					}
				}
			}
			
			//Second recognize structure 
			if(localTableOfContentsStartPageIndx != -1) {
//				/* TESTING */
//				System.out.println("localTableOfContentsStartPageIndx: " + localTableOfContentsStartPageIndx);
//				System.out.println("localTableOfContentsStartLineIndx: " + localTableOfContentsStartLineIndx + " " + pages.get(localTableOfContentsStartPageIndx).getLineAt(localTableOfContentsStartLineIndx).getText());
//				System.exit(0);
//				/* TESTING */
				int nullCount = 0;
				int numberCount = 0;
				int range = 0;		
				
				Page page = pages.elementAt(localTableOfContentsStartPageIndx);
				TOCTitleLine = page.getLineAt(localTableOfContentsStartLineIndx);

				// pages.elementAt(i).size() -2 to take into account possible headers
				for(int l = localTableOfContentsStartLineIndx + 1; l < page.size() -2; l++){

					String lastWord = page.getLineAt(l).getLastWordText();
					lastWord = TableOfContentsExtractor.getTheNumberAtTheEnd(lastWord);

					if(lastWord == null){
						nullCount++;
					}
					else{
						numberCount++;
					}
				}

//				/* TESTING */
//				System.out.println("nullCount: " + nullCount);
//				System.out.println("numberCount: " + numberCount);
//				/* TESTING */
			
				//+1 to count for line of authors or incomplete lines
				if(nullCount > (numberCount + 1)) {
					return false;
				} else {
					int offset = 0;
					if( BoundSimilarity.isInBound( pages.elementAt(localTableOfContentsStartPageIndx).getLineAt(0).getWordAt(0).getPositionY(), pageNumLine.linePosY, pageNumLine.fontSize, pages.elementAt(localTableOfContentsStartPageIndx).getLineAt(0).getWordAt(0).getFontSize(), 0.6f ) 
							|| pages.elementAt(localTableOfContentsStartPageIndx).getLineAt(0).getWordAt(0).getText().toUpperCase().matches(TableOfContentsExtractor.regexRomanNumber)
							|| pages.elementAt(localTableOfContentsStartPageIndx).getLineAt(0).getWordAt(pages.elementAt(localTableOfContentsStartPageIndx).getLineAt(0).size()-1).getText().toUpperCase().matches(TableOfContentsExtractor.regexRomanNumber)
							|| (pages.elementAt(localTableOfContentsStartPageIndx).getLineAt(0).size() == 1 && StringUtils.isNumeric(pages.elementAt(localTableOfContentsStartPageIndx).getLineAt(0).getWordAt(0).getText())))
						offset=1; //there is number line in the page, the first word should be in line #1
					else
						offset=0; //there is NO number line in the page, the first word should be in line #0
					
					//delete lines
					int remove = localTableOfContentsStartLineIndx - offset;
					while(remove > 0 && pages.elementAt(localTableOfContentsStartPageIndx).size() > 0) {
						pages.elementAt(localTableOfContentsStartPageIndx).removeLineAt(offset);
						remove--;
						
					}
					//remove extra lines 
					for(int line = offset+1; line < pages.elementAt(localTableOfContentsStartPageIndx).size(); line++) {
						if(pages.elementAt(localTableOfContentsStartPageIndx).getLineAt(line).size() <= 2) {
							pages.elementAt(localTableOfContentsStartPageIndx).removeLineAt(line);
							line--;
						} else {
							break;
						}
					}
					
//					/* TESTING */
//					System.out.println(">>>>>>>>>>>>>>>>>>>");
//					for(int line = 0; line < pages.elementAt(localTableOfContentsStartPageIndx).size(); line++) {
//						System.out.println(pages.elementAt(localTableOfContentsStartPageIndx).getLineAt(line).getText());
//					}
//					/* TESTING */
					
					this.tableOfContentsStartPageIndx = (byte) localTableOfContentsStartPageIndx;
					return true;	
				}	
			}
		}
		return false;
		
	}
	
	
	private int getFirstTOCLine() {
		int offset = 0;
		int i = this.tableOfContentsStartPageIndx;
		
//		/*TESTING*/
//		System.out.println("tableOfContentsStartPageIndx " + tableOfContentsStartPageIndx);
//		System.out.println("pages.elementAt(i).getLineAt(0).size() " + pages.elementAt(i).size());
//		/*TESTING*/
		
		//@i.alpizarchacon two more conditions added 
		if(  pages.elementAt(i).getLineAt(0).size() > 0 && BoundSimilarity.isInBound( pages.elementAt(i).getLineAt(0).getWordAt(0).getPositionY(), pageNumLine.linePosY, pageNumLine.fontSize, pages.elementAt(i).getLineAt(0).getWordAt(0).getFontSize(), 0.6f ) 
				|| pages.elementAt(i).getLineAt(0).getWordAt(0).getText().toUpperCase().matches(TableOfContentsExtractor.regexRomanNumber)
				|| pages.elementAt(i).getLineAt(0).getWordAt(pages.elementAt(i).getLineAt(0).size()-1).getText().toUpperCase().matches(TableOfContentsExtractor.regexRomanNumber)
				|| (pages.elementAt(i).getLineAt(0).size() == 1 && StringUtils.isNumeric(pages.elementAt(i).getLineAt(0).getWordAt(0).getText())))
			offset=1; //there is number line in the page, the first word should be in line #1
		else
			offset=0; //there is NO number line in the page, the first word should be in line #0
		
		short firstChapterStartPage = -1;
		for(int j=offset+1 ; j<pages.elementAt(i).size(); j++){

			if( StringUtils.isNumeric(pages.elementAt(i).getLineAt(j).getWordAt(pages.elementAt(i).getLineAt(j).size()-1).getText())
					&& pages.elementAt(i).getLineAt(j).getText().lastIndexOf(" ") != -1 && !WordListCheck.matchesTableOfContents(pages.elementAt(i).getLineAt(j).getText().substring(0, pages.elementAt(i).getLineAt(j).getText().lastIndexOf(" ")))){

				
				firstChapterStartPage = Short.parseShort( pages.elementAt(i).getLineAt(j).getWordAt(pages.elementAt(i).getLineAt(j).size()-1).getText() );
				
//				/*TESTING*/
//				System.out.println("!!! 1 firstChapterStartPage: " + firstChapterStartPage);
//				SystemLogger.getInstance().log("*tableOfContentsStartPageIndx: " + tableOfContentsStartPageIndx);	
//				SystemLogger.getInstance().log("*firstChapterStartPage (first rule): " + firstChapterStartPage);
//				/*TESTING*/
				
				break;
			}
			else{
				
				//if the number is joined with dashes e.g., "....90"
				if(pages.elementAt(i).getLineAt(j).getWordAt(pages.elementAt(i).getLineAt(j).size()-1).getText().matches(".*[.][.]+[0-9]+")){
					
					String temp;
					temp = pages.elementAt(i).getLineAt(j).getWordAt(pages.elementAt(i).getLineAt(j).size()-1).getText().replaceAll("[.][.]+", " ");
					
					temp = temp.substring(temp.lastIndexOf(" ")+1);
					
					firstChapterStartPage = Short.parseShort(temp);
					
//					/*TESTING*/
//					System.out.println("!!! 2 firstChapterStartPage: " + firstChapterStartPage);
//					SystemLogger.getInstance().log("*tableOfContentsStartPageIndx: " + tableOfContentsStartPageIndx);	
//					SystemLogger.getInstance().log("*firstChapterStartPage (second rule): " + firstChapterStartPage);
//					/*TESTING*/
					
					break;
					
				} else {

					//Short temp = TableOfContentsExtractor.getPageNumberFromLineTOC(pages.elementAt(i).getLineAt(j).getWordAt(pages.elementAt(i).getLineAt(j).size()-1).getText());
					String num = TableOfContentsExtractor.getTheNumberAtTheEnd(pages.elementAt(i).getLineAt(j).getWordAt(pages.elementAt(i).getLineAt(j).size()-1).getText());
					if(num != null && pages.elementAt(i).getLineAt(j).size() != 1) {
						firstChapterStartPage = Short.parseShort(num);
						
//						/*TESTING*/
//						System.out.println("!!! 3 firstChapterStartPage: " + firstChapterStartPage);
//						/*TESTING*/
						
						break;
					}
				}
			}
		}
		this.firstChapterStartIndex = (byte) firstChapterStartPage;
		return firstChapterStartPage;
	}

	/**
	 * InitialConquest function defines for three things;
	 * 
	 * 1) Position and Formatting properties of  page number in book 
	 * 2) Index of Table of Contents, position of the Title
	 * 3) Index of first chapter 
	 * @throws TOCNotFoundException 
	 * @throws BookWithoutPageNumbersException 
	 * 
	 * */
	private void InitialConquest() throws TOCNotFoundException, BookWithoutPageNumbersException {
		
		

		short firstChapterStartPage = -1;

/**
 * Search for the page number and its position
 * 
 * <pos> = TOP, when the page number located at top of the page
 * <pos> = BOTTOM, when the page number located at bottom of the page   
 * <pos> = NONE, when there are no recognizable page numbers on the book
 * 
 * When page number found, assign the location properties into <pageNumLine>
 * 
 * */		
		
		short lookupRange = (short) (((pages.size()/2)+10 < pages.size()) ? (pages.size()/2)+10 : pages.size());

//		/*TESTING*/
//		System.out.println("# pages: " + pages);
//		System.out.println("# pages size: " + pages.size());
//		System.out.println("# lookupRange: " + lookupRange);
//		System.out.println("# starting: " + pages.size()/2);
//		/*TESTING*/

		for(short i= (short) (pages.size()/2) ; i + 2 < lookupRange; i++){
			if(pages.get(i)!=null){
				
				//@i.alpizarchacon fix to the i+2 page is null, fix the next one that is not
				int j = i + 2;
				for(; j <lookupRange; j++) {
					if (pages.get(j) != null) {
						break;
					}
				}
				
				//check for end of lookupRange
				if(j >= lookupRange) {
					return;
				}
				
				//try to locate page number at the top of the page
				if(StringUtils.isNumeric(pages.get(i).getLineAt(0).getWordAt(0).getText()) && StringUtils.isNumeric(pages.get(j).getLineAt(0).getWordAt(0).getText()) ){
					
					if(Integer.parseInt(pages.get(i).getLineAt(0).getWordAt(0).getText()) + 2 == Integer.parseInt(pages.get(j).getLineAt(0).getWordAt(0).getText())){			

						pageNumLine=new LineDataContainer(pages.get(i).getLineAt(0).getFontName(), pages.get(i).getLineAt(0).getWordAt(0).getFontSize(), pages.get(i).getLineAt(0).getStartPositionX(), pages.get(i).getLineAt(0).getPositionY());
						pos=pageNumPos.TOP;
						findExtraPageNumLine((short)(pages.size()/2 -1), lookupRange, pos);
					}
				} else {

					int lastWord = pages.get(i).getLineAt(0).size() -1;
					int lastWordJ = pages.get(j).getLineAt(0).size() -1;
					
					if(StringUtils.isNumeric(pages.get(i).getLineAt(0).getWordAt(lastWord).getText()) && StringUtils.isNumeric(pages.get(j).getLineAt(0).getWordAt(lastWordJ).getText()) ){
						
						if(Integer.parseInt(pages.get(i).getLineAt(0).getWordAt(lastWord).getText()) + 2 == Integer.parseInt(pages.get(j).getLineAt(0).getWordAt(lastWordJ).getText())){			

							pageNumLine=new LineDataContainer(pages.get(i).getLineAt(0).getFontName(), pages.get(i).getLineAt(0).getWordAt(lastWord).getFontSize(), pages.get(i).getLineAt(0).getWordAt(lastWord).getStartPositionX(), pages.get(i).getLineAt(0).getPositionY());
							pos=pageNumPos.TOP;
							findExtraPageNumLine((short)(pages.size()/2 -1), lookupRange, pos);
						}
					}
				}
				
				//try to locate page number at the bottom of the page
				if(StringUtils.isNumeric(pages.get(i).getLineAt(pages.get(i).size()-1).getWordAt(0).getText()) && StringUtils.isNumeric(pages.get(j).getLineAt(pages.get(j).size()-1).getWordAt(0).getText()) ){

					if(Integer.parseInt(pages.get(i).getLineAt(pages.get(i).size()-1).getWordAt(0).getText()) + 2 == Integer.parseInt(pages.get(j).getLineAt(pages.get(j).size()-1).getWordAt(0).getText())){			
						
						pageNumLine=new LineDataContainer(pages.get(i).getLineAt(pages.get(i).size()-1).getFontName(), pages.get(i).getLineAt(pages.get(i).size()-1).getWordAt(0).getFontSize(), pages.get(i).getLineAt(pages.get(i).size()-1).getStartPositionX(), pages.get(i).getLineAt(pages.get(i).size()-1).getPositionY());
						pos=pageNumPos.BOTTOM;
						findExtraPageNumLine((short)(pages.size()/2 -1), lookupRange, pos);
					}
				} else {
					int lastWord = pages.get(i).getLineAt(pages.get(i).size()-1).size() -1;
					int lastWordJ = pages.get(j).getLineAt(pages.get(j).size()-1).size() -1;
					
					if(StringUtils.isNumeric(pages.get(i).getLineAt(pages.get(i).size()-1).getWordAt(lastWord).getText()) && StringUtils.isNumeric(pages.get(j).getLineAt(pages.get(j).size()-1).getWordAt(lastWordJ).getText()) ){
						
						if(Integer.parseInt(pages.get(i).getLineAt(pages.get(i).size()-1).getWordAt(lastWord).getText()) + 2 == Integer.parseInt(pages.get(j).getLineAt(pages.get(j).size()-1).getWordAt(lastWordJ).getText())){			

							pageNumLine=new LineDataContainer(pages.get(i).getLineAt(pages.get(i).size()-1).getFontName(), pages.get(i).getLineAt(pages.get(i).size()-1).getWordAt(lastWord).getFontSize(), pages.get(i).getLineAt(pages.get(i).size()-1).getWordAt(lastWord).getStartPositionX(), pages.get(i).getLineAt(pages.get(i).size()-1).getPositionY());
							pos=pageNumPos.BOTTOM;
							findExtraPageNumLine((short)(pages.size()/2 -1), lookupRange, pos);
						}
					}
				}

				if(!pos.equals(pageNumPos.NONE))
					break;
			}
		}
		
		//Find pageNumber for 
		//findPageNumberPositionContentOrder();
		
		if(pos == pageNumPos.NONE){
			
			/* Throw an exception or do something to inform future usage */
			throw new BookWithoutPageNumbersException("Book without page numbers");
		}	
		
//		/*TESTING*/
//		System.out.println("pageNumPos: " + pos);
//		System.out.println("pageNumLine.linePosX: " + pageNumLine.linePosX);
//		System.out.println("pageNumLineExtra.linePosX: " + pageNumLineExtra.linePosX);
//		/*TESTING*/
		
		
/**
 * Search for "table of contents" 
 * and first chapter`s printed page number 
 * 
 * Some books include page number to the Title pages
 * such as, chapter beginning, table of contents etc..
 * For such cases we check for the top line to see
 * if the first line is pageNumberLine. 
 * If so, we start from the second line.
 * Otherwise, we check the first line,
 * if it is the title we are looking for.
 * 
 * When table of contents found, store the index of the page
 * as table of contents beginning index <tableOfContentsStartPage>
 * and store the printed page number of first chapter 
 * in <firstChapterStartPage> to match the printed pages and indexes
 *
 * 
 * */
		
		byte offset;
		
		//trying to locate <tableOfContentsStartPageIndx>
		//the idea is that in TOC the first number at the rightest position of the page corresponds to the page number
		//of the first chapter. Other initial sections, must be number in a different way (e.g. I,II,V,..) to work.
		int endLookingForTOC = pages.size() > 20 ? 36 : pages.size();
		for(byte i=0 ; i <endLookingForTOC ; i++){
		
			if( pages.elementAt(i) != null  && !pages.elementAt(i).getLines().isEmpty() ){

				//@i.alpizarchacon two more conditions added 
				if( BoundSimilarity.isInBound( pages.elementAt(i).getLineAt(0).getWordAt(0).getPositionY(), pageNumLine.linePosY, pageNumLine.fontSize, pages.elementAt(i).getLineAt(0).getWordAt(0).getFontSize(), 0.6f ) 
						|| pages.elementAt(i).getLineAt(0).getWordAt(0).getText().toUpperCase().matches(TableOfContentsExtractor.regexRomanNumber)
						|| pages.elementAt(i).getLineAt(0).getWordAt(pages.elementAt(i).getLineAt(0).size()-1).getText().toUpperCase().matches(TableOfContentsExtractor.regexRomanNumber)
						|| (pages.elementAt(i).getLineAt(0).size() == 1 && StringUtils.isNumeric(pages.elementAt(i).getLineAt(0).getWordAt(0).getText())))
					offset=1; //there is number line in the page, the first word should be in line #1
				else
					offset=0; //there is NO number line in the page, the first word should be in line #0
				
				//check if the page has more that 5 lines and the text of the first line (with text) is contains "table of content"
				//@i.alpizarchacon added: page needs to have also TOC structure, not only title
				if ( pages.elementAt(i).size() > 5
						&& WordListCheck.containstableOfContents(pages.elementAt(i).getLineAt(offset).getText()) && !WordListCheck.omitTableOfContents(pages.elementAt(i).getLineAt(offset).getText())
						&& TableOfContentsExtractor.checkTocLines(pages.elementAt(i), null, textBodyFontSize)){	

					tableOfContentsStartPageIndx = i;
					TOCTitleLine = pages.elementAt(i).getLineAt(offset);

					//j(next line of the word ~"toc") < lines at page
					for(int j=offset+1 ; j<pages.elementAt(i).size(); j++){

						if( StringUtils.isNumeric(pages.elementAt(i).getLineAt(j).getWordAt(pages.elementAt(i).getLineAt(j).size()-1).getText())
								&& pages.elementAt(i).getLineAt(j).getText().lastIndexOf(" ") != -1 && !WordListCheck.matchesTableOfContents(pages.elementAt(i).getLineAt(j).getText().substring(0, pages.elementAt(i).getLineAt(j).getText().lastIndexOf(" ")))){

							
							firstChapterStartPage = Short.parseShort( pages.elementAt(i).getLineAt(j).getWordAt(pages.elementAt(i).getLineAt(j).size()-1).getText() );
							
							/*TESTING*/
//							System.out.println("firstChapterStartPage: " + firstChapterStartPage);
//							SystemLogger.getInstance().log("*tableOfContentsStartPageIndx: " + tableOfContentsStartPageIndx);	
//							SystemLogger.getInstance().log("*firstChapterStartPage (first rule): " + firstChapterStartPage);
//							/*TESTING*/
							
							break;
						}
						else{
							
							//if the number is joined with dashes "....90"
							if(pages.elementAt(i).getLineAt(j).getWordAt(pages.elementAt(i).getLineAt(j).size()-1).getText().matches(".*[.][.]+[0-9]+")){
								
								String temp;
								temp = pages.elementAt(i).getLineAt(j).getWordAt(pages.elementAt(i).getLineAt(j).size()-1).getText().replaceAll("[.][.]+", " ");
								
								temp = temp.substring(temp.lastIndexOf(" ")+1);
								
								firstChapterStartPage = Short.parseShort(temp);
								
//								/*TESTING*/
//								System.out.println("!!! 2 firstChapterStartPage: " + firstChapterStartPage);
//								SystemLogger.getInstance().log("*tableOfContentsStartPageIndx: " + tableOfContentsStartPageIndx);	
//								SystemLogger.getInstance().log("*firstChapterStartPage (second rule): " + firstChapterStartPage);
//								/*TESTING*/
								
								break;
								
							} else {
								String num = TableOfContentsExtractor.getTheNumberAtTheEnd(pages.elementAt(i).getLineAt(j).getWordAt(pages.elementAt(i).getLineAt(j).size()-1).getText());
								if(num != null && pages.elementAt(i).getLineAt(j).size() != 1) {
									firstChapterStartPage = Short.parseShort(num);
									
//									/*TESTING*/
//									System.out.println("!!! 3 firstChapterStartPage: " + firstChapterStartPage);
//									/*TESTING*/
									
									break;
								}
							}
						}
					}

					break;
				}		
			}
		}
		
//		/*TESTING*/
//		System.out.println("############### " + firstChapterStartPage);
//		System.out.println("TOC normal: " + (tableOfContentsStartPageIndx != -1 ? "YES" : "NO"));
//		/*TESTING*/

		//CHECK TOC
		if( tableOfContentsStartPageIndx == -1 ){

			boolean flexible = FlexibleTOCStartRecognition();
			
			
			if(flexible) {
				firstChapterStartPage = (short) getFirstTOCLine();	
				
//				/*TESTING*/
//				System.out.println("TOC FLEXIBLE: " + flexible);
//				/*TESTING*/
				
			} else {
				throw new TOCNotFoundException("Book does not have an Table of Content");
			}

		} else {
			//Check if table of contents does not have numbers
			if(!TableOfContentsExtractor.checkTocLines(pages.elementAt(tableOfContentsStartPageIndx), null, textBodyFontSize)) {
				throw new TOCNotFoundException("Book have a Table of Content without page numbers");
			}
		}
		
/**
 * Search for the actual start of the first page
 * with respect to actual page index in the book. 
 * 
 * Find the first chapter`s page number in the table of contents,
 * try to find matching page number among pages. When found assign the 
 * index into <firstChapterStartIndex>
 * 
 * */
		short lineNum , wordNum  ;
		for(int i=tableOfContentsStartPageIndx+1; i<pages.size(); i++){

			lineNum = -1;
			wordNum = -1;

			if( pages.get(i) != null ){	

				if( pos == pageNumPos.TOP ){

					lineNum = 0;

				}
				else if( pos == pageNumPos.BOTTOM ){

					lineNum = (short) (pages.get(i).size()-1);
				}
				else{

					/* 
					 * Some books have page numbering on top page but
					 * use page numbers on the bottom on chapter beginnings
					 * 
					 * */

					lineNum = (short) (pages.get(i).size()-1);
				}
				
//				/*TESTING*/
//				SystemLogger.getInstance().log("PAGE#: " + i);
//				//SystemLogger.getInstance().log("PAGE: " + pages.get(i));
//				//SystemLogger.getInstance().log("PAGE lines: " + pages.get(i).getLines());
//				//SystemLogger.getInstance().log("PAGE get line at : " + pages.get(i).getLineAt(lineNum));
//				/*TESTING*/
				
				if(pages.get(i) == null ||  pages.get(i).getLines() == null || pages.get(i).getLines().size() == 0) {
					continue;
				}
				
				if(BoundSimilarity.isInBound(pages.get(i).getLineAt(lineNum).getPositionY(), pageNumLine.linePosY, pageNumLine.fontSize, pages.get(i).getLineAt(lineNum).getFontSize(), 0.7f)) {
					
//					/*TESTING*/
//					System.out.println(">> 1: " + pages.get(i).getLineAt(lineNum).getWordAt(0).getText());
//					System.out.println(">> 2: " + pages.get(i).getLineAt(lineNum).getWordAt(pages.get(i).getLineAt(lineNum).size()-1).getText());
//					/*TESTING*/
					
					if( StringUtils.isNumeric(pages.get(i).getLineAt(lineNum).getWordAt(0).getText()) ){
						//System.out.println(">> 1: OK");
						wordNum = 0;
					}
					else if( StringUtils.isNumeric(pages.get(i).getLineAt(lineNum).getWordAt(pages.get(i).getLineAt(lineNum).size()-1).getText()) ){
						//System.out.println(">> 2: OK");
						wordNum = (short) (pages.get(i).getLineAt(lineNum).size()-1);
					}
				} else {
					if(StringUtils.isNumeric(pages.get(i).getLineAt(pages.get(i).size()-1).getText())) {
						//System.out.println(">> YES");
						wordNum = (short) (0);
						lineNum = (short) (pages.get(i).size()-1);
					}
				}

				if(wordNum != -1 && StringUtils.isNumeric(pages.get(i).getLineAt(lineNum).getWordAt(wordNum).getText()) ) {
					
//					/*TESTING*/
//					System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
//					System.out.println("------------------- chekcing: " + " / i: " + i + " / + wordNum " +  wordNum + " / pn: " + pages.get(i).getLineAt(lineNum).getWordAt(wordNum).getText());
//					/*TESTING*/
					
					if( Integer.parseInt(pages.get(i).getLineAt(lineNum).getWordAt(wordNum).getText()) == firstChapterStartPage ){
						firstChapterStartIndex = (byte) i;
						//System.out.println("~1");
						break;
					}
					else if( Integer.parseInt(pages.get(i).getLineAt(lineNum).getWordAt(wordNum).getText()) == firstChapterStartPage+1 ){
						firstChapterStartIndex = (byte) (i-1); 
						//System.out.println("~2");
						break;
					}
					else if( Integer.parseInt(pages.get(i).getLineAt(lineNum).getWordAt(wordNum).getText()) == firstChapterStartPage+2 ){
						firstChapterStartIndex = (byte) (i-2); 
						//System.out.println("~3");
						break;
					}
					
					int currentPageNumber = Integer.parseInt(pages.get(i).getLineAt(lineNum).getWordAt(wordNum).getText());
					if(currentPageNumber > firstChapterStartPage){
						currentPageNumber--;
						byte lastValidFirstChapterStartIndex = firstChapterStartIndex;
						firstChapterStartIndex = (byte) (i-1);
						
						while(!TableOfContentsExtractor.checkTocLines(pages.get(firstChapterStartIndex), TOCTitleLine, textBodyFontSize) && currentPageNumber >= firstChapterStartPage) {
							currentPageNumber--;
							lastValidFirstChapterStartIndex = firstChapterStartIndex;
							firstChapterStartIndex--;
						}
						firstChapterStartIndex = lastValidFirstChapterStartIndex;
						break;
					}
				}
			}
		}
		
		if(firstChapterStartIndex < 0) {
			throw new TOCNotFoundException("There is a problem with the TOC recognition, and the page index of first chapter was not recognized");
		} else {
			//checking for firstChapterStartIndex
			while(TableOfContentsExtractor.checkTocLines(pages.get(firstChapterStartIndex), TOCTitleLine, textBodyFontSize)) {
				firstChapterStartIndex++;
			}
		}
	}
	
	// not used
	private void copyNumberingOfPages(Vector<Page> in, Vector<Page> out) {
		if(in.size() == out.size()) {
			for(int i = 0; i < in.size(); i++) {
				if(in.elementAt(i) != null) {
					out.get(i).setPageNumber(in.get(i).getPageNumber());
					out.get(i).setPageIndex(in.get(i).getPageIndex());
				}
			}
		}
	}
		
	/** 
	 * Reading page numbers from pages and assigning them
	 * to pages.
	 * 
	 * Page numbering does not always follow a sequential order,
	 * sometimes there are gaps and jumps between page numbering
	 * by reading each page number from page itself prevents 
	 * having different order of pages compared to actual book.
	 * 
	 * */
	
	private void numberingPages(String bookID){
	
		short lineNum = 0;
		
//		/*TESTING*/
//		System.out.println("firstChapterStartIndex: " + firstChapterStartIndex);
//		System.out.println("POS: " + pos);
//		/*TESTING*/
		
		//index of firstChapter (physical page)
		int previousN = -1;
		for(int i=firstChapterStartIndex; i<pages.size(); i++){
			
			if(pages.get(i)!=null && pages.get(i).size() > 0){	
				
				SystemLogger.getInstance().log("Page: " + i);
				
				int actualN = -1;
				
				if( pos == pageNumPos.TOP )					
					lineNum = 0;
				else if( pos == pageNumPos.BOTTOM )
					lineNum = (short) (pages.get(i).size()-1);
				
//				/*TESTING*/
//				SystemLogger.getInstance().log("@ page: " + i + " " + pages.get(i));
//				SystemLogger.getInstance().log("@ lineNum: " + " " + lineNum);
//				SystemLogger.getInstance().log("@ pages.size(): " + " " + pages.get(i).size());
//				SystemLogger.getInstance().log("@ pages.get(i).getLineAt(lineNum): " + pages.get(i).getLineAt(lineNum));
//				SystemLogger.getInstance().log("@ pages.get(i).getLineAt(lineNum).getWordAt(0): " + pages.get(i).getLineAt(lineNum).getWordAt(0));
//				SystemLogger.getInstance().log("@ pages.get(i).getLineAt(lineNum): " + pages.get(i).getLineAt(lineNum));
//				SystemLogger.getInstance().log("@ pages.get(i).getLineAt(lineNum): " + pages.get(i).getLineAt(lineNum));
//				SystemLogger.getInstance().log("@ pages.get(i).getLineAt(lineNum): " + pages.get(i).getLineAt(lineNum));
//				/*TESTING*/
				
				if(StringUtils.isNumeric(pages.get(i).getLineAt(lineNum).getWordAt(0).getText())
						&& BoundSimilarity.isInBound(pages.get(i).getLineAt(lineNum).getPositionY(), pageNumLine.linePosY, pageNumLine.fontSize, pages.get(i).getLineAt(lineNum).getFontSize(), 0.6f)){
					
					actualN = Integer.parseInt(pages.get(i).getLineAt(lineNum).getWordAt(0).getText());
					if(previousN != -1 && actualN <= previousN) {
						//wrong number
						actualN = -1;
					} else {
						pages.get(i).setPageNumber(actualN);
						//SystemLogger.getInstance().log(" ... setting 1: " + pages.get(i).getPageNumber());
					}
					
				}
				else if(StringUtils.isNumeric(pages.get(i).getLineAt(lineNum).getWordAt(pages.get(i).getLineAt(lineNum).size()-1).getText())
						&&  BoundSimilarity.isInBound(pages.get(i).getLineAt(lineNum).getPositionY(), pageNumLine.linePosY, pageNumLine.fontSize, pages.get(i).getLineAt(lineNum).getFontSize(), 0.6f)){
					
					actualN = Integer.parseInt(pages.get(i).getLineAt(lineNum).getWordAt(pages.get(i).getLineAt(lineNum).size()-1).getText());
					if(previousN != -1 && actualN <= previousN) {
						//wrong number
						actualN = -1;
					} else {
						pages.get(i).setPageNumber(actualN);
						//SystemLogger.getInstance().log(" ... setting 2: " + pages.get(i).getPageNumber());
					}
					
				}
				else if(StringUtils.isNumeric(pages.get(i).getLineAt(lineNum).getWordAt(0).getText())
						&&  BoundSimilarity.isInBound(pages.get(i).getLineAt(lineNum).getPositionY(), pageNumLine.linePosY, pageNumLine.fontSize, pages.get(i).getLineAt(lineNum).getFontSize(), 0.6f)){
					
					actualN = Integer.parseInt(pages.get(i).getLineAt(lineNum).getWordAt(0).getText());
					if(previousN != -1 && actualN <= previousN) {
						//wrong number
						actualN = -1;
					} else {
						pages.get(i).setPageNumber(actualN);
						//SystemLogger.getInstance().log(" ... setting 3: " + pages.get(i).getPageNumber());
					}
					
				}
				else if(StringUtils.isNumeric(pages.get(i).getLineAt(lineNum).getWordAt(pages.get(i).getLineAt(lineNum).size()-1).getText())
						&&  BoundSimilarity.isInBound(pages.get(i).getLineAt(lineNum).getPositionY(), pageNumLine.linePosY, pageNumLine.fontSize, pages.get(i).getLineAt(lineNum).getFontSize(), 0.6f)){
					
					actualN = Integer.parseInt(pages.get(i).getLineAt(lineNum).getWordAt(pages.get(i).getLineAt(lineNum).size()-1).getText());
					if(previousN != -1 && actualN <= previousN) {
						//wrong number
						actualN = -1;
					} else {
						pages.get(i).setPageNumber(previousN);
						//SystemLogger.getInstance().log(" ... setting 4: " + pages.get(i).getPageNumber());
					}
					
				}
				else if(StringUtils.isNumeric(pages.get(i).getLineAt(pages.get(i).size()-1).getText())){
					
					actualN = Integer.parseInt(pages.get(i).getLineAt(pages.get(i).size()-1).getText());
					if(previousN != -1 && actualN <= previousN) {
						//wrong number
						actualN = -1;
					} else {
						pages.get(i).setPageNumber(actualN);
						//SystemLogger.getInstance().log(" ... setting 5: " + pages.get(i).getPageNumber());
					}
					
				}
				
				if(actualN == -1) {
					pages.get(i).setPageNumber(-1);
					pages.get(i).setSpecialPageNumbering(true);
					
					/*TESTING*/
					//SystemLogger.getInstance().log(" ... no #: " + pages.get(i).getPageNumber());
					/*TESTING*/
					
				} else {
					previousN = actualN;
				}		
			}	
		}

		crossCheckingPageNumbers(bookID);
		
//		/*TESTING*/
//		for(int i = 0; i < pages.size(); i++) {
//		
//			if (pages.get(i) != null) {
//				System.out.println("--------- ");
//				System.out.println("@Physical page(index): " + i);
//				System.out.println("@Logical page(page number): " + pages.get(i).getPageNumber());
//			
//			}
//		}
//		System.exit(0);
//		/*TESTING*/

	}
	
	private boolean pageTaken(int pageNumber) {
		for(int i=0; i<pages.size(); i++){
			if(pages.get(i) != null && pages.get(i).getPageNumber() == pageNumber) {
				return true;
			}
		}
		return false;
	}
	
	private boolean extensiveLooking(Page page, int number) {
		//look in the first 5 lines
		for(int i= 0; i < 5 && i<  page.size(); i++) {
			Line l = page.getLineAt(i);
			if(l.size() > 0) {
				if(l.getWordAt(0).getText().equals(String.valueOf(number))){
					return true;
				} else if (l.getLastWord().getText().equals(String.valueOf(number))) {
					return true;
				}
			}
		}
		//look in the last five lines
		for(int i= page.size() -1; i > 0 && i > (page.size() - 5); i--) {
			Line l = page.getLineAt(i);
			if(l.size() > 0) {
				if(l.getWordAt(0).getText().equals(String.valueOf(number))){
					return true;
				} else if (l.getLastWord().getText().equals(String.valueOf(number))) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	private void crossCheckingPageNumbers(String bookID){
		
		int lastOne = -1;
		for(int i = pages.size() -1; i >= firstChapterStartIndex; i--) {
			if(pages.get(i) != null) {
				if(pages.get(i).getSpecialPageNumbering()) {
					int candidate = lastOne -1;
					int candidate2 = lastOne - 2;
					int candidate3 = lastOne - 3;
					if(!pageTaken(candidate) && extensiveLooking(pages.get(i), candidate)) {
						pages.get(i).setPageNumber(candidate);
						pages.get(i).setSpecialPageNumbering(false);
						lastOne = candidate;
					} else if (!pageTaken(candidate2) && extensiveLooking(pages.get(i), candidate2)) {
						pages.get(i).setPageNumber(candidate2);
						pages.get(i).setSpecialPageNumbering(false);
						lastOne = candidate2;
					} else if (!pageTaken(candidate3) && extensiveLooking(pages.get(i), candidate3)) {
						pages.get(i).setPageNumber(candidate3);
						pages.get(i).setSpecialPageNumbering(false);
						lastOne = candidate3;
					} else if (!pageTaken(candidate) ) {
						pages.get(i).setPageNumber(candidate);
						pages.get(i).setSpecialPageNumbering(false);
						lastOne = candidate;
					} else {
						pages.get(i).setPageNumber(-1);
					}
				} else {
					lastOne = pages.get(i).getPageNumber();
				}
			}
		}
		
		int count = 0;
		for(int i=firstChapterStartIndex; i<pages.size(); i++) {
			if(pages.get(i) != null && pages.get(i).getPageNumber() == -1)
				count++;
		}
		
		for(int i=firstChapterStartIndex; i<pages.size(); i++) {
			if(pages.get(i) != null && pages.get(i).getPageNumber() == -1)
				pages.get(i).setPageNumber(-1 * count--);
		}
		
		for(int i=firstChapterStartIndex; i<pages.size(); i++) {
			if(pages.get(i) != null) {
				pageNumbers.add(pages.get(i).getPageNumber());
				pages.get(i).setPageIndex(i);
				SystemLogger.getInstance().debug("Page: " + i + " PN: " + pages.get(i).getPageNumber() + " PI: " +  pages.get(i).getPageIndex());
			}
		}

		this.cm.setPageNumbersOfBook(bookID, pageNumbers);
	}
	
	private void obtainMetadata() {
		String ISBN = null;
		int lookupRange = pages.size() > 6 ? 6: pages.size();
		
		//tries to remove a repeating line at the beginning of each page
		pages:
		for(short i=0; i < lookupRange; i++){
			if(pages.get(i) != null) {
							
				for(Line line: pages.get(i).getLines()) {
					for(int j = 0; j < line.size(); j++) {
						if(line.getWordAt(j).getText().equals("ISBN")) {
							if((j+1) <  line.size()) {
								if(line.getWordAt(j+1).getText().matches(StringOperations.getRegexISBN())) {
									
									String url = OpenLibraryISBNService + line.getWordAt(j+1).getText();
									String jsonString = SimpleHTTPRequest.doGetRequest(url);
									System.out.println(jsonString);
									JSONParser parser = new JSONParser();
									try {
										JSONObject obj = (JSONObject) parser.parse(jsonString);
										if(obj.values().size() == 1 ) {
											JSONObject map = (JSONObject) obj.values().toArray()[0];
											//title
											metadata.put("title", (String) map.get("title"));
											metadata.put("subtitle", (String) map.get("subtitle"));
											metadata.put("publish_date", (String) map.get("publish_date"));
											Object authorsObj = map.get("authors");
											if(authorsObj == null) {
												metadata.put("authors", (String) map.get("by_statement"));
											} else {
												JSONArray  authorsArray = (JSONArray) authorsObj;
												Iterator<Object> authorsIterator = authorsArray.iterator();
												String authors = "";
												while(authorsIterator.hasNext()) {
													JSONObject authorObject = (JSONObject) authorsIterator.next();
													authors += authorObject.get("name") + "|";
												}
												metadata.put("authors", authors);
											}
											Object publishersObj = map.get("publishers");
											
											if(publishersObj != null) {
												JSONArray  publishersArray = (JSONArray)publishersObj;
												Iterator<Object> publishersIterator = publishersArray.iterator();
												String publisher = "";
												while(publishersIterator.hasNext()) {
													JSONObject publisherObject = (JSONObject) publishersIterator.next();
													publisher += publisherObject.get("name") + " ";
												}
												metadata.put("publisher", publisher.trim());
											}
										}
									} catch (ParseException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									break pages;
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * On chapter beginnings sometimes page number is not printed
	 * to make up for it, here we compare three pages at a time
	 * so we can figure out skipping pages
	 * 
	 * */
	
	private void removeRepeatingLines(){
		
		//@i.alpizrachacon if the document is smaller than 3 pages, return
		if(pages.size() < 3) {
			return;
		}

		LineDataContainer lineToRemove=null;
			
		short lookupRange = (short) (((pages.size()/2)+20 < pages.size()) ? (pages.size()/2)+20 : pages.size());
		short startPage = (short)( (pages.size()/2)+2 < (pages.size()-1) ? pages.size()/2 : 0);
		
		//tries to remove a repeating line at the beginning of each page
		for(short i=  startPage; i < lookupRange && (i+2) < lookupRange; i++){
			
				if(pages.get(i)!= null && pages.get(i+2)!= null)
					if( pages.get(i).getLineAt(0).getText().equals( pages.get(i+2).getLineAt(0).getText())){
						
					    lineToRemove = new LineDataContainer("", pages.get(i).getLineAt(0).getFontSize(),0 , pages.get(i).getLineAt(0).getPositionY());					   
					}
			
			if(lineToRemove!=null){
				for(int j = 0; j<pages.size() ; j++)
					if(pages.get(j)!=null && pages.get(j).getLines()!=null && !pages.get(j).getLines().isEmpty())
						if(BoundSimilarity.isInBound(pages.get(j).getLineAt(0).getPositionY(), lineToRemove.linePosY, pages.get(j).getLineAt(0).getFontSize(), lineToRemove.fontSize, 0.6f)){
							
							pages.get(j).removeLineAt(0);
							pages.get(j).populatePageData();
							if(pages.get(j).getLines().size() == 0) {
								pages.set(j, null);
							}
						}				
				
				 break;
			}
		}
		
		lineToRemove=null;
		
		//tries to remove a repeating line at the end of each page
		for(short i=  startPage; i < lookupRange && (i+2) < lookupRange; i++){
			
			if(pages.get(i)!=null && pages.get(i+2)!= null)
				if(pages.get(i).getLineAt(pages.get(i).size()-1).getText().equals( pages.get(i+2).getLineAt(pages.get(i+2).size()-1).getText())){

					lineToRemove = new LineDataContainer("", pages.get(i).getLineAt(pages.get(i).size()-1).getFontSize(),0 , pages.get(i).getLineAt(pages.get(i).size()-1).getPositionY());				   
				}
			
			if(lineToRemove!=null){
				for(int j = 0; j<pages.size() ; j++){
					
					if(pages.get(j)!=null && pages.get(j).getLines()!=null && !pages.get(j).getLines().isEmpty())
						if(BoundSimilarity.isInBound(pages.get(j).getLineAt(pages.get(j).size()-1).getPositionY(), lineToRemove.linePosY, pages.get(j).getLineAt(pages.get(j).size()-1).getFontSize(), lineToRemove.fontSize, 0.6f)){
							
							pages.get(j).removeLineAt(pages.get(j).size()-1);
							pages.get(j).populatePageData();
							if(pages.get(j).getLines().size() == 0) {
								pages.set(j, null);
							}
						}
				}
				
				 break;
			}
		}
		
//		/*TESTING*/
//		int i = 0;
//		for(Page p : pages) {
//			System.out.println("-----------------");
//			System.out.println("Page: " + i++);
//			if(p == null)
//				continue;
//			String t = "";
//			for(Line l : p.getLines()) {
//				t += l.getText() + "/n";
//			}
//			System.out.println(t);
//		}
//		/*TESTING*/
		
	}
	
	/**
	 * 
	 */
	private void removePageNumLine(Vector<Page> pages, pageNumPos pos, LineDataContainer pageNumLine){

		short loopStartPoint;
		
		for(int i = tableOfContentsStartPageIndx ; i< pages.size() ; i++ ){

			if(pages.get(i)!=null && pages.get(i).getLines()!=null && !pages.get(i).getLines().isEmpty()){
				
				SystemLogger.getInstance().debug("# page n: " + i + " - " + pos);
				
				if(pos.equals(pageNumPos.TOP)){
					
					loopStartPoint= 0;
					
					int result = BoundSimilarity.isGreaterOrLesser(pages.get(i).getLineAt(loopStartPoint).getPositionY(), pageNumLine.linePosY, 
							pageNumLine.fontSize, pages.get(i).getLineAt(loopStartPoint).getFontSize(),
							0.9f);
					
					if( result == 0){
						pages.get(i).removeLineAt(loopStartPoint);
					} else if ( i < 8 && pages.get(i).getLineAt(loopStartPoint).getWordAt(pages.get(i).getLineAt(loopStartPoint).size() -1).getText().toUpperCase().matches(TableOfContentsExtractor.regexRomanNumber)) {
						//special case for scanned book
						pages.get(i).removeLineAt(loopStartPoint);
					}
					else{ 
						
						result = BoundSimilarity.isGreaterOrLesser( pages.get(i).getLineAt(pages.get(i).size()-1).getStartPositionX(),
								 pages.get(i).getWidth()/3,pageNumLine.fontSize, 
								 pages.get(i).getLineAt(pages.get(i).size()-1).getFontSize(), 0.9f);
						
//						/*TESTING*/
//						System.out.println("--> checking case two:");
//						System.out.println("\t--> result : " + result);
//						System.out.println("\t--> isNumeric: " + StringUtils.isNumeric(pages.get(i).getLineAt(pages.get(i).size()-1).getText()));
//						System.out.println("\t--> last line: " + pages.get(i).getLineAt(pages.get(i).size()-1).getText());
//						/*TESTING*/
						
						if(result == 1 && (StringUtils.isNumeric(pages.get(i).getLineAt(pages.get(i).size()-1).getText()) 
								|| pages.get(i).getLineAt(pages.get(i).size()-1).getText().toUpperCase().matches(TableOfContentsExtractor.regexRomanNumber) )){
					
							pages.get(i).removeLineAt(pages.get(i).size()-1);
						}
					}
				}
				else if(pos.equals(pageNumPos.BOTTOM)){
					
					loopStartPoint= (short) (pages.get(i).size()-1);
					
					int result = BoundSimilarity.isGreaterOrLesser(pages.get(i).getLineAt(loopStartPoint).getPositionY(), pageNumLine.linePosY, 
							pageNumLine.fontSize, pages.get(i).getLineAt(loopStartPoint).getFontSize(),
							0.9f);
					
					if( result == 0){
						pages.get(i).removeLineAt(loopStartPoint);	
					}			
				}
				pages.get(i).pageText();
			}
		}
	}
	
	private boolean normalizeTocStartXNecessary(List<Line> linesFirstP, List<Line> linesOtherP) {
		int mostLeftFirstP = 9999;
		for(int indexL = 1; indexL < linesFirstP.size(); indexL++) {
			Line l = linesFirstP.get(indexL);
			if(l.getStartPositionX() < mostLeftFirstP) {
				mostLeftFirstP = (int) l.getStartPositionX();
			}
		}
		
		int mostLeftOtherP = 9999;
		for(Line l : linesOtherP) {
			if(l.getStartPositionX() < mostLeftOtherP) {
				mostLeftOtherP = (int) l.getStartPositionX();
			}
		}
		
		
		if(mostLeftFirstP == mostLeftOtherP || Math.abs(mostLeftFirstP - mostLeftOtherP) <= 1) {
			//System.out.println("@@@ ## first!");
			return false;
		} else {
			//second check
			mostLeftFirstP = getMostLeftXOfChapters(linesFirstP, true);
			mostLeftOtherP = getMostLeftXOfChapters(linesOtherP, false);
			
//			System.out.println("mostLeftFirstP: " + mostLeftFirstP);
//			System.out.println("mostLeftOtherP: " + mostLeftOtherP);
			
			if(mostLeftFirstP == mostLeftOtherP || Math.abs(mostLeftFirstP - mostLeftOtherP) <= 1) {
				//System.out.println("@@@ ## second! false");
				return false;
			} else {
				//System.out.println("@@@ ## second! true");
				return true;
			}
		}
		
	}
	
	private int getMostLeftXOfChapters(List<Line> lines, boolean firstTOCPage) {
		int start = firstTOCPage ? 1 : 0;
		ArrayList<Line> topLevelLines = new ArrayList();
		
		//1 get the most left
		int mostLeft = 9999;		
		for(int indexL = start; indexL < lines.size(); indexL++) {
			Line l = lines.get(indexL);
			if(l.getStartPositionX() < mostLeft) {
				mostLeft = (int) l.getStartPositionX();
			}
		}
		
		//get the lines with most left
		for(Line l : lines) {
			if((int) l.getStartPositionX() == mostLeft) {
				topLevelLines.add(l);
			}
		}
		
		//get the most left of the first word 
		int mostLeftFirstWord = 9999;
		//boolean first = true;
		for(Line l: topLevelLines) {
			
			for(Text w: l.getWords()) {
				if(!StringUtils.isNumeric(w.getText())) {
					int startXW = (int) w.getStartPositionX();
					if(startXW < mostLeftFirstWord) {
						//System.out.println("*********** new low: "  + l.getText() + " " + l.getStartPositionX() + " " + w.getText() + " " + startXW);
						mostLeftFirstWord = startXW;
					}
					break;
				}	
			}
		}
		
		return mostLeftFirstWord;
	}
	
	/**
	 * 
	 * @param startPageNum
	 * @param endPageNum
	 */
	private void normalizeTocStartX(int startPageNum, byte endPageNum, Line TOCTitleLine, float textBodyFontSize){
		//i.alpizarchacon FIXED, the following line was added to force the calculation of the most X position.
		pages.get(startPageNum).calculateMostLeftRight();
		tocMostleft1= pages.get(startPageNum).getPageMostLeftX();
		
		//i.alpizarchacon added: initial check to see if odd and even pages have the same most X position
		boolean atLeastOne = false;
		for(int nextPage = startPageNum + 1; nextPage <= endPageNum; nextPage += 2) {
			SystemLogger.getInstance().debug(" checking page: "+ nextPage);
			if(pages.get(nextPage) == null) {
				endPageNum = (byte) (nextPage -1);
				break;
			}
			SystemLogger.getInstance().debug(" checking second page: "+ nextPage);
			if(!TableOfContentsExtractor.checkTocLines(pages.get(nextPage), TOCTitleLine, textBodyFontSize)) {
				endPageNum = (byte) (nextPage -1);
				break;
			}
			
			//OLD WAY
			/*pages.get(nextPage).calculateMostLeftRight();
			if (BoundSimilarity.isInBound(pages.get(startPageNum).getPageMostLeftX(), pages.get(nextPage).getPageMostLeftX(),pages.get(startPageNum).getPageMostLeftX(),pages.get(nextPage).getPageMostLeftX(),0.01f)) {
				System.out.println(" p: "+ nextPage);
				atLeastOne = true;
				break;
			} */
			//NEW WAY
			boolean normalizeTocStartXNecessary = normalizeTocStartXNecessary(pages.get(startPageNum).getLines(),  pages.get(nextPage).getLines());
			if(!normalizeTocStartXNecessary) {
				atLeastOne = true;
				break;
			}
		}
		
		if(atLeastOne) {
			SystemLogger.getInstance().log("Skipping normalizeTocStartX since all TOC pages start at same X position");
			return;
		}
		
//		/*TESTING*/
//		SystemLogger.getInstance().log("@@@@@@@ doing");
//		System.out.println("@@@ >> tocMostleft1: " + tocMostleft1);
//		System.out.println("@@@ >> after cMostleft1: " + pages.get(startPageNum).getPageMostLeftX());
//		System.out.println("@@@ >> after cMostleft1: " + pages.get(startPageNum).getPageMostLeftX());
//		System.exit(1);
//		/*TESTING*/
		
		for(int i=startPageNum;i<endPageNum;i++){

			if(pages.get(i)!=null && pages.get(i).getLines() != null && pages.get(i).size() > 0 ){
				
				//@i.alpizarchacon ADDED the folowing line to fix problem with the normalization
				pages.get(i).calculateMostLeftRight();
				
				if((int)(tocMostleft1 - pages.get(i).getPageMostLeftX()) > 0){
					//System.out.println("case 1");
					//each line of the page
					for(int j = 0 ; j<pages.get(i).getLines().size();j++){						
						
						Line temp= 	pages.get(i).getLineAt(j);
						temp.setStartPositionX(temp.getStartPositionX() + (tocMostleft1 - pages.get(i).getPageMostLeftX()));
						
						//each word of the line
						for(int k = 0 ; k < temp.size(); k++){
							
							if(temp.getWordAt(k)!= null) {
								temp.getWordAt(k).setStartPositionX(temp.getWordAt(k).getStartPositionX() + (tocMostleft1 - pages.get(i).getPageMostLeftX()) );	
								temp.getWordAt(k).setEndPositionX(temp.getWordAt(k).getEndPositionX() + (tocMostleft1 - pages.get(i).getPageMostLeftX()) );	
							}
								
						}
						
						pages.get(i).replaceLineAt(j, temp);
					}					
				}
				else if ((int)(tocMostleft1 - pages.get(i).getPageMostLeftX()) < 0){
					//System.out.println("case 2");
					for(int j = 0 ; j<pages.get(i).getLines().size();j++){						
						
						Line temp= 	pages.get(i).getLineAt(j);
						temp.setStartPositionX(temp.getStartPositionX() + (tocMostleft1 - pages.get(i).getPageMostLeftX()));
						
						for(int k = 0 ; k< temp.size(); k++){
							if(temp.getWordAt(k)!= null)
								temp.getWordAt(k).setStartPositionX(temp.getWordAt(k).getStartPositionX() +(tocMostleft1 - pages.get(i).getPageMostLeftX()) );	
								temp.getWordAt(k).setEndPositionX(temp.getWordAt(k).getEndPositionX() + (tocMostleft1 - pages.get(i).getPageMostLeftX()) );	
						}
						
						pages.get(i).replaceLineAt(j, temp);
					}	
				}
			}	
		}
		
//		/*TESTING*/
//		for(int i=startPageNum;i<endPageNum;i++){
//			for(int j = 0 ; j<pages.get(i).getLines().size();j++){
//				System.out.println(pages.get(i).getLineAt(j).getText() + " X: " + pages.get(i).getLineAt(j).getStartPositionX());
//			}	
//		}
//		System.exit(0);
//		/*TESTING*/
		
	}
	
	public static int getLineFontSize(Line l) {

//		/*TESTING*/
//		for(Text word: l.getWords()){
//			System.out.println("w: " +word.getText() + " fs: " + word.getFontSize() + " " + word.getFontName());
//		}
//		/*TESTING*/
		
		HashMap<Float, Integer> fontSizes = new HashMap <Float, Integer>();
		for(Text word: l.getWords()){
			Integer cant = fontSizes.get(word.getFontSize());
			if(cant == null) {
				cant = 0;
			}
			cant += word.size();
			fontSizes.put(word.getFontSize(), cant);
		}
		
		float biggestCommonFontSize =0;
		int biggestCant = 0;
		for(Entry<Float, Integer> entry : fontSizes.entrySet()) {
			//System.out.println("K: " + entry.getKey() + " V: " + entry.getValue());
			if(entry.getValue() > biggestCant) {
				biggestCant = entry.getValue();
				biggestCommonFontSize = entry.getKey();
			}
		}
		return (int) biggestCommonFontSize;
	}
	
	private boolean possiblePart(List<Line> pages, FormattingDictionary fD) {
		boolean part = false;
		int count = 0;
		for(Line l: pages) {
			if(getLineFontSize(l) == fD.getBodyFontSize()) {
				return false;
			}
			if(l.getText().toLowerCase().matches("(part|section) ([0-9]*|[mdclxvi]*)")) {
				part = true;
			}
			count++;
		}
		
		if(count < 3 || (count < 7 && part)) {
			return true;
		} else {
			return false;
		}
	}
	
	private List<TextBlock> getTextBlocksV2(FormattingDictionary fD, Pair<Float, Float> lineEdges, float lineSpacing) {
		List<TextBlock> textBlocks = new ArrayList<TextBlock>();
		TextBlock currentTextBlock = new TextBlock();
		TextBlock.setFormattingDictionary(fD);
		textBlocks.add(currentTextBlock);
		
		int bodyFontSize = fD.getBodyFontSize();
		Line previousLine = null;
		Line currentLine = null;
		int previousLineFontSize = 0;
		int currentLineFontSize = 0;
		if(pages.get(firstChapterStartIndex) != null && pages.get(firstChapterStartIndex).size() > 0) {
			previousLine = pages.get(firstChapterStartIndex).getLineAt(0);
			previousLineFontSize = getLineFontSize(previousLine);
			FormattingContainer fC = fD.findRole(previousLine.getFCKeySum());
			if(fC != null) {
				currentTextBlock.setRoleLabel(fC.getRoleLabel());
				currentTextBlock.setTitleLevel(fC.getTitleLevel());
			} else {
				currentTextBlock.setRoleLabel(RoleLabel.Body);
			}
		}
		
		///for(int i=firstChapterStartIndex; i<27; i++){
		for(int i=firstChapterStartIndex; i<pages.size(); i++){
			if(pages.get(i) == null)
				continue;
			for(int l= 0; l <  pages.get(i).size(); l++) {
				currentLine =   pages.get(i).getLineAt(l);
				currentLineFontSize = getLineFontSize(currentLine);
				boolean lineSpacingIsBigger = Math.round((currentLine.getPositionY() - previousLine.getPositionY())) < 0  || Math.round((currentLine.getPositionY() - previousLine.getPositionY())) > lineSpacing;
				
				
				if(fD.sameRole(previousLine, currentLine) || 
						previousLine.getFCKeySum().equals(currentLine.getFCKeySum()) ||
						previousLineFontSize == currentLineFontSize ||
						currentLineFontSize <= bodyFontSize && previousLineFontSize <= bodyFontSize) {
					
					if(currentLineFontSize <= bodyFontSize && previousLineFontSize <= bodyFontSize && lineSpacingIsBigger) {
						currentTextBlock = new TextBlock();
						textBlocks.add(currentTextBlock);
						currentTextBlock.addLine(currentLine);
						currentTextBlock.setRoleLabel(RoleLabel.Body);
					} else {
						currentTextBlock.addLine(currentLine);
					}
					
					
				} else {
					currentTextBlock = new TextBlock();
					textBlocks.add(currentTextBlock);
					currentTextBlock.addLine(currentLine);
					FormattingContainer fC = fD.findRole(currentLine.getFCKeySum());
					if(fC != null) {
						currentTextBlock.setRoleLabel(fC.getRoleLabel());
						currentTextBlock.setTitleLevel(fC.getTitleLevel());
					} else {
						currentTextBlock.setRoleLabel(RoleLabel.Body);
					}
				}
				previousLine = currentLine;
				previousLineFontSize = currentLineFontSize;
			}
			
		}
		return textBlocks;
	}
	
	private static List<TextBlock> getTextBlocksV2(List<Line> lines, FormattingDictionary fD, Pair<Float, Float> lineEdges, float lineSpacing) {
		List<TextBlock> textBlocks = new ArrayList<TextBlock>();
		TextBlock currentTextBlock = new TextBlock();
		TextBlock.setFormattingDictionary(fD);
		textBlocks.add(currentTextBlock);
		
		int bodyFontSize = fD.getBodyFontSize();
		Line previousLine = null;
		Line currentLine = null;
		int previousLineFontSize = 0;
		int currentLineFontSize = 0;
		
		previousLine = lines.get(0);
		previousLineFontSize = getLineFontSize(previousLine);
		FormattingContainer fC = fD.findRole(previousLine.getFCKeySum());
		if(fC != null) {
			currentTextBlock.setRoleLabel(fC.getRoleLabel());
			currentTextBlock.setTitleLevel(fC.getTitleLevel());
		} else {
			currentTextBlock.setRoleLabel(RoleLabel.Body);
		}
	
	
		for(int l= 0; l <  lines.size(); l++) {
			currentLine =   lines.get(l);
			currentLineFontSize = getLineFontSize(currentLine);
			boolean lineSpacingIsBigger = Math.round((currentLine.getPositionY() - previousLine.getPositionY())) < 0  || Math.round((currentLine.getPositionY() - previousLine.getPositionY())) > lineSpacing;
			
			
			if(fD.sameRole(previousLine, currentLine) || 
					previousLine.getFCKeySum().equals(currentLine.getFCKeySum()) ||
					previousLineFontSize == currentLineFontSize ||
					currentLineFontSize <= bodyFontSize && previousLineFontSize <= bodyFontSize) {
				
				if(currentLineFontSize <= bodyFontSize && previousLineFontSize <= bodyFontSize && lineSpacingIsBigger) {
					currentTextBlock = new TextBlock();
					textBlocks.add(currentTextBlock);
					currentTextBlock.addLine(currentLine);
					currentTextBlock.setRoleLabel(RoleLabel.Body);
				} else {
					currentTextBlock.addLine(currentLine);
				}
				
				
			} else {
				currentTextBlock = new TextBlock();
				textBlocks.add(currentTextBlock);
				currentTextBlock.addLine(currentLine);
				FormattingContainer fC2 = fD.findRole(currentLine.getFCKeySum());
				if(fC2 != null) {
					currentTextBlock.setRoleLabel(fC2.getRoleLabel());
					currentTextBlock.setTitleLevel(fC2.getTitleLevel());
				} else {
					currentTextBlock.setRoleLabel(RoleLabel.Body);
				}
			}
			previousLine = currentLine;
			previousLineFontSize = currentLineFontSize;
		}
			
		
		return textBlocks;
	}
	
	private List<TextBlock> getTextBlocks(FormattingDictionary fD, Pair<Float, Float> lineEdges) {
		List<TextBlock> textBlocks = new ArrayList<TextBlock>();
		TextBlock currentTextBlock = new TextBlock();
		textBlocks.add(currentTextBlock);
		Line previousLine = null;
		int previousLineFontSize = 0;
		if(pages.get(firstChapterStartIndex) != null && pages.get(firstChapterStartIndex).size() > 0) {
			previousLine = pages.get(firstChapterStartIndex).getLineAt(0);
			previousLineFontSize = getLineFontSize(previousLine);
		}

		for(int i=firstChapterStartIndex; i<pages.size(); i++){
			SystemLogger.getInstance().log("getTextBlocks Page #" + i);
			if(pages.get(i) != null && pages.get(i).size() > 0){
				if(possiblePart(pages.get(i).getLines(), fD)) {
					continue;
				}
				//first line
				Line firstLine = pages.get(i).getLineAt(0);
				int firstLineFontSize = getLineFontSize(firstLine);
				if(firstLineFontSize == previousLineFontSize && !BoundSimilarity.areWordsInLineCentered(firstLine, lineEdges) && (!firstLine.getText().toLowerCase().contains("chapter") && !firstLine.getText().toLowerCase().contains("appendix"))) {
					currentTextBlock.addLine(firstLine);
				} else {
					currentTextBlock = new TextBlock();
					textBlocks.add(currentTextBlock);
					currentTextBlock.addLine(firstLine);
				}
				previousLine = firstLine;
				previousLineFontSize = firstLineFontSize;
				boolean continueFirstBlock = false;
				for(int l= 1; l <  pages.get(i).size(); l++) {
					Line currentLine = pages.get(i).getLineAt(l);
					int currentLineFontSize = getLineFontSize(currentLine);
					double boundPercentage = 0.1;
					if(currentLineFontSize > fD.getBodyFontSize() && (currentLineFontSize == previousLineFontSize || l == 1)) {
						boundPercentage = 0.2;
					}
					
					/*TESTING*/
					//System.out.println("p: ... : " + previousLineFontSize );
					//System.out.println("c: ... : " + currentLineFontSize);
					/*TESTING*/
					
					boolean inBoundY = BoundSimilarity.isInYBound(previousLine,currentLine, boundPercentage);
					if((inBoundY && (currentLineFontSize >= previousLineFontSize))
							|| (l == 1 && (firstLine.getText().toLowerCase().contains("chapter") || firstLine.getText().toLowerCase().contains("appendix") ) && currentLineFontSize >= previousLineFontSize)
							|| (continueFirstBlock && currentLineFontSize == previousLineFontSize)) {
						currentTextBlock.addLine(currentLine);
						continueFirstBlock = true;
						currentTextBlock.setSpecial(true);
						
						/*TESTING*/
						//System.out.println("merging: ... : " + currentTextBlock.extractText());
						/*TESTING*/
						
					} else {
						continueFirstBlock = false;
						currentTextBlock = new TextBlock();
						textBlocks.add(currentTextBlock);
						currentTextBlock.addLine(currentLine);	
					}
					previousLine = currentLine;
					previousLineFontSize = currentLineFontSize;
				}
			}
		}
		return textBlocks;
	}
	
	private boolean areWordsInLineBoundAndAlignmentValid(Line line, Pair<Float, Float> lineEdges) {
		if(this.centerAlignmentValid) {
			return BoundSimilarity.areWordsInLineBound(line);
		} else {
			return BoundSimilarity.areWordsInLineBoundAndNotCentered(line, lineEdges);
		}
	}
	
	public List<BookSectionResource> identifyTextBlocksV2(String bookID, FormattingDictionary fD) {
		Pair<Float, Float> lineEdges = BoundSimilarity.learnLineEdges(pages, fD.getBodyFontSize());
		Float lineSpacing = BoundSimilarity.learnLineSpacing(pages, fD.getBodyFontSize());
		List<TextBlock> blocks = getTextBlocksV2(fD, lineEdges, lineSpacing);
//		for(TextBlock block: blocks) {
//			System.out.println(">> " + block.getRoleLabelString() + " fs: " + block.getFontSize());
//			System.out.println(block.extractText());
//		}
		return null;
	}
	
	public static List<TextBlock> identifyTextBlocksV2(FormattingDictionary fD, List<Line> lines) {
		Pair<Float, Float> lineEdges = BoundSimilarity.learnLineEdges(lines, fD.getBodyFontSize());
		Float lineSpacing = BoundSimilarity.learnLineSpacing(lines, fD.getBodyFontSize());
		List<TextBlock> blocks = new ArrayList<TextBlock>();
		if(lines.size() > 0) {
			blocks = getTextBlocksV2(lines, fD, lineEdges, lineSpacing);
		} 

//		for(TextBlock block: blocks) {
//			System.out.println(">> " + block.getRoleLabelString() + " fs: " + block.getFontSize());
//			System.out.println(block.extractText());
//		}
		return blocks;
	}
	
	private List<BookSectionResource> identifyTextBlocks(String bookID, FormattingDictionary fD) {
		List<BookSectionResource> bookContent = new ArrayList<BookSectionResource>();
		Pair<Float, Float> lineEdges = BoundSimilarity.learnLineEdges(pages, fD.getBodyFontSize());
			
		//System.out.println("---------lineEdges: " + lineEdges.getLeft() + " ");
		int np = 28;
		//System.out.println("---------LINES");
		//System.out.println("FS: " +  fD.getBodyFontSize());
		//System.out.println("pages.get(np).getWidth(): " + pages.get(np).getWidth());
		Line l0 = pages.get(np).getLineAt(0);
		//System.out.println("areWordsInLineBoundAndNotCentered: " + BoundSimilarity.areWordsInLineBoundAndNotCentered(l0, lineEdges) + " Centered: " + BoundSimilarity.areWordsInLineCentered(l0, lineEdges) +  " Y: " + l0.getPositionY() +" X: " + l0.getStartPositionX() + " H: " + l0.getLineHeight() + " T: " + l0.getText() +  " F: " + getLineFontSize(l0) + " Bold: " + l0.isBold() + " Italic: " + l0.isItalic() );
		for(int i = 1; i < pages.get(np).size(); i++) {
			Line l = pages.get(np).getLineAt(i);
			//System.out.println("areWordsInLineBoundAndNotCentered: " + BoundSimilarity.areWordsInLineBoundAndNotCentered(l, lineEdges) +  " InBound: " + BoundSimilarity.areWordsInLineBound(l ) + " Centered: " + BoundSimilarity.areWordsInLineCentered(l, lineEdges) + " Y: " + l.getPositionY() +" X: " + l.getStartPositionX() + " H: " + l.getLineHeight() + " FLY: " + l.getWordAt(0).getPositionY() + " F: " + getLineFontSize(l) + " T: " + l.getText() + " BN: " + BoundSimilarity.isInYBound(pages.get(np).getLineAt(i-1),l, 0.2) + " Bold: " + l.isBold() + " Italic: " + l.isItalic());
		}
		
		
		TextBlock.setFormattingDictionary(fD);
		List<TextBlock> blocks = getTextBlocks(fD, lineEdges);
		
		System.out.println("---------BLOCKS");
		for(int i = 0; i < blocks.size(); i++) {
			TextBlock block = blocks.get(i);
			System.out.println(">> BLOCK " + block.getFontSize() + " B: " + block.isBold() + " I:" + block.isItalic() + " not: " + BoundSimilarity.areLineNotCommon(block.asLine()));
			System.out.println("\t" + block.extractText());
		}
////		//System.exit(0);
		
		
		int bodyFontSize = fD.getBodyFontSize();
		Stack<FormattingContainer> styles = new Stack<FormattingContainer>();
		Stack<Integer> levels = new Stack<Integer>();
		Stack<BookSectionResource> resources = new Stack<BookSectionResource>();
		if(blocks.size() > 0) {
			if(blocks.get(0).extractText().length() == 0 || blocks.get(0).getFontSize() == 0) {
				blocks.remove(0);
			}
			TextBlock firstBlock = blocks.get(0);
			FormattingContainer firstBlockFC =  firstBlock.getFormattingContainer();
			styles.push(firstBlockFC);
			BookSectionResource currentResource = new BookSectionResource(BookSectionType.CHAPTER, firstBlock.extractText(), 1, null);
			bookContent.add(currentResource);
			resources.push(currentResource);
			System.out.println(">> FIRST BLOCK! FS: " + firstBlock.getFontSize() );
			System.out.println("\t" + firstBlock.extractText());
			System.out.println("\t centered: " + BoundSimilarity.areWordsInLineCentered(firstBlock.asLine(), lineEdges));
			if(BoundSimilarity.areWordsInLineCentered(firstBlock.asLine(), lineEdges)) {
				this.centerAlignmentValid = true;
			}
			
			
			for(int i = 1; i < blocks.size(); i++) {
				
				TextBlock block = blocks.get(i);
				FormattingContainer blockFC = block.getFormattingContainer();
				
//				if(block.extractText().contains("CHAPTER 3 Criticism in the USA: The Institutionalization of Shakespeare in the USA")) {
//					System.out.println(">> fs " + block.getFontSize());
//					System.out.println(">> blockFC.getFontSize() == firstBlockFC.getFontSize()): " + (blockFC.getFontSize() == firstBlockFC.getFontSize()));
//					System.out.println(">> BoundSimilarity.areLineNotCommon(block.asLine()): " + BoundSimilarity.areLineNotCommon(block.asLine()));
//					System.out.println(">> blockFC.getFontSize() <= bodyFontSize: " + (blockFC.getFontSize() <= bodyFontSize));
//					System.out.println(">> blockFC.getFontSize() > firstBlockFC.getFontSize(): " + (blockFC.getFontSize() > firstBlockFC.getFontSize()));
//					System.out.println(">> blockFC.getFontSize() != firstBlockFC.getFontSize(): " + (blockFC.getFontSize() != firstBlockFC.getFontSize()));
//					System.out.println(">> !BoundSimilarity.areWordsInLineBoundAndNotCentered(block.asLine(), lineEdges)): " + !BoundSimilarity.areWordsInLineBoundAndNotCentered(block.asLine(), lineEdges));
//					//System.exit(0);
//				}
				
				
			//	System.out.println(">> JUST BLOCK F: " + blockFC.getFontSize() + " T: " + block.extractText());
				if(BoundSimilarity.areLineNotCommon(block.asLine()) || blockFC.getFontSize() < bodyFontSize || blockFC.getFontSize() > firstBlockFC.getFontSize() || ( blockFC.getFontSize() != firstBlockFC.getFontSize() && !areWordsInLineBoundAndAlignmentValid(block.asLine(), lineEdges))) {
					System.out.println(">> continue 1 " + block.getFontSize());
					continue;
				} else {
					if(blockFC.getFontSize() == bodyFontSize ) {
						continue;
					}
					
					if(blockFC.getFontSize() == firstBlockFC.getFontSize()) {
						//top chapter
						System.out.println(">> BLOCK TITLE fs: " + block.getFontSize());
						System.out.println("\t" + block.extractText());
						styles.clear();
						styles.push(firstBlockFC);
						Integer last = 0;
						while(!levels.isEmpty()) {
							last = levels.pop();
						}
						last++;
						currentResource = new BookSectionResource(BookSectionType.CHAPTER, block.extractText(), bookContent.size()+1, null);
						bookContent.add(currentResource);
						resources.clear();
						resources.push(currentResource);
					} else if (!styles.empty() && blockFC.getFontSize() == styles.peek().getFontSize()) {
											
						if(blockFC.getKeySum().equals(styles.peek().getKeySum())) {
							//same level
							System.out.println(">> BLOCK SAME LEVEL SECUNDARY " + block.getFontSize());
							System.out.println("\t" + block.extractText());
							resources.pop();
							currentResource = new BookSectionResource(BookSectionType.SUBCHAPTER, block.extractText(), resources.peek().getChildrenSize() + 1, null);
							resources.peek().addChildren(currentResource);
							resources.push(currentResource);
						} else {
							System.out.println("search: " + styles.search(blockFC));
							if(styles.search(blockFC) == -1) {
								//add under
								System.out.println(">> BLOCK NEXT LEVEL 1 " + block.getFontSize());
								System.out.println("\t" + block.extractText());
								styles.push(blockFC);
								currentResource = new BookSectionResource(BookSectionType.SUBCHAPTER, block.extractText(), resources.peek().getChildrenSize() + 1, null);
								resources.peek().addChildren(currentResource);
								resources.push(currentResource);
							} else {
								//get back
								System.out.println(">> BLOCK BACK LEVEL 2 " + block.getFontSize());
								System.out.println("\t" + block.extractText());
								while(!styles.peek().equals(blockFC)) {
									styles.pop();
									resources.pop();
								}
								if(!resources.empty()) {
									resources.pop();
								}
								System.out.println("Resources peek: " + resources.peek());
								currentResource = new BookSectionResource(BookSectionType.SUBCHAPTER, block.extractText(), resources.peek().getChildrenSize() + 1, null);
								resources.peek().addChildren(currentResource);
								resources.push(currentResource);
							}
						}
					} else if (!styles.empty() && blockFC.getFontSize() > bodyFontSize) {
						//check if it is block: up
						if(blockFC.getFontSize() < styles.peek().getFontSize()) {
							System.out.println(">> BLOCK LEVEL SECUNDARY 3 " + block.getFontSize());
							System.out.println("\t" + block.extractText());
							styles.push(blockFC);
							currentResource = new BookSectionResource(BookSectionType.SUBCHAPTER, block.extractText(), resources.peek().getChildrenSize() + 1, null);
							resources.peek().addChildren(currentResource);
							resources.push(currentResource);
						} else {
							System.out.println(">> BLOCK BACK LEVEL 4 " + block.getFontSize());
							System.out.println("\t" + block.extractText());
							while(!styles.isEmpty() && !styles.peek().equals(blockFC)) {
								styles.pop();
								resources.pop();
							}
							if(!resources.empty()) {
								resources.pop();
							}
							if(!resources.empty()) {
								currentResource = new BookSectionResource(BookSectionType.SUBCHAPTER, block.extractText(), resources.peek().getChildrenSize() + 1, null);
								resources.peek().addChildren(currentResource);
								resources.push(currentResource);
							} else {
								//anomaly 
								System.out.println(">> AN 1 " + block.getFontSize());
								//currentResource = new BookSectionResource(BookSectionType.CHAPTER, block.extractText(), bookContent.size()+1);
								//bookContent.add(currentResource);
							}
							
							
						}
						
					} else {
						System.out.println(">> AN 2 " + block.getFontSize());
						System.out.println(">> firstBlockFC.getFontSize() : " + firstBlockFC.getFontSize());
//						System.out.println(">> blockFC.getFontSize() > bodyFontSize : " + (blockFC.getFontSize() > bodyFontSize));
//						System.out.println(">> blockFC.getFontSize() < styles.peek().getFontSize(): " + (blockFC.getFontSize() < styles.peek().getFontSize()));
//						System.out.println("else");
					}
				}				
				
//				if(block.extractText().equals("NOTES & COMPLEMENTS")) {				
//					System.out.println("**************");
//					System.out.println("**************");
//					System.exit(0);
//				}
				
			}
		}
		
		
		System.out.println("**********************");
		
		
		BookSectionResource.printBookContent(bookContent);
		//System.exit(0);
		return bookContent;
	}
	
	/**
	 * 
	 * @param bookID
	 */
	private void contentManagerBridge(String bookID, resourceType type){
		
//		/*TESTING*/
//		/*int words = 0;
//		for(int y = 21 ; y <= 28; y++) {
//			for(Line l : pages.get(y).getLines()) {
//				words += l.getWords().size();
//			}
//		}
//		System.out.println("Total words: " + words);*/
//		/*TESTING*/
		
		ArrayList<String> temp1 = new ArrayList<String>();
		if(type.equals(resourceType.BOOK)){		
			for(int i=firstChapterStartIndex; i<pages.size(); i++){

				SystemLogger.getInstance().log("Page #" + i + " bridged");
				if(pages.get(i) != null){

//					/*TESTING*/
//					//extracts inside the page, the coordinates and the format for each word of each line
//					//if(i < 91 || i > 135)
//					//	continue;
//					/*if(i == 21){
//						for(Line l : pages.get(i).getLines()) {
//							//System.out.println(l.getText());
//						}
//						System.out.println("Total words: " + words);
//						Line l = pages.get(i).getLineAt(3);
//						for(Text t: l.getWords()) {
//							//System.out.println(t.getText() + " B: " + t.isBold() + " I: " + t.isItalic() + " BB: " + t.getBoldCharsString() + " IB: " + t.getItalicCharsString());
//						}
//					}*/
//					//System.out.println("PI: " + i + " PN: " + pages.get(i).getPageNumber() +  "CANT: " + pages.get(i).getWords().size());
//					/*TESTING*/
					
					temp1 = new ArrayList<String>();
					pages.get(i).extractFormatData();		

					try {
						cm.addPageFormatting(bookID, pages.get(i).getFormatMap(),pages.get(i).getCoordsMap(), pages.get(i).getPageMetadata(), pages.get(i).getDictEntries());
					} catch (Exception e) {
						e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
					}	
				}
			}
		}
		else if(type.equals(resourceType.SLIDE)){
			for(int i=1; i<slides.size(); i++){
				SystemLogger.getInstance().log("Slide #" + i + " bridged");
				if(slides.get(i) != null){

					slides.get(i).extractFormatData();		

					try {
						cm.addPageFormatting(bookID, slides.get(i).getFormatMap(),slides.get(i).getCoordsMap(), slides.get(i).getPageMetadata(), slides.get(i).getDictEntries());
					} catch (Exception e) {
						e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
					}	
				}
			}
		}
	}
	
	private void createSlideToc(Vector<Slide> slides){
		
		String priorHeading = null;
		ArrayList<TOC> tempTOC = new ArrayList<TOC>();
		TOC toc=null;
		
		for(int i=1; i<slides.size(); i++){
		if(slides.get(i) != null){	
			priorHeading = slides.get(i).getSlideHeading();
			
			toc = new TOC();
			
			toc.setBold(false);
			toc.setItalic(false);
			toc.setFontSize(0);
			toc.setPageIndex((short) i);
			toc.setPageNumber(i+1);
			toc.setPosX(0);
			toc.setPosY(0);
			toc.setTitleText(priorHeading);
			
			tempTOC.add(toc);
		}
	}		
		
//		for(int i=0; i<slides.size(); i++){
//			
//			if(priorHeading == null){		
//				
//				priorHeading = slides.get(i).getSlideHeading();
//				
//				toc = new TOC();
//				
//				toc.setBold(false);
//				toc.setItalic(false);
//				toc.setFontSize(0);
//				toc.setPageNumber(i);
//				toc.setPosX(0);
//				toc.setPosY(0);
//				toc.setTitleText(priorHeading);
//				
//				tempTOC.add(toc);
//				
//			}
//			else{
//				
//				if(StringOperations.similarity(priorHeading, slides.get(i).getSlideHeading()) < 0.75){
//					
//					priorHeading = slides.get(i).getSlideHeading();
//					
//					toc = new TOC();
//					
//					toc.setBold(false);
//					toc.setItalic(false);
//					toc.setFontSize(0);
//					toc.setPageNumber(i);
//					toc.setPosX(0);
//					toc.setPosY(0);
//					toc.setTitleText(priorHeading);
//					
//					tempTOC.add(toc);
//					
//				}				
//			}			
//		}
//		
		this.toc = tempTOC;
		
	}
	
	
	public List<Page> getPages(){
		return pages;
	}
	
	public List<Page> getPagesContentOrder(){
		return pagesContentOrder;
	}
	
	public List<Slide> getSlides(){
		return slides;
	}
	
	public byte getPageNumOffset(){
		return pageNumOffset;
	}
	
	public float getPageHeight(){
		return pageHeight;
	}
	
	public float getPageWidth(){
		return pageWidth;
	}
	
	public List<TOC> getTableOfContents(){
		return toc;
	}
	
	public float getTocLeft1(){
		return tocMostleft1;
	}
	
	public float getTocLeft2(){
		return tocMostleft2;
	}
	
	public int getFirstChapterPageIndex(){
		return this.firstChapterStartIndex;
	}
	
	public ArrayList<Integer> getPageNumbers() {
		return pageNumbers;
	}

	public void setPageNumbers(ArrayList<Integer> pageNumbers) {
		this.pageNumbers = pageNumbers;
	}
	
	public String getResourceID(){
		return this.resourceName;
	}
	
	public resourceType getType(){
		return this.type;
	}
	
	public List<String> getPagesAsText(){

		List<String> corpusText =  new ArrayList<String>();
		
		if(this.type == resourceType.BOOK){
			
			for(int i = 0; i< pages.size(); i++){
				
				if(pages.get(i)!=null)
					corpusText.add(pages.get(i).getText());
				else
					corpusText.add("");			
			}			
		}
		else if(this.type == resourceType.SLIDE){
			
			for(int i = 0; i< slides.size(); i++){
				
				if(slides.get(i)!=null)
					corpusText.add(slides.get(i).getText());
				else
					corpusText.add("");			
			}			
		}
		
		return corpusText;		
	}
	
	public Map<Integer, List<String>> getPagesAsSentences() {
	
		Map<Integer, List<String>> corpusText = new HashMap<Integer, List<String>>();
		
		if(this.type == resourceType.BOOK){
			
			for(int i = 0; i< pages.size(); i++){
				
				if(pages.get(i)!=null){
					String[] sentences = pages.get(i).getText().split("\\.|\\?|\\!");
					corpusText.put(i, Arrays.asList(sentences));
				} else {
					corpusText.put(i, new ArrayList<String>());
				}
				
			}
			
		} else if(this.type == resourceType.SLIDE){
			
			for(int i = 0; i< slides.size(); i++){
				
				if(slides.get(i)!=null){
					String[] sentences = slides.get(i).getText().split("[a-z]\\.|\\?|!");
					corpusText.put(i, Arrays.asList(sentences));
				} else {
					corpusText.put(i, new ArrayList<String>());
				}
				
			}
			
		}
		
		return corpusText;	
	}
	
	public Map<Integer, List<String>> getPagesAsGroupedText() {
		
		Map<Integer, List<String>> corpusText = new HashMap<Integer, List<String>>();
		
		if(this.type == resourceType.SLIDE){
			
			for(int i = 0; i< slides.size(); i++){
				
				Slide currSlide = slides.get(i);
				
				if(currSlide!=null){
					
					List<String> listings = new ArrayList<String>();
					ArrayList<ElementBlock> groups = currSlide.getGroups();
					
					for(ElementBlock group : groups) {
						
						if(group.subGroup != null) {
							
							for(ElementBlock listing : group.subGroup) {
								
								StringBuilder linesToListing = new StringBuilder();
								
								for(int l = listing.firstLineIndx; l<= listing.lastLineIndx; l++){
									String lineText = currSlide.getLineAt(l).getText();
									linesToListing.append(lineText + " ");
								}
								
								listings.add(linesToListing.toString());
								
							}
							
						}
						
					}
					
					corpusText.put(i, listings);
					
				} else {
					corpusText.put(i, new ArrayList<String>());
				}
			}
		}
		
		return corpusText;
	}
	
	public ArrayList<List<Line>> getPagesAsLines(){
		
		ArrayList<List <Line>> pagesAsLines =  new ArrayList<List<Line>>();
		
		
		if(this.type == resourceType.BOOK){
			
			for(int i = 0; i< pages.size(); i++){
				
				if(pages.get(i)!=null)
					pagesAsLines.add(pages.get(i).getLines());
				else
					pagesAsLines.add(null);			
			}			
		}
		else if(this.type == resourceType.SLIDE){
			
			for(int i = 0; i< slides.size(); i++){
				
				if(slides.get(i)!=null)
					pagesAsLines.add(slides.get(i).getLines());
				else
					pagesAsLines.add(null);			
			}			
		}
		
		return pagesAsLines;
	}
	
	public ArrayList<ResourceUnit> getPagesAsResourceUnits(){
	
		ArrayList<ResourceUnit> pagesAsResourceUnits =  new ArrayList<ResourceUnit>();		
		
		if(this.type == resourceType.BOOK){
			
			for(int i = 0; i< pages.size(); i++){
				if(pages.get(i)!=null)
					pagesAsResourceUnits.add(pages.get(i));
				else
					pagesAsResourceUnits.add(null);			
			}			
		}
		
		return pagesAsResourceUnits;
	}
	
	public Map<String, PDFont> getFonts(){
		return this.fonts;
	}
	

	public List<BookSectionResource> getBookContent() {
		return bookContent;
	}

	public FormattingDictionary getLabelOfStyles() {
		return lOS;
	}

	public String getRawText() {
		return rawText;
	}

	public void setRawText(String rawText) {
		this.rawText = rawText;
	}

	public void setTOCTitleLine(Line tOCTitleLine) {
		TOCTitleLine = tOCTitleLine;
	}
	
	public String getTOCTitle() {
		return TOCTitleLine != null ? TOCTitleLine.getText() : "";
	}
	
	public List<Integer> getTOCPages(){
		return this.tocPages;
	}
	
	public Map<String,String> getMetadata(){
		return this.metadata;
	}
	
}
