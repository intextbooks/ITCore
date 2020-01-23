package intextbooks.content.extraction.format;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import intextbooks.content.extraction.buildingBlocks.format.ElementBlock;
import intextbooks.content.extraction.buildingBlocks.format.Line;
import intextbooks.content.extraction.buildingBlocks.format.ResourceUnit;
import intextbooks.content.extraction.buildingBlocks.format.Text;

public class FormatReasoner {

	private static float bodyTextFontSize = -1;
	
	private Map<Float, Integer> wordFontSizeCount = new HashMap<Float, Integer>();
	private Map<Float, Integer> lineFontSizeCount = new HashMap<Float, Integer>();
	
	public void pageTextGrouper(List<ResourceUnit> resource) {
		
		for(int i=0; i<resource.size(); i++){

			ArrayList<ElementBlock> groups = new ArrayList<ElementBlock>();
			ElementBlock tempGroup = null;
			
			float fontSize = 0;
			
			if(resource.get(i) != null){
				for(int j = 0; j<resource.get(i).size(); j++ ){
					
					if(fontSize == 0){
					
						tempGroup = new ElementBlock();
						tempGroup.firstLineIndx = j;
						tempGroup.fistLineY = resource.get(i).getLineAt(j).getPositionY();

						tempGroup.lastLineIndx = j;
						tempGroup.lastLineY = resource.get(i).getLineAt(j).getPositionY();
						
						tempGroup.fontSize = resource.get(i).getLineAt(j).getFontSize();
						
						groups.add(tempGroup);
						
						fontSize =  resource.get(i).getLineAt(j).getFontSize();
						
					}
					else{
						
						if(fontSize == resource.get(i).getLineAt(j).getFontSize()){
							
							tempGroup.lastLineIndx = j;
							tempGroup.lastLineY= resource.get(i).getLineAt(j).getPositionY();
							
							
						}
						else{
							
							tempGroup = new ElementBlock();
							tempGroup.firstLineIndx = j;
							tempGroup.fistLineY = resource.get(i).getLineAt(j).getPositionY();

							tempGroup.lastLineIndx = j;
							tempGroup.lastLineY = resource.get(i).getLineAt(j).getPositionY();
							
							tempGroup.fontSize = resource.get(i).getLineAt(j).getFontSize();
							
							groups.add(tempGroup);
							
							fontSize =  resource.get(i).getLineAt(j).getFontSize();
														
						}						
					}					
				}
								
				for(int j = 0; j<groups.size(); j++ ){
					
					if(groups.get(j).lastLineIndx-groups.get(j).firstLineIndx > 1){
					
						int smallest = 9999;
						ArrayList<ElementBlock> tempGroups = new ArrayList<ElementBlock>();
						
						
						for(int k = groups.get(j).firstLineIndx; k < groups.get(j).lastLineIndx; k++){
							
							if(smallest-1 > Math.abs((int)(resource.get(i).getLineAt(k).getPositionY()) -(int)( resource.get(i).getLineAt(k+1).getPositionY())) )							
								smallest = Math.abs((int)(resource.get(i).getLineAt(k).getPositionY()) -(int)( resource.get(i).getLineAt(k+1).getPositionY()));
						
						}
						
						for(int k = groups.get(j).firstLineIndx; k <= groups.get(j).lastLineIndx; k++){
							
							if(k == groups.get(j).firstLineIndx){
								
								tempGroup = new ElementBlock();
								tempGroup.firstLineIndx = k;
								tempGroup.fistLineY = resource.get(i).getLineAt(k).getPositionY();

								tempGroup.lastLineIndx = k;
								tempGroup.lastLineY = resource.get(i).getLineAt(k).getPositionY();
								
								tempGroup.fontSize = resource.get(i).getLineAt(k).getFontSize();
								
								tempGroups.add(tempGroup);
								
								fontSize =  resource.get(i).getLineAt(k).getFontSize();
								
							}else{
								
								if(smallest+1 >= Math.abs( (int)(resource.get(i).getLineAt(k-1).getPositionY()) -(int)( resource.get(i).getLineAt(k).getPositionY()))){
										
									tempGroup.lastLineIndx = k;
									tempGroup.lastLineY= resource.get(i).getLineAt(k).getPositionY();	
								
								}else{
										
									tempGroup = new ElementBlock();
									tempGroup.firstLineIndx = k;
									tempGroup.fistLineY = resource.get(i).getLineAt(k).getPositionY();
	
									tempGroup.lastLineIndx = k;
									tempGroup.lastLineY = resource.get(i).getLineAt(k).getPositionY();
										
									tempGroup.fontSize = resource.get(i).getLineAt(k).getFontSize();
										
									tempGroups.add(tempGroup);
										
									fontSize =  resource.get(i).getLineAt(k).getFontSize();		
									
								}								
							}
						}	
						
						groups.get(j).subGroup = tempGroups;
					}
				}
				
				resource.get(i).setGroups(groups);
			}
			
		}

	}
	
