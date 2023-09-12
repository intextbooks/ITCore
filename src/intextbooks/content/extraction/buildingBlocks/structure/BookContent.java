package intextbooks.content.extraction.buildingBlocks.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import intextbooks.SystemLogger;
import intextbooks.content.enrichment.books.Extractor;
import intextbooks.content.extraction.Utilities.WordListCheck;
import intextbooks.content.extraction.structure.NounExtractor;
import intextbooks.content.models.structure.Segment;
import intextbooks.persistence.Persistence;

public class BookContent {
	private static final String splitRegex = "[-_–\\s]";
	
	String allContent;
	Map<Integer, String> contentBySegment;
	Map<Integer, Map<Integer,String>> contentBySegmentAndPage;
	
	public BookContent() {
		contentBySegment = new HashMap<Integer, String>();
		contentBySegmentAndPage = new HashMap<Integer, Map<Integer,String>>();
	}
	
	public String getAllContent() {
		return allContent;
	}
	
	public void setAllContent(String allContent) {
		this.allContent = allContent;
	}
	
	public void addContentToSegment(Integer segment, String content) {
		contentBySegment.put(segment, content);
	}
	
	public void addContentToSegmentPage(Integer segment, Integer page, String content) {
		Map<Integer,String> pages = contentBySegmentAndPage.get(segment);
		if(pages == null) {
			pages = new HashMap<Integer,String>();
		}
		pages.put(page, content);
		contentBySegmentAndPage.put(segment, pages);
	}
	
	public void addContentToSegmentPage(Integer segment, Map<Integer,String> pages, String title) {
		int minPage = pages.keySet().stream().mapToInt(v -> v).min().orElse(-1);
		if(minPage != -1) {
			String newContent = title + "\n";
			newContent += pages.get(minPage);
			pages.put(minPage, newContent); 
		}
		contentBySegmentAndPage.put(segment, pages);
	}
	
	public String getContentOfSegment(Integer segment) {
		return contentBySegment.get(segment);
	}
	
	public String getContentOfSegmentPage(Integer segment, Integer page) {
		Map<Integer,String> pages = contentBySegmentAndPage.get(segment);
		if(pages != null) {
			return pages.get(page);
		}
		return null;
	}
	
	public String getContentOfPage(Integer page) {
		String result = "";
		for(Entry<Integer, Map<Integer,String>> entry: contentBySegmentAndPage.entrySet()) {
			String pageString = entry.getValue().get(page);
			if(pageString != null) {
				result += pageString + " ";
			}
		}
		return result;
	}
	
	public void store(String bookID) {
		for(Entry<Integer, Map<Integer,String>> entry : contentBySegmentAndPage.entrySet()) {
			for(Entry<Integer,String> pages: entry.getValue().entrySet()) {
				Persistence.getInstance().storeExtractonTempInfo(bookID, entry.getKey() + " " + pages.getKey(), pages.getValue());
			}
			
		}
	}
	
	public List<Integer> findSegmentsOnPage(int pageIndex){
		List<Integer> list = new ArrayList<Integer>();
		for(Entry<Integer, Map<Integer,String>> entry: contentBySegmentAndPage.entrySet()) {
			if(entry.getValue().keySet().contains(pageIndex)) {
				list.add(entry.getKey());
			}
		}
		
		return list;
	}
	
