package intextbooks.content.extraction.structure;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import org.semanticweb.skos.AddAssertion;
import org.semanticweb.skos.SKOSAnnotation;
import org.semanticweb.skos.SKOSAnnotationAssertion;
import org.semanticweb.skos.SKOSChange;
import org.semanticweb.skos.SKOSChangeException;
import org.semanticweb.skos.SKOSConcept;
import org.semanticweb.skos.SKOSConceptScheme;
import org.semanticweb.skos.SKOSCreationException;
import org.semanticweb.skos.SKOSDataFactory;
import org.semanticweb.skos.SKOSDataset;
import org.semanticweb.skos.SKOSEntityAssertion;
import org.semanticweb.skos.SKOSObjectProperty;
import org.semanticweb.skos.SKOSObjectRelationAssertion;
import org.semanticweb.skos.SKOSStorageException;
import org.semanticweb.skos.properties.SKOSBroaderTransitiveProperty;
import org.semanticweb.skos.properties.SKOSInSchemeProperty;
import org.semanticweb.skos.properties.SKOSNarrowerTransitiveProperty;
import org.semanticweb.skos.properties.SKOSRelatedProperty;
import org.semanticweb.skosapibinding.SKOSManager;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Seq;

import intextbooks.Configuration;
import intextbooks.SystemLogger;
import intextbooks.content.ContentManager;
import intextbooks.content.extraction.ExtractorController.resourceType;
import intextbooks.content.extraction.buildingBlocks.format.ResourceUnit;
import intextbooks.content.extraction.buildingBlocks.structure.IndexElement;
import intextbooks.content.extraction.buildingBlocks.structure.NodeDataContainer;
import intextbooks.content.extraction.format.FormatExtractor;
import intextbooks.persistence.Persistence;


public class StructureBuilder {

	private List <SKOSConcept> conceptList = new ArrayList<SKOSConcept>();
	private List <NodeDataContainer> titles = new Vector <NodeDataContainer> ();
	private List <String> sections = new Vector <String> ();
	private Stack <SKOSConcept> stckConcept = new Stack<SKOSConcept> ();
	private Stack <NodeDataContainer> stckObject = new Stack <NodeDataContainer>();
	private Map <Integer, SKOSConcept> pageMap = new HashMap <Integer, SKOSConcept> ();

	private ContentManager cm = ContentManager.getInstance();

	private  Model modelForPages;

	private SKOSManager manager;
	private SKOSDataFactory df;
	private SKOSDataset dataset;

	private SKOSBroaderTransitiveProperty isBroaderTrans;
	private SKOSNarrowerTransitiveProperty isNarrowerTrans;
	private SKOSInSchemeProperty inScheme;
	private SKOSRelatedProperty isRelated;

	private  String  baseURI, basePageURI, nameSpace;
	
	/**
	 * 
	 * Book related concepts
	 * 
	 */
	
	private  String baseTermURI, baseSegmentURI ;
	private String bookID;
	
	private SKOSConceptScheme bookScheme;
	private SKOSConceptScheme indexScheme;
	private SKOSConceptScheme pageScheme;
	private SKOSConceptScheme chapterScheme;
	private SKOSConceptScheme sectionScheme;
	private SKOSConceptScheme paragraphScheme;

	/**
	 * 
	 * Presentation related concepts
	 * 
	 */
	
	private SKOSConceptScheme presentationScheme;
	

	public StructureBuilder(String bookID, resourceType type) {

		this.nameSpace = Configuration.getInstance().getOntologyNS().replaceAll("#", "/");
		
		if(type.equals(resourceType.BOOK))
			this.baseURI = nameSpace+"Book/"+bookID+"#";
		else
			this.baseURI = nameSpace+"Presentation/"+bookID+"#";
		
		this.bookID = bookID;
		
		this.baseSegmentURI = baseURI.replaceAll("#", "")+"/segment#";
		
		if(type.equals(resourceType.BOOK)){
		
			this.basePageURI = baseURI.replaceAll("#", "")+"/page#";

			this.baseTermURI = baseURI.replaceAll("#", "")+"/indexTerm#";
		}
		else if(type.equals(resourceType.SLIDE))
			this.basePageURI = baseURI.replaceAll("#", "")+"/slide#";
					
		try {

			this.manager = new SKOSManager();

			this.initialize(type);

		} catch (SKOSCreationException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}
	}

