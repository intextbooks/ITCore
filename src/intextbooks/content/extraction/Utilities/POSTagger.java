package intextbooks.content.extraction.Utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.text.WordUtils;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import intextbooks.Configuration;
import intextbooks.SystemLogger;
import intextbooks.ontologie.LanguageEnum;

public class POSTagger {
	
	//@i.alizarchacon: changed to on-demand
	private static MaxentTagger englishTagger;
	private static MaxentTagger germanTagger;
	private static MaxentTagger frenchTagger;
	private static MaxentTagger spanishTagger;
	private static MaxentTagger dutchTagger;
	
	private static Set<String> englishNounTags = initEnglishNounTags();
	private static Set<String> germanNounTags = initGermanNounTags();
	private static Set<String> frenchNounTags = initFrenchNounTags();
	private static Set<String> spanishNounTags = initFrenchNounTags();
	private static Set<String> dutchNounTags = initDutchNounTags();
	
	
	public static boolean isNoun (String term, String text, LanguageEnum lang) {
		
		//0. Common logic
		Map<String, List<String>> invertedMap;
		String stemmedTerm = Stemming.stemText(lang, term);
		stemmedTerm = WordUtils.uncapitalize(stemmedTerm);
		Set<String> nounTags = null;
		
		
		//1. Tag text (specific for language, two taggers available (ApacheOpeNLP & Standfor POSTagger)
		if(lang.equals(LanguageEnum.DUTCH)) {
			POSTaggerME tagger;
			try {
				tagger = new POSTaggerME(new POSModel(new File(System.getProperty("user.dir")+"/"+Configuration.getInstance().getDutchPosModel())));
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			invertedMap = isNounOpenNLP(term, text, lang, tagger);
			nounTags = dutchNounTags;
		} else {
			MaxentTagger tagger = null;
			switch (lang) {
			case ENGLISH:
				if (englishTagger == null) {
					englishTagger = new MaxentTagger(System.getProperty("user.dir")+"/"+Configuration.getInstance().getEnglishPosModel());
				}
				tagger = englishTagger;
				nounTags = englishNounTags;
				break;
			case GERMAN:
				if (germanTagger == null) {
					germanTagger = new MaxentTagger(System.getProperty("user.dir")+"/"+Configuration.getInstance().getGermanPosModel());
				}
				tagger = germanTagger;
				nounTags = germanNounTags;
				break;
			case FRENCH:
				if (frenchTagger == null) {
					frenchTagger = new MaxentTagger(System.getProperty("user.dir")+"/"+Configuration.getInstance().getFrenchPosModel());
				}
				tagger = frenchTagger;
				nounTags = frenchNounTags;
				break;
			case SPANISH:
				if (spanishTagger == null) {
					spanishTagger = new MaxentTagger(System.getProperty("user.dir")+"/"+Configuration.getInstance().getSpanishPosModel());
				}
				tagger = spanishTagger;
				nounTags = spanishNounTags;
				break;
			case DUTCH:
				if (dutchTagger == null) {
					dutchTagger = new MaxentTagger(System.getProperty("user.dir")+"/"+Configuration.getInstance().getDutchPosModel());
				}
				tagger = dutchTagger;
				nounTags = dutchNounTags;
				break;
			default:
				tagger = englishTagger;
			}
			invertedMap = isNounStanfordNLP(stemmedTerm, text, lang, tagger);
		}
		
		
		//2. Search for word in tagged text
		List<String> termTags = invertedMap.get(stemmedTerm);
				
		if (termTags == null)			
			return false;
		
		else
		
			// word category is unique for given term
			if (termTags.size() == 1) {
//				/*TESTING*/
//				System.out.println("YES, word found with only one tag, word: "  +stemmedTerm + " tag: " + termTags.get(0));
//				/*TESTING*/
				String tag = termTags.get(0);
				if (nounTags.contains(tag))
					return true;
				else
					return false;
				
			} else { // term belongs to different word categories in given text
//				/*TESTING*/
//				System.out.println("NO, word found with more than one tag, word: "  +stemmedTerm + " tags: " + termTags);
//				/*TESTING*/
				//TODO: handle ambiguous word categories, i.e. check number of term's occurrence during highlighting
				return false;
				
			}
	
	}
	
	public static Map<String, List<String>> isNounOpenNLP(String stemmedTerm, String text, LanguageEnum lang, POSTaggerME tagger) {
		
		
		String[] words = text.split("[\\p{Punct}\\s]+");
		String tags[] = tagger.tag(words);
		
//		/*TESTING*/
//		for(int i = 0; i < tags.length; i++) {
//			System.out.println("________");
//			System.out.println("Word: " + words[i]);
//			System.out.println("Tag: " + tags[i]);
//		}
//		/*TESTING*/
		
		return invertTaggedText(words,tags, lang);
	}
	
	
	public static Map<String, List<String>> isNounStanfordNLP(String term, String text, LanguageEnum lang, MaxentTagger tagger) {

		List<HasWord> tokens = tokenize(text);
		List<TaggedWord> taggedText = tagger.tagSentence(tokens);
		return invertTaggedText(taggedText, lang);
		
	}
	
	//TODO: change back to private
	private static List<HasWord> tokenize(String text) {
		
		List<HasWord> tokens = new ArrayList<HasWord>();		
		String[] words = text.split("[\\p{Punct}\\s]+");
		
		for (String w : words) {			
			Word token = new Word(w);
			tokens.add(token);
		}
		
		return tokens;
		
	}
	
	private static Map<String, List<String>> invertTaggedText(List<TaggedWord> taggedText, LanguageEnum lang) {
		
		Map<String, List<String>> invertedMap = new HashMap<String, List<String>>();
		
		for (TaggedWord pair : taggedText) {
			
			String word = pair.word();
			String tag = pair.tag();
			String stemmedWord = Stemming.stemText(lang, word);
			stemmedWord = WordUtils.uncapitalize(stemmedWord);
			
			if (!invertedMap.containsKey(stemmedWord)) {
				
				List<String> occurrences = new ArrayList<String>();
				occurrences.add(tag);
				invertedMap.put(stemmedWord, occurrences);
				
			} else {
				
				List<String> wordTags = invertedMap.get(stemmedWord);
				
				if (!wordTags.contains(tag))
					wordTags.add(tag);
				
			}
			
		}
		
		return invertedMap;
	}
	
private static Map<String, List<String>> invertTaggedText(String[] words, String[] tags, LanguageEnum lang) {
		
		Map<String, List<String>> invertedMap = new HashMap<String, List<String>>();
		
		for (int i = 0; i < words.length; i++) {
			
			String word = words[i];
			String tag = tags[i];
			String stemmedWord = Stemming.stemText(lang, word);
			stemmedWord = WordUtils.uncapitalize(stemmedWord);
			
			if (!invertedMap.containsKey(stemmedWord)) {
				
				List<String> occurrences = new ArrayList<String>();
				occurrences.add(tag);
				invertedMap.put(stemmedWord, occurrences);
				
			} else {
				
				List<String> wordTags = invertedMap.get(stemmedWord);
				
				if (!wordTags.contains(tag))
					wordTags.add(tag);
				
			}
			
		}
		
		return invertedMap;
	}
	
	private static Set<String> initEnglishNounTags() {
		
		Set<String> tags = new HashSet<String>();
		tags.add("NN");
		tags.add("NNS");
		tags.add("NNP");
		tags.add("NNPS");
		
		return tags;
		
	}
	
	private static Set<String> initGermanNounTags() {
		
		Set<String> tags = new HashSet<String>();
		tags.add("NN");
		tags.add("NE");
		
		return tags;
		
	}
	
	//source: http://www.llf.cnrs.fr/Gens/Abeille/French-Treebank-fr.php
	private static Set<String> initFrenchNounTags() {
		
		Set<String> tags = new HashSet<String>();
		tags.add("NC");
		tags.add("NP");
		
		return tags;
		
	}
	
	//source: https://nlp.stanford.edu/software/spanish-faq.shtml#corpus
	private static Set<String> initSpanishNounTags() {
		
		Set<String> tags = new HashSet<String>();
		tags.add("nc00000");
		tags.add("nc0n000");
		tags.add("nc0p000");
		tags.add("nc0s000");
		tags.add("np00000");
		
		return tags;
		
	}
	
	//source: ApacheOpenNLP
	private static Set<String> initDutchNounTags() {
		
		Set<String> tags = new HashSet<String>();
		tags.add("N");
		
		return tags;
		
	}

}
