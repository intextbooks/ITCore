package intextbooks.content.models.formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import intextbooks.content.extraction.buildingBlocks.format.Line;

public class FormattingDictionary {
	private HashMap<Integer, FormattingContainer> dict = new HashMap<Integer,FormattingContainer>();
	private List<FormattingContainer> styleLibrary = null;
	private int counter;
	int bodyIndex = 0;
	int bodyFontSize = 0;
	private int totalWords;
	private Double[] percentages;

	/**
	 * 
	 */
	public FormattingDictionary() {
		super();
		this.counter = 0;
	}
	
	public int getTotal() {
		int f = 0;
		int total = 0;
		for(Entry<Integer, FormattingContainer> e : dict.entrySet()) {
			total += e.getValue().getFreq();
			f++;
		}
		System.out.println("total fonts 1: " + f);
		return total;
	}
	
	public Integer containsFormat(FormattingContainer format){
		if(this.dict.containsKey(format.getKeySum())) {
			this.dict.get(format.getKeySum()).incremetFreq(format.getFreq());
			return format.getKeySum();
		}
		else {
			return addFormatting(format);
		}
	}
	
	public boolean checkForFormat(Integer keySum){
		return this.dict.containsKey(keySum);
	}
	
	private Integer addFormatting(FormattingContainer formatting){
		formatting.setName(this.counter);
		counter++;
		this.dict.put(formatting.getKeySum(), formatting);
		
		return formatting.getKeySum();
	}

	public int getSize() {
		return this.counter;
	}

	public String getFormatName(Integer keySum) {
		if(this.dict.containsKey(keySum))
			return this.dict.get(keySum).getName();
		
		return null;
	}
	
	
	
	public String getFontFamily(Integer keySum){
		if(this.dict.containsKey(keySum))
			return this.dict.get(keySum).getFontFamily();
		
		return null;
	}
	
	public short getFontSize(Integer keySum){
		if(this.dict.containsKey(keySum))
			return this.dict.get(keySum).getFontSize();
		
		return 0;
	}
	
	public int getIndentation(Integer keySum){
		if(this.dict.containsKey(keySum))
			return this.dict.get(keySum).getIndentation();
		
		return 0;
	}
	public boolean getBold(Integer keySum){
		if(this.dict.containsKey(keySum))
			return this.dict.get(keySum).isBold();
		
		return false;
	}
	
	public boolean getItalic(Integer keySum){
		if(this.dict.containsKey(keySum))
			return this.dict.get(keySum).isItalic();
		
		return false;
	}
	
	public ArrayList<Integer> getAllFormatKeys(){
		Iterator iter = dict.entrySet().iterator();
		ArrayList<Integer> keys = new ArrayList<Integer>();
	    while (iter.hasNext()) {
	        Map.Entry<Integer,FormattingContainer> pairs = (Map.Entry)iter.next();
	        keys.add(pairs.getKey());
	    }
	    
	    return keys;
	}
	
	public void print() {
		int total = 0;
		TreeSet<FormattingContainer> format = new TreeSet<FormattingContainer>(new FormattingContainer());
		format.addAll(dict.values());
		System.out.println("----------------------");
		Iterator<FormattingContainer> it = format.descendingIterator();
//		System.out.println("original total fonts it: " + dict.size());
//		for(FormattingContainer f : dict.values()) {
//			System.out.println("******");
//			System.out.println(f.toString());
//		}
		System.out.println("total fonts is: " + format.size());
		while(it.hasNext()) {
			FormattingContainer fC = it.next();
			System.out.println("////////////////////////////////");
			System.out.println(fC.toString());
			total += fC.getFreq();
		}
		System.out.println("TOTAL: " + total);
	}
	
	public void printRoleLabels() {
		int total = 0;
		System.out.println("----------------------");
		System.out.println("total fonts is: " + styleLibrary.size());
		for(int i = 0; i < styleLibrary.size(); i++) {
			FormattingContainer fC = styleLibrary.get(i);
			System.out.println("////////////////////////////////");
			System.out.println(fC.toString());
			total += fC.getFreq();
		}
		System.out.println("TOTAL: " + total);
	}
	
