package intextbooks.content.extraction.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.hp.hpl.jena.sparql.util.Convert;

import intextbooks.SystemLogger;
import intextbooks.content.extraction.Utilities.BoundSimilarity;
import intextbooks.content.extraction.Utilities.GenericStatisticsMapHandle;
import intextbooks.content.extraction.Utilities.NERPersonTagger;
import intextbooks.content.extraction.Utilities.StringOperations;
import intextbooks.content.extraction.Utilities.WordListCheck;
import intextbooks.content.extraction.buildingBlocks.format.Line;
import intextbooks.content.extraction.buildingBlocks.format.Page;
import intextbooks.content.extraction.buildingBlocks.format.Text;
import intextbooks.content.extraction.buildingBlocks.structure.TOC;
import intextbooks.exceptions.TOCNotFoundException;
import intextbooks.ontologie.LanguageEnum;
import intextbooks.tools.utility.ListOperations;


public class TableOfContentsExtractor {

	public static enum tocType {FLAT, INDENTED, ORDERED};

	private static tocType type;

	final static List <String> RomanNumber = Arrays.asList("I", "II", "III","IV","V","VI","VII","VIII","IX","X",
			"XI","XII","XIII","XIV","XV","XVI","XVII","XVIII","XIX","XX");
	
	final public static String regexRomanNumber = "^(?=[MDCLXVI])M*(C[MD]|D?C{0,3})(X[CL]|L?X{0,3})(I[XV]|V?I{0,3})$";
	
	public static HashMap<Integer, Integer> lineSpacingStatistics;
	/**
	 * 
	 * @param tableOfContentsStartPageIndx
	 * @param pages
	 * @return
	 * @throws TOCNotFoundException 
	 */
	public static Pair<List<Integer>, ArrayList<TOC>> extractToc(int tableOfContentsStartPageIndx, Vector<Page> pages, Line TOCTitleLine, float textBodyFontSize, LanguageEnum lang) throws TOCNotFoundException{

		ArrayList<Line> toc = new ArrayList<Line>();
		ArrayList<Integer> tocPages = new ArrayList<Integer>();

		for(int i = tableOfContentsStartPageIndx; i<50 ; i++){

			Page page = pages.get(i);

			if(page == null)
				break;				
			else{
				boolean belongsToTOC;

				if(i == tableOfContentsStartPageIndx) {
					belongsToTOC = checkTocLines(page, null, textBodyFontSize);	
				} else {
					belongsToTOC = checkTocLines(page, TOCTitleLine, textBodyFontSize);	
				}
				if(!belongsToTOC)
					break;
//					/*TESTING*/
//					System.out.println("*Page: " + i);
//					System.out.println("*TocLines");
//					Iterator<Line> it = getTocLines(page).iterator();
//					while(it.hasNext())
//						System.out.println("****" + it.next().getText());
//					System.exit(0);
//					System.out.println("---- adding toc lines: page: " + i);
//					/*TESTING*/
				//SystemLogger.getInstance().setDebug(true);
				toc.addAll(getTocLines(page));
				tocPages.add(i);
			}
		}		
		//Check if empty TOC: TOC without page numbers
		if(toc.size() == 0) {
			throw new TOCNotFoundException("Book have a Table of Content without page numbers");
		}
		
		if(StringOperations.similarity(toc.get(0).getText(), toc.get(1).getText()) > 0.7)			
			toc.remove(1);			
		
		toc.remove(0);
		
//		/*TESTING*/
//		System.out.println(">>>>>>>>>>>TocLines before LineConcat");
//		Iterator<Line> it = toc.iterator();
//		int i = 0;
//		while(it.hasNext()) {
//			Line l = it.next();
//			System.out.println("#" + i++ + ": " + l.getText() + " SX: " + l.getStartPositionX() + " fs: " + l.getFontSize());
////			l.mostFrequentStyleFeatures();
////			System.out.println("#" + i++ + ": " + l.getText() + " SA: " + l.getFCKeySum());
//			for(Text w: l.getWords() ) {
//				System.out.println("\t" + w.getText() + ": " + w.getFontSize());
//			}
//		}
//			
//		System.exit(0);
//		/*TESTING*/

		tocLineConcat(toc, lang);
		
//		/*TESTING*/	
//		SystemLogger.getInstance().debug(">>>>>>>>>>>TocLines AFTER LineConcat");
//		Iterator<Line>it = toc.iterator();
//		int i = 0;
//		while(it.hasNext()) {
//			Line n = it.next();
//			SystemLogger.getInstance().debug("#" + i++ + ": " + n.getText() + " X: " + n.getStartPositionX());
//		}
//		//System.exit(0);
//		/*TESTING*/

		return Pair.of(tocPages, convertToTOC(toc));
	}
	