	/**
	 * 
	 * @param type
	 */
	private void initialize(resourceType type) {
		
		try {

			this.dataset 			= this.manager.createSKOSDataset(URI.create(this.baseURI));
			this.df 				= this.manager.getSKOSDataFactory();

			this. isBroaderTrans 	= df.getSKOSBroaderTransitiveProperty();
			this. isNarrowerTrans 	= df.getSKOSNarrowerTransitiveProperty();
			this. inScheme 			= df.getSKOSInSchemeProperty();
			this. isRelated 		= df.getSKOSRelatedProperty();			

			if(type.equals(resourceType.BOOK)){
				initializeBook();
			}
			else if(type.equals(resourceType.SLIDE)){
				initializePresentation();
			} 
			
		} catch (SKOSCreationException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		} catch (SKOSChangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}
	}
	
	
	/**
	 * 
	 * @throws SKOSChangeException
	 */
	private void initializePresentation() throws SKOSChangeException{
		
		SKOSChange change;
		
		this. bookScheme 		 = df.getSKOSConceptScheme(URI.create(this.baseURI + "presentationScheme"));
		this. pageScheme		 = df.getSKOSConceptScheme(URI.create(this.baseURI + "slideScheme"));
		this. paragraphScheme	 = df.getSKOSConceptScheme(URI.create(this.baseURI + "bodyScheme"));
		this. chapterScheme		= df.getSKOSConceptScheme(URI.create(this.baseURI + "chapterScheme"));
		this. sectionScheme		= df.getSKOSConceptScheme(URI.create(this.baseURI + "sectionScheme"));
		this. indexScheme		= df.getSKOSConceptScheme(URI.create(this.baseURI + "indexScheme"));
		
		SKOSEntityAssertion presentationEntityAssertion = df.getSKOSEntityAssertion(this.bookScheme);
		change = new AddAssertion(this.dataset, presentationEntityAssertion);			
		this.manager.applyChange(change);

		SKOSEntityAssertion pageEntitiyAssertion = df.getSKOSEntityAssertion(this.pageScheme);
		change = new AddAssertion(this.dataset, pageEntitiyAssertion);			
		this.manager.applyChange(change);

		SKOSEntityAssertion paragraphEntitiyAssertion = df.getSKOSEntityAssertion(this.paragraphScheme);
		change = new AddAssertion(this.dataset, paragraphEntitiyAssertion);			
		this.manager.applyChange(change);
		
		SKOSEntityAssertion chapterEntitiyAssertion = df.getSKOSEntityAssertion(this.chapterScheme);
		change = new AddAssertion(this.dataset, chapterEntitiyAssertion);			
		this.manager.applyChange(change);	
		
		SKOSEntityAssertion sectionEntitiyAssertion = df.getSKOSEntityAssertion(this.sectionScheme);
		change = new AddAssertion(this.dataset, sectionEntitiyAssertion);			
		this.manager.applyChange(change);	
	
		SKOSEntityAssertion indexEntitiyAssertion = df.getSKOSEntityAssertion(this.indexScheme);
		change = new AddAssertion(this.dataset, indexEntitiyAssertion);			
		this.manager.applyChange(change);

	}

	
	/**
	 * 
	 * @throws SKOSChangeException
	 */
	private void initializeBook() throws SKOSChangeException{
		
		SKOSChange change;

		
		this. bookScheme 		= df.getSKOSConceptScheme(URI.create(this.baseURI + "bookScheme"));
		this. indexScheme		= df.getSKOSConceptScheme(URI.create(this.baseURI + "indexScheme"));
		this. pageScheme		= df.getSKOSConceptScheme(URI.create(this.baseURI + "pageScheme"));
		this. paragraphScheme	= df.getSKOSConceptScheme(URI.create(this.baseURI + "paragraphScheme"));
		this. chapterScheme		= df.getSKOSConceptScheme(URI.create(this.baseURI + "chapterScheme"));	
		this. sectionScheme		= df.getSKOSConceptScheme(URI.create(this.baseURI + "sectionScheme"));

		SKOSEntityAssertion bookEntityAssertion = df.getSKOSEntityAssertion(this.bookScheme);
		change = new AddAssertion(this.dataset, bookEntityAssertion);			
		this.manager.applyChange(change);		

		SKOSEntityAssertion indexEntitiyAssertion = df.getSKOSEntityAssertion(this.indexScheme);
		change = new AddAssertion(this.dataset, indexEntitiyAssertion);			
		this.manager.applyChange(change);

		SKOSEntityAssertion pageEntitiyAssertion = df.getSKOSEntityAssertion(this.pageScheme);
		change = new AddAssertion(this.dataset, pageEntitiyAssertion);			
		this.manager.applyChange(change);

		SKOSEntityAssertion paragraphEntitiyAssertion = df.getSKOSEntityAssertion(this.paragraphScheme);
		change = new AddAssertion(this.dataset, paragraphEntitiyAssertion);			
		this.manager.applyChange(change);

		SKOSEntityAssertion chapterEntitiyAssertion = df.getSKOSEntityAssertion(this.chapterScheme);
		change = new AddAssertion(this.dataset, chapterEntitiyAssertion);			
		this.manager.applyChange(change);	
		
		SKOSEntityAssertion sectionEntitiyAssertion = df.getSKOSEntityAssertion(this.sectionScheme);
		change = new AddAssertion(this.dataset, sectionEntitiyAssertion);			
		this.manager.applyChange(change);	
	}

/**
 * 
 * @param bookID
 * @param pageNumbers
 * @param type
 */
	public void  produceSKOSModelOfPresentation(String bookID, ArrayList<Integer> pageNumbers, resourceType type){
		
		createPageResources(bookID, pageNumbers, type);
			
		try {	
			manager.save(dataset, URI.create("file:/Users/XPS/Desktop/blah.rdf"));
		} catch (SKOSStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}
		System.out.println();
	}
	
