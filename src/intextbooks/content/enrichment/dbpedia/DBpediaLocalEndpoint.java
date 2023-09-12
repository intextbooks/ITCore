package intextbooks.content.enrichment.dbpedia;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDBFactory;

import intextbooks.Configuration;
import intextbooks.SystemLogger;

public class DBpediaLocalEndpoint {
	
	private static DBpediaLocalEndpoint instance;
	private Model model;
	private String tdbDirectory = Configuration.getInstance().getTdbDirectory();
	SystemLogger logger = SystemLogger.getInstance();
	
	private DBpediaLocalEndpoint() {
		Dataset dataset = TDBFactory.createDataset(tdbDirectory);
		model = dataset.getDefaultModel(); 
	}
	
	synchronized public static DBpediaLocalEndpoint getInstance() {
		if(instance == null){
			 instance = new DBpediaLocalEndpoint();
		}
	    return instance;
	}
	
	/*******************************************************************************
	 ***************************** GENERAl *****************************************
	 *******************************************************************************/
	
	//Execute generic query
	synchronized public ResultSet executeQuery(String queryString) {
		Query query = QueryFactory.create(queryString);
		try(QueryExecution qexec = QueryExecutionFactory.create(query, model)){
			 ResultSet result = qexec.execSelect();
			 result = ResultSetFactory.copyResults(result) ;
			 return result;
		} catch (Exception e) {
			logger.log("EXCEPTION executeQuery LOCAL ENDPOINT: " + e.getMessage());
			return null;
		}
	}
	
	//Execute generic ask
	synchronized public boolean executeAsk(String queryString) {
		Query query = QueryFactory.create(queryString);
		try(QueryExecution qexec = QueryExecutionFactory.create(query, model)){
			 boolean result = qexec.execAsk();
			 return result;
		} catch (Exception e) {
			logger.log("EXCEPTION executeQuery LOCAL ENDPOINT: " + e.getMessage());
			return false;
		}
	}
	
	//Get all properties
	public void getAllProperties(String resource) {
		String queryString = "select ?property ?b where {\n" + 
				"         <" + resource + "> ?property ?b . }"; 
		Query query = QueryFactory.create(queryString);
		try(QueryExecution qexec = QueryExecutionFactory.create(query, model)){
		     ResultSet results = qexec.execSelect();
		     ResultSetFormatter.out(System.out, results, query) ;
		}
	}
	
	/*******************************************************************************
	 ***************************** SPACIFIC ****************************************
	 *******************************************************************************/
	
	/*
	 * Returns the abstracts of all resources that link to target resource
	 */
	synchronized public String querySourceAbstractsFromTargetCandidate(String targetR) {
		
		//0. String to save the abstracts
		String abstracts = "";
		//1. Get the resources that link to target resource
		String queryLinks = "SELECT DISTINCT ?s WHERE { ?s <http://dbpedia.org/ontology/wikiPageWikiLink> <" + targetR + "> . }"; 
		Query query = null;
		try {
			query = QueryFactory.create(queryLinks);
		} catch (Exception e) {
			e.printStackTrace();
			return abstracts;
		}
		try(QueryExecution qexec = QueryExecutionFactory.create(query, model)){
		     ResultSet results = qexec.execSelect();
		     while(results.hasNext()) {
		    	 QuerySolution sol = results.next();
		    	 String sourceR = sol.get("?s").toString();
//		    	 /*TESTING*/
//		    	 System.out.println("& " + sourceR);
//		    	 /*TESTING*/
		    	 //2. Get the abstract of the source resource
		    	 String queryAbstract = "SELECT DISTINCT ?o WHERE { <" + sourceR + "> <http://dbpedia.org/ontology/abstract> ?o . }"; 
		    	 Query query2 = QueryFactory.create(queryAbstract);
		    	 try(QueryExecution qexec2 = QueryExecutionFactory.create(query2, model)){
		    		 ResultSet results2 = qexec2.execSelect();
		    		 while(results2.hasNext()) {
		    			 QuerySolution sol2 = results2.next();
				    	 String abstractS = sol2.get("?o").toString();
				    	 int size = abstractS.length();
				    	 String sub = abstractS.substring(size-3);
						 if (sub.matches("@..")) {
							 abstractS = abstractS.substring(0, size-3);
						 }
				    	 abstracts += " " + abstractS;
//				    	 /*TESTING*/
//				    	 System.out.println("OK");
//				    	 System.out.println(sourceR + " > " + abstractS);
//				    	 Scanner s= new Scanner(System.in);
//				    	 char c = '0';
//						 try {
//							c = s.next().charAt(0);
//						 } catch (Exception e) {
//							e.printStackTrace();
//						 }
//						 /*TESTING*/
		    		 }
		    	 }
		     }
		} catch (Exception e) {
			e.printStackTrace();
			return abstracts;
		}
		//3. Get abstract of the target resource
		String queryAbstractT = "SELECT DISTINCT ?o WHERE { <" + targetR + "> <http://dbpedia.org/ontology/abstract> ?o . }"; 
		Query query3 = null;
		try {
			query3 = QueryFactory.create(queryAbstractT);
		} catch (Exception e) {
			e.printStackTrace();
			return abstracts;
		}
		try(QueryExecution qexec3 = QueryExecutionFactory.create(query3, model)){
			ResultSet results3 = qexec3.execSelect();
		    while(results3.hasNext()) {
		    	 QuerySolution sol = results3.next();
		    	 String abstractT = sol.get("?o").toString();
		    	 int size = abstractT.length();
		    	 String sub = abstractT.substring(size-3);
				 if (sub.matches("@..")) {
					 abstractT = abstractT.substring(0, size-3);
				 }
		    	 abstracts += " " + abstractT;
		    }
		}
		
		//4. return abstracts
		return abstracts;
	}
	
