package intextbooks.content.extraction.structure;

import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;

import intextbooks.SystemLogger;
import intextbooks.content.extraction.buildingBlocks.format.Line;
import intextbooks.content.extraction.buildingBlocks.format.Text;

public class ColumnedPage {
	
	//one column is "Vector<Line>"
	private Vector<Vector<Line>> columns;
	private Vector<SortedSet<Float>> positions;
	private Vector<HashMap<Integer,Float>> maps;
	
	public ColumnedPage() {
		columns = new Vector<Vector<Line>>();
		positions = new Vector<SortedSet<Float>> ();
		maps = new Vector<HashMap<Integer,Float>>();
	}
	
	public Vector<Vector<Line>> getColumns(){
		return columns;
	}
	
	public void removeLine(Line l) {
		for(int i = 0; i < columns.size(); i++) {
			Iterator<Line> lines = columns.get(i).iterator();
			while(lines.hasNext()) {
				Line line = lines.next();
				if(line.getText().equals(l.getText())) {
					lines.remove();
				}
			}
		}
	}
	
	public Vector<Line> getAllLines(){
		Vector<Line> lines = new Vector<Line>();
		for(int i = 0; i < columns.size(); i++) {
			for(Line line : columns.get(i)) {
				lines.add(line);
			}
		}
		return lines;
	}
	
	public Vector<SortedSet<Float>> getPositions(){
		return positions;
	}
	
	public Vector<HashMap<Integer,Float>> getMaps(){
		return maps;
	}
	
	public void addColumn(Vector<Line> column) {
		//1. add column
		columns.add(column);
		
	}
	
	public void processColumns() {
		//2. process each line of the column and store each (rounded) positionX 
		for(int i = 0; i < columns.size(); i++) {
			TreeSet<Float> pos_lines = new TreeSet<Float>();
			positions.add(pos_lines);
			for(Line l: columns.get(i)) {
				float pos = roundUp(l.getStartPositionX());
				pos_lines.add(pos);
			}
			
			//3. map the position to levels(1,2,3,etc)
			HashMap<Integer,Float> map_column = new HashMap<Integer,Float>();
			maps.addElement(map_column);
			Iterator<Float> it = pos_lines.iterator();
			for(int j = 0; it.hasNext(); j++) {
				map_column.put(j, it.next());
			}
			
		}
		
	}
	
	public void ajustPositionsX(Float reference, HashMap<Integer,Float> finalMap) {
		if(columns.size() != 0 && finalMap.size() != 0) {
			for(int i = 0; i < columns.size(); i++) {
				Float mostLeft = finalMap.get(i);
				SystemLogger.getInstance().debug("fms: " + finalMap.size());
				SystemLogger.getInstance().debug("i: " + i );
				if(mostLeft != null) {
					Float columnReference = mostLeft - reference;
					for(Line line : columns.get(i)) {
						Float newStartPositionX = line.getStartPositionX() - columnReference;
						line.setStartPositionX(newStartPositionX);
						line.getWordAt(0).setStartPositionX(newStartPositionX);
					}
				}
			}
		}
		
	}
	
	public int getNumberofColumns() {
		return columns.size();
	}
	
	public Float getPositionByColumnLevel(int column, int level) {
		if(column >= columns.size()) {
			return null;
		} else {
			return maps.get(column).get(level);
		}
	}
	
	public int getMaxCantLevel() {
		int max = 0;
		Iterator<HashMap<Integer,Float>> it = maps.iterator();
		while(it.hasNext()) {
			HashMap<Integer,Float> map = it.next();
			if(map.size() > max) {
				max = map.size();
			}
		}
		return max;
	}
	
	public Float roundUp(float n) {
		return Math.round(n*10)/10f;
	}
	
