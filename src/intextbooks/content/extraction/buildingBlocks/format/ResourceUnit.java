package intextbooks.content.extraction.buildingBlocks.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import intextbooks.content.extraction.Utilities.BoundSimilarity;
import intextbooks.content.models.formatting.CoordinatesContainer;
import intextbooks.content.models.formatting.FormattingContainer;
import intextbooks.content.models.formatting.PageMetadataEnum;

public class ResourceUnit extends Text {
	
	
	protected ArrayList<ElementBlock> groups = new ArrayList<ElementBlock> ();
	
	protected float mostLeft=-1;
	protected float mostRight=-1;

	protected List <Line> lines = new ArrayList<Line> ();
	protected List <Paragraph> paragraphs = new ArrayList<Paragraph> ();
	
	protected int pageIndex = 0;
	protected int pageNumber = -1;
	protected boolean specialPageNumbering = false;

	protected ArrayList<ArrayList<CoordinatesContainer>> coordMap = new ArrayList<ArrayList<CoordinatesContainer>>();
	protected ArrayList<ArrayList<Integer>> formatMap = new ArrayList<ArrayList<Integer>>(); 
	protected HashMap<String, FormattingContainer> dictEntries = new HashMap<String,FormattingContainer>();
	protected HashMap<PageMetadataEnum,String> metadata = new HashMap<PageMetadataEnum,String>();

	
	public void setSpecialPageNumbering(boolean val) {
		this.specialPageNumbering = val;
	}
	
	public boolean getSpecialPageNumbering() {
		return this.specialPageNumbering;
	}

