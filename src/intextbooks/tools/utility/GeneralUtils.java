package intextbooks.tools.utility;

import java.util.List;

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
}
