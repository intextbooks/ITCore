package intextbooks.content.extraction.buildingBlocks.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import intextbooks.content.extraction.buildingBlocks.format.Line;
import intextbooks.content.extraction.buildingBlocks.format.Text;
import intextbooks.content.models.formatting.FormattingContainer;
import intextbooks.content.models.formatting.FormattingDictionary;
import intextbooks.content.models.formatting.FormattingContainer.RoleLabel;

public class TextBlock {
	List<Line> lines;
	List<Text> words;
	BookSectionType type;
	private static FormattingDictionary fD;
	boolean special = false;
	
	private RoleLabel roleLabel;
	private int titleLevel;
	
	public TextBlock() {
		words = new ArrayList<Text>();
		lines = new ArrayList<Line>();
	}
	
	public void mergeBlock(TextBlock textBlock) {
		words.addAll(textBlock.words);
	}
	
	public void addLine(Line line) {
		words.addAll(line.getWords());
		lines.add(line);
	}
	
	public String extractText(){
		
		if(this.words.size() == 0)
			return "";
		
		String buffer="";	
		
		for(short i=0;i<this.words.size();i++){
			if(words.get(i) != null){
			
				buffer+=this.words.get(i).getText().trim() + " ";
				
			}
		}

		buffer=buffer.trim();
		
		return buffer;
	}
	
	public void setType(BookSectionType type) {
		this.type = type;
	}
	
	public BookSectionType getType() {
		return this.type;
	}
	
	public int getFontSize() {
		//IMPORTANT: Returns biggest font used, not the most common, and not only if body size
		int fontSize = 0;
		int cantSmaller= 0;
		int total = 0;
		HashMap<Float, Integer> fontSizes = new HashMap <Float, Integer>();
		for(Text word: words){
			if(special) {
				String text = word.getText();
				if(text.toLowerCase().contains("chapter")) {
					continue;
				}
			}
			
			//System.out.println("w: " + word.getText() + " c: " + word.size());
			Integer cant = fontSizes.get(word.getFontSize());
			if(cant == null) {
				cant = 0;
			}
			cant += word.size();
			fontSizes.put(word.getFontSize(), cant);
			total += words.size();

			if(word.getFontSize() < fD.getBodyFontSize()) {
				cantSmaller+= word.size();
			}
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
		if(cantSmaller != 0 && cantSmaller >= (total / 2)) {
			return fD.getBodyFontSize() -1;
		}
		
		//System.out.println("retorning biggestCommonFontSize: " + biggestCommonFontSize);
		
		return (int) biggestCommonFontSize;
	}
	
	public boolean isBold() {
		//changed to majority
		int bold = 0;
		int total = 0;
		for(Text word: words){
			if(word.isBold()) {
				bold += word.size();
			}
			total += word.size();
		}
		return bold >= (int) total/2 ? true : false;
	}
	
	public boolean isItalic() {
		//changed to majority
		int italic = 0;
		int total = 0;
		for(Text word: words){
			if(word.isItalic()) {
				italic += word.size();
			}
			total += word.size();
		}
		return italic >= (int) total/2 ? true : false;
	}
	
	public boolean isSpecialBodyText() {
		return false;
	}
	
	public FormattingContainer getFormattingContainer() {
		FormattingContainer fC =  new FormattingContainer();
		fC.setSize((float)this.getFontSize());
		fC.setBold(this.isBold());
		fC.setItalic(isItalic());
		fC.updateKeySum();
		return fC;
	}
	
	public static void setFormattingDictionary(FormattingDictionary fD) {
		TextBlock.fD = fD;
	}
	
	public Line asLine() {
		Line line = new Line();
		for(Text word: words){
			line.addWord(word);
		}
		line.extractValues();
		line.extractText();
		return line;
	}
	
	public List<Line> getLines(){
		return this.lines;
	}

	public boolean isSpecial() {
		return special;
	}

	public void setSpecial(boolean special) {
		this.special = special;
	}

	public RoleLabel getRoleLabel() {
		return roleLabel;
	}

	public void setRoleLabel(RoleLabel roleLabel) {
		this.roleLabel = roleLabel;
	}

	public int getTitleLevel() {
		return titleLevel;
	}

	public void setTitleLevel(int titleLevel) {
		this.titleLevel = titleLevel;
	}
	
	public String getRoleLabelString() {
		String res = "";
		if(roleLabel == RoleLabel.Title) {
			res = "Title " +  titleLevel;
		} else {
			res = "Body";
		}
		return res;
	}
	
}
