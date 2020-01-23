package intextbooks.content.extraction.format.lists;

import java.util.List;

import intextbooks.content.extraction.buildingBlocks.format.ResourceUnit;
import intextbooks.content.extraction.format.FormatReasoner;

public class SlideListingGrouper implements ListingGrouper {

	@Override
	public void groupListings(List<ResourceUnit> resources) {
		FormatReasoner reasoner = new FormatReasoner();
		reasoner.pageTextGrouper(resources);		
	}

}
