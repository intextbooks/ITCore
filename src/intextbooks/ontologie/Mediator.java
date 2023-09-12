package intextbooks.ontologie;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.semanticweb.owlapi.io.RDFResource;
import org.semanticweb.skos.SKOSConcept;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import intextbooks.Configuration;
import intextbooks.SystemLogger;
import intextbooks.persistence.Persistence;
import intextbooks.tools.utility.StringUtils;

public class Mediator {
	private static Mediator instance = null;
	private String interNS;
	private Model ontology;
	private int numberOfConcepts = 0;
	private static SystemLogger logger = SystemLogger.getInstance();
	
	protected Mediator() throws FileNotFoundException{
		 Model model = ModelFactory.createDefaultModel();
		 String rdfSource = Configuration.getInstance().getOntologyPath();
		 //InputStream in = Mediator.class.getResourceAsStream(rdfSource);
		 String path = System.getProperty("user.dir")+"/"+ rdfSource;
		 System.out.println("##Path: " + System.getProperty("user.dir"));
		 System.out.println("##rdf: " + rdfSource);
		 InputStream in = new FileInputStream(rdfSource);
		 if (in == null) {
			 //throw new IllegalArgumentException( "File: " + path + " not found");
			 throw new IllegalArgumentException( "File: " + rdfSource +" not found");
		 }
		
		 this.ontology = model.read(in, "");
		 this.interNS = Configuration.getInstance().getOntologyNS();
		 this.numberOfConcepts = getAllConcepts(false).size();
	}
	
	public static Mediator getInstance() {
		if(instance == null) {
			try {
				instance = new Mediator();
			} catch (FileNotFoundException e) {
				e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
				logger.log(e.getMessage());
				e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 	
			}
		}
		return instance;
	}
	
	public int getNumberOfConcepts(){
		return this.numberOfConcepts;
	}
	

	public ArrayList<String> getDefinitionOfConcept(String conceptName, LanguageEnum targetLang){
		ArrayList<String> desciptions = null;
		
		 try {
			 
			 String encodedConceptName = URLEncoder.encode(conceptName, "UTF-8");
			 String queryString =
						"PREFIX skos:<http://www.w3.org/2004/02/skos/core#> PREFIX interlingua: <"+interNS+">"+
						 "SELECT ?definition "+
								 "WHERE {<"+interNS+conceptName+"> skos:definition ?definition . " +
						         " FILTER langMatches( lang(?definition), '"+targetLang.getShortendLanguageCode()+"' )}";
						 
				 Query query = QueryFactory.create(queryString);
				    
				 // Execute the query and obtain results
				 QueryExecution qe = QueryExecutionFactory.create(query, this.ontology);
				 com.hp.hpl.jena.query.ResultSet results =  qe.execSelect();
				 
				 desciptions = extractLabels(results,"?definition");
				 
				 qe.close();
				 
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}		 
		
		
		return desciptions;
	}
	
	/**
	 * 
	 * @param targetLang
	 * @param conceptName
	 * @param all 
	 * @return labels
	 */
	public ArrayList<String> getLabelsOfConcept(LanguageEnum targetLang, String conceptName, boolean all){
		
		ArrayList<String> labels;
	    labels = getLabelOfConcept(targetLang, conceptName, "prefLabel");
	    
	    if(all)
	    	labels.addAll(getLabelOfConcept(targetLang, conceptName, "altLabel"));

		return labels;
	}
	
	private ArrayList<String> getLabelOfConcept(LanguageEnum targetLang, String conceptName, String labelName){
		ArrayList<String> labels = null;
		
		 try {
			 
			 String encodedConceptName = URLEncoder.encode(conceptName, "UTF-8");
			 String queryString =
						"PREFIX skos:<http://www.w3.org/2004/02/skos/core#> PREFIX interlingua: <"+interNS+">"+
						 "SELECT ?label "+
								 "WHERE {<"+interNS+conceptName+"> skos:"+labelName+"  ?label . " +
						         " FILTER langMatches( lang(?label), '"+targetLang.getShortendLanguageCode()+"' )}";
						 
				 Query query = QueryFactory.create(queryString);
				    
				 // Execute the query and obtain results
				 QueryExecution qe = QueryExecutionFactory.create(query, this.ontology);
				 com.hp.hpl.jena.query.ResultSet results =  qe.execSelect();
				 
				 labels = extractLabels(results,"?label");
				 
				 /*if(labels.size() == 0)
					 if(labelName.equals("prefLabel"))
						 System.out.println("no prefLabel found for concept " + encodedConceptName);
					 else
						 System.
						 out.println("no altLabel found for concept " + encodedConceptName);*/
				 qe.close();
				 
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}		 
		
		
		return labels;
	}
	

