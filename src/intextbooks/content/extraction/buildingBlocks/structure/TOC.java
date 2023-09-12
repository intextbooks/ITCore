package intextbooks.content.extraction.buildingBlocks.structure;

public class TOC {

	private String titleText;
	
	private int pageNumber;
	private int pageIndex;
	
	private float posX;
	private float posY;
	private float concatenatedPosY;
	
	private float fontSize;
	
	private boolean bold;
	private boolean italic; 
	private boolean section = false;
	private boolean erratum = false;
	
	protected float height;
	
	private String chapterPrefix = null;
	
	public String getTitleText() {
		return titleText;
	}
	public void setTitleText(String titleText) {
		this.titleText = titleText;
	}
	public int getPageNumber() {
		return pageNumber;
	}
	public void setPageNumber(int i) {
		this.pageNumber = i;
	}
	public int getPageIndex() {
		return pageIndex;
	}
	public void setPageIndex(short pageIndex) {
		this.pageIndex = pageIndex;
	}
	public float getPosX() {
		return posX;
	}
	public void setPosX(float posX) {
		this.posX = posX;
	}
	public float getPosY() {
		return posY;
	}
	public void setPosY(float posY) {
		this.posY = posY;
	}
	public void setConcatenatedPosY(float posX) {
		this.concatenatedPosY = posX;
	}
	public float getConcatenatedPosY() {
		return concatenatedPosY;
	}
	public float getFontSize() {
		return fontSize;
	}
	public void setFontSize(float fontSize) {
		this.fontSize = fontSize;
	}
	public boolean isBold() {
		return bold;
	}
	public void setBold(boolean bold) {
		this.bold = bold;
	}
	public boolean isItalic() {
		return italic;
	}
	public void setItalic(boolean italic) {
		this.italic = italic;
	}
	public String getChapterPrefix() {
		return chapterPrefix;
	}
	public void setChapterPrefix(String chapterPrefix) {
		this.chapterPrefix = chapterPrefix;
	}
	
	public void setSection(boolean value) {
		if(erratum && value)
			return;
		this.section = value;
	}
	
	public boolean getSection() {
		return this.section;
	}
	public float getHeight() {
		return height;
	}
	public void setHeight(float height) {
		this.height = height;
	}
	public boolean isErratum() {
		return erratum;
	}
	public void setErratum(boolean erratum) {
		this.erratum = erratum;
	}
	
	
	
}
