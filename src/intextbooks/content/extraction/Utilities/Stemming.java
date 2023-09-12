package intextbooks.content.extraction.Utilities;

import org.tartarus.snowball.ext.EnglishStemmer;
import org.tartarus.snowball.ext.FrenchStemmer;
import org.tartarus.snowball.ext.GermanStemmer;
import org.tartarus.snowball.ext.SpanishStemmer;

import intextbooks.ontologie.LanguageEnum;

import org.tartarus.snowball.ext.DutchStemmer;

public class Stemming {

	private static	FrenchStemmer  stemmerFr = new FrenchStemmer();
	private static  EnglishStemmer stemmerEn = new EnglishStemmer();
	private static  GermanStemmer  stemmerGr = new GermanStemmer();
	private static  SpanishStemmer  stemmerEs = new SpanishStemmer();
	private static  DutchStemmer  stemmerNl = new DutchStemmer();


	public static String stemText(LanguageEnum lang, String text){

		String result = "";
		
		if(lang.equals(LanguageEnum.ENGLISH)){				
			stemmerEn.setCurrent(text);
			stemmerEn.stem();
			result = stemmerEn.getCurrent();
		}
		else if(lang.equals(LanguageEnum.GERMAN)){				
			stemmerGr.setCurrent(text);
			stemmerGr.stem();
			result = stemmerGr.getCurrent();
		} else if(lang.equals(LanguageEnum.SPANISH)){				
			stemmerEs.setCurrent(text);
			stemmerEs.stem();
			result = stemmerEs.getCurrent();
		} else if(lang.equals(LanguageEnum.DUTCH)){				
			stemmerNl.setCurrent(text);
			stemmerNl.stem();
			result = stemmerNl.getCurrent();
		}
		else
		{				
			stemmerFr.setCurrent(text);
			stemmerFr.stem();
			result = stemmerFr.getCurrent();
		}
		
		return result;
	}
	
	public static void main(String args[]) {
		System.out.println(Stemming.stemText(LanguageEnum.ENGLISH, "dotplots"));
	}
}


