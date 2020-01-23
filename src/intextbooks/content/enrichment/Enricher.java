package intextbooks.content.enrichment;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.skos.AddAssertion;
import org.semanticweb.skos.SKOSChange;
import org.semanticweb.skos.SKOSChangeException;
import org.semanticweb.skos.SKOSConcept;
import org.semanticweb.skos.SKOSConceptScheme;
import org.semanticweb.skos.SKOSCreationException;
import org.semanticweb.skos.SKOSDataFactory;
import org.semanticweb.skos.SKOSDataProperty;
import org.semanticweb.skos.SKOSDataRelationAssertion;
import org.semanticweb.skos.SKOSDataset;
import org.semanticweb.skos.SKOSObjectRelationAssertion;
import org.semanticweb.skos.properties.SKOSDefinitionDataProperty;
import org.semanticweb.skos.properties.SKOSRelatedProperty;
import org.semanticweb.skosapibinding.SKOSManager;

import intextbooks.Configuration;
import intextbooks.SystemLogger;
import intextbooks.content.enrichment.dbpedia.DBpediaController;
import intextbooks.exceptions.EnrichedModelWIthProblems;
import intextbooks.ontologie.LanguageEnum;
import intextbooks.persistence.Persistence;
import intextbooks.tools.utility.OntologyUtils;

public class Enricher {
	String bookID;
	String category;
	Set<String> titleDomain;
	LanguageEnum langBook;
	Set<Concept> glossary;
	Map<String,Integer> categories;
	String seedDomainAbstracts;
	Map<String, Map<LanguageEnum, Map<String,Pair<Set<String>,String>>>> seedDomainEntities;
	Map<String, Map<LanguageEnum, Map<String,Set<Candidate>>>> conceptCandidates;
	Map<String, Map<LanguageEnum, Map<String, Candidate>>> finalConceptCandidates;
	Map<String, Candidate> finalResourcesForConcepts;
	double thresholdLimit;
	SKOSDataset model;
	SKOSManager manager;
	SKOSDataFactory factory;
	
	SystemLogger logger = SystemLogger.getInstance();
	DBpediaController controller = DBpediaController.getInstance();
	
	public Enricher(String bookID, String category, LanguageEnum langBook)  {
		this.bookID = bookID;
		this.category = category;
		if(this.category.contains("/page/")) {
			this.category = this.category.replace("/page/", "/resource/");
		}
		this.langBook = langBook;
		this.glossary = new HashSet<Concept>();
		this.titleDomain = controller.computeTitleDomain(category);
		this.thresholdLimit = Configuration.getInstance().getSimilarityScoreThreshold();
		this.finalResourcesForConcepts = new HashMap<String, Candidate>();
		
		try {
			this.manager = new SKOSManager();
		} catch (SKOSCreationException e) {
			e.printStackTrace();
		}
		this.factory = this.manager.getSKOSDataFactory();
	}
	
	/*
	 * When category discovery is necessary
	 */
	public Enricher(String bookID, LanguageEnum langBook)  {
		this.bookID = bookID;
		this.langBook = langBook;
		this.glossary = new HashSet<Concept>();
		this.thresholdLimit = Configuration.getInstance().getSimilarityScoreThreshold();
		this.finalResourcesForConcepts = new HashMap<String, Candidate>();
		try {
			this.manager = new SKOSManager();
		} catch (SKOSCreationException e) {
			e.printStackTrace();
		}
		this.factory = this.manager.getSKOSDataFactory();
		this.discoverCategory();
	}
	
	public void discoverCategory() {
		this.extractGlossary();
		Pair<Map<String, Map<LanguageEnum, Map<String, Pair<Set<String>, String>>>>, String>  res =controller.extractSeedDomain(glossary, categories);
	}
	
	
	public void enrichWithDBpedia() throws EnrichedModelWIthProblems, Exception {
		enrichWithDBpedia(null);
	}
	
	public void enrichWithDBpedia(String glossaryName) throws EnrichedModelWIthProblems, Exception {
		
		this.extractGlossary();
		
		enrichWithDBpediaProcess();
	}
	
	
	
