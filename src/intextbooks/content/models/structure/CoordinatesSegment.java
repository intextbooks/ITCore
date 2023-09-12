package intextbooks.content.models.structure;

import java.util.HashMap;

public class CoordinatesSegment {

	private double leftTopX;
	private double leftTopY;
	
	private double rightBottomX;
	private double rightBottomY;
	
	/**Automatically computed coordinates**/
	
	private double leftBottomX;
	private double leftBottomY;
	
	private double rightTopX;
	private double rightTopY;
	private boolean multiple;
	
	HashMap<String, Double> coordMap = new HashMap<String,Double>();
	
	
	/**
	 * @param lTopX
	 * @param lTopY
	 * @param rBottomX
	 * @param rBottomY
	 */
	public CoordinatesSegment(double lTopX, double lTopY, 
			double rBottomX, double rBottomY, boolean multiplePages) {
		
		this.leftTopX = lTopX;
		this.leftTopY = lTopY;
		this.rightBottomX = rBottomX;
		this.rightBottomY = rBottomY;

		this.multiple = multiplePages;
		
		this.coordMap.put("leftTopX",lTopX);
		this.coordMap.put("leftTopY",lTopY);
		this.coordMap.put("rightBottomX",rBottomX);
		this.coordMap.put("rightBottomY",rBottomY);
		
		
		
		/*this.coordMap.put("leftBottomX",this.leftBottomX);
		this.coordMap.put("leftBottomY",this.leftBottomY);
		this.coordMap.put("rightTopX",this.rightTopX);
		this.coordMap.put("rightTopY",this.rightTopY);*/
		
		
		
	}
	
	/**
	 * @return returns coordinates has HashMap <float leftBottom, float leftTop, float rightBottom,float rightTop>
	 */
	public HashMap<String,Double> getCoordinates(){
		return this.coordMap;
	}
	
	public double getLeftTopX() {
		return leftTopX;
	}

	public double getLeftTopY() {
		return leftTopY;
	}

	public double getRightBottomX() {
		return rightBottomX;
	}

	public double getRightBottomY() {
		return rightBottomY;
	}
	
	
}