	/**
	 * 
	 * @param bookID
	 * @param pageNumbers
	 * @param indexTerms
	 * @param parse
	 * @param type
	 */
	
	public void produceSKOSModelOfBook(String bookID, ArrayList<Integer> pageNumbers, List<IndexElement> indexElements, FormatExtractor parse, resourceType type){
		
		/*
		 * Creates a SKOS:Concept of type 'page' for each physical page of the book
		 */
		createPageResources(bookID, pageNumbers, type);

		try {
			/*
			 * Creates SKOS:Concept for each chapter, subchapter in book from TOC. 
			 */
			SKOSModel(bookID, pageNumbers, parse);
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}


	
	/**
	 * 
	 * @param ds
	 * @return
	 */
	
	private Set<SKOSConcept> getGlossaryConcepts(SKOSDataset ds){

		Set <SKOSConceptScheme> conceptSchemes = 	ds.getSKOSConceptSchemes();

		Iterator<SKOSConceptScheme> iterate = conceptSchemes.iterator();

		SKOSConceptScheme conceptScheme = null ;

		while(iterate.hasNext())
			conceptScheme = iterate.next();			

		Set<SKOSConcept> concepts=  ds.getConceptsInScheme(conceptScheme);
		
		return concepts;
}


 
	
	
	
	
	/**
	 * 
	 * @param indexTerm
	 * @param concepts
	 * @return
	 */
	private SKOSConcept getRelatedGlossaryTerm(String indexElement, Set<SKOSConcept> concepts){
		
		try {
			
			String glossaryTerm = Persistence.getInstance().getConceptOfIndexElement(indexElement, bookID);
		
			if(glossaryTerm != null){
				
				SKOSConcept currentConcept;
				
				 glossaryTerm = Configuration.getInstance().getOntologyNS()+glossaryTerm;
				 
				 URI glossaryURI = URI.create(glossaryTerm.replaceAll(" ", "+"));
				 
				 Iterator iterate = concepts.iterator();
				 
				 while(iterate.hasNext()){
					 
					 currentConcept = (SKOSConcept) iterate.next();
					 
					 if(currentConcept.getURI().equals(glossaryURI))
						 return currentConcept;
				 }				
			}
			
		} catch (SQLException e) {
				e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}
		
		return null;
	}

/**
 * Creates a SKOS:Concept of type 'page' for each physical page of the book
 * 
 * @param bookID
 * @param pageNumbers
 * @param type
 */
	private void createPageResources(String bookID, ArrayList<Integer> pageNumbers, resourceType type){

		this.modelForPages = ModelFactory.createDefaultModel();
		Seq pageSeq = this.modelForPages.createSeq("pageResources");
		int counter= 1;

		SKOSConcept concept;
		SKOSEntityAssertion pageEntity;
		SKOSChange change;
		SKOSObjectRelationAssertion propertyAssertion;

		String pageURI;

		for(int i = 0; i < pageNumbers.size(); i++){

			pageURI    	= this.	basePageURI+String.valueOf(pageNumbers.get(i));

			concept 	= this. df.getSKOSConcept(URI.create(pageURI));

			try {
				//add the "skos:inScheme" to the concept
				propertyAssertion = df.getSKOSObjectRelationAssertion(concept, inScheme, pageScheme);	
				change = new  AddAssertion(this.dataset, propertyAssertion);
				this.manager.applyChange(change);
				
				//add the concept to the SKOS
				pageEntity 	= this. df.getSKOSEntityAssertion(concept);
				change 		= new AddAssertion(this.dataset, pageEntity);
				this.manager.applyChange(change);

			} catch (SKOSChangeException e) {
				e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			}

			this.pageMap.put(pageNumbers.get(i), concept);

			Resource page = this.modelForPages.createResource(pageURI);	
			pageSeq.add(counter, page);
			counter++;
		}
	}

/**
 * 
 * @param manager
 * @param dataSet
 * @param titleData
 * @param isRelated
 * @param titleConcept
 */
	private void enrichTitlesWithPages(SKOSManager manager, SKOSDataset dataSet, NodeDataContainer titleData, 
			SKOSObjectProperty isRelated, SKOSConcept titleConcept ){

		SKOSDataFactory df = manager.getSKOSDataFactory();

		int start = titleData.getPageNumberStart();
		int end = titleData.getPageNumberEnd();

		for(int i = start ; i<=end ; i++){
			if(this.pageMap.containsKey(i)){

				SKOSConcept pageConcept = this.pageMap.get(i);
				SKOSObjectRelationAssertion propertyAssertion = df.getSKOSObjectRelationAssertion(titleConcept,isRelated,pageConcept);
				SKOSChange change = new AddAssertion(dataSet, propertyAssertion);

				try {
					manager.applyChange(change);
				} catch (SKOSChangeException e) {
					e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
				}
			}
		}
	}

	/**
	 * 
	 * @param sequence
	 * @param search
	 * @return
	 */
	private int indexOfURI(Seq sequence, String search){

		for(int i=0; i<sequence.size(); i++){
			if(sequence.getResource(i).getURI().equals(search))
				return i;
		}		
		return -1;
	}

	/**
	 * 
	 * 
	 * @param bookID
	 * @param pageNumbers
	 * @param indexTerms
	 * @throws UnsupportedEncodingException
	 */

	private void SKOSModel(String bookID, ArrayList<Integer> pageNumbers, FormatExtractor parse) throws UnsupportedEncodingException{
		SystemLogger.getInstance().debug("PageNumbers list: " + pageNumbers);
		
		int start = 0;
		while(titles.get(start).isSection())
			start++;
		
		try {
			SKOSConcept conceptTitle = this.df.getSKOSConcept(URI.create(URLEncoder.encode(this.baseURI +titles.get(start).getTitle().replaceAll(" ", "-"), "UTF-8")));

			SKOSEntityAssertion entityAssertion 	= df.getSKOSEntityAssertion(conceptTitle);
			SKOSChange change = new AddAssertion(this.dataset, entityAssertion);
			this.manager.applyChange(change);

			conceptList.add(conceptTitle);

			SKOSObjectRelationAssertion propertyAssertion = df.getSKOSObjectRelationAssertion(conceptTitle, inScheme, bookScheme);	
			change = new  AddAssertion(this.dataset, propertyAssertion);
			this.manager.applyChange(change);


			this.stckConcept.push(conceptTitle);   
			this.stckObject.push(this.titles.get(start));

			for(int i = start + 1 ; i<this.titles.size() ; i++){
				
				int beforeChap = i - 1;
				
				if(titles.get(i).isSection() || titles.get(i).isErratum() ) {
					SystemLogger.getInstance().debug("skipping: " + titles.get(i).getTitle());
					continue;
				}
				if(titles.get(beforeChap).isSection() || titles.get(beforeChap).isErratum()) {
					beforeChap--;
				}

				SKOSConcept concept2;

				conceptTitle =  this.df.getSKOSConcept(URI.create(URLEncoder.encode(this.baseURI +titles.get(i).getTitle().replaceAll(" ", "-"), "UTF-8")));
				entityAssertion = this.df.getSKOSEntityAssertion(conceptTitle);
				propertyAssertion = this.df.getSKOSObjectRelationAssertion(conceptTitle, inScheme, bookScheme);

				conceptList.add(conceptTitle);
				
				change = new AddAssertion(this.dataset, entityAssertion);
				this.manager.applyChange(change);

				change = new AddAssertion(this.dataset, propertyAssertion);
				this.manager.applyChange(change);

				if(this.titles.get(beforeChap).getHierarchyLevel() < this.titles.get(i).getHierarchyLevel()){

					concept2 = stckConcept.peek();

					propertyAssertion = df.getSKOSObjectRelationAssertion(concept2, isBroaderTrans, conceptTitle);
					change = new AddAssertion(dataset, propertyAssertion);
					manager.applyChange(change);

					propertyAssertion = df.getSKOSObjectRelationAssertion(conceptTitle, isNarrowerTrans, concept2);
					change = new AddAssertion(dataset, propertyAssertion);
					manager.applyChange(change);

					stckObject.push(titles.get(i));
					stckConcept.push(conceptTitle); 
				}
				else{	            		

					NodeDataContainer lastPopped = null;
					SKOSConcept lastPoppedConcpt = null;

					while( !stckObject.isEmpty() && stckObject.peek().getHierarchyLevel() >= titles.get(i).getHierarchyLevel()){

						lastPopped = stckObject.pop();
						lastPoppedConcpt = stckConcept.pop();

						SystemLogger.getInstance().debug("titles.get(i): " + titles.get(i).getTitle() );
					
						if(titles.get(i).getHierarchyLevel()==1){

							if(titles.get(i).getPageNumberStart() <= 0) {
								titles.get(i).setPageNumberStart(titles.get(i+1).getPageNumberStart());
							}
							 if(pageNumbers.contains(titles.get(i).getPageNumberStart()-1)) {
								titles.get(titles.indexOf(lastPopped)).setPageNumberEnd(titles.get(i).getPageNumberStart()-1);
								titles.get(titles.indexOf(lastPopped)).setTitleNextNodeHierarchy(titles.get(i).getTitle());
								SystemLogger.getInstance().debug("^^ 1 | " + titles.get(titles.indexOf(lastPopped)).getTitle()  + " EP: " + (titles.get(i).getPageNumberStart()-1) + " EndLine: " + titles.get(i).getTitle());
							}else {
								int check = titles.get(i).getPageNumberStart()-2;
								while(!pageNumbers.contains(check) && check >= 0) {
									check--;
								}
								titles.get(titles.indexOf(lastPopped)).setPageNumberEnd(check);
								titles.get(titles.indexOf(lastPopped)).setTitleNextNodeHierarchy(titles.get(i).getTitle());
								SystemLogger.getInstance().debug("^^ 2 | " + titles.get(titles.indexOf(lastPopped)).getTitle()  + " EP: " + (check) + " EndLine: " + titles.get(i).getTitle());
							}
								
						}
						else {
							titles.get(titles.indexOf(lastPopped)).setPageNumberEnd(titles.get(i).getPageNumberStart());
							titles.get(titles.indexOf(lastPopped)).setTitleNextNodeHierarchy(titles.get(i).getTitle());
							SystemLogger.getInstance().debug("^^ 3 | " + titles.get(titles.indexOf(lastPopped)).getTitle()  + " EP: " + (titles.get(i).getPageNumberStart()) + " EndLine: " + titles.get(i).getTitle());
						}


						enrichTitlesWithPages(manager, dataset, titles.get(titles.indexOf(lastPopped)), isRelated, lastPoppedConcpt);
					}

					if(!stckObject.isEmpty() ){
						concept2 = stckConcept.peek();

						propertyAssertion = df.getSKOSObjectRelationAssertion(concept2, isBroaderTrans, conceptTitle);
						change = new AddAssertion(dataset, propertyAssertion);
						manager.applyChange(change);

						propertyAssertion = df.getSKOSObjectRelationAssertion(conceptTitle, isNarrowerTrans, concept2);
						change = new AddAssertion(dataset, propertyAssertion);
						manager.applyChange(change);

					}

					stckObject.push(titles.get(i));
					stckConcept.push(conceptTitle); 
				}
			}
			
			//check if after the last subchapter there are no more first level chapters: 
			//  there are more than 1 elements in the stack
			if(stckObject.size() > 1) {
				
				List<ResourceUnit> book = parse.getPagesAsResourceUnits();
				
				int pageEnd = book.get(book.size()-1).getPageNumber();
				for(int i = 0; i < stckObject.size(); i++) {
					NodeDataContainer temp = stckObject.get(i);
					titles.get(titles.indexOf(temp)).setPageNumberEnd(pageEnd);
				}
			}
			

			// finally save the dataset to a file in RDF/XML format
			cm.setSKOSModel(bookID, manager);

		} catch (SKOSChangeException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());   //To change body of catch statement use File | Settings | File Templates.
		} 
	}