	private void enrichWithDBpediaProcess() throws EnrichedModelWIthProblems, Exception {
		EnrichmentData data = new EnrichmentData();
		//add root category to data
		data.setRootCategory(this.category);
		this.categories =  controller.expandDomainCategories(this.category);
		//adding categories to data
		data.addCategories(this.categories.keySet());
		logger.log("Expanded Domain Categories is done, size: " + this.categories.size() );
		Pair<Map<String, Map<LanguageEnum, Map<String, Pair<Set<String>, String>>>>, String>  res =controller.extractSeedDomain(glossary, categories);
		this.seedDomainEntities = res.getLeft();
		this.seedDomainAbstracts = res.getRight();
		logger.log("Extraction of Seed Domain Entities is done, size: " + this.seedDomainEntities.size() );
		logger.log("Size of seed domain abstract is: " + this.seedDomainAbstracts.length() );
		//adding coreSet to data
		for(Entry<String, Map<LanguageEnum, Map<String, Pair<Set<String>, String>>>> entry : this.seedDomainEntities.entrySet()) {
			Map<LanguageEnum, Map<String, Pair<Set<String>, String>>> langs = entry.getValue();
			Map<String, Pair<Set<String>, String>> values = langs.get(this.langBook);
			for(Entry<String, Pair<Set<String>, String>> entryVals : values.entrySet()) {		
				for(String indexKey: entryVals.getValue().getLeft()) {
					data.addToCoreSet(indexKey, entryVals.getKey());
					//System.out.println("adding: " + indexKey + " v: " + entryVals.getKey());
				}
			}
		}
		
		try {
			this.conceptCandidates = controller.computeCandidatesFromGlossary(glossary, seedDomainEntities);
			logger.log("Extraction of Concept Candidates is done, size: " + this.conceptCandidates.size());
			this.finalConceptCandidates = controller.chooseFromCandidatesExpanding(seedDomainAbstracts, conceptCandidates, categories, titleDomain, thresholdLimit);
			logger.log("Size of finalConceptCandidates: " + this.finalConceptCandidates.size());
		} catch (Exception e1) {
			SystemLogger.getInstance().log("ERROR computing candidates or disambiguating");
			throw e1;
		}
		
		//adding conceptCandidates to data
		for(Entry<String, Map<LanguageEnum, Map<String, Set<Candidate>>>> entry : this.conceptCandidates.entrySet()) {
			Map<LanguageEnum, Map<String, Set<Candidate>>> langs = entry.getValue();
			Map<String, Set<Candidate>> entryVals = langs.get(this.langBook);
			if(entryVals != null && entryVals.size() != 0) {
				for(String indexKey: entryVals.keySet()) {
					data.addToConceptCandidates(indexKey, entryVals.get(indexKey));
				}
			}
		}		
		
		for(String concept: finalConceptCandidates.keySet()) {
			Map<LanguageEnum, Map<String, Candidate>> map = finalConceptCandidates.get(concept);
			Map<String, Candidate> resources = map.get(LanguageEnum.ENGLISH);
			if(resources == null || resources.size() == 0) {
				//System.out.println("Candidate: " + concept + " WITHOUT RESULT");
			} else {
				//.out.println("Candidate: " + concept + " with RESULTs: " );
				for(String label: resources.keySet()) {
					//System.out.println("\tL: " + label + " R: " + resources.get(label));
					finalResourcesForConcepts.put(concept, resources.get(label));
				}
			}
		}
		
		//adding selectedResources to data
		data.setSelectedResources(finalResourcesForConcepts);
		
		for(String concept: seedDomainEntities.keySet()) {
			Map<LanguageEnum, Map<String,Pair<Set<String>,String>>> map = seedDomainEntities.get(concept);
			Map<String,Pair<Set<String>,String>> resources = map.get(LanguageEnum.ENGLISH);
			for(String URI: resources.keySet()) {
				Candidate tmp  = new Candidate();
				tmp.setURI(URI);
				tmp.setAbstractString(resources.get(URI).getRight());
				tmp.setSimilarityScore(1);
				finalResourcesForConcepts.put(concept,tmp);
			}
		}
		logger.log("Disambiguation of candidates is done, final size: " + this.finalResourcesForConcepts.size());
		try {
			this.model = EnrichedModelBuilder.createModel(langBook, finalResourcesForConcepts);
			if(model != null) {
				logger.log("Adding abstracts to the model... ");
				this.addAbstracts();
				logger.log("Adding DBpedia links to the model ... ");
				this.addDBpediaLinks();
				logger.log("Adding Wikipedia links to the model ... ");
				this.addWikiLinks();
				logger.log("Adding categories to the model ...");
				this.addCategories();
				logger.log("Adding relations to the model ...");
				this.addRelations();
				logger.log("Enrichment ended");
			} else {
				
			}
		} catch (Exception e) {
			SystemLogger.getInstance().log(e.getMessage());
			throw new EnrichedModelWIthProblems("There was an error and the Enriched Model or part of it was not created succesfully. ");
		}
	}
	
