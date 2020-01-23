package Test;

import java.util.Arrays;
import java.util.List;

import intextbooks.TextbookProcessor;
import intextbooks.content.extraction.Utilities.StringOperations;
import intextbooks.ontologie.LanguageEnum;

public class ProcessingTest {
	public static void main(String args[]) {
		
		String[] books = {
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/LiteratureBooks/SpringerLink/2019_Book_HamletAndEmotions.pdf",
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/HistoryBooks/SpringerLink/2019_Book_MediaAndTheColdWarInThe1980s.pdf",
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/2015_Book_SocialStatisticsAndEthnicDiver.pdf",
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/2011_Book_LaRefundaciónDeLaAtenciónPrima.pdf",
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/2012_Book_IncentivosALaIDIDeMedicamentos.pdf",
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/2014_Book_MigraineCéphaléesDeLEnfantEtDe.pdf",
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/2017_Book_KlimawandelInDeutschland.pdf"		
		};
		
		String[] indexBooks = {
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/StatisticsBooks/SpringerLink/2017_Book_IntuitiveIntroductoryStatistic.pdf",
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/StatisticsBooks/SpringerLink/2018_Book_ProbabilityAndStatisticsForCom.pdf",
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/HistoryBooks/SpringerLink/2019_Book_StudentRadicalismAndTheFormati.pdf",
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/HistoryBooks/SpringerLink/2019_Book_MediaAndTheColdWarInThe1980s.pdf",
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/LiteratureBooks/SpringerLink/2019_Book_HamletAndEmotions.pdf",
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/Test/Dekking_ModernIntroduction.pdf",
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/StatisticsBooks/SpringerLink/2013_Book_MathematicalStatisticsForEcono.pdf",
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/Test/pruebaColorBold.pdf",
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/CoreStatisticsBook_processed/2012_Book_ModernMathematicalStatisticsWi.pdf"

		};
		
		String[] staBook = {
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/STA+IR/Walpole_Probability_and_Statistics.pdf",
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/STA+IR/2005_Book_AModernIntroductionToProbabili.pdf"

		};
		
		String[] eduHint = {
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/EduHintOVD/2round/BRICKS-BIO-HV-05-bwt.pdf",
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/EduHintOVD/2round/MBO-MTL-17-B-01-20T-bwt.pdf",
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/EduHintOVD/2round/VMBO-EO-19-01-bwo.pdf",
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/EduHintOVD/2round/VMBO-EO-19-01-bwt.pdf",
				"/home/alpiz001/Documents/INTERLINGUA_BOOKS/EduHintOVD/2round/VMBO-EO-19-01-inl.pdf"

		};
		
		TextbookProcessor.processFullTextbook(staBook[1], LanguageEnum.ENGLISH, false, false, null, false, false, "isaacalpizar@gmail.com");
		
		
//		List<String> list = Arrays.asList("a","b","c");
//	    String result = String.join(" <> ", list);
//	    System.out.println(result);
	
		//System.out.println(Arrays.toString("1987".split(",")));
		//System.out.prin tln(Arrays.toString("90,10-60".split(",")));
		/*String and = "and";
		String text = "casa and house but casand no";
		System.out.println(text.replaceAll(and, "X"));
		System.out.println(text.replaceAll("\\b"+ and + "\\b", "X"));*/
		
//		"fr" : {
//            "tokenize.language" : "fr",
//            "pos.model" : "edu/stanford/nlp/models/pos-tagger/french/french.tagger",
//            "parse.model" : "edu/stanford/nlp/models/lexparser/frenchFactored.ser.gz",
//            // dependency parser
//            "depparse.model" : "edu/stanford/nlp/models/parser/nndep/UD_French.gz",
//            "depparse.language" : "french",
//            "ner.model":  DATA_ROOT+"/eunews.fr.crf.gz",
//            "ssplit.newlineIsSentenceBreak": "always" 
//        },
	}
}
