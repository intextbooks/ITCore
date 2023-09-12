package intextbooks.content.enrichment.dbpedia;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import intextbooks.Configuration;
import intextbooks.SystemLogger;
import intextbooks.content.enrichment.Concept;
import intextbooks.content.enrichment.DocumentSimilarity;
import intextbooks.content.extraction.Utilities.Stemming;
import intextbooks.exceptions.EnrichedModelWIthProblems;
import intextbooks.ontologie.LanguageEnum;
import intextbooks.content.enrichment.Candidate;
import intextbooks.content.enrichment.ComparatorCandidateSimilarityScore;
import intextbooks.tools.utility.StringUtils;

public class DBpediaController {
	
	static DBpediaController instance;
	
	private String enService = "http://dbpedia.org/sparql";
	private String frService = "http://fr.dbpedia.org/sparql";
	private String deService = "http://de.dbpedia.org/sparql";
	private String esService = "http://es.dbpedia.org/sparql";
	private String nlService = "http://nl.dbpedia.org/sparql";
	private String ruService = "http://ru.dbpedia.org/sparql";
	
	private HashMap<String, String> dict = new HashMap<String, String>();
	private HashMap<String, String> domainToPrefix = new HashMap<String, String>();
	private HashMap<String, String> prefixToResourceLink = new HashMap<String, String>();
	
	private int numberOfJumps;
	private double similarityScoreThreshold;
	
	private DBpediaLocalEndpoint endpoint;
	private SystemLogger logger;
	
	private DBpediaController() {
		this.dict.put("EXPLAIN", "dbpedia-owl:abstract");
		this.dict.put("CATEGORY", "dbpprop:category");
		this.dict.put("SHOW", "dbpedia-owl:thumbnail");
		this.dict.put("REDIRECTS", "dbpedia-owl:wikiPageRedirects");
		this.dict.put("REDIRECT-PAGES", "dbpedia-owl:wikiPageRedirects-2");
		this.dict.put("LINKS", "dbpedia-owl:wikiPageWikiLink");
		this.dict.put("DISAMBIGUATES", "dbpedia-owl:wikiPageDisambiguates");
		this.dict.put("IS-FROM-DISAMBIGUATES", "dbpedia-owl:wikiPageDisambiguates-2");
		this.dict.put("RESOURCE", "rdfs:label");
		this.dict.put("SAME", "owl:sameAs");
		this.dict.put("SUBJECT", "dcterms:subject");
		this.dict.put("WIKI", "foaf:isPrimaryTopicOf");
		this.dict.put("BROADER-CATEGORY", "skos:broader");
		this.dict.put("IS-BROADER-OF", "is-skos:broader-of");
			
		this.dict.put("ENGLISH", "en");
		this.dict.put("GERMAN", "de");
		this.dict.put("FRENCH", "fr");
		this.dict.put("SPANISH", "es");
		this.dict.put("DUTCH", "nl");
		this.dict.put("RUSSIAN", "ru");
	
		this.domainToPrefix.put("en", "dbpedia:");
		this.domainToPrefix.put("fr", "dbpedia-fr:");
		this.domainToPrefix.put("de", "dbpedia-de:");
		this.domainToPrefix.put("es", "dbpedia-es:");
		this.domainToPrefix.put("nl", "dbpedia-nl:");
		this.domainToPrefix.put("ru", "dbpedia-ru:");
		
		this.prefixToResourceLink.put("dbpedia:", "http://dbpedia.org/resource/");
		this.prefixToResourceLink.put("dbpedia-fr:", "http://fr.dbpedia.org/resource/");
		this.prefixToResourceLink.put("dbpedia-de:", "http://de.dbpedia.org/resource/");
		this.prefixToResourceLink.put("dbpedia-es:", "http://es.dbpedia.org/resource/");
		this.prefixToResourceLink.put("dbpedia-nl:", "http://nl.dbpedia.org/resource/");
		this.prefixToResourceLink.put("dbpedia-ru:", "http://ru.dbpedia.org/resource/");
		
		this.numberOfJumps = Configuration.getInstance().getNumberOfJumps();
		this.similarityScoreThreshold = Configuration.getInstance().getSimilarityScoreThreshold();
		
		this.logger = SystemLogger.getInstance();
		this.endpoint = DBpediaLocalEndpoint.getInstance();
	}
	
	public static DBpediaController getInstance() {
		if(instance == null) {
			instance = new DBpediaController();
		}
		return instance;
	}
	
	public Map<String,Integer> expandDomainCategories(String seedDomainCategory) throws EnrichedModelWIthProblems {
		//Test if category is valid
		boolean validCat = this.isValidCat(seedDomainCategory);
		if(!validCat) {
			throw new EnrichedModelWIthProblems("The DBPedia category is not valid");
		}
		
		
		//Add initial seed domain category
		Map<String,Integer> categories = new HashMap<String,Integer>();
		categories.put(seedDomainCategory, 1);
		
		//query first level subcategories
		String query = this.createQuery(seedDomainCategory, this.dict.get("IS-BROADER-OF"), null, null);
		System.out.println(query);
		ResultSet res = null;
		try {
			res = this.endpoint.executeQuery(query);
		} catch (Exception e) {
			this.logger.log("@@ error 'expandDomainCategories(with arguments)' query: " + query);
		}
		
		if(res != null && res.hasNext()) {
			while(res.hasNext()) {
				QuerySolution s = res.next();
				String cat = s.get("?res").toString();
				categories.put(cat,1);
				expandDomainCategoriesAux(cat, this.numberOfJumps - 1, categories);
			}
		}
		
//		/*TESTING*/
//		for(String cat: seedDomainCategories.keySet()) {
//			System.out.println("> " + cat + " : " + seedDomainCategories.get(cat));
//		}
//		/*TESTING*/
		
		logger.log("EXPANDED CATEGORIES count: " + categories.size());
		logger.log("EXPANDED CATEGORIES count: " + categories.keySet());
		
		return categories;
	}
	
	private void expandDomainCategoriesAux(String category, int jumpsLeft, Map<String,Integer> categories) {
		if(jumpsLeft > 0) {
			String query = this.createQuery(category, this.dict.get("IS-BROADER-OF"), null, null);
			ResultSet res = null;
			try {
				res = this.endpoint.executeQuery(query);
			} catch (Exception e) {
				this.logger.log("@@ error 'expandDomainCategoriesAux' query: " + query);
			}
			
			if(res != null && res.hasNext()) {
				while(res.hasNext()) {
					QuerySolution s = res.next();
					String cat = s.get("?res").toString();
					Integer cant = categories.get(cat);
					if(cant == null) {
						cant = 1;
					} else {
						cant += 1;
					}
					categories.put(cat,cant);
					expandDomainCategoriesAux(cat, jumpsLeft - 1, categories);
				}
			}
		}
	}
	