	public void concantLines() {
		//#1 Concat lines where the only page number is in the next line
		for(int i = 0; i < columns.size(); i++) {
			Vector<Line> column = columns.get(i);
			//check N-1 lines
			int l = 0;
			for(; l < column.size()-1; l++) {
				Line line1 = column.get(l);
				Line line2 = column.get(l+1);
				if(line1 != null && line1.size()>0 ) {
					if(StringUtils.isNumeric(line2.getText().replaceAll("[,-]", "").replaceAll(" ",""))){

						//@i.alpizarchacon fix to the problem that first words of some lines have different (bigger) positionX that the line itself.
						float pos1 = line1.getStartPositionX();
						line1.getWordAt(0).setStartPositionX(pos1);

						//form new line
						Line groupingLine = new Line();
						groupingLine.addWords(new Vector<Text>(line1.getWords()));
						groupingLine .addWords(new Vector<Text>(line2.getWords()));
						groupingLine.extractText();

						//update line
						column.set(l, groupingLine);
						column.remove(l+1);
					}
				}
			}
			//check last line (N) if there is another column
			if ((i+i) < columns.size() && columns.get(i+1).size() >= 1) {
				Line line1 = column.get(column.size()-1);
				Line line2 = columns.get(i+1).get(0);
				if(StringUtils.isNumeric(line2.getText().replaceAll("[,-]", "").replaceAll(" ",""))){

					//@i.alpizarchacon fix to the problem that first words of some lines have different (bigger) positionX that the line itself.
					float pos1 = line1.getStartPositionX();
					line1.getWordAt(0).setStartPositionX(pos1);

					//form new line
					Line groupingLine = new Line();
					groupingLine.addWords(new Vector<Text>(line1.getWords()));
					groupingLine .addWords(new Vector<Text>(line2.getWords()));
					groupingLine.extractText();

					//@i.alpizarchacon
					//page.remove(i+1) + add changed for set;
					column.set(column.size()-1, groupingLine);
					columns.get(i+1).remove(0);
				}
				
			}
			
		}
		
		//#2 Group lines where the line term does not have a page number, and it only has one child term with page number
		for(int i = 0; i < columns.size(); i++) {
			Vector<Line> column = columns.get(i);
			//check N-2 lines
			int l = 0;
			for(; l < column.size()-2; l++) {
				Line line1 = column.get(l);
				Line line2 = column.get(l+1);
				Line line3 = column.get(l+2);
				if(line1 != null && line1.size()>0 )	{

					//check that the term does not end in a number, and that two terms ahead the term is aligned.
					//This means that one term ahead is part of the actual term
					if( !StringUtils.isNumeric(line1.getWordAt(line1.size()-1).getText().replaceAll("[,-]", ""))
							&& 	 roundUp(line1.getStartPositionX()) >= roundUp(line3.getStartPositionX()) ){

						//@i.alpizarchacon fix to the problem that first words of some lines have different (bigger) positionX that the ilne itself.
						float pos1 = line1.getStartPositionX();
						line1.getWordAt(0).setStartPositionX(pos1);

						//form new line
						Line groupingLine = new Line();
						groupingLine.addWords(new Vector<Text>(line1.getWords()));
						groupingLine .addWords(new Vector<Text>(line2.getWords()));
						groupingLine.extractText();
						
						//update line
						column.set(l, groupingLine);
						column.remove(l+1);

					}
				}
			}
			//check N-1 line if there is another column
			if ((i+i) < columns.size() && columns.get(i+1).size() >= 1) {
				Line line1 = column.get(column.size()-2);
				Line line2 = column.get(column.size()-1);
				Line line3 = columns.get(i+1).get(0);
				if(line1 != null && line1.size()>0 )	{

					//check that the term does not end in a number, and that two terms ahead the term is aligned.
					//This means that one term ahead is part of the actual term
					if( !StringUtils.isNumeric(line1.getWordAt(line1.size()-1).getText().replaceAll("[,-]", ""))
							&& 	 roundUp(line1.getStartPositionX()) >= roundUp(line3.getStartPositionX()) ){

						//@i.alpizarchacon fix to the problem that first words of some lines have different (bigger) positionX that the ilne itself.
						float pos1 = line1.getStartPositionX();
						line1.getWordAt(0).setStartPositionX(pos1);

						//form new line
						Line groupingLine = new Line();
						groupingLine.addWords(new Vector<Text>(line1.getWords()));
						groupingLine .addWords(new Vector<Text>(line2.getWords()));
						groupingLine.extractText();
						
						//update line
						column.set(column.size()-2, groupingLine);
						column.remove(column.size()-1);

					}
				}
			}
			//check last line (N) if there is another column
			if ((i+i) < columns.size() && columns.get(i+1).size() >= 2) {
				Line line1 = column.get(column.size()-1);
				Line line2 = columns.get(i+1).get(0);
				Line line3 = columns.get(i+1).get(1);
				if(line1 != null && line1.size()>0 )	{

					//check that the term does not end in a number, and that two terms ahead the term is aligned.
					//This means that one term ahead is part of the actual term
					if( !StringUtils.isNumeric(line1.getWordAt(line1.size()-1).getText().replaceAll("[,-]", ""))
							&& 	 roundUp(line1.getStartPositionX()) >= roundUp(line3.getStartPositionX()) ){

						//@i.alpizarchacon fix to the problem that first words of some lines have different (bigger) positionX that the ilne itself.
						float pos1 = line1.getStartPositionX();
						line1.getWordAt(0).setStartPositionX(pos1);

						//form new line
						Line groupingLine = new Line();
						groupingLine.addWords(new Vector<Text>(line1.getWords()));
						groupingLine .addWords(new Vector<Text>(line2.getWords()));
						groupingLine.extractText();
						
						//update line
						column.set(column.size()-1, groupingLine);
						columns.get(i+1).remove(0);
					}
				}
			}
		}
		
//		/*TESTING*/
//		SystemLogger.getInstance().log("-----checking concat#1&#2---------");
//		for(int i = 0; i < columns.size(); i++) {
//			Vector<Line> column = columns.get(i);
//			SystemLogger.getInstance().log("> COLUMN: " + i);
//			int l = 0;
//			for(; l < column.size(); l++) {
//				SystemLogger.getInstance().log(">>>: " + column.get(l).getText());
//			}
//		
//		}
//		/*TESTING*/
		
	}
	
	public static void main (String args[]) {
		Float a= 282.56783f;
		Float pos = Math.round(a*100)/100f;
		Float pos2 = Math.round(a*10)/10f;
		System.out.println(pos);
		System.out.println(pos2);
	}
	
	public void print() {
		int i = 1;
		System.out.println("*************** C *******************");
		for(Vector<Line> c : columns) {
			System.out.println(">> Column #" + i++);
			for(Line l: c) {
				System.out.println(l.getText());
			}
		}
	}
 
}
