package intextbooks.content.extraction.Utilities;

import java.util.List;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;

import intextbooks.content.extraction.buildingBlocks.format.Line;
import intextbooks.content.extraction.buildingBlocks.format.Text;

public class LineEndPageNumber {

	
	public static Vector <Line> indexConcatLines(Vector <Line> page){
		
		for(int i = 0 ; i < page.size()-1 ; i++){
			
			if(!StringUtils.isNumeric(page.get(i).getWordAt(page.get(i).size()-1).getText())){
				
				short depth=1;
				
				
				if(StringUtils.isNumeric(page.get(i+1).getText().replaceAll(",", "").replaceAll(" ",""))){
		
					Line groupingLine = new Line();
					
					groupingLine.addWords(new Vector<Text>(page.get(i).getWords()));
					
					groupingLine .addWords(new Vector<Text>(page.get(i+1).getWords()));
					
					groupingLine.extractText();
					
					page.remove(i+1);
					
					page.add(i+1, groupingLine);
					
					page.remove(i);
					
				}
				
				if(!BoundSimilarity.isInBound(page.get(i).getStartPositionX(),page.get(i+1).getStartPositionX(),
						page.get(i).getFontSize(),page.get(i+1).getFontSize(),0.6f)){
					
					while(i+depth<page.size()-1
							&& !StringUtils.isNumeric(page.get(i+depth).getWordAt(page.get(i+depth).size()-1).getText().replaceAll(",", ""))
							&&  BoundSimilarity.isInBound(page.get(i+depth).getStartPositionX(),page.get(i+depth+1).getStartPositionX(),5,5,0.6f)
							){
						
						depth++;						
					}
					
					
				}
				
				

				
				
				
				
				
				
			}
			else if(BoundSimilarity.isInBound(page.get(i).getStartPositionX(),
					page.get(i+1).getStartPositionX(),page.get(i).getFontSize(),page.get(i+1).getFontSize(),0.6f)){
				
			}
			
///////////////////////////////////////////////////////
			
			
			if(page.get(i) != null && page.get(i).size()>0 )		
				if(!StringUtils.isNumeric(page.get(i).getWordAt(page.get(i).size()-1).getText())){
					
					short depth=1;
					
					while(i+depth<page.size()-1
							&& !StringUtils.isNumeric(page.get(i+depth).getWordAt(page.get(i+depth).size()-1).getText().replaceAll(",", ""))
							&&  BoundSimilarity.isInBound(page.get(i+depth).getStartPositionX(),page.get(i+depth+1).getStartPositionX(),5,5,0.6f)
							){
						
						depth++;						
					}
					
					short counter = 1;
					
					while(depth > 0){
						
						Line groupingLine = new Line();
						
						groupingLine.addWords(new Vector<Text>(page.get(i).getWords()));
						
						groupingLine .addWords(page.get(i+counter).getWords());
						
						groupingLine.extractText();
						
						page.remove(i+counter);
						
						page.add(i+counter, groupingLine);
						
						page.remove(i);
						
						counter++;
						depth --;
					}
					
			}			
		}
		
		return page;
	}
}