	public void constructStyleLibrary() {
		totalWords = 0;
		
		//First order the entries
		TreeSet<FormattingContainer> format = new TreeSet<FormattingContainer>(new FormattingContainer());
		format.addAll(dict.values());
		int biggestFreq = -1;
		int posBiggestFreq = 0;
		//Get the position of the body text formatting
		Iterator<FormattingContainer> it = format.descendingIterator();
		int pos =0;
		while(it.hasNext()) {
			FormattingContainer fC = it.next();
			if(fC.getFreq() > biggestFreq) {
				biggestFreq = fC.getFreq();
				posBiggestFreq = pos;
				this.bodyFontSize = fC.getFontSize();
			}
			this.totalWords += fC.getFreq();
			pos++;
		}
		
		//construct percentages 
		int cantTitle = 0;
		int cantBody = 0;
		int cantRest = 0;
		for(FormattingContainer fC : dict.values()) {
			if(fC.getFontSize() > this.bodyFontSize) {
				cantTitle += fC.getFreq();
			} else if (fC.getFontSize() < this.bodyFontSize) {
				cantRest += fC.getFreq();
			} else {
				cantBody += fC.getFreq();
			}
		}
		percentages = new Double[3];
		percentages[0] = (cantTitle * 100.0) / this.totalWords;
		percentages[1] = (cantBody * 100.0) / this.totalWords;
		percentages[2] = (cantRest * 100.0) / this.totalWords;
		
		//Construct the array
		if(biggestFreq != -1) {
			this.styleLibrary = new ArrayList<FormattingContainer>();
			it = format.descendingIterator();
			for(int i = 0; i <= posBiggestFreq; i++) {
				FormattingContainer fC = it.next();
				//cleaning styles with less than 10 matches || same font size as the body style
				if(fC.getFreq() > 10 && (fC.fontSize > this.bodyFontSize || i==posBiggestFreq))
					this.styleLibrary.add(fC);
			}
			this.bodyIndex = this.styleLibrary.size()-1;
			this.bodyFontSize = this.styleLibrary.get(this.bodyIndex).getFontSize();
			this.styleLibrary.get(this.bodyIndex).setRoleLabel(FormattingContainer.RoleLabel.Body);
		}
		
	}
	
	public void printData() {
		System.out.println("TOTAL words: " + this.totalWords);
		System.out.println("TITLE level : " + percentages[0] + "%");
		System.out.println("BODY level : " + percentages[1] + "%");
		System.out.println("REST level : " + percentages[2] + "%");
	}
	
	public List<FormattingContainer> getStyleLibrary() {
		if(this.styleLibrary == null)
			constructStyleLibrary();

		return this.styleLibrary;
	}
	
	public int getBodyFontSize() {
		if(this.styleLibrary == null) {
			constructStyleLibrary();
		}
		return this.bodyFontSize;
		//return this.styleLibrary.get(this.bodyIndex).getFontSize();
		
	}
	
	public void updateTitleRoleLabel(int keySum, int level) {
		FormattingContainer found = null;
		for(FormattingContainer fC: styleLibrary) {
			if(fC.getKeySum().equals(keySum)) {
				fC.setRoleLabel(FormattingContainer.RoleLabel.Title);
				fC.setTitleLevel(level);
				found = fC;
				break;
			}
		}
		for(FormattingContainer fC: styleLibrary) {
			if(found != null && fC != found && fC.getFontFamily().equals(found.getFontFamily()) && fC.getFontSize() == found.getFontSize() && fC.isBold() == found.isBold() &&
					fC.isItalic() == found.isItalic()) {
				fC.setRoleLabel(FormattingContainer.RoleLabel.Title);
				fC.setTitleLevel(level);
			}
		}
	}
	
	public void cleanlibrary() {
		Iterator<FormattingContainer> it = styleLibrary.iterator();
		while(it.hasNext()) {
			FormattingContainer fC = it.next();
			if(fC.getRoleLabel() == null) {
				it.remove();
			}
		}
		this.bodyIndex = styleLibrary.size() - 1;
		this.bodyFontSize = this.styleLibrary.get(this.bodyIndex).getFontSize();
	}
	
	public FormattingContainer findRole(int keySum) {
		for(FormattingContainer fC: styleLibrary) {
			if(fC.getKeySum().equals(keySum)) {
				return fC;
			}
		}
		return null;
	}
	
	public boolean sameRole(Line l1, Line l2) {
		FormattingContainer r1 = findRole(l1.getFCKeySum());
		FormattingContainer r2 = findRole(l2.getFCKeySum());
		if(r1 != null && r2 != null && r1.getRoleLabel().equals(r2.getRoleLabel()) && r1.getTitleLevel() == r2.getTitleLevel()){
			return true;
		} else {
			return false;
		}
	}
}
