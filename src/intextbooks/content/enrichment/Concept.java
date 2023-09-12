package intextbooks.content.enrichment;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.skos.SKOSAnnotation;
import org.semanticweb.skos.SKOSConcept;
import org.semanticweb.skos.SKOSConceptScheme;
import org.semanticweb.skos.SKOSDataset;
import org.semanticweb.skos.SKOSUntypedLiteral;

import intextbooks.SystemLogger;
import intextbooks.ontologie.LanguageEnum;
import intextbooks.tools.utility.OntologyUtils;
import uk.ac.manchester.cs.skos.SKOSRDFVocabulary;

public class Concept implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String conceptName;
	private String externalConceptName;
	private Map<LanguageEnum, Pair<String, Set<String>>> labels;
	
	public Concept(String conceptName ) {
		this.conceptName = conceptName;
		labels = new HashMap<LanguageEnum, Pair<String, Set<String>>>();
	}
	
	public void addLabels(LanguageEnum lang, String prefLabel, Set<String> altLabels) {
		labels.put(lang, Pair.of(prefLabel, altLabels));
	}
	
	public void addPrefLabel(LanguageEnum lang, String label) {
		Pair<String, Set<String>> tempPair = Pair.of(label, new HashSet<String>());
		labels.put(lang, tempPair);
	}
	
	public void addAltLabel(LanguageEnum lang, String label) {
		labels.get(lang).getValue().add(label);
	}
	
	public String getConceptName() {
		return this.conceptName;
	}
	
	public String  getPrefLabel(LanguageEnum lang) {
		return labels.get(lang).getKey();
	}
	
	public Set<String> getAltLabels (LanguageEnum lang) {
		return labels.get(lang).getValue();
	}
	
	public Set<String> getAllLabels(LanguageEnum lang){
		if(labels.get(lang).getValue() != null) {
			Set<String> tempSet = new LinkedHashSet<String>(labels.get(lang).getValue());
			tempSet.add(labels.get(lang).getKey());
			return tempSet;
		} else {
			Set<String> tempSet = new LinkedHashSet<String>();
			tempSet.add(labels.get(lang).getKey());
			return tempSet; 
		}
		
	}
	
	public void removeAltLabels(LanguageEnum lang) {
		labels.get(lang).getValue().clear();
	}
	
	public String getExternalConceptName() {
		return externalConceptName;
	}

	public void setExternalConceptName(String externalConceptName) {
		this.externalConceptName = externalConceptName;
	}

	public Set<LanguageEnum> getLangs() {
		return this.labels.keySet();
	}
	
	public String removeChar(String original, int remove) {
		String newString = "";
		for(char c: original.toCharArray()) {
			if(((int) c) != remove) {
				newString += c;
			}
		}
		return newString;
	}
	
	public static String removeChar(String original) {
		int remove = 8204;
		String newString = "";
		for(char c: original.toCharArray()) {
			if(((int) c) != remove) {
				newString += c;
			}
		}
		return newString;
	}
	
	public boolean containsLabel(LanguageEnum lang, String label){
		Pair<String, Set<String>> l = labels.get(lang);
		if(l != null) {
			if(l.getKey().equals(label)) {
				return true;
			}
			for(String oneLabel: l.getValue()) {
				if(oneLabel.equals(label)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public void removeNonPrintingChars() {
		
		int uniChar = 8204; 
		conceptName = removeChar(conceptName, uniChar);
		for(LanguageEnum key: LanguageEnum.values()) {
			Pair<String, Set<String>> labelsL = labels.get(key);
			if(labelsL != null) {
				String newKey = removeChar( labelsL.getKey(), uniChar);
				Set<String> newValue = new HashSet<String>();
				for(String oldValue: labelsL.getValue()) {
					newValue.add(removeChar( oldValue, uniChar));
				}
				labels.put(key, Pair.of(newKey, newValue));
			}
		}
	}
	
	
	
	@Override
	public String toString() {
		return "Concept [conceptName=" + conceptName + ", externalConceptName=" + externalConceptName + ", Pref="
				+ labels.get(LanguageEnum.ENGLISH).getKey() + " Al = " + labels.get(LanguageEnum.ENGLISH).getValue() + "]";
	}

	public static Set<Concept> createGlossaryFromSKOSConceptOntology(SKOSDataset ontology) {
		//Log
		SystemLogger logger =SystemLogger.getInstance();
		
		//Save Concepts
		Set<Concept> concepts = new HashSet<Concept>();
		
		for (SKOSConceptScheme scheme : ontology.getSKOSConceptSchemes()) {
			
			//each concept in the ontology
			for (SKOSConcept concept : scheme.getConceptsInScheme(ontology)) {
						
				String termName = null;

				try {
					termName = OntologyUtils.getConceptName(concept.getURI());
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					logger.log(e.toString());
				}
				
				//Create concept
				Concept tempConcept = new Concept(termName);
				
				//each PREF name in each LANG for each concept
				for (SKOSAnnotation anno : concept.getSKOSAnnotationsByURI(ontology, SKOSRDFVocabulary.PREFLABEL.getURI())) {

					SKOSUntypedLiteral lit = anno.getAnnotationValueAsConstant().getAsSKOSUntypedLiteral();
					String lang = lit.getLang();
					String term = lit.getLiteral();
					
					tempConcept.addPrefLabel(LanguageEnum.convertShortendCodeToEnum(lang), term);
				}
				
				for (SKOSAnnotation anno : concept.getSKOSAnnotationsByURI(ontology, SKOSRDFVocabulary.ALTLABEL.getURI())) {

					SKOSUntypedLiteral lit = anno.getAnnotationValueAsConstant().getAsSKOSUntypedLiteral();
					String lang = lit.getLang();
					String term = lit.getLiteral();
					
					tempConcept.addAltLabel(LanguageEnum.convertShortendCodeToEnum(lang), term);
				}
				
				concepts.add(tempConcept);
			}
		}
		
		return concepts;
	}
	
	public static void printGlossary(Set<Concept> glossary, LanguageEnum targetLang) {
		System.out.println("GLOSSARY -----------------"  + targetLang);
		if(glossary == null)
			return;
		int number = 0;
		Set<String> external = new HashSet<String>();
		for(Concept concept :  glossary) {
			number++;
			System.out.println("Concept: " + concept.getConceptName());
			System.out.println("\tLang: " + targetLang);
			System.out.print("\t\tPrefLabel: " + concept.getPrefLabel(targetLang));
			System.out.println("\tAltLabels: " + concept.getAltLabels(targetLang));
			System.out.println("\tExternal ConceptName: " + concept.getExternalConceptName());
			if(concept.getExternalConceptName() != null) {
				external.add(concept.getExternalConceptName());
			}
			
		}
		System.out.println("-----------");
		System.out.println("# of concepts in targetLang: " + number);
		System.out.println("# of concepts with external conceptName in targetLang: " + external.size());
	}
	
	public static void main(String args[]) {
		//Set<Concept> g = Extractor.readGlossary("introduction+modern.glossary");
		//Concept.printGlossary(g, LanguageEnum.ENGLISH);
	}
}
