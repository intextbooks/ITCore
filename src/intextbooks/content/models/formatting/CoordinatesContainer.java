package intextbooks.content.models.formatting;

import java.util.HashMap;

public class CoordinatesContainer {
	private double leftTopX;
	private double leftTopY;
	
	private double rightBottomX;
	private double rightBottomY;
	
	/**Automatically computed coordinates**/
	
	private double leftBottomX;
	private double leftBottomY;
	
	private double rightTopX;
	private double rightTopY;
	
	/**
	 * @param lTopX
	 * @param lTopY
	 * @param rBottomX
	 * @param rBottomY
	 */
	public CoordinatesContainer(double lTopX, double lTopY, 
			double rBottomX, double rBottomY) {
		
		this.leftTopX = lTopX;
		this.leftTopY = lTopY;
		this.rightBottomX = rBottomX;
		this.rightBottomY = rBottomY;
		
		this.leftBottomX = this.leftTopX;
		this.leftBottomY = this.rightBottomY;
		this.rightTopX = rBottomX;
		this.rightTopY = lTopY;		
		
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

	public double getLeftBottomX() {
		return leftBottomX;
	}

	public double getLeftBottomY() {
		return leftBottomY;
	}

	public double getRightTopX() {
		return rightTopX;
	}

	public double getRightTopY() {
		return rightTopY;
	}
	
	@Override 
	public String toString(){
		return "("+this.leftTopX + " , " + this.leftTopY + ") , (" + this.rightBottomX + " , " + this.rightBottomY + ") , (" +
		
		this.leftBottomX + " , " + this.leftBottomY + ") , (" +this.rightTopX + " , " + this.rightTopY+")";
	}
	
	
}