	public void extractGlossary() {
		logger.log("Creating a glossary of terms  using the Index Terms");		
		//get persistence layer
		Persistence persistence = Persistence.getInstance();
		//create empty glossary
		Map<String, Concept> concepts = new HashMap<String,Concept>();	
		//create glossary, independent concepts for independent languages
		int repeated = 0;
		int total = 0;
		Set<String> repeatedList = new HashSet<String>();
		
		//check each index term
		List<String[]> res = persistence.getListOfIndicesWithPageForBookV2(bookID);
		
		total += res.size();
		for(String[] tmp : res) {
			String id = tmp[0];
			String key_name = tmp[1];
			String label = tmp[2];
			String full_label = tmp[3];
			String externalConceptName = tmp[4];
			String conceptName;
			boolean full = false;
			if(label != null  && full_label.equals("1")) {
				conceptName = label.toLowerCase().trim();
				full = true;
			} else {
				conceptName = key_name.replaceAll(" <> ", " ").toLowerCase().trim();
			}
			Concept concept = concepts.get(conceptName);
			if(concept == null) {
				//new concept
				concept = new Concept(conceptName);
				if(externalConceptName == null) {
					concept.setExternalConceptName(conceptName);
				} else {
					concept.setExternalConceptName(externalConceptName);
				}
				concepts.put(conceptName, concept);
				if(full) {
					concept.addPrefLabel(langBook, label);
				} else {
					//add pref label
					concept.addPrefLabel(langBook, key_name.replaceAll(" <> ", " ").toLowerCase().trim());
					//add alt labels
					List<String> altLabels = persistence.getListOfIndexParts(id);
					for(String altLabel : altLabels) {
						if(!altLabel.toLowerCase().trim().equals(conceptName)) {
							concept.addAltLabel(langBook, altLabel);
						} 
					}
				}
			} else {
				repeated++;
				repeatedList.add(conceptName);
				logger.debug("Glossary term repeated: " + conceptName);		
			}
		}
		
		
		this.glossary = new HashSet<Concept>(concepts.values());
		Concept.printGlossary(glossary, this.langBook);
		logger.log("Number of index terms: " + total);
		logger.log("Number of concepts: " + concepts.size());
		logger.log("Number of REPEATED concepts: " + repeated);
		logger.log("REPEATED concepts: " + Arrays.toString(repeatedList.toArray()));
	}
	
