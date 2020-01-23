package intextbooks.content.enrichment;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import intextbooks.ontologie.LanguageEnum;

public class EnrichmentData implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7354274136466297810L;
	public String rootCategory;
	public Set<String> expandedCategories;
	public	Map<String, String> coreSet;
	public Map<String, Set<Candidate>> notSelectedResources;
	public Map<String, Candidate> selectedResources;
	public Map<String,Set<Candidate>> conceptCandidates;
	
	public EnrichmentData() {
		this.coreSet = new HashMap<String, String>();
		this.expandedCategories = new HashSet<String>();
		this.notSelectedResources = new HashMap<String, Set<Candidate>>() ;
		this.selectedResources = new HashMap<String, Candidate>();
		this.conceptCandidates = new HashMap<String,Set<Candidate>>();
	}
	
	public void setRootCategory(String rootCategory) {
		this.rootCategory = rootCategory;
	}
	
	public String getRootCategory() {
		return this.rootCategory;
	}
	
	public void setSelectedResources(Map<String, Candidate> selectedResources) {
		this.selectedResources = selectedResources;	
	}
	
	public Map<String, Candidate> getSelectedResources(){
		return this.selectedResources;
	}
	
	public void addToNotSelectedResource(String name, Set<Candidate> notSelected) {
		this.notSelectedResources.put(name, notSelected);
	}
	
	public void addCategories(Collection<String> categories) {
		this.expandedCategories.addAll(categories);
	}
	
	public void addToCoreSet(String key, String value) {
		this.coreSet.put(key, value);
	}
	
	public void addToConceptCandidates(String name, Set<Candidate> cadidates) {
		this.conceptCandidates.put(name, cadidates);
	}
	
	public Map<String,Set<Candidate>> getConceptCandidates(){
		return this.conceptCandidates;
	}
	
	public Set<String> getCoreSetAsSetOfURL(){
		return new HashSet<String>(this.coreSet.values());
	}
	
	public static void main(String[] args) {
	}

}
