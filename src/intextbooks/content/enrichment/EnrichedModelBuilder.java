package intextbooks.content.enrichment;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
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
import org.semanticweb.skos.SKOSObjectRelationAssertion;
import org.semanticweb.skos.SKOSStorageException;
import org.semanticweb.skosapibinding.SKOSFormatExt;
import org.semanticweb.skosapibinding.SKOSManager;

import intextbooks.Configuration;
import intextbooks.SystemLogger;
import intextbooks.ontologie.LanguageEnum;
import intextbooks.persistence.Disc;
import intextbooks.persistence.Persistence;
import intextbooks.tools.utility.StringUtils;

public class EnrichedModelBuilder {

	public String bookID;
	LanguageEnum lang;
	public SKOSDataset model;
	Map<String, Candidate> finalResourcesForConcepts;
	private static SystemLogger logger = SystemLogger.getInstance();
	private static String baseURI = Configuration.getInstance().getOntologyNS();

	public EnrichedModelBuilder(String bookID, LanguageEnum lang, Map<String, Candidate> finalResourcesForConcepts) {
		this.bookID = bookID;
		this.lang = lang;
		this.finalResourcesForConcepts = finalResourcesForConcepts;
		
	}
	
	public static SKOSDataset createModel(LanguageEnum lang, Map<String, Candidate> finalResourcesForConcepts)  {
		SKOSManager man;
		SKOSDataFactory factory;
		SKOSDataset vocab;
		try {
			man = new SKOSManager();
			factory = man.getSKOSDataFactory();
			vocab = man.createSKOSDataset(URI.create(baseURI.substring(0, baseURI.length()-2)));
			
			SKOSConceptScheme scheme = factory.getSKOSConceptScheme(URI.create(baseURI + "glossary"));
			SKOSEntityAssertion schemaAssertion = factory.getSKOSEntityAssertion(scheme);

			List<SKOSChange> changeList = new ArrayList<SKOSChange>();
			changeList.add(new AddAssertion(vocab, schemaAssertion));

			for (String conceptName: finalResourcesForConcepts.keySet()) {

				String suffix = "";
				try {
					suffix = URLEncoder.encode(conceptName.trim(), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
				}
				SKOSConcept concept = factory.getSKOSConcept(URI.create(baseURI + suffix));
				SKOSObjectRelationAssertion inScheme = factory.getSKOSObjectRelationAssertion(concept, factory.getSKOSInSchemeProperty(),scheme);

				changeList.add(new AddAssertion(vocab, inScheme));

				String isoCode = lang.getShortendLanguageCode();
				
				String norm = StringUtils.normalizeWhitespace(conceptName);
				SKOSAnnotation prefLabel = factory.getSKOSAnnotation(factory.getSKOSPrefLabelProperty().getURI(), norm, isoCode);
				SKOSAnnotationAssertion prefAssertion = factory.getSKOSAnnotationAssertion(concept, prefLabel);
				changeList.add(new AddAssertion(vocab, prefAssertion));
			}

			try {

				man.applyChanges(changeList);
				return vocab;

			} catch (SKOSChangeException e) {
				e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 	
			}
			
		} catch (SKOSCreationException e1) {
			
			SystemLogger.getInstance().log(e1.getLocalizedMessage());
		}	
		
		return null;
	}
	
	
	/*public void saveModel() {
		String folderPath = Configuration.getInstance().getModelFolder()+bookID+File.separator;
		File folder = Disc.getInstance().checkFolderStructure(folderPath);
		System.out.print(folder.exists());
		URI ontURI = this.generateURI(folderPath, "EnrichedModel(DBpedia).xml", false);

		try {
			SKOSManager man = new SKOSManager();
			man.save(this.model, SKOSFormatExt.RDFXML, ontURI);
		} catch (SKOSCreationException | SKOSStorageException e) {
			e.printStackTrace();
		}
	}*/

	/**
	 *
	 * @param filePath
	 *            : it is obtained from Configuration class
	 * @param optionalFileName
	 *            : to create a file under the given path. If not needed use
	 *            empty String.
	 * @return
	 */

	public static URI generateURI(String filePath, String optionalFileName, boolean prependSlash) {
		

		URI fileURI;
		filePath = filePath.replaceAll("\\\\", "/");
		
		//full path
		if(filePath.charAt(0) == '/') {
			String os = System.getProperty("os.name").toLowerCase();
			
			try {
				if (os.contains("win")) {
					fileURI = new URI("file", "//"  + filePath + optionalFileName, null);
				} else {			
					fileURI = new URI("file",  filePath + optionalFileName, null);
				}
			} catch (URISyntaxException e) {
				e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
				return null;
			}
			
			return fileURI;
		} else {
		//local path

			String projectPath = System.getProperty("user.dir").replaceAll("\\\\", "/");

			String lastChar = projectPath.substring(projectPath.length() - 1);

			if (prependSlash) {
				projectPath = "/" + projectPath;
			}

			if (!lastChar.equals("/")) {
				projectPath += "/";
			}

			try {

				String os = System.getProperty("os.name").toLowerCase();
				
				if (os.contains("win")) {
					fileURI = new URI("file", "///" + projectPath + filePath + optionalFileName, null);
				} else {			
					fileURI = new URI("file", "//" + projectPath + filePath + optionalFileName, null);
				}
				
				return fileURI;

			} catch (URISyntaxException e) {
				e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
				return null;
			}
		}
	}
	public static void main (String agrs[]) {
		Map<String, Candidate> finalResourcesForConcepts = new HashMap<String, Candidate>();
		Candidate tmp = new Candidate();
		tmp.setURI("www.google.com");
		finalResourcesForConcepts.put("concept one", tmp);
		tmp.setURI("www.google2.com");
		finalResourcesForConcepts.put("concept two", tmp);
		
		SKOSDataset model = EnrichedModelBuilder.createModel(LanguageEnum.ENGLISH, finalResourcesForConcepts);
		Persistence.getInstance().storeEnrichedModel("test2", model);
	
	}
}
