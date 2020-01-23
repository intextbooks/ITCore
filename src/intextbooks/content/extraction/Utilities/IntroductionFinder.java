package intextbooks.content.extraction.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import intextbooks.Configuration;
import intextbooks.SystemLogger;

public class IntroductionFinder {
	
	private static int introOccurrenceCount = Configuration.getInstance().getOccurrenceCountForIntroduction();
	
	public static int findIntroductionPage(Map<Integer, Integer> freqMap) {
		
		Map<Integer, List<Integer>> invertedList = invertFrequencyMap(freqMap);
		
		// return first page with expected support
		if (invertedList.containsKey(introOccurrenceCount)) {
			
			SystemLogger.getInstance().log("Found introduction with support " + introOccurrenceCount);
			
			List<Integer> occurrences = invertedList.get(introOccurrenceCount);
			return checkList(occurrences);
			
		} else { // no page with expected support found
			
			for (int k = 1; introOccurrenceCount-k > 0; k++) {
				
				int incCount = introOccurrenceCount + k;
				
				if (invertedList.containsKey(incCount)) {
					SystemLogger.getInstance().log("Found introduction with support " + incCount);
					List<Integer> occurrences = invertedList.get(incCount);
					return checkList(occurrences);
				}
				
				int decCount = introOccurrenceCount - k;
				
				if (invertedList.containsKey(decCount)) {
					SystemLogger.getInstance().log("Found introduction with support " + decCount);
					List<Integer> occurrences = invertedList.get(decCount);
					return checkList(occurrences);
				}
				
			}
			
		}
		
		return -1;
		
	}
	
	/**
	 * Creates mapping from occurrence counts to pages with corresponding support for a term.
	 * 
	 * @param freqMap Mapping from pages to their support
	 * @return Inverted list of occurrences
	 */
	
	private static Map<Integer, List<Integer>> invertFrequencyMap(Map<Integer, Integer> freqMap) {
		
		Map<Integer, List<Integer>> invertedList = new HashMap<Integer, List<Integer>>();
		
		for (Map.Entry<Integer, Integer> entry : freqMap.entrySet()) {
			
			int page = entry.getKey();
			int termFreq = entry.getValue();
			
			if (!invertedList.containsKey(termFreq))
				invertedList.put(termFreq, new ArrayList<Integer>());
			
			invertedList.get(termFreq).add(page);
			
		}
		
		return invertedList;
		
	}
	
	/**
	 * Returns smallest element greater than 0 (as far as possible).
	 * Values should not be -1 or 0 since pages with index smaller than 1 are truncated during extraction.
	 * 
	 * @param list
	 * @return
	 */
	
	private static int checkList(List<Integer> list) {
		
		Collections.sort(list);
		
		for (int i = 0; i < list.size(); i++) {			
			if (list.get(i) > 0)
				return list.get(i);			
		}
				
		return -1;
		
	}

}
