package intextbooks.content.extraction.buildingBlocks.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;

import intextbooks.SystemLogger;
import intextbooks.content.models.formatting.FormattingContainer;
import intextbooks.tools.utility.ListOperations;

public class Text {
	
	//composition
	protected String txt = "";
	protected List<CharacterBlock> characters;
	
	//style properties
	protected boolean italic = false;
	protected boolean bold = false;	
	protected PDColor fontColor = null;
	protected String fontName=null;
	protected String fontPrefix = "";
	protected float fontSize = -1;

	//geometric features
	protected float linePositionY = -1;
	protected float concatenatedLinePositionY = -1;
	protected float startPositionX = -1;
	protected float endPositionX = -1;
	protected float height;
	private float width;
	protected float spaceWidth;
	
	
	private Matrix coordinates = null;


	public Text(){
	}
	
	public Text(String s){
		this.txt=s;
	}
	
	//main
	public Text(String s, List<CharacterBlock> characters){
		this.txt=s;
		this.characters = characters;
		this.updateStyleProperties();
		this.updateGeometricProperties();
	}
	
	public Text(String s, float f, float P, float l, Matrix m){

		this.txt=s;
		this.fontSize=f;
		this.startPositionX=P;
		this.linePositionY=l;		
		this.setCoordinates(m);
	}
	
	private void updateStyleProperties() {	
		//get values from the characters blocks
		List<Boolean> boldList = new ArrayList<Boolean>();
		List<Boolean> italicList = new ArrayList<Boolean>();
		List<String> fontList = new ArrayList<String>();
		List<PDColor> colorList = new ArrayList<PDColor>();
		List<Float> sizeList = new ArrayList<Float>();
		for(CharacterBlock c: this.characters) {
			TextPosition text = c.getTextPosition();
			boldList.add(Text.isBold(text));
			italicList.add(Text.isItalic(text));
			String font = null;
			if(text.getFont().getFontDescriptor() != null && text.getFont().getFontDescriptor().getFontName() != null) {
				font = text.getFont().getFontDescriptor().getFontName();
			} 
			fontList.add(font);
			colorList.add(c.getColor());
			sizeList.add(text.getFontSizeInPt());
		}
		//get biggest values
		this.setBold(ListOperations.findMostFrequentItemBoolean(boldList));
		this.setItalic(ListOperations.findMostFrequentItemBoolean(italicList));
		String font = ListOperations.findMostFrequentItem(fontList);
		if(font != null)
			this.setFontName(font);
		this.setFontColor(ListOperations.findMostFrequentItem(colorList));
		this.setFontSize(ListOperations.findMaxFloatItem(sizeList));
	}
	
	private void updateGeometricProperties() {
		//with the help of the bounding boxes from the characters
		this.setStartPositionX((float)this.characters.get(0).getStartX());
		this.setEndPositionX((float)this.characters.get(this.characters.size() - 1).getEndX());
		this.setPositionY((float)this.characters.get(0).getEndY());
		this.setHeight((float)this.characters.get(0).getBBHeight());
		this.setWidth((float)this.characters.get(0).getBBWidth());
		this.setSpaceWidth((float)this.characters.get(0).getWidthOfSpace());	
	}

	public void setFontName(String f){

		if(f.contains("+")){
			this.fontPrefix = f.substring(0, f.indexOf("+"));
		}
		
		this.fontName=f.substring(f.indexOf("+")+1);
	}

	public String getFontPrefix(){

		return this.fontPrefix;
	}
	
	public void gsetFontPrefix(String p){

		this.fontPrefix = p;
	}

	public String getFontName(){

		return this.fontName;
	}

	public void setEndPositionX(float e){

		this.endPositionX=e;		
	}

	public float getEndPositionX(){

		return this.endPositionX;
	}

	public void setFontSize(float f){

		this.fontSize=f;
	}

	public float getFontSize(){

		return this.fontSize;
	}

	public String getText(){

		return this.txt;
	}
	
	public List<CharacterBlock> getCharacters(){
		return this.characters;
	}
	
