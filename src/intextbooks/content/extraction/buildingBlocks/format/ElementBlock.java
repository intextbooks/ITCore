package intextbooks.content.extraction.buildingBlocks.format;

import java.util.ArrayList;

public class ElementBlock implements Cloneable {
	
	public float fistLineY;
	public float lastLineY;
	public int firstLineIndx;
	public int lastLineIndx;
	public float fontSize;
	public ArrayList<ElementBlock> subGroup = null;
	
	@Override
	public ElementBlock clone() {

		ElementBlock newBlock = new ElementBlock();
		ArrayList<ElementBlock> newSubGroup = null;
		
		newBlock.fistLineY = this.fistLineY;
		newBlock.lastLineY = this.lastLineY;
		newBlock.firstLineIndx = this.firstLineIndx;
		newBlock.lastLineIndx = this.lastLineIndx;
		newBlock.fontSize = this.fontSize;
		
		if (this.subGroup != null) {
			
			newSubGroup = new ArrayList<ElementBlock>();
			
			for (ElementBlock block : this.subGroup)
				newSubGroup.add(block.clone());
			
		}
		
		newBlock.subGroup = newSubGroup;
		
		return newBlock;
		
	}
	
	public void setSubGroup(ArrayList<ElementBlock> subGroup) {
		
		this.subGroup = new ArrayList<ElementBlock>();
		
		for (ElementBlock block : subGroup)
			this.subGroup.add(block.clone());
		
	}
	
}
