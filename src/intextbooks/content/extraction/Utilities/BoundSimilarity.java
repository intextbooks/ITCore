package intextbooks.content.extraction.Utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import org.apache.commons.lang3.tuple.Pair;

import edu.stanford.nlp.util.StringUtils;
import intextbooks.SystemLogger;
import intextbooks.content.extraction.buildingBlocks.format.Line;
import intextbooks.content.extraction.buildingBlocks.format.Page;
import intextbooks.content.extraction.buildingBlocks.format.Text;
import intextbooks.content.extraction.buildingBlocks.structure.TOC;
import intextbooks.content.extraction.format.FormatExtractor;
import intextbooks.content.extraction.structure.TableOfContentsExtractor;
import intextbooks.tools.utility.ListOperations;

public class BoundSimilarity {
	
	public static boolean isInYBound(Line line1, Line line2, double boundRangePecentage) {
		double calculatedNextY = line1.getLineHeight() + line2.getLineHeight();
		calculatedNextY = calculatedNextY + (calculatedNextY * boundRangePecentage);
		double expectedNextY = 0;
		if(line1.getConcatenatedPositionY() != -1) {
			expectedNextY = line1.getConcatenatedPositionY() + calculatedNextY;
		} else {
			expectedNextY = line1.getPositionY() + calculatedNextY;
		}
//		/*TESTING*/
//		System.out.println("l1: " + line1.getText());
//		System.out.println("l2: " + line2.getText());
//		System.out.println("h1: " + line1.getLineHeight());
//		System.out.println("h2: " + line2.getLineHeight());
//		System.out.println("Y1: " + line1.getPositionY());
//		System.out.println("Y2: " + line2.getPositionY());
//		System.out.println("expected: " + expectedNextY);
//		System.out.println("line2.getPositionY() : " + line2.getPositionY() );
//		System.out.println("line2.getPositionY() <= expectedNextY : " + (line2.getPositionY() <= expectedNextY ) );
//		/*TESTING*/
		return line2.getPositionY() <= expectedNextY ? true : false;
	}
	
	public static boolean isInYBound(Line line1, Line line2, HashMap<Integer, List<Integer>> lineDiff, HashMap<Integer, List<Integer>> lineDiffTogether, boolean probablyL1Incomplete) {
		/*TESTING*/
		SystemLogger.getInstance().debug("l1: " + line1.getText());
		SystemLogger.getInstance().debug("l1 getFCKeySum: " + line1.getFCKeySum());
		SystemLogger.getInstance().debug("l2: " + line2.getText());
		SystemLogger.getInstance().debug("l2 getFCKeySum: " + line2.getFCKeySum());
		SystemLogger.getInstance().debug("Y1: " + line1.getPositionY());
		SystemLogger.getInstance().debug("Y2: " + line2.getPositionY());
		SystemLogger.getInstance().debug("(int)(line2.getPositionY() - line1.getPositionY()): " + Math.round(line2.getPositionY() - line1.getPositionY()));
		if(lineDiff.get(line1.getFCKeySum())!= null)
			SystemLogger.getInstance().debug("ListOperations.findMostFrequentItem(lineDiff.get(line1.getFCKeySum()))+1) : " + ((ListOperations.findMostFrequentItem(lineDiff.get(line1.getFCKeySum()))+1)));
		/*TESTING*/
		float diff = 0;
		if(line1.getConcatenatedPositionY() != -1) {
			diff = ( line2.getPositionY() - line1.getConcatenatedPositionY());
		} else {
			diff = ( line2.getPositionY() - line1.getPositionY());
		}
		if(lineDiff.get(line1.getFCKeySum()) != null && lineDiffTogether.get(line1.getFCKeySum()) != null) {
			int lineDiffForStyle = ListOperations.findMostFrequentItem(lineDiff.get(line1.getFCKeySum()));
			int minDiff = ListOperations.findMinItemInteger(lineDiffTogether.get(line1.getFCKeySum()));
			int maxDiff = ListOperations.findMaxItemInteger(lineDiffTogether.get(line1.getFCKeySum()));
			if(minDiff == maxDiff && maxDiff ==  lineDiffForStyle) {
				return true;
				
			} else if(minDiff == maxDiff && maxDiff !=  lineDiffForStyle) {
				if (Math.round(diff) <= minDiff) {
					return probablyL1Incomplete;
				}else {
					return false;
				}
			} else	if(minDiff != maxDiff) {
				if(Math.round(diff) < maxDiff) {
					SystemLogger.getInstance().debug("@new true");
					return true;	
				} else {
					return false;
				}
			} else {
				return false;
			}
			
		} else if(lineDiff.get(line1.getFCKeySum()) == null) {
			if(lineDiffTogether.get(line1.getFCKeySum()) != null) {
				int minDiff = ListOperations.findMinItemInteger(lineDiffTogether.get(line1.getFCKeySum()));
				int maxDiff = ListOperations.findMaxItemInteger(lineDiffTogether.get(line1.getFCKeySum()));
				SystemLogger.getInstance().debug("min: " + minDiff);
				SystemLogger.getInstance().debug("max: " + maxDiff);
				SystemLogger.getInstance().debug("diff: " + diff);
				if(minDiff != maxDiff && Math.round(diff) < maxDiff) {
					SystemLogger.getInstance().debug("@new true");
					return true;
				} if (minDiff == maxDiff && Math.round(diff) <= minDiff) {
					return true;
				} else {
					SystemLogger.getInstance().debug("@new false");
					return false;
				}
			} else {
				Integer line2LWKS = line1.getLastWord().getFormattingContainerKeySum();
				Integer line2FWKS = line2.getWordAt(0).getFormattingContainerKeySum();
				//if(!line1.getFCKeySum().equals(line2.getFCKeySum())) {
				if(!line2LWKS.equals(line2FWKS)) {
					return false;
				} else {
					return true;
				}
			}
			
		} else {
			Integer lineDiffForStyle = ListOperations.findMostFrequentItem(lineDiff.get(line1.getFCKeySum()));
			if(lineDiffForStyle != null) {
				SystemLogger.getInstance().debug("lineDiffForStyle: " + lineDiffForStyle + " diff: " + Math.round(diff));
				//areWordsAligned
				return Math.round(diff) < lineDiffForStyle ? true : false;
			}
			else {
				return true;
			}
		}
		
	}
	