	synchronized public int getInboundLinksCount(String targetR) {

		//System.out.println(targetR);
		String queryAbstractT = "SELECT (count(?v) as ?o)  WHERE { ?v <http://dbpedia.org/ontology/wikiPageWikiLink> <"+ targetR + "> . }";
		//System.out.println(queryAbstractT);
		Query query = QueryFactory.create(queryAbstractT);
		try(QueryExecution qexec = QueryExecutionFactory.create(query, model)){
			 ResultSet result = qexec.execSelect();
			 while(result.hasNext()) {
				 QuerySolution sol = result.next();
		    	 String cant = sol.get("?o").asLiteral().getString();
		    	 return Integer.parseInt(cant);
			 }
			 return 0;
		} catch (Exception e) {
			return 0;
		}
	}
	
	synchronized public String getSimpleAbstract(String targetR) {
		String queryAbstractT = "SELECT DISTINCT ?o WHERE { <" + targetR + "> <http://dbpedia.org/ontology/abstract> ?o . }";
		Query query = QueryFactory.create(queryAbstractT);
		try(QueryExecution qexec = QueryExecutionFactory.create(query, model)){
			 ResultSet result = qexec.execSelect();
			 while(result.hasNext()) {
				 QuerySolution sol = result.next();
		    	 String abstractT = sol.get("?o").toString();
		    	 int size = abstractT.length();
		    	 String sub = abstractT.substring(size-3);
				 if (sub.matches("@..")) {
					 abstractT = abstractT.substring(0, size-3);
				 }
		    	 return abstractT;
			 }
			 return null;
		} catch (Exception e) {
			return null;
		}
	}
	

	
	synchronized public Set<String> getSiblingCategories(String targetR) {
		Set<String> siblings = new HashSet<String>();
		List<String> parents = this.getBroaderCategories(targetR);
		for(String parent: parents) {
			List<String> children = this.getBroaderOfCategories(parent);
			siblings.addAll(children);
		}
		
		siblings.remove(targetR);
		return siblings;
	}
	
	synchronized public List<String> getCategories(String targetR) {
		List<String> categories = new ArrayList<String>();
		String queryAbstractT = "SELECT DISTINCT ?o WHERE { <" + targetR + "> <http://purl.org/dc/terms/subject> ?o . }";
		Query query = QueryFactory.create(queryAbstractT);
		try(QueryExecution qexec = QueryExecutionFactory.create(query, model)){
			 ResultSet result = qexec.execSelect();
			 while(result.hasNext()) {
				 QuerySolution sol = result.next();
		    	 String cat = sol.get("?o").toString();
		    	categories.add(cat);
			 }
		} catch (Exception e) {
			
		}
		return categories;
	}
	
