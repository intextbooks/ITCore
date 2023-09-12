package intextbooks.tools.utility;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PrimitiveIterator.OfInt;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.jboss.dna.common.text.Inflector;

import com.hp.hpl.jena.util.URIref;

import intextbooks.SystemLogger;
import intextbooks.content.extraction.Utilities.Stemming;
import intextbooks.ontologie.LanguageEnum;

public class StringUtils {
	
	/**
	 * Counts the occurrences of a substring in a string
	 * 
	 * @param str the whole string
	 * @param target the substring
	 * @return number of occurrences
	 */
	
	public static int countOccurrences(String str, String target) {
		
		int count = 0;
		int lastIndex = 0;
		
		while (lastIndex != -1) {
			
			lastIndex = str.indexOf(target, lastIndex);
			
			if (lastIndex != -1) {
				count++;
				lastIndex += target.length();
			}
			
		}
		
		return count;
		
	}

	/**
	 * Splits a string with a substring enclosed in brackets
	 *
	 * @param str
	 *            a string with a substring enclosed in brackets
	 * @return a pair where the left element is the original string without
	 *         brackets and the second element is the string without the
	 *         enclosed substring
	 */

	public static Pair<String, String> splitBrackets(String str) {

		try {
			int bracketL = str.indexOf('(');
			int bracketR = str.indexOf(')');
			String bracketStr = str.substring(bracketL, bracketR + 1);

			return Pair.of(str.replace("(", "").replace(")", ""), str.replace(bracketStr, ""));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("S: " + str);
			return null;
		}
		

	}

	/**
	 * Splits strings containing colons
	 *
	 * @param str
	 *            a string with colon
	 * @return a pair where the left element is the substring left of the colon
	 *         and the right element is the substring right of the colon
	 */

	public static Pair<String, String> splitColons(String str) {

		String[] splitted = str.split("\\:");
		return Pair.of(splitted[0].trim(), splitted[1].trim());

	}

	/**
	 * Normalizes whitespace
	 *
	 * @param str
	 * @return normalized string
	 */

	public static String normalizeWhitespace(String str) {

		String res = str.replaceAll("\\s+", " ");
		return res.trim();

	}

	/**
	 * Replaces semicolons inside of terms with commas, deletes punctuation at
	 * the beginning and end of terms
	 *
	 * @param str
	 * @return normalized string
	 */

	public static String normalizePunctuation(String str) {

		int lastInx = -1;
		int lInx = -1;

		while ((lInx = str.indexOf('(', lastInx + 1)) >= 0) {

			int rInx = str.indexOf(')', lInx);

			// sometimes the isi terms lack closing brackets...
			if (rInx < 0) {

				rInx = str.length() - 1;
				str += ")";

			}

			String bracketExp = str.substring(lInx, rInx + 1);
			bracketExp = bracketExp.replaceAll("\\;", "\\,");

			String leftStr = str.substring(0, lInx);

			String rightStr;

			if (rInx < str.length() - 1) {
				rightStr = str.substring(rInx + 1, str.length());
			} else {
				rightStr = "";
			}

			str = leftStr + bracketExp + rightStr;

			lastInx = rInx;

		}

		if (str.substring(0, 1).matches("[\\,\\;]")) {

			str = str.substring(1);

		}

		if (str.substring(str.length() - 1, str.length()).matches("[\\,\\;]")) {

			str = str.substring(0, str.length() - 1);

		}

		return str;

	}

	/**
	 * DBpedia resource links begin with an uppercase letter and have spaces
	 * replaced by underscores. Apostrophes need special escaping.
	 *
	 * @param str
	 * @return a string suited for a DBpedia query
	 */
	
	public static String normalizeDBpediaQuery(String str) {
		
		String mod = StringUtils.firstCharToUpperCase(str);
		mod = mod.replace(" ", "_");
		
		//mod = URIref.encode(mod);		
		return mod;
		
	}
	
	public static String normalizeReQuery(String str) {
		
		return str.replace("_", " ");
		
	}
	
	public static String normalizeResourceQuery(String str) {
		
		String mod = StringUtils.whiteSpaceToUnderScore(str);
		return StringUtils.normalizeDBpediaQuery(mod);
		
	}

