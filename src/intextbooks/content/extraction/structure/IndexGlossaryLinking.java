package intextbooks.content.extraction.structure;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.skos.SKOSAnnotation;
import org.semanticweb.skos.SKOSConcept;
import org.semanticweb.skos.SKOSConceptScheme;
import org.semanticweb.skos.SKOSCreationException;
import org.semanticweb.skos.SKOSDataset;
import org.semanticweb.skos.SKOSUntypedLiteral;

import intextbooks.SystemLogger;
import intextbooks.content.ContentManager;
import intextbooks.content.enrichment.Concept;
import intextbooks.content.extraction.Utilities.Stemming;
import intextbooks.content.extraction.Utilities.StringOperations;
import intextbooks.content.extraction.buildingBlocks.structure.IndexElement;
import intextbooks.content.extraction.buildingBlocks.structure.IndexTerm;
import intextbooks.ontologie.LanguageEnum;
import intextbooks.persistence.Persistence;
import intextbooks.tools.utility.OntologyUtils;
import intextbooks.tools.utility.StringUtils;
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

	public static List<SKOSConceptScheme> getSchemeList(){
		return conceptSchemeList;
	}
	
}
