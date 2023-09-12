package intextbooks.content.extraction.buildingBlocks.structure;

public class NodeDataContainer {

	private String title;
	private int hierarchyLevel;
	private int pageNumberStart;
	private int pageNumberEnd;
	private int sectionNumber;
	private boolean isSection = false;
	private boolean isErratum = false;
	private String titleNextNodeHierarchy;
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public int getHierarchyLevel() {
		return hierarchyLevel;
	}
	public void setHierarchyLevel(int hierarchyLevel) {
		this.hierarchyLevel = hierarchyLevel;
	}
	public int getPageNumberStart() {
		return pageNumberStart;
	}
	public void setPageNumberStart(int pageNumberStart) {
		this.pageNumberStart = pageNumberStart;
	}
	public int getPageNumberEnd() {
		return pageNumberEnd;
	}
	public void setPageNumberEnd(int pageNumberEnd) {
		this.pageNumberEnd = pageNumberEnd;
	}
	public int getSectionNumber() {
		return sectionNumber;
	}
	public void setSectionNumber(int sectionNumber) {
		this.sectionNumber = sectionNumber;
	}
	
	public boolean isSection() {
		return this.isSection;
	}
	
	public void setSection(boolean isSection) {
		this.isSection = isSection;
	}
	
	public boolean isErratum() {
		return this.isErratum;
	}
	
	public void setErratum(boolean isErratum ) {
		this.isErratum = isErratum ;
	}
	
	public String getTitleNextNodeHierarchy() {
		return titleNextNodeHierarchy;
	}
	public void setTitleNextNodeHierarchy(String titleNextNodeHierarchy) {
		this.titleNextNodeHierarchy = titleNextNodeHierarchy;
	}
	
	
}
