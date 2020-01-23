package intextbooks.content.extraction.buildingBlocks.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.graphics.color.PDColor;

import intextbooks.content.models.formatting.FormattingContainer;
import intextbooks.tools.utility.ListOperations;

public class Line extends Text{

	private List <Text> words = new ArrayList<Text> ();
	private Pattern p = Pattern.compile("[a-zA-Z0-9<>+!?,.+=_%$*&-]");
	private Pattern dash = Pattern.compile("[0-9-0-9]");
	private boolean artificial = false;
	private FormattingContainer formattingContainer;
	
	private Map<String, Boolean> properties = new HashMap<String, Boolean>();
	public Line(){
		
	}
	
	public Line(Line l){
		
		this.bold = l.bold;
		this.italic = l.italic;
		this.fontName = l.fontName;
		this.endPositionX = l.endPositionX;
		this.fontSize = l.fontSize;
		this.linePositionY = l.linePositionY;
		this.startPositionX= l.startPositionX;
		this.txt = l.txt;
		this.words = new ArrayList<Text>(l.getWords());
		this.p = l.p;
		this.formattingContainer = null;
	}
	
	public Line(Line l, List<Text> words){
		
		if(words == null) {
			words = new ArrayList<Text> ();
		}
		
		this.bold = l.bold;
		this.italic = l.italic;
		this.fontName = l.fontName;
		this.endPositionX = l.endPositionX;
		this.fontSize = l.fontSize;
		this.linePositionY = l.linePositionY;
		this.startPositionX= l.startPositionX;
		this.txt = l.txt;
		this.words = words;
		this.p = l.p;
		this.formattingContainer = null;
	}
	
	public Line (float x, float y){
		
		this.startPositionX=x;
		this.linePositionY=y;
	}
	
	public float mostFrequentFontSize(){

		Map <Float,  Integer> freqList = new HashMap <> ();

		for(short  i= 0 ; i<this.size(); i++){

			if(this.getWordAt(i)!=null){

				Integer k = freqList.get(this.getWordAt(i).getFontSize());

				k = (k == null) ? 1 : ++k;

				freqList.put(this.getWordAt(i).getFontSize(), k);			
			}
		}

		Map.Entry<Float, Integer> maxEntry = null;

		for (Map.Entry<Float, Integer> entry : freqList.entrySet())
		{
			if (maxEntry == null 
					|| entry.getValue().compareTo(maxEntry.getValue()) > 0 
					|| (entry.getValue().compareTo(maxEntry.getValue()) == 0 && maxEntry.getKey()<2) ){

				maxEntry = entry;
			}
		}
		
		this.setFontSize(maxEntry.getKey());
		return maxEntry.getKey();
	}
	
	public void mostFrequentLinePos(){
		Map <Float,  Integer> freqList = new HashMap <> ();
		float sum=0;
		
		for(short  i= 0 ; i<this.size(); i++){
			
			if(this.getWordAt(i)!=null){

				sum+=this.getWordAt(i).getPositionY();

				Integer k = freqList.get(this.getWordAt(i).getPositionY());

				k = (k == null) ? 1 : ++k;

				freqList.put(this.getWordAt(i).getPositionY(), k);			
			}
		}
		sum/=(float)this.size();
		
		/*Map.Entry<Float, Integer> maxEntry = null;

		for (Map.Entry<Float, Integer> entry : freqList.entrySet())
		{
		    if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
		    {
		        maxEntry = entry;
		    }
		}*/
		
		this.setPositionY(sum);		
	}
	
	public void mostFrequentStyleFeatures(){

		List<String> fonts = new ArrayList<String>();
		List<PDColor> colors = new ArrayList<PDColor>();
		List<Boolean> bolds = new ArrayList<Boolean>();
		List<Boolean> italics = new ArrayList<Boolean>();
		List<Float> sizes = new ArrayList<Float>();

		for(short  i= 0 ; i<this.size(); i++){

			if(this.getWordAt(i)!=null){

				fonts.add(this.getWordAt(i).getFontName());
				colors.add(this.getWordAt(i).getFontColor());
				bolds.add(this.getWordAt(i).isBold());
				italics.add(this.getWordAt(i).isItalic());
				sizes.add(this.getWordAt(i).getFontSize());
		
			}
		}
		String fontName = ListOperations.findMostFrequentItem(fonts);
		if(fontName != null)
			this.setFontName(fontName);
		this.setFontColor(ListOperations.findMostFrequentItem(colors));
	    this.setBold(ListOperations.findMostFrequentItemBoolean(bolds));
		this.setItalic(ListOperations.findMostFrequentItemBoolean(italics));
		this.setFontSize(ListOperations.findMostFrequentItem(sizes));
		
		this.formattingContainer = new FormattingContainer(this.getFontName(), (byte)this.getFontSize(),0, this.isBold(), this.isItalic(), this.getFontColor().getComponents());
	}
	