	public Pair<Map<String, Map<LanguageEnum, Map<String, Pair<Set<String>, String>>>>, String> extractSeedDomain(Set<Concept> concepts, Map<String,Integer> seedDomainCategories) {
		
		String seedDomainAbstracts = "";
		Map<String, Map<LanguageEnum, Map<String,Pair<Set<String>,String>>>> seedDomainEntities = new HashMap<String, Map<LanguageEnum, Map<String,Pair<Set<String>,String>>>>();
		
		/*STATISTICS correctness*/
		int total = 0;
		int totalChecked = 0;
		//DBpediaStatistics db = DBpediaStatistics.getInstance();
		//Map<String, Integer> dir  = db.revisedConcepts.get(LanguageEnum.ENGLISH);
		/*STATISTICS correctness*/
	
		//each concept in the glossary
		for (Concept concept : concepts) {
					
			String termName = concept.getConceptName();
			
			//Store information
			Map<LanguageEnum, Map<String,Pair<Set<String>, String>>> termAbstractsAndDbpediaLinks = new HashMap<LanguageEnum,  Map<String,Pair<Set<String>,String>>>();
			termAbstractsAndDbpediaLinks.put(LanguageEnum.ENGLISH, new HashMap<String,Pair<Set<String>,String>>());
			termAbstractsAndDbpediaLinks.put(LanguageEnum.FRENCH, new HashMap<String,Pair<Set<String>,String>>());
			termAbstractsAndDbpediaLinks.put(LanguageEnum.GERMAN, new HashMap<String,Pair<Set<String>,String>>());
			termAbstractsAndDbpediaLinks.put(LanguageEnum.SPANISH, new HashMap<String,Pair<Set<String>,String>>());
			termAbstractsAndDbpediaLinks.put(LanguageEnum.DUTCH, new HashMap<String,Pair<Set<String>,String>>());
			termAbstractsAndDbpediaLinks.put(LanguageEnum.RUSSIAN, new HashMap<String,Pair<Set<String>,String>>());
			boolean putConcept = false;
			
			//each LANG for each concept
			for(LanguageEnum langEnum: concept.getLangs()) {
				String lang = langEnum.getShortendLanguageCode();
				
				if(!lang.equals("en")) {
					continue;
				}
//				/*TESTING*/
//				SystemLogger.getInstance().debug("CONCEPT: " + concept.getConceptName() + " L: " + lang);
//				/*TESTING*/
				
				//each LABEl for each LANG-CONCEPT
//				/*TESTING*/
//				if(concept.getAllLabels(langEnum).size() > 1) {
//					SystemLogger.getInstance().debug("Testing: " + concept.getConceptName() + " | several labels: " + concept.getAllLabels(langEnum).size());
//				}
//				/*TESTING*/
				for(String term: concept.getAllLabels(langEnum)) {
					//Only for English
					//TODO change to all langs
					if(lang.equals("en")) {
						
						String preparedString = StringUtils.normalizeDBpediaQuery(term);
						Pair<String,String> result = askSeedDomainEntity(preparedString, lang, seedDomainCategories);
						if (result != null) {
							total++;
							//if(concept.getAllLabels(langEnum).size() > 1) {
								//System.out.println("@@@@:  " + result.getKey()); 
							//}
							String URI = result.getKey();
							String abstractString = result.getValue();
							Pair<Set<String>,String> termPair = termAbstractsAndDbpediaLinks.get(LanguageEnum.convertShortendCodeToEnum(lang)).get(URI);
							if(termPair == null) {
								//put new URI
								Set<String> tmpSet = new LinkedHashSet<String>();
								tmpSet.add(term);
								termAbstractsAndDbpediaLinks.get(langEnum).put(URI, Pair.of(tmpSet, abstractString));
								seedDomainEntities.put(termName, termAbstractsAndDbpediaLinks);
								seedDomainAbstracts += " " + result.getRight();
							} else {
								//update list of labels
								termPair.getKey().add(term);
							}					
										
//								/*TESTING STATISTICS*/
//								Integer res = dir.get(termName);
//								if((res != null && res == 1) || res == null) {
//									totalChecked++;
//									int size = termAbstract.length();
//									String sub = termAbstract.substring(size-3);
//									if (sub.matches("@..")) {
//										termAbstract = termAbstract.substring(0, size-3);
//									}
//			
//								} else {
//									System.out.println("[" +res+ "]" + "()NO! -> " + termName + "(" + preparedString + ")");
//								}
//								/*TESTING STATISTICS*/
						}
						
					}
				}
			}
		}
		
		//Sort seed categories
		//Map<String, Integer> sorted = this.connector.seedDomainCategories.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(java.util.stream.Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2,  LinkedHashMap::new));
		//System.out.println("SORTED: " + sorted);
		/*Iterator<String,Integer> ;
		sorted.
		for(int i =0;i<100;i++) {
			System.out.println(x);
		}*/
		
		/*TESTING*/
		System.out.println("---------------------" );
		for(String concept: seedDomainEntities.keySet()) {
			System.out.println("C: " + concept );
			Map<String, Pair<Set<String>, String>> terms = seedDomainEntities.get(concept).get(LanguageEnum.ENGLISH);
			for(String URI : terms.keySet()) {
				Pair<Set<String>, String> pair = terms.get(URI);
				
				System.out.println("\tURI: " + URI);
				System.out.println("\t\tSize abstract: " + pair.getValue().length());
				System.out.println("\t\tLabels: ");
				for(String label: pair.getKey()) {
					System.out.println("\t\t\t " + label);
				}
				
			}
		}
		/*TESTING*/
		
//		/*TESTING STATISTICS*/
		
		System.out.println("Length: " + seedDomainAbstracts.length());
		System.out.println("TOTAL: " + total);
		System.out.println("TOTAL w: " + seedDomainEntities.keySet().size());
		System.out.println(seedDomainAbstracts);
//		System.out.println("TOTAL CHECKED: " + totalChecked);
//		System.out.println("TOTAL CATEGORIES: " + this.connector.seedDomainCategories.size());
//		/*TESTING STATISTICS*/
		
		return Pair.of(seedDomainEntities, seedDomainAbstracts);
	
	}
	
