package intextbooks.content.extraction.buildingBlocks.structure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class IndexTermMatch {
	int segmentID;
	Set<String> readingOrder;
	Set<String> nounString;
	Set<String> bookString;
	boolean full;
	List<String> POS;
	List<String> NER;
	int priority;
	
	public IndexTermMatch() {
		readingOrder = new HashSet<String>();
		nounString = new HashSet<String>();
		bookString = new HashSet<String>();
		POS = new ArrayList<String>();
		NER = new ArrayList<String>();	
	}

	public int getSegmentID() {
		return segmentID;
	}

	public void setSegmentID(int segmentID) {
		this.segmentID = segmentID;
	}

	public Set<String> getReadingOrder() {
		return readingOrder;
	}
	
	public String getReadingOrderString() {
		if(readingOrder.size() > 0) {
			return (String) readingOrder.toArray()[0];
		} else {
			return null;
		}
	}

	public void addReadingOrder(String readingOrder) {
		this.readingOrder.add(readingOrder);
	}

	public Set<String> getNounString() {
		return nounString;
	}

	public void addNounString(String nounString) {
		this.nounString.add(nounString);
	}

	public Set<String> getBookString() {
		return bookString;
	}

	public void addBookString(String bookString) {
		Iterator<String> iterator = this.bookString.iterator();
		while(iterator.hasNext()) {
			String tmp = iterator.next();
			if(tmp.toLowerCase().contains(bookString.toLowerCase().trim()))
				return;
		}
		this.bookString.add(bookString);
	}

	public boolean isFull() {
		return full;
	}

	public void setFull(boolean full) {
		this.full = full;
	}

	public List<String> getPOS() {
		return POS;
	}
	
	public void addPOS(String pOS) {
		POS.add(pOS);
	}

	public List<String> getNER() {
		return NER;
	}

	public void addNER(String nER) {
		NER.add(nER);
	}
	
	public int getPriority() {
		return this.priority;
	}
	
	public void setPriority(int priority) {
		this.priority = priority;
	}
	
	public int getNumberOfCorrrectResults() {
		return this.readingOrder.size();
	}

	@Override
	public String toString() {
		return "IndexTermMatch [segmentID=" + segmentID + ", readingOrder=" + readingOrder + ", nounString="
				+ nounString + ", bookString=" + bookString + ", full=" + full + ", POS=" + POS + ", NER=" + NER
				+ ", priority=" + priority + "]";
	}
	
	
}
