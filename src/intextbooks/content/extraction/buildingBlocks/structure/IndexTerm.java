package intextbooks.content.extraction.buildingBlocks.structure;

import java.util.ArrayList;

public class IndexTerm {

	private String ID;
	private ArrayList <Integer> pageNum = new ArrayList<Integer> ();
	private ArrayList <Integer> pageIndx = new ArrayList<Integer>();
	private String parent = null ; 
	
	public IndexTerm(){
		
	}
	
	public IndexTerm(String name, int pageNumber, int pageIndex){
		
		ID = name;
		pageNum.add(pageNumber);
		pageIndx.add(pageIndex);
		
	}

	public void setID(String name){
		
		this.ID = name;
	}
	
	public String getID(){
		
		return this.ID;
	}
	
	public void addAPageNumber(int pageNum){
		
		this.pageNum.add(pageNum);
	}
	
	public void addAPageIndex(int pageNum){
		
		this.pageIndx.add(pageNum);
	}
	
	public int getAPageNumber(int position){
		
		if(position < this.pageNum.size() && position >= 0 ){
			
			return pageNum.get(position);
		}
		else{
			return -1;
		}		
	}
	
	public int getAPageIndex(int position){
		
		if(position < this.pageIndx.size() && position >= 0 ){
			
			return pageIndx.get(position);
		}
		else{
			return -1;
		}		
	}
	
	public void setPageNumbers(ArrayList <Integer> list ){
		
		this.pageNum = list;
	}
	
	public void setPageIndicies(ArrayList <Integer> list ){
		
		this.pageIndx = list;
	}
	
	public ArrayList <Integer> getPageNumbers(){
		
		return this.pageNum;
	}

	public ArrayList <Integer> getPageIndicies(){
		
		return this.pageIndx;
	}
	
	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	@Override
	public String toString() {
		return "IndexTerm [ID=" + ID + ", pageNum=" + pageNum + ", pageIndx=" + pageIndx + ", parent=" + parent + "]";
	}
}
