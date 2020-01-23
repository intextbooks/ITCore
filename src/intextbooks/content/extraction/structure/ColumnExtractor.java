package intextbooks.content.extraction.structure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import intextbooks.SystemLogger;
import intextbooks.content.extraction.Utilities.WordListCheck;
import intextbooks.content.extraction.buildingBlocks.format.CharacterBlock;
import intextbooks.content.extraction.buildingBlocks.format.Line;
import intextbooks.content.extraction.buildingBlocks.format.Text;


public class ColumnExtractor {
	ColumnsFormat columnsFormat;
	List<Integer> ignoredLines;
	List<Line> pageLines;
	
	public ColumnExtractor(List<Line> pageLines) {
		this.columnsFormat = new ColumnsFormat();
		this.ignoredLines =  new ArrayList<Integer>();
		this.pageLines = pageLines;
	}
	
	private void restartSegments(List<ColumnSegment> segments ) {
		for(ColumnSegment s: segments) {
			s.restartCount();
		}
	}
	
	private List<Line> getWindowLines(List<Line> pageLines, List<Integer> ignoredLines, int lastLine){
		List<Line> windowLines = new ArrayList<Line>();
		for(int i=0; i <= lastLine; i++) {
			if(!ignoredLines.contains(i)) {
				windowLines.add(pageLines.get(i));
			}
		}
		
		return windowLines;
	}
	
	public boolean identifyColumns() {
		
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
		System.out.println("average width : " + averageWidth);
		
		//check lines
		int currentLineIndex = 0;
		int totalHits = 0;
		List<Line> lines = new ArrayList<Line>();
		while(currentLineIndex < pageLines.size()) {

			 //List<Line> lines = getWindowLines(pageLines, ignoredLines, currentLine);
			Line  currentLine = pageLines.get(currentLineIndex);
//			System.out.println("######");
//			System.out.println("current line: " + currentLineIndex + " - " + currentLine.getText());
//			System.out.println("lines size: " + lines.size());
//			System.out.println("ignoredLines size: " + ignoredLines.size());
//			System.out.println("ignoredLines: " + ignoredLines);
			
			//create segments
			List<ColumnSegment> segments = new ArrayList<ColumnSegment>();
			Float segmentStart = startLeft;
			Float segmentInterval = averageWidth;
			Float segmentEnd = segmentStart + segmentInterval;
			while(segmentStart <= endRight) {
				segments.add(new ColumnSegment(segmentStart, segmentEnd));
				segmentStart = segmentEnd;
				segmentEnd = segmentStart + segmentInterval;
			}
			
			//count hits for the segment
			for(ColumnSegment seg: segments) {
				for(Line line: lines) {
					for(Text word: line.getWords()) {
						seg.check(word.getStartPositionX(), word.getEndPositionX());
					}
				}
				for(Text word: currentLine.getWords()) {
					seg.check(word.getStartPositionX(), word.getEndPositionX());
				}
			}
			
			//set the segment blank or not
			for(ColumnSegment seg: segments) {
				if(seg.getHits() == 0) {
					seg.setBlank(true);
				} else {
					seg.setBlank(false);
				}
			}
			
			//merge adjacent segments
			Iterator<ColumnSegment> it = segments.iterator();
			if(it.hasNext()) {
				ColumnSegment previous = it.next();
				while(it.hasNext()) {
					ColumnSegment segment = it.next();
					if(previous.getBlank() == segment.getBlank()) {
						previous.merge(segment);
						it.remove();
					} else {
						previous = segment;
					}
				}
			}
			
			
			int totalA = 0;
			for(ColumnSegment seg: segments) {
				if(seg.getBlank()) {
					totalA++;
				}
			}
			
			if(totalA > 0) {
				totalHits++;
				lines.add(currentLine);
				//System.out.println("RESULT: OK");
			} else {
				ignoredLines.add(currentLineIndex);
				//System.out.println("RESULT: ignore");
				//if(lines.size() > 0)
					//System.out.println("last line in lines: " + lines.get(lines.size() -1).getText());
			}
			
			currentLineIndex++;
			restartSegments(segments);
		}
		
//		System.out.println("Total lines: " + pageLines.size());
//		System.out.println("Total hits: " + totalHits);
//		System.out.println("ignoreLines: " + ignoredLines);
//		for(int i: ignoredLines) {
//			System.out.println("\t" + pageLines.get(i).getText());
//		}
		
		
		if(pageLines.size() > pageLines.size() / 2) {
			System.out.println(">> COLUMN!");
			
			//create segments
			List<ColumnSegment> segments = new ArrayList<ColumnSegment>();
			Float segmentStart = startLeft;
			Float segmentInterval = averageWidth;
			Float segmentEnd = segmentStart + segmentInterval;
			while(segmentStart <= endRight) {
				segments.add(new ColumnSegment(segmentStart, segmentEnd));
				segmentStart = segmentEnd;
				segmentEnd = segmentStart + segmentInterval;
			}
			
			//count hits for the segment
			for(ColumnSegment seg: segments) {
				for(Line line: lines) {
					for(Text word: line.getWords()) {
						seg.check(word.getStartPositionX(), word.getEndPositionX());
					}
				}
			}
			
			//set the segment blank or not
			for(ColumnSegment seg: segments) {
				if(seg.getHits() == 0) {
					seg.setBlank(true);
				} else {
					seg.setBlank(false);
				}
			}
			
			//merge adjacent segments
			Iterator<ColumnSegment> it = segments.iterator();
			if(it.hasNext()) {
				ColumnSegment previous = it.next();
				while(it.hasNext()) {
					ColumnSegment segment = it.next();
					if(previous.getBlank() == segment.getBlank()) {
						previous.merge(segment);
						it.remove();
					} else {
						previous = segment;
					}
				}
			}
			
			 
			boolean first = true;
			for(ColumnSegment seg: segments) {
				SystemLogger.getInstance().debug("----------");
				SystemLogger.getInstance().debug("SEGMENT START: " + seg.getStart());
				SystemLogger.getInstance().debug("SEGMENT END: " + seg.getEnd());
				SystemLogger.getInstance().debug("SEGMENT hits: " + seg.getHits());
				SystemLogger.getInstance().debug("SEGMENT is blanck: " + seg.getBlank());
				if(!seg.getBlank()) {
					if(!first) {
						columnsFormat.addColumn(seg.getStart());
						SystemLogger.getInstance().debug("COLUMNS X: " + seg.getStart());
					} else {
						first = false;
						SystemLogger.getInstance().debug("first blanck X: " + seg.getStart());
					}
				}
			}
			SystemLogger.getInstance().debug("NUMBER OF COLUMNS: " + columnsFormat.getNumberOfColumns() +  " Start: " + columnsFormat.getStartOfColumns());
			
			if(columnsFormat.getStartOfColumns().size() == 1){
				return false;
			} else {
				return true;
			}
		} else {
			System.out.println(">> NO column!");
			return false;
		}
	}
	