	public Map<String, Map<LanguageEnum, Map<String,Set<Candidate>>>> computeCandidatesFromGlossary(Set<Concept> bookGlossary, Map<String, Map<LanguageEnum, Map<String,Pair<Set<String>,String>>>> seedDomainEntities) {
		System.out.println("Using glossary with " + bookGlossary.size() + " concepts");
		Map<String, Map<LanguageEnum, Map<String,Set<Candidate>>>> conceptCandidates =  new HashMap<String, Map<LanguageEnum, Map<String,Set<Candidate>>>>();
		
		//Check each concept
		int conceptsChecked = 0;
		int conceptsCheckedWithCandidate = 0;
		int labelsWithCandidates= 0;
		int totalLabels =0;
		int sizeConcepts= 0;
		int count = 1;
		
		for(Concept concept: bookGlossary) {
			
			if(concept.getLangs().contains(LanguageEnum.ENGLISH) && concept.getAllLabels(LanguageEnum.ENGLISH).size() != 0) {
				sizeConcepts++;
			}
		}
		
		System.out.println("Size Eng: " + sizeConcepts);
		for(Concept concept: bookGlossary) {
			if(count % 20 == 0) {
				SystemLogger.getInstance().log("Getting candidates for term #" + count);
			}
			
			String conceptName = concept.getConceptName();
			//if(!conceptName.equals("mad")) {
			//	continue;
			//}
			boolean expandConceptForAllLangs = true;
			//Check for each lang
			Map<LanguageEnum, Map<String,Set<Candidate>>> langMap = new HashMap<LanguageEnum, Map<String,Set<Candidate>>>();
			for(LanguageEnum langEnum: concept.getLangs()) {
				//TODO remove - do for all langs
				if(langEnum != LanguageEnum.ENGLISH)
					continue;
				
					System.out.println("~#" + count + " of #" + sizeConcepts);
					count++;
					
					//Expand concept
					System.out.println("*************************");
					System.out.println(">Concept: " + conceptName);
					if(seedDomainEntities.containsKey(conceptName)) {
						System.out.println("\t> IN SEED DOMAIN, skipping ...");
						continue;
					}
					conceptsChecked++;
					Map<String, Set<Candidate>> labelsMap = new HashMap<String, Set<Candidate>>();
					//for each label
					boolean atLeatOne = false;
					for(String label : concept.getAllLabels(langEnum)) {
						System.out.println(">>>>>>>>>>>>>>Label: " + label);
						totalLabels++;
						Set<Candidate> candidates = askExpandDomainEntity(label, langEnum.getShortendLanguageCode());
						System.out.println("# of candidates: " + candidates.size());
						if(candidates == null || candidates.size() == 0) {
							System.out.println("\t\tignoring");
						} else {
							labelsMap.put(label, candidates);
							labelsWithCandidates++;
							atLeatOne = true;
						}
					}
					langMap.put(langEnum, labelsMap);
					if(atLeatOne) {
						conceptsCheckedWithCandidate++;
					}
				
			}
			conceptCandidates.put(conceptName, langMap);
		}
		System.out.println("--------------------------");
		System.out.println("conceptsChecked: " + conceptsChecked);
		System.out.println("conceptsChecked with Candidates: " + conceptsCheckedWithCandidate);
		System.out.println("totalLabels: " + totalLabels);
		System.out.println("labelsWithCandidates: " + labelsWithCandidates);
		System.out.println("--end");
		
		return conceptCandidates;
	}
	
	private String getDBpediaLink(String termName, String domain) {
			
			String link = this.prefixToResourceLink.get(this.domainToPrefix.get(domain));
			return link + StringUtils.normalizeDBpediaQuery(termName);
			
	}
	
	public void out(String v) {
		System.out.println(v);
	}
	
	public Map<String, Map<LanguageEnum, Map<String, Candidate>>> chooseFromCandidatesExpanding(String booksSeedAbstract, Map<String, Map<LanguageEnum, Map<String,Set<Candidate>>>> conceptCandidates, Map<String,Integer> seedDomainCategories, Set<String> titleDomain, double thresholdLimit) {
		out("************************** thresholdLimit: " + thresholdLimit);

		//Map<String, Map<LanguageEnum, Map<String,Set<Candidate>>>> 
		int totalConcepts = 0;
		int totalLabels = 0;
		int totalCandidates = 0;
		int totalLabelsFinal = 0;
		//average
		double top1Value = 0;
		double top1Amount = 0;
		double top2Value = 0;
		double top2Amount = 0;
		double restValue = 0;
		double restAmount = 0;
		
		double acceptedValue = 0;
		double acceptedAmount = 0;
		double rejectedValue =0;
		double rejectedAmount = 0;
		
		double threshold = 1;
		boolean stop = false;
		
		//accuracy, recall
		int correct = 0;
		
		//
		Map<String, Map<LanguageEnum, Map<String, Candidate>>> finalexpandedDomainEntities = new HashMap<String, Map<LanguageEnum, Map<String, Candidate>>>();
		while(threshold >= thresholdLimit || !stop) {
			
			stop = true;
			if(threshold < thresholdLimit) {
				threshold = thresholdLimit;
			}
			SystemLogger.getInstance().log("----- Cycle with threshold:" + threshold);
			out("----- Cycle with threshold: " + threshold);
			out("----- Cycle with stop: " + stop);
			int n = 0;
			for(String conceptName : conceptCandidates.keySet()) {
				SystemLogger.getInstance().log("Checking: " + n++);
				//ENGLISH
				Map<String,Set<Candidate>> labelsMap = conceptCandidates.get(conceptName).get(LanguageEnum.ENGLISH);
				if(labelsMap != null && labelsMap.size() > 0 ) {
					//out("> Concept: " + conceptName);
					totalConcepts++;
					
					Map<LanguageEnum, Map<String, Candidate>> tConceptMap = finalexpandedDomainEntities.get(conceptName);
					if(tConceptMap == null) {
						tConceptMap = new HashMap<LanguageEnum, Map<String, Candidate>>();
						finalexpandedDomainEntities.put(conceptName, tConceptMap);
					}
					Map<String, Candidate> tLangMap = tConceptMap.get(LanguageEnum.ENGLISH);
					if(tLangMap == null) {
						tLangMap = new HashMap<String, Candidate>();
						tConceptMap.put(LanguageEnum.ENGLISH, tLangMap);
					}
					for(String label: labelsMap.keySet()) {
						//Check if already label is final: if already selectd then don't evaluate again
						if(tLangMap.containsKey(label)) {
							continue;
						}
						out("\t>> Label: " + label);
						out("\t>> Size of candidates: " + labelsMap.get(label).size());
						//totalLabels++;
						Set<Candidate>candidates = labelsMap.get(label);
						totalCandidates += candidates.size();
						TreeSet<Candidate> cosines = computeCosineScoresAndTitle(label, booksSeedAbstract, candidates, LanguageEnum.ENGLISH, thresholdLimit, seedDomainCategories, titleDomain);
						Iterator<Candidate> iteratorCandidates = cosines.descendingIterator();
						int number = 1;
						boolean countFlag = false;
						while(iteratorCandidates.hasNext()) {
							Candidate candidate = iteratorCandidates.next();
							Double cosine = candidate.getSimilarityScore();
							if(cosine >= threshold) {
								String abstractString = this.askSeedDoaminEntityAbstract(candidate.getURI());
								booksSeedAbstract += abstractString;
								tLangMap.put(label, candidate);
							} else {
								continue;
							}
							/*if(number == 1) {
								if(cosine >= threshold) {
									totalLabels++;
									out("\t>> " + candidate.getURI() + " accepted");
									stop = false;
									tLangMap.put(label, candidate);
									String abstractString = this.askSeedDoaminEntityAbstract(candidate.getURI());
									totalLabelsFinal++;
									booksSeedAbstract += abstractString;
									countFlag = true;
									top1Value += cosine;
									top1Amount++;
									top2Value += cosine;
									top2Amount++;
									acceptedValue += cosine;
									acceptedAmount++;
									out("########### added: " + label + " - " + candidate.getURI());
									
								}
							} else  if (number == 2 && countFlag) {
								top2Value += cosine;
								top2Amount++;
								rejectedValue += cosine;
								//rejectedAmount++;
							} else {
								if(countFlag) {
									restValue += cosine;
									restAmount++;
									rejectedValue += cosine;
									//rejectedAmount++;
								}
								
							}*/
							number++;
						}
					}
					
				}
			}
			
			double scale = Math.pow(10, 1);
			threshold =  Math.round((threshold - 0.1) * scale) / scale;
			
		}
		
		return finalexpandedDomainEntities;
	}
	
