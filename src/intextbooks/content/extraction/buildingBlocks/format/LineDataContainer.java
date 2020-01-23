package intextbooks.content.extraction.buildingBlocks.format;

public class LineDataContainer {

	public String fontName;
	public float fontSize;
	public float linePosX;
	public float linePosY;
	public boolean bold;
	
	public LineDataContainer(String fn, float fs, float sp) {
		// TODO Auto-generated constructor stub
		
		fontName=fn;
		fontSize= fs;
		linePosX=sp;
	}
	
	public LineDataContainer(String fn, float fs, float sp,boolean b) {
		// TODO Auto-generated constructor stub
		
		fontName=fn;
		fontSize= fs;
		linePosX=sp;
		bold=b;
	}
	
	public LineDataContainer(String fn, float fs, float sp, float lp) {
		// TODO Auto-generated constructor stub
		
		fontName=fn;
		fontSize= fs;
		linePosX=sp;
		linePosY=lp;
	}
	
	
    @Override
    public int hashCode() {
        int result = (int) fontSize;
        result = (int) (31 * result + linePosX);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LineDataContainer other = (LineDataContainer) obj;
        if (fontSize != other.fontSize)
            return false;
        if(linePosX != other.linePosX)
            return false;
        if (fontName!=null) 
        	if(!fontName.equals(other.fontName))
        		return false;
        if(bold != other.bold)
        	return false;

        return true;
    }
}