	/**
	 * @param label
	 * @param originLang
	 * @return conceptURI
	 */
	public String getConceptForLabel(LanguageEnum originLang, String label, boolean withURI){
		 
		 String queryString = "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> PREFIX interlingua: <" + interNS + ">"
					+ "SELECT ?concept " + "WHERE { {?concept skos:prefLabel  \"" + label + "\"@" + originLang.getShortendLanguageCode() + "}"
					+ "UNION {?concept skos:altLabel  \"" + label + "\"@" + originLang.getShortendLanguageCode() + "} }";
					 
			 Query query = QueryFactory.create(queryString);
			    
			 // Execute the query and obtain results
			 QueryExecution qe = QueryExecutionFactory.create(query, this.ontology);
			 ResultSet results =  qe.execSelect();

			 while(results.hasNext()){
				 String concept = results.nextSolution().getResource("concept").getURI();
				 if(!withURI)
				 	concept = concept.substring(concept.indexOf("#")+1, concept.length());
				 	
				return concept;
			 }
			 
			 return null;
		
	}
	
	public String getConceptForLabel(LanguageEnum originLang, String label, boolean withURI, boolean upperCaseRetry) {

		String queryString = "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> PREFIX interlingua: <" + interNS + ">"
				+ "SELECT ?concept " + "WHERE { {?concept skos:prefLabel  \"" + label + "\"@" + originLang.getShortendLanguageCode() + "}"
				+ "UNION {?concept skos:altLabel  \"" + label + "\"@" + originLang.getShortendLanguageCode() + "} }";

		Query query = QueryFactory.create(queryString);

		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.create(query, this.ontology);
		ResultSet results = qe.execSelect();

		if (!results.hasNext() && upperCaseRetry) {
			return this.getConceptForLabel(originLang, StringUtils.firstCharToLowerCase(label), withURI, false);
		}

		while (results.hasNext()) {

			String concept = results.nextSolution().getResource("concept").getURI();

			if (!withURI) {
				concept = concept.substring(concept.indexOf("#") + 1, concept.length());
			}

			return concept;
		}

		return null;
	}
	
	
	public ArrayList<String> getURIsForConcept(String conceptName) {
		return getDBpediaURIsForConceptHelper(conceptName, null);
	}
	
	public ArrayList<String> getDBpediaURIsForConcept(String conceptName, LanguageEnum language) {
		return getDBpediaURIsForConceptHelper(conceptName, language.getShortendLanguageCode());
	}
	
	private ArrayList<String> getDBpediaURIsForConceptHelper(String conceptName, String language) {
		
		try {
			String decodedConceptName = URLDecoder.decode(conceptName, "UTF-8");
			String queryString;
			
			if(language == null)
				queryString = "PREFIX terms:<http://purl.org/dc/terms/>"
						+"SELECT ?source "+
						"WHERE {<"+interNS+conceptName+"> terms:source ?source}";
			else
				queryString = "PREFIX terms:<http://purl.org/dc/terms/>"
						+"SELECT ?source "+
						"WHERE {<"+interNS+conceptName+"> terms:source ?source ."
						         + " FILTER langMatches( lang(?source), '"+language+"')}";
			
			Query query = QueryFactory.create(queryString);

			// Execute the query and obtain results
			QueryExecution qe = QueryExecutionFactory.create(query, this.ontology);
			ResultSet results = qe.execSelect();
			
			 ArrayList<String> URIs = new ArrayList<String>();
			 
			 while(results.hasNext()){
					QuerySolution queryResult = results.nextSolution();
					URIs.add(queryResult.getLiteral("?source").toString());
			 }
				 
			 return URIs;
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}
		
		
		return null;
	}
	
