package intextbooks.content.extraction.structure;

public class ChapterMetaData {
	int pageStartIndex;
	int pageEndIndex;

	int pageStart;
	int pageEnd;

	int lineStart;
	int lineEnd;
	int lineTitleStart;

	double leftTopX;
	double leftTopY;

	double rightBottomX;
	double rightBottomY;

	String pageTitle;
	
	boolean hasParagraph;
	
	int chapterHierarchy;
	
	boolean isNonContent;

	public ChapterMetaData(int si, int s, int ls, int ei, int e, int le,double lTX, double lTY, double rBX, double rBY, String t, boolean hP, int lTS) {

		pageStartIndex = si;
		pageStart=s;
		lineStart=ls;
		pageEndIndex=ei;
		pageEnd=e;
		lineEnd=le;
		pageTitle=t;
		leftTopX = lTX;
		leftTopY= lTY;
		rightBottomX= rBX;
		rightBottomY= rBY;		
		hasParagraph = hP;
		lineTitleStart = lTS;
		isNonContent = false;
	}

	public int getPageStartIndex() {
		return pageStartIndex;
	}

	public void setPageStartIndex(int pageStartIndex) {
		this.pageStartIndex = pageStartIndex;
	}

	public int getPageEndIndex() {
		return pageEndIndex;
	}

	public void setPageEndIndex(int pageEndIndex) {
		this.pageEndIndex = pageEndIndex;
	}

	public int getPageStart() {
		return pageStart;
	}

	public void setPageStart(int pageStart) {
		this.pageStart = pageStart;
	}

	public int getPageEnd() {
		return pageEnd;
	}

	public void setPageEnd(int pageEnd) {
		this.pageEnd = pageEnd;
	}

	public int getLineStart() {
		return lineStart;
	}
	
	public int getTitleLineStart() {
		return lineTitleStart;
	}

	public void setLineStart(int lineStart) {
		this.lineStart = lineStart;
	}

	public int getLineEnd() {
		return lineEnd;
	}

	public void setLineEnd(int lineEnd) {
		this.lineEnd = lineEnd;
	}

	public double getLeftTopX() {
		return leftTopX;
	}

	public void setLeftTopX(double leftTopX) {
		this.leftTopX = leftTopX;
	}

	public double getLeftTopY() {
		return leftTopY;
	}

	public void setLeftTopY(double leftTopY) {
		this.leftTopY = leftTopY;
	}

	public double getRightBottomX() {
		return rightBottomX;
	}

	public void setRightBottomX(double rightBottomX) {
		this.rightBottomX = rightBottomX;
	}

	public double getRightBottomY() {
		return rightBottomY;
	}

	public void setRightBottomY(double rightBottomY) {
		this.rightBottomY = rightBottomY;
	}

	public String getPageTitle() {
		return pageTitle;
	}

	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}

	public boolean isHasParagraph() {
		return hasParagraph;
	}

	public void setHasParagraph(boolean hasParagraph) {
		this.hasParagraph = hasParagraph;
	}

	public int getChapterHierarchy() {
		return chapterHierarchy;
	}

	public void setChapterHierarchy(int chapterHierarchy) {
		this.chapterHierarchy = chapterHierarchy;
	}

	public boolean isNonContent() {
		return isNonContent;
	}

	public void setNonContent(boolean isNonContent) {
		this.isNonContent = isNonContent;
	}
	
	
	
	
}