	public void addBoldItalic(boolean b, boolean i, String f, PDColor fontColor) {
		/*int pos = txt.length() - 1;
		//add values for each char
		if(pos >= 0) {
			this.boldChars.set(pos, b);
			this.italicChars.set(pos, i);
			Integer cant = this.fontColorChars.get(fontColor);
			if(cant == null) {
				cant = 0;
			}
			cant++;
			this.fontColorChars.put(fontColor, cant);
			//update the color font of the word if necessary
			boolean updateFontColor = true;
			for(Integer val: this.fontColorChars.values()) {
				if(cant < val) {
					updateFontColor = false;
					break;
				}
			}
			if(updateFontColor) {
				this.fontColor = fontColor;
			}
			this.fontChars.add(f);
		}
		
		//update values for the word
		int half = Math.round(txt.length() / 2.0f);
		if(this.boldChars.cardinality() >= half) {
			this.bold=true;
		} else {
			this.bold=false;
		}
		if(this.italicChars.cardinality() >= half) {
			this.italic=true;
		} else {
			this.italic=false;
		}
		String font = ListOperations.findMostFrequentItem(this.fontChars);
		if(font != null)
			this.setFontName(font);
			*/
	}

	public void setBold(boolean b)
	{
		this.bold=b;
	}

	public boolean isBold(){

		return this.bold;
	}

	public void setItalic(boolean b)
	{
		this.italic=b;
	}

	public boolean isItalic(){

		return this.italic;
	}
	
	public PDColor getFontColor() {
		return this.fontColor;
	}
	
	public void setFontColor(PDColor color) {
		this.fontColor = color;
	}

	public float getStartPositionX(){

		return this.startPositionX;
	}

	public void setStartPositionX(float p){

		this.startPositionX=p;
	}

	public float getPositionY(){

		return this.linePositionY;
	}

	public void setPositionY(float p){

		this.linePositionY=p;
	}
	public float getConcatenatedPositionY(){

		return this.concatenatedLinePositionY;
	}

	public void setConcatenatedPositionY(float p){

		this.concatenatedLinePositionY=p;
	}

	public int size(){

		if(this.txt!= null && !this.txt.isEmpty())
			return this.txt.length();

		return 0;
	}

	public Matrix getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(Matrix coordinates) {
		this.coordinates = coordinates;
	}

	public float getHeight() {
		return height;
	}

	public void setHeight(float height) {
		this.height = height;
	}

	public float getWidth() {
		return width;
	}

	public void setWidth(float width) {
		this.width += width;
	}
	
	public void setSpaceWidth(float width) {
		this.spaceWidth = width;
	}
	
	public float getSpaceWidth() {
		return this.spaceWidth;
	}
	
	public String getDetailedText() {
		return "W: " + this.txt + " B? " + this.isBold() + " I? " + this.isItalic() + " F: " + this.getFontName() + " FS: " + this.getFontSize() + " C: " + this.getFontColor() 
		+ " SX: " + this.getStartPositionX() + " EX: " + this.getEndPositionX() + " Y: " + this.getPositionY();
	}

	public void setText(String s){

		this.txt=s;
	}
	
	/**
	 * STATIC METHODS
	 */
	private static boolean isItalic(TextPosition text){
		String postscriptName;
			
		if(text.getFont().getFontDescriptor() != null && text.getFont().getFontDescriptor().getFontName() != null)
			 postscriptName = text.getFont().getFontDescriptor().getFontName();
		else
			postscriptName = null;
		
		if(postscriptName != null && (postscriptName.toLowerCase().contains("italic") || postscriptName.toLowerCase().contains("oblique")
				|| text.getFont().getFontDescriptor().isItalic() || text.getFont().getFontDescriptor().getItalicAngle() != 0)) {
			return true;
		} else		
			return false;		
	}
	
	private static boolean isBold (TextPosition text){
		
		String postscriptName;

		if(text.getFont().getFontDescriptor() != null && text.getFont().getFontDescriptor().getFontName() != null)
			 postscriptName = text.getFont().getFontDescriptor().getFontName();
		else
			postscriptName = null;
		
		if(postscriptName != null && (postscriptName.toLowerCase().contains("bold") || text.getFont().getFontDescriptor().isForceBold())) {
			return true;
		}
		else		
			return false;
	}
	
	public Integer getFormattingContainerKeySum() {
		FormattingContainer f = new FormattingContainer(this.getFontName(), (byte)this.getFontSize(),0, this.isBold(), this.isItalic(), this.getFontColor().getComponents());
		return f.getKeySum();
	}
	

}
