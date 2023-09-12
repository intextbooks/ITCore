package intextbooks;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import intextbooks.content.extraction.buildingBlocks.format.Page;

public class RandomAccessToElements {
	
	private static RandomAccessToElements instance = null;
	private Vector <Page> pages;
	
	protected RandomAccessToElements() throws IOException {
	}
	
	public static RandomAccessToElements getInstance() {
		if(instance == null) {
			try {
				instance = new RandomAccessToElements();
			} catch (Exception e) {
				e.printStackTrace(); 
			}
			
		}
		return instance;
	}
	
	public void setPages(Vector <Page> pages) {
		this.pages = pages;
	}
	
	public List<Page> getPages(){
		return this.pages;
	}

}
