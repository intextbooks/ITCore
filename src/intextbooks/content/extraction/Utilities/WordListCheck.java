package intextbooks.content.extraction.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import intextbooks.ontologie.LanguageEnum;

public class WordListCheck {

	private static ArrayList<String> notTableOfContents = new ArrayList<String>();
	
	private static ArrayList<String> tableOfContents = new ArrayList<String>();

	private static ArrayList<String> indexPreferred = new ArrayList<String>();
	
	private static ArrayList<String> index = new ArrayList<String>();
	
	private static ArrayList<String> see = new ArrayList<String>();
	
	private static ArrayList<String> seeAlso = new ArrayList<String>();
	
	private static ArrayList<String> specialCaseExample = new ArrayList<String>();
	
	private static ArrayList<String> specialCasesPart = new ArrayList<String>();
	private static ArrayList<String> specialCasesComplete = new ArrayList<String>();
	
	private static HashMap<LanguageEnum,String> andForList = new HashMap<LanguageEnum,String>();
	
	private static HashMap<LanguageEnum,String> cont = new HashMap<LanguageEnum,String>();
	
	private static ArrayList<String> exercises = new ArrayList<String>();
	
	private static ArrayList<String> appendix = new ArrayList<String>();
	
	private static ArrayList<String> chapter = new ArrayList<String>();
	
	private static ArrayList<String> copyRight = new ArrayList<String>();
	private static ArrayList<String> copyRightContains = new ArrayList<String>();
	

	public static void WordListCheck() {
	
		//*****TOC
		//EN
		tableOfContents.add("contents");
		tableOfContents.add("content");
		tableOfContents.add("table of content");
		tableOfContents.add("table of contents");
		//DE
		tableOfContents.add("inhaltsverzeichnis");
		//FR
		tableOfContents.add("table des matières");
		tableOfContents.add("table of contents");
		tableOfContents.add("table des matieres");
		tableOfContents.add("contenu");
		tableOfContents.add("sommaire");
		
		//ES
		tableOfContents.add("índice general");	
		tableOfContents.add("indice general");
		tableOfContents.add("contenido");
		tableOfContents.add("índice");
		tableOfContents.add("index");
		//NL
		tableOfContents.add("inhoudsopgave");
		tableOfContents.add("inhoud");
		
		//*****NOT TOC
		notTableOfContents.add("abreviada");
		notTableOfContents.add("abreviado");
		notTableOfContents.add("breve");
		notTableOfContents.add("brief");
		
		//*****Index
		//EN
		index.add("index");
		indexPreferred.add("subject index");
		indexPreferred.add("index of Subjects");
		//DE
		index.add("stichwortverzeichnis");
		index.add("sachregister");
		index.add("sachverzeichnis");
		//ES
		index.add("índice");
		index.add("índice analítico");
		index.add("indice");
		index.add("index");
		//NL
		
		//*****See
		see.add("see");
		
		//*****See
		seeAlso.add("see also");
		
		//*****TOC
		//EN
		andForList.put(LanguageEnum.ENGLISH, "and");
		//DE
		andForList.put(LanguageEnum.GERMAN, "und");
		//FR
		andForList.put(LanguageEnum.FRENCH, "et");
		//ES
		andForList.put(LanguageEnum.SPANISH, "y");
		//NL
		andForList.put(LanguageEnum.DUTCH, "en");
		
		//*****Index
		//EN
		cont.put(LanguageEnum.ENGLISH, "cont.");
		
		//Section Exercise
		exercises.add("exercises");
		exercises.add("review exercises");
		
		//Appendix
		appendix.add("appendix");
		
		//Chapter title
		chapter.add("chapter");
		
		//CopyRight
		copyRight.add("doi");
		copyRight.add("doi:");
		copyRight.add("©");
		copyRight.add("c");
		copyRight.add("publisher");
		copyRight.add("publishing");
		copyRightContains.add("doi.org/");
		
		//Example
		//specialCaseExample.add("example");
		
		//SpecialCases
		//specialCasesPart.add("data");
		//specialCasesComplete.add("summary of");
		//specialCasesComplete.add("expectation of");
		//specialCasesComplete.add("variance of");
		
	}
	
	/**
	 * 
	 * @param input
	 * @return
	 */
	
	public static boolean containsSpecialCases(String input){
		String regex;
		Pattern pattern;
		Matcher matcher;
		//preprocesing
		int index1 = input.lastIndexOf("<>");
		if(index1 != -1) {
			index1 = index1 + 2;
			if(index1 < input.length())
				input = input.substring(index1);
		}
		input = input.replaceAll("\\d","");
		input = input.replaceAll("[,;]","");
		input = input.trim();
		input = input.toLowerCase();
		//System.out.println("final: " + input);
		
		if(specialCasesPart == null || specialCasesPart.size() < 1 || specialCasesComplete == null || specialCasesComplete.size() < 1)
			WordListCheck();
		
		
		for(short i = 0 ; i < specialCasesPart .size() ; i++) {
			if(Arrays.asList(input.split(" ")).contains(specialCasesPart.get(i))) {
				return true;
			}
		}
		
		for(short i = 0 ; i < specialCasesComplete .size() ; i++) {
			regex = "\\b"+specialCasesComplete.get(i).toLowerCase()+"\\b";
			
			pattern = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
		    matcher = pattern.matcher(input);
		    if(matcher.matches() == true) {
				return true;
			}	
		}
		
		return false;
	}
	
