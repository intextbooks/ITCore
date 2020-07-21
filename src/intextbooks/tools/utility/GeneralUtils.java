package intextbooks.tools.utility;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import intextbooks.content.extraction.buildingBlocks.format.Page;

public class GeneralUtils {
	public static int getValidPreviousPage(int page, List<Page> pages) {
		while(page >= 0) {
			if(pages.get(page) != null && pages.get(page).size() > 0) {
				return page;
			}
			page--;
		}
		return 0;
	}
	
	public static boolean containsPageNumber(List<Pair<Integer, Integer>> pages, int pageNumber) {
		for(Pair<Integer, Integer> page: pages) {
			if(page.getLeft().equals(pageNumber)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean containsSegment(Map<Integer,List<Integer>> correspondingSegments, Integer segment) {
		for(List<Integer> segments: correspondingSegments.values()) {
			if(segments.contains(segment)) {
				return true;
			}
		}
		return false;
	}
}
