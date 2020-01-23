package intextbooks.content.models.formatting.lists;

public class ListingContainer {
	
	private int listID;
	private int listingID;
	private int pageIndex;
	private int segmentID;
	private double topY;
	private double bottomY;
	
	/**
	 * Container for list elements (listings) in text resources.
	 * 
	 * @param listID ID of the whole list
	 * @param listingID ID of the single list element
	 * @param pageIndex
	 * @param segmentID
	 * @param topY
	 * @param bottomY
	 */
	
	public ListingContainer(int listID, int listingID, int pageIndex, int segmentID, double topY, double bottomY) {
		
		this.listID = listID;
		this.listingID = listingID;
		this.pageIndex = pageIndex;
		this.segmentID = segmentID;
		this.topY = topY;
		this.bottomY = bottomY;
		
	}
	
	public ListingContainer(int listID, int listingID, int pageIndex, double topY, double bottomY) {
		
		this.listID = listID;
		this.listingID = listingID;
		this.pageIndex = pageIndex;
		this.topY = topY;
		this.bottomY = bottomY;
		
	} 
	
	public int getListID() {
		return this.listID;
	}
	
	public int getListingID() {
		return this.listingID;
	}
	
	public int getPageIndex() {
		return this.pageIndex;
	}
	
	public int getSegmentID() {
		return this.segmentID;
	}
	
	public double getTopY() {
		return this.topY;
	}
	
	public double getBottomY() {
		return this.bottomY;
	}

}