	public static boolean containsExample(String input){
		
		if(specialCaseExample == null || specialCaseExample .size() < 1)
			WordListCheck();
				
		for(short i = 0 ; i < specialCaseExample .size() ; i++)				
			if(Arrays.asList(input.split(" ")).contains(specialCaseExample.get(i))) {
				return true;
			}			
		
		return false;
	}
	
	public static List<String> getSeeList(){
		if(see == null || see .size() < 1)
			WordListCheck();	
		
		return see;
	}
	
	public static List<String> getSeeAlsoList(){
		if(seeAlso == null || seeAlso .size() < 1)
			WordListCheck();	
		
		return seeAlso;
	}
	
	public static boolean matchesIndex(String input){
		
		if(index == null || index .size() < 1)
			WordListCheck();
				
		for(short i = 0 ; i < index .size() ; i++)				
			if(input != null && input.toLowerCase().replaceAll("[0-9]", "").matches(index.get(i).toLowerCase()))
				return true;
		
		//less restrictive
//		for(short i = 0 ; i < Index .size() ; i++)	
//			if(input != null && input.toLowerCase().contains(Index.get(i).toLowerCase()))
//				return true;
			
		return false;
	}
	
	public static boolean matchesPreferredIndex(String input){
		
		if(indexPreferred == null || indexPreferred .size() < 1)
			WordListCheck();
				
		for(short i = 0 ; i < indexPreferred .size() ; i++)				
			if(input != null && input.toLowerCase().matches(indexPreferred.get(i).toLowerCase()))
				return true;
		
		//less restrictive
		for(short i = 0 ; i < indexPreferred .size() ; i++)	
			if(input != null && input.toLowerCase().contains(indexPreferred.get(i).toLowerCase()))
				return true;
			
		return false;
	}
	
	/**
	 * 
	 * @param input
	 * @return
	 */
	public static boolean containsIndex(String input){
	
		
		if(index == null || index.size() < 1)
			WordListCheck();
		
		
			for(short i = 0 ; i < index.size() ; i++)		
				if(input.toLowerCase().contains(index.get(i).toLowerCase()))
					return true;
			
		
		return false;
	}
	
	/***
	 * 
	 * @param input
	 * @return
	 */
	public static boolean matchesTableOfContents(String input){
		
		if(tableOfContents == null || tableOfContents .size() < 1)
			WordListCheck();
		
		
		for(short i = 0 ; i < tableOfContents .size() ; i++)			
			if(input.toLowerCase().matches(tableOfContents .get(i).toLowerCase()))
				return true;
			
		
		return false;
	}
	
	/**
	 * 
	 * @param input
	 * @return
	 */
	public static boolean containstableOfContents(String input){

		if(tableOfContents == null || tableOfContents.size() < 1)
			WordListCheck();
		
			for(short i = 0 ; i < tableOfContents.size() ; i++) {
				if(input.toLowerCase().equals(tableOfContents.get(i).toLowerCase()))
					return true;
			}
			
		return false;		
	}
	
	public static boolean omitTableOfContents(String input){

		if(notTableOfContents == null || notTableOfContents.size() < 1)
			WordListCheck();
		
		
			for(short i = 0 ; i < notTableOfContents.size() ; i++)
				if(input.toLowerCase().contains(notTableOfContents.get(i).toLowerCase()))
					return true;
			
		return false;		
	}
	
	public static String getAndListWord(LanguageEnum lang) {
		return andForList.get(lang);
	}
	
	public static String getContWord(LanguageEnum lang) {
		return cont.get(lang);
	}
	
	public static boolean isExerciseSection(String input) {
		if(exercises == null || exercises .size() < 1)
			WordListCheck();
		
		if(exercises.contains(input.toLowerCase().replaceAll("[^a-zA-Z\\s]", ""))) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean isAppendixSection(String input) {
		if(appendix == null || appendix .size() < 1)
			WordListCheck();
		
		for(String word: appendix) {
			if(input.contains(word))
				return true;
		}
		
		return false;
	}
	
	public static boolean containsChapterTitle(String input) {
		if(chapter == null || chapter .size() < 1)
			WordListCheck();
		
		for(String chapterWord: chapter) {
			if(input.toLowerCase().contains(chapterWord)) {
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean isCopyrightWord(String input) {
		if(copyRight == null || copyRight .size() < 1)
			WordListCheck();
		
		for(String copyrightWord: copyRight) {
			if(input.toLowerCase().equals(copyrightWord)) {
				return true;
			}
		}
		for(String copyrightWord: copyRightContains) {
			if(input.toLowerCase().contains(copyrightWord)) {
				return true;
			}
		}
		
		return false;
	}
}
