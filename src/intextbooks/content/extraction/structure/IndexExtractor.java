package intextbooks.content.extraction.structure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import intextbooks.SystemLogger;
import intextbooks.content.ContentManager;
import intextbooks.content.enrichment.books.Extractor;
import intextbooks.content.extraction.ContentExtractor;
import intextbooks.content.extraction.Utilities.BoundSimilarity;
import intextbooks.content.extraction.Utilities.HyphenationResolver;
import intextbooks.content.extraction.Utilities.Match;
import intextbooks.content.extraction.Utilities.StringOperations;
import intextbooks.content.extraction.Utilities.WordListCheck;
import intextbooks.content.extraction.buildingBlocks.format.CharacterBlock;
import intextbooks.content.extraction.buildingBlocks.format.Line;
import intextbooks.content.extraction.buildingBlocks.format.Page;
import intextbooks.content.extraction.buildingBlocks.format.Text;
import intextbooks.content.extraction.buildingBlocks.structure.BookContent;
import intextbooks.content.extraction.buildingBlocks.structure.IndexElement;
import intextbooks.content.extraction.buildingBlocks.structure.IndexTerm;
import intextbooks.content.extraction.buildingBlocks.structure.IndexTermMatch;
import intextbooks.content.extraction.buildingBlocks.structure.IndexTermMatches;
import intextbooks.content.extraction.buildingBlocks.structure.TOC;
import intextbooks.content.models.formatting.FormattingDictionary;
import intextbooks.exceptions.NoIndexException;
import intextbooks.ontologie.LanguageEnum;
import intextbooks.tools.utility.GeneralUtils;
import intextbooks.tools.utility.ListOperations;


public class IndexExtractor {
	
	ColumnsFormat columnFormat;
    public static final String seeDelimiters = ",.(";
    public static final String inLineDelimiter = ";";
    public static final String allDelimiters = ",;.";
    public static final String termDivider = "|#|";
    public static final String termDividerRegex = "\\s\\|#\\|\\s";
	public String termDelimiter = ",";
	public String pageReferenceDelimiter = ",";
	public int spaceDelimiterSize = 0;;
	private static ContentManager cm = ContentManager.getInstance();
	private String bookID;
	private int biggetsPageNumber = -1;
	private HyphenationResolver hyphenResolver;
	
	int firstIndexPage = -1;
	
	private class Segment {
		float start;
		float end;
		boolean blank;
		int hits;
		
		public Segment(float s, float e) {
			this.start = s;
			this.end = e;
			this.blank = false;
			this.hits = 0;
		}
		
		public void incHit() {
			this.hits++;
		}
		
		public void setBlank(boolean b) {
			this.blank = b;
		}
		
		public boolean getBlank() {
			return this.blank;
		}
		
		public int getHits() {
			return this.hits;
		}

		public float getStart() {
			return start;
		}

		public void setStart(float start) {
			this.start = start;
		}

		public float getEnd() {
			return end;
		}

		public void setEnd(float end) {
			this.end = end;
		}
		
		public void check(float wStart, float wEnd) {
			if( (wStart >= start && wStart < end)
				|| (wEnd >= start && wEnd < end)
				|| (wStart < start && wEnd > end)){
					this.incHit();
				}
		}
		
		public void merge(Segment otherSegment) {
			if(this.blank == otherSegment.getBlank()) {
				this.hits += otherSegment.getHits();
				this.end = otherSegment.end;
			}
		}

		@Override
		public String toString() {
			return "Segment [start=" + start + ", end=" + end + ", blank=" + blank + ", hits=" + hits + "]";
		}
	}

	public IndexExtractor(String bookID) {
		this.bookID = bookID;
		this.hyphenResolver = HyphenationResolver.getInstance(this.cm.getBookLanguage(bookID));
	}
	
	public int getFirstIndexPage() {
		return this.firstIndexPage;
	}
	
	private class ColumnsFormat{
		int numberOfColumns;
		List<Integer> startOfColumns;
		
		public ColumnsFormat() {	
			numberOfColumns = 1;
			startOfColumns = new ArrayList<Integer>();
			startOfColumns.add(0);
		}
		
		public void addColumn(Float posX) {
			numberOfColumns++;
			startOfColumns.add(posX.intValue());
		}
		
		public int getNumberOfColumns() {
			return this.numberOfColumns;
		}
		
		public int getStartOfColumn(int column) {
			return this.startOfColumns.get(column);
		}
		
		public List<Integer> getStartOfColumns() {
			return this.startOfColumns;
		}
		
	}

	/**
	 *
	 * @param resourcePages :
	 * @param toc
	 * @param indexTerms
	 * @throws IOException
	 * @throws NoIndexException 
	 *
	 **/

	public List<IndexElement> extractIndex(List<Page> resourcePages, List<TOC> toc, String bookID, FormattingDictionary styleLibrary, Map<String, PDFont> fonts, boolean processReadingLabels, Map<String, String> metadata) throws IOException, NoIndexException{

		SystemLogger.getInstance().log("Index extraction Start");

		Vector <Line> pseudoIndex = new Vector <Line> ();

		/*
		 * It finds the Index pages and extract the lines
		 */
		pseudoIndex = findIndexPages(resourcePages, toc, styleLibrary, fonts, metadata);
		
//		/*TESTING*/
//		System.out.println(">>>>> pesudoIndex start --> Index size:" + pseudoIndex.size() );
//		Iterator<Line> it = pseudoIndex.iterator();
//		while(it.hasNext()) {
//			Line l = it.next();
//			System.out.println(l.getStartPositionX() + " " + l.getText() );
//		}
//		System.out.println(">>>>> pesudoIndex end" );
//		System.exit(0);
//		/*TESTING*/
		
		/*
		 * It fixes hyphenation
		 */
		SystemLogger.getInstance().log("Resolving hypens ... ");
		exhaustiveHyphenFix(pseudoIndex);
		
		/*
		 * It fixes lines with dashes for range of pages
		 */
		SystemLogger.getInstance().log("Resolving dashes for range of pages ... ");
		exhaustiveIndexDashFix(pseudoIndex);
		
		SystemLogger.getInstance().log("Resolving references to notes in index pages ... ");
		exhaustiveNotesFix(pseudoIndex);

		SystemLogger.getInstance().log("Resolving section title in pages ... ");
		exhaustiveLetterTitleRemover(pseudoIndex);
		
//		/*TESTING*/
//		System.out.println(">>>>> pesudoIndex  after exhaustiveIndexDashFix & exhaustiveLetterTitleRemover--> Index size:" + pseudoIndex.size() );
//		Iterator<Line> it = pseudoIndex.iterator();
//		while(it.hasNext()) {
//			Line t = it.next();
//			System.out.println(t.getStartPositionX() + " " + t.getText() );
//			//System.out.println(t.getStartPositionX() );
//		}
//		System.out.println(">>>>> pesudoIndex after exhaustiveIndexDashFix end" );
//		System.exit(0);
//		/*TESTING*/
		
		SystemLogger.getInstance().log("Detecting term delimiter ... ");
		findTermDelimeter(pseudoIndex);	
		
		SystemLogger.getInstance().log("Resolving \"see/also\" references in pages ... ");
		exhaustiveSeeCasesResolver(pseudoIndex);
		
		SystemLogger.getInstance().log("Resolving roman references in pages ... ");
		exhaustiveRomanReferencesFix(pseudoIndex);
		
//		/*TESTING*/
//		System.out.println(">>>>> pesudoIndex  after exhaustiveIndexDashFix & exhaustiveLetterTitleRemover--> Index size:" + pseudoIndex.size() );
//		Iterator<Line> it = pseudoIndex.iterator();
//		while(it.hasNext()) {
//			Line t = it.next();
//			System.out.println(t.getStartPositionX() + " " + t.getText() );
//			//System.out.println(t.getStartPositionX() );
//		}
//		System.out.println(">>>>> pesudoIndex after exhaustiveIndexDashFix end" );
//		System.exit(0);
//		/*TESTING*/
		/*
		 * It creates index terms, grouping different lines into one when corresponding
		 */
		SystemLogger.getInstance().log("Creating index elements ... ");
		pseudoIndex = this.createIndexElements(pseudoIndex, cm.getBookLanguage(bookID));
		
//		/*TESTING*/
//		System.out.println(">>>>> pesudoIndex  after createIndexElements --> Index size:" + pseudoIndex.size() );
//		Iterator<Line> it = pseudoIndex.iterator();
//		while(it.hasNext()) {
//			Line t = it.next();
//			System.out.println(t.getText() );
//			System.out.println("\t" + t.getAllProperties());
//			//System.out.println(t.isArtificial() );
//		}
//		System.out.println(">>>>> pesudoIndex after concat lines end" );
//		System.exit(0);
//		/*TESTING*/

		/*
		 * Removes special cases, useful for constructing glossary for DBPEDIA enrichment
		 */
		//SystemLogger.getInstance().log("Removing special cases ... ");
		//removeIndexElementsSpecialCases(pseudoIndex);
		
		SystemLogger.getInstance().log("Index size:" + pseudoIndex.size() );

		//* Convert from Lines to Index Elements
		SystemLogger.getInstance().log("Converting index elements to internal structure ... ");
		Vector<IndexElement> indexElements = convertToIndexElements(resourcePages, pseudoIndex);
		
		//compute just the permutations for the index elements
		for(int i = 0; i < indexElements.size(); i++) {
			indexElements.get(i).getLabelPermutations();
		}

		/*
		 * log all entries
		 */
		
		for(int k=0; k<pseudoIndex.size();k ++)
			SystemLogger.getInstance().log(pseudoIndex.get(k).getText());
		
//		/*TESTING*/
//		SystemLogger.getInstance().log("List of Index Elements: " + indexElements.size());
//		for(IndexElement iE: indexElements) {
//			SystemLogger.getInstance().log(iE.toString());
//		}
//		System.exit(0);
//		/*TESTING*/
		
		//OLD-TESTING
		//* Write resourcePages and BookPages
//		WriterReader.writeBookPages(bookID, resourcePages);
		
//		/*STATISTICS*/
//		//System.out.println("Statstics #1");
//		//CountDirectLabelsApproachForIndexElements(resourcePages,indexElements);
//		System.out.println("Statstics #2");
//		CountNounPhrasesApproachLabelsForIndexElements(resourcePages,indexElements);
//		System.exit(0);
//		/*STATISTICS*/
		
		//findIndexElementsInBook(resourcePages,indexElements);
		//FOR GETTING VALUES
		//FindLabelsForIndexElements(resourcePages, indexElements);
		//printPageText(resourcePages, indexElements);
		
//		/*TESTING*/
//		QuickFileWriter f = new QuickFileWriter("IndexElements.txt");
//		for(IndexElement indexElement: indexElements) {
//			f.write(indexElement.toString());
//		}
//		f.close();
//		f = new QuickFileWriter("IndexTerms.txt");
//		for(IndexTerm indexTerm: indexTerms) {
//			f.write(indexTerm.toString());
//		}
//		System.exit(0);
//		/*TESTING*/
		
//		/*TESTING*/
//		//IndexElement
//		Iterator<IndexElement> it = indexElements.iterator();
//		int n = 0;
//		while(it.hasNext()) {
//			n++;
//			IndexElement el = it.next();
//			System.out.println(el);
//			//System.out.println("parts Size: " + el.getParts().size());
//			//System.out.println("permutations Size: " + el.getLabelPermutations().size() + " : " + el.getLabelPermutations());
//		}
//		System.exit(0);
//		/*TESTING*/
	
		
		SystemLogger.getInstance().log("Index extraction....Done");
		
		return indexElements;	
	}

	private float calculateMostLeft(List <Line> vec){

		float mostLeft = 999999;

		if(vec!= null && !vec.isEmpty())
			for(short i=0; i< vec.size(); i++){

				if(mostLeft > vec.get(i).getStartPositionX()){

					mostLeft = vec.get(i).getStartPositionX();
				}
			}

		return mostLeft;
	}

/**
 *
 * @param left
 * @param right
 * @return
 */
	private Vector <Line> lineUpColumns(Vector<Line> left, Vector<Line>right){

		float leftMostLeft = 0, rightMostLeft = 0 , diff = 0;
		Vector <Line> result = new Vector <Line> ();

		leftMostLeft = calculateMostLeft(left);
		rightMostLeft = calculateMostLeft(right);

		diff = leftMostLeft - rightMostLeft;

		for(int i = 0 ; i < right.size() ; i++  ){

			for(int j = 0; j < right.get(i).size() ; j++){

				right.get(i).getWordAt(j).setStartPositionX(right.get(i).getWordAt(j).getStartPositionX() + diff);
			}

			right.get(i).setStartPositionX(right.get(i).getStartPositionX() + diff);
		}

		result.addAll(left);
		result.addAll(right);

		return result;
	}

/**
 *
 * @param pageLines
 * @param offset
 * @return
 */
	private void extractColumnedPage (List<Line> pageLines, byte offset, ColumnedPage columnedPage){

		float a = calculateMostLeft(pageLines);

		Vector<Line> tempLineBuffer = new Vector <Line> (pageLines);

		Vector<Line> column = new Vector<Line>();

		short counter = (short) (offset);

		for(counter = 0; counter < pageLines.size(); counter++ ){
			if( BoundSimilarity.isInBound(a, pageLines.get(counter).getStartPositionX(),
					pageLines.get(counter).getFontSize(), pageLines.get(counter).getFontSize(), 5.5f)){

				column.add(pageLines.get(counter));
			}
		}

		tempLineBuffer.removeAll(column);
		columnedPage.addColumn(column);

		//there are more columns to be processed
		if(tempLineBuffer.size()>0){
			extractColumnedPage(tempLineBuffer, (byte) 0, columnedPage);
		}
	}

/**
 *
 * @param pageLines
 * @param offset
 * @return
 */
	private Vector<Line> extractColumn(List<Line> pageLines, byte offset){



		float a = calculateMostLeft(pageLines);

//		/*TESTING*/
//		SystemLogger.getInstance().log(">>>>> PAGES LINES");
//		Iterator<Line> it = pageLines.iterator();
//		while(it.hasNext()) {
//			Line tmp = it.next();
//			SystemLogger.getInstance().log(">" + tmp.getText());
//			SystemLogger.getInstance().log(">> pos: " + tmp.getStartPositionX());
//			SystemLogger.getInstance().log(">> size: " + tmp.getFontSize());
//			boolean val = BoundSimilarity.isInBound(a, tmp.getStartPositionX(),
//					tmp.getFontSize(), tmp.getFontSize(), 5.5f);
//			SystemLogger.getInstance().log(">> inbound: " + val);
//		}
//		SystemLogger.getInstance().log(">>>>> PAGES LINES END" );
//		/*TESTING*/

		Vector<Line> tempLineBuffer = new Vector <Line> (pageLines);

		Vector<Line> column = new Vector<Line>();

		short counter = (short) (offset);

		for(counter = 0; counter < pageLines.size(); counter++ ){
			if( BoundSimilarity.isInBound(a, pageLines.get(counter).getStartPositionX(),
					pageLines.get(counter).getFontSize(), pageLines.get(counter).getFontSize(), 5.5f)){

				column.add(pageLines.get(counter));
			}
		}

		tempLineBuffer.removeAll(column);

		if(tempLineBuffer.size()>0){

			Vector<Line> rightColumn = extractColumn(tempLineBuffer, (byte) 0);

//			/*TESTING*/
//			SystemLogger.getInstance().log(">>>>> LEFT COLUM");
//			it = column.iterator();
//			while(it.hasNext())
//				SystemLogger.getInstance().log(">" + it.next().getText());
//			SystemLogger.getInstance().log(">>>>> LEFT COLUM" );
//			SystemLogger.getInstance().log(">>>>> RIGHT COLUM");
//			it = rightColumn.iterator();
//			while(it.hasNext())
//				SystemLogger.getInstance().log(">" + it.next().getText());
//			SystemLogger.getInstance().log(">>>>> RIGHT COLUM" );
//			/*TESTING*/

			column = lineUpColumns(column, rightColumn);
		}

		return column;
	}

	/**
	 *
	 * @param page
	 * @param titlePresent
	 * @return
	 */

	private ColumnedPage extractColumnsofIndexPage(Page page, boolean titlePresent){
		if(titlePresent)
			page.removeLineAt(0);

		for(int i = 0 ; i < page.size() ; i++){

			if(page.getLineAt(i).isBold()
					&&	!page.getLineAt(i).getText().replaceAll(" ", "").matches("\\d+")){

				page.removeLineAt(i);
				i--;
			}
		}

		page.pageText();

		ColumnedPage columnedPage = new ColumnedPage();
		extractColumnedPage(page.getLines(),(byte) 0, columnedPage);

		return columnedPage;
	}
/**
 *
 * @param book
 * @param toc
 * @return
 */
	private Pair<Integer,Line> findIndexPage(List<Page> book, List<TOC> toc, FormattingDictionary styleLibrary){

		boolean inTOC = false;
		int pageNumber=0;
		int pageIndex = -1;
		Line line = null;

		//look for preferred index
		for(int i=toc.size()-1 ; i>0 ; i--){

			String title="";

			title = toc.get(i).getTitleText();

			if(WordListCheck.matchesPreferredIndex(title)){
				inTOC = true;
				pageNumber = toc.get(i).getPageNumber();
				pageIndex = findPageIndexWithPageNumber(book, pageNumber);
				
				for(int l = 0; l < book.get(pageIndex).getLines().size(); l++) {
					if(WordListCheck.matchesIndex(book.get(pageIndex).getLines().get(l).getText())){
						line = book.get(pageIndex).getLines().get(l);
						break;
					}
				}
				/*TESTING*/
				//SystemLogger.getInstance().log("1 !! found: " + pageNumber + " " + pageIndex );
				/*TESTING*/
				return Pair.of(pageIndex, line);
			}
		}
		
		for(int i=toc.size()-1 ; i>0 ; i--){

			String title="";

			title = toc.get(i).getTitleText();

			if(WordListCheck.matchesIndex(title) && !title.toLowerCase().contains("author")){
				inTOC = true;
				pageNumber = toc.get(i).getPageNumber();
				pageIndex = findPageIndexWithPageNumber(book, pageNumber);
				
				for(int l = 0; l < book.get(pageIndex).getLines().size(); l++) {
					if(WordListCheck.matchesIndex(book.get(pageIndex).getLines().get(l).getText())){
						line = book.get(pageIndex).getLines().get(l);
						break;
					}
				}
				/*TESTING*/
				//SystemLogger.getInstance().log("2 !! found: " + pageNumber + " " + pageIndex );
				/*TESTING*/
				return Pair.of(pageIndex, line);
			}
		}
		
		/*TESTING*/
//		for(Page p: book) {
//			if(p != null)
//				System.out.println("I: " + p.getPageIndex() + " / PN: " + p.getPageNumber());
//		}
//		System.exit(0);
		/*TESTING*/

		if(!inTOC){

			for(int i=book.size()-1 ; i>0 ; i--){

				if(book.get(i) != null && book.get(i).size() > 1 && WordListCheck.matchesPreferredIndex(book.get(i).getLineAt(0).getText()) && book.get(i).getLineAt(0).getFontSize() > styleLibrary.getBodyFontSize()) {
					pageIndex = i;
					line = book.get(i).getLineAt(0);
					return Pair.of(pageIndex, line);
				}
				if(book.get(i) != null && book.get(i).size() > 1 && WordListCheck.matchesPreferredIndex(book.get(i).getLineAt(1).getText()) && book.get(i).getLineAt(1).getFontSize() > styleLibrary.getBodyFontSize()) {
					pageIndex = i;
					line = book.get(i).getLineAt(1);
					return Pair.of(pageIndex, line);
				}
			}
			
			for(int i=book.size()-1 ; i>0 ; i--){

				if(book.get(i) != null && book.get(i).size() > 1 && WordListCheck.matchesIndex(book.get(i).getLineAt(0).getText()) && book.get(i).getLineAt(0).getFontSize() > styleLibrary.getBodyFontSize()) {
					pageIndex = i;
					line = book.get(i).getLineAt(0);
					return Pair.of(pageIndex, line);
				}
				if(book.get(i) != null && book.get(i).size() > 1 && WordListCheck.matchesIndex(book.get(i).getLineAt(1).getText()) && book.get(i).getLineAt(1).getFontSize() > styleLibrary.getBodyFontSize()) {
					pageIndex = i;
					line = book.get(i).getLineAt(1);
					return Pair.of(pageIndex, line);
				}
			}
		}
		
		return Pair.of(-1, null);
	}
	