	public static String whiteSpaceToUnderScore(String str) {
		return str.replace(' ', '_');
	}

	public static String underScoreToWhiteSpace(String str) {
		return str.replace('_', ' ');
	}

	public static String firstCharToLowerCase(String str) {

		String firstChar = str.substring(0, 1);
		return firstChar.toLowerCase() + str.substring(1);

	}

	public static String firstCharToUpperCase(String str) {

		if(str.length() == 1) {
			return str.toUpperCase();
		} else if (str.length() == 0) {
			return str;
		}
		
		String firstChar = str.substring(0, 1);
		return firstChar.toUpperCase() + str.substring(1);

	}

	/**
	 * Returns the ending of the URL, i.e. the part after the last slash
	 *
	 * @param url
	 * @return the last part of the URL
	 */

	public static String truncateURL(String url) {

		int slashInx = url.lastIndexOf("/");
		return url.substring(slashInx + 1);

	}
	
	/**
	 * Cleans some known noise from DBpedia resources
	 * 
	 * @param str
	 * @return De-noised DBpedia resource name
	 */

	public static String whiteListBrackets(String str) {

		int bracketL = str.indexOf('(');
		int bracketR = str.indexOf(')');
		
		String res;
		try {
			String bracketStr = str.substring(bracketL, bracketR + 1);

			res = str;

			switch (bracketStr) {

				case ("(Mathematik)"):
					res = str.replace(bracketStr, "");

				case ("(mathématiques)"):
					res = str.replace(bracketStr, "");

				case ("(mathematics)"):
					res = str.replace(bracketStr, "");

				case ("(Stochastik)"):
					res = str.replace(bracketStr, "");

				case ("(stochastique)"):
					res = str.replace(bracketStr, "");

				case ("(stochastics)"):
					res = str.replace(bracketStr, "");

				case ("(Statistik)"):
					res = str.replace(bracketStr, "");

				case ("(statistiques)"):
					res = str.replace(bracketStr, "");

				case ("(statistics)"):
					res = str.replace(bracketStr, "");

				case ("(probabilités)"):
					res = str.replace(bracketStr, "");

			}
		} catch (StringIndexOutOfBoundsException e) {
			e.printStackTrace();
			return str;
		}

		return res.trim();

	}
	
	public static String encloseWithDiamonds(String str) {
		return "<" + str + ">";
	}
	
	public static String getDomainRegex(LanguageEnum lang) {
		
		switch (lang) {
			case ENGLISH:
				return "/db";
			case GERMAN:
				return "de\\\\.";
			case FRENCH:
				return "fr\\\\.";
			case SPANISH:
				return "es\\\\.";
			case DUTCH:
				return "nl\\\\.";
			case RUSSIAN:
				return "ru\\\\.";
			default:
				return "/db";
		}
		
	}
	
	public static String stripRedundantURLs(String urls) {
		
		int secInx = urls.indexOf("http://", 1);
		
		if (secInx == -1) {
			return urls;
		} else {
			return urls.substring(0, secInx);
		}
		
	}
	
	public static String stripCategoryString(String uri) {
		
		return truncateURL(uri).replace("Category:", "").replace("Kategorie:", "").replace("Catégorie:", "");
		
	}
	
	public static String removeLangTagFromAbstract(String originalAbstract) {
		int size = originalAbstract.length();
   	 	String sub = originalAbstract.substring(size-3);
		if (sub.matches("@..")) {
			originalAbstract = originalAbstract.substring(0, size-3);
		}
		return originalAbstract;
	}
	
	public static String preProcessParentesis(String text){
		String newText = "";
		IntStream is = text.chars();
		int[] t = is.toArray();
		char last = ' ';
		for(int charVal: t) {
			char val = (char) charVal;
			if((val == '(' && last != ' ') || (last == ')' && val != ' ') ) {
					newText += " ";
			} else if((val == '[' && last != ' ') || (last == ']' && val != ' ') ) {
				newText += " ";
			}  
			
			
			newText += val;
			last = val;
		}
		return newText;
	}
	
