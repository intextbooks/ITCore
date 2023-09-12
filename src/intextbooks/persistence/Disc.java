package intextbooks.persistence;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.multipdf.PageExtractor;
import org.semanticweb.skos.SKOSCreationException;
import org.semanticweb.skos.SKOSDataset;
import org.semanticweb.skos.SKOSStorageException;
import org.semanticweb.skosapibinding.SKOSFormatExt;
import org.semanticweb.skosapibinding.SKOSManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import intextbooks.Configuration;
import intextbooks.SystemLogger;
import intextbooks.content.ContentManager;
import intextbooks.content.enrichment.EnrichedModelBuilder;
import intextbooks.content.models.formatting.CoordinatesContainer;
import intextbooks.content.models.formatting.FormattingContainer;
import intextbooks.content.models.formatting.FormattingDictionary;
import intextbooks.content.models.formatting.Page;
import intextbooks.content.models.formatting.PageMetadataEnum;
import intextbooks.content.models.structure.CoordinatesSegment;
import intextbooks.content.models.structure.Segment;
import intextbooks.ontologie.LanguageEnum;


public class Disc {

	private static Disc instance = null;
	private static String modelFolder;
	private static SystemLogger logger = SystemLogger.getInstance();
	private static String NS = Configuration.getInstance().getOntologyNS();


	protected Disc() {
		modelFolder = Configuration.getInstance().getModelFolder();

	}

	public static Disc getInstance() {
		if(instance == null) {
			instance = new Disc();
		}
		return instance;
	}



	public void addPageFormatting(String parentBook, String pageIndex,
			ArrayList<ArrayList<Integer>> formatMap, ArrayList<ArrayList<CoordinatesContainer>> coordMap) throws Exception{

		if(formatMap.size() == coordMap.size()){
			for(int i=0; i<formatMap.size();i++){
				ArrayList<Integer> fLine = formatMap.get(i);

				ArrayList<CoordinatesContainer> cLine = coordMap.get(i);

				if(fLine.size() == cLine.size()){
					for(int j=0; j<fLine.size();j++){

						//we have to increment line and word position (i and j) since the PDF starts counting at 1
						Database.getInstance().addFormatEntry(parentBook, pageIndex,i+1, j+1, fLine.get(j), cLine.get(j));
					}
				}else
					throw new Exception("Creating map entries failed");
			}
		}else
			throw new Exception("Creating map entries failed");



	}





	public void storeFormatModel(String parentBook, HashMap<String, Page> pageMap, FormattingDictionary dict) {

		Document formatDictXML = dictAsXML(parentBook, dict);
		Document pageMapXML = pageMapAsXML(parentBook, pageMap);

		try {
			writeFile2Disk(Configuration.getInstance().getModelFolder()+parentBook+File.separator, "dict.xml", formatDictXML);
			writeFile2Disk(Configuration.getInstance().getModelFolder()+parentBook+File.separator, "pageMap.xml", pageMapXML);
		} catch (TransformerException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}
	}
	
	public void storeEnrichedModel(String parentBook, SKOSDataset model) {
		String folderPath = Configuration.getInstance().getModelFolder()+parentBook+File.separator;
		checkFolderStructure(folderPath);
		URI ontURI = EnrichedModelBuilder.generateURI(folderPath, "EnrichedModel-DBpedia.xml", false);

		try {
			SKOSManager man = new SKOSManager();
			man.save(model, SKOSFormatExt.RDFXML, ontURI);
			File file = new File (Configuration.getInstance().getModelFolder()+parentBook+File.separator, "EnrichedModel-DBpedia.xml");
			file.setReadable(true, false);
			file.setExecutable(true, false);
		} catch (SKOSCreationException | SKOSStorageException e) {
			e.printStackTrace();
		}
	}
	