	public Vector<ResourceUnit> trimPossiblyUnrelated(Vector<ResourceUnit> resource){

		List<Line> listToRemove = new ArrayList<Line> ();
		Vector<ResourceUnit> temp =  new Vector<ResourceUnit> (resource) ;
		
		for(int i=0; i<resource.size(); i++){
			if(resource.get(i)!=null){
				for(int j=0; j<resource.get(i).size(); j++){
					if(resource.get(i).getLineAt(j)!= null && resource.get(i).getLineAt(j).getFontSize() < bodyTextFontSize){
						listToRemove.add(resource.get(i).getLineAt(j));					
					}
				}
				temp.get(i).getLines().removeAll(listToRemove);	
			}
		}

		return temp;
	}
	
	public Vector<ResourceUnit> trimDefaultFont(Vector<ResourceUnit> resource){
		
		List<Line> listToRemove = new ArrayList<Line> ();
		Vector<ResourceUnit> temp =  new Vector<ResourceUnit> (resource) ;

		for(int i=0; i<resource.size(); i++){
			if(resource.get(i)!=null){
				for(int j=0; j<resource.get(i).size(); j++){
					if(resource.get(i).getLineAt(j)!= null && resource.get(i).getLineAt(j).getFontSize() <= bodyTextFontSize){
						listToRemove.add(resource.get(i).getLineAt(j));					
					}
				}
				temp.get(i).getLines().removeAll(listToRemove);	
			}
		}

		return temp;
	}

	
public Vector<ResourceUnit> trimDefaultNonBoldFont(Vector<ResourceUnit> resource){
		
		List<Line> listToRemove = new ArrayList<Line> ();
		Vector<ResourceUnit> temp =  new Vector<ResourceUnit> (resource) ;
		
		for(int i=0; i<resource.size(); i++){
			if(resource.get(i)!=null){
				for(int j=0; j<resource.get(i).size(); j++){
					if(resource.get(i).getLineAt(j)!= null && resource.get(i).getLineAt(j).getFontSize() <= bodyTextFontSize
							&&  !resource.get(i).getLineAt(j).isBold()){
						listToRemove.add(resource.get(i).getLineAt(j));					
					}
				}
				temp.get(i).getLines().removeAll(listToRemove);	
			}
		}

		return temp;
	}

	//count the number of time each font appears in the texts
	public void checkFontSize(List <Text> words){		

		for(int i=0; i<words.size(); i++){

			if( words.get(i) != null){

				float fontSize  = words.get(i).getFontSize();
				Integer n = wordFontSizeCount.get(fontSize);

				n = (n == null) ? 1 : ++n;

				wordFontSizeCount.put(fontSize, n);
			}
		}		
	}
	
	public float getTextBodyFontSize(){

		if(bodyTextFontSize == -1){

			Map.Entry<Float, Integer> maxEntry = null;

			for (Map.Entry<Float, Integer> entry :wordFontSizeCount.entrySet())
			{
			    if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
			    {
			        maxEntry = entry;
			    }
			}
		
			bodyTextFontSize = maxEntry.getKey();
		
		}

		return bodyTextFontSize;		
	}
	
	public void checkLineFontSizes(List <Line> lines){
		
		
		if(lines != null)
		for(int i=0; i<lines.size(); i++){
			
			float fontSize  = lines.get(i).getFontSize();
			Integer n = lineFontSizeCount.get(fontSize);

			n = (n == null) ? 1 : ++n;

			lineFontSizeCount.put(fontSize, n);
		}		
	}


}
