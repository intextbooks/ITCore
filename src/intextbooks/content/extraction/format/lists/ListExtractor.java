package intextbooks.content.extraction.format.lists;

import java.util.ArrayList;

import intextbooks.content.extraction.buildingBlocks.format.ResourceUnit;;

public interface ListExtractor {
	
	public void extractLists(ArrayList<ResourceUnit> resources);
	
	public void persistLists();

}
