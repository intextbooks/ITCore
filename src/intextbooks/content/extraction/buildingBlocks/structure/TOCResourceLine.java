package intextbooks.content.extraction.buildingBlocks.structure;

public class TOCResourceLine {
	TOCResourceType type;
	String text;
	int level;
	
	public TOCResourceLine(TOCResourceType type, String text, int level) {
		this.type = type;
		this.text = text;
		this.level = level;
	}
	
	public TOCResourceType getType() {
		return type;
	}
	public void setType(TOCResourceType type) {
		this.type = type;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	@Override
	public String toString() {
		return type + "|" + level + "|" + text;
	}
}
