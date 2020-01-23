package intextbooks.content.extraction.buildingBlocks.format;

import java.util.ArrayList;
import java.util.List;

public class Slide extends ResourceUnit {
	
	private Line headingLine=null;
	
	private byte headingStartLine;
	private byte headingEndLine;
	private String headingText="";
	
	private byte bodyStartLine;
	private byte bodyEndLine;
	private String bodyText="";
	
	public Line getHeadingLine() {
		return headingLine;
	}

	public void setHeadingLine(Line headingLine) {
		this.headingLine = headingLine;
	}

	public String getSlideHeading(){
		return this.headingText;
	}
	
	public String getSlideBody(){
		return this.bodyText;
	}
	
	public int getSlideIndex() {
		return pageIndex;
	}

	public void setSlideIndex(int slideIndex) {
		this.pageIndex = slideIndex;
	}
	
	public void setWholeSlide(List<Line> l){
		
		this.lines=l;
	}
		
	 public void bubblesrt()
	  {
	        Line temp;
	        if (this.lines.size()>1) // check if the number of orders is larger than 1
	        {
	            for (int x=0; x<this.lines.size(); x++) // bubble sort outer loop
	            {
	                for (int i=0; i < this.lines.size()- x - 1; i++) {
	                    if (this.lines.get(i).getPositionY() > this.lines.get(i+1).getPositionY()){
	                        temp = this.lines.get(i);
	                        this.lines.set(i,this.lines.get(i+1) );
	                        this.lines.set(i+1, temp);
	                    }
	                }
	            }
	        }

	  }
	 
	public void identfyHeadingBody(){
		
		bubblesrt();
		
		float fontSize = lines.get(0).fontSize;
		headingStartLine = 0;
		headingEndLine =  0;
		
		if(lines.size() == 1){
			headingStartLine = 0;
			headingEndLine= 0;
			
			fontSize = lines.get(0).fontSize;
			
			headingText+=lines.get(0).getText();
		}
		else		
		 for(byte i=1; i<lines.size(); i++){
			
			if(fontSize < lines.get(i).fontSize){
				
				headingStartLine = i;
				headingEndLine= i;
				
				fontSize = lines.get(i).fontSize;
			}
			else if(fontSize == lines.get(i).fontSize){
				
				headingEndLine = i;
				
			}
			else{
				
				bodyStartLine = (byte) (headingEndLine+1);
				bodyEndLine = (byte) lines.size();
				
				for(byte j = headingStartLine; j<=headingEndLine; j++)
					headingText+=lines.get(j).getText()+" ";
				
				for(byte k = bodyStartLine; k<bodyEndLine; k++){
					bodyText+=lines.get(k).getText()+" ";
					
				}
					break;
			}			
		}		
		
		headingLine = new Line(lines.get(headingStartLine));
		headingLine.setText(headingText);
	}
}