	private boolean isIndexPage(List <Line> page, Line indexTitleLine, Line originalFirstLine){
		SystemLogger.getInstance().debug("Size page: " + page.size());
		if(indexTitleLine != null) {
			int countEqual = 0;
			
			if(!originalFirstLine.equals(indexTitleLine) && originalFirstLine.getWordAt(0).getFontSize() == indexTitleLine.getWordAt(0).getFontSize()
					&& (BoundSimilarity.isInBound( originalFirstLine.getStartPositionX(), indexTitleLine.getStartPositionX(), originalFirstLine.getWordAt(0).getFontSize(), indexTitleLine.getWordAt(0).getFontSize(), 0.2f )
					|| BoundSimilarity.isInBound( originalFirstLine.getEndPositionX(), indexTitleLine.getEndPositionX(), originalFirstLine.getWordAt(0).getFontSize(), indexTitleLine.getWordAt(0).getFontSize(), 0.2f )
					) ) {
				
				countEqual++;
			}
			
			for(int i = 0; i < page.size(); i++){
				if(!page.get(i).equals(indexTitleLine) && page.get(i).getWordAt(0).getFontSize() == indexTitleLine.getWordAt(0).getFontSize()
						&& (BoundSimilarity.isInBound( page.get(i).getStartPositionX(), indexTitleLine.getStartPositionX(), page.get(i).getWordAt(0).getFontSize(), indexTitleLine.getWordAt(0).getFontSize(), 0.2f )
						|| BoundSimilarity.isInBound( page.get(i).getEndPositionX(), indexTitleLine.getEndPositionX(), page.get(i).getWordAt(0).getFontSize(), indexTitleLine.getWordAt(0).getFontSize(), 0.2f )
						) ) {
					countEqual++;
				}
			}

			if(countEqual >= 1) {
				SystemLogger.getInstance().debug("Return because of indexTitleLine");
				return false;
			}
		}
		
		int range;
		int counter = 0;

		SystemLogger.getInstance().debug("size: " + page.size());
		if(page.size() / 2 < 21){
			range = page.size();
		}
		else{
			range = page.size() / 2;
		}

		for(int j=0 ; j < range - 1; j++){
			
			String lineText = page.get(j).getText().replaceAll("\\W", "");
			SystemLogger.getInstance().debug("line: " + page.get(j).getText() + " lineText: " + lineText);
			
			if(lineText.matches("[a-zA-Z]+[0-9]+")){
				SystemLogger.getInstance().debug("yes: " + lineText);
				counter++;
			} if (lineText.matches("[0-9]+")) {
				SystemLogger.getInstance().debug("yes 2: " + lineText);
				counter++;
			} else if (!BoundSimilarity.isInBound(page.get(j).getStartPositionX(),page.get(j+1).getStartPositionX(),page.get(j).getFontSize(),page.get(j).getFontSize(),0.2f ) && page.get(j+1).getStartPositionX() > page.get(j).getStartPositionX() )  {
				SystemLogger.getInstance().debug("yes 4: " + lineText);
				counter++;
			} else {
				boolean all = true;
				for(Text word: page.get(j).getWords() ) {
					if(!wordIsPageReference(word.getText())){
						all =false;
						break;
					}
				}
				if(all) {
					SystemLogger.getInstance().debug("yes 5 (all words are locators): " + lineText);
					counter++;
				}
				
			}
		}

		SystemLogger.getInstance().debug("counter: " + counter);
		SystemLogger.getInstance().debug("range/2: " + range/2);

		if(counter>=range/2){
			return true;
		}
		else{
			return false;
		}

	}
	
	private int findIndexFontSize(List<Line> page) {
		Map<Float,Integer> fontMap = new HashMap<Float,Integer>();
		Iterator<Line> it = page.iterator();
		//count
		while(it.hasNext()) {
			Line l = it.next();
			Integer n = fontMap.get(l.getFontSize());	
			if(n == null) {
				n = 0;
			}
			n++;
			fontMap.put(l.getFontSize(), n);
		}
		//biggest
		Integer biggestN = 0;
		Float fontSize = 0f;
		for(Entry<Float,Integer> entry: fontMap.entrySet()) {
			if(entry.getValue() > biggestN) {
				biggestN = entry.getValue();
				fontSize = entry.getKey();
			}
		}
		return fontSize.intValue();
	}
	
	private void removeNoteLines(List<Line> page, int fontSize) {
		SystemLogger.getInstance().debug("fontSize: " + fontSize);
		Iterator<Line> it = page.iterator();
		while(it.hasNext()) {
			Line l = it.next();
			SystemLogger.getInstance().debug("FS: " + l.getFontSize() + " T: " + l.getText());
			if(((int)l.getFontSize()) < fontSize) {
				SystemLogger.getInstance().debug("DELETED :  " + l.getText());
				it.remove();
				
			}
		}
	}
	
	private boolean wordInColumn(int start, int end, int posX, int startOfNextWordX) {
		int diff = 4;
		//1 if the upper bound is to close to the limit, and the word is not in-line bound with the predecessor, then it belongs to the next column
		if(posX >= (start - diff) && posX < end && Math.abs(end - posX) < diff && Math.abs(startOfNextWordX - posX) > 1) {
			return false;
		} else if(posX >= (start - diff) && posX < end ){
			return true;
		} else {
			return false;
		}
	}
	
	private ColumnedPage asColumnedPage(List<Line> pageLines, ColumnsFormat cF,  boolean titlePresent) {
		ColumnedPage columnedPage = new ColumnedPage();
		Vector<Vector<Line>> columns = new Vector<Vector<Line>>();
		//create columns
		for(int i = 0; i < cF.getNumberOfColumns(); i++) {
			Vector<Line> tempColumn = new Vector<Line>();
			columns.add(tempColumn);
			columnedPage.addColumn(tempColumn);
			SystemLogger.getInstance().debug("creating column : "+ i);
		}
		
		//add lines to columns
		SystemLogger.getInstance().debug("page lines size : "+ pageLines.size());
		SystemLogger.getInstance().debug("titlePresent: "+ titlePresent);
		lines:
		for(int i = 0; i < pageLines.size(); i++) {
			Line currentLine = pageLines.get(i);
			if(titlePresent) {
				if(WordListCheck.matchesIndex(currentLine.getText())) {
					titlePresent = false;
					continue;
				}
			}
			SystemLogger.getInstance().debug("line : " + currentLine.getText());
			Line newLine = new Line();
			List<Text> words = currentLine.getWords();
			int currentColumn = 0;
			float endOfLastTextX = 0f;
			float lastWordSpacing = 0f;
			int startOfNextWordX = 0;
			for(int j = 0; j < words.size(); j++ ) {
				Text word = words.get(j);
				float currentWidthOfSpace = word.getSpaceWidth();
				
				if(j == 0) {
					endOfLastTextX = words.get(0).getEndPositionX();
					lastWordSpacing = words.get(0).getSpaceWidth();
				} else {
					startOfNextWordX = (int) (endOfLastTextX + (currentWidthOfSpace));
				}
				 
				SystemLogger.getInstance().debug("w : " + word.getText());
				//if last column
				boolean wordInColumn;
				if((currentColumn + 1) == cF.numberOfColumns) {
					wordInColumn = wordInColumn(cF.getStartOfColumn(currentColumn), 99999, (int) word.getStartPositionX(), startOfNextWordX);
				} else {
					wordInColumn = wordInColumn(cF.getStartOfColumn(currentColumn), cF.getStartOfColumn(currentColumn+1), (int) word.getStartPositionX(), startOfNextWordX);
					SystemLogger.getInstance().debug("getStartOfColumn : " + cF.getStartOfColumn(currentColumn));
					SystemLogger.getInstance().debug("cF.getStartOfColumn(currentColumn+1) : " + cF.getStartOfColumn(currentColumn+1));
					SystemLogger.getInstance().debug("word.getStartPositionX() : " + word.getStartPositionX());
				}
				
				//add to current column or to the next one
				if(wordInColumn) {
					//System.out.println("YES: " + word.getText());
					newLine.addWord(word);
				} else {
					SystemLogger.getInstance().debug("NO: " + word.getText());
					if(j != 0) {
						if(Math.round(word.getStartPositionX()) < (startOfNextWordX)) {
							SystemLogger.getInstance().debug("beginning : " + word.getStartPositionX());
							SystemLogger.getInstance().debug("startOfNextWordX : " + startOfNextWordX);
							SystemLogger.getInstance().debug("~~~~~~ : " + currentLine.getText());
							//if in the last lines only
							if((i + 5) >  pageLines.size()) {
								break lines;
							} else {
								continue lines;
							}
							
						}
					}
					
					//second check for special line: if the word is in-line bound, then it is a non-index line and should be eliminated
					//////////////////////////////////////////////////////////
					if(Math.abs(startOfNextWordX - word.getStartPositionX()) <= 1 && cF.getNumberOfColumns() > 1){
						if((i + 5) >  pageLines.size()) {
							break lines;
						} else {
							continue lines;
						}
					}
					//////////////////////////////////////////////////////////
					
					
					//add to current line
					if(newLine.size() > 0) {
						columns.get(currentColumn).add(newLine);
						newLine.extractValues();
					}
					newLine = new Line();
					newLine.addWord(word);
					
					//check if word is in current column or it needs to skip it to the next one
					currentColumn++;
					boolean keep = true;
					while(keep ) {
						if((currentColumn + 1) == cF.numberOfColumns) {
							wordInColumn = wordInColumn(cF.getStartOfColumn(currentColumn), 99999, (int) word.getStartPositionX(), startOfNextWordX);
						} else {
							wordInColumn = wordInColumn(cF.getStartOfColumn(currentColumn), cF.getStartOfColumn(currentColumn+1), (int) word.getStartPositionX(), startOfNextWordX);
						}
						
						if(wordInColumn) {
							keep = false;
							break;
						} else {
							currentColumn++;
						}	
					}
					
					SystemLogger.getInstance().debug("Current column: " + currentColumn);
				}
				
				endOfLastTextX = word.getEndPositionX();
				lastWordSpacing = currentWidthOfSpace;
			}
			if(newLine.size() > 0) {
				columns.get(currentColumn).add(newLine);
				newLine.extractValues();
			}
			
		}
		
		
		return columnedPage;
	}
	
	private int discoverSpaceWordsToNumbers(List<Line> pageLines) {
		//K: space, V: amount
		HashMap<Integer, Integer> spaces = new HashMap<Integer, Integer>();
		for(int i = 0; i < pageLines.size(); i++) {
			List<Text> words = pageLines.get(i).getWords();
			if(words.size() > 0) {
				boolean beforeText = false;
				float beforeX = 0;
				String beforeTextWord = "";
				for(int j = 0; j < words.size(); j++) {
					Text w = words.get(j);
					String textWord = w.getText();
					textWord = textWord.replace(",", "").replace("-", "").replace("–", "").replace(".", "");
					if(textWord.matches("\\d+") && beforeText) {
						float diff = w.getStartPositionX() - beforeX;
						int diffInt = Math.round(diff);
						Integer cant = spaces.get(diffInt);
						if(cant == null) {
							cant = 0;
						}
						cant++;
						spaces.put(diffInt, cant);
						beforeText = false;
						SystemLogger.getInstance().debug(" diff between : " + beforeTextWord + " vs. " + w.getText() + " diff: " + diffInt);
					} else if (textWord.matches("\\d+") && !beforeText){
						
					} else if (!textWord.matches("\\d+")){
						beforeText = true;
						beforeX = w.getEndPositionX();
						beforeTextWord = w.getText();
					}
				}
			}
		}
		
		//find biggest
		int totalCount =0;
		int biggestKey = 0;
		int biggestVal = 0;
		for(Entry<Integer,Integer> entry : spaces.entrySet()) {
			if(entry.getValue() > biggestVal) {
				biggestVal = entry.getValue();
				biggestKey = entry.getKey();
				
			}
			totalCount += entry.getValue();
		}
		
		SystemLogger.getInstance().debug("Count #: " + biggestKey + " Count: " + biggestVal + " Total count: " + totalCount);
	
		if(biggestVal > totalCount/2) {
			return biggestKey;
		} else {
			return -1;
		}
	}
	
	private ColumnsFormat discoverColumnFormat(List<Line> pageLines) {
		//SystemLogger.getInstance().setDebug(true);
		//SystemLogger.getInstance().debug("DETECTED: " + breakN + " " + word.getText());
		
		//List<Line> linesCopy = new ArrayList<Line>
		
		//remove title section
		Iterator<Line> iterator = pageLines.iterator();
		int checkLines = 5;
		while(iterator.hasNext() && checkLines > 0) {
			Line currentLine = iterator.next();
			if(WordListCheck.matchesIndex(currentLine.getText()) || WordListCheck.matchesPreferredIndex(currentLine.getText())) {
				iterator.remove();
			}
			checkLines--;
		}
		
		float startLeft = 99999;
		float endRight = 0;
		float s;
		float e;
		for(Line l: pageLines) {
			s = l.getStartPositionX();
			e = l.getEndPositionX();
			if(s < startLeft) {
				startLeft = s;
			}
			if(e > endRight) {
				endRight = e;
			}
		}
		
		//average width of characters
		List<CharacterBlock> list = new ArrayList<CharacterBlock>();
		for(Line l: pageLines) {
			list.addAll(l.getCharactersInLine());
		}
		float totalWidth = 0;
		for(CharacterBlock c: list) {
			totalWidth += c.getBBWidth();
		}
		float averageWidth = totalWidth / list.size();
		
		List<Segment> segments = new ArrayList<Segment>();
		Float segmentStart = startLeft;
		Float segmentInterval = averageWidth;
		Float segmentEnd = segmentStart + segmentInterval;
		while(segmentStart <= endRight) {
			segments.add(new Segment(segmentStart, segmentEnd));
			segmentStart = segmentEnd;
			segmentEnd = segmentStart + segmentInterval;
		}
		
		for(Segment seg: segments) {
			for(Line line: pageLines) {
				for(Text word: line.getWords()) {
					seg.check(word.getStartPositionX(), word.getEndPositionX());
				}
			}
		}
		
		for(Segment seg: segments) {
			int hits = seg.getHits();
			if(hits == 0) {
				seg.setBlank(true);
			} else {
				seg.setBlank(false);
			}
		}
		
		Iterator<Segment> it = segments.iterator();
		if(it.hasNext()) {
			Segment previous = it.next();
			while(it.hasNext()) {
				Segment segment = it.next();
				if(previous.getBlank() == segment.getBlank()) {
					previous.merge(segment);
					it.remove();
				} else {
					previous = segment;
				}
				
			}
		}
		
		int totalA = 0;
		for(Segment seg: segments) {
			if(seg.getBlank()) {
				totalA++;
			}
		}
		
//		/*TESTING*/
//		System.out.println("startLeft: " + startLeft);
//		System.out.println("endRight: " + endRight);
//		System.out.println("segments: " + segments);
//		System.out.println("totalA: " + totalA + " out of " + segments.size());
//		/*TESTING*/
		
		ColumnsFormat cF = new ColumnsFormat();
		boolean first = true;
		for(Segment seg: segments) {
			if(!seg.getBlank()) {
				if(!first) {
					cF.addColumn(seg.getStart());
					SystemLogger.getInstance().debug("COLUMNS X: " + seg.getStart());
				} else {
					first = false;
				}
			}
		}
		
		SystemLogger.getInstance().debug("NUMBER OF COLUMNS: " + cF.getNumberOfColumns() +  " Start: " + cF.getStartOfColumns());
		
		return cF;
	}
	
	/*private boolean isColumnLine(Line line, ColumnsFormat cF) {
		for(int i = 0; i < line.size(); i++) {
			
		}
	}*/
	
	private float calculateMostLeftRightForIndex(List<Line> page) {
		HashMap<Float, Integer> fontSizes = new HashMap <Float, Integer>();
		for(Line line: page) {
			Integer cant = fontSizes.get(line.getFontSize());
			if(cant == null) {
				cant = 0;
			}
			cant++;
			fontSizes.put(line.getFontSize(), cant);
		}
		
		float biggestCommonFontSize =0;
		int biggestCant = 0;
		for(Entry<Float, Integer> entry : fontSizes.entrySet()) {
			if(entry.getValue() > biggestCant) {
				biggestCant = entry.getValue();
				biggestCommonFontSize = entry.getKey();
			}
		}
		
		float mostLeft = 99999;
		for(Line line: page) {
			if(line.getFontSize() == biggestCommonFontSize) {
				if(line.getStartPositionX() < mostLeft) {
					mostLeft = line.getStartPositionX();
				}
			}
		}
		
		return mostLeft;
	}

	/**
	 *
	 * @param book
	 * @param toc
	 * @return
	 * @throws NoIndexException 
	 * 
	 * 
	 */

