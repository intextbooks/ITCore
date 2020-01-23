package intextbooks.content.models.formatting.lists;

import java.util.ArrayList;

public class ListingModel {
	
	private String parentBookID;
	private ListingMap map;
	
	public ListingModel(String bookID, boolean clean) {
		
		this.parentBookID = bookID;
		this.map = new ListingMap(this.parentBookID);
		
		if (clean)
			this.map.createFreshListingMap();
		
	}
	
	public void addListing(ListingContainer listing) {
		this.map.addListing(listing);
	}
	
	public ArrayList<ListingContainer> getListingsInSegment(int segmentID) {
		return this.map.getListingsInSegment(segmentID);
	}
	
	public ArrayList<ListingContainer> getListingsOnPage(int pageIndex) {
		return this.map.getListingsOnPage(pageIndex);
	}

}
