package intextbooks.content.utilities;

import java.util.ArrayList;

public class ConceptContainer {
	String indexTerm;
	String conceptName;
	ArrayList<String> URIlist;
	
	
	public ConceptContainer(String indexTerm){
		
		this.indexTerm = indexTerm;
		this.conceptName = null;
		this.URIlist = new ArrayList<String>();
		
	}

	public String getIndexTerm() {
		return indexTerm;
	}

	public String getConceptName() {
		return conceptName;
	}
	
	public void setConceptName(String conceptName) {
		if(conceptName != null && conceptName.equals(""))
			this.conceptName = null;
		else
			this.conceptName = conceptName;
	}

	public ArrayList<String> getURIs(){
		return this.URIlist;
	}
	
	public void addURI(String uri){
		this.URIlist.add(uri);
	}
	
	public void setURIs(ArrayList<String> uris){
		this.URIlist = uris;
	}
	
	public boolean hasURIsAttached(){
		return this.URIlist.size() > 0;
	}

	public boolean hasConcept() {
		return this.conceptName != null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((conceptName == null) ? 0 : conceptName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConceptContainer other = (ConceptContainer) obj;
		if (conceptName == null) {
			if (other.conceptName != null)
				return false;
		} else if (!conceptName.equals(other.conceptName))
			return false;
		return true;
	}
	
	
	
}
