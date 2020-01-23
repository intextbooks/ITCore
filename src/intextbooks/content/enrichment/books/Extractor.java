package intextbooks.content.enrichment.books;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;

import org.apache.commons.text.similarity.LevenshteinDistance;

import intextbooks.Configuration;
import intextbooks.SystemLogger;
import intextbooks.content.ContentManager;
import intextbooks.content.extraction.structure.IndexGlossaryLinking;
import intextbooks.content.models.Book;
import intextbooks.ontologie.LanguageEnum;
import intextbooks.persistence.Persistence;
import intextbooks.content.enrichment.Concept;
import intextbooks.content.enrichment.dbpedia.DBpediaController;
import intextbooks.tools.utility.StringUtils;

public class Extractor {
	
	static DBpediaController controller;
	
	public static Set<Concept> readGlossary(String name) {
		try {
			//read
			System.out.println("Reading glossary: "+ name + " from disk ...");
			FileInputStream fileIn;
			ObjectInputStream in;
			fileIn = new FileInputStream("data/glossaries/" + name + ".ser");
			in = new ObjectInputStream(fileIn);
			Set<Concept> glossary = (Set<Concept>) in.readObject();
			in.close();
	        fileIn.close();
	        System.out.println("Glossary: " + name + " read from disk successfully");
			return glossary;
		} catch (Exception e) {
			System.out.println("There was a problem while reading glossary: " + name + " from disk");
			e.printStackTrace();
			return null;
		}
	}
	