	private Vector<Line> findIndexPages(List<Page> book, List<TOC> toc, FormattingDictionary styleLibrary, Map<String, PDFont> fonts, Map<String, String> metadata) throws NoIndexException {

		SystemLogger.getInstance().log("Identifying Index Pages.");

		Vector <Line> indexLines = null;

		Pair<Integer,Line> resultIndex = findIndexPage(book, toc, styleLibrary);
		int pageIndex = resultIndex.getLeft();
		
		float pageIndexMostLeft = 0;
		boolean needForXNormalization = true;
		Line indexTitleLine = resultIndex.getRight();
		
		
		if(pageIndex == -1){
			throw new NoIndexException("No Index Segment Exists");
		}
		else{
			SystemLogger.getInstance().log("Page Index: " + pageIndex);
			this.firstIndexPage = pageIndex;

			indexLines=  new Vector<Line>();
			Vector<ColumnedPage> columnedPages = new Vector<ColumnedPage>();
			boolean titlePresent;
			ColumnsFormat cF = null;
			
//			/*TESTING*/
//			SystemLogger.getInstance().setDebug(true);
//			SystemLogger.getInstance().debug("Lines");
//			for(Line l: book.get(pageIndex).getLines()) {
//				SystemLogger.getInstance().debug("L: " + l.getText());
//			}
//			System.exit(0);
//			/*TESTING*/
			
			float maxLinePosY = 0;
			int numOfIndexPages = 0;
			int indexFontSize = findIndexFontSize(book.get(pageIndex).getLines());

			//1 phase: check index pages and get them as ColumnedPage
			for(int i = pageIndex; i < book.size(); i++ ){
				SystemLogger.getInstance().debug("CUrrent index page: " + i);
				//first page
				if(i==pageIndex) {
					titlePresent = true;
					//remove non-index lines
					ContentExtractor.removeCopyRightLines(book.get(i).getLines(), book.get(i).getPageNumber(), metadata);
					ContentExtractor.removeSideLines(book.get(i).getLines());
					
//					/*TESTING*/
//					System.out.println("LINES FINAL*********");
//					SystemLogger.getInstance().setDebug(true);
//					for(Line l: book.get(i).getLines()) {
//						SystemLogger.getInstance().debug("L: " + l.getText());
//					}
//					System.exit(0);
//					/*TESTING*/
				}
				else {
					//check for null page
					if(book.get(i) == null || book.get(i).getLines().size() == 0) {
						break;
					}
						
					//other pages
					titlePresent = false;
					
					//MOST LEFT X NORMALIZATION
					float currentPageMostLeft = calculateMostLeftRightForIndex( book.get(i).getLines());
					if(needForXNormalization && (Math.round(currentPageMostLeft) != Math.round(pageIndexMostLeft)) && (Math.abs(Math.round(currentPageMostLeft) - Math.round(pageIndexMostLeft)) >= 1)) {
						float mostLeftDiff = pageIndexMostLeft - currentPageMostLeft;
						
						
						if(mostLeftDiff != 0){
							SystemLogger.getInstance().debug("pageIndexMostLeft: " + pageIndexMostLeft);
							SystemLogger.getInstance().debug("currentPageMostLeft: " + currentPageMostLeft);
							SystemLogger.getInstance().debug("mostLeftDiff: " + mostLeftDiff);
							
							for(int j = 0 ; j<book.get(i).getLines().size();j++){						
								
								Line temp= 	book.get(i).getLineAt(j);
								temp.setStartPositionX(temp.getStartPositionX() + (mostLeftDiff));
								
								//each word of the line
								for(int k = 0 ; k < temp.size(); k++){
									
									if(temp.getWordAt(k)!= null) {
										temp.getWordAt(k).setStartPositionX(temp.getWordAt(k).getStartPositionX() + mostLeftDiff );	
										temp.getWordAt(k).setEndPositionX(temp.getWordAt(k).getEndPositionX() + mostLeftDiff );	
									}
										
								}
								book.get(i).replaceLineAt(j, temp);
							}	
							
						}
					} else{
						if(i == (pageIndex + 1)) {
							needForXNormalization = false;
						}
						SystemLogger.getInstance().debug("currentPageMostLeft " + currentPageMostLeft);
						SystemLogger.getInstance().debug("pageIndexMostLeft " + pageIndexMostLeft);
						SystemLogger.getInstance().debug("SAME MOSTLEF! no MOST LEFT X NORMALIZATION");
						//System.exit(0);
					}
				}	
				
//				/*TESTING*/
//				SystemLogger.getInstance().debug("AFTER: " + i);
//				for(Line l: book.get(i).getLines()) {
//					System.out.println("L: " + l.getText());
//				}
//				System.exit(0);
//				//SystemLogger.getInstance().setDebug(true);
//				/*TESTING*/
				
//				/*TESTING*/
//				if(i==774) {
//					for(Line l: book.get(i).getLines()) {
//						System.out.println("# " + l.getText());
//					}
//					SystemLogger.getInstance().setDebug(true);
//				//System.exit(0);
//				}
//				/*TESTING*/
				
				Line originalFirstLine = book.get(i).getLineAt(0);
			
				cF = discoverColumnFormat(book.get(i).getLines());
				//check if there are "note" lines at the beginning of the index that prevents the columns to be recognized
				if(cF.numberOfColumns == 1 && i == pageIndex) {
					for(int l = 1; l < 5; l++) {
						List<Line> view  = book.get(i).getLinesView(l, book.get(i).getLines().size()-1);
						ColumnsFormat tmpCF = discoverColumnFormat(view);
						if(tmpCF.numberOfColumns > 1) {
							book.get(i).removeFirstLines(l);
							cF = tmpCF;
							break;
						}
					}
				}
				ColumnedPage columnedPage = asColumnedPage(book.get(i).getLines(), cF, titlePresent);
			
////				/*TESTING*/
//				columnedPage.print();
////				System.exit(0);
////				/*TESTING*/
				
				if(i==pageIndex) {
					pageIndexMostLeft = calculateMostLeftRightForIndex(columnedPage.getAllLines()); 
					SystemLogger.getInstance().debug("First page most left: " + pageIndexMostLeft);
					SystemLogger.getInstance().debug("line 1 most left: " + columnedPage.getAllLines().get(0).getStartPositionX());
					SystemLogger.getInstance().debug("text: " + columnedPage.getAllLines().get(0).getText());
				}
				//SystemLogger.getInstance().setDebug(true);
				SystemLogger.getInstance().debug("checking: " + i );
				SystemLogger.getInstance().debug("@@ checking index: " + i);
				boolean isIndexPage = i == pageIndex ? isIndexPage(columnedPage.getAllLines(), null, originalFirstLine) : isIndexPage(columnedPage.getAllLines(),indexTitleLine, originalFirstLine);
				
				if(isIndexPage) {
					//removeNoteLines(book.get(i).getLines(), indexFontSize);
					//ColumnedPage columnedPage = extractColumnsofIndexPage(book.get(i), titlePresent);
					columnedPage.processColumns();
					columnedPages.add(columnedPage);
					
					if(!titlePresent) {
						SystemLogger.getInstance().debug(">>>>>>>>>>>>>>>> page: " + i);
						Vector<Line> allLines = columnedPage.getAllLines();
						if(allLines.get(allLines.size() -1).getPositionY() > maxLinePosY) {
							maxLinePosY = allLines.get(allLines.size() -1).getPositionY();
						}
					}
				} else {
					break;
				}
				numOfIndexPages++;
			}
			
//			/*TESTING*/
//			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//			for(ColumnedPage columnedPage : columnedPages) {
//				for(Line l: columnedPage.getAllLines()) {
//					System.out.println("- " + l.getText() + " X: " + l.getStartPositionX());
//				}
//			}
//			System.exit(0);
//			/*TESTING*/
			
			
//			/*TESTING*/
//			SystemLogger.getInstance().log("--------- MAX: " + maxLinePosY);
//			SystemLogger.getInstance().log("AFTER");
//			for(Line l: columnedPages.get(0).getAllLines()) {
//				System.out.println("L: " + l.getText() + " -- Y: " + l.getPositionY());
//			}
//			System.exit(0);
//			/*TESTING*/			

			//2 phase: consensus
			//2.1 get max number of columns & max number of level
			int numOfColumns =0;
			int numOfLevels =0;
			for(int i = 0; i < columnedPages.size(); i++) {
				if(columnedPages.get(i).getNumberofColumns() > numOfColumns) {
					numOfColumns = columnedPages.get(i).getNumberofColumns();
				}
				int nLevels = columnedPages.get(i).getMaxCantLevel();
				if( nLevels> numOfLevels) {
					numOfLevels = nLevels;
				}
			}

			//2.2 Getting frequency of position for each level
			HashMap<Integer,HashMap<Integer,HashMap<Float, Integer >>> evenMap = new HashMap<Integer,HashMap<Integer,HashMap<Float, Integer >>>();
			HashMap<Integer,HashMap<Integer,HashMap<Float, Integer >>> oddMap = new HashMap<Integer,HashMap<Integer,HashMap<Float, Integer >>>();
			//for each page
			for(int i = 0; i < columnedPages.size(); i++) {
				SystemLogger.getInstance().debug("Procesing page #: " + i);
				ColumnedPage columnedPage = columnedPages.get(i);
				SystemLogger.getInstance().debug("numOfColumns  size #: " + numOfColumns);
				//for each column
				for(int j=0; j < numOfColumns; j++) {
					//map to use
					HashMap<Integer,HashMap<Integer,HashMap<Float, Integer >>> currentMap = null;
					//get map for even or odd page
					if(i % 2 == 0) {
						currentMap = evenMap;
					} else {
						currentMap = oddMap;
					}
					HashMap<Integer,HashMap<Float, Integer >> mapOfColumn = currentMap.get(j);
					if(mapOfColumn ==  null) {
						mapOfColumn = new HashMap<Integer,HashMap<Float, Integer >>();
						currentMap.put(j, mapOfColumn);
					}
					//for each level
					for(int l = 0; l < numOfLevels; l++) {
						Float pos = columnedPage.getPositionByColumnLevel(j, l);
						if (pos == null) {
							break;
						}
						HashMap<Float, Integer> mapOfLevel = mapOfColumn.get(l);
						if(mapOfLevel == null) {
							mapOfLevel = new HashMap<Float, Integer>();
							mapOfColumn.put(l, mapOfLevel);
						}
						Integer currentFrecuencyForPostion = mapOfLevel.get(pos);
						if (currentFrecuencyForPostion == null) {
							mapOfLevel.put(pos, 1);
						} else {
							mapOfLevel.put(pos, currentFrecuencyForPostion+1);
						}
					}
				}
			}

//			/*TESTING*/
//			SystemLogger.getInstance().log("Testing EVEN MAP");
//			//for(int c = 0; c ==0; c++) {
//			for(int c = 0; c < numOfColumns; c++) {
//				SystemLogger.getInstance().log(">Column #: " + c);
//				HashMap<Integer, HashMap<Float, Integer>> columnMap = evenMap.get(c);
//				for(int l = 0; l < numOfLevels; l++) {
//					SystemLogger.getInstance().log(">>Level #: " + l);
//					HashMap<Float, Integer> levelMap = columnMap.get(l);
//					if(levelMap == null || levelMap.size() == 0) {
//						SystemLogger.getInstance().log(">>skypped");
//						continue;
//					}
//					Iterator<Float> it = levelMap.keySet().iterator();
//					while(it.hasNext()) {
//						Float keyValue = it.next();
//						SystemLogger.getInstance().log(">>Pos: " + keyValue + " cant: " + levelMap.get(keyValue));
//					}
//				}
//			}
//			
//			
//			SystemLogger.getInstance().log("Testing ODD MAP");
//			//for(int c = 0; c ==0; c++) {
//			for(int c = 0; c < numOfColumns; c++) {
//				SystemLogger.getInstance().log(">Column #: " + c);
//				HashMap<Integer, HashMap<Float, Integer>> columnMap = oddMap.get(c);
//				for(int l = 0; l < numOfLevels; l++) {
//					SystemLogger.getInstance().log(">>Level #: " + l);
//					HashMap<Float, Integer> levelMap = columnMap.get(l);
//					if(levelMap == null) {
//						SystemLogger.getInstance().log(">>skypped");
//						continue;
//					}
//					Iterator<Float> it = levelMap.keySet().iterator();
//					while(it.hasNext()) {
//						Float keyValue = it.next();
//						SystemLogger.getInstance().log(">>Pos: " + keyValue + " cant: " + levelMap.get(keyValue));
//					}
//				}
//			}
//			System.exit(0);
//			/*TESTING*/
			
			//2.3 getting consensual map for level 1
			//Even pages
			HashMap<Integer,Float> finalEvenMap = new HashMap<Integer,Float>();
			for(int c = 0; c < numOfColumns; c++) {
				//read
				HashMap<Integer,HashMap<Float, Integer >> columnMap = evenMap.get(c);
				//write
				//check level 0
				int l = 0;
				//read
				HashMap<Float, Integer > levelMap = columnMap.get(l);
				if (levelMap == null) {
					continue;
				}
				//write
				Set<Float> values = levelMap.keySet();
				Iterator<Float> iterator = values.iterator();
				if (values.size() == 1) {
					Float val = iterator.next();
					finalEvenMap.put(c, val);
				} else {
					int max_cant = 0;
					Float max_pos = 0.0f;
					//get pos that has more occurrences
					while(iterator.hasNext()) {
						Float pos = iterator.next();
						Integer cant = levelMap.get(pos);
						if(cant >= max_cant) {
							max_pos = pos;
							max_cant = cant;
						}
					}
					//check if the max occurrence is more than once
					int catOccurrences = 0;
					Float min_pos = max_pos;
					iterator = values.iterator();
					while(iterator.hasNext()) {
						Float pos = iterator.next();
						Integer cant = levelMap.get(pos);
						if(cant == max_cant) {
							catOccurrences++;
							if (pos < min_pos) {
								min_pos = pos;
							}
						}
					}
					if (catOccurrences == 1) {
						finalEvenMap.put(c, max_pos);
					} else {
						finalEvenMap.put(c, min_pos);
					}
				}	
			}
			
			//Odd pages
			HashMap<Integer,Float> finalOddMap = new HashMap<Integer,Float>();
			for(int c = 0; c < numOfColumns; c++) {
				//read
				HashMap<Integer,HashMap<Float, Integer >> columnMap = oddMap.get(c);
				if(columnMap == null) {
					break;
				}
				//write
				//check level 0
				int l = 0;
				//read
				HashMap<Float, Integer > levelMap = columnMap.get(l);
				if (levelMap == null) {
					continue;
				}
				//write
				Set<Float> values = levelMap.keySet();
				Iterator<Float> iterator = values.iterator();
				if (values.size() == 1) {
					Float val = iterator.next();
					finalOddMap.put(c, val);
				} else {
					int max_cant = 0;
					Float max_pos = 0.0f;
					//get pos that has more occurrences
					while(iterator.hasNext()) {
						Float pos = iterator.next();
						Integer cant = levelMap.get(pos);
						if(cant >= max_cant) {
							max_pos = pos;
							max_cant = cant;
						}
					}
					//check if the max occurrence is more than once
					int catOccurrences = 0;
					Float min_pos = max_pos;
					iterator = values.iterator();
					while(iterator.hasNext()) {
						Float pos = iterator.next();
						Integer cant = levelMap.get(pos);
						if(cant == max_cant) {
							catOccurrences++;
							if (pos < min_pos) {
								min_pos = pos;
							}
						}
					}
					if (catOccurrences == 1) {
						finalOddMap.put(c, max_pos);
					} else {
						finalOddMap.put(c, min_pos);
					}
				}	
			}
			
//			/*TESTING*/
//			SystemLogger.getInstance().log("----FINAL EVEN MAP-----");
//			for (int i = 0; i < finalEvenMap.size(); i++) {
//				SystemLogger.getInstance().log(">COLUMN: " + i);
//				Float val = finalEvenMap.get(i);
//				SystemLogger.getInstance().log(">0 LEVEL: " + val);
//			}
//			
//			SystemLogger.getInstance().log("----FINAL ODD MAP-----");
//			for (int i = 0; i < finalOddMap.size(); i++) {
//				SystemLogger.getInstance().log(">COLUMN: " + i);
//				Float val = finalOddMap.get(i);
//				SystemLogger.getInstance().log(">0 LEVEL: " + val);
//			}
//			/*TESTING*/
//			System.exit(0);
			
			//3 phase: change startPositionX of lines and add them to final vector
			Float reference = finalEvenMap.get(0);
			//System.out.println("reference: " + reference);

			//for each page
			for(int i = 0; i < columnedPages.size(); i++) {
				if(i % 2 == 0 && finalEvenMap.size() > 0 && reference != null) {
					columnedPages.get(i).ajustPositionsX(reference, finalEvenMap);
				} else if (i % 2 == 1 && finalOddMap.size() > 0 &&  reference != null) {
					columnedPages.get(i).ajustPositionsX(reference, finalOddMap);
				}
				indexLines.addAll(columnedPages.get(i).getAllLines());
			}
		}	
			
//			/*TESTING*/
//			SystemLogger.getInstance().log("----------------------------------------");
//			for(Line line: indexLines) {
//				SystemLogger.getInstance().log("P: " + line.getStartPositionX() + " L: " + line.getText());
//			}
//			System.exit(0);
//			/*TESTING*/
			
		SystemLogger.getInstance().log("Identifying Index Pages..... Done");

		return indexLines;
	}

	/**
	 *
	 * @param book
	 * @param pseudoIndex
	 * @param index
	 */

	//@REVIEW
	private void convertToIndexTerm(List<Page> book, Vector <Line> pseudoIndex, List<IndexTerm> index ){

		for(int k=0; k<pseudoIndex.size();k ++){

			if(pseudoIndex != null && !pseudoIndex.get(k).getText().isEmpty() ){

				if(pseudoIndex.get(k).size() > 1){

					short counter =0 ;
					IndexTerm temp  = new IndexTerm();

					for(short i=0 ;i<pseudoIndex.get(k).size()-1 ; i++){

						if(pseudoIndex.get(k).getWordAt(counter).getText().matches("[,]*[0-9]*[,][,]*")
								|| StringUtils.isNumeric(pseudoIndex.get(k).getWordAt(counter).getText())){

							if(StringUtils.isNumeric(pseudoIndex.get(k).getWordAt(counter+1).getText())
									|| pseudoIndex.get(k).getWordAt(counter+1).getText().matches("[,]*[0-9]*[,][,]*"))
								break;
						}

						counter++;
					}

					String buf = "";

					for(short i = 0 ; i <counter ; i++){
						buf += " " + pseudoIndex.get(k).getWordAt(i).getText().trim();
					}

					if(buf.contains("<>")){

						String junk = buf.substring(0, buf.indexOf("<>"));
						junk = junk.replaceAll(",", "").trim();

						temp.setParent(junk);

					}


					String [] split = 	buf.split(" ");
					buf = "";

					for(String spill : split ){
						if(!spill.equals("<>"))
							buf+=" "+spill;
					}

					buf = buf.trim();

					temp.setID(buf.replaceAll("[,]", ""));

					for(short i = counter; i < pseudoIndex.get(k).size(); i++){
						buf = pseudoIndex.get(k).getWordAt(i).getText().replaceAll("[^0-9]", "");

						if(!buf.equals("")){
							int pageIndex = findPageIndexWithPageNumber(book, Integer.parseInt(buf));
							if (pageIndex != -1) {
								temp.addAPageNumber(Integer.parseInt(buf));
								temp.addAPageIndex(pageIndex);
								
							} else {
								SystemLogger.getInstance().debug("@@ ommitted: " + temp.getID() + " ,pageNumber: " + Integer.parseInt(buf));
							}
							
						}
					}
					
					//if there are no indexes for the term
					if (temp.getPageIndicies().size() == 0) {
						temp.setID(pseudoIndex.get(k).getText().replaceAll("[,]", ""));
						temp.addAPageNumber(-1);
						temp.addAPageIndex(-1);
					}

					index.add(temp);
				}
				else{
					IndexTerm temp  = new IndexTerm();
					temp.setID(pseudoIndex.get(k).getWordAt(0).getText());
					temp.addAPageNumber(-1);
					temp.addAPageIndex(-1);
					index.add(temp);
				}
			}
		}
	}
	