	synchronized public List<String> getOneLevelBroaderCatHierarchy(String targetR) {
		List<String> categories = new ArrayList<String>();
		//broader of
		categories.addAll(this.getBroaderOfCategories(targetR));
		//broader
		categories.addAll(this.getBroaderCategories(targetR));
		
		return categories;
	}
	
	synchronized public List<String> getBroaderOfCategories(String targetR) {
		List<String> categories = new ArrayList<String>();
		String queryString = "SELECT DISTINCT ?o WHERE { ?o <http://www.w3.org/2004/02/skos/core#broader> <" + targetR + "> . }";
		Query query = QueryFactory.create(queryString);
		try(QueryExecution qexec = QueryExecutionFactory.create(query, model)){
			 ResultSet result = qexec.execSelect();
			 while(result.hasNext()) {
				 QuerySolution sol = result.next();
		    	 String cat = sol.get("?o").toString();
		    	categories.add(cat);
			 }
		} catch (Exception e) {
			SystemLogger.getInstance().log("ERROR getBroaderOfCategories -> " + targetR + " " + e.getMessage());
		}
		return categories;
	}
	
	synchronized public List<String> getBroaderCategories(String targetR) {
		List<String> categories = new ArrayList<String>();
		String queryString = "SELECT DISTINCT ?o WHERE { <" + targetR + "> <http://www.w3.org/2004/02/skos/core#broader> ?o . }";
		Query query = QueryFactory.create(queryString);
		try(QueryExecution qexec = QueryExecutionFactory.create(query, model)){
			 ResultSet result = qexec.execSelect();
			 while(result.hasNext()) {
				 QuerySolution sol = result.next();
		    	 String cat = sol.get("?o").toString();
		    	categories.add(cat);
			 }
		} catch (Exception e) {
			SystemLogger.getInstance().log("ERROR getBroaderCategories -> " + targetR + " " + e.getMessage());
		}
		return categories;
	}
	
	synchronized public List<String> getSubjectOfResources(String targetR) {
		List<String> resources = new ArrayList<String>();
		String queryString = "SELECT DISTINCT ?o WHERE { ?o <http://purl.org/dc/terms/subject> <" + targetR + "> . }";
		Query query = QueryFactory.create(queryString);
		try(QueryExecution qexec = QueryExecutionFactory.create(query, model)){
			 ResultSet result = qexec.execSelect();
			 while(result.hasNext()) {
				 QuerySolution sol = result.next();
		    	 String cat = sol.get("?o").toString();
		    	 resources.add(cat);
			 }
		} catch (Exception e) {
			SystemLogger.getInstance().log("ERROR getBroaderOfCategories -> " + targetR + " " + e.getMessage());
		}
		return resources;
	}
	
	synchronized public List<String> getLinks(String targetR) {
		List<String> categories = new ArrayList<String>();
		String queryAbstractT = "SELECT DISTINCT ?o WHERE { <" + targetR + "> <http://dbpedia.org/ontology/wikiPageWikiLink> ?o . }";
		Query query = QueryFactory.create(queryAbstractT);
		try(QueryExecution qexec = QueryExecutionFactory.create(query, model)){
			 ResultSet result = qexec.execSelect();
			 while(result.hasNext()) {
				 QuerySolution sol = result.next();
		    	 String cat = sol.get("?o").toString();
		    	categories.add(cat);
			 }
		} catch (Exception e) {
			
		}
		return categories;
	}
	
	synchronized public String getWikiLink(String targetR) {
		String queryAbstractT = "SELECT DISTINCT ?o WHERE { <" + targetR + "> <http://xmlns.com/foaf/0.1/isPrimaryTopicOf> ?o . }";
		Query query = QueryFactory.create(queryAbstractT);
		try(QueryExecution qexec = QueryExecutionFactory.create(query, model)){
			 ResultSet result = qexec.execSelect();
			 while(result.hasNext()) {
				 QuerySolution sol = result.next();
		    	 String link = sol.get("?o").toString();
		    	 return link;
			 }
		} catch (Exception e) {
			return null;
		}
		return null;
	}
	