	private IndexTermMatch getMatch(NounExtractor extractor, Integer segment, ArrayList<String> candidates, ArrayList<String> sentences, String text, boolean fullLabel, int priority, boolean testPROPN) {
		IndexTermMatch match = new IndexTermMatch();
		match.setSegmentID(segment);
		
//		for(String candidate: candidates) {
//			if(text.toLowerCase().contains(candidate.toLowerCase())) {
//				match.addBookString(candidate);
//				match.addReadingOrder(candidate);
//			}
//		}
		//System.out.println("C: " + candidates);
		//System.out.println("S: " + sentences);
		JSONArray array = extractor.getJSON(candidates, sentences, "en", testPROPN);
		//SystemLogger.getInstance().debug("# results : " + array.size());
		if(array.size() > 0 ) {
			for(Object sol: array) {
				JSONObject obj = (JSONObject) sol;
				//process reading order
				String correctKey = (String) obj.get("correct");
				match.addReadingOrder(correctKey);
				//process proper name
				Boolean proper_name = (Boolean) obj.get("proper_name");
				if(proper_name)
					match.addPOS("PROPN");
				//SystemLogger.getInstance().debug("proper_strings: " + proper_name);
				//process noun_string
				JSONArray noun_strings = (JSONArray) obj.get("noun_string");
				for(Object str: noun_strings) {
					if(proper_name) {
						String[] names = ((String)str).split(" ");
						for(String name: names) {
							match.addNounString(name);
						}
					} else {
						match.addNounString((String)str);
					}
					//SystemLogger.getInstance().debug("noun: " + (String)str);
				}
				//process book_string
				JSONArray book_strings = (JSONArray) obj.get("book_string");
				for(Object str: book_strings) {
					match.addBookString((String)str);
					//SystemLogger.getInstance().debug("book string: " + (String)str);
				}
				//process book_string
				//SystemLogger.getInstance().debug("correct key: " + correctKey);
				//fullLabel
				match.setFull(fullLabel);
				//SystemLogger.getInstance().debug("full? : " + fullLabel);
				//priority
				match.setPriority(priority);
			}
			return match;
		} else {
			return null;
		}	
	}
	
