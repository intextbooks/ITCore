package intextbooks.tools.utility;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.semanticweb.skos.RemoveAssertion;
import org.semanticweb.skos.SKOSChange;
import org.semanticweb.skos.SKOSChangeException;
import org.semanticweb.skos.SKOSConcept;
import org.semanticweb.skos.SKOSConceptScheme;
import org.semanticweb.skos.SKOSCreationException;
import org.semanticweb.skos.SKOSDataProperty;
import org.semanticweb.skos.SKOSDataRelationAssertion;
import org.semanticweb.skos.SKOSDataset;
import org.semanticweb.skos.SKOSObjectProperty;
import org.semanticweb.skos.SKOSObjectRelationAssertion;
import org.semanticweb.skos.properties.SKOSDefinitionDataProperty;
import org.semanticweb.skos.properties.SKOSRelatedProperty;
import org.semanticweb.skosapibinding.SKOSManager;

public class OntologyUtils {

	public static String getConceptName(URI uri) throws UnsupportedEncodingException {

		String name = uri.toString();
		name = URLDecoder.decode(name, "UTF-8").trim();
		name = name.substring(name.lastIndexOf('#') + 1, name.length());

		return name;

	}
	
	public static SKOSDataset stripEnrichments(SKOSDataset ontology) throws SKOSCreationException, SKOSChangeException {
		
		SKOSManager man = new SKOSManager();
		List<SKOSChange> changeList = new ArrayList<SKOSChange>();
		
		for (SKOSConceptScheme scheme : ontology.getSKOSConceptSchemes()) {

			for (SKOSConcept concept : scheme.getConceptsInScheme(ontology)) {
				
				Set<SKOSDataRelationAssertion> dataSet = concept.getDataRelationAssertions(ontology);
				
				for (SKOSDataRelationAssertion ass : dataSet) {
					
					SKOSDataProperty prop = ass.getSKOSProperty();
					
//					if (prop instanceof SKOSDefinitionDataProperty) {
					if (prop.getURI().toString().contains("definition")) {
						changeList.add(new RemoveAssertion(ontology, ass));
					}
					
					if (prop.getURI().toString().equals("http://purl.org/dc/terms/source")) {
						changeList.add(new RemoveAssertion(ontology, ass));
					}
					
				}

				Set<SKOSObjectRelationAssertion> objectSet = concept.getObjectRelationAssertions(ontology);
				
				for (SKOSObjectRelationAssertion ass : objectSet) {
					
					SKOSObjectProperty prop = ass.getSKOSProperty();
					
//					if (prop instanceof SKOSRelatedProperty) {
					if (prop.getURI().toString().contains("related")) {
						changeList.add(new RemoveAssertion(ontology, ass));
					}
					
				}
				
			}
			
		}
		
		man.applyChanges(changeList);
		
		return ontology;
		
	}

}