	//@todo
//	public Set<String> getSynonymsOfTerm(String term, String domain){
//		Set<String> results = new HashSet<String>();
//		
//		//DBpedia sparql endpoint according to language
//		String service = null;
//		if (domain.equals("en")) {
//			service = this.enService;
//		} else if (domain.equals("fr")) {
//			service = this.frService;
//		} else if (domain.equals("de")) {
//			//service = this.deService;
//			service = this.enService;
//		} else if (domain.equals("es")) {
//			service = this.esService;
//		} else if (domain.equals("nl")) {
//			service = this.nlService;
//		} else if (domain.equals("ru")) {
//			//service = this.ruService;
//			service = this.enService;
//		} else {
//			service = this.enService;
//		}
//		
//		String newTerm = getDirectedPage(term, domain, service);
//		if(newTerm != null) {
//			term = newTerm;
//		} else {
//			//Normalized term to DBPEDIA URI
//			term = this.getDBpediaLink(term, domain);
//		}
//		
//		//get redirects
//		String q0 = this.createQuery(term, this.dict.get("REDIRECT-PAGES"), null, domain);
//		ResultSet res0 = null;
//		try {
//			res0 = this.localEndpoint.executeQuery(q0);
//		} catch (Exception e) {
//			this.logger.log("@@ error askDomainEntity REDIRECTS service:" + service + " query: " + q0);
//		}
//		if(res0 != null && res0.hasNext()) {
//			while(res0.hasNext()) {
//				QuerySolution sol = res0.next();
//				String candidateURI = sol.get("?res").toString();
//				results.add(StringUtils.truncateURL(candidateURI).toLowerCase().trim());
//			}
//		}
//		
//		return results;
//	}
	
	

	public static void main(String[] args) {
		DBpediaLocalEndpoint source = DBpediaLocalEndpoint.getInstance();
		//source.getAllProperties("http://dbpedia.org/resource/Tail_probability");
		//source.querySourceAbstractsFromTargetCandidate("http://dbpedia.org/resource/Sum_of_squares");
		//System.out.println(source.getBroaderOfCategories("http://dbpedia.org/resource/Category:Statistics"));
		//System.out.println(source.getSiblingCategories("http://dbpedia.org/resource/Category:Statistics"));
		//System.out.println(source.getOneLevelBroaderCatHierarchy("http://dbpedia.org/resource/Category:Statistics").size());
		//String query = "PREFIX dbpedia-ru: <http://ru.dbpedia.org/resource/> PREFIX dbpedia-nl: <http://nl.dbpedia.org/resource/> PREFIX dbpedia-es: <http://es.dbpedia.org/resource/> PREFIX dbpedia-de: <http://de.dbpedia.org/resource/> PREFIX dbpedia-fr: <http://fr.dbpedia.org/resource/> PREFIX dbpedia: <http://dbpedia.org/resource/> PREFIX dbpedia-owl:<http://dbpedia.org/ontology/> PREFIX dbpprop: <http://dbpedia.org/property/> PREFIX owl:<http://www.w3.org/2002/07/owl#> PREFIX dcterms: <http://purl.org/dc/terms/> PREFIX foaf: <http://xmlns.com/foaf/0.1/> PREFIX skos: <http://www.w3.org/2004/02/skos/core#> select ?res where {<http://dbpedia.org/resource/Markov_process> dbpedia-owl:wikiPageRedirects ?res }";
		String query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> select distinct ?super where {\n" + 
				"  ?super (^skos:broader){0,3} <http://dbpedia.org/resource/Category:Statistics>, <http://dbpedia.org/resource/Category:Plots_(graphics)>\n" + 
				"} ";
		ResultSet sr = source.executeQuery(query);
		ResultSetFormatter.out(System.out, sr, QueryFactory.create(query)) ;
		//System.out.println(source.getLinks("http://dbpedia.org/resource/Mean"));
		//String res = source.querySourceAbstractsFromTargetCandidate("http://dbpedia.org/resource/Cauchy_distribution");
		//System.out.println(res);
		/*try {
			FileUtils.writeStringToFile(new File("test.txt"),res);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		/*try {
			DocumentSimilarity d = new DocumentSimilarity("this is a the text related words", "this is the  words text related", LanguageEnum.ENGLISH);
			System.out.println(d.v1);
			System.out.println(d.v2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

	}

}