	/**
	 * 
	 * @param ID
	 * @param isParapgraph
	 * @param positionIndex
	 */
	public void addSegmentToSKOSModel(int ID, boolean isParapgraph, int positionIndex){
		
		if(positionIndex >= conceptList.size()) {
			SystemLogger.getInstance().debug(">> skipping");
			return;
		}
		
		SKOSConcept 				concept;
		SKOSObjectRelationAssertion propertyAssertion;
		SKOSChange					change;
		
		String						baseSegmentURI	= this.baseSegmentURI + String.valueOf(ID);
		
		concept = df.getSKOSConcept(URI.create(baseSegmentURI.replaceAll(" ", "-")));
		
		if(isParapgraph)			
			propertyAssertion = df.getSKOSObjectRelationAssertion(concept, inScheme, paragraphScheme);	
		else
			propertyAssertion = df.getSKOSObjectRelationAssertion(concept, inScheme, chapterScheme);	
				
		change = new  AddAssertion(dataset, propertyAssertion);
		
		try {
			
			manager.applyChange(change);
			
			SystemLogger.getInstance().debug("positionIndex: " + positionIndex);
			SystemLogger.getInstance().debug("conceptList: " + conceptList.size());
			propertyAssertion = df.getSKOSObjectRelationAssertion(concept, isRelated, conceptList.get(positionIndex));
			change = new  AddAssertion(dataset, propertyAssertion);
			manager.applyChange(change);
			
			propertyAssertion = df.getSKOSObjectRelationAssertion(conceptList.get(positionIndex), isRelated, concept);
			change = new  AddAssertion(dataset, propertyAssertion);
			manager.applyChange(change);
			
		} catch (SKOSChangeException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}
	}
	
	
/**
 * 
 * @param title
 * @param level
 */
	public void add(String title,int pageNumber, int level, int section, boolean isErratum){

		NodeDataContainer temp = new NodeDataContainer();

		temp.setHierarchyLevel(level);
		temp.setTitle(title);
		temp.setPageNumberStart(pageNumber);
		temp.setSectionNumber(section);
		temp.setErratum(isErratum);
		
		titles.add(temp);
	}
	
