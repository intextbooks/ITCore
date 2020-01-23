package intextbooks.content.enrichment;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Set;

public class Candidate implements Comparable<Candidate>, Serializable {
	
	private String URI;
	private String abstractString;
	private Set<String> categories;
	private Set<String> titleDomain;
	private double similarityScore;
	
	public double titleScore;
	public double cosineScore;
	public double categoriesScore;
	public int categoriesCount;
	
	
	
	public Candidate() {
		
	}
	
	public Candidate(String URI, String abstractString, Set<String> categories, Set<String> titleDomain) {
		this.URI = URI;
		this.abstractString = abstractString;
		this.categories = categories;
		this.titleDomain = titleDomain;
		this.similarityScore = 0;
	}
	
	public double getSimilarityScore() {
		return similarityScore;
	}

	public void setSimilarityScore(double similarityScore) {
		this.similarityScore = similarityScore;
	}

	public Set<String> getCategories() {
		return categories;
	}

	public void setCategories(Set<String> categories) {
		this.categories = categories;
	}

	public Set<String> getTitleDomain() {
		return titleDomain;
	}

	public void setTitleDomain(Set<String> titleDomain) {
		this.titleDomain = titleDomain;
	}

	@Override
	public int compareTo(Candidate o) {
		return URI.compareTo(o.getURI());
	}
	
	@Override
	public boolean equals(Object o) {
		try {
			return URI.equals(((Candidate)o).getURI());
		} catch (Exception e) {
			return false;
		}
	}
	
    @Override
    public int hashCode() {
        return URI.hashCode();
    }

	public String getURI() {
		return URI;
	}

	public void setURI(String uRI) {
		URI = uRI;
	}

	public String getAbstractString() {
		return abstractString;
	}

	public void setAbstractString(String abstractString) {
		this.abstractString = abstractString;
	}

	@Override
	public String toString() {
		return "Candidate [URI=" + URI + ", score="+ similarityScore + ", categoriesCount="+ categoriesCount + ", abstractString(len)=" + abstractString.length() + ", categories=" + categories
				+ ", titleDomain=" + titleDomain + "]";
	}

	
}