	public static ArrayList<String> getPermutations(List<String> parts) {
	    ArrayList<String> results = new ArrayList<String>();

	    // the base case
	    if (parts.size() == 1) {
	        results.add(parts.get(0));
	        return results;
	    }

	    for (int i = 0; i < parts.size(); i++) {
	        String first = parts.get(i);
	        List<String> remains = new ArrayList<String>();
	        for(int j = 0; j < parts.size(); j++) {
	        	if(j != i) {
	        		remains.add(parts.get(j));
	        	}
	        }

	        ArrayList<String> innerPermutations = getPermutations(remains);

	        for (int j = 0; j < innerPermutations.size(); j++)
	            results.add(first + " " + innerPermutations.get(j));
	    }
	    return results;
	}
	
	public static String fixFI(String text) {
		return text.replaceAll("ﬁ", "fi");
	}
	
	public static String getNormalizedKey(String key) {
		return key.replaceAll(" <> ", " ").toLowerCase().trim();
	}
	
	public static String stemText(String text, LanguageEnum lang) {
		String parts[] = text.split("\\s+");
		String stemedText = "";
		for(String part: parts) {
			stemedText += Stemming.stemText(lang, part) + " ";
		}
		return stemedText.trim();
	}
	
	public static Pair<String, String> separateString(String text){
		String main = "";
		String secundary = "";
		IntStream intStream = text.chars();
		OfInt iterator = intStream.iterator();
		boolean inWord = false;
		while(iterator.hasNext()) {
			char c = (char) iterator.nextInt();
			if(c == '(') {
				inWord = true;
			} else if (c == ')') {
				inWord = false;
				secundary += " ";
			} else {
				if(inWord) {
					secundary += c;
				} else {
					main += c;
				}
			}
		}
		main = main.replaceAll("\\s+", " ").trim();
		secundary = secundary.trim();
		if(secundary.equals(""))
			secundary = null;
		
		//try second options: split a word with /
		if(secundary == null) {
			if (text.contains("/")) {
				main = "";
				secundary = "";
				String[] parts = text.split("\\s");
				for(String part: parts) {
					if(part.contains("/")) {
						String words[] = part.split("/");
						if(words.length == 2) {
							main += words[0] + " ";
							secundary += words[1] + " ";
						} else {
							main += part + " ";
							secundary += part + " ";
						}
					} else {
						main += part + " ";
						secundary += part + " ";
					}
				}
				main = main.trim();
				secundary = secundary.trim();
			}
		}
		
		return Pair.of(main, secundary);
	}
	
	public static String normalizeStringForDBpedia(String text) {
		text = Normalizer.normalize(text, Normalizer.Form.NFKD);
		text = text.replaceAll("\\p{M}", "");
		text = text.replaceAll("\\p{Pi}", "'");
		text = text.replaceAll("\\p{Pf}", "'");
		
		return text;
	}
	
	public static String removePunctuation(String text) {
		text = text.replaceAll("\\p{P}", "");
		text = text.replaceAll("\\p{Po}", "");
		
		return text;
	}
	
	synchronized public static String singularize(String text) {
		Inflector inflector = Inflector.getInstance();
		return inflector.singularize(text);
	}
	
	public static Set<String> getDifferentVersionsOfTerm(String term){
		Set<String> newTerms = new HashSet<String>();
		String singular = StringUtils.singularize(term);
		newTerms.add(singular.trim());
		newTerms.add(singular.toLowerCase().trim());
		newTerms.add(singular.toUpperCase().trim());
		term = StringUtils.normalizeStringForDBpedia(term.trim());
		newTerms.add(term.trim());
		newTerms.add(term.toLowerCase().trim());
		newTerms.add(term.toUpperCase().trim());
		newTerms.add(StringUtils.singularize(term));
		term = StringUtils.removePunctuation(term.trim());
		newTerms.add(term.trim());
		newTerms.add(term.toLowerCase().trim());
		newTerms.add(term.toUpperCase().trim());
		newTerms.add(StringUtils.singularize(term));
		
		return newTerms;
	}
	
	public static void main (String[] args) {
		//StringUtils.singular("Analysis of variance (ANOVA) <> definition of");
		//System.out.println(StringUtils.separateString("point estimate/stimator"));
		//System.out.println(StringUtils.separateString("Probability density function (pdf) is c (casa) 45"));
//		List<String> l = new ArrayList<String>();
//		l.add("casa");
//		l.add("papel");
//		System.out.println(StringUtils.getPermutations(l));
	}
	

}