	public static boolean isInYBound(TOC line1, TOC line2, double boundRangePecentage) {
		double calculatedNextY = line1.getHeight() + line2.getHeight();
		calculatedNextY = calculatedNextY + (calculatedNextY * boundRangePecentage);
		double expectedNextY = 0;
		if(line1.getConcatenatedPosY() != -1) {
			expectedNextY = line1.getConcatenatedPosY() + calculatedNextY;
		} else {
			expectedNextY = line1.getPosY() + calculatedNextY;
		}
		SystemLogger.getInstance().debug("expected: " + expectedNextY);
		SystemLogger.getInstance().debug("line2.getPositionY() : " + line2.getPosY() );
		SystemLogger.getInstance().debug("line2.getPositionY() <= expectedNextY : " + (line2.getPosY() <= expectedNextY ) );
		return line2.getPosY() <= expectedNextY ? true : false;
	}
	
	
	public static boolean areWordsInLineBound(Line line) {
		//System.out.println("Line: " + line.getText() + " Size: " + line.size());
		List<Text> words = line.getWords();
		boolean firstWord = false;
		if (words.size() == 1) {
			return true;
		} else if(words.size() > 0) {
			int count = 0;
			float endOfLastTextX = words.get(0).getEndPositionX();
			float lastWordSpacing = words.get(0).getSpaceWidth();
			float currentWidthOfSpace;
			//System.out.println("Word: " + words.get(0).getText() + " SX: " + words.get(0).getStartPositionX() + " End: " + endOfLastTextX + " lastWordSpacing " + lastWordSpacing);
			
			for(int j = 1; j < words.size(); j++) {
				//System.out.println("For j: " + j  + " W: " + words.get(j).getText()) ;
				Text word = words.get(j);
				currentWidthOfSpace = word.getSpaceWidth();
				float startOfNextWordX = endOfLastTextX + ((currentWidthOfSpace+lastWordSpacing)* 0.6f)+1;
				//System.out.println("currentWidthOfSpace: " + currentWidthOfSpace + " lastWordSpacing: " + lastWordSpacing );
				//System.out.println("Word: " + word.getText() + "SX: " + word.getStartPositionX() + " StartOfNext: " + (startOfNextWordX + 4));
				if(word.getStartPositionX() > (startOfNextWordX + 4)) {
					//System.out.println("Word: " + word.getText() + " SX: " + word.getStartPositionX() + " StartOfNext: " + (startOfNextWordX + 4));
					//return false;
					count++;
					if(j == 1) {
						firstWord = true;
					}
				}
				endOfLastTextX = word.getEndPositionX();
				lastWordSpacing = currentWidthOfSpace;
			}
			//System.out.println("count: " + count);
			if( count == 0 || (count == 1 && (line.isBold() || line.isItalic())) || (count == 1 && firstWord)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}	
	}

	
	public static boolean areLineNotCommon(Line line) {
		List<Text> words = line.getWords();
		int countNum= 0;
		int countFewChars = 0;
		for(int j = 0; j < words.size(); j++) {
			String text = words.get(j).getText();
			if(StringUtils.isNumeric(text)) {
				countNum++;
			}
			if(text.length() <= 3) {
				countFewChars++;
			}
		}
		
		//only numbers in the words
		//all words 3 or less chars
		if(countNum == line.size() || countFewChars == line.size() || (words.size() > 20) || (countFewChars > (line.size() * 0.75))) {
			return true;
		}
		return false;
	}
	
	public static boolean areWordsInLineCentered(Line line, Pair<Float, Float> lineEdges) {
		if(line.size() > 0) {
			float s = line.getWordAt(0).getStartPositionX();
			float e = line.getWordAt(line.size()-1).getEndPositionX();
			float middle = (s+e) / 2;
			float occuppacy = line.getWordAt(line.size()-1).getEndPositionX() - line.getWordAt(0).getStartPositionX();
			
			float middelEdges = (lineEdges.getLeft() + lineEdges.getRight())/2;
			float occupacyEdges = lineEdges.getRight() - lineEdges.getLeft();
			//words are centered + the line is less that 40% of the line capacity + there is no words in the edges
			if(Math.abs(middelEdges - middle) < 10 && occuppacy < (occupacyEdges * 0.4)
					&& Math.abs(line.getWordAt(0).getStartPositionX() - lineEdges.getLeft()) > 15
					&& Math.abs(line.getWordAt(line.size()-1).getEndPositionX() - lineEdges.getRight()) > 15
					) {
				return true;
			}
			
			SystemLogger.getInstance().debug("middle"  + ": " + middle);
			SystemLogger.getInstance().debug("occuppacy"  + ": " +occuppacy );

			SystemLogger.getInstance().debug("s"  + ": " + s);
			SystemLogger.getInstance().debug("e"  + ": " + e);
			SystemLogger.getInstance().debug("middelEdges"  + ": " + middelEdges);
			SystemLogger.getInstance().debug("occupacyEdges"  + ": " + occupacyEdges);
			SystemLogger.getInstance().debug("lineEdges S"  + ": " + lineEdges.getLeft());
			SystemLogger.getInstance().debug("lineEdges E"  + ": " + lineEdges.getRight());
	
			//OLD:
//			Float expected = lineEdges.getLeft() + (line.getWordAt(0).getSpaceWidth() * 2) + 15;
//			if(line.getWordAt(0).getStartPositionX() > expected) {
//				return true;
//			} else {
//				return false;
//			}
		}
		return false;
	}
	
	public static Pair<Float, Float> learnLineEdges(Vector <Page> pages, int bodyFontSize){
		short lookupRange = (short) (((pages.size()/2)+5 < pages.size()) ? (pages.size()/2)+5 : pages.size());

//		/*TESTING*/
//		System.out.println("# pages: " + pages);
//		System.out.println("# pages size: " + pages.size());
//		System.out.println("# lookupRange: " + lookupRange);
//		System.out.println("# starting: " + pages.size()/2);
//		/*TESTING*/

		List<Line> allLines = new ArrayList<Line>();
		for(short i= (short) (pages.size()/2) ; i < lookupRange;  i++){
			if(pages.get(i) != null)
				allLines.addAll(pages.get(i).getLines());
		}
		Float startX = 9999f;
		Float endX = 0f;
		for(Line l: allLines) {
			if(l.size() > 0 && l.getFontSize() == bodyFontSize) {
				if(l.getWordAt(0).getStartPositionX() < startX) {
					startX = l.getWordAt(0).getStartPositionX();
				}
				if(l.getWordAt(l.size()-1).getEndPositionX() > endX) {
					endX = l.getWordAt(l.size()-1).getEndPositionX();
				}
			}
		}
		return Pair.of(startX, endX);
	}
	
	public static Pair<Float, Float> learnLineEdges(List<Line> lines, int bodyFontSize){
		Float startX = 9999f;
		Float endX = 0f;
		for(Line l: lines) {
			if(l.size() > 0 && l.getFontSize() == bodyFontSize) {
				if(l.getWordAt(0).getStartPositionX() < startX) {
					startX = l.getWordAt(0).getStartPositionX();
				}
				if(l.getWordAt(l.size()-1).getEndPositionX() > endX) {
					endX = l.getWordAt(l.size()-1).getEndPositionX();
				}
			}
		}
		return Pair.of(startX, endX);
	}
	
	public static Float learnLineSpacing(Vector <Page> pages, int bodyFontSize){
//		/*TESTING*/
//		System.out.println("# pages: " + pages);
//		System.out.println("# pages size: " + pages.size());
//		System.out.println("# lookupRange: " + lookupRange);
//		System.out.println("# starting: " + pages.size()/2);
//		/*TESTING*/

		HashMap<Float, Integer> freq = new HashMap<Float, Integer>();
		for(short i= 0 ; i < pages.size();  i++){
			if(pages.get(i) != null) {
				for(int l=1; l < pages.get(i).getLines().size(); l++) {
					Line prev = pages.get(i).getLines().get(l-1);
					Line curr = pages.get(i).getLines().get(l);
					int prevFS = FormatExtractor.getLineFontSize(prev);
					int currFS = FormatExtractor.getLineFontSize(curr);
					if(prevFS == currFS && currFS == bodyFontSize) {
						float diff = Math.round(curr.getPositionY() - prev.getPositionY());
						Integer locFreq = freq.get(diff);
						if(locFreq == null)
							locFreq = 0;
						locFreq++;
						freq.put(diff, locFreq);
					}
					
				}
			}
		}
		Float lineSpacing = 0f;
		Integer biggestFreq = 0;
		for(Entry<Float, Integer> e: freq.entrySet()) {
			if(e.getValue() > biggestFreq) {
				biggestFreq = e.getValue();
				lineSpacing = e.getKey();
			}
		}
		
		return lineSpacing;
	}
	
	public static Float learnLineSpacing(List<Line> lines, int bodyFontSize){
//		/*TESTING*/
//		System.out.println("# pages: " + pages);
//		System.out.println("# pages size: " + pages.size());
//		System.out.println("# lookupRange: " + lookupRange);
//		System.out.println("# starting: " + pages.size()/2);
//		/*TESTING*/

		HashMap<Float, Integer> freq = new HashMap<Float, Integer>();

		for(int l=1; l < lines.size(); l++) {
			Line prev = lines.get(l-1);
			Line curr = lines.get(l);
			int prevFS = FormatExtractor.getLineFontSize(prev);
			int currFS = FormatExtractor.getLineFontSize(curr);
			if(prevFS == currFS && currFS == bodyFontSize) {
				float diff = Math.round(curr.getPositionY() - prev.getPositionY());
				Integer locFreq = freq.get(diff);
				if(locFreq == null)
					locFreq = 0;
				locFreq++;
				freq.put(diff, locFreq);
			}
			
		}
			
		Float lineSpacing = 0f;
		Integer biggestFreq = 0;
		for(Entry<Float, Integer> e: freq.entrySet()) {
			if(e.getValue() > biggestFreq) {
				biggestFreq = e.getValue();
				lineSpacing = e.getKey();
			}
		}
		
		return lineSpacing;
	}
	
	public static boolean areWordsInLineBoundAndNotCentered(Line line, Pair<Float, Float> lineEdges) {
		
		
		return areWordsInLineBound(line) && !areWordsInLineCentered(line, lineEdges) ;
	}

	/**
	 * 
	 * @param argumentTocheck
	 * @param boundToCheck
	 * @param boundRangeSetter1
	 * @param boundRangeSetter2
	 * @param boundRangePecentage
	 * @return
	 */
	
	public static boolean isInBound(float argumentTocheck, float boundToCheck, float boundRangeSetter1, float boundRangeSetter2, float boundRangePecentage){
		
//		/*TESTING*/
//		SystemLogger.getInstance().log("^^^^0 ArgumentToCheck: " + argumentTocheck);
//		SystemLogger.getInstance().log("^^^^0 boundToCheck: " + boundToCheck);
//		SystemLogger.getInstance().log("^^^^1: " + (boundToCheck - ( boundRangeSetter1 * boundRangePecentage)));
//		SystemLogger.getInstance().log("^^^^1 RESULT: " + (argumentTocheck > boundToCheck - ( boundRangeSetter1 * boundRangePecentage)));
//		SystemLogger.getInstance().log("^^^^2: " + (boundToCheck + ( boundRangeSetter2 * boundRangePecentage)));
//		SystemLogger.getInstance().log("^^^^2 RESULT: " + (argumentTocheck < boundToCheck + ( boundRangeSetter2 * boundRangePecentage)));
//		/*TESTING*/
		
		if( 	argumentTocheck == boundToCheck
			||	(argumentTocheck > boundToCheck - ( boundRangeSetter1 * boundRangePecentage ) 
			&&	argumentTocheck < boundToCheck + ( boundRangeSetter2 * boundRangePecentage) )
				) 
			return true;		
		else			
			return false;
	}
	
	
	/**
	 * 
	 * @param argumentTocheck
	 * @param boundToCheck
	 * @param boundRangeSetter1
	 * @param boundRangeSetter2
	 * @param boundRangePecentage
	 * @return
	 */
	public static int isGreaterOrLesser(float argumentTocheck, float boundToCheck, float boundRangeSetter1, float boundRangeSetter2, float boundRangePecentage){
		
		if(argumentTocheck < boundToCheck - ( boundRangeSetter1 * boundRangePecentage ))
			return -1;
			
		if(argumentTocheck > boundToCheck + ( boundRangeSetter2 * boundRangePecentage))
			return 1;
		
		return 0;	
	}	
}
