package intextbooks.content.extraction.buildingBlocks.format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Page extends ResourceUnit{	

	private Map <LineDataContainer,  Integer> lineDataMap = new HashMap <> ();
		
	public void pageText(){
		
		this.txt = "" ;
		
		for(int i = 0; i< this.size() ; i++){			
			this.txt+=this.getLineAt(i).getText()+"\n";		
		}
		
	}
	
	public String getContinuousPageText(){
		
		String txt = "" ;
		
		for(int i = 0; i< this.size() ; i++){	
			this.getLineAt(i).extractText();
			txt+=this.getLineAt(i).getText()+" ";		
		}
		
		return txt;
	}
	
	public void setWholePage(List<Line> l){
		
		this.lines=l;
	}
		
	public void populatePageData(){
		
		if(!lines.isEmpty()){
			mostLeft=99999;
			mostRight=0;
			
			for(short i=0; i< lines.size(); i++){				
				
				if(mostLeft > (float) Math.floor(lines.get(i).getStartPositionX())){
					
					mostLeft= lines.get(i).getStartPositionX();
				}
				
				if(mostRight < (float) Math.floor(lines.get(i).getEndPositionX())){
					
					mostRight = lines.get(i).getEndPositionX();
				}
				
				LineDataContainer data = new LineDataContainer(lines.get(i).getFontName(),lines.get(i).getFontSize(),(float) Math.floor(lines.get(i).getStartPositionX()));				
				
				Integer n = lineDataMap.get(data);
				
				n = (n == null) ? 1 : ++n;
				
				lineDataMap.put(data, n);					
			}			
		}		
	}

	@Override
	public void print() {
		System.out.println(">>>>>>>>>>>>> page: " + this.pageIndex + " <<<<<<<<<<<<<");
		for(Line l: this.getLines()) {
			l.extractText();
			System.out.println(l.getText());
		}
		
	}
	

}
