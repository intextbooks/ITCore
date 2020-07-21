package intextbooks.content.extraction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.stanford.nlp.util.StringUtils;
import intextbooks.SystemLogger;
import intextbooks.content.extraction.Utilities.WordListCheck;
import intextbooks.content.extraction.buildingBlocks.format.Line;
import intextbooks.content.extraction.buildingBlocks.format.Text;
import intextbooks.tools.utility.ListOperations;

public class ContentExtractor {
	
	public static void removeCopyRightLines(List<Line> lines, int pageNumber, Map<String, String> metadata) {
		int hits = 0;
		int startRemoval = -1;
		boolean justPageNumber = false;
		String pageNumberString = String.valueOf(pageNumber);
		String bookTitle = null;
		if(metadata.get("title") != null) {
			bookTitle = metadata.get("title").toLowerCase();
		}
		String authors = null;
		if(metadata.get("authors") != null) {
			authors = metadata.get("authors").toLowerCase();
		}
		
		if(lines.size() > 5) {
			lines:
			for(int i = lines.size() - 5; i < lines.size(); i++) {
				Line line = lines.get(i);
				SystemLogger.getInstance().debug("*removing copyright checking: " + line.getText());
				List<Text> words = line.getWords();
				
				//title of textbook 
				if(bookTitle != null) {
					if(line.getText().toLowerCase().contains(bookTitle)) {
						hits++;
						if(startRemoval == -1) {
							startRemoval = i;
						}
					}
				}
				
				//author of textbook
				
				
				for(int j = 0; j < words.size(); j++) {
					String wordText = words.get(j).getText();
					//check page number: first word or last one
					if(j == 0 || (j+1) == words.size()) {
						if(StringUtils.isNumeric(wordText) && wordText.equals(pageNumberString)) {
							hits++;
							if(startRemoval == -1) {
								startRemoval = i;
							}
							if((i + 1) == lines.size() && line.size() == 1) {
								justPageNumber = true;
							}
						}
					}
					//check copyrigth words
					if(WordListCheck.isCopyrightWord(wordText)) {
						hits++;
						if(startRemoval == -1) {
							startRemoval = i;
						}
					}
					
					//authors of textbook
					if(authors != null) {
						if(authors.contains(wordText.toLowerCase())) {
							hits++;
							if(startRemoval == -1) {
								startRemoval = i;
							}
						}
					}
					
				}
			}
		}
		SystemLogger.getInstance().debug("*removing copyright hits: " + hits);
		
		//check if there are lines that need to be remove
		if(hits >= 2 || justPageNumber) {
			while(lines.size() > startRemoval) {
				SystemLogger.getInstance().debug("*removing copyright line: " + lines.get(lines.size() - 1).getText());
				lines.remove(lines.size() - 1);
			}
		}
	}
	
	//remove note and not important lines
	public static void removeSideLines(List<Line> lines) {
		List<Float> fontSizes = new ArrayList<Float>();
		for(int i = 0; i < lines.size(); i++) {
			fontSizes.add(lines.get(i).mostFrequentFontSize());
		}
		Float textFontSize = ListOperations.findMostFrequentItem(fontSizes);
		boolean keep = true;
		while(keep && lines.size() > 0) {
			if(lines.get(lines.size()-1).getFontSize() < textFontSize) {
				lines.remove(lines.size()-1);
			} else {
				break;
			}
		}
	}
}