	/**
	 * 
	 * @return
	 */
	public int getPageNumber() {
		return pageNumber;
	}
	/**
	 * 
	 * @param pageNumber
	 */
	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}
	/**
	 * 
	 * @param groups
	 */
	public void setGroups(ArrayList<ElementBlock> groups){
		
		this.groups = new ArrayList<ElementBlock>();
		
		for (ElementBlock group : groups)
			this.groups.add(group.clone());
		
	}
	/**
	 * 
	 * @return
	 */
	public ArrayList<ElementBlock> getGroups(){
		return this.groups;
	}
	
	public void addParagraph(Paragraph p) {
		this.paragraphs.add(p);
	}
	
	public void extractLines() {
		for(Paragraph p: this.paragraphs) {
			p.clean();
			this.lines.addAll(p.getLines());
		}
			
		for(Line line : lines) {
			line.extractText();
		}
	}
	
	/**
	 * 
	 * @param l
	 */
	public void addLine(Line l){	
		lines.add(l);
	}
	/**
	 * 
	 * @param l
	 */
	public void addLines(List<Line> l){
		this.lines.addAll(l);
	}
	/**
	 * 	
	 * @param l
	 */
	public void setLines(List<Line> l){
		this.lines=l;
	}
	/**
	 * 	
	 * @param pageIndex
	 * @param l
	 */
	public void replaceLineAt(int pageIndex, Line l){

		lines.add(pageIndex,l);
		lines.remove(pageIndex+1);
	}
	/**
	 * 	
	 * @param pageIndex
	 */
	public void removeLineAt(int pageIndex){
		lines.remove(pageIndex);
	}
	/**
	 * 	
	 * @param pageIndex
	 * @return
	 */
	public Line getLineAt(int pageIndex){
		return lines.get(pageIndex);
	}
	/**
	 * 	
	 * @return
	 */
	public List<Line> getLines(){
		return lines;
	}
	
	public List<Line> getLinesView(int start, int end){
		List<Line> view = new ArrayList<Line>();
		for(int i = start; i <= end; i++) {
			view.add(this.lines.get(i));
		}
		return view;
	}
	
	public void removeFirstLines(int numberOfLines) {
		Iterator<Line> iterator =  lines.iterator();
		while(iterator.hasNext() && numberOfLines >0) {
			iterator.next();
			iterator.remove();
			numberOfLines--;
		}
	}
	/**
	 * 	
	 * @param pageIndex
	 */
	public void setPageIndex(int pageIndex){
		this.pageIndex=pageIndex;
	}
	/**
	 * 	
	 * @return
	 */
	public int getPageIndex(){
		return this.pageIndex;
	}
	/**
	 * 
	 * @return
	 */
	public ArrayList<ArrayList<Integer>> getFormatMap(){
		
		return formatMap;
	}
	/**
	 * 
	 * @return
	 */
	public ArrayList<ArrayList<CoordinatesContainer>> getCoordsMap(){
		
		return coordMap;
	}
	/**
	 * 
	 * @return
	 */
	public HashMap<String, FormattingContainer> getDictEntries(){
		
		return dictEntries;
	}
	/**
	 * 
	 * @return
	 */
	public HashMap<PageMetadataEnum,String> getPageMetadata(){
		
		return metadata;
	}
	/**
	 * 
	 * @return
	 */
	public float getPageMostLeftX(){
		
		return mostLeft;
	}
	/**
	 * 
	 * @return
	 */
	public float getPageMostRight(){

		return mostRight;
	}	
	/**
	 * 	
	 */
	@Override
	public int size(){

		if(this.lines!=null && this.lines.size()>0)
			return this.lines.size();

		return 0;		
	}
	
	public void printStylesTable() {
		
		TreeSet<FormattingContainer> format = new TreeSet<FormattingContainer>(new FormattingContainer());
		format.addAll(dictEntries.values());
		System.out.println("----------------------");
		System.out.println("total fonts: " + dictEntries.size());
		for(FormattingContainer fC: dictEntries.values()) {
			System.out.println(fC.toString());
		}
		System.out.println("----------------------");
		Iterator<FormattingContainer> it = format.descendingIterator();
		System.out.println("total fonts it: " + format.size());
		while(it.hasNext()) {
			FormattingContainer fC = it.next();
			System.out.println("////////////////////////////////");
			System.out.println(fC.toString());
		}
	}
	
	//TESTING
	public void extractFormatData(ArrayList<String> temp1){
		int cant = 0;
		//each line of the page
		for(int j=0 ; j<this.getLines().size() ; j++){

			ArrayList<Integer> line = new ArrayList<Integer>();
			ArrayList<CoordinatesContainer> lineCoords = new ArrayList<CoordinatesContainer>();
			
			//each word of each line
			for(int k=0 ; k< this.getLineAt(j).getWords().size() ; k++){
				
				Text temp = this.getLineAt(j).getWordAt(k);
				
				FormattingContainer format = new  FormattingContainer(temp.getFontName(),
						(byte)temp.getFontSize(),0,
						temp.isBold(), temp.isItalic(), temp.getFontColor().getComponents());
				
				if(temp.getFontSize() == 1200) {
					System.out.println("19: " + temp.getText());
					//System.out.println("> -1: " +this.getLineAt(j-1).getText());
					System.out.println(">  0: " +this.getLineAt(j).getText());
					System.out.println("> +1: " +this.getLineAt(j+1).getText());
					System.out.println("all");
					System.out.println(this.getText());
					System.exit(0);
				}
				if(!temp.isBold() && temp.isItalic()) {
					//temp1.add(temp.txt);
				}
				
				CoordinatesContainer coords = new CoordinatesContainer(temp.getStartPositionX(), temp.getPositionY()+temp.getFontSize(),
						temp.getEndPositionX(),temp.getPositionY());
				
				
				if(!dictEntries.containsKey(format.getKeySum().toString())){
					dictEntries.put(format.getKeySum().toString(), format);
					cant++;
				} else {
					dictEntries.get(format.getKeySum().toString()).incrementFreq();
					cant++;
				}
				
				lineCoords.add(coords);
				line.add(format.getKeySum());
			}
			
			coordMap.add(lineCoords);
			formatMap.add(line);
		}
		temp1.add(String.valueOf(cant));
		metadata.put(PageMetadataEnum.PageIndex, String.valueOf(this.pageIndex));	
	}
	
	/**
	 * 	
	 */
	public void extractFormatData(){
		ArrayList<String> temp1 = new ArrayList<String>();

		//each line of the page
		for(int j=0 ; j<this.getLines().size() ; j++){

			ArrayList<Integer> line = new ArrayList<Integer>();
			ArrayList<CoordinatesContainer> lineCoords = new ArrayList<CoordinatesContainer>();
			
			//each word of each line
			for(int k=0 ; k< this.getLineAt(j).getWords().size() ; k++){
				
				Text temp = this.getLineAt(j).getWordAt(k);
				
				FormattingContainer format = new  FormattingContainer(temp.getFontName(),
						(byte)temp.getFontSize(),0,
						temp.isBold(), temp.isItalic(), temp.getFontColor().getComponents());
				
//				if(temp.getFontSize() == 10) {
//					System.out.println("10: " + temp.getText());
//					System.out.println("> -1: " +this.getLineAt(j-1).getText());
//					System.out.println(">  0: " +this.getLineAt(j).getText());
//					System.out.println("> +1: " +this.getLineAt(j+1).getText());
//					System.out.println("all");
//					System.out.println(this.getText());
//					System.exit(0);
//				}
				
				CoordinatesContainer coords = new CoordinatesContainer(temp.getStartPositionX(), temp.getPositionY()+temp.getFontSize(),
						temp.getEndPositionX(),temp.getPositionY());
				
				
				if(!dictEntries.containsKey(format.getKeySum().toString())){
					dictEntries.put(format.getKeySum().toString(), format);
				} else {
					dictEntries.get(format.getKeySum().toString()).incrementFreq();
				}
				
				lineCoords.add(coords);
				line.add(format.getKeySum());
			}
			
			coordMap.add(lineCoords);
			formatMap.add(line);
		}
		
		metadata.put(PageMetadataEnum.PageIndex, String.valueOf(this.pageIndex));	
	}
	/**
	 * 
	 */
	public void extractText(){
	
		this.txt = "";
		
		for(int j=0 ; j<this.getLines().size() ; j++)
			this.txt += lines.get(j).getText()+"\n";
	}
	/**
	 * 
	 */
	protected void sortLines(){
		
		if(mostLeft == -1 || mostRight == -1){
			calculateMostLeftRight();
		}
		
		Line swap ;
		
		for (int c = 0; c < ( lines.size() - 1 ); c++) {
			for (int d = 0; d < lines.size() - c - 1; d++) {   	  

					if (lines.get(d).getPositionY() > lines.get(d+1).getPositionY()
						&& (BoundSimilarity.isInBound(lines.get(d+1).getStartPositionX(), mostLeft, lines.get(d+1).getFontSize(), lines.get(d+1).getFontSize(), 0.5f)
						|| BoundSimilarity.isInBound(lines.get(d+1).getEndPositionX(), mostRight, lines.get(d+1).getFontSize(), lines.get(d+1).getFontSize(), 0.5f))){  /* For descending order use < */

					swap   = lines.get(d);
					lines.add(d, lines.get(d+1));
					lines.remove(d+1);
					lines.add(d+1, swap);
					lines.remove(d+2);
				}
			}
		}
	}
	
	//@i.alpizarchacon CHANGED there was a problem because -1 was never going to be > that a position x
	public void calculateMostLeftRight(){
		
		//added
		if (lines.size() >= 1 ) {
			mostLeft= lines.get(0).getStartPositionX();
		}
		
		for(short i=1; i< lines.size(); i++){	
			
			if(mostLeft > (float) Math.floor(lines.get(i).getStartPositionX())){
				
				mostLeft= lines.get(i).getStartPositionX();
			}
		}
	}
	
	public ArrayList<Text> getWords(){
		ArrayList<Text> currentPage = new ArrayList<Text>();
		
		for(int i= 0; i < lines.size(); i++){
    		for(int j = 0; j < lines.get(i).size(); j++){
    			
    			currentPage.add(lines.get(i).getWordAt(j));	
    		}    		
    	}
		
		return currentPage;
	}
	
	/****************************************************************************
	 * 							Convert to Child Class							*
	 ****************************************************************************/
	
	protected void setMetdata(HashMap<PageMetadataEnum,String> m ){
		this.metadata = m; 
	}
	
	protected void setCoordMap(ArrayList<ArrayList<CoordinatesContainer>> c){
		this.coordMap = c;
	}
	
	protected void setFormatMap(ArrayList<ArrayList<Integer>> f){
		this.formatMap = f;
	}
	
	protected void setDictEntries(HashMap<String, FormattingContainer> d){
		this.dictEntries = d;
	}
		
	protected void setMostRight(float r){
		this.mostRight = r;
	}
	
	protected void setMostLeft(float l){
		this.mostLeft = l;
	}

	public Page convertToPage(){
		
		Page p = new Page();
		
		p.setLines(lines);
		p.setCoordMap(coordMap);
		p.setDictEntries(dictEntries);
		p.setFormatMap(formatMap);
		p.setMetdata(metadata);
		p.setMostLeft(mostLeft);
		p.setMostRight(mostRight);
		p.setPageIndex(pageIndex);
		
		if(pageNumber!=-1){
			p.setPageNumber(pageNumber);
		}
		
		p.setText(txt);
		
		return p;		
	}

	public void print() {
		System.out.println(">>>>>>>>>>>>> page: " + this.pageIndex + " <<<<<<<<<<<<<");
		for(Paragraph p: this.paragraphs) {
			System.out.println("Paragraph ***** " + p);
			for(Line l: p.getLines()) {
				l.extractText();
				System.out.println("---- line");
				System.out.println(l.getDetailedText());
			}
		}
	}
	
	
	
}
