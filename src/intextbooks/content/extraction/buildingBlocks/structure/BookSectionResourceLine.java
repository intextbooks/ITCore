package intextbooks.content.extraction.buildingBlocks.structure;

import java.util.Vector;

public class BookSectionResourceLine {
	BookSectionType type;
	String text;
	Vector<Integer> level;
	Vector<String> levelFathers;
	
	public BookSectionResourceLine(BookSectionType type, String text, int level) {
		this.type = type;
		this.text = text;
		this.level = new Vector<Integer>();
		this.level.addElement(level);
		this.levelFathers = new Vector<String>();
	}
	
	public BookSectionResourceLine(BookSectionType type, String text, Vector<Integer> level, Vector<String> father) {
		this.type = type;
		this.text = text;
		this.level = level;
		this.levelFathers = father;
	}
	
	public BookSectionType getType() {
		return type;
	}
	public void setType(BookSectionType type) {
		this.type = type;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public Vector<Integer> getLevel(){
		return level;
	}
	public void setLevel(Vector<Integer> level) {
		this.level = level;
	}
	
	public void addLevel(int level) {
		this.level.addElement(level);
	}
	
	public Vector<String> getLevelFathers() {
		return levelFathers;
	}

	public void setLevelFathers(Vector<String> levelFathers) {
		this.levelFathers = levelFathers;
	}

	@Override
	public String toString() {
		return type + "|" + level + "|" + text + " | " + levelFathers;
	}
}
