package intextbooks.content.models.structure;

import java.util.ArrayList;
import java.util.HashMap;

import intextbooks.content.ContentManager;

public class Segment {
	private int id;
	private String content;
	private String type;
	private int parentId;
	private int level;
	private ArrayList<Segment> children = new ArrayList<Segment>();
	private int startPageIndex = -1;
	private int endPageIndex = -1; 
	private CoordinatesSegment coordinates;
	private int pageNumber=0;
	private String title = "";
	
	/**
	 * @param id
	 * @param content
	 * @param type
	 * @param parentId
	 * @param level
	 * @param startIndex
	 * @param endIndex
	 */
	// used by extractor
	public Segment(int id, String content, String type, int parentId, int level, int startIndex, int endIndex, CoordinatesSegment coords, int pageNumber, String title) {
		super();
		this.id = id;
		this.content = content;
		this.type = type;
		this.parentId = parentId;
		this.level = level;
		this.startPageIndex = startIndex;
		this.endPageIndex = endIndex;
		this.coordinates = coords;
		this.pageNumber = pageNumber;
		this.title = title;
	}
	
	// used by persistence load
	public Segment(int id, String content, String type, int parentId, int level, int startIndex, int endIndex, int pageNumber, String title) {
		super();
		this.id = id;
		this.content = content;
		this.type = type;
		this.parentId = parentId;
		this.level = level;
		this.startPageIndex = startIndex;
		this.endPageIndex = endIndex;
		this.pageNumber = pageNumber;
		this.title = title;
	}
	/**
	 * @param id
	 * @param content
	 * @param type
	 * @param parentId
	 * @param level
	 */
	//used to create the book node itself
	public Segment(int id, String content, String type, int parentId, int level) {
		super();
		this.id = id;
		this.content = content;
		this.type = type;
		this.parentId = parentId;
		this.level = level;
	}

	public void addStartPage(int pageIndex){
		this.startPageIndex = pageIndex;
	}
	
	public void addEndPagePage(int pageIndex){
		this.endPageIndex = pageIndex;
	}
	
	
	public ArrayList<Segment> getChildren() {
		return this.children;
	}

	public int getId() {
		return this.id;
	}

	public void addChild(Segment segment) {
		this.children.add(segment);
		
	}

	public int getParent() {
		return this.parentId;
	}

	public String getContent() {
		
		if(this.type.equals("paragraph"))
			return this.content; //we have to changed that to the actual file content 
		else	
			return this.content;
	}
	
	public String getTitle(){
		return this.title;  
	}

	public String getType() {
		return this.type;
	}

	public int getLevel() {
		return this.level;
	}
	
	public int getPageNumber(){
		return this.pageNumber;
	}
	
	public int getStartPageIndex(){
		return this.startPageIndex;
	}
	
	public int getEndPageIndex(){
		return this.endPageIndex;
	}
	
	
	public HashMap<String, Double> getPosition(){
		return this.coordinates.getCoordinates();
	}
	
	public String getLeftTopPosition(){
		return String.valueOf(this.coordinates.getLeftTopX() +"," +String.valueOf(this.coordinates.getLeftTopY()));
	}
	
	public String getRightBottomPosition(){
		return String.valueOf(this.coordinates.getRightBottomX() +"," +String.valueOf(this.coordinates.getRightBottomY()));
	}
	

}