	public Set<String> computeTitleDomain(String seedDomainCategory) {
		Set<String> titleDomain = new HashSet<String>();
		//prerequisite: seedDomainCategory
		String domainCategory = seedDomainCategory;
		int start = domainCategory.lastIndexOf(":")+1;
		if(start != -1) {
			String domain = domainCategory.substring(start);
			StringTokenizer st = new StringTokenizer(domain,"_");
		    while (st.hasMoreTokens()) {
		    	 titleDomain.add(Stemming.stemText(LanguageEnum.ENGLISH, st.nextToken().toLowerCase()));
		    }
		}
		return titleDomain;
	}
	
	private TreeSet<Candidate> computeCosineScoresAndTitle(String labelTitle, String seedAbstract, Set<Candidate> candidates, LanguageEnum lang, Double thresholdLimit, Map<String,Integer> seedDomainCategories, Set<String> titleDomain) {
		TreeSet<Candidate> cosines = new TreeSet<Candidate>(new ComparatorCandidateSimilarityScore());
		//check title redirect
		out("checking redirects: " + labelTitle);
		String redirects = this.getDirectedPage(labelTitle, "en", "http://dbpedia.org/sparql");
		if(redirects != null) {
			out("REDIRECTS: " + labelTitle + " <> " + redirects);
			labelTitle = redirects;
		}
		
		//find perfect match: name + domain & count same labels
		int amount_same_label = 0;
		for(Candidate candidate: candidates) {
			int cats = 0;
			for(String catt: candidate.getCategories()) {
				if(seedDomainCategories.keySet().contains(catt)) {
					cats++;
				}
			}
			//1. SAME title, and same domain
			boolean domainMatch = false;
			int countTitleTokes = 0;
			for(String tok: candidate.getTitleDomain()) {
				if(titleDomain.contains(tok)) {
					countTitleTokes++;
				}
			}
			boolean sameLabel = sameLabelTitleWithoutDomain(labelTitle, candidate.getURI());
			if(countTitleTokes == titleDomain.size() && sameLabel) {
				out("SAME TITLE AND DOMAIN (match found): " + labelTitle + " <> " + candidate.getURI());
				candidate.setSimilarityScore(1);
				cosines.add(candidate);	
				return cosines;
			} else if (sameLabel) {
				amount_same_label++;
			}
		}
		//if not perfect match
		out("--not perfect matched, checking other candidates");
		if(amount_same_label <= 1) {
			out(">>>> only one label with the same name + no domain: bump if TH is reached");
		} else {
			out(">>>> withou any bumps: several candidates with the same label name");
		}
		for(Candidate candidate: candidates) {
			//calculate cosine
			double cosine = computeCosineScore(seedAbstract, candidate, lang);
			int cats = 0;
			for(String catt: candidate.getCategories()) {
				if(seedDomainCategories.keySet().contains(catt)) {
					cats++;
				}
			}
			out("## Candidate: " + candidate.getURI() + " C: " + cosine + " cats: " + cats);
			
			if(amount_same_label <= 1 && sameLabelTitleWithoutDomain(labelTitle, candidate.getURI()) && cosine >= thresholdLimit) {
				out("SAME TITLE AND CANDIDATE TITLE WITH TH LIMIT: "+ labelTitle + " <-> " + candidate.getURI());
				cosine = 0.98 + (cosine / 100);
			}
			candidate.setSimilarityScore(cosine);
			//Check if labelTitle is present in cancidateURI or candidateAbstract
			cosines.add(candidate);
		}
		return cosines;
	}
	
