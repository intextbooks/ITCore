package intextbooks.content.extraction.format.lists;

import java.util.ArrayList;

import intextbooks.SystemLogger;
import intextbooks.content.ContentManager;
import intextbooks.content.extraction.buildingBlocks.format.ElementBlock;
import intextbooks.content.extraction.buildingBlocks.format.ResourceUnit;
import intextbooks.content.models.formatting.lists.ListingContainer;

public class SlideListExtractor implements ListExtractor {
	
	private String bookID;
	private ArrayList<ResourceUnit> resources;
	
	public SlideListExtractor(String bookID) {
		this.bookID = bookID;
	}

	@Override
	public void extractLists(ArrayList<ResourceUnit> resources) {
		
		SlideListingGrouper grouper = new SlideListingGrouper();
		grouper.groupListings(resources);
		this.resources = resources;
	}
		
	@Override
	public void persistLists() {
		
		SystemLogger.getInstance().log("Persist extracted lists");
		
		// must be 0 since resources are persisted starting 1 and not 0
		int listIndex = 0;
		int listingIndex = 0;
		int pageIndex = 0;

		// one ResourceUnit is one page
		for (ResourceUnit resource : this.resources) {
			
			pageIndex++;
			listIndex++;
			
			// the lists are created as groups in the listing grouper and stored in the resources
			ArrayList<ElementBlock> groups = resource.getGroups();
			
			for (ElementBlock group : groups) {
				
				if (group != null) {
					
					// check for list elements
					if (group.subGroup != null) {
							
						for (ElementBlock subGroup : group.subGroup) {
								
							listingIndex++;
							ListingContainer listing = new ListingContainer(listIndex, listingIndex, pageIndex, subGroup.fistLineY, subGroup.lastLineY);
							ContentManager.getInstance().addListing(this.bookID, listing);
							SystemLogger.getInstance().log("Adding listing " + listingIndex + " in list " + listIndex);
								
						}
					
					}
						
				}
				
			}
			
		}
		
		SystemLogger.getInstance().log("Extracted lists persisted");
		
	}

}
