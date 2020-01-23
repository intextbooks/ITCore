package intextbooks.content.extraction.buildingBlocks.format;

import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.text.TextPosition;


public class CharacterBlock implements Comparable<CharacterBlock> {
	
	String unicode;
	PDColor nonStrokingColor;
	BoundingBox boundingBox;
	TextPosition textPosition;
	
	public CharacterBlock(PDColor nonStrokingColor, BoundingBox boundingBox, TextPosition textPosition) {
		this.unicode = textPosition.getUnicode();
		this.nonStrokingColor = nonStrokingColor;
		this.boundingBox = boundingBox;
		this.textPosition = textPosition;
	}
	
	public double getBBHeight(){
		return this.boundingBox.getHeight();
	}
	
	public double getBBWidth(){
		return this.boundingBox.getWidth();
	}
	
	public float getFontSize() {
		return this.textPosition.getFontSizeInPt();
	}
	
	public double getStartX() {
		return this.boundingBox.getLowerLeftX();
	}
	
	public double getEndX() {
		return this.boundingBox.getUpperRightX();
	}
	
	public double getStartY() {
		return this.boundingBox.getLowerLeftY();
	}
	
	public double getEndY() {
		return this.boundingBox.getUpperRightY();
	}
	
	public double getWidthOfSpace() {
		return this.textPosition.getWidthOfSpace();
	}
	
	public PDColor getColor() {
		return this.nonStrokingColor;
	}
	
	public TextPosition getTextPosition() {
		return this.textPosition;
	}

	@Override
	public String toString() {
		String toString = "";
		toString += unicode;
		return toString;
	}

	@Override
	public int compareTo(CharacterBlock o) {
		if( (this.getStartY() == o.getStartY() && this.getEndY() == o.getEndY()) ||
				(this.getStartY() < o.getStartY() && o.getStartY() < this.getEndY() && this.getEndY() < o.getEndY()) || 
				(this.getEndY() > o.getEndY() && o.getEndY() > this.getStartY() && this.getStartY()  > o.getStartY())) {
			if(this.getStartX() < o.getStartX()) {
				return -1;
			} else if (this.getStartX() > o.getStartX()) {
				return 1;
			} else {
				if(this.getStartY() < o.getStartY() ) {
					return -1;
				} else if (this.getStartY() > o.getStartY()) {
					return 1;
				} else {
					return 0;
				}
			}
		} else if (this.getStartY() > o.getEndY()) {
			return 1;	
		} else {
			return -1;
		}
	}
}