	public List<Line> getLines(List<Integer> pageBreaks){
		List<ColumnedPage> columnedPages = asColumnedPage(pageBreaks);
		List<Line> lines = new ArrayList<Line>();
		for(ColumnedPage cP: columnedPages) {
			lines.addAll(cP.getAllLines());
		}
		return lines;
	}
	
	public List<ColumnedPage> asColumnedPage(List<Integer> pageBreaks) {
		List<ColumnedPage> columnedPages = new ArrayList<ColumnedPage>();
		ColumnedPage columnedPage = new ColumnedPage();
		Vector<Vector<Line>> columns = new Vector<Vector<Line>>();
		
		//add lines to columns
		lines:
		for(int i = 0; i < pageLines.size(); i++) {
			
			//if there is a page break
			if(i == 0 || pageBreaks.contains(i)) {
				columnedPage = new ColumnedPage();
				columnedPages.add(columnedPage);
				
				columns = new Vector<Vector<Line>>();
				
				//create columns
				for(int c = 0; c < columnsFormat.getNumberOfColumns(); c++) {
					Vector<Line> tempColumn = new Vector<Line>();
					columns.add(tempColumn);
					columnedPage.addColumn(tempColumn);
					SystemLogger.getInstance().debug("creating column : "+ c);
				}
			}
			
			Line currentLine = pageLines.get(i);
			SystemLogger.getInstance().debug("line : " + currentLine.getText());
			
			if(this.ignoredLines.contains(i)) {
				columns.get(0).add(currentLine);
				continue;
			}
			
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
				if((currentColumn + 1) == columnsFormat.numberOfColumns) {
					wordInColumn = wordInColumn(columnsFormat.getStartOfColumn(currentColumn), 99999, (int) word.getStartPositionX(), startOfNextWordX);
				} else {
					wordInColumn = wordInColumn(columnsFormat.getStartOfColumn(currentColumn), columnsFormat.getStartOfColumn(currentColumn+1), (int) word.getStartPositionX(), startOfNextWordX);
					SystemLogger.getInstance().debug("getStartOfColumn : " + columnsFormat.getStartOfColumn(currentColumn));
					SystemLogger.getInstance().debug("cF.getStartOfColumn(currentColumn+1) : " + columnsFormat.getStartOfColumn(currentColumn+1));
					SystemLogger.getInstance().debug("word.getStartPositionX() : " + word.getStartPositionX());
				}
				
				//add to current column or to the next one
				if(wordInColumn) {
					//System.out.println("YES: " + word.getText());
					newLine.addWord(word);
				} else {
					SystemLogger.getInstance().debug("NO: " + word.getText());	
					
					//add the current line
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
						if((currentColumn + 1) == columnsFormat.numberOfColumns) {
							wordInColumn = wordInColumn(columnsFormat.getStartOfColumn(currentColumn), 99999, (int) word.getStartPositionX(), startOfNextWordX);
						} else {
							wordInColumn = wordInColumn(columnsFormat.getStartOfColumn(currentColumn), columnsFormat.getStartOfColumn(currentColumn+1), (int) word.getStartPositionX(), startOfNextWordX);
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
			//last line
			if(newLine.size() > 0) {
				columns.get(currentColumn).add(newLine);
				newLine.extractValues();
			}
		}
		
		
		return columnedPages;
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
	
	public int getNumberOfColumns() {
		if(this.columnsFormat != null) {
			return this.columnsFormat.getNumberOfColumns();
		}
		return 1;
	}

}
