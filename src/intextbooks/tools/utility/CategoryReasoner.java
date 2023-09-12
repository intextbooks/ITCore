package intextbooks.tools.utility;

import org.tartarus.snowball.SnowballProgram;
import org.tartarus.snowball.ext.FrenchStemmer;
import org.tartarus.snowball.ext.German2Stemmer;
import org.tartarus.snowball.ext.PorterStemmer;

import intextbooks.ontologie.LanguageEnum;

public class CategoryReasoner {
	
	private static PorterStemmer enStemmer = new PorterStemmer();
	private static German2Stemmer deStemmer = new German2Stemmer();
	private static FrenchStemmer frStemmer = new FrenchStemmer();
	
	public static boolean determineIsARelation(LanguageEnum lang, String concept, String subject) {
		
		String[] conceptTokens = concept.toLowerCase().split(" ");
		String[] subjectTokens = subject.toLowerCase().split(" ");
		
		for (String conceptToken : conceptTokens) {
			for (String subjectToken : subjectTokens) {
				if (matchStems(lang, conceptToken, subjectToken)) {
					return true;
				}
			}
		}
		
		return false;
		
	}
	
	private static boolean matchStems(LanguageEnum lang, String concept, String subject) {
		
		SnowballProgram stemmer;
		switch (lang) {
			case ENGLISH:
				stemmer = enStemmer;
				break;
			case GERMAN:
				stemmer = deStemmer;
				break;
			case FRENCH:
				stemmer = frStemmer;
				break;
			default:
				stemmer = enStemmer;
		}
		
		stemmer.setCurrent(concept);
		stemmer.stem();
		String conceptStem = stemmer.getCurrent();
		
		stemmer.setCurrent(subject);
		stemmer.stem();
		String subjectStem = stemmer.getCurrent();
		
		if (conceptStem.equals(subjectStem))
			return true;
		else
			return false;
		
	}

}