	public ArrayList<String> getWikipediaLinksForConcept(String conceptName, LanguageEnum language) {
		
		String queryString;
		
		if(language == null)
			queryString = "PREFIX foaf:<http://xmlns.com/foaf/0.1/>"
					+"SELECT ?source "+
					"WHERE {<"+interNS+conceptName+"> foaf:isPrimaryTopicOf ?source}";
		else
			queryString = "PREFIX foaf:<http://xmlns.com/foaf/0.1/>"
					+"SELECT ?source "+
					"WHERE {<"+interNS+conceptName+"> foaf:isPrimaryTopicOf ?source ."
					         + " FILTER langMatches( lang(?source), '"+language.getShortendLanguageCode()+"')}";
		
		Query query = QueryFactory.create(queryString);
		
		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.create(query, this.ontology);
		ResultSet results = qe.execSelect();
					
		ArrayList<String> URIs = new ArrayList<String>();
					 
		while(results.hasNext()){
			QuerySolution queryResult = results.nextSolution();
			String w = queryResult.getLiteral("?source").toString();
			w = w.replaceAll("@..", "");
			URIs.add(w);
		}
						 
		return URIs;
		
	}
	
	public ArrayList<String> getRelatedConcepts(String conceptName) {
		
		String queryString = "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> PREFIX interlingua: <" + interNS + "> "
				+ "SELECT ?concept WHERE {<"+ interNS + conceptName + "> skos:related ?concept}";
		
		Query query = QueryFactory.create(queryString);
		QueryExecution qe = QueryExecutionFactory.create(query, ontology);
		ResultSet results = qe.execSelect();
		
		ArrayList<String> relatedConcepts = new ArrayList<String>();
		
		while(results.hasNext()){
			QuerySolution queryResult = results.nextSolution();
			relatedConcepts.add(queryResult.getResource("concept").getURI());
		}
		
		return relatedConcepts;
		
	}
	
	public ArrayList<String> translateTerm(LanguageEnum originLang,LanguageEnum targetLang, String label, boolean all){
		String concept = getConceptForLabel(originLang, label, false);
		ArrayList<String> translations = new ArrayList<String>();
		
		if(concept != null)
				translations = getLabelsOfConcept(targetLang, concept, all);
		
		
		return translations;
	}
	
	public ArrayList<String> getConceptsInLanguage(LanguageEnum targetLang) {
		ArrayList<String> knownConcepts = getAllConcepts(false);
		Iterator<String> iter = knownConcepts.iterator();
		
		ArrayList<String> conceptList = new ArrayList<String>();
		System.out.println("concepts found #" + knownConcepts.size());
		
		while(iter.hasNext()){
			conceptList.addAll(getLabelsOfConcept(targetLang, iter.next(), false));
		}
		return conceptList;
	}
	
	public ArrayList<String> getAllConcepts(boolean withURI) {
		 String queryString =
					"PREFIX skos:<http://www.w3.org/2004/02/skos/core#> PREFIX interlingua: <"+interNS+">"+
					 "SELECT ?concept "+
							 "WHERE { ?concept skos:inScheme  <"+interNS+"glossary> }";
		 Query query = QueryFactory.create(queryString);
		    
		 // Execute the query and obtain results
		 QueryExecution qe = QueryExecutionFactory.create(query, this.ontology);
		 com.hp.hpl.jena.query.ResultSet results =  qe.execSelect();
		 
		 ArrayList<String> concepts = new ArrayList<String>();
		 
		 while(results.hasNext()){
			 String concept = results.nextSolution().getResource("concept").getURI();
			 if(!withURI)
			 	concept = concept.substring(concept.indexOf("#")+1, concept.length());
			 
			 concepts.add(concept);
		 }

		 return concepts;
	}
	
	private ArrayList<String> extractLabels(ResultSet results, String resultVar){
		ArrayList<String> resultArray = new ArrayList<String>();
		String currentResult;
		
		while(results.hasNext()){
			QuerySolution queryResult = results.nextSolution();
			currentResult = queryResult.getLiteral(resultVar).toString();
			currentResult = currentResult.substring(0, currentResult.indexOf("@"));
			resultArray.add(currentResult);
	    }
		
		return resultArray;
		
	}
	
	private void printRDF2Console(){
	     this.ontology.write(System.out);
	}

	public List<String> getParentsOfConcept(SKOSConcept concept){
		
		
		String queryString = "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> PREFIX interlingua: <" + interNS + ">"
				+ "SELECT ?concept " + "WHERE  {<" + concept.getURI().toString() + ">  skos:broader ?concept }";
			

		Query query = QueryFactory.create(queryString);

		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.create(query, this.ontology);
		ResultSet results = qe.execSelect();
	
		List<String> parents = new ArrayList<String> ();
		
		 while(results.hasNext()){
			 
			 parents.add(results.nextSolution().getResource("concept").getURI());
		
		 }
	
		 return parents;
	}
	

	
	
	
}
