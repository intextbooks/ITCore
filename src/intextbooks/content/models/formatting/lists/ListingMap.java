package intextbooks.content.models.formatting.lists;

import java.sql.SQLException;
import java.util.ArrayList;

import intextbooks.SystemLogger;
import intextbooks.persistence.Persistence;

public class ListingMap {
	
	private Persistence persistence = Persistence.getInstance();
	private String parentBook;
	
	public ListingMap(String parentBook) {
		this.parentBook = parentBook;
	}
	
	public void createFreshListingMap() {
		try {
			this.persistence.createListingMap(this.parentBook);
		} catch (SQLException e) {
			e.printStackTrace();
			SystemLogger.getInstance().log(e.toString());
		}
	}
	
	public void addListing(ListingContainer listing) {
		try {
			this.persistence.addListing(this.parentBook, listing);
		} catch (SQLException e) {
			e.printStackTrace();
			SystemLogger.getInstance().log(e.toString());
		}
	}
	
	public ArrayList<ListingContainer> getListingsInSegment(int segmentID) {
		try {
			return this.persistence.getListingsInSegment(this.parentBook, segmentID);
		} catch (SQLException e) {
			e.printStackTrace();
			SystemLogger.getInstance().log(e.toString());
			return new ArrayList<ListingContainer>();
		}
	}
	
	public ArrayList<ListingContainer> getListingsOnPage(int pageIndex) {
		try {
			return this.persistence.getListingsOnPage(this.parentBook, pageIndex);
		} catch (SQLException e) {
			e.printStackTrace();
			SystemLogger.getInstance().log(e.toString());
			return new ArrayList<ListingContainer>();
		}
	}

}