	public static boolean checkTOCTitleLine(Page page, Line TOCTitleLine) {
		int countEqual = 0;
		for(int i = 0; i < page.size(); i++){
			if(page.getLineAt(i).size() == 0)
				continue;
			if(!page.getLineAt(i).equals(TOCTitleLine) && page.getLineAt(i).getWordAt(0).getFontSize() == TOCTitleLine.getWordAt(0).getFontSize() &&  page.getLineAt(i).getWordAt(0).getFontName().equals(TOCTitleLine.getWordAt(0).getFontName())
					&& (BoundSimilarity.isInBound( page.getLineAt(i).getStartPositionX(), TOCTitleLine.getStartPositionX(), page.getLineAt(i).getWordAt(0).getFontSize(), TOCTitleLine.getWordAt(0).getFontSize(), 0.9f )
					|| BoundSimilarity.isInBound( page.getLineAt(i).getEndPositionX(), TOCTitleLine.getEndPositionX(), page.getLineAt(i).getWordAt(0).getFontSize(), TOCTitleLine.getWordAt(0).getFontSize(), 0.9f )
					) && getRomanNumberAtTheEnd(page.getLineAt(i).getLastWordText().toUpperCase()) == null && getTheNumberAtTheEnd(page.getLineAt(i).getLastWordText().toUpperCase()) == null) {
				countEqual++;
			}
		}
		
		/*if(countEqual == 1) {
			return true;
		} else if(countEqual > 1 && page.size() > 1) {
			return false;
		}*/
		//System.out.println("^^ Checking tocLine: " + countEqual);
		if(countEqual >= 1) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 	
	 * @param page
	 * @return
	 */
	public static boolean checkTocLines(Page page, Line TOCTitleLine, float textBodyFontSize){

		int nullCount = 0;
		int numberCount = 0;
		int range = page.size();
		Integer sequencePageNumber = null;
		
		/*
		 * Filters:
		 * 1) There is no title of other section
		 * 2) Numbering at the end of lines is in order
		 * 3) Lines with numbers at the end should be more than 20% of all lines
		 */
		
		//CHECK if TOC structure belongs to other section like List of Figures...
		if(TOCTitleLine != null && checkTOCTitleLine(page, TOCTitleLine)) {
			return false;
		}
		
		//get the font size for the numbers and the position
		HashMap<Float,Integer> fontSizeStatistics = new HashMap<Float,Integer>();
		HashMap<Integer,Integer> XCoordinateStatistics = new HashMap<Integer,Integer>();
		for(int i = 0; i < range; i++){
			String lastWord = page.getLineAt(i).getLastWordText();
			Float lastWordFontSize = page.getLineAt(i).getLastWord().getFontSize();
			Integer lastWordCoordinate = Math.round(page.getLineAt(i).getLastWord().getEndPositionX());
			lastWord = getTheNumberAtTheEnd(lastWord);
			
			if((lastWord != null && i != range-1)) {
				//font size
				Integer nFontSizeStatistics =  fontSizeStatistics.get(lastWordFontSize);
				if(nFontSizeStatistics == null)
					nFontSizeStatistics = 0;
				nFontSizeStatistics++;
				fontSizeStatistics.put(lastWordFontSize, nFontSizeStatistics);
				//X Coordinates
				//Math.round
				Integer nXCoordinateStatistics =  XCoordinateStatistics.get(lastWordCoordinate);
				if(nXCoordinateStatistics == null)
					nXCoordinateStatistics = 0;
				nXCoordinateStatistics++;
				XCoordinateStatistics.put(lastWordCoordinate, nXCoordinateStatistics);
			}
		}

		Integer biggestCount = 0;
		Float biggestSize = 0.0f;
		Integer tempCount = 0;
		for(Float k: fontSizeStatistics.keySet()) {
			tempCount = fontSizeStatistics.get(k);
			if(tempCount > biggestCount) {
				biggestCount = tempCount;
				biggestSize = k;
			}

		}

		biggestCount = 0;
		tempCount = 0;
		Integer biggestXCoordiante = 0;
		for(Integer k: XCoordinateStatistics.keySet()) {
			tempCount = XCoordinateStatistics.get(k);
			if(tempCount > biggestCount) {
				biggestCount = tempCount;
				biggestXCoordiante = k;
			}

		}	
		
		//check that all lines ending with a number are in increasing order
		//not last line because of possible page number
		int sameNumber = 0;
		int biggestSameNumber = 0;
		for(int i = 0; i < range; i++){
			
			SystemLogger.getInstance().debug("line ------: " + page.getLineAt(i).getText());

			Text lastWord = page.getLineAt(i).getLastWord();
			String lastWordText = page.getLineAt(i).getLastWordText();
			if(lastWordText.toLowerCase().equals("e1") || lastWordText.toLowerCase().equals("e2")) {
				continue;
			}
			String lastWordRoman = getRomanNumberAtTheEnd(lastWordText.toUpperCase());
			lastWordText = getTheNumberAtTheEnd(lastWordText);
			
			if((lastWordText == null && i == range-1)) {
				continue;
			}
			
			if(lastWordText != null) {
				Integer currentPageNumber = Integer.parseInt(lastWordText);
				if(sequencePageNumber == null) {
					sequencePageNumber = currentPageNumber;
				} else {
					SystemLogger.getInstance().debug("comparing: " + sequencePageNumber + " --- " + currentPageNumber);
					if (sequencePageNumber < currentPageNumber  ) {
						sequencePageNumber = currentPageNumber;
						if(sameNumber > biggestSameNumber) {
							biggestSameNumber = sameNumber;
						}
						sameNumber = 0;
					} else if (sequencePageNumber.equals(currentPageNumber )) {
						sameNumber++;
					}else {
						SystemLogger.getInstance().debug (" before false : " + sequencePageNumber +  " - " + currentPageNumber);
						if((i == range-1) || lastWord.getFontSize() != biggestSize || !BoundSimilarity.isInBound(Math.round(lastWord.getEndPositionX()), biggestXCoordiante, biggestSize, biggestSize, 0.85f) ) {
							SystemLogger.getInstance().debug("i == range-1: " + (i == range-1));
							SystemLogger.getInstance().debug("lastWord.getFontSize() != biggestSize: " + (lastWord.getFontSize() != biggestSize));
							SystemLogger.getInstance().debug("!BoundSimilarity.isInBound(Math.round(lastWord.getEndPositionX()), biggestXCoordiante, biggestSize, biggestSize, 0.85f): " + (!BoundSimilarity.isInBound(Math.round(lastWord.getEndPositionX()), biggestXCoordiante, biggestSize, biggestSize, 0.85f)));
							continue;
						}
						SystemLogger.getInstance().debug(" false : no toc -------------");
						return false;
					}
				}
			}
			
			
//			/*TESTING*/
//			System.out.println("LINE: " + page.getLineAt(i).getText() + " --W: " + lastWord);
//			System.out.println("\t number at end: " + lastWord);
//			System.out.println("\t roman number at end: " + lastWordRoman);
//			/*TESTING*/
			
			if(lastWordText == null && lastWordRoman == null){
				nullCount++;
			}
			else{
				SystemLogger.getInstance().debug("# Ok Line: "+ page.getLineAt(i).getText());
				numberCount++;
			}
		}
		
		/*TESTING*/
		SystemLogger.getInstance().debug("null: "+ nullCount);
		SystemLogger.getInstance().debug("numberCount: "+ numberCount);
		/*TESTING*/
		
		if(sequencePageNumber == null) {
			return false;
		}
		if(nullCount > numberCount) {
			//More hard requirements:
//			//second check: difference is more than the 30% of numbered lines
//			int diff = nullCount - numberCount;
//			System.out.println("diff: "+ diff);
//			if(diff > (numberCount + (numberCount * 0.3))) {
//				/*if(sequencePageNumber != null && numberCount >= 2) {
//					System.out.println("PAGE TOC");
//					return true;
//				} else {
//					System.out.println("PAGE NO TOC");
//					return false;
//				}*/
//				System.out.println("PAGE NO TOC");
//				return false;		
//			} else {
//				System.out.println("PAGE TOC");
//				return true;
//			}	
			if(biggestSameNumber >= 4 && numberCount <= 7) {
				return false;
			}
			
			//More relax requirements:
			if(numberCount > (numberCount + nullCount) * 0.2){
				return true;
			} else {
				return false;
			}
		} else {
			return true;	
		}	
	}

	/**
	 * 	
	 * @param page
	 * @return
	 */
	public static boolean checkTocLinesOLD(Page page, Line TOCTitleLine){

		int nullCount = 0;
		int numberCount = 0;
		int range = 0;

		//if(page.size() < 15)
			range = page.size();
		//else
		//	range = 15; 

		for(int i = 0; i < range; i++){
			//CHECK if TOC structure belongs to other section like List of Figures...
			if(TOCTitleLine != null && !page.getLineAt(i).equals(TOCTitleLine) && page.getLineAt(i).getWordAt(0).getFontSize() == TOCTitleLine.getWordAt(0).getFontSize() &&  page.getLineAt(i).getWordAt(0).getFontName().equals(TOCTitleLine.getWordAt(0).getFontName())
					&& (BoundSimilarity.isInBound( page.getLineAt(i).getStartPositionX(), TOCTitleLine.getStartPositionX(), page.getLineAt(i).getWordAt(0).getFontSize(), TOCTitleLine.getWordAt(0).getFontSize(), 0.9f )
					|| BoundSimilarity.isInBound( page.getLineAt(i).getEndPositionX(), TOCTitleLine.getEndPositionX(), page.getLineAt(i).getWordAt(0).getFontSize(), TOCTitleLine.getWordAt(0).getFontSize(), 0.9f )
					)) {
				return false;
			}

			String lastWord = page.getLineAt(i).getLastWordText();
			String lastWordRoman = getRomanNumberAtTheEnd(lastWord.toUpperCase());
			lastWord = getTheNumberAtTheEnd(lastWord);
			
//			/*TESTING*/
//			System.out.println("LINE: " + page.getLineAt(i).getText() + " --W: " + lastWord);
//			System.out.println("\t number at end: " + lastWord);
//			System.out.println("\t roman number at end: " + lastWordRoman);
//			/*TESTING*/
			
			if(lastWord == null && lastWordRoman == null){
				nullCount++;
			}
			else{
				numberCount++;
			}
		}
		
		/*TESTING*/
		System.out.println("null: "+ nullCount);
		System.out.println("numberCount: "+ numberCount);
		/*TESTING*/
		
		if(nullCount > numberCount) {
			//second check: difference is more thatn the 30% of numbered lines
			int diff = nullCount - numberCount;
			System.out.println("diff: "+ diff);
			if(diff > (numberCount + (numberCount * 0.5))) {
				System.out.println("PAGE NO TOC");
				return false;
			} else {
				System.out.println("PAGE TOC");
				return true;
			}	
		} else {
			return true;	
		}	
	}

	/**
	 * 
	 * @param page
	 * @return
	 */
	private static ArrayList<Line> getTocLines(Page page){
		SystemLogger.getInstance().debug("TOC page: " + page.getPageIndex());
		
		ArrayList<Line> tableOfContents = new ArrayList<Line>();

		for(int i= 0; i < page.size(); i++){
			
			if(page.getLineAt(i).size() == 0) {
				continue;
			}
			
			SystemLogger.getInstance().debug("TOC line before cleaning: " + page.getLineAt(i).getText());
			tocInvalidCharCleaning(page.getLineAt(i));
			

			SystemLogger.getInstance().debug("TOC line AFTER cleaning: " + page.getLineAt(i).getText()+  " " + page.getLineAt(i).getFontSize());
			for(Text t:  page.getLineAt(i).getWords()) {
				SystemLogger.getInstance().debug("\t" + t.getText() + " " + t.getFontSize());
			}
			if(page.getLineAt(i).size() > 0) {
				dashFillFix(page.getLineAt(i));
			} else {
				continue;
			}
			
			SystemLogger.getInstance().debug("TOC line AFTER dashFIll: " + page.getLineAt(i).getText());
			tableOfContents.add(page.getLineAt(i));			
		}	
		
		return tableOfContents;
	}
	
	private static ArrayList<TOC> convertToTOC(ArrayList<Line> toc){
		
		ArrayList<TOC> resultTOC = new ArrayList<TOC>();
		
		//check if last line is a number
		if(toc.get(toc.size()-1).size() == 1 && (StringUtils.isNumeric(toc.get(toc.size()-1).getText()) || toc.get(toc.size()-1).getText().toUpperCase().matches(regexRomanNumber) )) {
			toc.remove(toc.size()-1);
		}
		
		for(int i = 0; i< toc.size(); i ++){
			SystemLogger.getInstance().debug("TOC: " + toc.get(i).getText());
			TOC temp = new TOC();
			
			if(toc.get(i).getText().lastIndexOf(" ") != -1) {
				int limit =0;
				if(StringUtils.isNumeric(toc.get(i).getText().substring(toc.get(i).getText().lastIndexOf(" ")+1))) {
					temp.setTitleText(toc.get(i).getText().substring(0 , toc.get(i).getText().lastIndexOf(" ")).toLowerCase());
					limit = toc.get(i).getWords().size() - 1;
				} else {
					temp.setTitleText(toc.get(i).getText());
					limit = toc.get(i).getWords().size();
				}
				List<Float> sizes = new ArrayList<Float>();
				for(int w = 0; w < limit; w++) {
					sizes.add(toc.get(i).getWordAt(w).getFontSize());
				}
				temp.setFontSize(ListOperations.findMostFrequentItem(sizes));
			} else {
				//continue;
				if((i+1) < toc.size() && toc.get(i).size() == 1) {
					toc.get(i+1).addWordAt(0, toc.get(i).getLastWord());
					toc.get(i+1).extractText();
					continue;
				}
			}
			
			try {
				temp.setPageNumber(Integer.parseInt(toc.get(i).getText().substring(toc.get(i).getText().lastIndexOf(" ")+1)));
			} catch (NumberFormatException e) {
				if(toc.get(i).getText().substring(toc.get(i).getText().lastIndexOf(" ")+1).toLowerCase().equals("e1")) {
					temp.setErratum(true);
				}
				temp.setPageNumber(0);
			}
			
			temp.setPosX(toc.get(i).getStartPositionX());
			temp.setPosY(toc.get(i).getPositionY());
			temp.setItalic(toc.get(i).isItalic());
			temp.setBold(toc.get(i).isBold());
			temp.setConcatenatedPosY(toc.get(i).getConcatenatedPositionY());
			temp.setHeight(toc.get(i).getLineHeight());
			
//			String prefix = toc.get(i).getWordAt(0).getText().replaceAll("[.]", "");
			String prefix = toc.get(i).getWordAt(0).getText();
			
			if(StringUtils.isNumeric(prefix)){
				
				temp.setChapterPrefix(prefix);
			}
			else{
				temp.setChapterPrefix("0");
			}
			
			resultTOC.add(temp);
		}
		
		//remove special bad recognized entries
		TOCCleanUp(resultTOC);

		
//		/*TESTING*/
//		for(TOC t: resultTOC) {
//			System.out.println(t.getTitleText() + " S: " + t.getSection() + " E: " + t.isErratum());
//		}
//		System.exit(0);
//		/*TESTING*/
		
		tableOfContentsType(resultTOC);
		
		//check last entry if it is empty
		if(resultTOC.get(resultTOC.size() -1).getPageNumber() == 0) {
			resultTOC.remove(resultTOC.size() -1);
		}
		
		return resultTOC;
	}

	public static void TOCCleanUp(ArrayList<TOC> resultTOC) {
		Iterator<TOC> it = resultTOC.iterator();
		while(it.hasNext()) {
			TOC entry = it.next();
			String text = entry.getTitleText();
			if(text.length() <= 3 && entry.getPageNumber() == 0 && !text.toUpperCase().matches(regexRomanNumber) && !text.toLowerCase().contains("part") && !text.toLowerCase().contains("section") ) {
				SystemLogger.getInstance().debug("CANDIDATE: " + text);
				it.remove();
			}
		}
	}
	
	public static void TOCMarkEntriesWithoutPageNumber(ArrayList<TOC> resultTOC) {
		
		for(int i=0; i < resultTOC.size(); i++) {
			TOC entry = resultTOC.get(i);
			if(entry.getPageNumber() == 0 && (i+1) < resultTOC.size() && resultTOC.get(i+1).getPageNumber() != 0 ) {
				//check pages
				System.out.println("checking additional: " + entry.getTitleText());
			}
		}
	}

	/**
	 * 
	 * @param lastWord
	 * @return
	 */
	public static String getTheNumberAtTheEnd(String lastWord){

		String letter = null;

		if(lastWord.length() > 0)
			for(int i = lastWord.length()-1; i >= 0 ; i--){

				if(!StringUtils.isNumeric((lastWord.substring(i))))
					break;

				letter = lastWord.substring(i);
			}

		return letter;
	}
	
	public static String getRomanNumberAtTheEnd(String lastWord){

		String letter = null;

		if(lastWord.length() > 0)
			for(int i = lastWord.length()-1; i >= 0 ; i--){

				if(!lastWord.substring(i).matches(regexRomanNumber)) {
					if(lastWord.substring(i).matches("[A-Z]+")) {
						return null;
					} else {
						break;
					}
				}
				letter = lastWord.substring(i);
			}

		return letter;
	}
	


	/**
	 * 
	 * @param line
	 */
	private static void tocInvalidCharCleaning(Line line){

		String title;

		for(int i = 0; i < line.size(); i++){
			
			title= line.getWordAt(i).getText();
			title = title.replaceAll("[?/|:*\"\\<>%(){}=∈]��", "");

			//A whitespace character || A non-word character
			if(title.matches("^[\\s|\\W]")){

				line.removeWordAt(i);
				i--;
				//SystemLogger.getInstance().log("^#1: ");
			}
			//
			else if(title.matches(".*[.][.]+")){

				title = title.replaceAll("[.][.]+", "");
				line.getWordAt(i).setText(title);
				//SystemLogger.getInstance().log("^#2: " + title);
			}
			//A word with dots follow by a number: Word......89
			else if(title.matches("(.*[.][.]+[0-9]+)|(.*[-][-]+[0-9]+)|(.*[–][–]+[0-9]+)")){
//				String word = title.substring(0, title.indexOf('.'));
//				String number = title.substring(title.lastIndexOf(".") + 1);
//				line.getWordAt(i).setText(word);
//				line.addWord(new Text(number));
//				SystemLogger.getInstance().log("^#3: ");
//				SystemLogger.getInstance().log(">>>word: " + word);
//				SystemLogger.getInstance().log(">>>number "+ number);
			//A word with a last dot
			} else if(title.matches("^.*[.]")) {
				//title = title.replaceAll("[.]", "");
				//line.getWordAt(i).setText(title);
				//SystemLogger.getInstance().log("^#4: " + title);
			
			} else
				line.getWordAt(i).setText(title);
		}

		line.extractText();
	}
	
	static Pair<Character, Integer> maxRepeating(String str) 
    { 
        int len = str.length(); 
        int count = 0; 
  
        // Find the maximum repeating character 
        // starting from str[i] 
        char res = str.charAt(0); 
        for (int i=0; i<len; i++) 
        { 
            int cur_count = 1; 
            for (int j=i+1; j<len; j++) 
            { 
                if (str.charAt(i) != str.charAt(j)) 
                    break; 
                cur_count++; 
            } 
  
            // Update result if required 
            if (cur_count > count) 
            { 
                count = cur_count; 
                res = str.charAt(i); 
            } 
        } 
        return Pair.of(res, count); 
    } 


	/**
	 * 
	 * @param line
	 */
	private static void dashFillFix(Line line){
		
		
		String text = line.getLastWordText();

		if(text.matches("(.*[.][.]+[0-9]+)|(.*[-][-]+[0-9]+)|(.*[–][–]+[0-9]+)|(.*[\\s][.\\s]+[\\s]*[0-9]+)|(.*[\\s][.\\s]+[\\s]*[IVXMivxm]+)|(.*[.][.]+[IVXMivxm]+)|(.*[-][-]+[IVXMivxm]+)")){
//			/*TESTING*/
//			SystemLogger.getInstance().log("1111 TOC dashFillFix line: " + line.getText());
//			SystemLogger.getInstance().log("************* BEFORE");
//			for(Text w: line.getWords()) {
//				SystemLogger.getInstance().log("T: " + w.getText() + " SX: " + w.getStartPositionX() + " EX: " + w.getEndPositionX() + " Y: " + w.getPositionY());
//			}
//			SystemLogger.getInstance().log("-----------");
//			/*TESTING*/

			Text lastWord = line.getWordAt(line.size() - 1);
			byte length = (byte) text.length();

			String temp1, temp2;
			text = text.replace(" ", "");
			temp1 = text.replaceFirst("[.]", " ");
			temp1 = temp1.replace(".", "");

			temp2 = temp1.substring(temp1.lastIndexOf(" ")+1).trim();
			temp1 = temp1.substring(0, temp1.lastIndexOf(" ")).trim();	
			
		
		
			//create new words
			float singleWidth = lastWord.getWidth() / text.length();
			Text tmp = new Text(temp1,lastWord.getFontSize(),lastWord.getStartPositionX() , lastWord.getPositionY(), line.getCoordinates()); 
			Text tmp2 = new Text(temp2,lastWord.getFontSize(),lastWord.getEndPositionX() - (temp2.length()*singleWidth), lastWord.getPositionY() , line.getCoordinates());
			tmp.setSpaceWidth(lastWord.getSpaceWidth());
			
			tmp2.setSpaceWidth(lastWord.getSpaceWidth());
			tmp.setWidth(temp1.length() * singleWidth);
			tmp.setEndPositionX(tmp2.getStartPositionX() - tmp2.getSpaceWidth());
			tmp.setBold(lastWord.isBold());
			tmp.setBold(lastWord.isItalic());
			tmp.setHeight(lastWord.getHeight());
			tmp.setFontName(lastWord.getFontName());
			tmp.setFontColor(lastWord.getFontColor());
			
			tmp2.setSpaceWidth(lastWord.getSpaceWidth());
			tmp2.setWidth(temp2.length() * singleWidth);
			tmp2.setEndPositionX(lastWord.getEndPositionX());
			tmp2.setBold(lastWord.isBold());
			tmp2.setBold(lastWord.isItalic());
			tmp2.setHeight(lastWord.getHeight());
			tmp2.setFontName(lastWord.getFontName());
			tmp.setFontColor(lastWord.getFontColor());
			
//			/*TESTING*/
//			SystemLogger.getInstance().log("> temp1 original: " + temp1 + " size: " + temp1.length());
//			SystemLogger.getInstance().log("> temp2 original: " + temp2 + " size: " + temp2.length());
//			SystemLogger.getInstance().log("> before:  " +line.getEndPositionX());
//			SystemLogger.getInstance().log("> W1: " +tmp.getText() + " S: " + tmp.getStartPositionX() + " End: " + tmp.getEndPositionX());
//			SystemLogger.getInstance().log("> W1: " +tmp2.getText() + " S: " + tmp2.getStartPositionX() + " End: " + tmp2.getEndPositionX());
//			/*TESTING*/

			//update line
			int in = line.size() - 1;
			line.removeWordAt(in);
			if(temp2.length() != 0)
				line.addWordAt(in, tmp2);
			if(temp1.length() != 0)
				line.addWordAt(in, tmp);		
			
			line.extractText();	
			
//			/*TESTING*/
//			SystemLogger.getInstance().log("************* AFTER");
//			for(Text w: line.getWords()) {
//				SystemLogger.getInstance().log("T: " + w.getText() + " SX: " + w.getStartPositionX() + " EX: " + w.getEndPositionX() + " Y: " + w.getPositionY() + " FS: " + w.getFontSize());
//			}
//			SystemLogger.getInstance().log("-----------");
//			/*TESTING*/
			//System.exit(0);
		} else {
			
//			/*TESTING*/
//			SystemLogger.getInstance().log("222 TOC dashFillFix line ##############: " + line.getText());
//			SystemLogger.getInstance().log("************* BEFORE");
//			for(Text w: line.getWords()) {
//				SystemLogger.getInstance().log("T: " + w.getText() + " SX: " + w.getStartPositionX() + " EX: " + w.getEndPositionX() + " Y: " + w.getPositionY());
//			}
//			SystemLogger.getInstance().log("-----------");
//			/*TESTING*/
			
			String lineText = line.getText();
			if(lineText != null) {
				Pair<Character, Integer> res = maxRepeating(line.getText());
				char mostUsedChar = res.getLeft();
				int amount = res.getRight();
				if(!((mostUsedChar >= 48 && mostUsedChar <= 57) || (mostUsedChar >= 65 && mostUsedChar <= 90) || (mostUsedChar >= 97 && mostUsedChar <= 122))) {
					if(amount > 5) {
						String patron = "";
						for(int i = 0; i < amount; i++) {
							patron+=mostUsedChar;
						}
						//SystemLogger.getInstance().log("patron: " + patron);
						//SystemLogger.getInstance().log("line.getText().contains(patron): " + line.getText().contains(patron));
						if(line.getText().contains(patron)) {
							for(int in = 0; in < line.size(); in++) {
								Text w = line.getWordAt(in);
								if(w.getText().equals(patron)) {
									line.removeWordAt(in);
									break;
								} else if(w.getText().contains(patron)) {
									String temp1, temp2;
									Text lastWord = w;
									
									//int end1W = w.getText().indexOf(patron);
									//int start2W = end1W + patron.length();

									temp1 = w.getText().replace(patron, " ");
									temp2 = temp1.substring(temp1.lastIndexOf(" ")+1).trim();	
									temp1 = temp1.substring(0, temp1.lastIndexOf(" ")).trim();
			
									float singleWidth = lastWord.getWidth() / text.length();
									Text tmp = new Text(temp1,lastWord.getFontSize(),lastWord.getStartPositionX() , lastWord.getPositionY(), line.getCoordinates()); 
									Text tmp2 = new Text(temp2,lastWord.getFontSize(),lastWord.getEndPositionX() - (temp2.length()*singleWidth), lastWord.getPositionY() , line.getCoordinates());
									tmp.setSpaceWidth(lastWord.getSpaceWidth());
									
									tmp.setSpaceWidth(lastWord.getSpaceWidth());
									tmp.setWidth(temp1.length() * singleWidth);
									tmp.setEndPositionX(tmp2.getStartPositionX() - tmp2.getSpaceWidth());
									tmp.setBold(lastWord.isBold());
									tmp.setBold(lastWord.isItalic());
									tmp.setHeight(lastWord.getHeight());
									tmp.setFontName(lastWord.getFontName());
									tmp.setFontColor(lastWord.getFontColor());
									
									tmp2.setSpaceWidth(lastWord.getSpaceWidth());
									tmp2.setWidth(temp2.length() * singleWidth);
									tmp2.setEndPositionX(lastWord.getEndPositionX());
									tmp2.setBold(lastWord.isBold());
									tmp2.setBold(lastWord.isItalic());
									tmp2.setHeight(lastWord.getHeight());
									tmp2.setFontName(lastWord.getFontName());
									tmp2.setFontColor(lastWord.getFontColor());

									line.removeWordAt(in);
									
									if(temp2.length() != 0)
										line.addWordAt(in, tmp2);
									if(temp1.length() != 0)
										line.addWordAt(in, tmp);
									break;
								}
								
							}
							line.extractText();
						}
					
					}
				}
			}
		}
		
	}
	
	private static boolean lineIsIncomplete(Line line, HashMap<Float, Integer> positions) {
		//SystemLogger.getInstance().debug("TESTING line for incomplete: "+ line.getText() + " LW: " + line.getWordAt(line.size()-1).getText());
		Text lastWord = line.getWordAt(line.size()-1);
		//if it does not end in number: incomplete
		if (!StringUtils.isNumeric(lastWord.getText())){
			return true;
		//if it does end in number: check further
		} else if(positions == null) {
			if(line.size() >= 2) {
				Text beforeWord = line.getWordAt(line.size()-2);
				if (beforeWord.getText().equals("and") || beforeWord.getText().equals("or") || beforeWord.getText().equals("of") || beforeWord.getText().equals(",") || beforeWord.getText().equals(":")) {
					return true;
				} 
			}
			return false;
		} else {
			
			Integer positionToCompare = 9999;
			if(positions.get(line.getWordAt(0).getFontSize()) != null) {
				positionToCompare = positions.get(line.getWordAt(0).getFontSize());
			} else if (positions.get(line.getLastWord().getFontSize()) != null) {
				positionToCompare = positions.get(line.getLastWord().getFontSize());
			}
			
//			/*TESTING*/
//			System.out.println("TESTING: "+ line.getText() + " X: " +  line.getStartPositionX());
//			System.out.println("LW: "+ lastWord.getText() + " -EPX: "+ lastWord.getEndPositionX() + " --PO: " + positions.get(line.getWordAt(0).getFontSize()));
//			System.out.println("positionToCompare: "+ positionToCompare);
//			System.out.println("Math.round(lastWord.getEndPositionX()) : "+ Math.round(lastWord.getEndPositionX()) );
//			System.out.println("Math.round(lastWord.getEndPositionX()) >= positionToCompare: "+ (Math.round(lastWord.getEndPositionX()) >= positionToCompare));
//			/*TESTING*/
			
			if (Math.round(lastWord.getEndPositionX()) >= (positionToCompare - 5) || (int) lastWord.getEndPositionX() == positionToCompare ){
				//System.out.println("1 lineisincomplete false: "+ line.getText());
				return false;
			} else {
				return true;
			}	
		}
	}
	
	private static boolean lineIsIncompleteLineCapacity(Line line, float maxWordsBoundary) {
		float start = line.getWordAt(0).getStartPositionX();
		float last = line.getWordAt(line.size()-1).getEndPositionX();
		float topBoundaryNormalized = maxWordsBoundary - start;
		float lineLimitNormalized = last - start;
		float lineLimiteP = lineLimitNormalized / topBoundaryNormalized;
		System.out.println("start:  " + start);
		System.out.println("last:  " + last);
		System.out.println("lineLimiteP: " + lineLimiteP);
		System.out.println("LLLLL: " + line.getText()  + " V: " + lineLimiteP);
		if(lineLimiteP <= 0.6) {
			return false;
		} else {
			return true;
		}
	}
	
	private static HashMap<Float, Integer> preParseLinesToGetPositionOfNumbers(ArrayList<Line> toc) {
		HashMap<Float,HashMap<Integer,Integer>> count = new HashMap<Float,HashMap<Integer,Integer>>();
		HashMap<Float, Integer> finalMap = new HashMap<Float, Integer>();
		//1 count frequency
		for(short i = 0 ; i<toc.size() ; i++) {
			//System.out.println("line: " + toc.get(i).getText());
			//System.out.println("last word: " + toc.get(i).getWordAt(toc.get(i).size()-1).getText());
			//System.out.println("last word p: " + toc.get(i).getWordAt(toc.get(i).size()-1).getEndPositionX());
			if (StringUtils.isNumeric(toc.get(i).getWordAt(toc.get(i).size()-1).getText())) {
				Float fontSize = toc.get(i).getWordAt(toc.get(i).size()-1).getFontSize();
				Integer positionX = Math.round(toc.get(i).getWordAt(toc.get(i).size()-1).getEndPositionX());
				HashMap<Integer,Integer> tempMap = count.get(fontSize);
				if(tempMap == null) {
					tempMap = new HashMap<Integer,Integer>();
					count.put(fontSize, tempMap);
				}
				Integer cant = tempMap.get(positionX);
				if (cant == null) {
					tempMap.put(positionX, 1);
				} else {
					tempMap.put(positionX, ++cant);
				}
			}	
		}
		
//		/*TESTING*/
//		SystemLogger.getInstance().log("----FREQUENCIES-----");
//		for(Float fontSize : count.keySet()) {
//			HashMap<Integer,Integer> frequency = count.get(fontSize);
//			SystemLogger.getInstance().log(">>> FontSize: " + fontSize);
//			for(Integer positionX : frequency.keySet()) {
//				Integer tempCount = frequency.get(positionX);
//				SystemLogger.getInstance().log(">>>>> Position: " + positionX + " Count: " + tempCount);
//			}
//		}
//		/*TESTING*/	
		
		//2 get biggest frequency for each font size
		boolean isValid = true;
		for(Float fontSize : count.keySet()) {
			HashMap<Integer,Integer> frequency = count.get(fontSize);
			Integer biggestCount = 0;
			Integer biggestPosition = 0;
			Integer allCount = 0;
			for(Integer positionX : frequency.keySet()) {
				Integer tempCount = frequency.get(positionX);
				allCount += tempCount;
				if(tempCount > biggestCount) {
					biggestCount = tempCount;
					biggestPosition = positionX;
				}
			}
			//System.out.println("biggestCount: " + biggestCount);
			//System.out.println("allCount: " + allCount);
			if(biggestCount < (allCount / 2)) {
				isValid = false;
				break;
			}
			finalMap.put(fontSize, biggestPosition);
		}
		
		if(!isValid) {
			//System.out.println("INVALID PositionOfNumbers");
			return null;
		}
		
//		/*TESTING*/
//		SystemLogger.getInstance().log("----FINAL MAP-----");
//		for (Float key : finalMap.keySet()) {
//			SystemLogger.getInstance().log("K: " + key + " V: " + finalMap.get(key));
//		}
//		System.exit(0);
//		/*TESTING*/
		
		return finalMap;
	}
	
	public static boolean areWordsAligned(Line line1, Line line2) {
		int posX1 = 0;
		int posX2 = 0;
		for(Text w: line1.getWords()) {
			if(!StringUtils.isNumeric(w.getText().replaceAll("[.]", ""))) {
				posX1 = (int) w.getStartPositionX();
				//System.out.println("x1: " + w.getText());
				break;
			}
		}
		for(Text w: line2.getWords()) {
			if(!StringUtils.isNumeric(w.getText().replaceAll("[.]", ""))) {
				posX2 = (int) w.getStartPositionX();
				//System.out.println("x2: " + w.getText());
				break;
			}
		}
		
//		/*TESTING*/
//		System.out.println("posx1: " + posX1);
//		System.out.println("posx2: " + posX2);
//		System.out.println("posX1 != 0: " + (posX1 != 0));
//		System.out.println("line1.getFontSize() == line2.getFontSize(): " + (line1.getFontSize() == line2.getFontSize()));
//		System.out.println("line1.isBold() == line2.isBold(: " +(line1.isBold() == line2.isBold()));
//		System.out.println("(posX1 == posX2 || (posX1+1) == posX2 || (posX1-1) == posX2): " + ((posX1 == posX2 || (posX1+1) == posX2 || (posX1-1) == posX2)));
//		System.out.println("posx2: " + posX2);
//		/*TESTING*/
		
		if(posX1 != 0 && line1.getFontSize() == line2.getFontSize() && line1.isBold() == line2.isBold() && line1.isItalic() == line2.isItalic() && (posX1 == posX2 || (posX1+1) == posX2 || (posX1-1) == posX2) &&
				Math.round(line1.getStartPositionX()) != Math.round(line2.getStartPositionX()) ) {
			//System.out.println("areWordsAligned: true");
			return true;
		} else {
			//System.out.println("areWordsAligned: false");
			return false;
		}
		
		
	}
	
	private static boolean isListOfNames(String text, LanguageEnum lang) {
		String replaceWord = WordListCheck.getAndListWord(lang);
		//remove word that are part of a list of names
		text = text.replaceAll("\\b"+ replaceWord + "\\b", ""); 
		text = text.replaceAll("\\b"+ replaceWord.toUpperCase() + "\\b", ""); 
		text = text.replaceAll(",", ""); 
		text = text.replaceAll(";", ""); 
		
		//NER tag of the line of text
		List<String> nerTags = NERPersonTagger.getInstance(lang).getNerTags(text);
		
		//List of NER tags for PERSON
		List<String> tags = NERPersonTagger.getPersonLabels();
		
		int cant = 0;
		for(String tag: nerTags) {
			if(tags.contains(tag)) {
				cant++;
			}
		}
		
		if((cant/(double) nerTags.size()) >= 0.8) {
			return true;
		} else {
			return false;
		}
	}

	private static boolean linesShareStyle(Line line1, Line line2, boolean omitLastWordLine1, boolean omitLastWordLine2) {
		int endLine1 = line1.getWords().size();
		if(omitLastWordLine1) {
			endLine1--;
		}
		int endLine2 = line2.getWords().size();
		if(omitLastWordLine2) {
			endLine2--;
		}
		for(int i=0; i < endLine1; i++) {
			Text word1 = line1.getWordAt(i);
			for(int j=0; j < endLine2; j++) {
				Text word2 = line2.getWordAt(j);
				if(word1.getFormattingContainerKeySum().equals(word2.getFormattingContainerKeySum())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 *Check for a single entry of TOC in multiple lines, at the end each line represents a TOC entry + SECTION detection
	 *FIXED @i.alpizarchacon 
	 * @param toc
	 */
	private static void tocLineConcat(ArrayList<Line> toc, LanguageEnum lang){
		
		//1. Get statistics about the line spacing, but subtracting the Y coordinates between lines
		lineSpacingStatistics = new HashMap<Integer, Integer>();
		Integer lastYCoordinate = Math.round(toc.get(0).getPositionY());
		Integer diff = 0;
		Integer amount = 0;
		for(int i = 1; i < toc.size(); i++) {
			Integer actualYCoordinate = Math.round(toc.get(i).getPositionY());
			if(actualYCoordinate > lastYCoordinate) {
				diff = actualYCoordinate - lastYCoordinate;
				amount = lineSpacingStatistics.get(diff);
				if(amount == null) {
					amount = 0;
				}
				amount++;
				lineSpacingStatistics.put(diff, amount);
			}
			lastYCoordinate = actualYCoordinate;
		}
		
		//2. Get the biggest line spacing, and the most used line spacing
		Pair<Number,Number> p = GenericStatisticsMapHandle.getBiggestKeys(lineSpacingStatistics);
		Integer fontBiggetsSpace = p.getLeft().intValue();
		Integer fontMostUsedSpace = p.getRight().intValue();
		
		Number smallestSpace = GenericStatisticsMapHandle.getSmallestKey(lineSpacingStatistics);
		
		// Get the position of the number pages in the TOC
		HashMap<Float, Integer> endPositions = preParseLinesToGetPositionOfNumbers(toc);
		
		// Calculate the in-line word boundary: to detect incomplete lines
		Float maxWordsBoundary = 0.f;
		for(int i = 0; i < toc.size(); i++) {
			int j = 0;
			if(lineIsIncomplete(toc.get(i), endPositions)) {
				j = toc.get(i).size()-1;
			} else {
				j = toc.get(i).size()-2;
			}
			if( toc.get(i).getWordAt(j).getEndPositionX() > maxWordsBoundary) {
				maxWordsBoundary = toc.get(i).getWordAt(j).getEndPositionX();
			}
		}
		
		
//		/*TESTING*/
//		System.out.println("**************** STATS");
//		for(Integer k: lineSpacingStatistics.keySet()) {
//			System.out.println("K: " + k + " / amount: " + lineSpacingStatistics.get(k));
//		}
//		System.out.println("fontBiggetsSpace: " + fontBiggetsSpace);
//		System.out.println("fontMostUsedSpace: " + fontMostUsedSpace);
//		/*TESTING*/
		
		//two lines with the same format, together, and both with page numbers
		HashMap<Integer, List<Integer>> lineDiff = new HashMap<Integer, List<Integer>>();
		for(short i = 0 ; i<toc.size()-1 ; i++) {
			SystemLogger.getInstance().debug("lineDiff *****************");
			if(linesShareStyle(toc.get(i), toc.get(i+1), true, true)) {
				if(!lineIsIncomplete(toc.get(i), endPositions) && !lineIsIncomplete(toc.get(i+1), endPositions)) {
					SystemLogger.getInstance().debug("l " +toc.get(i).getText() + " k: " + toc.get(i).getFCKeySum() + " y:" + toc.get(i).getPositionY());
					SystemLogger.getInstance().debug("l2 " +toc.get(i+1).getText() + " k: " + toc.get(i+1).getFCKeySum() + " y:" + toc.get(i+1).getPositionY());
					SystemLogger.getInstance().debug("diff " + (toc.get(i+1).getPositionY() - toc.get(i).getPositionY()));
					List<Integer> diffs = lineDiff.get(toc.get(i).getFCKeySum());
					if(diffs == null)
						diffs = new ArrayList<Integer>();
					diffs.add(Math.round((toc.get(i+1).getPositionY() - toc.get(i).getPositionY())));
					lineDiff.put(toc.get(i).getFCKeySum(), diffs);
				}
			}
		}
		
		//two lines with the same format, together, but the first one without page number, and the second with page number
		HashMap<Integer, List<Integer>> lineDiffTogether = new HashMap<Integer, List<Integer>>();
		for(short i = 0 ; i<toc.size()-1 ; i++) {
			SystemLogger.getInstance().debug("lineDiff 2 *****************");
			//toc.get(i).mostFrequentStyleFeatures();
			//toc.get(i+1).mostFrequentStyleFeatures();
			
			//first line incomplete, second complete
			if(toc.get(i).getFCKeySum().equals(toc.get(i+1).getFCKeySum()) && toc.get(i).getPositionY() < toc.get(i+1).getPositionY()
					&& lineIsIncomplete(toc.get(i), endPositions) && !lineIsIncomplete(toc.get(i+1), endPositions)) {
				SystemLogger.getInstance().debug("l " +toc.get(i).getText() + " k: " + toc.get(i).getFCKeySum() + " y:" + toc.get(i).getPositionY());
				SystemLogger.getInstance().debug("l2 " +toc.get(i+1).getText() + " k: " + toc.get(i+1).getFCKeySum() + " y:" + toc.get(i+1).getPositionY());
				SystemLogger.getInstance().debug("diff " + (toc.get(i+1).getPositionY() - toc.get(i).getPositionY()));
				List<Integer> diffs = lineDiffTogether.get(toc.get(i).getFCKeySum());
				if(diffs == null)
					diffs = new ArrayList<Integer>();
				diffs.add(Math.round((toc.get(i+1).getPositionY() - toc.get(i).getPositionY())));
				lineDiffTogether.put(toc.get(i).getFCKeySum(), diffs);
			}
		}
		
		/*TESTING*/
		SystemLogger.getInstance().debug(">>>>>>>>> lineDiff 1");
		for(Integer k: lineDiff.keySet()) {
			SystemLogger.getInstance().debug("K: " + k + " v: " +lineDiff.get(k));
		}
		SystemLogger.getInstance().debug(">>>>>>>>>>> lineDiff 2");
		for(Integer k: lineDiffTogether.keySet()) {
			SystemLogger.getInstance().debug("K: " + k + " v: " +lineDiffTogether.get(k));
		}
		//System.exit(0);
		/*TESTING*/
		
		//first: delete all lines with Roman numbers: after normal number, stop
		for(short i = 0 ; i<toc.size()-1 ; i++) {
			if(StringUtils.isNumeric(toc.get(i).getWordAt(toc.get(i).size()-1).getText())){
				break;
			}
			StringTokenizer tokenizer = new StringTokenizer(toc.get(i).getWordAt(toc.get(i).size()-1).getText());
			while (tokenizer.hasMoreElements()) {
				String tok = tokenizer.nextToken();
				if(!tokenizer.hasMoreElements()) {
					if(tok.toUpperCase().trim().matches(regexRomanNumber)) {
						toc.remove(i);
						i--;
						break;
					}
				}
		    }
		}
		
		
		//second: concat lines
		NEXTLINE:
		for(short i = 0 ; i<toc.size()-1 ; i++) {		
			//Case1: working when the TOC entry is in two or more lines + sections
			if(lineIsIncomplete(toc.get(i), endPositions)) { 
				SystemLogger.getInstance().debug("* l1: "+ toc.get(i).getText() + "S: "+ toc.get(i).getFCKeySum() + " Y: " + toc.get(i).getPositionY());
				boolean next = true;
				int lastLineXCoordinate =  Math.round(toc.get(i).getStartPositionX());
				int lastLineYCoordinate = Math.round(toc.get(i).getPositionY());
				while(next && (i+1) < toc.size()) {
					SystemLogger.getInstance().debug("* l2: "+ toc.get(i+1).getText() +  "S: "+ toc.get(i+1).getFCKeySum() + " Y: " + toc.get(i+1).getPositionY());
					
					//data needed to validate the cases
					int diffXCoordinate = Math.round(toc.get(i+1).getStartPositionX()) - lastLineXCoordinate;
					int diffYCoordinate = Math.round(toc.get(i+1).getPositionY()) - lastLineYCoordinate;
					
					int diffYCoordinatePredecessor = 0;
					if(i > 0 && toc.get(i-1).getConcatenatedPositionY() == -1) {
						diffYCoordinatePredecessor = Math.round(toc.get(i).getPositionY() - toc.get(i-1).getPositionY());
					} else if (i > 0) {
						diffYCoordinatePredecessor = Math.round(toc.get(i).getPositionY() - toc.get(i-1).getConcatenatedPositionY());
					} 
					//int diffYCoordinatePredecessor = i > 0 ? Math.round(toc.get(i).getPositionY() - previouspositionY) : 0;
					
					SystemLogger.getInstance().debug("?? PART: " + toc.get(i).getText());
					SystemLogger.getInstance().debug("tdiffYCoordinatePredecessor >= (fontBiggetsSpace - 1)) : " + (diffYCoordinatePredecessor >= (fontBiggetsSpace - 1)));
					SystemLogger.getInstance().debug("\tdiffYCoordinatePredecessor: " + diffYCoordinatePredecessor);
					SystemLogger.getInstance().debug("\tfontBiggetsSpace - 1) : " + (fontBiggetsSpace - 1));
					SystemLogger.getInstance().debug("\t(i == 0 || toc.get(i).getPositionY() < toc.get(i-1).getPositionY())) : " + (i == 0 || toc.get(i).getPositionY() < toc.get(i-1).getPositionY()));
					SystemLogger.getInstance().debug("\t(i == 0  : " + (i == 0));
					SystemLogger.getInstance().debug("\ttoc.get(i).getPositionY() : " + (toc.get(i).getPositionY()));
					SystemLogger.getInstance().debug("\ttoc.get(i-1).getPositionY())) : " + (i == 0 ? "NA" : ( toc.get(i-1).getPositionY())));
					SystemLogger.getInstance().debug("\ttoc.get(i).getPositionY() : " + (toc.get(i).getPositionY()));
					SystemLogger.getInstance().debug("\ttoc.get(i+1).getPositionY())) : " + (i == 0 ? "NA" : ( toc.get(i+1).getPositionY())));
					SystemLogger.getInstance().debug("\t!BoundSimilarity.isInYBound(toc.get(i), toc.get(i+1), lineDiff): " + !BoundSimilarity.isInYBound(toc.get(i), toc.get(i+1), lineDiff,lineDiffTogether, lineIsIncompleteLineCapacity(toc.get(i), maxWordsBoundary)));
					
					//1: check if the line is not "close" to the following line, and it is a list of names -> authors list, and should be removed
					/*
					 * check 1: if the second line is not align in a hierarchical way, it has a smaller X-coordinate
					 * check 2: if the second line is not close enough of the first line to be consider a extension of the line, they Y-coodinates diferences is bigger that the average
					 * check 3: if next line is complete
					 * check 4: if the line is a list of names 
					 */
					if(((diffXCoordinate < -1) || (diffYCoordinate > (fontMostUsedSpace + 1)) || !lineIsIncomplete(toc.get(i+1), endPositions)) && isListOfNames(toc.get(i).getText(), lang)) {
						SystemLogger.getInstance().debug("AUTHORS: " + toc.get(i).getText());
						toc.remove(i);
						i--;
						continue NEXTLINE;
					}
					//2: check that the line is a list of names and the next line has a different formatting -> authors list, and should be removed
					else if(isListOfNames(toc.get(i).getText(), lang) && toc.get(i).getFCKeySum() != toc.get(i+1).getFCKeySum()) {
						SystemLogger.getInstance().debug("AUTHORS: " + toc.get(i).getText());
						toc.remove(i);
						i--;
						continue NEXTLINE;
					}
					//3: check if previous line is far away from current line, and next line is too far way -> TOC part, do not concatenate
					/*
					 * check 1: is not the first line, and the Y-coordinates between the line and its predecessor is bigger that the space line used for the biggest font
					 * check 2: or it is the first line or it is at the beginning of a page
					 * check 3: the current line and the next one are too far away to be consider a extension of the line
					 */
					else if (((i > 0 && diffYCoordinatePredecessor >= (fontBiggetsSpace - 1)) || (i == 0 || toc.get(i).getPositionY() < toc.get(i-1).getPositionY())) && !BoundSimilarity.isInYBound(toc.get(i), toc.get(i+1), lineDiff, lineDiffTogether, lineIsIncompleteLineCapacity(toc.get(i), maxWordsBoundary))) {
						SystemLogger.getInstance().debug("#### PART: " + toc.get(i).getText());
						//condition removed, maybe add it later: if(!areWordsAligned(toc.get(i), toc.get(i+1))) {\
						continue NEXTLINE;
					} else {
						//Next line is complete:
						if(!lineIsIncomplete(toc.get(i+1), endPositions)) {
							next = false;
							//if next line is too far away, don't concatenate: Parts of TOC
							if(diffYCoordinate >= (fontBiggetsSpace - 1) ) {
								System.out.println("diffYCoordinate: " + diffYCoordinate);
								System.out.println(" (fontBiggetsSpace - 1): " +  (fontBiggetsSpace - 1));
								System.out.println("Reson 1");
								break;
							}
						}
						//concat the two lines: i + (i+1)
						toc.get(i).addWords(toc.get(i+1).getWords());
						lastLineYCoordinate = Math.round(toc.get(i+1).getPositionY());
						toc.get(i).extractText();
						toc.get(i).setConcatenatedPositionY(toc.get(i+1).getPositionY());
						toc.remove(i+1);
					}
				}	
			}
//			else if(i > 0){
//				
//				float currentlineX = toc.get(i).getWordAt(toc.get(i).size()-1).getStartPositionX();
//				float priorlineX = toc.get(i-1).getWordAt(toc.get(i-1).size()-1).getStartPositionX();
//				float rangeSetter1 = toc.get(i).getWordAt(toc.get(i).size()-1).getFontSize();
//				float rangeSetter2 = toc.get(i-1).getWordAt(toc.get(i-1).size()-1).getFontSize();
//				//case 2: before: was concatenating the last TOC entry of a page wit the first TOC entry of next page
//				//after: second validation-> if current line does not end in a number, but the next one does, concatenate
//				if(!BoundSimilarity.isInBound(currentlineX, priorlineX, rangeSetter1, rangeSetter2, 0.9f)){
//
//					boolean currentLineIncomplete = lineIsIncomplete(toc.get(i), endPositions);
//					boolean nextLineComplete = toc.get(i+1) != null ? !lineIsIncomplete(toc.get(i+1), endPositions) : false;
//					
//					if(currentLineIncomplete && nextLineComplete) {
//					
////						/*TESTING*/
////						System.out.println("$Concat");
////						System.out.println("$$Case 2: " + toc.get(i).getText());
//////						System.out.println("$$currentlineX: " + currentlineX);
//////						System.out.println("$$priorlineX: " + priorlineX);
//////						System.out.println("$$rangeSetter1: " + rangeSetter1);
//////						System.out.println("$$rangeSetter2: " + rangeSetter2);
////						/*TESTING*/
//							
//					toc.get(i).addWords(toc.get(i+1).getWords());
//
//					toc.get(i).extractText();
//					
////						/*TESTING*/
////						System.out.println("$$After: " + toc.get(i).getText());
////						/*TESTING*/
//
//					toc.remove(i+1);
//					}
//				}
//			}
		}
		
		//toc line content incomplete
		ListIterator<Line> listIterator = toc.listIterator(toc.size());
		while(listIterator.hasPrevious()){
			Line line = listIterator.previous();
			if(lineIsIncomplete(line, endPositions)) {
				listIterator.remove();
			} else {
				break;
			}
		}
	}



	/**
	 * 
	 * @param toc
	 */
	public static void tableOfContentsType(ArrayList<TOC> toc){

		List<String> chapterNumbering = new ArrayList<String> ();

		/**
		 * 
		 * Is table of Contents Flat or Indented 
		 *
		 **/

		byte range = (byte) ((toc.size()/2<10) ? toc.size() : 10);

		float startPosX = toc.get(0).getPosX();
		float fontSize = toc.get(0).getFontSize();

		tocType type = null;

		for(byte i=1; i<range; i++){

			if(!BoundSimilarity.isInBound(toc.get(i).getPosX(), startPosX, toc.get(i).getFontSize(), fontSize, 1)){
				type = tocType.INDENTED;
				break;
			}

			if(i> range/2)
				type = tocType.FLAT;
		}

		if(type == tocType.FLAT){		

			Pattern MY_PATTERN = Pattern.compile("([0-9]+[.|-]*)+");

			Matcher m;

			for(byte i=0; i<range; i++){

				m = MY_PATTERN.matcher(toc.get(i).getChapterPrefix());

				if(m.find())
					chapterNumbering.add(m.group(0));
			}

			if(chapterNumbering != null)
				for(byte i =1; i< chapterNumbering.size(); i++)
					if(chapterNumbering.get(i).length() > chapterNumbering.get(i-1).length()
							&& chapterNumbering.get(i).contains(chapterNumbering.get(i-1))){

						type = tocType.ORDERED;
					}	
		}

		if(type == tocType.ORDERED){
			setType(tocType.ORDERED);
		
			for(int i = 0; i< toc.size(); i ++){
				
				String prefix = toc.get(i).getChapterPrefix().replaceAll("[.]", "");
				
				if(StringUtils.isNumeric(prefix)){
					
					toc.get(i).setChapterPrefix(prefix);
				}
				else{
					toc.get(i).setChapterPrefix("0");
				}				
			}		
		}
		else if(type == tocType.INDENTED)
			setType(tocType.INDENTED);		
		else if(type == tocType.FLAT)
			setType(tocType.FLAT);
		
		SystemLogger.getInstance().debug("tableOfContentsType: " + type);
	}
	
	// use getTheNumberAtTheEnd
	@Deprecated
	public static Short getPageNumberFromLineTOC(String line) {
		String number = "";
		for(int i = line.length() - 1; i >= 0; i--) {
			String c = line.substring(i, i+1);
			if(StringUtils.isNumeric(c)) {
				number = c + number;
			} else {
				break;
			}
		}
		
		if(!number.equals("")) {
			return Short.parseShort(number);
		} else {
			return null;
		}
	}


	public static tocType getType() {
		return type;
	}

	public static void setType(tocType typ) {
		type = typ;
	}
}
