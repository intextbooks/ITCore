package intextbooks.content.extraction.Utilities;

import java.util.Arrays;
import java
.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import intextbooks.Configuration;
import intextbooks.ontologie.LanguageEnum;

public class NERPersonTagger {
	
	private static NERPersonTagger instanceENGLISH = null;
	private static NERPersonTagger instanceDUTCH = null;
	private static NERPersonTagger instanceGERMAN = null;
	private static NERPersonTagger instanceFRENCH = null;
	private static NERPersonTagger instanceSPANISH = null;
	StanfordCoreNLP pipeline;
	
	protected NERPersonTagger(LanguageEnum lang) {
		 Properties props = new Properties();
		 props.put("annotators", "tokenize,ssplit,pos,lemma,ner");
		 props.put("ner.applyNumericClassifiers", "false");
		 props.put("ner.applyFineGrained", "false");
		 props.put("ner.buildEntityMentions", "false");
		 props.put("ssplit.isOneSentence", "true");
		 
		 switch (lang) {

			case GERMAN:
				props.put("tokenize.language", "German");
				props.put("pos.model", Configuration.getInstance().getGermanPosModel());
				props.put("ner.model", Configuration.getInstance().getGermanNERModel() + "," + Configuration.getInstance().getEnglishNERModel());
				break;
			case FRENCH:
				props.put("tokenize.language", "French");
				props.put("pos.model", Configuration.getInstance().getFrenchPosModel());
				props.put("ner.model", Configuration.getInstance().getFrenchNERModel() + "," + Configuration.getInstance().getEnglishNERModel());
				break;
			case SPANISH:
				props.put("tokenize.language", "Spanish");
				props.put("pos.model", Configuration.getInstance().getSpanishPosModel());
				System.out.println(Configuration.getInstance().getSpanishNERModel() + "," + Configuration.getInstance().getEnglishNERModel());
				props.put("ner.model", Configuration.getInstance().getSpanishNERModel() + "," + Configuration.getInstance().getEnglishNERModel());
				break;
			case DUTCH:
				props.put("tokenize.language", "English");
				props.put("pos.model", Configuration.getInstance().getEnglishPosModel());
				props.put("ner.model", Configuration.getInstance().getDutchNERModel() + "," + Configuration.getInstance().getEnglishNERModel());
				break;
			case ENGLISH:
				default:
				props.put("tokenize.language", "English");
				props.put("pos.model", Configuration.getInstance().getEnglishPosModel());
				props.put("ner.model", Configuration.getInstance().getEnglishNERModel());
				break;
		 }
		 pipeline = new StanfordCoreNLP(props); 
	}
	
	public static NERPersonTagger getInstance(LanguageEnum lang) {
		switch (lang) {
			case DUTCH:
				if(instanceDUTCH == null) {
					instanceDUTCH = new NERPersonTagger(lang);
				}
				return instanceDUTCH;
			case GERMAN:
				if(instanceGERMAN == null) {
					instanceGERMAN = new NERPersonTagger(lang);
				}
				return instanceGERMAN;
			case FRENCH:
				if(instanceFRENCH == null) {
					instanceFRENCH = new NERPersonTagger(lang);
				}
				return instanceFRENCH;
			case SPANISH:
				if(instanceSPANISH == null) {
					instanceSPANISH = new NERPersonTagger(lang);
				}
				return instanceSPANISH;
			case ENGLISH:
			default:
				if(instanceENGLISH == null) {
					instanceENGLISH = new NERPersonTagger(lang);
				}
				return instanceENGLISH;
		}
	}
	
	public List<String> getNerTags(String text){
		 CoreDocument document = new CoreDocument(text);
		 pipeline.annotate(document);
		 
		 return document.sentences().get(0).nerTags();
	}
	
	public List<CoreLabel> getTokens(String text){
		 CoreDocument document = new CoreDocument(text);
		 pipeline.annotate(document);
		 
		 return document.sentences().get(0).tokens();
	}
	
	public static List<String> getPersonLabels() {
		String string = Configuration.getInstance().getNERTags();
		List<String> list = Arrays.asList(string.split("\\s*,\\s*"));
		return list;
	}

	public static void main(String[] args) {
		
		 
		// create a document object
		String s1 = "Patrick Simon  Victor Piché  Alpizar Torres Rojas Amélie A. Gagnon Séverin Dujardin Gérin-Lajoie Jean-Claude Philippon" +
		"Reinier Huitink Marlijn van Vreden Ian Montes Axel Alemán Elena Quesada y Amaya Chicote";
		String s2 = "Ricard Meneu Salvador Peiró";
		// annnotate the document
		

			 
			 for(String ner: NERPersonTagger.getInstance(LanguageEnum.FRENCH).getNerTags(s2)) {
				 System.out.println(ner);
				 System.out.println();
				}
			 
			 NERPersonTagger.getPersonLabels();
//			 for(CoreLabel tok: NERPersonTagger.getInstance(LanguageEnum.SPANISH).getTokens(s2)) {
//				 System.out.println(tok);
//				 System.out.println();
//				}
	}
		

	

}