	private boolean sameLabelTitleWithoutDomain(String labelTitle, String candidateURI) {
		List<String> conceptWords = new ArrayList<String>();
		String conceptURIWords = labelTitle.substring(labelTitle.lastIndexOf("/")+1);
		int last = conceptURIWords.indexOf("(");
		if(last != -1) {
			conceptURIWords = conceptURIWords.substring(0, last);
		}
		conceptURIWords = conceptURIWords.replaceAll("[_()]", " ").toLowerCase();
		StringTokenizer st = new StringTokenizer(conceptURIWords);
	    while (st.hasMoreTokens()) {
	    	conceptWords.add(st.nextToken());
	    }
	    //get set of words from candidate
	   List<String> cadidateWords = new ArrayList<String>();
	    //Check the URI
		String resourceWords = candidateURI.substring(candidateURI.lastIndexOf("/")+1);
		last = resourceWords.indexOf("(");
		if(last != -1) {
			resourceWords = resourceWords.substring(0, last);
		}
		resourceWords = resourceWords.replaceAll("[_()]", " ").toLowerCase();
		st = new StringTokenizer(resourceWords);
	    while (st.hasMoreTokens()) {
	    	cadidateWords.add(st.nextToken());
	    }
	    
	    if(conceptWords.size() == cadidateWords.size()) {
	    	for(String word: conceptWords) {
		         if(!cadidateWords.contains(word)) {
		        	 return false;
		         }
		    }
	    	return true;
	    } else {
	 	   return false;
	    }
	}
	