	public void addSection(String sectionTitle) {
		sections.add(sectionTitle);
		NodeDataContainer temp = new NodeDataContainer();

		temp.setHierarchyLevel(0);
		temp.setTitle(sectionTitle);
		temp.setPageNumberStart(-1);
		temp.setSectionNumber(-1);
		temp.setSection(true);
		
		titles.add(temp);
	}
	
	public List<String> getSections() {
		return this.sections;
	}


/**
 * 
 * @return
 */
	public int deepestRank(){

		int depth = 0;

		for(int i=0 ; i<titles.size(); i++)
			if(titles.get(i).getHierarchyLevel() > depth)
				depth = titles.get(i).getHierarchyLevel();
		
		return depth;
	}
	
	
	public SKOSConceptScheme getChapterScheme() {
		return chapterScheme;
	}

	public SKOSConceptScheme getParagraphScheme() {
		return paragraphScheme;
	}
	
	public SKOSManager getManager(){
		return manager;
	}
	
	public SKOSDataFactory getDf() {
		return df;
	}

	public SKOSInSchemeProperty getInScheme() {
		return inScheme;
	}

	public SKOSDataset gettDataset() {
		return dataset;
	}
	
	public String getBaseSegmentURI() {
		return baseSegmentURI;
	}

	public int getHierarchyLevel(int i){
		return this.titles.get(i).getHierarchyLevel();
	}

	public List<NodeDataContainer> getHierarchyList(){
		return this.titles;
	}

	public NodeDataContainer getEntryAt(int i){

		return titles.get(i);
	}
	
	public static void main(String args[]) {
		
	}
}