	private Vector<IndexElement> convertToIndexElements(List<Page> book, Vector <Line> pseudoIndex ){

		Vector<IndexElement> indexElements = new Vector<IndexElement>();
		Map<String, IndexElement> indexElementsMap = new HashMap<String, IndexElement>();
		
		for(int k=0; k<pseudoIndex.size();k ++){

			if(pseudoIndex != null && !pseudoIndex.get(k).getText().isEmpty() ){
				String tokens[] = pseudoIndex.get(k).getText().split(" <> ");
				IndexElement temp  = IndexElement.createIndexElement(pseudoIndex.get(k),tokens, book);
				indexElements.add(temp);
				IndexElement parent = indexElementsMap.get(temp.getParentId());
				temp.setParent(parent);
				if(temp.getPageNumbers().size() > 0 && temp.getPageNumbers().get(0) != -1)
					indexElementsMap.put(temp.getKey(), temp);
				if(pseudoIndex.get(k).getPropertyAsBoolean("crossreference")) {
					temp.addCrossreferenceInformation(pseudoIndex.get(k).getProperty("type-crossreference"), pseudoIndex.get(k).getProperty("text-crossreference"));
				}
			}
		}
		
		for(String key: indexElementsMap.keySet()) {
			System.out.println("#" + key + "# " + key.length());
		}
		
		//second pass for crossreferences
		NounExtractor extractor = NounExtractor.getInstance();
		for(IndexElement iElement : indexElements){
			if(iElement.hasCrossreference()) {
				System.out.println("CRT: #" + iElement.getCrossreferenceText() + "# " + iElement.getCrossreferenceText().length());
				//System.out.println(indexElementsMap.containsKey(iElement.getCrossreferenceText()));
				//System.out.println(indexElementsMap.containsKey(iElement.getCrossreferenceText().trim()));
				IndexElement crossreference = indexElementsMap.get(iElement.getCrossreferenceText());
				Long best = 0L;
				if(crossreference == null)
					crossreference = indexElementsMap.get(iElement.getCrossreferenceText().toLowerCase());
				if(crossreference == null) {
					//do exhaustive approach	
					for(IndexElement indexE: indexElements) {
						if(indexE != iElement && indexE.getPageNumbers().size() > 0 && indexE.getPageNumbers().get(0) != -1) {
							ArrayList<String> permutations = indexE.getLabelPermutations();
							ArrayList<String> permutationsFixed = new ArrayList<String>();
							for(String permutation: permutations) {
								permutation = intextbooks.tools.utility.StringUtils.preProcessParentesis(permutation);
								permutationsFixed.add(permutation);
							}
							Long result = extractor.getTextMatch(iElement.getCrossreferenceText().trim(), permutationsFixed, "en");
							if(result > 0 && result > best) {
								crossreference = indexE;
								best = result;
								if(result >= 1)
									break;
							}
						}
					}
				}
				iElement.setCrossreference(crossreference);
				/*TESTING*/
				if(crossreference != null) {
					System.out.println("YES !!!!!!!!!!!!!!!!!!!!!!!!!: " + best);
					System.out.println(iElement);
					System.out.println("\t" + crossreference);
				} else {
					System.out.println("NO !!!!!!!!!!!!!!!!!!!!!!!!!");
					System.out.println(iElement);
				}
				/*TESTING*/
			}
			
		}
		return indexElements;
	}
	
	/*
	 * TESTING: find IndexElements using only permutations
	 */
	/*private void findIndexElementsInBook(List<Page> book, Vector<IndexElement> indexElements ){
		String allText;
		int i = 58;
		for(; i < indexElements.size(); i++) {
			Scanner s= new Scanner(System.in);
				IndexElement indexElement = indexElements.get(i);
				SystemLogger.getInstance().debug(indexElement.getKey());
				
				for(Integer page: indexElement.getPageIndexes()) {
					if(page == -1)
						continue;
					SystemLogger.getInstance().debug("# page: "+ page);
					allText = book.get(page).getText().toLowerCase().replaceAll("\n", " ");
					
					ArrayList<String> permutations = indexElement.getLabelPermutations();
					ArrayList<String> sentences = Extractor.extractSentencesFromText(allText);
					Iterator<String> it =  sentences.iterator();
					while(it.hasNext()) {
						SystemLogger.getInstance().debug("\t "+ it.next());
					}
					JSONArray array = NounExtractor.getInstance().getJSON(permutations, sentences, "en");
					SystemLogger.getInstance().debug("permutations--> " + permutations);
					SystemLogger.getInstance().debug("JSON ALL--> " + array.toJSONString());
					
					if(array.size() == 0 && permutations.size() > 1) {
						SystemLogger.getInstance().debug("LAST:  " + indexElement.getLastPartAsArrayList().get(0));
						array = NounExtractor.getInstance().getJSON(indexElement.getLastPartAsArrayList(), sentences, "en");
						SystemLogger.getInstance().debug("JSON LAST--> " + array.toJSONString());
					}
				}
				
				s.nextLine();
		}
		
		System.exit(0);
	}*/
	
