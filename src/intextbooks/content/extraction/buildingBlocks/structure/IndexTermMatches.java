package intextbooks.content.extraction.buildingBlocks.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

public class IndexTermMatches {
	
	final int numberOfQueues = 4;
	
	List<IndexTermMatch>[] queues;
	
	public IndexTermMatches() {
		queues = new List[numberOfQueues];
		for(int i = 0; i < numberOfQueues; i++) {
			queues[i] = new ArrayList<IndexTermMatch>();
		}
	}
	
	public void add(IndexTermMatch element, int priority) {
		if(priority <= queues.length) {
			queues[priority - 1].add(element);
		}
	}
	
	public void add(IndexTermMatch element) {
		if(element.getPriority() <= queues.length) {
			queues[element.getPriority()  - 1].add(element);
		}
	}
	
	public IndexTermMatch poll() {
		for(int i = 0; i < queues.length; i++) {
			List<IndexTermMatch> list = queues[i];
			if(list.size() == 1) {
				IndexTermMatch res = list.get(0);
				list.remove(0);
				return res;
			} else if (list.size() > 1){
				//SELECT BASED ON NUMBER OF CORRECT RESULTS
				int maxCant = list.stream().mapToInt(n -> n.getNumberOfCorrrectResults()).max().orElse(0);
				List<IndexTermMatch> results = new ArrayList<IndexTermMatch>();
				for(IndexTermMatch match: list) {
					if(match.getNumberOfCorrrectResults() == maxCant) {
						results.add(match);
					}
				}
				//SELECT BASED ON NUMBER OF SEGMENT ID
				IndexTermMatch res = results.stream().max(Comparator.comparing(IndexTermMatch::getSegmentID)).orElse(null);	
				list.remove(res);
				return res;
			}
		}
		return null;
	}
}
