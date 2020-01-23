package intextbooks.content.extraction.structure;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.IntStream;

import org.semanticweb.skos.SKOSAnnotation;
import org.semanticweb.skos.SKOSConcept;
import org.semanticweb.skos.SKOSConceptScheme;
import org.semanticweb.skos.SKOSCreationException;
import org.semanticweb.skos.SKOSDataset;
import org.semanticweb.skos.SKOSUntypedLiteral;

import intextbooks.SystemLogger;
import intextbooks.content.ContentManager;
import intextbooks.content.extraction.Utilities.Stemming;
import intextbooks.content.extraction.Utilities.StringOperations;
import intextbooks.content.extraction.buildingBlocks.structure.IndexElement;
import intextbooks.content.extraction.buildingBlocks.structure.IndexTerm;
import intextbooks.ontologie.LanguageEnum;
import intextbooks.persistence.Persistence;
import intextbooks.tools.utility.OntologyUtils;
import uk.ac.manchester.cs.skos.SKOSRDFVocabulary;

public class IndexGlossaryLinking {
	
	private static ContentManager cm = ContentManager.getInstance();
	private static List<SKOSConceptScheme> conceptSchemeList = new ArrayList<SKOSConceptScheme>();
	
		
	/**
	 * 
	 * @param ds
	 * @return
	 */
	
	public static  Set<SKOSConcept> getGlossaryConcepts(SKOSDataset ds){

			Set <SKOSConceptScheme> conceptSchemes = 	ds.getSKOSConceptSchemes();

			Iterator<SKOSConceptScheme> iterate = conceptSchemes.iterator();

			SKOSConceptScheme conceptScheme = null ;
			Set<SKOSConcept> concepts = null;

			 
			while(iterate.hasNext()){

				conceptScheme = iterate.next();		

				conceptSchemeList.add(conceptScheme);
				
				if(concepts == null){
					concepts=  ds.getConceptsInScheme(conceptScheme);
				}
				else{
					concepts.addAll(ds.getConceptsInScheme(conceptScheme));
				}			
			}
			
			return concepts;
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
			} 
			