	public Integer getFCKeySum() {
		return this.formattingContainer.getKeySum();
	}
	
	
	
	public void extractText(){
		
		if(this.getWords().size() == 0)
			return;
		
		String buffer="";	
		
		for(short i=0;i<this.getWords().size();i++){
			if(this.getWordAt(i) != null){
				
				buffer+=this.getWords().get(i).getText()+" ";
			}
		}

		buffer=buffer.trim();

//		/*TESTING*/
//		System.out.println("***WORD: " + this.getWordAt(0).getText());
//		System.out.println("***POSX: " + this.getWordAt(0).getStartPositionX());
//		/*TESTING*/	
		this.startPositionX = this.getWordAt(0).getStartPositionX();
		this.endPositionX = this.getWordAt(this.words.size() - 1).getEndPositionX();
		
		this.setText(buffer);
		this.mostFrequentLinePos();
		this.mostFrequentStyleFeatures();
	}

	
	public Text getWordAt(int i){
		
		return words.get(i);
	}
	
	public void addWord(Text t){
		
		words.add(t);
		this.setEndPositionX(t.getEndPositionX());
	}
	
	public void addWords(List<Text> ws){
		if(words.isEmpty())		
			words = new ArrayList<Text>(ws);	
		else {
			words.addAll(ws);
			if(ws.size() > 0)
				this.setEndPositionX(ws.get(ws.size()-1).getEndPositionX());
		}
	}
	
	public void addWordAt(int pos, Text t){
		words.add(pos, t);
	}
	
	public void removeWordAt(int i){
		
		this.words.remove(i);		
	}
	
	public void removeWordsFrom(int i){
		
		this.words.subList(i, this.words.size()).clear();		
	}
	
	public List<Text> getWords(){
		
		return words;
	}
		
	public boolean checkBold(){
				
		for(short i=0; i<words.size(); i++){
			
			if(this.getWordAt(i) != null){
				if(!words.get(i).isBold())
					return false;			
			}
		}

		return true;
	}
	
	public boolean checkItalic(){
		
		for(short i=0; i<this.getWords().size(); i++){
			
			if(this.getWordAt(i) != null){
				if(!this.getWordAt(i).isItalic())
					return false;			
			}
		}

		return true;		
	}
	
	public String getLastWordText(){
		
		return words.get(words.size() - 1).getText();
		
	}
	
	public Text getLastWord(){
		
		return words.get(words.size() - 1);
	}
	
	public boolean isArtificial() {
		return artificial;
	}

	public void setArtificial(boolean artificial) {
		this.artificial = artificial;
	}
	
	public float getLineHeight() {
		if(this.height == 0.0f) {
			float maxHeight = 0;
			for(Text word: this.words) {
				if(word.getHeight() > maxHeight)
					maxHeight = word.getHeight();
			}
			this.height = maxHeight;
		}
		return this.height;
	}
	
	public List<CharacterBlock> getCharactersInLine(){
		List<CharacterBlock> list = new ArrayList<CharacterBlock>();
		for(Text word: this.words) {
			list.addAll(word.getCharacters());
		}
		return list;
	}

	@Override
	public int size(){
		
		if(this.getWords()!=null && this.getWords().size()>0)
			return this.getWords().size();
		
		return 0;
		
	}
	public float getEndPositionX(){
		if(this.endPositionX == -1 && words.size() > 0) {
			this.endPositionX = words.get(words.size()-1).endPositionX;
		}
		return this.endPositionX;
	}
	
	public void extractValues() {
		if(this.words.size() > 0) {
			this.endPositionX = words.get(words.size()-1).endPositionX;
			this.linePositionY = words.get(0).linePositionY;
			this.startPositionX= words.get(0).startPositionX;
			this.extractText();
		}
	}
	
	@Override
	public String getDetailedText() {
		String str = "";
		for(Text t : words) {
			if(!str.equals("")) {
				str += "\n";
			}
			str += t.getDetailedText();
		}
		return str;
	}
	
	public void setProperty(String key, boolean value) {
		this.properties.put(key, value);
	}
	
	public boolean getProperty(String key) {
		Boolean value = this.properties.get(key);
		if(value != null) {
			return value;
		} else {
			return false;
		}
	}
	
}
