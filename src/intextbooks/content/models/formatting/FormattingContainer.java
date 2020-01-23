package intextbooks.content.models.formatting;

import java.util.Arrays;
import java.util.Comparator;

public class FormattingContainer implements Comparator<FormattingContainer> {
	
	public enum RoleLabel {
        Title, Body;
    }
	
	 public String fontFamily;
	 public short fontSize;
	 public int indentation;
	 public boolean bold;
	 public boolean italic;
	 private Integer keySum;
	 private String name;
	 private float[] fontColorComponents;
	 private int freq;
	 private RoleLabel roleLabel;
	 private int titleLevel;
	 
	 public FormattingContainer() {
		 super();
		 this.freq = 1;
		 this.fontFamily = "Times New Roman";
		 this.fontSize = 100;
		 this.fontColorComponents = null;
		 this.computeBuffer();
	 }
	 
	 public FormattingContainer(FormattingContainer formattingContainer) {
		super();
		this.fontFamily = formattingContainer.getFontFamily();
		this.fontSize = formattingContainer.getFontSize();
		this.indentation = 0;
		this.bold = formattingContainer.isBold();
		this.italic = formattingContainer.isItalic();
		this.fontColorComponents =  formattingContainer.getFontColorComponents();
		this.name = "";
		this.freq = 1;
		this.computeBuffer();
		
	 }
	 
	 private void computeBuffer() {
		String buffer  = fontFamily + Short.toString(fontSize) + Boolean.toString(bold) + Boolean.toString(italic) + Arrays.toString(fontColorComponents);
		this.keySum = buffer.hashCode();
	 }
	 
	/**
	 * @param fontFamily
	 * @param fontSize
	 * @param indentation
	 * @param bold
	 * @param italic
	 */
	public FormattingContainer(String fontFamily, byte fontSize,
			int indentation, boolean bold, boolean italic, float[] fontColorComponents) {
		super();
		
		this.fontFamily = fontFamily;
		this.fontSize = fontSize;
		this.indentation = indentation;
		this.bold = bold;
		this.italic = italic;
		this.name = "";
		this.fontColorComponents = fontColorComponents;
		this.freq = 1;
		this.computeBuffer();
		
	}

	public void incrementFreq() {
		this.freq++;
	}
	
	public void incremetFreq(int val) {
		this.freq += val;
	}
	
	public int getFreq() {
		return this.freq;
	}
	
	public void setFreq(int freq) {
		this.freq = freq;
	}

	public Integer updateKeySum() {
		this.computeBuffer();
		return this.keySum;
	}

	public Integer getKeySum() {
		if(keySum == null) {
			this.computeBuffer();
		}
		return  this.keySum;
	}

	public void setName(int counter) {
		this.name = "Format_"+String.valueOf(counter);
	}

	public String getName() {
		return this.name;
	}

	public void setFontFamily(String fontFamily) {
		 this.fontFamily = fontFamily;
	}

	public String getFontFamily() {
		return fontFamily;
	}


	public void setSize(Float size) {
		fontSize = size.shortValue();
	}

	public short getFontSize() {
		return fontSize;
	}

	public int getIndentation() {
		return indentation;
	}

	public boolean isBold() {
		return bold;
	}
	
	public boolean hasFontFace() {
		return (bold || italic);
	}

	public void setBold(boolean bold) {
		this.bold = bold;
	}

	public void setItalic(boolean italic) {
		this.italic = italic;
	}
	
	public boolean isItalic() {
		return italic;
	}
	
	public float[] getFontColorComponents() {
		return this.fontColorComponents;
	}
	
	public void setFontColorComponents(float[] fontColorComponents) {
		this.fontColorComponents = fontColorComponents;
	}
	
	public boolean hasColor() {
		int cant0 = 0;
		for(float v: this.fontColorComponents) {
			if(v == 0) {
				cant0++;
			}
		}
		if(cant0 == 3) {
			return false;
		} else {
			return true;
		}
	}

	public RoleLabel getRoleLabel() {
		return roleLabel;
	}

	public void setRoleLabel(RoleLabel roleLabel) {
		this.roleLabel = roleLabel;
	}

	public int getTitleLevel() {
		return titleLevel;
	}

	public void setTitleLevel(int titleLevel) {
		this.titleLevel = titleLevel;
	}

	@Override
	public String toString() {
		return "FormattingContainer [role=" + roleLabel + " level="+ titleLevel +  " fontFamily=" + fontFamily + ", fontSize=" + fontSize + ", indentation="
				+ indentation + ", bold=" + bold + ", italic=" + italic + " , color=" + Arrays.toString(fontColorComponents) + ", keySum=" + keySum + ", name=" + name + ", freq=" + freq + "]";
		//String buffer  = fontFamily + Short.toString(fontSize) + Boolean.toString(bold) + Boolean.toString(italic) + Arrays.toString(fontColorComponents);
		//buffer+= " k:" +  buffer.hashCode();
		//return buffer;
	}
	
	@Override
    public boolean equals(Object o) { 
  
        // If the object is compared with itself then return true   
        if (o == this) { 
            return true; 
        } 
  
        /* Check if o is an instance of Complex or not 
          "null instanceof [type]" also returns false */
        if (!(o instanceof FormattingContainer)) { 
            return false; 
        } 
          
        // typecast o to Complex so that we can compare data members  
        FormattingContainer c = (FormattingContainer) o; 
          
        // Compare the data members and return accordingly  
        return this.getKeySum().equals(c.getKeySum()); 
    } 

	@Override
	public int compare(FormattingContainer o1, FormattingContainer o2) {
		if(o1.getKeySum() == o2.getKeySum()) {
			return 0;
		} else {
			if(o1.getFontSize() > o2.getFontSize()) {
				return 1;
			} else if(o1.getFontSize() < o2.getFontSize()){
				return -1;
			} else {
				if((o1.isBold() || o1.isItalic()) && !(o2.isBold() || o2.isItalic())) {
					return 1;
				} else if (!(o1.isBold() || o1.isItalic()) && (o2.isBold() || o2.isItalic())) {
					return -1;
				} else {
					if(o1.hasColor() && !o2.hasColor()) {
						return 1;
					} else if(!o1.hasColor() && o2.hasColor()) {
						return -1;
					}else {
						if(o1.getFreq() > o2.getFreq()) {			
							return 1;
						} else if(o1.getFreq() < o2.getFreq()) {
							return -1;
						} else {
							if(o1.getFontFamily() == null && o2.getFontFamily() == null){
								return 0;
							} else if (o1.getFontFamily() != null && o2.getFontFamily() == null) {
								return 1;
							} else if (o1.getFontFamily() == null && o2.getFontFamily() != null) {
								return -1;
							} else {
								return o1.getFontFamily().compareTo(o2.getFontFamily());
							}
						}
					}
				}
			}
		}
	}
}
