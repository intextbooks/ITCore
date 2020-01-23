package intextbooks.content.enrichment;

import java.util.Comparator;

public class ComparatorCandidateSimilarityScore implements Comparator<Candidate> {

	@Override
	public int compare(Candidate o1, Candidate o2) {
		return ((Double)o1.getSimilarityScore()).compareTo(o2.getSimilarityScore());
	}

}