	public void addAbstracts() {
		List<SKOSChange> changeList = new ArrayList<SKOSChange>();
			
		for (SKOSConceptScheme scheme : this.model.getSKOSConceptSchemes()) {

			for (SKOSConcept concept : scheme.getConceptsInScheme(this.model)) {

				String termName = null;

				try {
					termName = OntologyUtils.getConceptName(concept.getURI());
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					this.logger.log(e.toString());
				}
				
				String abstractStr = this.controller.getSimpleAbstract(finalResourcesForConcepts.get(termName).getURI());
				SKOSDefinitionDataProperty defProp = this.factory.getSKOSDefinitionDataProperty();
				String lang = this.langBook.getShortendLanguageCode();
				if(abstractStr != null) {
					SKOSDataRelationAssertion defAssertion = this.factory.getSKOSDataRelationAssertion(concept, defProp, abstractStr,lang);
					changeList.add(new AddAssertion(this.model, defAssertion));
				}
			}
		}
		try {
			this.manager.applyChanges(changeList);
		} catch (SKOSChangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addDBpediaLinks() {
	
		List<SKOSChange> changeList = new ArrayList<SKOSChange>();
		
		for (SKOSConceptScheme scheme : this.model.getSKOSConceptSchemes()) {
	
			for (SKOSConcept concept : scheme.getConceptsInScheme(this.model)) {
	
				String termName = null;
	
				try {
					termName = OntologyUtils.getConceptName(concept.getURI());
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					this.logger.log(e.toString());
				}
				
				String link = finalResourcesForConcepts.get(termName).getURI();
				SKOSDataProperty dcsource = this.factory.getSKOSDataProperty(URI.create("http://purl.org/dc/terms/source"));
				String lang = this.langBook.getShortendLanguageCode();
				if(link != null) {
					SKOSDataRelationAssertion sourceAssertion = this.factory.getSKOSDataRelationAssertion(concept, dcsource, link, lang);
					changeList.add(new AddAssertion(this.model, sourceAssertion));
				}		
			}
		}
		
		try {
			this.manager.applyChanges(changeList);
		} catch (SKOSChangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public void addWikiLinks() {
		List<SKOSChange> changeList = new ArrayList<SKOSChange>();
		
		for (SKOSConceptScheme scheme : this.model.getSKOSConceptSchemes()) {
	
			for (SKOSConcept concept : scheme.getConceptsInScheme(this.model)) {
	
				String termName = null;
	
				try {
					termName = OntologyUtils.getConceptName(concept.getURI());
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					this.logger.log(e.toString());
				}
				
				String link = this.controller.getWikiLink(finalResourcesForConcepts.get(termName).getURI());
				if(link != null) {
					SKOSDataProperty isPrimaryTopicOf = this.factory.getSKOSDataProperty(URI.create("http://xmlns.com/foaf/0.1/isPrimaryTopicOf"));
					String lang = this.langBook.getShortendLanguageCode();
					SKOSDataRelationAssertion topicAssertion = this.factory.getSKOSDataRelationAssertion(concept, isPrimaryTopicOf, link, lang);
					changeList.add(new AddAssertion(this.model, topicAssertion));
				}	
			}
		}
		
		try {
			this.manager.applyChanges(changeList);
		} catch (SKOSChangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void addCategories() {
		
		List<SKOSChange> changeList = new ArrayList<SKOSChange>();
		
		for (SKOSConceptScheme scheme : this.model.getSKOSConceptSchemes()) {
	
			for (SKOSConcept concept : scheme.getConceptsInScheme(this.model)) {
	
				String termName = null;
	
				try {
					termName = OntologyUtils.getConceptName(concept.getURI());
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					this.logger.log(e.toString());
				}
				
				List<String> categories = this.controller.getCategories(finalResourcesForConcepts.get(termName).getURI());
				SKOSDataProperty dcsubject = this.factory.getSKOSDataProperty(URI.create("http://purl.org/dc/terms/subject"));
				String lang = this.langBook.getShortendLanguageCode();
				for(String cat: categories) {
					if(cat != null) {
						SKOSDataRelationAssertion sourceAssertion = this.factory.getSKOSDataRelationAssertion(concept, dcsubject, cat, lang);
						changeList.add(new AddAssertion(this.model, sourceAssertion));
					}	
				}
			}
		}
		
		try {
			this.manager.applyChanges(changeList);
		} catch (SKOSChangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public void addRelations() {
		String baseURI = Configuration.getInstance().getOntologyNS();
		
		List<SKOSChange> changeList = new ArrayList<SKOSChange>();
		
		for (SKOSConceptScheme scheme : this.model.getSKOSConceptSchemes()) {
	
			for (SKOSConcept concept : scheme.getConceptsInScheme(this.model)) {
	
				String termName = null;
	
				try {
					termName = OntologyUtils.getConceptName(concept.getURI());
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					this.logger.log(e.toString());
				}
				
				List<String> links = this.controller.getLinks(finalResourcesForConcepts.get(termName).getURI());
				SKOSRelatedProperty relProp = this.factory.getSKOSRelatedProperty();
				String lang = this.langBook.getShortendLanguageCode();
				for(String link: links) {
					for(String originalTerm:  finalResourcesForConcepts.keySet()) {
						String resourceURI = finalResourcesForConcepts.get(originalTerm).getURI();
						if(resourceURI.equals(link)) {
							try {
								URI uri = URI.create(baseURI + URLEncoder.encode(originalTerm.trim(), "UTF-8"));
								SKOSConcept linkedConcept = this.factory.getSKOSConcept(uri);
								if(linkedConcept != null) {
									SKOSObjectRelationAssertion relAssertion1 = this.factory.getSKOSObjectRelationAssertion(concept, relProp,linkedConcept);
									changeList.add(new AddAssertion(this.model, relAssertion1));
								}
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
		
		try {
			this.manager.applyChanges(changeList);
		} catch (SKOSChangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	
	public void storeModel() {
		if(this.model != null) {
			Persistence.getInstance().storeEnrichedModel(this.bookID, this.model);
		}
	}
	
	public String getBookID() {
		return bookID;
	}

	public String getCategory() {
		return category;
	}

	public Set<String> getTitleDomain() {
		return titleDomain;
	}

	public Set<Concept> getGlossary() {
		return glossary;
	}

	public Map<String, Integer> getCategories() {
		return categories;
	}

	public String getSeedDomainAbstracts() {
		return seedDomainAbstracts;
	}

	public Map<String, Map<LanguageEnum, Map<String, Pair<Set<String>, String>>>> getSeedDomainEntities() {
		return seedDomainEntities;
	}

	public Map<String, Map<LanguageEnum, Map<String, Set<Candidate>>>> getConceptCandidates() {
		return conceptCandidates;
	}

	public Map<String, Map<LanguageEnum, Map<String, Candidate>>> getFinalConceptCandidates() {
		return finalConceptCandidates;
	}

	public Map<String, Candidate> getFinalResourcesForConcepts() {
		return finalResourcesForConcepts;
	}

	public double getThresholdLimit() {
		return thresholdLimit;
	}

	public static void main(String args[]) throws Exception {
//		Enricher e = new Enricher("F3FF72ED295FC77450514F6706A3B20A", "http://dbpedia.org/resource/Category:Statistics", LanguageEnum.ENGLISH);
//		e.enrichWithDBpedia();
		//BRICKS-geography 
		Enricher e = new Enricher("0964181729E95A4FB83DDDD3645EDB43", LanguageEnum.ENGLISH);

	}
	
	
	
	

}