	synchronized public static void updateIndexElementsWithNounInformation(BookContent bookContent, List<IndexElement> indexElements ){
		//SystemLogger.getInstance().setDebug(true);
		SystemLogger.getInstance().debug("########################## updateIndexElementsWithNounInformation ##########################");
		int elementsWithLabels = 0;
		int numberOfPages= 0;
		int indexElementsFull = 0;
		int indexElementsPartial = 0;
		String allText;
		int errors = 0;
		int noPages = 0;
		int indexElementsP = 0;
		NounExtractor extractor = NounExtractor.getInstance();
		int limit = indexElements.size();
		//int limit = 10;
		int noCount = 0;
		for(int i = 0; i < limit; i++) {
			IndexElement indexElement = indexElements.get(i);
			if(indexElement.isArtificial())
				continue;
			indexElementsP++;
			SystemLogger.getInstance().log("Getting reading label for: " + indexElement.getKey());
			ArrayList<String> permutations = indexElements.get(i).getLabelPermutations();
			List<Integer> pages = indexElements.get(i).getPageIndexes();
			boolean flag = false;
			SystemLogger.getInstance().debug("********************************************");
			SystemLogger.getInstance().debug("Index Element: "+ indexElements.get(i).getKey());
			SystemLogger.getInstance().debug("Permutations: " + permutations);
			SystemLogger.getInstance().debug("Pages: " + pages.size());
			Set<Integer> hitPages = new HashSet<Integer>();
			//numberOfPages += pages.size();
			//if just one element, don't run the algorithm
//			if(permutations.size() == 1) {
//				indexElement.setFullLabel(true);
//				indexElement.setLabel(permutations.get(0));
//				indexElement.addNounPhrase(permutations.get(0));
//				continue;
//			}
					
			Map<String, Integer>  fullLabelMap = new HashMap<String,Integer>();
			String partialLabel = null;
			boolean oneFullLabel = false;
			boolean normalSearch = false;
			for(Integer page: pages) {
				if(page != -1) {
					numberOfPages++;
					boolean fullLabel = false;
					//allText = book.get(page).getText().toLowerCase().replaceAll("\n", " ");
					allText = bookContent.getContentOfPage(page).toLowerCase().replaceAll("\n", " ");
					ArrayList<String> sentences = Extractor.extractSentencesFromText(allText);
					try {
						JSONArray array = extractor.getJSON(permutations, sentences, "en");
						if(array.size() > 0 ) {
							fullLabel = true;
							//oneFullLabel = true;
						}
						
						if(array.size() == 0 && permutations.size() > 1) {
							array = extractor.getJSON(indexElement.getLastPartAsArrayList(), sentences, "en");
						}
						
						//there is a result
						if(array.size() != 0) {
							hitPages.add(page);
							//process results
							for(Object sol: array) {
								JSONObject obj = (JSONObject) sol;
								String correctKey = (String) obj.get("correct");
								JSONArray noun_strings = (JSONArray) obj.get("noun_string");
								JSONArray book_strings = (JSONArray) obj.get("book_string");
								//process noun_string
								for(Object str: noun_strings) {
									indexElement.addNounPhrase((String)str);
								}
								//process book_string
								for(Object str: book_strings) {
									indexElement.addPageToSentence(page, -1, (String)str);
								}
								if(fullLabel) {
									Integer count = fullLabelMap.get(correctKey);
									if(count == null) {
										count = 0;
									}
									count++;
									fullLabelMap.put(correctKey, count);
								} else {
									partialLabel = correctKey;
								}
							}
						} else {
							boolean found = false;
							for(String permutation: permutations) {		
								int index = allText.indexOf(permutation.toLowerCase());
								if(index != -1) {
									hitPages.add(page);
									indexElement.addPageToSentence(page, -1, permutation);
									Integer count = fullLabelMap.get(permutation);
									if(count == null) {
										count = 0;
									}
									count++;
									fullLabelMap.put(permutation, count);
									found = true;
									
									break;
								} 
							}
							if(!found) {
								String searchString = indexElements.get(i).getLastPartAsArrayList().get(0);
								int index = allText.indexOf(searchString.toLowerCase());
								if(index != -1) {
									hitPages.add(page);
									partialLabel = searchString;
									found = true;
									normalSearch = true;
								}
							}
								
							if(!found) {
								System.out.println("NOOOO");
								System.out.println("indexElement: " + indexElement.getKey());
								System.out.println("page: " + page);
								noCount++;
//								if(noCount == 2) {
//									System.out.println(bookContent.getContentOfPage(page).toLowerCase());
//									System.exit(0);
//								}
								
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						errors++;
					}
				} else {
					noPages++;
					break;
				}
			}
			
			//process key label
			if(fullLabelMap.size() != 0) {
				Integer maxVal = -1;
				for(Integer val : fullLabelMap.values()) {
					if(val > maxVal) {
						maxVal = val;
					}
				}
				for(Entry<String, Integer> entry : fullLabelMap.entrySet()) {
					if(entry.getValue() == maxVal) {
						indexElement.setLabel(entry.getKey());
						indexElement.setFullLabel(true);
						SystemLogger.getInstance().log("\tfull label: " + entry.getKey());
						break;
					}
				}
				indexElementsFull++;
			} else if (partialLabel != null) {
				indexElement.setLabel(partialLabel);
				indexElement.setFullLabel(false);
				indexElementsPartial++;
			}
			
			
			elementsWithLabels += hitPages.size();
			
			System.out.println(indexElement);
			
//			if(normalSearch) {
//				System.exit(0);
//			}
			
		}
		
		SystemLogger.getInstance().debug("TERM Detection approach ********************************************");
		SystemLogger.getInstance().debug("Number of IndexElements: " + indexElementsP);
		SystemLogger.getInstance().debug("indexElementsFull: " + indexElementsFull);
		SystemLogger.getInstance().debug("indexElementsPartial: " + indexElementsPartial);
		SystemLogger.getInstance().debug("indexElements no pages: " + noPages);
		SystemLogger.getInstance().debug("Number of index pages: " + numberOfPages);
		SystemLogger.getInstance().debug("Number of index pages with at least one label match: " + elementsWithLabels);
		SystemLogger.getInstance().debug("errors: " + errors);
		//CountDirectLabelsApproachForIndexElements(bookContent,indexElements);
	}
	
	
private static void CountDirectLabelsApproachForIndexElements(BookContent bookContent, List<IndexElement> indexElements ){
		
		int elementsWithLabels = 0;
		int numberOfPages= 0;
		int indexElementsFull = 0;
		int indexElementsPartial = 0;
		int noPages = 0;
		int indexElementsP = 0;
		for(int i = 0; i < indexElements.size(); i++) {
			if(indexElements.get(i).isArtificial())
				continue;
			indexElementsP++;
			List<String> permutations = indexElements.get(i).getLabelPermutations();
			List<Integer> pages = indexElements.get(i).getPageIndexes();
			boolean flag = false;
			SystemLogger.getInstance().debug("****************");
			SystemLogger.getInstance().debug("" + indexElements.get(i));
			SystemLogger.getInstance().debug("Permutations: " + permutations);
			SystemLogger.getInstance().debug("Pages: " + pages.size());
			Set<Integer> hitPages = new HashSet<Integer>();
			//numberOfPages += pages.size();
//			for(String permutation: permutations) {
//				for(Integer page: pages) {
//					if(page != -1) {
//						
//						String pageText = book.get(page).getText().toLowerCase();
//						int index = pageText.indexOf(permutation.toLowerCase());
//						if(index != -1) {
//							hitPages.add(page);
//							continue;
//							//System.out.println("\t"+permutation);
//						} 
//					} 
//				}
//			}
			
			boolean conceptFoundFull = false;
			boolean conceptFound = false;
			for(Integer page: pages) {
				if(page != -1) {
					numberOfPages++;
					boolean found = false;
					//String pageText = book.get(page).getText().toLowerCase();
					String pageText = bookContent.getContentOfPage(page).toLowerCase();
					for(String permutation: permutations) {		
						int index = pageText.indexOf(permutation.toLowerCase());
						if(index != -1) {
							hitPages.add(page);
							found = true;
							conceptFound = true;
							conceptFoundFull = true;
							break;
						} 
					}
					if(!found) {
						String searchString = indexElements.get(i).getLastPartAsArrayList().get(0);
						int index = pageText.indexOf(searchString.toLowerCase());
						if(index != -1) {
							hitPages.add(page);
							conceptFound = true;
						}
					}
				}
				else {
					noPages++;
				}
			}
			
			if(conceptFound) {
				if(conceptFoundFull) {
					indexElementsFull++;
				} else {
					indexElementsPartial++;
				}
			}
			
			elementsWithLabels += hitPages.size();
			
		}
		SystemLogger.getInstance().debug("DirectLabelsApproachForIndexElements -----------------------");
		SystemLogger.getInstance().debug("Number of IndexElements: " + indexElementsP);
		SystemLogger.getInstance().debug("indexElementsFull: " + indexElementsFull);
		SystemLogger.getInstance().debug("indexElementsPartial: " + indexElementsPartial);
		SystemLogger.getInstance().debug("indexElements noPages: " + noPages);
		SystemLogger.getInstance().debug("Number of index pages: " + numberOfPages);
		SystemLogger.getInstance().debug("Number of index pages with at least one label match: " + elementsWithLabels);
	}
private void CountNounPhrasesApproachLabelsForIndexElements(List<Page> book, Vector<IndexElement> indexElements ){
	
	int elementsWithLabels = 0;
	int numberOfPages= 0;
	String allText;
	int errors = 0;
	for(int i = 0; i < indexElements.size(); i++) {
		IndexElement indexElement = indexElements.get(i);
		ArrayList<String> permutations = indexElements.get(i).getLabelPermutations();
		List<Integer> pages = indexElements.get(i).getPageIndexes();
		boolean flag = false;
		SystemLogger.getInstance().debug("****************");
		SystemLogger.getInstance().debug("" + indexElements.get(i));
		SystemLogger.getInstance().debug("Permutations: " + permutations);
		SystemLogger.getInstance().debug("Pages: " + pages.size());
		Set<Integer> hitPages = new HashSet<Integer>();
		numberOfPages += pages.size();
		for(String permutation: permutations) {
			for(Integer page: pages) {
				if(page != -1) {
					//System.out.println("# page: "+ page);
					allText = book.get(page).getText().toLowerCase().replaceAll("\n", " ");
					ArrayList<String> sentences = Extractor.extractSentencesFromText(allText);
					Iterator<String> it =  sentences.iterator();
					try {
						JSONArray array = NounExtractor.getInstance().getJSON(permutations, sentences, "en");
						//System.out.println("permutations--> " + permutations);
						//System.out.println("JSON ALL--> " + array.toJSONString());
						
						if(array.size() == 0 && permutations.size() > 1) {
							//System.out.println("LAST:  " + indexElement.getLastPartAsArrayList().get(0));
							array = NounExtractor.getInstance().getJSON(indexElement.getLastPartAsArrayList(), sentences, "en");
							//System.out.println("JSON LAST--> " + array.toJSONString());
						}
						
						if(array.size() != 0) {
							hitPages.add(page);
							//System.out.println("\t"+permutation);
						}
					} catch (Exception e) {
						e.printStackTrace();
						errors++;
					}
				}
				
			}
		}
		
		elementsWithLabels += hitPages.size();
		
	}
	SystemLogger.getInstance().debug("Number of IndexElements: " + indexElements.size());
	SystemLogger.getInstance().debug("Number of index pages: " + numberOfPages);
	SystemLogger.getInstance().debug("Number of index pages with at least one label match: " + elementsWithLabels);
	SystemLogger.getInstance().debug("errors: " + errors);
	
}
private void printPageText(List<Page> book, Vector<IndexElement> indexElements ){
		
		int elementsWithLabels = 0;
		for(int i = 0; i < indexElements.size(); i++) {
			String id = indexElements.get(i).getKey();
			if(id.equals("empirical bootstrap <> simulation <> for centered sample mean")) {
				for(int page: indexElements.get(i).getPageIndexes()) {
					String pageText = book.get(page).getText().toLowerCase().replaceAll("\n", " ");
					SystemLogger.getInstance().debug("****************************");
					SystemLogger.getInstance().debug("Page: " + page);
					SystemLogger.getInstance().debug(pageText);
				}
				
			}		

		}

		System.exit(0);
	}

	/**
	 *
	 * @param arrayList
	 * @param bookID
	 * @param index
	 */
	public void storeIndexToDatabase(ArrayList<List<Line>> arrayList, String bookID, List<IndexElement> index){
		
//		/*TESTING*/
//		System.out.println("######################## index");
//		for(IndexTerm it : index) {
//			System.out.println("> " + it.getID());
//		}
//		/*TESTING*/

		SystemLogger.getInstance().log("Storing Index to Database");
		for(int i = 0 ; i<index.size(); i++){
				index.get(i).storeInDB(bookID);
		}
		SystemLogger.getInstance().log("Updating crossrefrences in database");
		for(int i = 0 ; i<index.size(); i++){
				if(index.get(i).hasCrossreference())
					index.get(i).updateCrossreference();
		}

		SystemLogger.getInstance().log("Index Storage End");
	}
	
	public void matchIndexTermsToSegments(List<IndexElement> index, BookContent bookContent){
		NounExtractor extractor = NounExtractor.getInstance();
		for(int i = 0 ; i<index.size(); i++){
			SystemLogger.getInstance().log("matching " + index.get(i).getKey() + "(" + i + "/" + index.size() + ")");
			Map<Integer,List<Integer>>  segments = null;

			if(index.get(i).getPageNumbers() != null && !index.get(i).isArtificial()){
				segments = segmentOfIndex(bookContent, index.get(i), extractor);
			}
			else{
				segments = new HashMap<Integer,List<Integer>>();
				ArrayList<Integer> list = new ArrayList<Integer>();
				list.add(-1);
				segments.put(-1, list);
			}
			index.get(i).setPageSegments(segments);
		}
		
		//for artificial terms: second time
		for(int i = 0 ; i<index.size(); i++){
			if(index.get(i).isArtificial()) {
				Map<Integer,List<Integer>> segments = new HashMap<Integer,List<Integer>>();
				
				for(int j = 0 ; j<index.size(); j++){
					if(i != j) {
						if(index.get(j).getParent() != null && index.get(j).getParent() == index.get(i)) {
							segments.putAll(index.get(j).getPageSegments());
						}
					}
				}
				
				index.get(i).setPageSegments(segments);
			}
		}
	}
	
	public static Map<Integer,List<Integer>> segmentOfIndex(BookContent bookContent, IndexElement indexElement, NounExtractor extractor){
		Map<Integer,List<Integer>> correspondingSegments = new HashMap<Integer,List<Integer>> ();
		String indexID = indexElement.getNormalizedKey();
		List<Pair<Integer,Integer>> pages = indexElement.getPages();
		
//		List<String> list1 = new ArrayList(Arrays.asList("ball bearing example data" , "law of large numbers", "\"µ ± a few σ\" rule", "Billingsley, P.", "Ross, S.M.", "central limit theorem applications" ));
//		List<String> list2 = new ArrayList(Arrays.asList("Hypothesis composite", "Random variable discrete"));
//		if(list2.contains(indexID)) {
//			SystemLogger.getInstance().setDebug(true);
//		} else {
//			List<Integer> list = new ArrayList<Integer>();
//			list.add(-1);
//			SystemLogger.getInstance().setDebug(false);
//			correspondingSegments.put(-1,list);
//			return correspondingSegments;
//		}
		
		/*TESTING*/
		SystemLogger.getInstance().debug("====================segmentOfIndex=========================");
		SystemLogger.getInstance().debug(">>>indexID: " + indexElement.getKey());
		SystemLogger.getInstance().debug(">>>indexElement: " + indexElement);
		/*TESTING*/
		
		
		for(Pair<Integer, Integer> page: pages) {
			int pageNumber = page.getLeft();
			int pageIndex = page.getRight();
			if(pageNumber == -1) {
				List<Integer> list = new ArrayList<Integer>();
				list.add(-1);
				correspondingSegments.put(-1, list);
				continue;
			}
			SystemLogger.getInstance().debug("-------PAGE-------");
			SystemLogger.getInstance().debug("% page number: " + pageNumber);
			SystemLogger.getInstance().debug("% page index: " + pageIndex);
			List<Integer> segments = bookContent.findSegmentsOnPage(pageIndex);
			SystemLogger.getInstance().debug("page index: "+ (pageIndex) + " % segments on page (size: " + segments.size() + ") : " + segments);
			//do search
			IndexTermMatches queue = new IndexTermMatches();
			for(Integer segment: segments) {
				SystemLogger.getInstance().debug("testing  segment: " + segment);
				IndexTermMatch match = bookContent.segmentContainsTerm(segment, pageIndex, indexElement, extractor);
				SystemLogger.getInstance().debug("\tres: " + match);
				if(match != null) {
					queue.add(match);
				}
			}
			
			IndexTermMatch result = queue.poll();
			
			if(result == null) {
				if(segments.size() == 1) {
					SystemLogger.getInstance().debug("-> SELECTION: only one option , " + segments.get(0));
					List<Integer> list = new ArrayList<Integer>();
					list.add(segments.get(0));
					correspondingSegments.put(pageNumber, list);
				} else if (segments.size() > 1) {
					
					SystemLogger.getInstance().debug("NO SELECTION AT ALL: multiple options");
					//case1: index term includes previous and next page -> assign to all segments on page
					if(GeneralUtils.containsPageNumber(pages, pageNumber -1) && GeneralUtils.containsPageNumber(pages, pageNumber +1)) {
						correspondingSegments.put(pageNumber, segments);
						SystemLogger.getInstance().debug("CASE1");
					//case 2: index term includes the following page, but no the previous one -> select the last segment
					} else if (GeneralUtils.containsPageNumber(pages, pageNumber +1)) {
						List<Integer> list = new ArrayList<Integer>();
						list.add(segments.get(segments.size()-1));
						correspondingSegments.put(pageNumber, list);
						SystemLogger.getInstance().debug("CASE2");
					}
					else {
						//case3: next page is not part of the term -> select the segment that is already selected
						List<Integer> candidates = new ArrayList<Integer>();
						//if any of the segments has been selected before, put it as candidate
						for(Integer segment: segments) {
							if(GeneralUtils.containsSegment(correspondingSegments, segment)) {
								candidates.add(segment);
								SystemLogger.getInstance().debug("CASE3 candidate: " + segment);
							}
						}
						if(candidates.size() == 0)
							candidates = segments;
						//select the last segment
						List<Integer> list = new ArrayList<Integer>();
						list.add(candidates.get(candidates.size()-1));
						correspondingSegments.put(pageNumber, list);
						SystemLogger.getInstance().debug("CASE3");
					}	
				} else {
					List<Integer> list = new ArrayList<Integer>();
					list.add(-1);
					correspondingSegments.put(pageNumber, list);
				}
			} else {
				for(String noun: result.getNounString())
						indexElement.addNounPhrase(noun);
				for(String sentence: result.getBookString())
						indexElement.addPageToSentence(pageNumber, result.getSegmentID(), sentence);
				if(result.isFull()) {
					indexElement.setFullLabel(true);
					indexElement.setLabel(result.getReadingOrderString());
				}
				if(result.getPOS() != null && result.getPOS().contains("PROPN"))
					indexElement.setPropn(true);
				List<Integer> list = new ArrayList<Integer>();
				list.add(result.getSegmentID());
				correspondingSegments.put(pageNumber, list);
				SystemLogger.getInstance().debug("SELECTION: " + result);
			}
		}
		
		
		return correspondingSegments;
	}
		

	/**
	 *
	 * @param arrayList
	 * @param bookID
	 * @param indexID
	 * @param pageIndicies
	 * @return
	 */

	public static List<Integer> segmentOfIndex(ArrayList<List<Line>> pagesAndLines,  String bookID, String indexID, List<Integer> pageIndices){
		
		/*TESTING*/
//		if(indexID.equals("law of large numbers") || indexID.contains("µ ± a few σ")) {
//			SystemLogger.getInstance().setDebug(true);
//		} else {
//			SystemLogger.getInstance().setDebug(false);
//		}
		SystemLogger.getInstance().debug("====================segmentOfIndex=========================");
		SystemLogger.getInstance().debug(">>>indexID: " + indexID);
		SystemLogger.getInstance().debug(">>>pageIndices: " + pageIndices);
		SystemLogger.getInstance().debug(">>>pageIndicesSize: " + pageIndices.size());
		/*TESTING*/
		
		List<Integer> correspondingSegments = new ArrayList<Integer> ();
		//correspondingSegments.add(1);
		
		if(pageIndices.get(0)!=-1){
			for(int i = 0; i<pageIndices.size(); i++){
				SystemLogger.getInstance().debug("% page indice: " + i);
				int pageNumber = pageIndices.get(i);
				SystemLogger.getInstance().debug("% page number: " + pageNumber);
				//If the page number is wrong (-1), skip it
				if (pageNumber == -1) {
					continue;
				}
				List<Integer> segments =  cm.getSegmentsOnPage(bookID, pageNumber);
				SystemLogger.getInstance().debug("page: "+ (pageNumber) + " % segment of pagenumber (size: " + segments.size() + ") : " + segments);
				
				if(segments.size() == 1){

					correspondingSegments.add(segments.get(0));
				}
				else{

					//System.out.println("% result from arrayList.get(pageNumber): " + arrayList.get(pageNumber).get(0).getText());
					float pos = searchForWords(pagesAndLines.get(pageNumber), indexID.split(" "));
					SystemLogger.getInstance().debug("pos: " + pos);
			
					int segmt = cm.getSegmentOfWord(bookID, pageNumber, pos);
					SystemLogger.getInstance().debug("segmt: " + segmt);
					
					/* TESTING */
//					if(indexID.equalsIgnoreCase("sample") && pageNumber == 14) {
//						System.out.println("pos: "  + pos);
//						System.out.println("segmt: "  + segmt);
//						System.out.println("correspondingSegments: "  + correspondingSegments);
//						System.exit(0);
//					}
//					/* TESTING */
				
					try {
						if(segmt == -1) {
							correspondingSegments.add(segments.get(segments.size()-1));
							SystemLogger.getInstance().debug("1 assigning: " + segments.get(segments.size()-1));
						}
						else {
							correspondingSegments.add(segmt);
							SystemLogger.getInstance().debug("2 assigning: " + segmt);
						}
					} catch (ArrayIndexOutOfBoundsException e) {
//						/*TESTING*/
//						System.out.println("!!!!!!! ERROR !!!!!!");
//						System.out.print("pos: ");
//						System.out.println(pos);
//						System.out.print("segmt: ");
//						System.out.println(segmt);
//						System.out.print("segments.size: ");
//						System.out.println(segments.size());
//						System.out.print("page number");
//						System.out.println(pageNumber);
//						/*TESTING*/
						throw e;
					}
				}
			}
		}
		else{
			correspondingSegments.add(-1);
		}

		return correspondingSegments;

	}

	/**
	 *
	 * @param list
	 * @param words
	 * @return
	 */

	private static float searchForWords(List<Line> list ,String[] words){

	    	ArrayList <Match> matches = new ArrayList<Match>();
	    	ArrayList<Text> currentPage = new ArrayList<Text>();

	    	for(int i= 0; i < list.size(); i++){
	    		for(int j = 0; j < list.get(i).size(); j++){

	    			currentPage.add(list.get(i).getWordAt(j));
	    		}
	    	}

	    	int currentWord;
	    	int counter=0;

	    	for(int i = 0; i < currentPage.size(); i++ ){

	    		double similarityValue = StringOperations.similarity(currentPage.get(i).getText().toLowerCase(), words[counter].toLowerCase());

				if(similarityValue > 0.8d){
					
					SystemLogger.getInstance().debug(" ** match: " + words[counter].toLowerCase() + " to: " + currentPage.get(i).getText().toLowerCase() + " s: " + similarityValue);

					int rightRange;
					int leftRange;

					Match match = new Match();

					counter++;

					match.appendWord(currentPage.get(i),similarityValue);

					currentWord = i;

					leftRange = currentWord - words.length;
					rightRange = currentWord + words.length;

					if(leftRange < 0){
						leftRange = 0;
					}

					if(rightRange > currentPage.size()-1){
						rightRange = currentPage.size();
					}

					for(byte k=1; k<words.length; k++){

						for(int n = leftRange; n < i; n++){

							if(counter < words.length){
								similarityValue = StringOperations.similarity(currentPage.get(n).getText().toLowerCase(), words[counter].toLowerCase());

								if(similarityValue > 0.7d ){

									match.appendWord(currentPage.get(n),similarityValue);

								}
							}
						}

						for(int n = i+1; n < rightRange; n++){

							if(counter < words.length){
								similarityValue = StringOperations.similarity(currentPage.get(n).getText().toLowerCase(), words[counter].toLowerCase());

								if(similarityValue > 0.7d ){

									match.appendWord(currentPage.get(n),similarityValue);

								}
							}
						}

						if(match.size()-1>k){

							byte sameWordMatchCount = (byte) (match.size() - k);

							byte left=0, right=0;
							ArrayList<Text> temp = match.getMatches();

							for(byte l=k; l<match.size(); l++){

								if(temp.get(l).getStartPositionX() < temp.get(0).getStartPositionX()){
									left++;
								}
								else{
									right++;
								}
							}

							if(left == sameWordMatchCount){


								for(byte l=(byte) (k+1); l<match.size(); l++){

									if(temp.get(k).getStartPositionX() < temp.get(l).getStartPositionX()){

										temp.remove(k);
										l--;
									}
									else{

										temp.remove(l);
										l--;
									}
								}

							}
							else if( right == sameWordMatchCount){

								for(byte l=(byte) (k+1); l<match.size(); l++){

									if(temp.get(k).getStartPositionX() > temp.get(l).getStartPositionX()){

										temp.remove(k);
										l--;
									}
									else{

										temp.remove(l);
										l--;
									}
								}

							}
							else{

								for(byte l=k; l<match.size(); l++){

									if(temp.get(k-1).getStartPositionX() > temp.get(l).getStartPositionX()){

										temp.remove(l);
										l--;
									}
								}
							}
						}

						counter++;
					}

					counter = 0;

					match.sortWords();
					matches.add(match);

					i=rightRange-1;
				}
	    	}

	    	if(!matches.isEmpty()){

	    		int longestMatchSize = 0;

	    		for(Match longest : matches){
	    			SystemLogger.getInstance().debug("match: " + longest.getText() + " size: " + longest.getMatches().size());

	    			if(longest.getMatches().size() > longestMatchSize){

	    				longestMatchSize = longest.getMatches().size();
	    			}
	    		}


	    		Iterator<Match> iterator = matches.iterator();

	    		ArrayList<Match> matchBuffer = new ArrayList<Match>();

	    		while(iterator.hasNext()){

	    			Match temp = iterator.next();

	    			if(temp.getMatches().size() == longestMatchSize){
	    				matchBuffer.add(temp);
	    				//iterator.remove();
	    			}

	    		}

	    		matches = matchBuffer;
	    	}
	    	
	    	SystemLogger.getInstance().debug(" matches size: " + matches.size() );

	    	if(!matches.isEmpty()) {
	    		
	    		//find the best match
	    		
	    		Iterator<Match> iterator = matches.iterator();
	    		
	    		double bestMatchRatio = 0;
	    		Match bestMatch = null;

	    		while(iterator.hasNext()){

	    			Match temp = iterator.next();

	    			if(temp.getMatchRatio() > bestMatchRatio){
	    				bestMatchRatio = temp.getMatchRatio();
	    				bestMatch = temp;
	    			}

	    		}

	    		return bestMatch.getMatches().get(0).getPositionY();
	    	}
	    	else
	    		return -1;
	    }

	
	
	
	private void exhaustiveHyphenFix(Vector <Line> pseudoIndex){
		for(int i = 0 ; i < pseudoIndex.size()-1; i++){
			String lastWord = pseudoIndex.get(i).getLastWord().getText();
			if(lastWord.charAt(lastWord.length()-1) == HyphenationResolver.normalHyphen) {			
				if(pseudoIndex.get(i+1).size() > 0 && hyphenResolver.hyphenatedWord(lastWord, pseudoIndex.get(i+1).getWordAt(0).getText())){
					lastWord = lastWord.substring(0, lastWord.length()-1) + pseudoIndex.get(i+1).getWordAt(0).getText();
					pseudoIndex.get(i).getLastWord().setText(lastWord);
					pseudoIndex.get(i+1).removeWordAt(0);
					if(pseudoIndex.get(i+1).getWords().size() > 0)
						pseudoIndex.get(i).addWords(pseudoIndex.get(i+1).getWords());
					pseudoIndex.remove(i+1);
					pseudoIndex.get(i).extractText();
					i--;
				}
			}
		}
	}
	
	/**
	 *
	 * @param pseudoIndex
	 */

	private void exhaustiveIndexDashFix(Vector <Line> pseudoIndex ){
		//join lines with a splitted number range
		for(int i = 0 ; i < pseudoIndex.size() ; i++){
			String text = StringOperations.cleanStringForRegex(pseudoIndex.get(i).getLastWordText());
			if(text.matches(StringOperations.getRegexNumberRangeSplitted()) && i+1 < pseudoIndex.size()) {
				String text2 =  StringOperations.cleanStringForRegex(pseudoIndex.get(i+1).getWordAt(0).getText());
				if(text2.matches(StringOperations.getRegexNumber())) {
					//join two words
					String newText = pseudoIndex.get(i).getLastWordText() + pseudoIndex.get(i+1).getWordAt(0).getText();
					pseudoIndex.get(i).getLastWord().setText(newText);
					pseudoIndex.get(i+1).removeWordAt(0);
					//join the lines
					pseudoIndex.get(i).addWords(pseudoIndex.get(i+1).getWords());
					pseudoIndex.get(i).extractText();
					pseudoIndex.remove(i+1);
					i--;	
				}
			}
		}
		
		//each line
		for(int i = 0 ; i < pseudoIndex.size() ; i++){
			//each word
			for(int j = 0 ; j < pseudoIndex.get(i).size() ; j++ ){
				//Fix for two indexPage words united in one word
				if(pseudoIndex.get(i).getWordAt(j).getText().matches("([\\s|,])*[0-9]+[,][0-9]+([–]|[-])[0-9]+([\\s|,|.])*")){
					String [] separated = pseudoIndex.get(i).getWordAt(j).getText().split(",");
					Text originalWord = pseudoIndex.get(i).getWordAt(j);
					pseudoIndex.get(i).removeWordAt(j);
					for(int pos = separated.length -1; pos >= 0; pos--) {
						String text = "";
						if(pos == separated.length -1) {
							text = separated[pos] + (separated[pos].charAt(separated[pos].length()-1) != originalWord.getText().charAt(originalWord.getText().length()-1) ? String.valueOf(originalWord.getText().charAt(originalWord.getText().length()-1)) : "");
						} if(pos == 0) {
							text = (separated[pos].charAt(0) != originalWord.getText().charAt(0) ? String.valueOf(originalWord.getText().charAt(0)) : "") + separated[pos];
						} else {
							text = separated[pos];
						}
							
						Text temp = new Text(text);
						temp.setFontName(originalWord.getFontName());
						temp.gsetFontPrefix(originalWord.getFontPrefix());
						temp.setFontSize(originalWord.getFontSize());
						temp.setPositionY(originalWord.getPositionY());
						temp.setStartPositionX(originalWord.getStartPositionX());
						temp.setEndPositionX(originalWord.getEndPositionX());
						temp.setItalic(originalWord.isItalic());
						temp.setBold(originalWord.isBold());
						temp.setHeight(originalWord.getHeight());
						temp.setWidth(originalWord.getWidth());
						temp.setCoordinates(originalWord.getCoordinates());
						pseudoIndex.get(i).addWordAt(j, temp);
					}
				}

				//if(pseudoIndex.get(i).getWordAt(j).getText().matches("([\\s|,])*[0-9]+([–]|[-])[0-9]+([\\s|,|.])*")){
				String originalText = pseudoIndex.get(i).getWordAt(j).getText();
				String text = StringOperations.cleanStringForRegex(originalText);			
				if(text.matches(StringOperations.getRegexNumberRange())){	
					SystemLogger.getInstance().debug("#### " + text);
					String [] separated = text.split("[-|–]");

					//@i.alpizarchacon: changed to add all numbers in the range, not only the start and end
					int newpages = 0;
					if(separated.length == 2) {
						try {
							String startString = separated[0].charAt(0) != originalText.charAt(0) ? String.valueOf(originalText.charAt(0)) : "";
							String endString = separated[1].charAt(separated[1].length()-1) != originalText.charAt(originalText.length()-1) ? String.valueOf(originalText.charAt(originalText.length()-1)) : "";
							int start = Integer.parseInt(separated[0]);
							int end = Integer.parseInt(separated[1]);

							if(biggetsPageNumber == -1) {
								biggetsPageNumber = this.cm.getPageNumbersOfBook(bookID).get(this.cm.getPageNumbersOfBook(bookID).size() - 1);
							}
							
							//fix range: e.g., 586-9
							if(end < start) {
								int sizeEnd = String.valueOf(end).length();
								int sizeStart = String.valueOf(start).length();
								if(sizeEnd < sizeStart) {
									String newEnd =  String.valueOf(start).substring(0, String.valueOf(start).length() -sizeEnd) + String.valueOf(end) ;
									end = Integer.valueOf(newEnd);
								}
							}
							
							if(start <= end && start <= this.biggetsPageNumber && end <= this.biggetsPageNumber) {
								
								String localPageReferenceDelimiter = this.pageReferenceDelimiter;
								for(int e = end; start <= e; e--) {
									String val = "";
									if(e == end) {
										val = String.valueOf(e);
									} else {
										val = String.valueOf(e) + localPageReferenceDelimiter;
									}
									Text newWord = new Text(val, pseudoIndex.get(i).getWordAt(j).getFontSize(),
											pseudoIndex.get(i).getWordAt(j).getStartPositionX(), pseudoIndex.get(i).getWordAt(j).getPositionY(),pseudoIndex.get(i).getWordAt(j).getCoordinates());
									newWord.setFontName(pseudoIndex.get(i).getWordAt(j).getFontName());
									newWord.setFontColor(pseudoIndex.get(i).getWordAt(j).getFontColor());
									newWord.setItalic(pseudoIndex.get(i).getWordAt(j).isItalic());
									newWord.setBold(pseudoIndex.get(i).getWordAt(j).isBold());

									pseudoIndex.get(i).addWordAt(j, newWord);
									newpages++;
									
									//add , . or ; at the end of last and first page reference
									if(e == end) {
										newWord.setText(newWord.getText() + endString);
									} else if(e == start) {
										newWord.setText(startString + newWord.getText());
									}
								}
							} else {
								continue;
							}
						} catch (NumberFormatException e) {
							for(int k= separated.length-1; k>=0; k--){

								Text newWord = new Text(separated[k], pseudoIndex.get(i).getWordAt(j).getFontSize(),
										pseudoIndex.get(i).getWordAt(j).getStartPositionX(), pseudoIndex.get(i).getWordAt(j).getPositionY(),pseudoIndex.get(i).getWordAt(j).getCoordinates());
								newWord.setFontName(pseudoIndex.get(i).getWordAt(j).getFontName());
								newWord.setFontColor(pseudoIndex.get(i).getWordAt(j).getFontColor());
								newWord.setItalic(pseudoIndex.get(i).getWordAt(j).isItalic());
								newWord.setBold(pseudoIndex.get(i).getWordAt(j).isBold());
								
								pseudoIndex.get(i).addWordAt(j, newWord);
								newpages++;
							}
						}
						
						pseudoIndex.get(i).removeWordAt(j+newpages);
						pseudoIndex.get(i).extractText();
						SystemLogger.getInstance().debug("#### " + pseudoIndex.get(i).getText());
					}
				}
			}
		}
		
	}
	
	private void exhaustiveNotesFix(Vector <Line> pseudoIndex ){
		for(int i = 0 ; i < pseudoIndex.size() ; i++){

			for(int j = 0 ; j < pseudoIndex.get(i).size() ; j++ ){
				String text = pseudoIndex.get(i).getWordAt(j).getText();
				Pattern pattern = Pattern.compile(StringOperations.getRegexNoteNumber());
				Matcher matcher = pattern.matcher(text);
				if(matcher.find()){
					pattern = Pattern.compile(StringOperations.getRegexNoteParcialNumber());
					matcher = pattern.matcher(text);
					if(matcher.find()) {
						String buffer = text.substring(0,matcher.end()-1);
						int secondPartOffset = matcher.end();
						for(char c : text.substring(matcher.end()).toCharArray()) {
							if(c < 48 || c > 57) {
								break;
							}
							secondPartOffset++;
						}
						buffer += text.substring(secondPartOffset);
						pseudoIndex.get(i).getWordAt(j).setText(buffer);
						
					}
				}
			}
			pseudoIndex.get(i).extractText();
		}
	}
	
	private void exhaustiveRomanReferencesFix(Vector <Line> pseudoIndex ) {
		for(int i = 0 ; i < pseudoIndex.size() ; i++){	
			SystemLogger.getInstance().debug("c: " +pseudoIndex.get(i).getText() );
			if(this.termDelimiter.equals(" ")){
				List<Text> words = pseudoIndex.get(i).getWords();
				float beforeX = 0;
				boolean continueDeleting = false;
				for(int w =0; w < words.size(); w++) {
					Text word = words.get(w);
					String token = word.getText();	
					if(StringOperations.cleanStringForRegex(token).matches(StringOperations.getRegexRomanNumber())
							|| StringOperations.cleanStringForRegex(token).matches(StringOperations.getRegexRomanNumberRange())) {
						float diff = word.getStartPositionX() - beforeX;
						int diffInt = Math.round(diff);
						SystemLogger.getInstance().debug("\tdiff: " +diffInt + " this.spaceDelimiterSize: " + this.spaceDelimiterSize );
						if(w == 0) {
							if((w+1) < words.size() && !wordIsPageReference(words.get(w+1).getText())) {
								break;
							} if ((w+1) < words.size() && wordIsPageReference(words.get(w+1).getText())) {
								if(this.pageReferenceDelimiter.equals(" ")){
									pseudoIndex.get(i).removeWordAt(w);
									continueDeleting = true;
									break;
								} else {
									if(this.pageReferenceDelimiter.equals(token.substring(token.length()-1))) {
										pseudoIndex.get(i).removeWordAt(w);
										continueDeleting = true;
										break;
									}
								}
							}
							
						} else if(diffInt >= this.spaceDelimiterSize || continueDeleting) {
							//2 delete word
							pseudoIndex.get(i).removeWordAt(w);
							continueDeleting = true;
							break;
						}
					}
					beforeX = word.getEndPositionX();
				}
				//3 extract new text
				pseudoIndex.get(i).setProperty("romanlocator", true);
				pseudoIndex.get(i).extractText();
			} else {
				//1 find token
				String fullText = pseudoIndex.get(i).getText();
				StringTokenizer tk = new StringTokenizer(fullText, this.termDelimiter);
				//first part is heading, should not be deleted
				if(tk.countTokens() >= 2) {
					tk.nextToken();
				}
				while(tk.hasMoreElements()) {
					String token = tk.nextToken();
					if(StringOperations.cleanStringForRegex(token).matches(StringOperations.getRegexRomanNumber())
							|| StringOperations.cleanStringForRegex(token).matches(StringOperations.getRegexRomanNumberRange())) {
						//2 delete word
						for(int w = 0; w < pseudoIndex.get(i).size(); w++) {
							if(pseudoIndex.get(i).getWordAt(w).getText().replace(",", "").replace(" ", "").matches(token.replace(" ", ""))) {
								pseudoIndex.get(i).removeWordAt(w);
								break;
							}
						}
					}
				}
				//3 extract new text
				pseudoIndex.get(i).setProperty("romanlocator", true);
				pseudoIndex.get(i).extractText();
			}
		}
	}
	
	
	private void exhaustiveLetterTitleRemover(Vector <Line> pseudoIndex ){
		Iterator<Line> it = pseudoIndex.iterator();
		while(it.hasNext()) {
			Line indexLine = it.next();
			if(indexLine.size() == 1 && indexLine.getWordAt(0).getText().trim().length() == 1 && indexLine.getWordAt(0).getText().trim().matches("[a-zA-Z]")){
				it.remove();
				continue;
			}
			if((indexLine.isBold() || indexLine.checkBold()) && indexLine.getText().toLowerCase().equals("symbols")){
				it.remove();
				continue;
			}
			if(indexLine.getText().replace(" ", "").matches("([a-zA-Z])(,[a-zA-Z]){1,2}")) {
				SystemLogger.getInstance().debug("match letter title multiple: " + indexLine.getText());
				String text = indexLine.getText().replaceAll("[^a-zA-Z]", "");
				char[] chars = text.toCharArray();
				char current = chars[0];
				boolean remove = true;
				for(int i=1; i < chars.length; i++) {
					if((current+1) != chars[i]) {
						remove = false;
						break;
					}
					current = chars[i];
				}
				if(remove) {
					it.remove();
				}
			}
		}
	}
	
	/*
	 * Finds the delimiter between the label and the page references of the index terms
	 * Also, it finds the delimiter between the pages references 
	 */
	private void findTermDelimeter(Vector <Line> pseudoIndex ){

		List<String> delimiters = new ArrayList<String>();
		List<String> paraReferenceDelimiters = new ArrayList<String>();
		Iterator<Line> it = pseudoIndex.iterator();
		
		//find the delimiters used between the label part and the page references part
		while(it.hasNext()) {
			int firstPageReferences = -1;
			Line line = it.next();
			List<Text> words = line.getWords();
			
			SystemLogger.getInstance().debug("I: " + line.getText());
			SystemLogger.getInstance().debug("w: " + words.size());
			//find first locator in line
			for(int i = 0; i < words.size(); i++) {
				Text word = words.get(i);
				SystemLogger.getInstance().debug("checking: " + word.getText() + " length:" + word.getText().length());
				boolean pageReference = this.wordIsPageReference(word.getText());
				SystemLogger.getInstance().debug("result: " + pageReference);
				if(pageReference) {
					firstPageReferences = i;
					break;
				}
			}
			SystemLogger.getInstance().debug("firstPageReferences: " + firstPageReferences);
			
			//find delimiters
			if(firstPageReferences != -1) {
				//find delimiter between heading/subheading and locators
				//use the word before the first locator
				if(firstPageReferences - 1 >= 0) {
					String wordText = words.get(firstPageReferences - 1).getText();
					if(wordText.length() > 0) {
						String lastCharOfWord = wordText.substring(wordText.length() - 1);
						if(IndexExtractor.allDelimiters.contains(lastCharOfWord)) {
							delimiters.add(lastCharOfWord);
						} else {
							delimiters.add("space");
						}		
					}
				}
				
				//find the locators delimiter
				for(int i = firstPageReferences; i < words.size() -1; i++) {
					Text currentWord = words.get(i);
					Text nextWord = words.get(i + 1);
					//if next word is a page reference (could be that it is a cross-reference word
					boolean pageReference = this.wordIsPageReference(nextWord.getText());
					SystemLogger.getInstance().debug("nextWord: " + nextWord.getText());
					SystemLogger.getInstance().debug("pageReference: " + pageReference);
					if(pageReference) {
						
						String lastCharOfWord = currentWord.getText().substring(currentWord.getText().length() - 1);
						if(IndexExtractor.allDelimiters.contains(lastCharOfWord)) {
							paraReferenceDelimiters.add(lastCharOfWord);
						} else {
							paraReferenceDelimiters.add("space");
						}
					} else {
						break;
					}
				}
			
			}		
		}
		//find the most used delimiter
		String delimiter = ListOperations.findMostFrequentItem(delimiters);
		System.out.println(delimiter);
		System.out.println(paraReferenceDelimiters);
		String pageReferenceDelimiter = ListOperations.findMostFrequentItem(paraReferenceDelimiters);
		if(pageReferenceDelimiter == null)
			pageReferenceDelimiter = "space";
		//SystemLogger.getInstance().setDebug(true);
		SystemLogger.getInstance().debug("most used delimiter: $" + delimiter + "$");
		SystemLogger.getInstance().debug("most used page reference delimiter : $" + pageReferenceDelimiter + "$" + " " + pageReferenceDelimiter.length());
		this.termDelimiter = delimiter;
		this.pageReferenceDelimiter = pageReferenceDelimiter.equals("space")? " " : pageReferenceDelimiter;
		if(delimiter.equals("space")) {
			int space = this.discoverSpaceWordsToNumbers(pseudoIndex);
			SystemLogger.getInstance().debug("Distance space: " + space);
			this.spaceDelimiterSize = space;
			this.termDelimiter = " ";
		}
	}
	
	
	private void exhaustiveSeeCasesResolver(Vector <Line> pseudoIndex ){
		//SystemLogger.getInstance().setDebug(true);
		//1-> get the normal font for the words
		List<String> fonts = new ArrayList<String>();
		for(int i = 0 ;i < pseudoIndex.size(); i++){
			for(int w = 0; w < pseudoIndex.get(i).size(); w++) {
				Text word = pseudoIndex.get(i).getWordAt(w);
				if(!wordIsPageReference(word.getText())) {
					fonts.add(word.getFontName());
				}
			}
		}
		String textFont = ListOperations.findMostFrequentItem(fonts);
		
		String regex;
		Pattern pattern;
		Matcher matcher;
		List<String> seeList = WordListCheck.getSeeList();
		List<String> seeAlsoList = WordListCheck.getSeeAlsoList();
		
		int nextTermSameIndentation = 0;
		int nextTermDiffIndentation = 0;
		int continuedLineSameIndentation = 0;
		int continuedLineDiffIndentation = 0;
		boolean onlyBiggerX = false;
		
		//see if split see lines appear at the at the same start X or bigger 
		for(int i = 0 ; i < pseudoIndex.size(); i++){
			Line line = pseudoIndex.get(i);
			if(lineContainsSeeCase(line, null , textFont, seeList)) {
				int y=i+1;
				while(y < pseudoIndex.size()) {
					Line tmpLine = pseudoIndex.get(y);
					if(!lineHasPageReferences(tmpLine) && !lineContainsSeeCase(tmpLine, null , textFont, seeList)) {
						//its continuation
						if(tmpLine.getStartPositionX() > line.getStartPositionX()) {
							SystemLogger.getInstance().debug("MATCH 1: " + line.getText() + " vs. " + tmpLine.getText() + " dff: " + (tmpLine.getStartPositionX() - line.getStartPositionX()));
							continuedLineDiffIndentation++;
						} else {
							SystemLogger.getInstance().debug("MATCH 2: " + line.getText() + " vs. " + tmpLine.getText() + " dff: " + (tmpLine.getStartPositionX() - line.getStartPositionX()));
							continuedLineSameIndentation++;
						}
					} else {
						//its new index term
						if( (int) tmpLine.getStartPositionX() <= (int) line.getStartPositionX()) {
							nextTermSameIndentation++;
						} else {
							nextTermDiffIndentation++;
						}
						
						break;
					}
					y++;
				}
			}
		}
		
		if(continuedLineDiffIndentation > 1 && nextTermSameIndentation > 1) {
			onlyBiggerX =  true;
		}
		
		SystemLogger.getInstance().debug("nextTermSameIndentation: " + nextTermSameIndentation);
		SystemLogger.getInstance().debug("nextTermDiffIndentation: " + nextTermDiffIndentation);
		SystemLogger.getInstance().debug("continuedLineSameIndentation: " + continuedLineSameIndentation);
		SystemLogger.getInstance().debug("continuedLineDiffIndentation: " + continuedLineDiffIndentation);
		SystemLogger.getInstance().debug("onlyBiggerX: " + onlyBiggerX);

		//2-> see if the "see" words appears and it is special (different font, after . or , or ()
		for(int i = 0 ; i < pseudoIndex.size(); i++){
			Line line = pseudoIndex.get(i);
			List<Text> lineWords = line.getWords();
			int indexWord = -1;
			words:
			for(int w=0; w< lineWords.size(); w++) {
				Text word = lineWords.get(w);
				String text = word.getText().toLowerCase();
				
				//if any result
				for(String see: seeList) {
					regex = "[" + IndexExtractor.seeDelimiters + "]?\\b" + see +"\\b";
				    pattern = Pattern.compile(regex);
				    matcher = pattern.matcher(text);
					//first check
				    if(matcher.matches() && !word.getFontName().equals(textFont)) {
						//second check
				    	if((w > 0 && (!word.getFontName().equals(lineWords.get(w-1).getFontName()) || seeDelimiters.contains(lineWords.get(w-1).getText().substring(lineWords.get(w-1).getText().length()-1)) || seeDelimiters.contains(text.substring(0,1))))
				    			|| (w == 0 && i > 0) && (!word.getFontName().equals(pseudoIndex.get(i-1).getLastWord().getFontName()) || seeDelimiters.contains(pseudoIndex.get(i-1).getLastWord().getText().substring(pseudoIndex.get(i-1).getLastWord().getText().length()-1)))){
				    	   	SystemLogger.getInstance().debug("\nMATCH: " + line.getText());
							indexWord = w;
							//concat lines
							int y=i+1;
							int previousLineStartPositionX = (int) line.getStartPositionX();
							boolean first =true;
							if(w == 0) {
								first = false;
							}
							while(y < pseudoIndex.size()) {
								Line tmpLine = pseudoIndex.get(y);
								if(!lineHasPageReferences(tmpLine) && !lineContainsSeeCase(tmpLine, null , textFont, seeList)) {
									//check if the reference is the last text in the line
									boolean lastWordOfLine = false;
									
									if(w == lineWords.size() - 1)
										lastWordOfLine = true;
									if(!lastWordOfLine) {
										String textOfLine = line.getText(w);
										for(String seeAlso: seeAlsoList) {
											regex = "[" + IndexExtractor.seeDelimiters + "]?\\b" + seeAlso +"\\b";
										    pattern = Pattern.compile(regex);
										    matcher = pattern.matcher(textOfLine);
										    if(matcher.find()) {
										    	if(matcher.end() >= textOfLine.length()) {
										    		lastWordOfLine = true;
											    	break;
										    	}
										    	
										    }
										}
									}
									if((lastWordOfLine)||(onlyBiggerX && (int) tmpLine.getStartPositionX() > previousLineStartPositionX) || (!onlyBiggerX || !first) && (int) tmpLine.getStartPositionX() >= previousLineStartPositionX){
										previousLineStartPositionX = (int) tmpLine.getStartPositionX();
										line.addWords(tmpLine.getWords());
										line.extractText();
										pseudoIndex.remove(y);	
										first = false;
									} else {
										break;
									}
								} else {
									break;
								}
							}
							SystemLogger.getInstance().debug("Complete LINE: " + line.getText());
							break words;
				    	}
					}
				}
			}		
			//3-> store and erase "see/also" case + additional fixes
			if(indexWord != -1) {
				//extract see/also text
				SystemLogger.getInstance().debug("---> " + line.getText(indexWord));
				SystemLogger.getInstance().debug("#" + line.getText());
				Pair<Integer, String> crossreferenceValues = this.typeOfCrossreference(line.getText(indexWord), seeList, seeAlsoList);
				//remove "see" from  line
				line.removeWordsFrom(indexWord);
				//if line is empty, remove it
				if(line.size() == 0) {
					pseudoIndex.remove(i);
					SystemLogger.getInstance().debug("FINAL LINE removed");
					if(i-1 >= 0) {
						line = pseudoIndex.get(i-1);
					} else 
						continue;
				}
				//set property to identify that the line contained a "see/also" case
				line.setProperty("crossreference", true);
				line.setProperty("type-crossreference", String.valueOf(crossreferenceValues.getLeft()));
				line.setProperty("text-crossreference", crossreferenceValues.getRight());
				//remove last '.' || ',' before "see" line
				Text lastWord = line.getLastWord();
				String lastWordText = lastWord.getText();
				if(seeDelimiters.contains(String.valueOf(lastWordText.charAt(lastWordText.length()-1)))) {
					lastWordText = lastWordText.substring(0, lastWordText.length()-1);
					lastWord.setText(lastWordText);
				}
				line.extractText();
				SystemLogger.getInstance().debug("FINAL LINE: " + line.getText());
				SystemLogger.getInstance().debug(line.getAllProperties());
			}	
		}	
//		/*TESTING*/
//		Iterator<Line> it = pseudoIndex.iterator();
//		System.out.println("after \"also\": " + pseudoIndex.size());
//		while(it.hasNext()) {
//			Line l = it.next();
//			System.out.println("^ " +l.getText() + " CX: " + l.getProperty("crossreference") );
//		}
//		System.exit(1);
//		/*TESTING*/
	}
	
	private boolean lineContainsSeeCase(Line line, Line lineBefore, String textFont, List<String> seeList) {
		List<Text> lineWords = line.getWords();
		for(int w=0; w< lineWords.size(); w++) {
			Text word = lineWords.get(w);
			String text = word.getText().toLowerCase();
			for(String see: seeList) {
				String regex = "[" + IndexExtractor.seeDelimiters + "]?\\b" + see +"\\b";
			    Pattern pattern = Pattern.compile(regex);
			    Matcher matcher = pattern.matcher(text);
				//first check
			    if(matcher.matches() && !word.getFontName().equals(textFont)) {
					//second check
			    	if((w > 0 && (!word.getFontName().equals(lineWords.get(w-1).getFontName()) || seeDelimiters.contains(lineWords.get(w-1).getText().substring(lineWords.get(w-1).getText().length()-1)) || seeDelimiters.contains(text.substring(0,1))))
			    			|| (w == 0 && lineBefore != null) && (!word.getFontName().equals(lineBefore.getLastWord().getFontName()) || seeDelimiters.contains(lineBefore.getLastWord().getText().substring(lineBefore.getLastWord().getText().length()-1)))){
			    	   	//SystemLogger.getInstance().debug("MATCH2: " + line.getText());
						return true;
			    	}
			    }
			}
		}
		return false;
	}
	
	private Pair<Integer, String> typeOfCrossreference(String text, List<String> seeList, List<String> seeAlsoList) {

		//try with see also
		for(String seeAlso: seeAlsoList) {
			String regex = "[" + IndexExtractor.seeDelimiters + "]?\\b" + seeAlso +"\\b";
		    Pattern pattern = Pattern.compile(regex);
		    Matcher matcher = pattern.matcher(text.toLowerCase());
			//first check
		    if(matcher.find()) {
		    	if(matcher.end()+1 >= text.length()) {
		    		return Pair.of(1, "");
		    	}
		    	return Pair.of(1, cleanReferenceString(text.substring(matcher.end()+1).trim()));
		    }
		}
		
		//try with see
		for(String see: seeList) {
			String regex = "[" + IndexExtractor.seeDelimiters + "]?\\b" + see +"\\b";
		    Pattern pattern = Pattern.compile(regex);
		    Matcher matcher = pattern.matcher(text.toLowerCase());
			//first check
		    if(matcher.find()) {
		    	//SystemLogger.getInstance().debug(""+matcher.end());
		    	//SystemLogger.getInstance().debug(""+text.length());
		    	//SystemLogger.getInstance().debug(""+text);
		    	if(matcher.end()+1 > text.length())
		    		return Pair.of(0, "");
		    	return Pair.of(0, cleanReferenceString(text.substring(matcher.end()+1).trim()));
		    }
		}
		
		return Pair.of(0, "");
	}
	
	private String cleanReferenceString(String input) {
		String characters = ")]}.";
		String last = input.substring(input.length()-1);
		if(characters.contains(last)) {
			input = input.substring(0, input.length()-1);
		}
		return input;
	}
	
	private boolean allNumbers(String text) {
		text = text.replace(" ", "").replaceAll("[,.]", "");
		for(int i = 0; i< text.length(); i++) {
			char c = text.charAt(i);
			if(c < 48 || c > 57) {
				return false;
			}
		}
		return true;
	}
	
	private int getWordPosOfFirstPageReferece(Line line) {
		//1 find token
		String fullText = line.getText();
		if(this.termDelimiter.equals(" ") && line.size() > 0) {
			List<Text> words = line.getWords();
			float beforeX = 0;
			for(int i = 0; i < words.size(); i++) {
				Text word = words.get(i);
				boolean pageReference = this.wordIsPageReference(word.getText());
				if(pageReference) {
					float diff = word.getStartPositionX() - beforeX;
					int diffInt = Math.round(diff);
					//if the space between the page reference and the previous word is >= to the normal space, or if the position is less than the previous word, which means it was concatenated. 
					if(diffInt >= this.spaceDelimiterSize || word.getStartPositionX() < beforeX) {
						return i;
					}
				}
				beforeX = word.getEndPositionX();
			}
		} else {
			String[] tokens = fullText.split("[" + this.termDelimiter + this.pageReferenceDelimiter + "]");
			for(String token: tokens) {
				if(wordIsPageReference(token)) {
					String clearToken = StringOperations.cleanStringForRegex(token);
					for(int w = 0; w < line.size(); w++) {
						if(StringOperations.cleanStringForRegex(line.getWordAt(w).getText()).equals(clearToken)) {
							return w;
						}
					}			
				}
			}
//			StringTokenizer tk = new StringTokenizer(fullText, this.termDelimiter);
//			while(tk.hasMoreElements()) {
//				String token = tk.nextToken();
//				System.out.println(line.getText());
//				System.out.println("delimiter: " + this.termDelimiter);
//				if(wordIsPageReference(token)) {
//					String clearToken = StringOperations.cleanStringForRegex(token);
//					for(int w = 0; w < line.size(); w++) {
//						if(StringOperations.cleanStringForRegex(line.getWordAt(w).getText()).equals(clearToken)) {
//							return w;
//						}
//					}			
//				}
//			}		
		}
		return -1;
	}
	
	private int getWordPosOfFirstNumberPageReferece(Line line) {
		//1 find token
		String fullText = line.getText();
		if(this.termDelimiter.equals(" ") && line.size() > 0) {
			List<Text> words = line.getWords();
			float beforeX = 0;
			for(int i = 0; i < words.size(); i++) {
				Text word = words.get(i);
				boolean pageReference = this.wordIsNumberPageReference(word.getText());
				if(pageReference) {
					float diff = word.getStartPositionX() - beforeX;
					int diffInt = Math.round(diff);
					//if the space between the page reference and the previous word is >= to the normal space, or if the position is less than the previous word, which means it was concatenated. 
					if(diffInt >= this.spaceDelimiterSize || word.getStartPositionX() < beforeX) {
						return i;
					}
				}
				beforeX = word.getEndPositionX();
			}
		} else {
			String[] tokens = fullText.split("[" + this.termDelimiter + this.pageReferenceDelimiter + "]");
			for(String token: tokens) {
				if(wordIsNumberPageReference(token)) {
					String clearToken = StringOperations.cleanStringForRegex(token);
					for(int w = 0; w < line.size(); w++) {
						if(StringOperations.cleanStringForRegex(line.getWordAt(w).getText()).equals(clearToken)) {
							return w;
						}
					}			
				}
			}
//			StringTokenizer tk = new StringTokenizer(fullText, this.termDelimiter);
//			while(tk.hasMoreElements()) {
//				String token = tk.nextToken();
//				System.out.println(line.getText());
//				System.out.println("delimiter: " + this.termDelimiter);
//				if(wordIsPageReference(token)) {
//					String clearToken = StringOperations.cleanStringForRegex(token);
//					for(int w = 0; w < line.size(); w++) {
//						if(StringOperations.cleanStringForRegex(line.getWordAt(w).getText()).equals(clearToken)) {
//							return w;
//						}
//					}			
//				}
//			}		
		}
		return -1;
	}
	
	
	//###############################################
	//Index term: label + references
	private int getWordPosOfFirstPageReferece(String fullText) {
		//1 find token
		StringTokenizer tk = new StringTokenizer(fullText, this.termDelimiter);
		int i =0;
		while(tk.hasMoreElements()) {
			String token = tk.nextToken();
			if(wordIsPageReference(token)) {
				return i;			
			}
			i++;
		}		
		return -1;
	}
	
	private boolean lineHasPageReferences(Line line) {
		int start = getWordPosOfFirstPageReferece(line);
		if(start != -1) {
			return true;
		} else {
			return false;
		}
	}
	

	private boolean wordIsPageReference(String text) {
		
		if(biggetsPageNumber == -1) {
			biggetsPageNumber = this.cm.getPageNumbersOfBook(bookID).get(this.cm.getPageNumbersOfBook(bookID).size() - 1);
		}
		
		String originalString = text;
		text = StringOperations.cleanStringForRegex(text);
		if(text.matches(StringOperations.getRegexNumber())) {
			int val = 0;
			try {
				val = Integer.valueOf(text);
			} catch (NumberFormatException e) {
				boolean allNumbers = true;
				for(char c: originalString.toCharArray()) {
					if(c != 44 && (c < 48 || c > 57)) {
						allNumbers = false;
						break;
					}
				}
				return allNumbers;
			}
			if(val <= biggetsPageNumber)
				return true;
			else 
				return false;
		} else if(text.matches(StringOperations.getRegexNumberNumberRange())) {
			return false;
		} else if(text.matches(StringOperations.getRegexNumberRange())) {
			String [] separated = text.split("[-|–]");
			if(separated.length == 2) {
				int start = Integer.parseInt(separated[0]);
				int end = Integer.parseInt(separated[1]);
				if(start <= end && start <= this.biggetsPageNumber && end <= this.biggetsPageNumber) {
					return true;
				}
			}
			return false;
		} else if(text.matches(StringOperations.getRegexRomanNumber())) {
			return true;
		} else if(text.matches(StringOperations.getRegexRomanNumberRange())) {
			return true;
		} else if(text.matches(StringOperations.getRegexNoteNumber())) {
			return true;
		}
		
		return false;
	}
	
	private boolean wordIsNumberPageReference(String text) {
		
		if(biggetsPageNumber == -1) {
			biggetsPageNumber = this.cm.getPageNumbersOfBook(bookID).get(this.cm.getPageNumbersOfBook(bookID).size() - 1);
		}
		
		text = StringOperations.cleanStringForRegex(text);
		if(text.matches(StringOperations.getRegexNumber())) {
			int val = Integer.valueOf(text);
			if(val <= biggetsPageNumber)
				return true;
			else 
				return false;
		}
		
		return false;
	}
	
	//###############################################
	private Vector <Line> createIndexElements(Vector <Line> page, LanguageEnum lang){
		//#1 Group lines where only page number(s) is in the next line
		for(int i = 0 ; i < page.size()-1 ; i++){

			if(page.get(i) != null && page.get(i).size()>0 )	{

				SystemLogger.getInstance().debug(" checking: " + page.get(i+1).getText() + " R: " +  (StringUtils.isNumeric(page.get(i+1).getText().replaceAll("[,.]", "").replaceAll(" ","")) || allNumbers(page.get(i+1).getText())));
				if(StringUtils.isNumeric(page.get(i+1).getText().replaceAll("[,.]", "").replaceAll(" ","")) || allNumbers(page.get(i+1).getText())){
				//if(StringOperations.isIndexPageNumber(page.get(i+1))){

					//@i.alpizarchacon fix to the problem that first words of some lines have different (bigger) positionX that the line itself.
					float pos1 = page.get(i).getStartPositionX();
					page.get(i).getWordAt(0).setStartPositionX(pos1);

					Line groupingLine = new Line();

					groupingLine.addWords(new Vector<Text>(page.get(i).getWords()));
					groupingLine.ingestProperties(page.get(i).getProperties());

					groupingLine .addWords(new Vector<Text>(page.get(i+1).getWords()));
					groupingLine.ingestProperties(page.get(i+1).getProperties());
					
					//test for more lines
					for(int extra = i+2; extra < page.size();) {
						SystemLogger.getInstance().debug("\t 2 checking: "+ page.get(extra).getText() + " R: " + (page.get(extra) != null && page.get(extra).size()>0 && allNumbers(page.get(extra).getText())));
						if(page.get(extra) != null && page.get(extra).size()>0){
							//String textWithoutSeeCases = exhaustiveSeeCasesResolverOneLine(page.get(extra));
							if(allNumbers(page.get(extra).getText()))  {
								SystemLogger.getInstance().debug("grouping");
								groupingLine.addWords(new Vector<Text>(page.get(extra).getWords()));
								groupingLine.ingestProperties(page.get(extra).getProperties());
								page.remove(extra);
							} else {
								break;
							}
						} else {
							break;
						}
					}

//					/*TESTING*/
//					System.out.println("@@CASE 1 ");
//					System.out.println("L1: " + page.get(i).getText());
//					System.out.println("L2" + page.get(i+1).getText());
////					System.out.println("L1 positionX: " + page.get(i).getStartPositionX());
////					System.out.println("L1 WORD: " + page.get(i).getWordAt(0).getText());
////					System.out.println("L1 WORD positionX: " + page.get(i).getWordAt(0).getStartPositionX());
////					System.out.println("L2 positionX: " + page.get(i+1).getStartPositionX());
//					/*TESTING*/
//
//					//*TESTING*/
//					//System.out.println("New Line: " + page.get(i).getText());
//					//System.out.println("New Line positionX before extractText: " + groupingLine.getStartPositionX());
//					//*TESTING*/

					groupingLine.extractText();

					//@i.alpizarchacon
					//page.remove(i+1) + add changed for set;
					page.set(i+1, groupingLine);
					page.remove(i);
				}
			}
		}
		
//		/*TESTING*/
//		Iterator<Line> it = page.iterator();
//		System.out.println("after case #1 size: " + page.size());
//		while(it.hasNext())
//			System.out.println("^ " + it.next().getText());
//		System.exit(1);
//		/*TESTING*/

		//#1.5 Group lines where the line term does not have a page number, and it only has one child term without page number
		for(int i = 0 ; i < page.size()-2 ; i++){
			if(page.get(i) != null && page.get(i).size()>0 )	{
				
				if(page.get(i).getPropertyAsBoolean("crossreference") || page.get(i).getPropertyAsBoolean("romanlocator")) {
					continue;
				}

				//check that the term does not end in a number, and that two terms ahead the term is aligned.
				//This means that one term ahead is part of the actual term
				if(!StringUtils.isNumeric(page.get(i).getWordAt(page.get(i).size()-1).getText().replaceAll("[,.]", ""))
						&& 	 (BoundSimilarity.isInBound(page.get(i).getStartPositionX(),page.get(i+2).getStartPositionX(),page.get(i).getFontSize(),page.get(i+2).getFontSize(),0.6f ) 
								|| page.get(i).getStartPositionX() > page.get(i+2).getStartPositionX())
						&& page.get(i+1).getStartPositionX() >= page.get(i).getStartPositionX()){
 
					//if the second line does not end in a number page
					if(!StringUtils.isNumeric(page.get(i+1).getWordAt(page.get(i+1).size()-1).getText().replaceAll("[,.]", ""))){
						//@i.alpizarchacon fix to the problem that first words of some lines have different (bigger) positionX that the line itself.
						float pos1 = page.get(i).getStartPositionX();
						page.get(i).getWordAt(0).setStartPositionX(pos1);

//						/*TESTING*/
//						System.out.println("@@CASE 1.5! ");
//						System.out.println("L1: " + page.get(i).getText());
//						System.out.println("\t1 crossreference: " + page.get(i).getProperty("crossreference"));
//						System.out.println("L2: " + page.get(i+1).getText());
//						System.out.println("L1 positionX: " + page.get(i).getStartPositionX());
//						System.out.println("L1 WORD 0 positionX: " + page.get(i).getWordAt(0).getStartPositionX());
//						System.out.println("L2 positionX: " + page.get(i+1).getStartPositionX());
//						/*TESTING*/

						Line groupingLine = new Line();

						groupingLine.addWords(new Vector<Text>(page.get(i).getWords()));
						groupingLine.ingestProperties(page.get(i).getProperties());
						groupingLine .addWords(new Vector<Text>(page.get(i+1).getWords()));
						groupingLine.ingestProperties(page.get(i+1).getProperties());

						groupingLine.extractText();

						//@i.alpizarchacon
						page.set(i+1, groupingLine);
						page.remove(i);

//						/*TESTING*/
//						System.out.println("New Line: " + page.get(i).getText());
//						System.out.println("New Line positionX: " + page.get(i).getStartPositionX());
//						/*TESTING*/
					}
				}
			}
		}
		
//		/*TESTING*/
//		Iterator<Line> it1 = page.iterator();
//		System.out.println("***");
//		while(it1.hasNext())
//			System.out.println("^ " + it1.next().getText());
//		System.exit(1);
//		/*TESTING*/
//		
		//TODO improve this: make it in one method that can be called several times, until no line has been appended
		//second pass for indexes with 3 columns or more
		if(this.columnFormat != null && this.columnFormat.numberOfColumns >= 3) {
			SystemLogger.getInstance().debug("############################################# 2 ");
			for(int i = 0 ; i < page.size()-2 ; i++){
				if(page.get(i) != null && page.get(i).size()>0 )	{
					
					if(page.get(i).getPropertyAsBoolean("crossreference")) {
						continue;
					}

					//check that the term does not end in a number, and that two terms ahead the term is aligned.
					//This means that one term ahead is part of the actual term
					if(!StringUtils.isNumeric(page.get(i).getWordAt(page.get(i).size()-1).getText().replaceAll("[,.]", ""))
							&& 	 (BoundSimilarity.isInBound(page.get(i).getStartPositionX(),page.get(i+2).getStartPositionX(),page.get(i).getFontSize(),page.get(i+2).getFontSize(),0.6f ) 
									|| page.get(i).getStartPositionX() > page.get(i+2).getStartPositionX())
							&& page.get(i+1).getStartPositionX() >= page.get(i).getStartPositionX()){

						//if the second line does not end in a number page
						if(!StringUtils.isNumeric(page.get(i+1).getWordAt(page.get(i+1).size()-1).getText().replaceAll("[,.]", ""))){
							//@i.alpizarchacon fix to the problem that first words of some lines have different (bigger) positionX that the line itself.
							float pos1 = page.get(i).getStartPositionX();
							page.get(i).getWordAt(0).setStartPositionX(pos1);

//							/*TESTING*/
//							System.out.println("@@CASE 1.5! ");
//							System.out.println("L1: " + page.get(i).getText());
//							System.out.println("L2: " + page.get(i+1).getText());
//							System.out.println("L1 positionX: " + page.get(i).getStartPositionX());
//							System.out.println("L1 WORD 0 positionX: " + page.get(i).getWordAt(0).getStartPositionX());
//							System.out.println("L2 positionX: " + page.get(i+1).getStartPositionX());
//							/*TESTING*/

							Line groupingLine = new Line();

							groupingLine.addWords(new Vector<Text>(page.get(i).getWords()));
							groupingLine.ingestProperties(page.get(i).getProperties());
							groupingLine .addWords(new Vector<Text>(page.get(i+1).getWords()));
							groupingLine.ingestProperties(page.get(i+1).getProperties());

							groupingLine.extractText();

							//@i.alpizarchacon
							page.set(i+1, groupingLine);
							page.remove(i);

//							/*TESTING*/
//							System.out.println("New Line: " + page.get(i).getText());
//							System.out.println("New Line positionX: " + page.get(i).getStartPositionX());
//							/*TESTING*/
						}
					}
				}
			}
		}
		
//		/*TESTING*/
//		Iterator<Line> it = page.iterator();
//		System.out.println("after case #1.5 size: " + page.size());
//		while(it.hasNext())
//			System.out.println("^ " + it.next().getText());
//		System.exit(1);
//		/*TESTING*/

		//#2 Group lines where the line term does not have a page number, and it only has one child term with page number
		int cases = 0;
		for(int i = 0 ; i < page.size()-2 ; i++){
 			
			if(page.get(i) != null && page.get(i).size()>0 )	{
				
				if(page.get(i).getPropertyAsBoolean("crossreference")) {
					continue;
				}

				//check that the term does not end in a number, and that two terms ahead the term is aligned.
				//This means that one term ahead is part of the actual term
				int line1Start = (int) page.get(i).getStartPositionX();
				int line2Start = (int) page.get(i+1).getStartPositionX();
				int line3Start = (int) page.get(i+2).getStartPositionX();
				if(!StringUtils.isNumeric(page.get(i).getWordAt(page.get(i).size()-1).getText().replaceAll("[,.]", ""))
						&& 	 (BoundSimilarity.isInBound(page.get(i).getStartPositionX(),page.get(i+2).getStartPositionX(),page.get(i).getFontSize(),page.get(i+2).getFontSize(),0.6f )
								|| line2Start - line3Start > 4)
						&& line2Start > line1Start){

					//@i.alpizarchacon fix to the problem that first words of some lines have different (bigger) positionX that the line itself.
					float pos1 = page.get(i).getStartPositionX();
					page.get(i).getWordAt(0).setStartPositionX(pos1);

//					/*TESTING*/
//					cases++;
//					System.out.println("@@CASE 2 ");
//					System.out.println("L1: " + page.get(i).getText());
//					System.out.println("L2: " + page.get(i+1).getText());
//					System.out.println("L1 positionX: " + page.get(i).getStartPositionX());
//					System.out.println("L1 WORD 0 positionX: " + page.get(i).getWordAt(0).getStartPositionX());
//					System.out.println("L2 positionX: " + page.get(i+1).getStartPositionX());
//					/*TESTING*/

					Line groupingLine = new Line();

					groupingLine.addWords(new Vector<Text>(page.get(i).getWords()));
					groupingLine.ingestProperties(page.get(i).getProperties());
					//comment: in springer books these cases are only one line
					groupingLine .addWords(new Vector<Text>(page.get(i+1).getWords()));
					groupingLine.ingestProperties(page.get(i+1).getProperties());

					groupingLine.extractText();

					//@i.alpizarchacon
					page.set(i+1, groupingLine);
					page.remove(i);

					/*TESTING*/
					//System.out.println("New Line: " + page.get(i).getText());
					//System.out.println("New Line positionX: " + page.get(i).getStartPositionX());
					/*TESTING*/
				}
			}
		}
		
//		/*TESTING*/
//		Iterator<Line> it = page.iterator();
//		System.out.println("after case #2 size: " + page.size());
//		while(it.hasNext()) {
//			Line l = it.next();
//			System.out.println("^" + l.getStartPositionX() + " " + l.getText());
//		}
//		System.exit(1);
//		/*TESTING*/
		
		//#2.4 Group lines that have 2do and 3rd level index terms together
		for(int i = 0 ; i < page.size()-1; i++){
			if(page.get(i) != null && page.get(i).size()>0 && page.get(i).getText().contains(this.inLineDelimiter))	{
				//create new line
				Line newLine = new Line(page.get(i));
				//add next lines
				int j = 1;
				while(page.get(i+j).getStartPositionX() > (newLine.getStartPositionX() + 4)) {
					newLine.addWords(page.get(i+j).getWords());
					newLine.ingestProperties(page.get(i+j).getProperties());
					j++;
				}
				newLine.extractText();
				//validate the new line
				StringTokenizer st = new StringTokenizer(newLine.getText(), this.inLineDelimiter);
				boolean validFormat = true;
				if(st.countTokens() > 1) {
					st.nextToken();
					while(st.hasMoreElements()) {
						String part = st.nextToken();
						if(this.getWordPosOfFirstPageReferece(part) == -1) {
							validFormat = false;
							break;
						}
					}
				}
				//adjust lines
				if(validFormat) {
					page.set(i, newLine);
					while(j > 1) {
						page.remove(i+1);
						j--;
					}
				} 	
			}
		}

//		/*TESTING*/
//		Iterator<Line> it = page.iterator();
//		System.out.println("after case #2.4 size: " + page.size());
//		while(it.hasNext())
//			System.out.println("^ " + it.next().getText());
//		System.exit(1);
//		/*TESTING*/
		
		//#2.5 Group lines where the line term does not have a page number, and it has several children but only the last child term with page number
		for(int i = 0 ; i < page.size()-2 ; i++){
 			
			if(page.get(i) != null && page.get(i).size()>0 )	{
				
				if(page.get(i).getPropertyAsBoolean("crossreference")) {
					continue;
				}

				//check that the term does not end in a number, and that two terms ahead the term is aligned.
				//This means that one term ahead is part of the actual term
				int line1Start = (int) page.get(i).getStartPositionX();
				List<Line> nextLines = new ArrayList<Line>();
				if(!StringUtils.isNumeric(page.get(i).getWordAt(page.get(i).size()-1).getText().replaceAll("[,.]", ""))){
					//get lines
					nextLines.add(page.get(i));
					for(int y = i+1; y < page.size(); y++) {
						Line tmp = page.get(y);
						if((int)tmp.getStartPositionX() <= line1Start) {
							break;
						} else {
							nextLines.add(tmp);
						}
					}
					
					//check lines
					boolean allWithoutNumber = true;
					for(int z = 0; z < nextLines.size() -1; z++) {
						if(StringUtils.isNumeric(nextLines.get(z).getWordAt(nextLines.get(z).size()-1).getText().replaceAll("[,.]", ""))) {
							allWithoutNumber = false;
						}
					}
					if(allWithoutNumber) {
						if(StringUtils.isNumeric(nextLines.get(nextLines.size() -1).getWordAt(nextLines.get(nextLines.size() -1).size()-1).getText().replaceAll("[,.]", ""))) {
							Line groupingLine = new Line();
							for(int z = 0; z < nextLines.size(); z++) {
								groupingLine.addWords(new Vector<Text>(nextLines.get(z).getWords()));
								groupingLine.ingestProperties(nextLines.get(z).getProperties());
							}
							groupingLine.extractText();

							page.set(i, groupingLine);
							int times = nextLines.size()-1;
							while(times > 0){
								page.remove(i+1);
								times--;
							}
						}
					}
				}
			}
		}
		
//		/*TESTING*/
//		System.out.println("after case #2.5 size: " + page.size());
//		Iterator<Line> it = page.iterator();
//		while(it.hasNext()) {
//			Line l = it.next();
//			System.out.println("^ " + l.getStartPositionX() + " " + l.getText());
//		}
//		System.exit(0);
//		/*TESTING*/	
		
		//#2.6 Remove lines of continuing index terms "(cont.)"
		for(int i = 0 ; i < page.size()-1 ; i++){
			for(Text word: page.get(i).getWords()) {
				if(WordListCheck.isContWord(word.getText())) {
					page.remove(i);
					i--;
					break;
				}
			}
		}
		
//		//#2.7 Remove page references with roman numbers
//		SystemLogger.getInstance().log("Resolving roman numbers references in index pages ... ");
//		exhaustiveRomanReferencesFix(page);
		
//		/*
//		 * EDUHINT clean up
//		 * 
//		 */
//		Iterator<Line> it1 = page.iterator();
//		while(it1.hasNext()) {
//			Line l = it1.next();
//			for(int w=0; w < l.size(); w++) {
//				try {
//					if(l.getWordAt(w).getFontColor().toRGB() == 9683212) {
//						l.removeWordAt(w);
//						w--;
//					}
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//			l.extractText();
//		}
//		/*
//		 * EDUHINT clean up END
//		 * 
//		 */
		
//		/*TESTING*/
//		Iterator<Line> it = page.iterator();
//		System.out.println("____________________________");
//		System.out.println("before grouping: " + page.size());
//		while(it.hasNext()) {
//			Line l = it.next();
//			System.out.println("^ " + l.getStartPositionX() + " " + l.getText());
//		}
//		System.exit(0);
//		/*TESTING*/
//		
		//#4 Resolve multiple in line entries
		for(int i = 0 ; i < page.size(); i++){
			if (page.get(i).getText().contains(IndexExtractor.inLineDelimiter)) {
				Vector<Line> groupedLines = new Vector<Line>();
				groupedLines.add(page.get(i));
				page.remove(i);
				resolveMultipleInLineEntries(groupedLines);
				page.addAll(i,groupedLines);
				i+=groupedLines.size()-1;
			}
		}
		
//		/*TESTING*/
//		Iterator<Line> it5 = page.iterator();
//		System.out.println(" before 5: " + page.size());
//		while(it5.hasNext()) {
//			Line l = it5.next();
//			System.out.println("^ " + l.getStartPositionX() + " " + l.getText());
//		}
//		System.out.println("IndexExtractor.termDelimiter " + this.termDelimiter);
//		System.exit(0);
//		/*TESTING*/
		
		//#5 Mark the division between label and page references in each index term
		for(int i = 0 ; i < page.size(); i++){
			int div = this.getWordPosOfFirstNumberPageReferece(page.get(i));
			if(div == 0 && page.get(i-1) != null){
				page.get(i-1).addWords(page.get(i).getWords());
				page.get(i-1).extractText();
				page.remove(i);
				i--;
				continue;
			}
			if(div != -1) {
				//remove last divider char before page numbers (like ",")
				Text lastWordBeforeDivider = page.get(i).getWordAt(div-1);
				if(!this.termDelimiter.equals(" ")) {
					if(this.termDelimiter.equals(lastWordBeforeDivider.getText().substring(lastWordBeforeDivider.getText().length()-1))) {
						lastWordBeforeDivider.setText(lastWordBeforeDivider.getText().substring(0, lastWordBeforeDivider.getText().length()-1));
					}
				}
				//add term divider
				Text w = new Text(IndexExtractor.termDivider);
				page.get(i).addWordAt(div, w);
				//remove extra chars for page numbers (like ,)
				for(int j=div+1; j < page.get(i).size(); j++) {
					String text = page.get(i).getWordAt(j).getText();
					text = text.replaceAll("[^0-9]", "").trim();
					page.get(i).getWordAt(j).setText(text);
				}
				page.get(i).extractText();
			}
		}

//		/*TESTING*/
//		Iterator<Line> it5 = page.iterator();
//		System.out.println("____________________________ resolveMultipleInLineEntries: " + page.size());
//		while(it5.hasNext()) {
//			Line l = it5.next();
//			System.out.println("^ " + l.getStartPositionX() + " " + l.getText());
//		}
//		System.exit(0);
//		/*TESTING*/

 		//#6 Create index terms, grouping different lines
		for(int i = 0 ; i < page.size()-1 ; i++){

			if(page.get(i) != null && page.get(i).size()>0 )	{

				if(!BoundSimilarity.isInBound(page.get(i).getStartPositionX(),page.get(i+1).getStartPositionX(),
						page.get(i).getFontSize(),page.get(i+1).getFontSize(),0.6f)){

//					/*TESTING*/
//					System.out.println("@@@ NEW GROUP @@@");
//					System.out.println("L1: " + page.get(i).getText());
//					System.out.println("L1 posX: " + page.get(i).getStartPositionX());
//					System.out.println("L2: " + page.get(i+1).getText());
//					System.out.println("L2 posX: " + page.get(i+1).getStartPositionX());
//					//System.exit(1);
//					/*TESTING*/

					short depth = 1;

					Vector<Line> groupedLines = new Vector<Line>();

					groupedLines.add(page.get(i));

					//@i.alpizarchacon: removed -1 from where condition, it was preventing last index term to be grouped
					while(i+depth < page.size()
							&&  !BoundSimilarity.isInBound(page.get(i).getStartPositionX(),page.get(i+depth).getStartPositionX(),
									page.get(i).getFontSize(),page.get(i+depth).getFontSize(),0.6f)
							){

						groupedLines.add(page.get(i+depth));

						depth++;
					}

					page.removeAll(groupedLines);

//					/*TESTING*/
//					System.out.println("@@@ NEW GROUP @@@");
//					Iterator<Line> it = groupedLines.iterator();
//					groupedLines.get(0).getText().trim();
//					while(it.hasNext())
//						System.out.println("^ " + it.next().getText());

					Vector<Line> temp =  indexGroupResolverIndexElements(groupedLines);

					page.addAll(i,temp);

					i+=temp.size()-1;			
				}
			} 
		}
		return page;
	}

	private void removeIndexElementsSpecialCases(Vector <Line> page){
		for(int i = 0 ; i < page.size()-1 ; i++){
			page.get(i).extractText();
			String text = page.get(i).getText();
			boolean indexSpecialCase = WordListCheck.containsSpecialCases(text);
			if( indexSpecialCase ) {
				SystemLogger.getInstance().debug("ERASE: " + text);
				page.remove(i);
				i--;
			}
		}
	}
	
	private void resolveMultipleInLineEntries(Vector<Line> lines) {
		for(int i =0; i < lines.size(); i++) {
			Line line = lines.get(i);
			StringTokenizer st = new StringTokenizer(line.getText(), this.inLineDelimiter);
			if(st.countTokens() > 1) {
				st.nextToken();
				boolean split = false;
				while(st.hasMoreElements()) {
					String part = st.nextToken();
					if(this.getWordPosOfFirstPageReferece(part) != -1) {
						split = true;
						break;
					}
				}
				if(split) {
					boolean first = true;
					float start = 0;
					List<Text> tmp = new ArrayList<Text>();
					int j = i + 1;
					for(int w=0; w < line.size(); w++) {
						Text word = line.getWordAt(w);
						tmp.add(word);
						if(word.getText().indexOf(this.inLineDelimiter) == word.getText().length() -1 || w == line.size()-1) {
							word.setText(word.getText().replaceAll(this.inLineDelimiter, ""));
							if(first) {
								Line newLine = new Line(line, tmp);
								newLine.extractText();
								lines.set(i, newLine);
								start = newLine.getLastWord().getEndPositionX() + newLine.getLastWord().getSpaceWidth();
								tmp = new ArrayList<Text>();
								first = false;
							} else {
								Line newLine = new Line(line, tmp);
								newLine.getWordAt(0).setStartPositionX(start);
								newLine.extractText();
								lines.add(j++, newLine);
								tmp = new ArrayList<Text>();
							}
							
						}
					}
					i = j - 1;
				}
			}
		}
	}
	
	private Vector<Line> indexGroupResolverInnerLayersIndexElements(Vector<Line> groupedLines){

		Vector<Line> buffer = new Vector<Line> (groupedLines);
		Vector<Line> resolved = new Vector<Line>();

		if(groupedLines.size()>1){
			if(!BoundSimilarity.isInBound(groupedLines.get(0).getStartPositionX(),groupedLines.get(1).getStartPositionX(),
					groupedLines.get(0).getFontSize(),groupedLines.get(1).getFontSize(),0.6f)){

				Vector<Line> temp = new Vector<Line> ();
				Line root = new Line(groupedLines.get(0));
				root.ingestProperties(groupedLines.get(0).getProperties());
				buffer.remove(0);

				byte depth = 1;

				//@i.alpizarchacon FIXED changed from 'depth < groupedLines.size() - 1' to 'depth < groupedLines.size()'
				while(depth < groupedLines.size()
						&&  !BoundSimilarity.isInBound(root.getStartPositionX(),groupedLines.get(depth).getStartPositionX(),
								root.getFontSize(),groupedLines.get(depth).getFontSize(),0.6f)
						){

					temp.add(groupedLines.get(depth));

					buffer.remove(0);
					depth++;
				}

				temp = indexGroupResolverInnerLayersIndexElements(temp);

				//if the root line has page numbers, add the line to the resolved group
				if(root.getText().contains(this.termDivider)){
					resolved.add(root);
				} else {
					//get the page number from the children nodes, and add the line to the resolved group
					SortedSet<Integer> pages = this.findPagesInLines(temp);
					if(pages.size() != 0) {
						Iterator<Integer> it= pages.iterator();
						root.addWord(new Text(this.termDivider));
						while(it.hasNext()) {
							Integer page = it.next();
							root.addWord(new Text(page.toString() ));	
						}
						root.extractText();
					}
					root.setArtificial(true);
					resolved.add(root);	
				}

				//resolve inner lines
				for(byte i=0; i<temp.size() ; i++){

					Line tempLine = new Line();

					Pair<Line, Line> division = separateTermLine(root);
					tempLine.addWords(division.getLeft().getWords());
					tempLine.addWord(new Text("<>"));
					tempLine.addWords(temp.get(i).getWords());
					tempLine.ingestProperties(temp.get(i).getProperties());
					tempLine.setArtificial(temp.get(i).isArtificial());
					tempLine.extractText();
					resolved.add(tempLine);
					
				}
				resolved.addAll(indexGroupResolverInnerLayersIndexElements(buffer));
			}
			else{

				resolved.add(groupedLines.get(0));
				buffer.remove(0);

				resolved.addAll(indexGroupResolverInnerLayersIndexElements(buffer));
			}

			return resolved;
		}
		else
			return groupedLines;
	}
	
	/**
	 *
	 * @param groupedLines
	 * @return
	 */
	private Vector<Line> indexGroupResolverIndexElements(Vector<Line> groupedLines){
		Line root;

		Vector<Line> buffer = new Vector<Line> (groupedLines);
		Vector<Line> resolved = new Vector<Line>();
		Vector<Line> temp = new Vector<Line>();

		root = new Line ( groupedLines.get(0));
		root.ingestProperties(groupedLines.get(0).getProperties());
		buffer.remove(0);

		temp = indexGroupResolverInnerLayersIndexElements(buffer);

//		/*TESTING*/
//		System.out.println("***** TEMP");
//		System.out.println("root-> " + root.getText());
//		Iterator<Line> it2 = temp.iterator();
//		while(it2.hasNext())
//			System.out.println("^ " + it2.next().getText());
//		System.out.println("***** END");
//		/*TESTING*/

		//if the root line has page numbers, add the line to the resolved group
		if(root.getText().contains(this.termDivider)) {
			resolved.add(root);
		} else {
			//get the page number from the children nodes, and add the line to the resolved group
			SortedSet<Integer> pages = this.findPagesInLines(temp);
			if(pages.size() != 0) {
				Iterator<Integer> it= pages.iterator();
				root.addWord(new Text(this.termDivider));
				while(it.hasNext()) {
					Integer page = it.next();
					root.addWord(new Text(page.toString()));
				}
				root.extractText();
			}
			root.setArtificial(true);
			resolved.add(root);	
		}

		//Construct index lines joinining the root with the children lines
		for(short i = 0 ; i < temp.size() ; i++){
			
			Line tempLine = new Line();
			
			Pair<Line, Line> division = separateTermLine(root);
			tempLine.addWords(division.getLeft().getWords());
			tempLine.addWord(new Text("<>"));
			tempLine.addWords(temp.get(i).getWords());
			tempLine.ingestProperties(temp.get(i).getProperties());
			tempLine.setArtificial(temp.get(i).isArtificial());
			tempLine.extractText();
			resolved.add(tempLine);
		}
		return resolved;
	}

/**
 *
 * @param book
 * @param pageNumber
 * @return
 */
	private int findPageIndexWithPageNumber(List<Page> book, int pageNumber){

		for(int i = 0; i<book.size(); i++){

			if(book.get(i) != null && book.get(i).getPageNumber() == pageNumber)
				return i;
		}

		return -1;
	}
	
	
	public SortedSet<Integer> findPagesInLines(Vector<Line> lines){
		SortedSet<Integer> results = new TreeSet<Integer>();
		for(Line line: lines) {
			Line references = separateTermLine(line).getRight();
			for(Text word: references.getWords()) {
				String buf = word.getText().replaceAll("[^0-9]", "");
				if(!buf.equals("")){
					if(StringUtils.isNumeric(buf)) {
						try {
							results.add(Integer.parseInt(buf));
						} catch (NumberFormatException e) {
							e.printStackTrace();
						}
					}				
				}
			}
		}
		return results;
	}
	
	public Pair<Line,Line> separateTermLine(Line line){
		Line label = new Line(line, null);
		Line references = new Line(line, null);
		
		boolean divider = false;
		for(Text word: line.getWords()) {
			if(word.getText().equals(this.termDivider)) {
				divider = true;
			} else if (divider) {
				references.addWord(word);
			} else {
				label.addWord(word);
			}
		}
		
		return Pair.of(label, references);
	}

}