	public void storeTEIModel(String parentBook, Document document) {
		String folderPath = Configuration.getInstance().getModelFolder()+parentBook+File.separator;
		checkFolderStructure(folderPath);
		
		try {
			// create the xml file
			//transform the DOM Object to an XML File
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.VERSION, "1.1");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			DOMSource domSource = new DOMSource(document);
			File file = new File (Configuration.getInstance().getModelFolder()+parentBook+File.separator, "teiModel.xml");
			
			StreamResult streamResult = new StreamResult(file);
			transformer.transform(domSource, streamResult);
			file = new File (Configuration.getInstance().getModelFolder()+parentBook+File.separator, "teiModel.xml");
			file.setReadable(true, false);
			file.setExecutable(true, false);	
		} catch (Exception e) {
			e.printStackTrace();
			SystemLogger.getInstance().log("Error persisting TEI model: " + e.getMessage());
		}
	}
	
	public void grantExecutionRights(String filePath) {
		File file = new File (filePath);
		file.setReadable(true, false);
		file.setExecutable(true, false);	
	}

	private Document pageMapAsXML(String parentBook, HashMap<String, Page> pageMap) {

		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder;

			docBuilder = docFactory.newDocumentBuilder();

			Document mapXML = docBuilder.newDocument();
			Element rootElement = mapXML.createElementNS(NS,"IL:pageMap");
			rootElement.setAttribute("book", parentBook);
			mapXML.appendChild(rootElement);

			//prepare pages
			Iterator pageIter = pageMap.entrySet().iterator();
		    while (pageIter.hasNext()) {
		        Map.Entry pairs = (Map.Entry)pageIter.next();
		        Page currentPage = (Page) pairs.getValue();
		        Element pageElement = mapXML.createElementNS(NS,"IL:page");

		        pageElement.setAttribute("id", currentPage.getMetadata(PageMetadataEnum.PageIndex));
		        pageElement.setAttribute("lc", String.valueOf(currentPage.getLineCount()));
		        //System.out.println("writing page with index " + currentPage.getMetadata(PageMetadataEnum.PageIndex) + " to pageMap");
		        pageMetadataAsXML(currentPage, mapXML, pageElement);
		        rootElement.appendChild(pageElement);
		    }

			return mapXML;

		} catch (ParserConfigurationException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}

		return null;
	}


	private void pageMetadataAsXML(Page page, Document doc, Element pageElement){

		HashMap<String,String> metadata = page.getMetadataAsMap();

		 Element lineCountNode = doc.createElementNS(NS,"IL:meta");
		 lineCountNode.setAttribute("name", "LineCount");
	     lineCountNode.setAttribute("value", String.valueOf(page.getLineCount()));
	     pageElement.appendChild(lineCountNode);


		Iterator iter = metadata.entrySet().iterator();
	    while (iter.hasNext()) {
	        Map.Entry<String,String> pairs = (Map.Entry)iter.next();
	        Element meta = doc.createElementNS(NS,"IL:meta");
	        meta.setAttribute("name" , pairs.getKey());
	        meta.setAttribute("value", pairs.getValue());

	        pageElement.appendChild(meta);

	    }

	}






	private Document dictAsXML(String parentBook, FormattingDictionary dict) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder;

			docBuilder = docFactory.newDocumentBuilder();

			Document dictXML = docBuilder.newDocument();
			Element rootElement = dictXML.createElementNS(NS,"IL:formatDictionary");
			rootElement.setAttribute("book", parentBook);
			dictXML.appendChild(rootElement);

			ArrayList<Integer> formatKeys = dict.getAllFormatKeys();
			Iterator<Integer> iter = formatKeys.iterator();

			while(iter.hasNext()){
				Element containerElement = dictXML.createElementNS(NS,"IL:formatting");
				Integer keySum = iter.next();
				containerElement.setAttribute("keySum", keySum.toString());

				Element fontElement = dictXML.createElementNS(NS,"IL:font");
				fontElement.setAttribute("family", dict.getFontFamily(keySum));
				fontElement.setAttribute("size", String.valueOf(dict.getFontSize(keySum)));
				containerElement.appendChild(fontElement);

				Element faceElement = dictXML.createElementNS(NS,"IL:face");
				faceElement.setAttribute("italic",  Boolean.toString(dict.getItalic(keySum)));
				faceElement.setAttribute("bold",  Boolean.toString(dict.getBold(keySum)));
				containerElement.appendChild(faceElement);

				Element indentationElement = dictXML.createElementNS(NS, "IL:indentation");
				indentationElement.setAttribute("value", String.valueOf(dict.getIndentation(keySum)));
				containerElement.appendChild(indentationElement);

				rootElement.appendChild(containerElement);

			}

			return dictXML;

		} catch (ParserConfigurationException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}

		return null;

	}


	private void writeFile2Disk(String folder, String fileName, Document xml) throws TransformerException{

		File dataDir = checkFolderStructure(folder);

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		
		DOMSource source = new DOMSource(xml);
		File file = new File(dataDir,fileName);
		StreamResult result = new StreamResult(file);
		transformer.transform(source, result);
		file.setReadable(true, false);
		file.setExecutable(true, false);

	}

	public File checkFolder(String path){
		return this.checkFolderStructure(path);
	}

	public File checkFolderStructure(String path){
		String currentPath="";
		if(path.charAt(0) != '/')
			path = System.getProperty("user.dir")+"/"+ path;
		String[] pathSegments = path.split("/");

		if(pathSegments.length > 0){
			File dataDir = new File("");
			for(int iter=0; iter<pathSegments.length;iter++){
				currentPath = currentPath+pathSegments[iter]+File.separator;
				dataDir = new File(currentPath);

				if(!dataDir.exists()){
						dataDir.mkdir();
				};
				dataDir.setReadable(true, false);
				dataDir.setExecutable(true, false);
			}
			return dataDir;
		}else{
			return null;
		}
	}

	public void removeBook(String bookID) throws IOException{
		String modelData = Configuration.getInstance().getModelFolder()+bookID+File.separator;
		String PDFsegments = Configuration.getInstance().getExtractedSegmentsFolderPDF()+bookID+File.separator;
		String TXTsegments = Configuration.getInstance().getExtractedSegmentsFolderTXT()+bookID+File.separator;


		FileUtils.deleteDirectory(new File(modelData));
		FileUtils.deleteDirectory(new File(PDFsegments));
		FileUtils.deleteDirectory(new File(TXTsegments));
	}

	/**
	 *
	 * @param filePath : it is obtained from Configuration class
	 * @param optionalFileName : to create a file under the given path.
	 * If not needed use empty String.
	 * @return
	 */

	public URI generateURI(String filePath, String optionalFileName){

		URI fileURI;

		filePath = filePath.replaceAll("\\\\", "/");

		String projectPath = System.getProperty("user.dir").replaceAll("\\\\", "/");

		String firstChar = projectPath.substring(0, 1);

		String lastChar = projectPath.substring(projectPath.length() - 1);

		if (!firstChar.equals("/")) {
			projectPath = "/" + projectPath;
		}

		if (!lastChar.equals("/")) {
			projectPath += "/";
		}

		try {

			//full path
			if(filePath.charAt(0) == '/') {
				fileURI = new URI("file", filePath + optionalFileName, null);
			} else {
			//local path
				fileURI = new URI("file", "//" + projectPath + filePath + optionalFileName, null);
			}
			
			return fileURI;

		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
			return null;
		}

	}

	public void storeStructureModel(String parentBook, Segment hierarchy, int indexCounter, int maxDepth, SKOSManager skosModel){
		Document structureXML = structureAsXML(parentBook, hierarchy, indexCounter, maxDepth);
		try {
			writeFile2Disk(Configuration.getInstance().getModelFolder()+parentBook+File.separator, "structure.xml", structureXML);

			if(skosModel != null && !skosModel.getSKOSDataSets().isEmpty()){
				SystemLogger.getInstance().log("Persisting SKOS model");
				skosModel.save(skosModel.getSKOSDataSets().iterator().next(), SKOSFormatExt.RDFXML,
						generateURI(Configuration.getInstance().getModelFolder()+parentBook+File.separator, "SKOSModel.xml"));
				File file = new File (Configuration.getInstance().getModelFolder()+parentBook+File.separator, "SKOSModel.xml");
				file.setReadable(true, false);
				file.setExecutable(true, false);
			}else{
				SystemLogger.getInstance().log("SKOS model is empty, nothing to persist");
			}

		} catch (TransformerException | SKOSStorageException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
			
		}
	}



	private Document structureAsXML(String parentBook, Segment hierarchy, int indexCounter, int maxDepth) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder;

			docBuilder = docFactory.newDocumentBuilder();
			Document structureXML = docBuilder.newDocument();

			Element rootElement = structureXML.createElementNS(NS, "IL:structure");
					rootElement.setAttribute("maxDepth", String.valueOf(maxDepth));
					rootElement.setAttribute("indexCounter",String.valueOf(indexCounter));


			rootElement.appendChild(createStructureSegments(parentBook, hierarchy,structureXML));

			structureXML.appendChild(rootElement);

			return structureXML;

		} catch (ParserConfigurationException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}

		//in case something went wrong return null
		return null;
	}

	private Element createStructureSegments(String parentBook, Segment segment, Document structureXML) {
			Element segmentElement;

			segmentElement = segment.getId() == 0 ? structureXML.createElementNS(NS,"IL:book") : structureXML.createElementNS(NS, "IL:segment");

			segmentElement.setAttribute("id", String.valueOf(segment.getId()));
			segmentElement.setAttribute("startIndex", String.valueOf(segment.getStartPageIndex()));
			segmentElement.setAttribute("endIndex", String.valueOf(segment.getEndPageIndex()));
			segmentElement.setAttribute("pageNumber", String.valueOf(segment.getPageNumber()));

			segmentElement.setAttribute("parentId", String.valueOf(segment.getParent()));
			segmentElement.setAttribute("level", String.valueOf(segment.getLevel()));
			segmentElement.setAttribute("type", segment.getType());

			if(!segment.getType().equals("book")){
				HashMap<String,Double> coords = segment.getPosition();

				segmentElement.setAttribute("topLeftX", String.valueOf(coords.get("leftTopX")));
				segmentElement.setAttribute("topLeftY",  String.valueOf(coords.get("leftTopY")));

				segmentElement.setAttribute("bottomRightX", String.valueOf(coords.get("rightBottomX")));
				segmentElement.setAttribute("bottomRightY", String.valueOf(coords.get("rightBottomY")));
				segmentElement.setAttribute("title", segment.getTitle());
			}
			
//			if( segment.getContent() == null) {
//				segmentElement.appendChild(structureXML.createTextNode("null"));
//			}else {
//				segmentElement.appendChild(structureXML.createTextNode(segment.getContent()));
//			}
//					
			segmentElement.appendChild(structureXML.createTextNode(segment.getContent()));
			
			if( segment.getContent() == null || segment.getContent().equals("")){
				System.out.println(">>>>>>>>>> " + segment.getId());
				System.out.println("segment.getContent() " + segment.getContent());
			}

			//createPDFs(parentBook, segment.getStartPageIndex(), segment.getEndPageIndex(), segment.getId());

			Iterator<Segment> childIter = segment.getChildren().iterator();

			while(childIter.hasNext())
				segmentElement.appendChild(createStructureSegments(parentBook, childIter.next(),structureXML));

		return segmentElement;
	}



	public FormattingDictionary loadFormatDictionary(String bookID) {

		String formatFilePath = Configuration.getInstance().getModelFolder()+bookID;
		FormattingDictionary newDict = new FormattingDictionary();


		File fXmlFile = new File(formatFilePath+File.separator+"dict.xml");
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			NodeList formattingNodes =  doc.getElementsByTagNameNS(NS,"formatting");
			for(int iter=0;iter<formattingNodes.getLength();iter++){

				Element format = (Element)formattingNodes.item(iter);
				//System.out.println("----" + format.getNodeName());
				newDict.containsFormat(loadFormatting(format));
			}

		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}
		return newDict;
	}

	private FormattingContainer loadFormatting(Element formatting) {
		String fontFamily = formatting.getElementsByTagNameNS(NS,"font").item(0).getAttributes().getNamedItem("family").getNodeValue();
		String fontSize = formatting.getElementsByTagNameNS(NS,"font").item(0).getAttributes().getNamedItem("size").getNodeValue();
		String bold = formatting.getElementsByTagNameNS(NS,"face").item(0).getAttributes().getNamedItem("bold").getNodeValue();
		String italic = formatting.getElementsByTagNameNS(NS,"face").item(0).getAttributes().getNamedItem("italic").getNodeValue();
		String indentation = formatting.getElementsByTagNameNS(NS,"indentation").item(0).getAttributes().getNamedItem("value").getNodeValue();
		float[] fontColorComponents = new float[3];
		fontColorComponents[0] = Float.valueOf(formatting.getElementsByTagNameNS(NS,"fontColor").item(0).getAttributes().getNamedItem("v1").getNodeValue());
		fontColorComponents[1] = Float.valueOf(formatting.getElementsByTagNameNS(NS,"fontColor").item(0).getAttributes().getNamedItem("v2").getNodeValue());
		fontColorComponents[2] = Float.valueOf(formatting.getElementsByTagNameNS(NS,"fontColor").item(0).getAttributes().getNamedItem("v3").getNodeValue());
		

		//System.out.println(fontFamily + " " + fontSize + " " + bold + " " + italic + " " + indentation);


		FormattingContainer fContainer =
				new FormattingContainer(fontFamily, Byte.valueOf(fontSize), Integer.parseInt(indentation), Boolean.parseBoolean(bold), Boolean.parseBoolean(italic), 
						fontColorComponents);

		return fContainer;
	}


	public HashMap<String,Page> loadPageMap(String bookID) throws Exception {
		HashMap<String, Page> pages = new HashMap<String,Page>();
		String formatFilePath = Configuration.getInstance().getModelFolder()+bookID;
		ContentManager cm = ContentManager.getInstance();

		File fXmlFile = new File(formatFilePath+File.separator+"pageMap.xml");
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();
			//System.out.println(doc.getDocumentElement().getNodeName());
			//System.out.println(NS);
			NodeList pageNodes = doc.getElementsByTagNameNS(NS,"page");

			for(int iter=0;iter<pageNodes.getLength();iter++)
				pages.put(pageNodes.item(iter).getAttributes().getNamedItem("id").getNodeValue(),
							loadPageFormat(bookID, (Element) pageNodes.item(iter), pageNodes.item(iter).getAttributes().getNamedItem("id").getNodeValue()));




		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}

		return pages;

	}

	private Page loadPageFormat(String parentBook, Element item, String lineCount) throws Exception {

		NodeList metadataNodes = item.getElementsByTagNameNS(NS,"meta");
		HashMap<PageMetadataEnum,String> metadataMap = new HashMap<PageMetadataEnum,String>();


		for(int iter=0;iter<metadataNodes.getLength();iter++){
			String metaName = metadataNodes.item(iter).getAttributes().getNamedItem("name").getNodeValue();
			String metaValue = metadataNodes.item(iter).getAttributes().getNamedItem("value").getNodeValue();

			metadataMap.put(PageMetadataEnum.valueOf(metaName), metaValue);

		}
		//System.out.println("Loading PageMap of page " + metadataMap.get(PageMetadataEnum.PageIndex));
		NodeList lineNodes = item.getElementsByTagNameNS(NS,"line");

		Page newPage = new Page(parentBook, metadataMap, Integer.parseInt(lineCount));


		return newPage;
	}


	public SKOSManager loadStructureModel(String bookID) {
		SKOSManager skosModel;
		try {
			skosModel = new SKOSManager();
			skosModel.loadDataset(URI.create(modelFolder+bookID));

			return skosModel;
		} catch (SKOSCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
			return null;
		}
	}

	public Segment loadStructure(String bookID) {
		Segment hierarchy = new Segment(0, "book", "book", -1,0);
		String formatFilePath = Configuration.getInstance().getModelFolder()+bookID;

		File fXmlFile = new File(formatFilePath+File.separator+"structure.xml");
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			Element bookNode = (Element) doc.getElementsByTagNameNS(NS,"book").item(0);

			if(bookNode == null)
				return null;
			else
				hierarchy = loadSegments(bookNode);


		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}

		return hierarchy;
	}
	
	public Segment loadTEIStructure(String bookID) {
		Segment hierarchy = new Segment(0, "book", "book", -1,0);
		String formatFilePath = Configuration.getInstance().getModelFolder()+bookID;

		File fXmlFile = new File(formatFilePath+File.separator+"teiModel.xml");
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			Element bookNode = (Element) doc.getElementsByTagName("front").item(0);

			if(bookNode == null)
				return null;
			else {
				bookNode = (Element) bookNode.getElementsByTagName("div").item(0);
				bookNode = (Element) bookNode.getElementsByTagName("list").item(0);
				loadTEISegments(bookNode, hierarchy);
			}
				


		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}

		return hierarchy;
	}

	public File loadParagraphFile(String bookID, int index) {
		String filePath = Configuration.getInstance().getExtractedSegmentsFolderTXT()+bookID;

		return  new File(filePath+File.separator+String.valueOf(index)+".txt");
	}

	public int[] loadCounters(String bookID) {

		String formatFilePath = Configuration.getInstance().getModelFolder()+bookID;

		File fXmlFile = new File(formatFilePath+File.separator+"structure.xml");
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		DocumentBuilder dBuilder;

		int[] counters = new int[2];
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			Element bookNode = (Element) doc.getElementsByTagNameNS(NS,"structure").item(0);

			if(bookNode != null){
				counters[0] = Integer.parseInt(bookNode.getAttribute("maxDepth"));
				counters[1] = Integer.parseInt(bookNode.getAttribute("indexCounter"));
			}

		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}

		return counters;
	}


	private Segment loadSegments(Element node) {
		NodeList children = node.getChildNodes();

		CoordinatesSegment coords;

		if(!node.getAttribute("type").equals("book")){
			 coords = new CoordinatesSegment(Double.parseDouble(node.getAttribute("topLeftX")),
															 Double.parseDouble(node.getAttribute("topLeftY")),
															 Double.parseDouble(node.getAttribute("bottomRightX")),
															 Double.parseDouble(node.getAttribute("bottomRightY")),
															 !node.getAttribute("endIndex").equals(node.getAttribute("startIndex")));
		}else{
			coords = null;
		}


		Segment newSegment = new Segment(Integer.parseInt(node.getAttribute("id")),
															getTextContentOfNode(node),
															node.getAttribute("type"),
															Integer.parseInt(node.getAttribute("parentId")),
															Integer.parseInt(node.getAttribute("level")),
															Integer.parseInt(node.getAttribute("startIndex")),
															Integer.parseInt(node.getAttribute("endIndex")),
															coords,
															Integer.parseInt(node.getAttribute("pageNumber")),
															node.getAttribute("title"));


//		System.out.println(">> new segment added: " + newSegment.getId());
//		System.out.println(">> Title: " + newSegment.getTitle());
//		System.out.println(">> Type: " + newSegment.getType());
//		System.out.println(">> StartPageIndex: " + newSegment.getStartPageIndex());
//		System.out.println(">> EndPageIndex: " + newSegment.getEndPageIndex());
//		try {
//			TimeUnit.SECONDS.sleep(2);
//		} catch (InterruptedException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}

		for(int iter=0;iter<children.getLength();iter++){
			 if (children.item(iter).getNodeType() == Node.ELEMENT_NODE) {
				 Element currentNode = (Element) children.item(iter);

				 newSegment.addChild(loadSegments(currentNode));
			 }
		}

		return newSegment;
	}
	
	private void loadTEISegments(Element node, Segment parent) {

		NodeList list = node.getChildNodes();
		Segment newSegmentC = null;
		for(int i = 0; i < list.getLength(); i++) {
			if(list.item(i).getNodeName().equals("item")) {
				Element current = (Element) list.item(i);
				String allText = current.getTextContent().trim();
				Element ref = (Element) current.getElementsByTagName("ref").item(0);
				String target = ref.getAttribute("target");
				if(target == null || target.length() == 0)
					continue;
				int id =Integer.valueOf(target.replace("seg_", ""));
				String pageString = ref.getTextContent().trim();
				int pageStart = Integer.valueOf(pageString);
				allText = allText.substring(0, allText.length() - pageString.length());
				
				newSegmentC = new Segment(id,allText,"chapter",parent.getId(),parent.getLevel()+1,pageStart,0,null,pageStart,allText);
				Segment newSegmentP = new Segment(id+1,allText,"paragraph",newSegmentC.getId(),newSegmentC.getLevel()+1,pageStart,0,null,pageStart,allText);
				newSegmentC.addChild(newSegmentP);
				parent.addChild(newSegmentC);
				
				//System.out.println("# title: " + allText + " #page: " + pageString + " #seg: " + id);
			} else if (list.item(i).getNodeName().equals("list")) {
				Element current = (Element) list.item(i);
				loadTEISegments(current,newSegmentC);
			}
		}
	}


	private  String getTextContentOfNode(Node node) {
	    NodeList list = node.getChildNodes();
	    StringBuilder textContent = new StringBuilder();
	    for (int i = 0; i < list.getLength(); ++i) {
	        Node child = list.item(i);
	        if (child.getNodeType() == Node.TEXT_NODE)
	            textContent.append(child.getTextContent());
	    }
	    return textContent.toString();
	}

	public void storeSegment(String bookID, int index, String content){
		storeSegment(bookID, String.valueOf(index), content);
	}
	
	public void storeSegment(String bookID, String name, String content){
		String filePath = Configuration.getInstance().getExtractedSegmentsFolderTXT()+bookID;

		File dataDir = checkFolderStructure(filePath);
		File paragraphFile = (new File(dataDir,name+".txt"));
		Writer out;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(paragraphFile), "UTF-8"));
			out.write(content);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}
	}

	public void storeExtractonTempInfo(String bookID, String fileName, String content){
		fileName = fileName.replace("/", "-");
		String filePath = Configuration.getInstance().getExtractionTmpFolder()+bookID;

		File dataDir = checkFolderStructure(filePath);
		File paragraphFile = (new File(dataDir,fileName+".txt"));
		Writer out;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(paragraphFile), "UTF-8"));
			out.write(content);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}
	}
	
	public void storeContentFilePersonalized(String filePath, String fileName, String content){
		fileName = fileName.replace("/", "-");

		File dataDir = checkFolderStructure(filePath);
		File paragraphFile = (new File(dataDir,fileName+".txt"));
		Writer out;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(paragraphFile), "UTF-8"));
			out.write(content);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}
	}

	public String storeContentFile(String fileName, FileItem item) throws IOException {
		String contentFolder = Configuration.getInstance().getContentFolder();

		File dataDir = checkFolderStructure(contentFolder);
		InputStream in = item.getInputStream();

		File file =  (new File(dataDir,fileName));
     	OutputStream out = new FileOutputStream(file);
		IOUtils.copy(in, out);

		return fileName;

	}



	public void performCleanPDFsplit(String parentBook, String parentBookFileName, Segment segment){

		try {

			PDDocument pdf = null;
	    	File pdfFile = new File(Configuration.getInstance().getContentFolder()+File.separator+parentBookFileName);

			pdf = PDDocument.load(pdfFile);
			
			//Create folder to store the PDF segments
			String folder = Configuration.getInstance().getExtractedSegmentsFolderPDF()+"clean"+File.separator+parentBook+File.separator;
			File folderFile = new File(folder);
			if(!folderFile.exists()) {
				folderFile.mkdir();
			}

			performCleanPDFsplitHelper(parentBook, parentBookFileName, segment,pdf);


			pdf.close();
		} catch (IOException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}


	}


	public void performPDFsplit(String parentBook, String parentBookFileName, Segment segment){

		try {


			parentBookFileName = parentBookFileName.replace(".pdf", "");
			parentBookFileName = parentBookFileName + "_highlighted.pdf";

			PDDocument pdf = null;
	    	File pdfFile = new File(Configuration.getInstance().getContentFolder()+File.separator+parentBookFileName);


			pdf = PDDocument.load(pdfFile);


			performPDFsplitHelper(parentBook, parentBookFileName, segment,pdf);

			pdf.close();
		} catch (IOException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}


	}


	public void performCleanPDFsplitHelper(String parentBook, String parentBookFileName, Segment segment, PDDocument pdf) {

		String cleanSegmentPath = Configuration.getInstance().getExtractedSegmentsFolderPDF()+"clean"+File.separator+parentBook+File.separator;
	    checkFolderStructure(cleanSegmentPath);

		if(!segment.getType().equals("book")){
			ArrayList<Integer> leafList = ContentManager.getInstance().getLeaves(parentBook, segment.getId());

			if(leafList.size() > 0)
				createCleanPDFs(parentBookFileName, parentBook, segment.getStartPageIndex(), ContentManager.getInstance().getEndIndexOfSegment(parentBook, leafList.get(0)), segment.getId(),pdf, null,null,null);
			else
				createCleanPDFs(parentBookFileName, parentBook, segment.getStartPageIndex(), segment.getEndPageIndex(), segment.getId(),pdf, null,null,null);
		}

		Iterator<Segment> childIter = segment.getChildren().iterator();


		while(childIter.hasNext())
			performCleanPDFsplitHelper(parentBook, parentBookFileName, childIter.next(),pdf);

	}



	public void performPDFsplitHelper(String parentBook, String parentBookFileName, Segment segment, PDDocument pdf) {
		checkFolderStructure(Configuration.getInstance().getExtractedSegmentsFolderPDF()+parentBook+File.separator);

		String cleanSegmentPath = Configuration.getInstance().getExtractedSegmentsFolderPDF()+File.separator+"clean"+File.separator+parentBook+File.separator;
	    checkFolderStructure(cleanSegmentPath);

		if(!segment.getType().equals("book")){
			ArrayList<Integer> leafList = ContentManager.getInstance().getLeaves(parentBook, segment.getId());

			if(leafList.size() > 0)
				createPDFs(parentBookFileName, parentBook, segment.getStartPageIndex(), ContentManager.getInstance().getEndIndexOfSegment(parentBook, leafList.get(0)), segment.getId(),pdf, null,null,null);
			else
				createPDFs(parentBookFileName, parentBook, segment.getStartPageIndex(), segment.getEndPageIndex(), segment.getId(),pdf, null,null,null);
		}

		Iterator<Segment> childIter = segment.getChildren().iterator();


		while(childIter.hasNext())
			performPDFsplitHelper(parentBook, parentBookFileName, childIter.next(),pdf);

	}





	public String performPDFsplit(String parentBookID, String parentBookFileName, Segment segment, String userName, LanguageEnum userLang, HashMap<String, Boolean> explainPresent){

		try {
			parentBookFileName = parentBookFileName.replace(".pdf", "");
			parentBookFileName = parentBookFileName + "_highlighted.pdf";

			PDDocument pdf = null;
	    	File pdfFile = new File(Configuration.getInstance().getContentFolder()+File.separator+parentBookFileName);

			pdf = PDDocument.load(pdfFile);

			String URL = performPDFsplitHelper(parentBookID, parentBookFileName, segment,userName, userLang, pdf, explainPresent);

			pdf.close();

			return URL;
		} catch (IOException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
			return "";
		}
	}



	public String performPDFsplitHelper(String parentBookID, String parentBookFileName, Segment segment, String userName, LanguageEnum userLang, PDDocument pdf, HashMap<String, Boolean> explainPresent) {

		checkFolderStructure(Configuration.getInstance().getExtractedSegmentsFolderPDF()+userName+File.separator+parentBookID+File.separator);

		if(!segment.getType().equals("book")){

			ArrayList<Integer> leafList = ContentManager.getInstance().getLeaves(parentBookID, segment.getId());

			if(leafList.size() > 0)
				return createPDFs(parentBookFileName, parentBookID, segment.getStartPageIndex(), ContentManager.getInstance().getEndIndexOfSegment(parentBookID, leafList.get(0)), segment.getId(),pdf, userName, userLang, explainPresent).replace("WebContent/", "");
			else
				return createPDFs(parentBookFileName, parentBookID, segment.getStartPageIndex(), segment.getEndPageIndex(), segment.getId(),pdf, userName, userLang, explainPresent).replace("WebContent/", "");

				//return createPDFs(parentBookFileName, parentBookID, segment.getStartPageIndex(), segment.getEndPageIndex(), segment.getId(), pdf, userName).replace("WebContent/", "");
		}else
			return null;
	}

    private String createPDFs(String fileName, String bookId, int startIndex, int endIndex, int segmentNumber, PDDocument pdf, String userName, LanguageEnum userLang,  HashMap<String, Boolean> explainPresent) {

        try {

        	logger.log("performing split for segment " + segmentNumber + " , startIndex "+ startIndex + " and endIndex " + endIndex);

        	PDDocument newDocument = pdf;
			PageExtractor pageExtractor = new PageExtractor(newDocument);
	        pageExtractor.setStartPage(startIndex);
	        pageExtractor.setEndPage(endIndex);
	        newDocument = pageExtractor.extract();

		    String folder = userName == null ? Configuration.getInstance().getExtractedSegmentsFolderPDF()+bookId+File.separator
		    								 : Configuration.getInstance().getExtractedSegmentsFolderPDF()+userName+/*File.separator*/"/"+bookId+"/";
		    newDocument = userName == null ? newDocument : addUserNameToAnnotations(newDocument, userName,userLang,explainPresent);

		    newDocument.save(folder+segmentNumber+".pdf");
		    newDocument.close();

		    File f = new File(folder+segmentNumber+".pdf");

		    while(!f.exists())
		    	f = new File(folder+segmentNumber+".pdf");


		} catch (IOException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}
        String URLstem = Configuration.getInstance().getExtractedSegmentsFolderPDF();
        if(URLstem.indexOf("repository/")>=0)
        	URLstem = URLstem.substring(URLstem.indexOf("repository/"),URLstem.length());

        return URLstem+userName+File.separator+bookId+File.separator+String.valueOf(segmentNumber)+".pdf";
    }

    private String createCleanPDFs(String fileName, String bookId, int startIndex, int endIndex, int segmentNumber, PDDocument pdf, String userName, LanguageEnum userLang,  HashMap<String, Boolean> explainPresent) {

        try {

        	logger.log("performing split for segment " + segmentNumber + " , startIndex "+ startIndex + " and endIndex " + endIndex);

        	PDDocument newDocument = pdf;
			PageExtractor pageExtractor = new PageExtractor(newDocument);
	        pageExtractor.setStartPage(startIndex+1);
	        pageExtractor.setEndPage(endIndex);
	        newDocument = pageExtractor.extract();

		    String folder = userName == null ? Configuration.getInstance().getExtractedSegmentsFolderPDF()+"clean"+File.separator+bookId+File.separator
		    								 : Configuration.getInstance().getExtractedSegmentsFolderPDF()+userName+/*File.separator*/"/"+bookId+"/";

		    newDocument = userName == null ? newDocument : addUserNameToAnnotations(newDocument, userName,userLang,explainPresent);

		    newDocument.save(folder+segmentNumber+".pdf");
		    newDocument.close();

		    File f = new File(folder+segmentNumber+".pdf");

		    while(!f.exists())
		    	f = new File(folder+segmentNumber+".pdf");


		} catch (IOException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}
        String URLstem = Configuration.getInstance().getExtractedSegmentsFolderPDF()+"clean"+File.separator;
        if(URLstem.indexOf("repository/")>=0)
        	URLstem = URLstem.substring(URLstem.indexOf("repository/"),URLstem.length());

        return URLstem+userName+File.separator+bookId+File.separator+String.valueOf(segmentNumber)+".pdf";
    }



    public void removePersonalizedPDF(String userName, String parentBookID) {
    	try {
    		File deleteMe = checkFolderStructure(Configuration.getInstance().getExtractedSegmentsFolderPDF()+userName+File.separator+parentBookID+File.separator);
			FileUtils.deleteDirectory(deleteMe);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    private PDDocument addUserNameToAnnotations(PDDocument pdf, String userName, LanguageEnum userLang, HashMap<String, Boolean> explainPresent){
    	//System.out.println("Disc > into addUserNameToAnnotations");
    	try {
			 Iterator<PDPage> pageIter =  pdf.getPages().iterator();
			 while(pageIter.hasNext()){
				 PDPage currentPage = pageIter.next();
				 Iterator<PDAnnotation> annoIter;
				 annoIter = currentPage.getAnnotations().iterator();
				 while(annoIter.hasNext()){
					 PDAnnotation currentAnno = annoIter.next();
					 if(currentAnno.toString().contains("PDAnnotationLink")){
						 PDAnnotationLink pda = (PDAnnotationLink) currentAnno;
						 PDActionJavaScript pdaj = (PDActionJavaScript) pda.getAction();
						 String annoStr = pdaj.getAction();
						 annoStr = annoStr.replaceAll("#user#", userName);

						String[] splitted =  annoStr.split("&");

						for(byte i=0; i<splitted.length; i++){

							if(splitted[i].contains("indexTerm")){

								int position = splitted[i].indexOf("=");

								String term = splitted[i].substring(position+1);

								if(explainPresent!=null && !explainPresent.isEmpty()){

									if( explainPresent.containsKey(term) &&  !explainPresent.get(term)){
										annoStr = annoStr.replaceFirst("'#Explain#',bEnabled:true", "'#Explain#',bEnabled:false");
									}
									else{
										annoStr = annoStr.replaceFirst("'#Explain#',bEnabled:false", "'#Explain#',bEnabled:true");
									}
								}
								else{
									annoStr = annoStr.replaceFirst("'#Explain#',bEnabled:true", "'#Explain#',bEnabled:false") ;
								}



								/*if(explainPresent!=null && !explainPresent.isEmpty()
										&& explainPresent.containsKey(term) &&
										!explainPresent.get(term))
									annoStr = annoStr.replaceFirst("'#Explain#',bEnabled:true", "'#Explain#',bEnabled:false");
								else
									annoStr = annoStr.replaceFirst("'#Explain#',bEnabled:false", "'#Explain#',bEnabled:true");*/

									break;
								}

							}

						 String Translate= "Translate",Explain ="Explain",Assess = "Assess";

						 if(userLang.equals(LanguageEnum.ENGLISH)){

							 Translate = "Translate";
							 Explain = "Explain";
							 Assess = "Assess";
						 }
						 else if(userLang.equals(LanguageEnum.FRENCH)){
							 Translate = "Traduire";
							 Explain = "Expliquer";
							 Assess = "évaluer";
						 }
						 else if(userLang.equals(LanguageEnum.GERMAN)){
							 Translate = "Uebersetzen";
							 Explain = "Erklaeren";
							 Assess = "Einstufung";

						 }

						 annoStr = annoStr.replaceAll("#Translate#", Translate);
						 annoStr = annoStr.replaceAll("#Assesment#", Assess);
						 annoStr = annoStr.replaceAll("#Explain#", Explain);

						 pdaj.setAction(annoStr);
					 }

			   }
			 }

		} catch (IOException e) {
			e.printStackTrace();
		}
	return pdf;
    }

	public Element getStructureXML(String bookID) {
		String formatFilePath = Configuration.getInstance().getModelFolder()+bookID;

		File fXmlFile = new File(formatFilePath+File.separator+"structure.xml");
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			return doc.getDocumentElement();

		} catch (IOException | ParserConfigurationException | SAXException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}

		return null;
	}

	public void clearUserPDFfolder(String userName) {

		try {
			File deleteMe = checkFolderStructure(Configuration.getInstance().getExtractedSegmentsFolderPDF()+userName+File.separator);
			FileUtils.deleteDirectory(deleteMe);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public void storePages(String bookID, List<intextbooks.content.extraction.buildingBlocks.format.Page> pages) {
		String dir = "repository/content/pages/" + bookID + "/";
		File dirFile = new File(dir);
		if(dirFile.mkdirs() || dirFile.exists()) {
			
			for(int i = 0; i < pages.size(); i++) {
				intextbooks.content.extraction.buildingBlocks.format.Page p = pages.get(i);
				if(p == null) {
					continue;
				}
				String text = p.getContinuousPageText();
				try {
					Files.write(Paths.get(dir + i + ".txt"), text.getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("f");
		}
		
		
	}





}
