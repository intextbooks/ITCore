package intextbooks.content.extraction.structure;

import java.util.ArrayList;
import java.util.List;

public class ColumnsFormat{
	int numberOfColumns;
	List<Integer> startOfColumns;
	
	public ColumnsFormat() {	
		numberOfColumns = 1;
		startOfColumns = new ArrayList<Integer>();
		startOfColumns.add(0);
	}
	
	public void addColumn(Float posX) {
		numberOfColumns++;
		startOfColumns.add(posX.intValue());
	}
	
	public int getNumberOfColumns() {
		return this.numberOfColumns;
	}
	
	public int getStartOfColumn(int column) {
		return this.startOfColumns.get(column);
	}
	
	public List<Integer> getStartOfColumns() {
		return this.startOfColumns;
	}
	
}