			newText += val;
			last = val;
		}
		return newText;
	}
	
	public static String[] getStemmedStringArray(String[] array, LanguageEnum lang, String textoO) {
		List<String> result = new ArrayList<String>(array.length);
		
		for(int i = 0; i < array.length; i++) {
			String w = array[i];
			w = w.replace("(", "");
			w = w.replace(")", "");
			result.add(Stemming.stemText(lang, w));
		}
		
		return result.toArray(new String[0]);
	}

	/**
	 * 
	 * @param glossaryTerm
	 * @param indexTerm
	 * @return
	 */
	public static double searchForMatchingWords(String[] glossaryTerm, String[] indexTerm){
		float weightOfaChar;
		short totalLengthOfStream = 0;
		double returnValue = 0;
		byte tracker = -1;
		double resultRatio = 0;
		double indexToGlossaryTermCount ; 
		
		byte glossaryTermSize = 0;
		byte indexTermSize = 0;
		
		
		glossaryTermSize = (byte) glossaryTerm.length;
		indexTermSize = (byte) indexTerm.length;
		
		if(indexTermSize> glossaryTermSize)
			indexToGlossaryTermCount = glossaryTermSize/indexTermSize;
		
		else
			indexToGlossaryTermCount = indexTermSize/glossaryTermSize;
		
		for(byte i = 0; i< indexTerm.length; i++)
			totalLengthOfStream+= indexTerm[i].length();
		
		weightOfaChar = (float)1/(float)totalLengthOfStream;
		
		for(byte i = 0; i < indexTerm.length; i++){
			
			returnValue = 0;
			double	highestmatchRatio = 0.7;
			tracker = -1;
			
			for(byte j = 0; j < glossaryTerm.length; j++){
				
				double ratio = StringOperations.similarity(indexTerm[i],glossaryTerm[j]);
				
				if(ratio > highestmatchRatio){
					highestmatchRatio = ratio;
					tracker = j;					
				}					
			}
			
			if(tracker!= -1){
				returnValue = highestmatchRatio;
				glossaryTerm[tracker] = "";	
			}
			
			resultRatio += returnValue * weightOfaChar * indexTerm[i].length();
		}
			
		if(resultRatio > 0.9)			
			return resultRatio  * indexToGlossaryTermCount;
			
		return 0;
	}

	
	/**
	 * Links index terms to glossary concepts.
	 * It returns a list of all the glossary terms that were not identified in the index terms. 
	 * 
	 * @param bookID
	 * @param lang
	 * @param indexTerms
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	
	public static Set<SKOSConcept> linkIndexToGlossary(String bookID, LanguageEnum lang, List<IndexElement> indexElements) throws UnsupportedEncodingException{

		SystemLogger.getInstance().log("Linking Index to Glossary Terms");	
	
		SKOSDataset ds = null;
		
		Set<SKOSConcept>concepts = getGlossaryConcepts(ds);
		
		boolean match = false;
		int directHitCount =0;
		int hierarchicalHitCount =0;
		int hitCount =0;
		
		SystemLogger.getInstance().log("Glossary size : " + concepts.size());
		
		double currentMatchRatio, highestmatchRatio = 0;
		
		SKOSConcept conceptToLink = null;
		
		List<SKOSConcept> removeFromConcepts = new ArrayList<SKOSConcept> ();
		
		String glossaryTerm = null;

		for(int i = 0; i < indexElements.size(); i++){
			
//			/*TESTING*/
//			if(indexTerms.get(i).getID().equals("Diagrama(s) de dispersión") || indexTerms.get(i).getID().equals("Estimador eficiente") || indexTerms.get(i).getID().equals("Distribución t")) {
//				System.out.println(indexTerms.get(i).getID());
//			}
//			/*TESTING*/
			
			glossaryTerm = null;
			conceptToLink = null;
			
			highestmatchRatio = 0.9;
			
			Iterator<SKOSConcept> iterator = concepts.iterator();
						
			while(iterator.hasNext()){
				
				match = false;
				
				SKOSConcept concept = iterator.next();

				for (SKOSAnnotation anno : concept.getSKOSAnnotationsByURI(ds, SKOSRDFVocabulary.PREFLABEL.getURI())) {
					
					SKOSUntypedLiteral lit = anno.getAnnotationValueAsConstant().getAsSKOSUntypedLiteral();
										
					if(lit.getLang().equals(lang.getShortendLanguageCode().toString())){
					
						String term = lit.getLiteral();	
						String originalTerm = term;
						term = term.replaceAll("-", " ");
					
						
						//old way
					    //currentMatchRatio = searchForMatchingWords(Stemming.stemText(lang, term).split(" |-"),Stemming.stemText(lang, indexTerms.get(i).getID()).split(" ") );
						//new way
						term = preProcessParentesis(term);
						String id = null;
						String originalId = null;
						if(indexElements.get(i).isFullLabel()) {
							id = preProcessParentesis(indexElements.get(i).getLabel());
							originalId = indexElements.get(i).getLabel();
						} else {
							id = preProcessParentesis(indexElements.get(i).getNormalizedKey());
							originalId= indexElements.get(i).getNormalizedKey();
						}
						
						
						if(originalId != null && originalId.equalsIgnoreCase(originalTerm)) {
							currentMatchRatio = 1;
						} else {
							currentMatchRatio = searchForMatchingWords(getStemmedStringArray(term.split(" |-"), lang, term),getStemmedStringArray(id.split(" "),lang, indexElements.get(i).getNormalizedKey()));
						}
						
						if(currentMatchRatio > highestmatchRatio ){
							highestmatchRatio = currentMatchRatio;						
							match = true;
							conceptToLink = concept;
						}						
					}					
				}
				
				for (SKOSAnnotation anno : concept.getSKOSAnnotationsByURI(ds, SKOSRDFVocabulary.ALTLABEL.getURI())) {

					SKOSUntypedLiteral lit = anno.getAnnotationValueAsConstant().getAsSKOSUntypedLiteral();
					
					if(lit.getLang().equals(lang.getShortendLanguageCode().toString())){
						
						String term = lit.getLiteral();
						String originalTerm = term;
						term = term.replaceAll("-", " ");
						
						//old way
						//currentMatchRatio = searchForMatchingWords(Stemming.stemText(lang, term).split(" |-"),Stemming.stemText(lang, indexTerms.get(i).getID()).split(" ") );
						//new way
						term = preProcessParentesis(term);
						String id = null;
						String originalId = null;
						if(indexElements.get(i).isFullLabel()) {
							id = preProcessParentesis(indexElements.get(i).getLabel());
							originalId = indexElements.get(i).getLabel();
						} else {
							id = preProcessParentesis(indexElements.get(i).getNormalizedKey());
							originalId= indexElements.get(i).getNormalizedKey();
						}
						
						if(originalId != null && originalId.equalsIgnoreCase(originalTerm)) {
							currentMatchRatio = 1;
						} else {
							currentMatchRatio = searchForMatchingWords(getStemmedStringArray(term.split(" |-"), lang, term),getStemmedStringArray(id.split(" "),lang, indexElements.get(i).getNormalizedKey()));
						}			
						
						if(currentMatchRatio > highestmatchRatio){
							highestmatchRatio = currentMatchRatio;
							match = true;
							conceptToLink = concept;
						}						
					}
				}		
			}
			
			//child entry gets concept from parent entry
//			if(conceptToLink==null && indexTerms.get(i).getParent() != null){
//				glossaryTerm = cm.getConceptOfIndexTerm(bookID, indexTerms.get(i).getParent());
//				if (glossaryTerm != null) {
//					hierarchicalHitCount++;
//				}
//			}
			
			if(conceptToLink!=null){
				
				directHitCount++;
				
				if(!removeFromConcepts.contains(conceptToLink))
					removeFromConcepts.add(conceptToLink);
					
				glossaryTerm =conceptToLink.getURI().toString();
				int lastIndex = glossaryTerm.lastIndexOf("#");
				glossaryTerm = glossaryTerm.substring(lastIndex + 1);

				String glossaryTerm2 = OntologyUtils.getConceptName(conceptToLink.getURI());
 
				hitCount++;
				cm.addConceptToIndex(bookID, indexElements.get(i).getKey() ,glossaryTerm2);
			}
//			else if(glossaryTerm!=null){
//				glossaryTerm = URLDecoder.decode(glossaryTerm, "UTF-8");	
//				cm.addConceptToIndex(bookID, indexTerms.get(i).getID(),glossaryTerm);
//				hitCount++;
//			}
		}	
		
		SystemLogger.getInstance().log("Linking Index to Glossary Terms...... Done");
		SystemLogger.getInstance().log("Total hits# : " + hitCount);
		SystemLogger.getInstance().log("Direct link # : " + directHitCount);
		SystemLogger.getInstance().log("Parental link # : " + hierarchicalHitCount);
		
		concepts.removeAll(removeFromConcepts);
		
		return concepts;
	}


	public static List<SKOSConceptScheme> getSchemeList(){
		return conceptSchemeList;
	}
	
//	public static  SKOSDataset getGlossaryontology(){
//
//		try {
//			SKOSDataset  ds = Persistence.getInstance().loadOntology();
//			return ds;
//		} catch (SKOSCreationException e) {
//			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
//		}
//
//		return null;		 
//	}
	
}