	private double computeCosineScore(String seedAbstract, Candidate candidate, LanguageEnum lang) {
		double cosineScore = 0;
		try {
			cosineScore = DocumentSimilarity.getSimilarity(seedAbstract, candidate.getAbstractString(), lang);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cosineScore;
	}
	
	private TreeSet<Candidate> computeCosineScores(String seedAbstract, Set<Candidate> candidates, LanguageEnum lang) {
		TreeSet<Candidate> cosines = new TreeSet<Candidate>(new ComparatorCandidateSimilarityScore());
		for(Candidate candidate: candidates) {
			double cosine = computeCosineScore(seedAbstract, candidate, lang);
			candidate.setSimilarityScore(cosine);
			cosines.add(candidate);
		}
		//return cosines.descendingIterator();
		return cosines;
	}
	
	
	
	
public Pair<String,String> askSeedDomainEntity(String term, String domain, Map<String,Integer> seedDomainCategories) {
		
		//DBpedia sparql endpoint according to language
		String service = null;
		if (domain.equals("en")) {
			service = this.enService;
		} else if (domain.equals("fr")) {
			service = this.frService;
		} else if (domain.equals("de")) {
			//service = this.deService;
			service = this.enService;
		} else if (domain.equals("es")) {
			service = this.esService;
		} else if (domain.equals("nl")) {
			service = this.nlService;
		} else if (domain.equals("ru")) {
			//service = this.ruService;
			service = this.enService;
		} else {
			service = this.enService;
		}
		
		//Pre-condition: redirects
		String originalTerm = term;
		String q0 = this.createQuery(term, this.dict.get("REDIRECTS"), null, domain);
		ResultSet res0 = null;
		try {
			res0 = this.endpoint.executeQuery(q0);
		} catch (Exception e) {
			this.logger.log("@@ error askDomainEntity service:" + service + " query: " + q0);
		}
		if(res0 != null && res0.hasNext()) {
			QuerySolution s = res0.next();
			String newTerm = s.get("?res").toString();
			term = newTerm;
			//System.out.println("OLD: " + originalTerm + " NEW: " + term);
		}
		
		//First condition
		//Query CATEGORIES(SUBJECT) of term
		String q1 = this.createQuery(term, this.dict.get("SUBJECT"), domain, domain);
		ResultSet res = null;
		try {
			res = this.endpoint.executeQuery(q1);
		} catch (Exception e) {
			this.logger.log("@@ error askDomainEntity service:" + service + " query: " + q1);
		}
		
		Boolean inDomain1 = false;
		Set<String> categories =  seedDomainCategories.keySet();
		List<String> entityCategories = new ArrayList<String>();
		if(res != null && res.hasNext()) {
			while(res.hasNext()) {
				QuerySolution s = res.next();
				String cat = s.get("?res").toString();
				entityCategories.add(cat);
				if(categories.contains(cat)) {
					inDomain1 = true;
				}
			}
		}
		
		//Second condition
		//If the page is a disambiguation page or the page is part of a disambiguation page
		if(inDomain1) {
			//Check id page is part of a disambiguation page
			String q2 = this.createQuery(term, this.dict.get("IS-FROM-DISAMBIGUATES"), null, domain);
			ResultSet res2 = null;
			try {
				res2 = this.endpoint.executeQuery(q2);
			} catch (Exception e) {
				this.logger.log("@@ error askDomainEntity service:" + service + " query: " + q2);
			}
			
			//check if page is a disambiguation page
			String q3 = this.createQuery(term, this.dict.get("DISAMBIGUATES"), null, domain);
			ResultSet res3 = null;
			try {
				res3 = this.endpoint.executeQuery(q3);
			} catch (Exception e) {
				this.logger.log("@@ error askDomainEntity service:" + service + " query: " + q3);
			}
			
			Boolean inDomain2 = false;
			if((res2 == null || !res2.hasNext()) && (res3 == null || !res3.hasNext())) {
				inDomain2 = true;
			}
			
			if(inDomain2) {
				//Check for abstract
				String q4 = this.createQuery(term, this.dict.get("EXPLAIN"), domain, domain);
				ResultSet res4 = null;
				try {
					res4 = this.endpoint.executeQuery(q4);
				} catch (Exception e) {
					this.logger.log("@@ error askDomainEntity service:" + service + " query: " + q4);
				}
				
				if(res4 != null && res4.hasNext()) {
					QuerySolution s = res4.next();
					String abstractString = s.get("?res").toString();
					
					//Option 1: add count of categories and NEW categories
					/*for(String cat: entityCategories) {
						Integer cant = seedDomainCategories.get(cat);
						if(cant == null) {
							cant = 1;
						} else {
							cant += 1;
						}
						seedDomainCategories.put(cat,cant);
					}*/
					//Option 2: only add count of categories
					for(String cat: entityCategories) {
						Integer cant = seedDomainCategories.get(cat);
						if(cant != null) {
							seedDomainCategories.put(cat,cant+1);
						}
					}
					
//					/*TESTING*/
//					System.out.println("> " + term);
//					/*TESTING*/
					
					//return Pair(url, abstract)
					if(!term.contains("http://")) {
						term = this.getDBpediaLink(term, domain);
					}
					return Pair.of(term, abstractString);
				}
				return null;
			}
		}
		return null;
	}

	public Set<Candidate> askExpandDomainEntity(String term, String domain) {
		
		String originalTerm = term;
		term = StringUtils.normalizeDBpediaQuery(term);
		
		//DBpedia sparql endpoint according to language
		String service = null;
		if (domain.equals("en")) {
			service = this.enService;
		} else if (domain.equals("fr")) {
			service = this.frService;
		} else if (domain.equals("de")) {
			//service = this.deService;
			service = this.enService;
		} else if (domain.equals("es")) {
			service = this.esService;
		} else if (domain.equals("nl")) {
			service = this.nlService;
		} else if (domain.equals("ru")) {
			//service = this.ruService;
			service = this.enService;
		} else {
			service = this.enService;
		}
		
		//List of candidates 
		Set<Candidate> candidates = new HashSet<Candidate>();
		
		//Pre-check: redirects
		String newTerm = getDirectedPage(term, domain, service);
		if(newTerm != null) {
			term = newTerm;
		} else {
			//Normalized term to DBPEDIA URI
			term = this.getDBpediaLink(term, domain);
		}
		
		//first candidate check: direct page
		String candidateAbstractsOriginal = this.endpoint.querySourceAbstractsFromTargetCandidate(term);
		if(candidateAbstractsOriginal != null && candidateAbstractsOriginal.length() != 0) {
			Set<String> categories = this.getCategoriesOfResource(term, domain, service);
			Set<String> titleDomain = this.getTitlteDomainOfResource(term, LanguageEnum.convertShortendCodeToEnum(domain));
			Candidate tempCandidate = new Candidate(term,candidateAbstractsOriginal, categories, titleDomain);
			candidates.add(tempCandidate);
			System.out.println("adding 1: " + tempCandidate);
		}
		//ONLY DIRECT ABSTRACT, NOT AGGREGATED
		/*String q1 = this.createQuery(term, this.dict.get("EXPLAIN"), domain, domain);
		ResultSet res = null;
		try {
			res = this.localEndpoint.executeQuery(q1);
		} catch (Exception e) {
			this.logger.log("@@ error ask service:" + service + " query: " + q1);
		}
		if (res != null && res.hasNext()) {
			while(res.hasNext()) {
				QuerySolution sol = res.next();
				String abstractString = StringUtils.removeLangTagFromAbstract(sol.get("?res").toString());
				Set<String> categories = this.getCategoriesOfResource(term, domain, service);
				Set<String> titleDomain = this.getTitlteDomainOfResource(term, LanguageEnum.convertShortendCodeToEnum(domain));
				Candidate tempCandidate = new Candidate(term,abstractString, categories, titleDomain);
				candidates.add(tempCandidate);
				System.out.println("adding 1: " + tempCandidate);
			}
		}*/
		
		//second candidate check: check if URI is a disambiguation page
		String q2 = this.createQuery(term, this.dict.get("DISAMBIGUATES"), null, domain);
		ResultSet res2 = null;
		try {
			res2 = this.endpoint.executeQuery(q2);
		} catch (Exception e) {
			this.logger.log("@@ error askDomainEntity DISAMBIGUATES service:" + service + " query: " + q2);
		}
		if (res2 != null && res2.hasNext()) {
			List<String> candidateURIs = new ArrayList<String>();
			while(res2.hasNext()) {
				QuerySolution sol = res2.next();
				String candidateURI = sol.get("?res").toString();
				candidateURIs.add(candidateURI);
				if(!filterCandidateURI(candidateURI, term)) {
					//check for redirects
					//Pre-check: redirects
					String newCandidate = getDirectedPage(candidateURI, domain, service);
					if(newCandidate != null) {
						candidateURI = newCandidate;			
					}
					String candidateAbstracts = this.endpoint.querySourceAbstractsFromTargetCandidate(candidateURI);
					if(candidateAbstracts != null && candidateAbstracts.length() != 0) {
						Set<String> categories = this.getCategoriesOfResource(candidateURI, domain, service);
						Set<String> titleDomain = this.getTitlteDomainOfResource(candidateURI, LanguageEnum.convertShortendCodeToEnum(domain));
						Candidate tempCandidate = new Candidate(candidateURI,candidateAbstracts, categories, titleDomain);
						candidates.add(tempCandidate);
						System.out.println("adding 2: " + tempCandidate);
					}
					
				}
				
			}
		}
		
		//third candidate check: check if URI is PART OF a disambiguation page
		List<String> listOfPages = this.checkIfPageDisabiguatesOf(term, domain, service);
		listOfPages = filterDisambiguationLinks(listOfPages, term);
		for(String candidateURI: listOfPages) {
			String candidateAbstracts = this.endpoint.querySourceAbstractsFromTargetCandidate(candidateURI);
			if(candidateAbstracts != null && candidateAbstracts.length() != 0) {
				Set<String> categories = this.getCategoriesOfResource(candidateURI, domain, service);
				Set<String> titleDomain = this.getTitlteDomainOfResource(candidateURI, LanguageEnum.convertShortendCodeToEnum(domain));
				Candidate tempCandidate = new Candidate(candidateURI,candidateAbstracts, categories, titleDomain);
				candidates.add(tempCandidate);
				System.out.println("adding 3: " + tempCandidate);
			}
		}
			
		
		
		return candidates;
	}
	
	public List<String> filterDisambiguationLinks(List<String> originalList, String concept){
		//this.logger.log("COncept: " + concept);
		List<String> newList = new ArrayList<String>();
		/*//get set of words from concept
		Set<String> conceptWords = new HashSet<String>();
		 StringTokenizer st = new StringTokenizer(concept);
	     while (st.hasMoreTokens()) {
	    	 conceptWords.add(st.nextToken().toLowerCase());
	     }
	    // this.logger.log("COncept words: " + conceptWords);
		
	     //compare the resources with the concept words
		for(String resource : originalList) {
			//this.logger.log("checking: " + resource);
			String resourceWords = resource.substring(resource.lastIndexOf("/")+1);
			resourceWords = resourceWords.replaceAll("[_()]", " ");
			st = new StringTokenizer(resourceWords);
		    while (st.hasMoreTokens()) {
		         if(conceptWords.contains(st.nextToken().toLowerCase())) {
		        	 newList.add(resource);
		        	 //this.logger.log("Added: " + resource);
		        	 break;
		         }
		    }
		}*/
		for(String candidate: originalList) {
			boolean filter = filterCandidateURI(candidate, concept);
			if(!filter) {
				newList.add(candidate);
			}
			
		}
		
		return newList;
	}
	
	public boolean filterCandidateURI(String candidateURI, String conceptURI){
		//System.out.println("checking: "+ candidateURI + " <-> " + conceptURI);
		//get set of words from concept
		List<String> conceptWords = new ArrayList<String>();
		String conceptURIWords = conceptURI.substring(conceptURI.lastIndexOf("/")+1);
		conceptURIWords = conceptURIWords.replaceAll("[_()]", " ").toLowerCase();
		StringTokenizer st = new StringTokenizer(conceptURIWords);
	    while (st.hasMoreTokens()) {
	    	conceptWords.add(st.nextToken());
	    }
	    //get set of words from candidate
	   List<String> cadidateWords = new ArrayList<String>();
	    //Check the URI
		String resourceWords = candidateURI.substring(candidateURI.lastIndexOf("/")+1);
		resourceWords = resourceWords.replaceAll("[_()]", " ").toLowerCase();
		st = new StringTokenizer(resourceWords);
	    while (st.hasMoreTokens()) {
	    	cadidateWords.add(st.nextToken());
	    }
	    
	   //1. check at least one matching word, or one matching sequence
	   for(String word: cadidateWords) {
	         if(conceptWords.contains(word)) {
	        	 return false;
	         } else {
	        	 for(String conceptWord: conceptWords) {
	        		 if(conceptWord.contains(word) || word.contains(conceptWord)) {
	        			 return false;
	        		 }	 
	        	 } 
	         }
	    }
	   
	   //2. check initials
	   if(conceptURIWords.length() == cadidateWords.size()) {
		   //System.out.println("first filter ok: " + conceptURIWords.length());
		   for(int i = 0; i < conceptURIWords.length();i++) {
			   //System.out.println("check: " + conceptURIWords.charAt(i) + "<>" +cadidateWords.get(i).charAt(0) );
			   if(conceptURIWords.charAt(i) != cadidateWords.get(i).charAt(0)) {
				   return true;
			   }
		   }
		   return false;
	   } else {
		   return true;
	   }
	}
	
	private Set<String> getCategoriesOfResource(String resource, String domain, String service){
		Set<String> categories = new HashSet<String>();
				
		//check if the resource is a disambiguation page
		String query = this.createQuery(resource, this.dict.get("SUBJECT"), null, domain);
		ResultSet res = null;
		try {
			res = this.endpoint.executeQuery(query);
		} catch (Exception e) {
		// This seems to be the only way to ignore 404 errors thrown by DBpedia if a page is on maintenance
			this.logger.log("@@ error getCategoriesOfResource service:" + service + " query: " + query);
		}
		if(res != null && res.hasNext()) {
			
			while(res.hasNext()) {
				QuerySolution sol = res.next();
				String cat = sol.get("?res").toString();
				categories.add(cat);
			}
		}
		
		return categories;
	}
	
	private Set<String> getTitlteDomainOfResource(String resource, LanguageEnum lang) {
		Set<String> titleDomain = new HashSet<String>();
		
		int start = resource.lastIndexOf("(")+1;
		int end = resource.lastIndexOf(")");
		if(start != -1 && end != -1) {
			String domain = resource.substring(start,end);
			
			StringTokenizer st = new StringTokenizer(domain,"_");
		    while (st.hasMoreTokens()) {
		    	 titleDomain.add(Stemming.stemText(lang, st.nextToken().toLowerCase()));
		    }
		}
		
	     
	   return titleDomain;
	}
	
	public List<String> checkIfPageDisabiguatesOf(String resource, String domain, String service) {
		ArrayList<String> listOfPages = new ArrayList<String>();
		
		String query = this.createQuery(resource, this.dict.get("IS-FROM-DISAMBIGUATES"), null, domain);
		ResultSet res = null;
		try {
			res = this.endpoint.executeQuery(query);
		} catch (Exception e) {
		// This seems to be the only way to ignore 404 errors thrown by DBpedia if a page is on maintenance
			this.logger.log("@@ error checkIfPageDisabiguatesOf IS-FROM-DISAMBIGUATES service:" + service + " query: " + query);
		}
		
		
		while(res != null && res.hasNext()) {
			QuerySolution sol = res.next();
			String uri = sol.get("?res").toString();
			listOfPages.addAll(getResouresFromDisambiguatePage(uri, domain, service));
		}
		
		return listOfPages;
	}
	
	/**
	 * 
	 * @param resource
	 * @param domain
	 * @param service
	 * @return
	 */
	private List<String> getResouresFromDisambiguatePage(String resource, String domain, String service){
		ArrayList<String> listOfPages = new ArrayList<String>();
		
		//check if the resource is a disambiguation page
		String query = this.createQuery(resource, this.dict.get("DISAMBIGUATES"), null, domain);
		ResultSet resDis = null;
		try {
			resDis = this.endpoint.executeQuery(query);
		} catch (Exception e) {
		// This seems to be the only way to ignore 404 errors thrown by DBpedia if a page is on maintenance
			this.logger.log("@@ error getResouresFromDisambiguatePage service:" + service + " query: " + query);
		}
		
		if(resDis != null && resDis.hasNext()) {
			
			while(resDis.hasNext()) {
				QuerySolution sol = resDis.next();
				String uri = sol.get("?res").toString();
				listOfPages.add(uri);
			}
		}
		return listOfPages;
	}
	
	
	
	public String getDirectedPage(String term, String domain, String service) {
		String q0 = this.createQuery(term, this.dict.get("REDIRECTS"), null, domain);
		ResultSet res0 = null;
		try {
			res0 = this.endpoint.executeQuery(q0);
		} catch (Exception e) {
			this.logger.log("@@ error askDomainEntity REDIRECTS service:" + service + " query: " + q0);
		}
		if(res0 != null && res0.hasNext()) {
			QuerySolution s = res0.next();
			String newTerm = s.get("?res").toString();
			return newTerm;
		} else {
			return null;
		}
	}
	
	public String getTruePage(String term, String domain, String service) {
		String result = this.getDirectedPage(term, domain, service);
		if(result == null)
			result = term;
		return result;
	}
	
	private String createQuery(String concept, String directive, String lang, String domain) {

		String query = "PREFIX dbpedia-ru: <http://ru.dbpedia.org/resource/> PREFIX dbpedia-nl: <http://nl.dbpedia.org/resource/> PREFIX dbpedia-es: <http://es.dbpedia.org/resource/> PREFIX dbpedia-de: <http://de.dbpedia.org/resource/> PREFIX dbpedia-fr: <http://fr.dbpedia.org/resource/> PREFIX dbpedia: <http://dbpedia.org/resource/> PREFIX dbpedia-owl:<http://dbpedia.org/ontology/> PREFIX dbpprop: <http://dbpedia.org/property/> PREFIX owl:<http://www.w3.org/2002/07/owl#> PREFIX dcterms: <http://purl.org/dc/terms/> PREFIX foaf: <http://xmlns.com/foaf/0.1/> PREFIX skos: <http://www.w3.org/2004/02/skos/core#> select ?res where {";

		if (lang != null) {
			
			if (directive.equals("owl:sameAs")) {
				
				if (!concept.contains("http://")) {
					
					query = query + StringUtils.encloseWithDiamonds(this.getDBpediaLink(concept, domain)) + " " + directive + " ?res. "
							+ "filter regex(?res,'" + StringUtils.getDomainRegex(LanguageEnum.convertShortendCodeToEnum(lang)) + "')" + "}";
					
				} else {
					
					query = query + StringUtils.encloseWithDiamonds(concept) + " " + directive + " ?res. "
							+ "filter regex(?res,'" + StringUtils.getDomainRegex(LanguageEnum.convertShortendCodeToEnum(lang)) + "')" + "}";
					
				}
				
			} else if (directive.equals("dcterms:subject")) {
				if (!concept.contains("http://")) {
					
					query = query + StringUtils.encloseWithDiamonds(this.getDBpediaLink(concept, domain)) + " " + directive + " ?res .}";
					
				} else {
					
					query = query + StringUtils.encloseWithDiamonds(concept) + " " + directive + " ?res .}";
					
				}
			} else if (!directive.equals("rdfs:label")) {
				
				if (!concept.contains("http://")) {
				
					query = query + StringUtils.encloseWithDiamonds(this.getDBpediaLink(concept, domain)) + " " + directive + " ?res ." + "filter(langMatches(lang(?res),'"
							+ lang + "'))" + "}";
					
				} else {
					
					query = query + StringUtils.encloseWithDiamonds(concept) + " " + directive + " ?res ." + "filter(langMatches(lang(?res),'"
							+ lang + "'))" + "}";
					
				}
				
			} 
			else {
				query = query + "?res " + directive + " \"" + StringUtils.underScoreToWhiteSpace(concept) + "\"@" + lang + "}";
			}
			
		} else {
			
			//without http
			if (!concept.contains("http://")) {
				if (directive.equals("dbpedia-owl:wikiPageDisambiguates") || directive.equals("dbpedia-owl:wikiPageWikiLink") || directive.equals("dcterms:subject") || directive.equals("foaf:isPrimaryTopicOf") || directive.equals("skos:broader") || directive.equals("dbpedia-owl:wikiPageRedirects") || directive.equals("dbpprop:category")) {				
					query = query + StringUtils.encloseWithDiamonds(this.getDBpediaLink(concept, domain)) + " " + directive + " ?res }";				
				} else if (directive.equals("dbpedia-owl:wikiPageDisambiguates-2")) {
					query = query + "?res dbpedia-owl:wikiPageDisambiguates " + StringUtils.encloseWithDiamonds(this.getDBpediaLink(concept, domain)) + " }";
				}  
				else {
					query = query + this.domainToPrefix.get(domain) + concept + " " + directive + " ?res}";
				}
			} else {
				if (directive.equals("dbpedia-owl:wikiPageDisambiguates") || directive.equals("dbpedia-owl:wikiPageWikiLink") || directive.equals("dcterms:subject") || directive.equals("foaf:isPrimaryTopicOf") || directive.equals("skos:broader") || directive.equals("dbpedia-owl:wikiPageRedirects") || directive.equals("dbpprop:category")) {				
					query = query + StringUtils.encloseWithDiamonds(concept) + " " + directive + " ?res }";				
				} else if (directive.equals("dbpedia-owl:wikiPageDisambiguates-2")) {
					query = query + "?res dbpedia-owl:wikiPageDisambiguates " + StringUtils.encloseWithDiamonds(concept) + " }";
				} else if (directive.equals("is-skos:broader-of")) {
					query = query + "?res skos:broader " + StringUtils.encloseWithDiamonds(concept) + " }";
				} else if(directive.equals("dbpedia-owl:wikiPageRedirects-2")) {
					query = query + "?res dbpedia-owl:wikiPageRedirects " + StringUtils.encloseWithDiamonds(concept) + " }";
				}
				else {
					query = query + this.domainToPrefix.get(domain) + concept + " " + directive + " ?res}";
				}
			}
		}

		return query;
	}
	
	public String askSeedDoaminEntityAbstract(String targetR) {
		return this.endpoint.getSimpleAbstract(targetR);
	}
	
	public boolean isValidCat(String cat) {
		boolean res = this.endpoint.executeAsk("PREFIX dct: <http://purl.org/dc/terms/>  ASK {?r dct:subject <" + cat + "> .} ");
		return res;
	}
	
	public String getSimpleAbstract(String resourceURI) {
		return this.endpoint.getSimpleAbstract(resourceURI);
	}
	
	public List<String> getCategories(String resourceURI) {
		return this.endpoint.getCategories(resourceURI);
	}
	
	public List<String> getOneLevelBroaderCatHierarchy(String resourceURI) {
		return this.endpoint.getOneLevelBroaderCatHierarchy(resourceURI);
	}
	
	public List<String> getBroaderOfCategories(String resourceURI) {
		return this.endpoint.getBroaderOfCategories(resourceURI);
	}
	
	public List<String> getBroaderCategories(String resourceURI) {
		return this.endpoint.getBroaderCategories(resourceURI);
	}
	
	public Set<String> getSiblingCategories(String resourceURI) {
		return this.endpoint.getSiblingCategories(resourceURI);
	}
	
	public List<String> getSubjectOfResources(String resourceURI) {
		return this.endpoint.getSubjectOfResources(resourceURI);
	}
	
	public List<String> getLinks(String resourceURI) {
		return this.endpoint.getLinks(resourceURI);
	}
	
	public String getWikiLink(String resourceURI) {
		return this.endpoint.getWikiLink(resourceURI);
	}
	
//	public Set<String> getSynonymsOfTerm(String term, String domain) {
//		Set<String> synonyms = this.getConnector().getSynonymsOfTerm(term, domain);
//		return synonyms;
//	}
	
	public static void main(String args[]) {
		DBpediaController instance = DBpediaController.getInstance();
		//instance.expandDomainCategories("http://dbpedia.org/resource/Category:Statistics");null
		System.out.print(instance.isValidCat("http://dbpedia.org/resource/Category:Statistical_tests"));
	}

}
