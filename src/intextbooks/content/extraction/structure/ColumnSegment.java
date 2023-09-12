package intextbooks.content.extraction.structure;


public class ColumnSegment {

	float start;
	float end;
	boolean blank;
	int hits;
	
	public ColumnSegment(float s, float e) {
		this.start = s;
		this.end = e;
		this.blank = false;
		this.hits = 0;
	}
	
	public void restartCount() {
		this.hits = 0;
		this.blank = true;
	}
	
	public void incHit() {
		this.hits++;
	}
	
	public void setBlank(boolean b) {
		this.blank = b;
	}
	
	public boolean getBlank() {
		return this.blank;
	}
	
	public int getHits() {
		return this.hits;
	}

	public float getStart() {
		return start;
	}

	public void setStart(float start) {
		this.start = start;
	}

	public float getEnd() {
		return end;
	}

	public void setEnd(float end) {
		this.end = end;
	}
	
	public void check(float wStart, float wEnd) {
		if( (wStart >= start && wStart < end)
			|| (wEnd >= start && wEnd < end)
			|| (wStart < start && wEnd > end)){
				this.incHit();
			}
	}
	
	public void merge(ColumnSegment otherSegment) {
		if(this.blank == otherSegment.getBlank()) {
			this.hits += otherSegment.getHits();
			this.end = otherSegment.end;
		}
	}

	@Override
	public String toString() {
		return "Segment [start=" + start + ", end=" + end + ", blank=" + blank + ", hits=" + hits + "]";
	}
	
}
