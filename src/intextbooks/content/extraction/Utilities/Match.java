package intextbooks.content.extraction.Utilities;

import java.util.ArrayList;
import java.util.Iterator;

import intextbooks.content.extraction.buildingBlocks.format.Text;

public class Match { 
	
	private ArrayList<Text> words = new ArrayList<Text>();
	private String text;
	private int length = 0;
	private double matchRatio=0;
	
	public void appendWord(Text word, double similarityValue){
		
		length++;
		matchRatio+=similarityValue;
		words.add(word);
	}
	
	public void prependWord(Text word){
		
		words.add(words.size()-1, word);
	}  
	
	public ArrayList<Text> getMatches(){
		
		return this.words;
	}
	
	public void sortWords(){
		
		Text swap;
		
		for (int c = 0; c < (words.size() - 1 ); c++) {
			for (int d = 0; d < words.size() - c - 1; d++) {   	  

					if(words.get(d).getStartPositionX() > words.get(d+1).getStartPositionX() && words.get(d).getPositionY() == words.get(d+1).getPositionY()){
						
						swap   = words.get(d);
						words.add(d, words.get(d+1));
						words.remove(d+1);
						words.add(d+1, swap);
						words.remove(d+2);
					}
			}
		}		
	}
	
	public void setText(String text){		
		this.text = text;
	}
	
	public String getText(){


		if(this.text == null){

			String text = "";

			Iterator<Text> iterator = words.iterator();

			while(iterator.hasNext()){

				text += iterator.next().getText();
			}
		this.text = text;
		
		}
		
		return this.text;

	}
	
	public int size(){
		
		return  words.size();
		
	}
	
	public double getMatchRatio() {
		return this.matchRatio;
	}

}