	public IndexTermMatch segmentContainsTerm(Integer segment, Integer page, IndexElement indexElement, NounExtractor extractor) {
		try {
			String text = contentBySegmentAndPage.get(segment).get(page);
			ArrayList<String> sentences = Extractor.extractSentencesFromText(text.replace("\n", " "));
			ArrayList<String> permutations = indexElement.getLabelPermutations();
			
			//1 try full label
			boolean testPROPN = indexElement.getParts().size() == 1 ? true : false;
//			SystemLogger.getInstance().debug("PROPN: " + testPROPN);
//			SystemLogger.getInstance().debug("SENTENCES: " + sentences);
			IndexTermMatch newMatch = getMatch(extractor, segment, permutations, sentences, text, true, 1, testPROPN);
			if(newMatch != null) {
				SystemLogger.getInstance().debug("result FULL");
				return newMatch;
			} else {
				SystemLogger.getInstance().debug("no result FULL");
				
				//4 check parts
				String parts[] = indexElement.getNormalizedKey().split(BookContent.splitRegex);
				SystemLogger.getInstance().debug("PARTS: " + Arrays.toString(parts));
				ArrayList<String> newPermutations = new ArrayList(Arrays.asList(parts));
				newMatch = getMatch(extractor, segment, newPermutations, sentences, text, false, 4, false);
				if(newMatch != null) {
					SystemLogger.getInstance().debug("result PARTS");
					return newMatch;
				} else {
					SystemLogger.getInstance().debug("no result PARTS");
					return null;
				}
				
			}
			//return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public IndexTermMatch segmentContainsTermORIGINAL(Integer segment, Integer page, IndexElement indexElement, NounExtractor extractor) {
		try {
			IndexTermMatch match = new IndexTermMatch();
			match.setSegmentID(segment);
			String text = contentBySegmentAndPage.get(segment).get(page);
			ArrayList<String> permutations = indexElement.getLabelPermutations();
			//try permutations
			for(String permutation: permutations) {
				if(text.toLowerCase().contains(permutation.toLowerCase()))
					match.addBookString(permutation);
					match.setSegmentID(segment);
			}
			
			ArrayList<String> sentences = Extractor.extractSentencesFromText(text.toLowerCase().replace("\n", " "));
			
			//try term recognition
			try {
				boolean fullLabel = false;
				SystemLogger.getInstance().debug("permutationS: " + permutations);
//				SystemLogger.getInstance().debug("sentences: ");
//				for(String s: sentences) {
//					System.out.println("# " + s);
//				}
				JSONArray array = extractor.getJSON(permutations, sentences, "en");
				if(array.size() > 0 ) {
					fullLabel = true;
				}

				if(array.size() == 0 && permutations.size() > 1) {
					array = extractor.getJSON(indexElement.getLastPartAsArrayList(), sentences, "en");
				}

				//there is a result
				if(array.size() != 0) {
					//process results
					for(Object sol: array) {
						JSONObject obj = (JSONObject) sol;
						//process reading order
						String correctKey = (String) obj.get("correct");
						match.addReadingOrder(correctKey);
						//process proper name
						Boolean proper_name = (Boolean) obj.get("proper_name");
						if(proper_name)
							match.addPOS("PROPN");
						SystemLogger.getInstance().debug("proper_strings: " + proper_name);
						//process noun_string
						JSONArray noun_strings = (JSONArray) obj.get("noun_string");
						for(Object str: noun_strings) {
							if(proper_name) {
								String[] names = ((String)str).split(" ");
								for(String name: names) {
									match.addNounString(name);
								}
							} else {
								match.addNounString((String)str);
							}
							SystemLogger.getInstance().debug("noun: " + (String)str);
						}
						//process book_string
						JSONArray book_strings = (JSONArray) obj.get("book_string");
						for(Object str: book_strings) {
							match.addBookString((String)str);
							SystemLogger.getInstance().debug("book string: " + (String)str);
						}
						//process book_string
						SystemLogger.getInstance().debug("correct key: " + correctKey);
						SystemLogger.getInstance().debug("full? : " + fullLabel);
						//priority
						if(fullLabel) {
							match.setFull(true);
							match.setPriority(1);
						}
						else
							match.setPriority(2);
					}
					return match;
				} 
			} catch (Exception e) {
				e.printStackTrace();;
			}
			
			//try full for example items
			ArrayList<String> permutationsFixed = new ArrayList<String>();
			boolean arePermutationsFixed = false;
			for(String permutation: permutations) {
				String newPermutation = null;
				if(WordListCheck.containsExample(permutation)) {
					newPermutation = WordListCheck.removeExample(permutation);
					if(newPermutation != null) {
						arePermutationsFixed = true;
						permutationsFixed.add(newPermutation);
					}
				} 
				if(arePermutationsFixed) {
					boolean testPROPN = indexElement.getParts().size() == 1 ? true : false;
					SystemLogger.getInstance().debug("EXAMPLE CASE, propN: " + testPROPN);
					IndexTermMatch newMatch = getMatch(extractor, segment, permutationsFixed, sentences, text, true, 1, testPROPN);
					if(newMatch != null) {
						return newMatch;
					} else {
						SystemLogger.getInstance().debug("no result");
					}
				}
			}
			
			//try just last part
			String key  = indexElement.getNormalizedKey();
			String[] parts = key.split(" ");
			ArrayList<String> list = new ArrayList(Arrays.asList(parts));
			JSONArray array = extractor.getJSON(list, sentences, "en");
			if(array.size() > 0 ) {
				for(Object sol: array) {
					JSONObject obj = (JSONObject) sol;
					//process reading order
					String correctKey = (String) obj.get("correct");
					match.addReadingOrder(correctKey);
					//process proper name
					Boolean proper_name = (Boolean) obj.get("proper_name");
					if(proper_name)
						match.addPOS("PROPN");
					SystemLogger.getInstance().debug("proper_strings: " + proper_name);
					//process noun_string
					JSONArray noun_strings = (JSONArray) obj.get("noun_string");
					for(Object str: noun_strings) {
						if(proper_name) {
							String[] names = ((String)str).split(" ");
							for(String name: names) {
								match.addNounString(name);
							}
						} else {
							match.addNounString((String)str);
						}
						SystemLogger.getInstance().debug("noun: " + (String)str);
					}
					//process book_string
					JSONArray book_strings = (JSONArray) obj.get("book_string");
					for(Object str: book_strings) {
						match.addBookString((String)str);
						SystemLogger.getInstance().debug("book string: " + (String)str);
					}
					//process book_string
					SystemLogger.getInstance().debug("correct key: " + correctKey);
					SystemLogger.getInstance().debug("full? : " + false);
					//priority
					match.setPriority(3);
				}
				return match;
			}
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}
}