	public static boolean writeGlossary(Set<Concept> glossary, String name) {
		try {
			System.out.println("Writing glossary: " + name + " to disk ...");
			FileOutputStream fileOut;
			ObjectOutputStream out;
			
			//writing abstracts
			fileOut = new FileOutputStream("data/glossaries/" + name + ".glossary.ser");
			out = new ObjectOutputStream(fileOut);
			out.writeObject(glossary);
			out.close();
			fileOut.close();
			System.out.println("Glossary: " + name + " written to disk successfully");
			return true;
		} catch (Exception e) {
			System.out.println("There was a problem while writing glossary: " + name + " to disk");
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean writeText(String name, String text) {
		BufferedWriter bw = null;
		FileWriter fw = null;

		try {
			fw = new FileWriter("data/abstracts/" + name + ".txt");
			bw = new BufferedWriter(fw);
			bw.write(text);

			System.out.println("Abstract text: " + name + " written to disk successfully");
			return true;
		} catch (IOException e) {
			System.out.println("There was a problem while writing abstract text: " + name + " to disk");
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (bw != null)
					bw.close();
				if (fw != null)
					fw.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public static String readText(String name) {
		String text = ""; 
	    try {
			text = new String(Files.readAllBytes(Paths.get("data/abstracts/" + name + ".txt")));
			System.out.println("Abstract text: " + name + " read from disk successfully");
		} catch (IOException e) {
			System.out.println("There was a problem while reading abstract text: " + name + " from disk");
			e.printStackTrace();
		} 
	    return text;
	}
	
	public static Set<Concept> extractGlossaryFromBooks(String name,List<String> ignoreList) throws SQLException{
		//get persistence layer
		Persistence persistence = Persistence.getInstance();
		//create empty glossary
		Map<String, Concept> concepts = new HashMap<String,Concept>();
		
		//get books
		ArrayList<String> booksIDs = persistence.getBookList();
		
		//create glossary, independent concepts for independent languages
		for(String bookID: booksIDs) {
			//ignore list
			if(ignoreList != null && ignoreList.contains(bookID))
				continue;
			LanguageEnum langBook = LanguageEnum.convertToEnum(persistence.getLanguage(bookID));
			//check each index term
			for(String indexTerm : persistence.getListOfIndicesWithPageForBook(bookID)) {
				//check if concept exists
				indexTerm = indexTerm.toLowerCase();
				Concept concept = concepts.get(indexTerm);
				if(concept == null) {
					//new concept
					concept = new Concept(indexTerm);
					concepts.put(indexTerm, concept);
					concept.addLabels(langBook, indexTerm, null);
					concept.getAllLabels(langBook);
				} 
			}
		}
		
		Set<Concept> glossary = new HashSet(concepts.values());
		Extractor.writeGlossary(glossary, name);
		return glossary;
	}
	
	public static Set<Concept> extractGlossaryFromBooksV2 (String name, List<String> booksIDs) throws SQLException{
		//get persistence layer
		Persistence persistence = Persistence.getInstance();
		//create empty glossary
		Map<String, Concept> concepts = new HashMap<String,Concept>();	
		//create glossary, independent concepts for independent languages
		int repeated = 0;
		int total = 0;
		Set<String> repeatedList = new HashSet<String>();
		for(String bookID: booksIDs) {
			LanguageEnum langBook = LanguageEnum.convertToEnum(persistence.getLanguage(bookID));
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
				System.out.println(label);
				System.out.println(full_label);
				if(label != null  && full_label.equals("1")) {
					conceptName = label.toLowerCase().trim();
					full = true;
				} else {
					conceptName = key_name.replaceAll(" <> ", " ").toLowerCase().trim();
				}
				//System.out.println("Name: " + conceptName);
				Concept concept = concepts.get(conceptName);
				if(concept == null) {
					//new concept
					concept = new Concept(conceptName);
					concept.setExternalConceptName(externalConceptName);
					concepts.put(conceptName, concept);
					if(full) {
						concept.addPrefLabel(langBook, label);
					} else {
						//add alt labels
						List<String> altLabels = persistence.getListOfIndexParts(id);
						//System.out.println("parts: " +altLabels.size() );
						for(String altLabel : altLabels) {
							if(!altLabel.toLowerCase().trim().equals(conceptName)) {
								concept.addAltLabel(langBook, altLabel);
							} else {
								//add pref label
								concept.addPrefLabel(langBook, altLabel);
								//System.out.println("part == conceptName: " +altLabel );
							}
						}
					}
				} else {
					repeated++;
					repeatedList.add(conceptName);
					System.out.println("repeated: " + conceptName);
				}
				//System.out.println(id + "-" + key_name + "-" + label + "-" + full_label);
			}
		}
		
		Set<Concept> glossary = new HashSet(concepts.values());
		Extractor.writeGlossary(glossary, name);
		Concept.printGlossary(glossary, LanguageEnum.ENGLISH);
		System.out.println("Number of index terms: " + total);
		System.out.println("Number of concepts: " + concepts.size());
		System.out.println("Number of REPEATED concepts: " + repeated);
		System.out.println("Number of REPEATED concepts LIST: " + repeatedList.size());
		System.out.println(Arrays.toString(repeatedList.toArray()));
		return glossary;
	}
	
	public static Set<Concept> extractGlossaryFromBooksV2OnlyPrefLabel (String name, List<String> booksIDs) throws SQLException{
		//get persistence layer
		Persistence persistence = Persistence.getInstance();
		//create empty glossary
		Map<String, Concept> concepts = new HashMap<String,Concept>();	
		//create glossary, independent concepts for independent languages
		int repeated = 0;
		int total = 0;
		Set<String> repeatedList = new HashSet<String>();
		
		//initial glossary with conceptNames
		for(String bookID: booksIDs) {
			//get list with concepts in the book
			LanguageEnum langBook = LanguageEnum.convertToEnum(persistence.getLanguage(bookID));
			List<String> conceptsInBook = persistence.getListOfUsedConceptNames(bookID);
			//add concepts to the glossary
			for(String conceptName: conceptsInBook) {
				conceptName = conceptName.toLowerCase().trim();
				Concept concept = new Concept(conceptName);
				concept.setExternalConceptName(conceptName);
				concepts.put(conceptName, concept);
				concept.addPrefLabel(langBook, conceptName);
				System.out.println("adding " + conceptName);
			}
		}
		
		for(String bookID: booksIDs) {
			System.out.println(persistence.getLanguage(bookID));
			LanguageEnum langBook = LanguageEnum.convertToEnum(persistence.getLanguage(bookID));
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
				String keyName;

				System.out.println(label);
				System.out.println(full_label);
				if(full_label.equals("1")) {
					conceptName = label.toLowerCase().trim();
					keyName = key_name.replaceAll(" <> ", " ").toLowerCase().trim();
				} else {
					conceptName = key_name.replaceAll(" <> ", " ").toLowerCase().trim();
					keyName = key_name.replaceAll(" <> ", " ").toLowerCase().trim();
				}
				
				//1. try with externalConceptName
				Concept concept = null;
				if(externalConceptName != null && !externalConceptName.equals("null")) {
					externalConceptName = externalConceptName.toLowerCase().trim();
					concept = concepts.get(externalConceptName);
				} else {
					concept = concepts.get(conceptName);
				}
				
				if(concept == null) {
					//new concept
					concept = new Concept(conceptName);
					concept.setExternalConceptName(externalConceptName);
					concepts.put(conceptName, concept);
					concept.addPrefLabel(langBook, keyName);
				} else {
					repeated++;
					repeatedList.add(conceptName);
					if(!concept.getAllLabels(langBook).contains(keyName)) {
						concept.addAltLabel(langBook, keyName);
					}
					
					System.out.println("repeated: " + conceptName);
				}
			}
		}
		
		Set<Concept> glossary = new HashSet(concepts.values());
		for(Concept c: glossary) {
			c.removeNonPrintingChars();
		}
		Extractor.writeGlossary(glossary, name);
		Concept.printGlossary(glossary, LanguageEnum.ENGLISH);
		System.out.println("Number of index terms: " + total);
		System.out.println("Number of concepts: " + concepts.size());
		System.out.println("Number of REPEATED concepts: " + repeated);
		System.out.println("Number of REPEATED concepts LIST: " + repeatedList.size());
		System.out.println(Arrays.toString(repeatedList.toArray()));
		
		System.exit(0);
		return glossary;
	}
	
	public static Set<Concept> extractGlossaryFromBooksV2PrefLabelAltLabel (String name, List<String> booksIDs) throws SQLException{
		//get persistence layer
		Persistence persistence = Persistence.getInstance();
		//create empty glossary
		Map<String, Concept> concepts = new HashMap<String,Concept>();	
		//create glossary, independent concepts for independent languages
		int repeated = 0;
		int total = 0;
		Set<String> repeatedList = new HashSet<String>();
		
		
		//initial glossary with conceptNames
		for(String bookID: booksIDs) {
			//get list with concepts in the book
			LanguageEnum langBook = LanguageEnum.convertToEnum(persistence.getLanguage(bookID));
			List<String> conceptsInBook = persistence.getListOfUsedConceptNames(bookID);
			//add concepts to the glossary
			for(String conceptName: conceptsInBook) {
				conceptName = conceptName.toLowerCase().trim();
				Concept concept = new Concept(conceptName);
				concept.setExternalConceptName(conceptName);
				concepts.put(conceptName, concept);
				concept.addPrefLabel(langBook, conceptName);
				System.out.println("adding " + conceptName);
			}
		}
		
		for(String bookID: booksIDs) {
			LanguageEnum langBook = LanguageEnum.convertToEnum(persistence.getLanguage(bookID));
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
				String keyName;
				
				System.out.println("id: " + key_name);
				System.out.println("l: " + label);
				System.out.println("fl: " + full_label);
				
				if(full_label.equals("1")) {
					conceptName = label.toLowerCase().trim();
					keyName = key_name.replaceAll(" <> ", " ").toLowerCase().trim();
				} else {
					conceptName = key_name.replaceAll(" <> ", " ").toLowerCase().trim();
					keyName = conceptName;
				}
				
				//1. try with externalConceptName
				Concept concept = null;
				if(externalConceptName != null && !externalConceptName.equals("null")) {
					externalConceptName = externalConceptName.toLowerCase().trim();
					concept = concepts.get(externalConceptName);
				} else {
					concept = concepts.get(conceptName);
				}
							
				if(concept == null) {
					//new concept
					concept = new Concept(conceptName);
					concept.setExternalConceptName(externalConceptName);
					concepts.put(conceptName, concept);

					//add alt labels
					List<String> altLabels = persistence.getListOfIndexParts(id);
					concept.addPrefLabel(langBook, keyName);
					if(!full_label.equals("1")) {
						for(String altLabel : altLabels) {
							altLabel = altLabel.toLowerCase().trim();
							if(!altLabel.equals(keyName)) {
								concept.addAltLabel(langBook, altLabel);
							}
						}
					}
					
					
				} else {
					repeated++;
					repeatedList.add(conceptName);
					if(!concept.getPrefLabel(langBook).equals(keyName)) {
						concept.addAltLabel(langBook, keyName);
					}
					System.out.println("repeated: " + conceptName);
				}
				//System.out.println(id + "-" + key_name + "-" + label + "-" + full_label);
			}
		}
		
		Set<Concept> glossary = new HashSet(concepts.values());
		for(Concept c: glossary) {
			c.removeNonPrintingChars();
		}
		Extractor.writeGlossary(glossary, name);
		Concept.printGlossary(glossary, LanguageEnum.ENGLISH);
		System.out.println("Number of index terms: " + total);
		System.out.println("Number of concepts: " + concepts.size());
		System.out.println("Number of REPEATED concepts: " + repeated);
		System.out.println("Number of REPEATED concepts LIST: " + repeatedList.size());
		System.out.println(Arrays.toString(repeatedList.toArray()));
		
		
		
		return glossary;
	}
	
	public static void intersectionFromGlossariesV2 () throws SQLException{
		int intersection = 0;
		String name = "1560525848675.glossary";
		String name2 = "1560683698011.glossary";
		Set<Concept> glossary = Extractor.readGlossary(name);
		Set<Concept> glossary2 = Extractor.readGlossary(name2);
		List<String> glossaryNames2 = new ArrayList<String>();
		for(Concept concept : glossary2) {
			glossaryNames2.add(concept.getConceptName());
		}
		for(Concept concept : glossary) {
			String conceptName = concept.getConceptName();
			if(glossaryNames2.contains(conceptName)) {
				intersection++;
				System.out.println(">: " + conceptName);
			}
		}
		System.out.println("INTERSECTION: " + intersection);
		
	}
	

	
	public static void mergeGlsosaries(String name, Set<Concept> glossary1, List<Set<Concept>> glossariesList, LanguageEnum lang) {
		int totalEqual0 = 0;
		int totalEqual = 0;
		int totalEqual2 = 0;
		int totalEqual3 = 0;
		int totalEqual5 = 0;
		int totalEqual6 = 0;
		int totalEqual7 = 0;
		Set<String> repeated0External = new HashSet<String>();
		Set<Concept> repeated0 = new HashSet<Concept>();
		Set<Concept> repeated = new HashSet<Concept>();
		Set<Concept> repeated2 = new HashSet<Concept>();
		Set<Concept> repeated3 = new HashSet<Concept>();
		Set<Concept> repeated5 = new HashSet<Concept>();
		Set<Concept> repeated6 = new HashSet<Concept>();
		Set<Concept> repeated7 = new HashSet<Concept>();
		int conceptsG1 = glossary1.size();
		List<Integer> conceptsGL = new ArrayList<Integer>();
		
		
		
		//merge glossaries into 1
		for(Set<Concept> glossary: glossariesList) {
			conceptsGL.add(glossary.size());
			Iterator<Concept> itGlossary;
			
			//** 0 extact external concept match
			System.out.println("0 b: " + glossary.size());
			itGlossary = glossary.iterator();
			while(itGlossary.hasNext()) {
				Concept concept = itGlossary.next();
				//System.out.println(concept);
				String externalConcept = concept.getExternalConceptName();
				if(externalConcept != null) {
					for(Concept conceptO:glossary1) {
						String oExternalConcept = conceptO.getExternalConceptName();
						if(oExternalConcept != null && oExternalConcept.equals(externalConcept)) {
							Set<String> allLabels = concept.getAllLabels(lang);
							for(String newLabel: allLabels) {
								if(!conceptO.containsLabel(lang, newLabel)) {
									conceptO.addAltLabel(lang, newLabel);
									System.out.println("adding: " + newLabel + " to: " + conceptO.getConceptName() + " (from: " + conceptO.getConceptName() + ")");
								}
							}
							totalEqual0++;
							repeated0.add(concept);
							if(repeated0External.contains(oExternalConcept)) {
								System.out.println("original: " + conceptO.getConceptName());
								System.out.println("g: " + concept.getConceptName());
								System.out.println("external: " + oExternalConcept);
							}
							repeated0External.add(oExternalConcept);
							itGlossary.remove();
							break;
						}
					}
				}
			}
			
			System.out.println("total:  " + totalEqual0 + " | " + repeated0External.size());
			//System.out.println("0 a: " + glossary.size());
			//System.out.println("0 a: " + repeated0);
			//System.exit(0);
			
			//** 1.1 exact match prefLabel
			itGlossary = glossary.iterator();
			while(itGlossary.hasNext()) {
				Concept concept = itGlossary.next();
				//System.out.println(concept);
				String gPrefLabel = concept.getPrefLabel(lang);
				for(Concept conceptO:glossary1) {
					String oPrefLabel = conceptO.getPrefLabel(lang);
					if(oPrefLabel.equals(gPrefLabel)) {
						totalEqual++;
						repeated.add(concept);
						itGlossary.remove();
						Set<String> allLabels = concept.getAllLabels(lang);
						for(String newLabel: allLabels) {
							if(!conceptO.containsLabel(lang, newLabel)) {
								conceptO.addAltLabel(lang, newLabel);		
								System.out.println("adding: " + newLabel + " to: " + conceptO.getConceptName() + " (from: " + conceptO.getExternalConceptName() + ")");
							}
						}
						break;
					}
					Set<String> oAltLabels = conceptO.getAltLabels(lang);
					for(String oAltLabel: oAltLabels) {
						if(oAltLabel.equals(gPrefLabel)) {
							totalEqual++;
							repeated.add(concept);
							itGlossary.remove();
							Set<String> allLabels = concept.getAllLabels(lang);
							for(String newLabel: allLabels) {
								if(!conceptO.containsLabel(lang, newLabel)) {
									conceptO.addAltLabel(lang, newLabel);
									System.out.println("adding: " + newLabel + " to: " + conceptO.getConceptName() + " (from: " + conceptO.getExternalConceptName() + ")");
								}
							}
							break;
						}
					}
				}
			}
			
			System.out.println("1.1 a: " + glossary.size());
			
			
			//** 1.2 alt labels exactMatch
			itGlossary = glossary.iterator();
			while(itGlossary.hasNext()) {
				Concept concept = itGlossary.next();
				Set<String> gAltLabels  = concept.getAltLabels(lang);
				concept:
				for(String gAltLabel:  gAltLabels) {
					for(Concept conceptO:glossary1) {
						String oPrefLabel = conceptO.getPrefLabel(lang);
						if(oPrefLabel.equals(gAltLabel)) {
							totalEqual++;
							repeated.add(conceptO);
							conceptO.addAltLabel(lang, concept.getConceptName());
							itGlossary.remove();
							Set<String> allLabels = concept.getAllLabels(lang);
							for(String newLabel: allLabels) {
								if(!conceptO.containsLabel(lang, newLabel)) {
									conceptO.addAltLabel(lang, newLabel);
									System.out.println("adding: " + newLabel + " to: " + conceptO.getConceptName() + " (from: " + conceptO.getExternalConceptName() + ")");
								}
							}
							break concept;
						}
					}
				}
			}
			
			System.out.println("1.2 a: " + glossary.size());
			
			
//			System.out.println("Repeated: " + totalEqual);
//			Concept.printGlossary(repeated, lang);
//			System.exit(0);
//			
			//** 2 words without special chars
			System.out.println("############################ 3");
			itGlossary = glossary.iterator();
			while(itGlossary.hasNext()) {
				Concept concept = itGlossary.next();
				Set<String> gAllLabels  = concept.getAllLabels(lang);
				concept:
				for(String gAllLabel:  gAllLabels) {
					gAllLabel = gAllLabel.replaceAll("[^a-zA-Z0-9]", "");
					for(Concept conceptO:glossary1) {
						Set<String> oAllLabels = conceptO.getAllLabels(lang);
						for(String oAllLabel:  oAllLabels) {
							oAllLabel = oAllLabel.replaceAll("[^a-zA-Z0-9]", "");
							if(oAllLabel.equals(gAllLabel)){
								totalEqual2++;
								repeated2.add(conceptO);
								conceptO.addAltLabel(lang, concept.getConceptName());
								itGlossary.remove();
								Set<String> allLabels = concept.getAllLabels(lang);
								for(String newLabel: allLabels) {
									if(!conceptO.containsLabel(lang, newLabel)) {
										System.out.println("BEF: " +conceptO );
										conceptO.addAltLabel(lang, newLabel);
										System.out.println("after: " +conceptO );
										System.out.println("adding: " + newLabel + " to: " + conceptO.getConceptName() + " (from: " + conceptO.getExternalConceptName() + ")");
									}
								}
//								System.out.println("***");
//								System.out.println("OConcept: " + conceptO.getConceptName() + " " + conceptO.getConceptName().trim().length());
//								System.out.println("EConcept: " + concept.getConceptName() + " " + concept.getConceptName().trim().length());
//								for(char c: concept.getConceptName().toCharArray()) {
//									System.out.println("\tc: " + c);
//									System.out.println("\t#: " + ((int) c));
//								}
								break concept;
							}
						}
						
					}
				}
			}
			
			System.out.println("2 a: " + glossary.size());
			
//			System.out.println("Repeated2 (spacial chars: " + totalEqual2);
//			Concept.printGlossary(repeated2, lang);
//			System.exit(0);
			
			//** 3 words with abbreviation in ()
			System.out.println("############################ 4");
			itGlossary = glossary.iterator();
			while(itGlossary.hasNext()) {
				Concept concept = itGlossary.next();
				Set<String> gAllLabels  = concept.getAllLabels(lang);
				concept:
					for(String gAllLabel:  gAllLabels) {
						if(gAllLabel.contains("(") && gAllLabel.contains(")") && gAllLabel.lastIndexOf(")") == (gAllLabel.trim().length() -1) && gAllLabels.size() == 1){
							gAllLabel = gAllLabel.replaceAll("[^a-zA-Z0-9()]", "");
							Pair<String,String> words = Extractor.separateWords(gAllLabel);
							String w1 = words.getKey();
							String w2 = words.getValue();
							
							for(Concept conceptO:glossary1) {
								Set<String> oAllLabels = conceptO.getAllLabels(lang);
								for(String oAllLabel:  oAllLabels) {
									oAllLabel = oAllLabel.replaceAll("[^a-zA-Z0-9()]", "");
									if(oAllLabel.contains("(") && oAllLabel.lastIndexOf(")") == (oAllLabel.trim().length() -1) && oAllLabels.size() == 1) {
										Pair<String,String> wordsO = Extractor.separateWords(oAllLabel);
										String w1O = wordsO.getKey();
										String w2O = wordsO.getValue();
										if(w1O.equals(w1) || w1O.equals(w2) || w2O.equals(w1) || w2O.equals(w2)) {
											totalEqual3++;
											repeated3.add(concept);
											conceptO.addAltLabel(lang, concept.getConceptName());
											itGlossary.remove();
											Set<String> allLabels = concept.getAllLabels(lang);
											for(String newLabel: allLabels) {
												if(!conceptO.containsLabel(lang, newLabel)) {
													System.out.println("BEF: " +conceptO );
													conceptO.addAltLabel(lang, newLabel);
													System.out.println("after: " +conceptO );
													System.out.println("adding: " + newLabel + " to: " + conceptO.getConceptName() + " (from: " + conceptO.getExternalConceptName() + ")");
												}
											}
											break concept;
										}
									} else {
										if(oAllLabel.equals(w1) || oAllLabel.equals(w2)) {
											totalEqual3++;
											repeated3.add(conceptO);
											conceptO.addAltLabel(lang, concept.getConceptName());
											itGlossary.remove();
											Set<String> allLabels = concept.getAllLabels(lang);
											for(String newLabel: allLabels) {
												if(!conceptO.containsLabel(lang, newLabel)) {
													System.out.println("BEF: " +conceptO );
													conceptO.addAltLabel(lang, newLabel);
													System.out.println("after: " +conceptO );
													System.out.println("adding: " + newLabel + " to: " + conceptO.getConceptName() + " (from: " + conceptO.getExternalConceptName() + ")");
												}
											}
											break concept;
										}
									}
								}	
							}
						}
					}
			}
			
			System.out.println("3 a: " + glossary.size());
			
//			System.out.println("Repeated3 (()): " + totalEqual3);
//			Concept.printGlossary(repeated3, lang);
//			System.exit(0);
			
//			//** 5 similar words: Interlingua
//			System.out.println("############################ 5");
//			itGlossary = glossary.iterator();
			double currentMatchRatio, highestmatchRatio = 0;
//			while(itGlossary.hasNext()) {
//				highestmatchRatio = 0.9;
//				boolean match = false;
//				Concept conceptToLink = null;
//				Concept concept = itGlossary.next();
//				Set<String> gAllLabels  = concept.getAllLabels(lang);
//				concept:
//					for(String gAllLabel:  gAllLabels) {
//						gAllLabel = gAllLabel.replaceAll("-", " ");
//						gAllLabel = IndexGlossaryLinking.preProcessParentesis(gAllLabel);
//						
//						for(Concept conceptO:glossary1) {
//							Set<String> oAllLabels = conceptO.getAllLabels(lang);
//							for(String oAllLabel:  oAllLabels) {
//								oAllLabel = oAllLabel.replaceAll("-", " ");
//								oAllLabel = IndexGlossaryLinking.preProcessParentesis(oAllLabel);
//								
//								currentMatchRatio = IndexGlossaryLinking.searchForMatchingWords(IndexGlossaryLinking.getStemmedStringArray(gAllLabel.split(" |-"), lang, gAllLabel),IndexGlossaryLinking.getStemmedStringArray(oAllLabel.split(" "),lang, oAllLabel));
//							
//								if(gAllLabel.equals("database") && oAllLabel.equals("databases") ) {
//									System.out.println("~~~~~~~~~~~ DB");
//									System.out.println(currentMatchRatio);
//								}
//								
//								if(currentMatchRatio > highestmatchRatio ){
//									highestmatchRatio = currentMatchRatio;						
//									match = true;
//									conceptToLink = conceptO;
//								}	
//							
//							}
//						}
//					}
//				if(match) {
//					totalEqual5++;
//					repeated5.add(concept);
//					conceptToLink.addAltLabel(lang, concept.getConceptName());
//					itGlossary.remove();
//					System.out.println(">> match G2: " + concept.getConceptName() + " GO: " + conceptToLink.getConceptName());
//				}
//			}
//			
//			System.out.println("Repeated3 (()): " + totalEqual5);
//			Concept.printGlossary(repeated5, lang);
//			System.exit(0);
			
			//** 6 synonym with DBPEDIA
			System.out.println("############################ 6 DBPEDIA ");
			itGlossary = glossary.iterator();
			while(itGlossary.hasNext()) {
				highestmatchRatio = 0.92;
				boolean match = false;
				Concept conceptToLink = null;
				Concept concept = itGlossary.next();
				Set<String> gAllLabels  = concept.getAllLabels(lang);
				concept:
					for(String gAllLabel:  gAllLabels) {
						Set<String> synonyms = Extractor.getSynonymsFromWikipedia(gAllLabel);
						for(String synonym: synonyms) {
							synonym = synonym.replaceAll("-", " ");
							synonym = synonym.replaceAll("_", " ");
							synonym = synonym.replaceAll("’", " ");
							synonym = synonym.replaceAll("'", " ");
							synonym = IndexGlossaryLinking.preProcessParentesis(synonym);
							for(Concept conceptO:glossary1) {
								Set<String> oAllLabels = conceptO.getAllLabels(lang);
								for(String oAllLabel:  oAllLabels) {
									oAllLabel = oAllLabel.replaceAll("-", " ");
									oAllLabel = oAllLabel.replaceAll("’", " ");
									oAllLabel = oAllLabel.replaceAll("'", " ");
									oAllLabel = IndexGlossaryLinking.preProcessParentesis(oAllLabel);
									
									currentMatchRatio = IndexGlossaryLinking.searchForMatchingWords(IndexGlossaryLinking.getStemmedStringArray(synonym.split(" |-"), lang, synonym),IndexGlossaryLinking.getStemmedStringArray(oAllLabel.split(" "),lang, oAllLabel));
									//if(currentMatchRatio > highestmatchRatio ){
									if(oAllLabel.equals(synonym) ){
										highestmatchRatio = currentMatchRatio;						
										match = true;
										conceptToLink = conceptO;
									}
									
//									if(concept.getConceptName().equals("bayes’ theorem") && conceptO.getConceptName().equals("bayes’ rule")) {
//										System.out.println(Arrays.toString(IndexGlossaryLinking.getStemmedStringArray(synonym.split(" |-"), lang, synonym)));
//										System.out.println(Arrays.toString(IndexGlossaryLinking.getStemmedStringArray(oAllLabel.split(" |-"), lang, oAllLabel)));
//										System.out.println("C: " + synonym + " VS. " + oAllLabel);
//										System.out.println("r: " + currentMatchRatio);
//										System.out.println(currentMatchRatio > highestmatchRatio);
//									}
								}
							}
						}
						
					}
				if(match) {
					System.out.println("------------------------------------");
					if(conceptToLink.getExternalConceptName() != null && concept.getExternalConceptName() != null 
							&& !conceptToLink.getExternalConceptName().equals(concept.getExternalConceptName())){
						//System.out.println("ERROR: >>> " +conceptToLink.getExternalConceptName() + " vs. " + concept.getExternalConceptName() );
						continue;
					}
					
					totalEqual6++;
					repeated6.add(concept);
					System.out.println("BEF1 : " +conceptToLink );
					conceptToLink.addAltLabel(lang, concept.getConceptName());
					System.out.println("AFTER 1: " +conceptToLink );
					itGlossary.remove();
					Set<String> allLabels = concept.getAllLabels(lang);
					for(String newLabel: allLabels) {
						if(!conceptToLink.containsLabel(lang, newLabel)) {
							System.out.println("adding: " + newLabel + " to: " + conceptToLink.getConceptName() + " (from: " + concept + ")");
							System.out.println("> bef: " +conceptToLink );
							conceptToLink.addAltLabel(lang, newLabel);
							System.out.println("> after: " +conceptToLink );
							
						}
					}
					if(conceptToLink.getExternalConceptName() == null && concept.getExternalConceptName() != null) {
						conceptToLink.setExternalConceptName(concept.getExternalConceptName());
						System.out.println("CONCEPT NAME: " + conceptToLink);
					}
					
					System.out.println("6 >> match G2: " + concept + " GO: " + conceptToLink.getConceptName());
				}
			}
			
			System.out.println("6 a: " + glossary.size());
			//System.exit(0);
			
//			System.out.println("Repeated6 (()): " + totalEqual6);
//			Concept.printGlossary(repeated6, lang);
//			System.exit(0);
			
//			//** 7 words with / 
//			System.out.println("############################ 7");
//			itGlossary = glossary.iterator();
//			while(itGlossary.hasNext()) {
//				Concept concept = itGlossary.next();
//				Set<String> gAllLabels  = concept.getAllLabels(lang);
//				concept:
//					for(String gAllLabel:  gAllLabels) {
//						if(gAllLabel.contains("/") && gAllLabel.indexOf(" ") == -1){
//							gAllLabel = gAllLabel.replaceAll("[^a-zA-Z0-9()/]", "");
//							Pair<String,String> words = Extractor.separateWordsSlash(gAllLabel);
//							String w1 = words.getKey();
//							String w2 = words.getValue();
//							
//							for(Concept conceptO:glossary1) {
//								Set<String> oAllLabels = conceptO.getAllLabels(lang);
//								for(String oAllLabel:  oAllLabels) {
//									oAllLabel = oAllLabel.replaceAll("[^a-zA-Z0-9()/]", "");
//									if(oAllLabel.contains("/") && gAllLabel.indexOf(" ") == -1) {
//										Pair<String,String> wordsO = Extractor.separateWordsSlash(oAllLabel);
//										String w1O = wordsO.getKey();
//										String w2O = wordsO.getValue();
//										if(w1O.equals(w1) || w1O.equals(w2) || w2O.equals(w1) || w2O.equals(w2)) {
//											totalEqual7++;
//											repeated7.add(concept);
//											conceptO.addAltLabel(lang, concept.getConceptName());
//											itGlossary.remove();
//											System.out.println("7.1 >> match G2: " + concept.getConceptName() + " GO: " + conceptO.getConceptName());
//											System.out.println(" >> " + oAllLabel);
//											break concept;
//										}
//									} else {
//										if(oAllLabel.equals(w1) || oAllLabel.equals(w2)) {
//											totalEqual7++;
//											repeated7.add(concept);
//											conceptO.addAltLabel(lang, concept.getConceptName());
//											itGlossary.remove();
//											System.out.println("7.2 >> match G2: " + concept.getConceptName() + " GO: " + conceptO.getConceptName());
//											System.out.println("gAllLabel: " + gAllLabel );
//											System.out.println("oAllLabel: " + oAllLabel );
//											System.out.println("w1: " + w1 );
//											System.out.println("w2: " + w2 );
//											break concept;
//										}
//									}
//								}	
//							}
//						}
//					}
//			}
			
			System.out.println("before: " +  glossary1.size());
			System.out.println("restante" +  glossary.size());
			
			//restante
			itGlossary = glossary.iterator();
			while(itGlossary.hasNext()) {
				Concept concept = itGlossary.next();
				glossary1.add(concept);
				//concept.removeAltLabels(lang);
			}
			
			System.out.println("after" +  glossary1.size());
			
//			System.out.println("Repeated7: " + totalEqual7);
//			Concept.printGlossary(repeated7, lang);
//			System.exit(0);
			
//			//NOT USE
//			//** 6 similar words: LevenshteinDistance()
//			System.out.println("############################ 6");
//			itGlossary = glossary.iterator();
//			while(itGlossary.hasNext()) {
//				Concept concept = itGlossary.next();
//				Set<String> gAllLabels  = concept.getAllLabels(lang);
//				concept:
//					for(String gAllLabel:  gAllLabels) {
//						gAllLabel = gAllLabel.replaceAll("[^a-zA-Z0-9]", "");
//						for(Concept conceptO:glossary1) {
//							Set<String> oAllLabels = conceptO.getAllLabels(lang);
//							for(String oAllLabel:  oAllLabels) {
//								oAllLabel = oAllLabel.replaceAll("[^a-zA-Z0-9]", "");
//								if(similarityOfString(oAllLabel, gAllLabel) > 0.95 && !oAllLabel.equals(gAllLabel)){
//									totalEqual6++;
//									repeated6.add(concept);
//									conceptO.addAltLabel(lang, concept.getConceptName());
//									itGlossary.remove();
//									System.out.println(">> match LL: " + concept.getConceptName() + " GO: " + conceptO.getConceptName());
//									break concept;
//								}
//							}
//						}
//					}
//			}
//			System.exit(0);
		
			
		}
		
		
		System.out.println("Origianl size G1: " + conceptsG1);
		System.out.println("Origianl size GL: " + conceptsGL);
		System.out.println("Repeated External: " + repeated0External.size());
		//Concept.printGlossary(repeated0, lang);
		System.out.println("Repeated: " + totalEqual);
		//Concept.printGlossary(repeated, lang);
		System.out.println("Repeated2: " + totalEqual2);
		//Concept.printGlossary(repeated2, lang);
		System.out.println("Repeated3: " + totalEqual3);
		//Concept.printGlossary(repeated3, lang);
		System.out.println("Repeated5: " + totalEqual5);
		//Concept.printGlossary(repeated5, lang);
		System.out.println("Repeated6: " + totalEqual6);
		//Concept.printGlossary(repeated6, lang);
		System.out.println("Repeated7: " + totalEqual7);
		//Concept.printGlossary(repeated7, lang);
		System.out.println("Final glossary size: " +  glossary1.size());
		
		writeGlossary(glossary1,  name);
		Concept.printGlossary(glossary1, lang);
		
	}
	
	private static Pair<String, String> separateWords(String text){
		int iStart = text.indexOf("(");
		int iEnd = text.indexOf(")");
	
		String w1 = text.substring(0, iStart).trim();
		String w2 = text.substring(iStart+1, iEnd);
		
		return Pair.of(w1, w2);
	}
	
	private static Pair<String, String> separateWordsSlash(String text){
		int iStart = text.indexOf("/");
		
	
		String w1 = text.substring(0, iStart).trim();
		String w2 = text.substring(iStart+1);
		
		return Pair.of(w1, w2);
	}
	
	 private static double similarityOfString(String s1, String s2) {
	        String longer = s1, shorter = s2;
	        if (s1.length() < s2.length()) { // longer should always have greater length
	            longer = s2; shorter = s1;
	        }
	        int longerLength = longer.length();
	        if (longerLength == 0) { return 1.0; /* both strings are zero length */ }
	        /* // If you have StringUtils, you can use it to calculate the edit distance:
	        return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter)) /
	                                                             (double) longerLength; */
	        LevenshteinDistance lD = new LevenshteinDistance();
	        return (longerLength - lD.apply(longer, shorter)) / (double) longerLength;
	    }
	 
	 public static Set<String> getSynonymsFromWikipedia(String word){
		 
		 if(Extractor.controller == null) {
			 Extractor.controller = DBpediaController.getInstance();
		 }
		 
		//Set<String> synonyms = Extractor.controller.getSynonymsOfTerm(word,  LanguageEnum.ENGLISH.getShortendLanguageCode());
		return null;
	 }
	
	
	
	/*public static Set<Concept> combineGlossary(String name, Set<Concept> glossary1, Set<Concept> glossary2) throws SQLException{
		//create empty glossary
		Map<String, Concept> concepts = new HashMap<String,Concept>();
		//check glossary 1
		for(Concept concept: glossary1) {
			//check if concept exist in glossary 2
			if(glossary2.contains(concept)) {
				//check each language
				
			}
		}
		
		Set<Concept> glossary = new HashSet(concepts.values());
		Extractor.writeGlossary(glossary, name);
		return glossary;
	}*/
	
	public static boolean isValidEndOfSentance(int index, String allText) {
		try {
			//check for number: 5.6
			if(Character.isDigit(allText.charAt(index-1)) && Character.isDigit(allText.charAt(index+1))) {
				return false;
			}
			
			//check for: ...
			if(allText.charAt(index+1) == '.' && allText.charAt(index+2) == '.') {
				return false;
			}
			if(allText.charAt(index-1) == '.' && allText.charAt(index+1) == '.') {
				return false;
			}
			if(allText.charAt(index-2) == '.' && allText.charAt(index-1) == '.') {
				return false;
			}
			
			//check for: e.g.
			if(allText.charAt(index-1) == 'e' && allText.charAt(index+1) == 'g' && allText.charAt(index+2) == '.') {
				return false;
			}
			if(allText.charAt(index-3) == 'e' && allText.charAt(index-2) == '.' && allText.charAt(index-1) == 'g') {
				return false;
			}
			
			return true;
			
		} catch(Exception e) {
			return true;
		}
	}
	
	public static int getNextValidEndOfSentance(int index, String allText) {
		while (index >= 0) {
			 index = allText.indexOf('.', index + 1);
			 if(Extractor.isValidEndOfSentance(index, allText)){
			    	return index;
			 }
		}
		return -1;
	}
	
	public static String extractParagraph(String searchTerm, String allText) {
		//to lower
		searchTerm = searchTerm.toLowerCase();
		allText = allText.toLowerCase();
				
		int startTerm;
		String regex = "\\b"+searchTerm+"\\b";
	    Pattern pattern = Pattern.compile(regex);
	    Matcher matcher = pattern.matcher(allText);
		if(matcher.find() == true) {
			startTerm = matcher.start();
		} else {
			return null;
		}
		System.out.println("  "+ allText.length());
		System.out.println("  ^"+ allText + "^");
		System.out.println(" intex index: "+ startTerm);
		if(startTerm == -1)
			return null;
		
		int index = 0;
		int newIndex;
		String[] buffer = new String[5];
		int indexBuffer = 0;
		String sentence;
		while (index >= 0 && indexBuffer <= 4) {
		    newIndex = Extractor.getNextValidEndOfSentance(index, allText);
		    
		    if(newIndex == -1) {
		    	sentence = allText.substring(index + 1);
		    } else {
		    	if(index == 0) {
		    		index--;
		    	}
		    	sentence = allText.substring(index + 1, newIndex + 1);
		    }
		    if( index <= startTerm && startTerm <= newIndex || (newIndex == -1 && index <= startTerm)) {
		    	indexBuffer = 2;
		    } else if(indexBuffer == 2) {
		    	buffer[0] = buffer[1];
		    	indexBuffer = 1;
		    }
		    
		    buffer[indexBuffer] = sentence;
		    indexBuffer++;
		    index = newIndex;
		}
		
		sentence = "";
		for(int i = 0; i < 5; i++) {
			if(buffer[i] != null) {
				sentence += buffer[i];
			}
		}
		return sentence;
		
	}
	
	public static String extractSentence(String searchTerm, String allText) {
		//to lower
		searchTerm = searchTerm.toLowerCase();
		allText = allText.toLowerCase();
		
		int startTerm;
		String regex = "\\b"+searchTerm+"\\b";
	    Pattern pattern = Pattern.compile(regex);
	    Matcher matcher = pattern.matcher(allText);
		if(matcher.find() == true) {
			startTerm = matcher.start();
		} else {
			return null;
		}
		
		String before = allText.substring(0, startTerm);
		String after = allText.substring(startTerm);
		
		int startSentence = before.lastIndexOf(".");
		String firstPart = before.substring(startSentence + 1).trim();
		
		int endSentence = after.indexOf(".");
		if(endSentence == -1)
			endSentence = after.length()-1;
		String lastPart = after.substring(0, endSentence).trim();
		
		String sentence = firstPart + " " + lastPart;
		
		return sentence;
	}
	
	public static Set<String> extractSentenceV2(String searchTerm, ArrayList<String> sentences) {
		//to lower
		searchTerm = StringUtils.preProcessParentesis(searchTerm.toLowerCase());
		
		//result
		Set<String> result = new HashSet<String>();
		
		for(String sentence: sentences) {
			if(sentence.contains(searchTerm)) {
				result.add(sentence);
			}
		}
		
		return result;
	}
	
	public static Set<String> extractParagraphV2(String searchTerm, ArrayList<String> sentences) {
		//to lower
		searchTerm = StringUtils.preProcessParentesis(searchTerm.toLowerCase());
		
		//result
		Set<String> result = new HashSet<String>();
		
		for(int i = 0; i < sentences.size(); i++) {
			String sentence = sentences.get(i);
			List<String> paragraph = new ArrayList<String>();
			if(sentence.contains(searchTerm)) {
				if(i - 2 >= 0) {
					paragraph.add(sentences.get(i-2));
				}
				if(i - 1 >= 0) {
					paragraph.add(sentences.get(i-1));
				}
				paragraph.add(sentence);
				if(i + 1 < sentences.size()) {
					paragraph.add(sentences.get(i+1));
				}
				if(i + 2 < sentences.size()) {
					paragraph.add(sentences.get(i+2));
				}
				String fullParagraph = "";
				for(String text: paragraph) {
					fullParagraph += text;
				}
				result.add(fullParagraph);
//				System.out.println("--------------------------------------------");
//				System.out.println("> " + searchTerm);
//				System.out.println(fullParagraph);
			}
		}
		
		return result;
	}
	
	public static String extractSeedAbstractAllIndexTerms(String name, LanguageEnum lang, List<String> ignoreList, int type) {
		//Content Manager
		ContentManager cm = ContentManager.getInstance();
		cm.loadBookModels();
		
		//result
		StringBuilder sb = new StringBuilder();
		
		//get books
		List<String> langBooks = cm.getListOfAllBooks(lang);
		langBooks.removeAll(ignoreList);
		
		//get introduction for each index term of each book
		// source of code: class IntroductoryTextFetcher
		for(String bookID : langBooks) {
			
			List<String> indexTerms = cm.getListOfIndicesWithPages(bookID);
			
			for (String index : indexTerms) {
				
				System.out.println("Index term: " + index);
			
				//if(!index.toLowerCase().equals("boxplot")) {
				//	continue;
				//}
				
				int page = cm.getFirstOccurrenceOfIndex(bookID, index);
				//@i.alpizarchacon align page with numbers on segments + 1 
				page = page+1;
				List<Integer> segments = cm.getSegmentsOnPage(bookID, page);
				
				System.out.println("page: " + page);
				
				Map<Integer, String> segmentsMap = new HashMap<Integer,String>();
				for (int segID : segments) {	
					if (cm.isSegmentParagraph(bookID, segID)) {
						System.out.println("segment: " + segID);
						
					
						File parFile = cm.getParagraphText(bookID, segID);
						BufferedReader reader = null;
						StringBuilder text = new StringBuilder();
						try {
							
							reader = new BufferedReader(new FileReader(parFile));
							String line = null;
							
							while ((line = reader.readLine()) != null) {
								text.append(line);
							}
							
						} catch (FileNotFoundException e) {
							e.printStackTrace();
							SystemLogger.getInstance().log(e.toString());
						} catch (IOException e) {
							e.printStackTrace();
							SystemLogger.getInstance().log(e.toString());
						} finally {
							
							try {
								reader.close();
							} catch (IOException e) {
								e.printStackTrace();
								SystemLogger.getInstance().log(e.toString());
							}
							
						}
						String segmentText = text.toString();
						segmentsMap.put(segmentText.length(), segmentText);						
					}
				}
				//Find smaller segment
				Integer smallest = 0;
				boolean first = true;
				for(Integer segmentSize : segmentsMap.keySet()) {
					if(first) {
						smallest = segmentSize;
						first = false;
					}
					if(segmentSize < smallest) {
						smallest = segmentSize;
					}
				}
				
				String result = null;
				String segmentText = segmentsMap.get(smallest);
				if(segmentText != null && segmentText != "") {
					if(type == 0) {
						result = extractSentence(index, segmentText);
					} else {
						result = extractParagraph(index, segmentText);
					}
					if(result != null && result != "") {
						sb.append(result);
						sb.append(" \n");
					}
					
				}
				
			}
		}
		String text = sb.toString();
		System.out.println("Size of seed abstract: " + text.length());
		Extractor.writeText(name, text);
		return text;
	}
	
	public static String extractSeedAbstractAllIndexTermsV2(String name, LanguageEnum lang, List<String> langBooks, int type) {
		Persistence p = Persistence.getInstance();
		Set<String> finalSentences = new HashSet<String>();

		for(String bookID : langBooks) {
			BookPages bookPages = WriterReader.readBookPages(bookID);
			Map<Integer, Set<String>> map = p.getPagesAndSentecesForIndex(bookID);
			//for each page
			for(Integer key : map.keySet()) {
				System.out.println("Page: " + key);
				String pageText = bookPages.getTextFromPage(key);
				ArrayList<String> pageSentences = Extractor.extractSentencesFromText(pageText);
				for(String targetText: map.get(key)) {
					if(type == 0) {
						Set<String> sentences = Extractor.extractSentenceV2(targetText, pageSentences);
						finalSentences.addAll(sentences);
						System.out.println("Part of sentence: " + targetText);
						System.out.println("Sentences: " + sentences);
					} else {
						Set<String> sentences = Extractor.extractParagraphV2(targetText, pageSentences);
						finalSentences.addAll(sentences);
						System.out.println("Part of sentence: " + targetText);
						System.out.println("Paragraphs: " + sentences);
					}
				}
			}
		}
		
		//result
		StringBuilder sb = new StringBuilder();
		for(String sentence: finalSentences) {
			sb.append(sentence);
			sb.append("\n");
		}
		String text = sb.toString();
		System.out.println("Size of seed abstract: " + text.length());
		Extractor.writeText(name, text);
		return text;
	}
	
	public static String extractIntroductionsForRandomIndexTerms(String name, LanguageEnum lang, double porcentage, List<String> ignoreList) {
		//Content Manager
		ContentManager cm = ContentManager.getInstance();
		cm.loadBookModels();
		
		//result
		StringBuilder sb = new StringBuilder();
		
		//get books
		List<String> langBooks = cm.getListOfAllBooks(lang);
		langBooks.removeAll(ignoreList);
		
		//get introduction for each index term of each book
		// source of code: class IntroductoryTextFetcher
		for(String bookID : langBooks) {
			
			//get the index terms
			List<String> indexTerms = cm.getListOfIndices(bookID);
			//random : shuffle the terms
			Collections.shuffle(indexTerms);
			Double amount = (indexTerms.size()*(porcentage / 100.0));
			
			for (int i = 0; i < amount; i++) {
				String index = indexTerms.get(i);
				
				int page = cm.getFirstOccurrenceOfIndex(bookID, index);
				//@i.alpizarchacon align page with numbers on segments + 1 
				page = page+1;
				List<Integer> segments = cm.getSegmentsOnPage(bookID, page);
			
				for (int segID : segments) {
				
					if (cm.isSegmentParagraph(bookID, segID)) {
					
						File parFile = cm.getParagraphText(bookID, segID);
						BufferedReader reader = null;
						try {
							
							reader = new BufferedReader(new FileReader(parFile));
							String line = null;
							
							while ((line = reader.readLine()) != null) {
								sb.append(line);
							}
							
						} catch (FileNotFoundException e) {
							e.printStackTrace();
							SystemLogger.getInstance().log(e.toString());
						} catch (IOException e) {
							e.printStackTrace();
							SystemLogger.getInstance().log(e.toString());
						} finally {
							
							try {
								reader.close();
							} catch (IOException e) {
								e.printStackTrace();
								SystemLogger.getInstance().log(e.toString());
							}
							
						}
						
					}
					
					sb.append(" \n");
				}
			}
		}
		String text = sb.toString();
		Extractor.writeText(name, text);
		return text;
	}
	
	public static String extractIntroductionsForIndexTerm(String name, LanguageEnum lang, List<String> ignoreList) {
		//Content Manager
		ContentManager cm = ContentManager.getInstance();
		cm.loadBookModels();
		
		//result
		StringBuilder sb = new StringBuilder();
		
		//get books
		List<String> langBooks = cm.getListOfAllBooks(lang);
		langBooks.removeAll(ignoreList);
		
		//get introduction for each index term of each book
		// source of code: class IntroductoryTextFetcher
		for(String bookID : langBooks) {
			
			
				String index = name;
				
				int page = cm.getFirstOccurrenceOfIndex(bookID, index);
				//@i.alpizarchacon align page with numbers on segments + 1 
				page = page+1;
				List<Integer> segments = cm.getSegmentsOnPage(bookID, page);
			
				for (int segID : segments) {
				
					if (cm.isSegmentParagraph(bookID, segID)) {
					
						File parFile = cm.getParagraphText(bookID, segID);
						BufferedReader reader = null;
						try {
							
							reader = new BufferedReader(new FileReader(parFile));
							String line = null;
							
							while ((line = reader.readLine()) != null) {
								sb.append(line);
							}
							
						} catch (FileNotFoundException e) {
							e.printStackTrace();
							SystemLogger.getInstance().log(e.toString());
						} catch (IOException e) {
							e.printStackTrace();
							SystemLogger.getInstance().log(e.toString());
						} finally {
							
							try {
								reader.close();
							} catch (IOException e) {
								e.printStackTrace();
								SystemLogger.getInstance().log(e.toString());
							}
							
						}
						
					}
					
					sb.append(" \n");
				}
			
		}
		String text = sb.toString();
		Extractor.writeText(name, text);
		return text;
	}
	

	
	public static ArrayList<String> extractSentencesFromText(String allText) {
		ArrayList<String> sentences = new ArrayList<String>();
		int index = 0;
		int newIndex;
		String sentence;
		allText = StringUtils.preProcessParentesis(allText);
		while (index >= 0) {
		    newIndex = Extractor.getNextValidEndOfSentance(index, allText);
		    
		    if(newIndex == -1) {
		    	sentence = allText.substring(index + 1);
		    } else {
		    	if(index == 0) {
		    		index--;
		    	}
		    	sentence = allText.substring(index + 1, newIndex + 1);
		    }
		    index = newIndex;
		    sentences.add(sentence);
		}
		
		return sentences;
	}
	
	
	
	public static void main (String[] args) throws SQLException {
		
//		Pair<String,String> r = Extractor.separateWords("Hyper Ind Top Ser (HITS)");
//		System.out.println(r.getKey());
//		System.out.println(r.getValue());
		
		//System.out.println(similarityOfString("hola", "holo"));
		Extractor.getSynonymsFromWikipedia("Bayes' theorem");
		
		//* VERSION 2
//		String Dekking_Modern_Introduction = "1540304374718";
//		String Walpole_Probability_and_Statistics = "1540304692049";
//		String Information_Retrieval = "1540367329892";
//		List<String> ignoreList = new ArrayList<String>();
//		ignoreList.add(Information_Retrieval);
//		ignoreList.add(Dekking_Modern_Introduction);
//		//ignoreList.add(Walpole_Probability_and_Statistics);
		
		//* VERSION 2
		//String name = "Dekking_Modern_Introduction_v2.glossary";
		//String name = "Walpole_Probability_and_Statistics_v2.glossary";
		//String name = "Walpole&Dekking.glossary_v2.glossary";
		//String name = "IR.glossary_v2.glossary";
		
		//String name = "Dekking_Modern_Introduction_v2.abstracts.sentence";
		//String name = "Dekking_Modern_Introduction_v2.abstracts.paragraph";
		//String name = "Walpole_Probability_and_Statistics_v2.abstracts.sentence";
		//String name = "Walpole_Probability_and_Statistics_v2.abstracts.paragraph";
		//String name = "Walpole&Dekking.glossary_v2.abstracts.sentence";
		//String name = "Walpole&Dekking.glossary_v2.abstracts.paragraph";
		//String name = "IR.glossary_v2.abstracts.sentence";
		//String name = "IR.glossary_v2.abstracts.paragraph";
		
//		List<String> bookList = new ArrayList<String>();
//		bookList.add("1560525848675");
//		//bookList.add("1560683698011");
//		
//		//* Create glossary
//		List<String> bookList = new ArrayList<String>();
//		bookList.add("1560525848675");
//		Extractor.extractGlossaryFromBooksV2("1560525848675",bookList);
//		bookList = new ArrayList<String>();
//		bookList.add("1560683698011");
//		Extractor.extractGlossaryFromBooksV2("1560683698011",bookList);
//		intersectionFromGlossariesV2 ();
//		
		
		//Set<Concept> IRGlossary = Extractor.readGlossary("information_retrieval.glossary");
		//Concept.printGlossary(IRGlossary, LanguageEnum.ENGLISH);
		
		
		//*IntersectionFromGlossariesV2
		//intersectionFromGlossariesV2();
		
		//*Extract sentences and paragraphs
		
		//String name = "Walpole_Probability_and_Statistics_v2.glossary";
		//String name = "Walpole&Dekking.glossary_v2.glossary";
		//extractSeedAbstractAllIndexTermsV2(name, LanguageEnum.ENGLISH, bookList,1);
		
		//Extractor.extractGlossaryFromBooks(name,ignoreList);
		//Set<Concept> glossary = Extractor.readGlossary(name);
		//Concept.printGlossary(glossary, LanguageEnum.ENGLISH);
	
		//Extractor.extractSeedAbstractAllIndexTerms("Walpole_Probability_and_Statistics.abstracts.sentence",LanguageEnum.ENGLISH, ignoreList, 0);
		
		//String t = "fin . oracion 6.7  casa . num ... i.";
		//System.out.println(t.charAt(33));
		//System.out.println(Extractor.isValidEndOfSentance(33,t));
		
		//String result = Extractor.readText("booksSeedAbstract_all_concepts");
		//System.out.println(result.length());
		//Extractor.extractIntroductionsForRandomIndexTerms("booksSeedAbstract_random_concepts", LanguageEnum.ENGLISH, 25, ignoreList);
		//Extractor.extractIntroductionsForRandomSegments("booksSeedAbstract_random_segments", LanguageEnum.ENGLISH, 25, ignoreList);
		//String result = Extractor.readText("booksSeedAbstract_all_terms");
		//System.out.println(result.length());
		//result = Extractor.readText("booksSeedAbstract_random_terms");
		//System.out.println(result.length());
		//result = Extractor.readText("booksSeedAbstract_random_segments");
		//System.out.println(result.length());
		
		//System.out.println(extractIntroductionsForIndexTerm("alternative hypothesis",LanguageEnum.ENGLISH, ignoreList));
	}

}
