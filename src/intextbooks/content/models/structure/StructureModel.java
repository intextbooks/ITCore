package intextbooks.content.models.structure;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.semanticweb.skosapibinding.SKOSManager;
import org.w3c.dom.Document;

import intextbooks.SystemLogger;
import intextbooks.ontologie.LanguageEnum;
import intextbooks.persistence.Persistence;

public class StructureModel {
	private String parentBookID;
	private String parentBookFileName;
	private int indexCounter;
	private Segment hierarchy;
	private SKOSManager model;
	private Document teiModel;
	private IndexMap indices;
	private int bookDepth;
	
	/**
	 * @param parentBook
	 */
	public StructureModel(String parentBook, String fileName) {
		System.out.println("====================new StrcutureModel=========================");
		this.parentBookID = parentBook;
		this.parentBookFileName = fileName;
		this.hierarchy = new Segment(0, "book", "book", -1,0);
		
		this.indexCounter = 1;
		this.indices = new IndexMap(parentBook);
	}

	public void loadModel(String bookID) {
		System.out.println("====================structureModel.loadModel=========================");
		Persistence persi = Persistence.getInstance();
		this.parentBookID = bookID;
		this.parentBookFileName = persi.getFileName(bookID);
		
		if(this.parentBookFileName != null){
			System.out.println("====================before persi.loadStructure=========================");
//			try {
//				SystemLogger.getInstance().log("Waiting 1 minutes");
//				TimeUnit.MINUTES.sleep(1);
//			} catch (InterruptedException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
			
			this.hierarchy = persi.loadStructure(bookID);
			
			this.indices = new IndexMap(bookID);
			
			int[] counter = persi.loadCounters(bookID);
			this.bookDepth = counter[0];
			this.indexCounter = counter[1];
			
		}else
			SystemLogger.getInstance().log("Unable to find book:  " + bookID + " in content table");
	}
	

	public int getIndexCounter(){
		
		return this.indexCounter;
	}
	
	public int addChapter(Integer parentIndex, Integer after, String reference, int pageNumber, int startPage, int endPage, 
			double coordLB, double coordLT, double coordRB, double coordRT, boolean multiplePages, String title) {
		
		return addSegment(parentIndex, after, reference, pageNumber, "chapter", startPage, endPage, null, new CoordinatesSegment(coordLB, coordLT, coordRB, coordRT, multiplePages), reference, false);
	}

	public int addParagraph(Integer parentIndex, Integer after, String reference, int pageNumber, int startPage, int endPage, String fileContent,
			double coordLB, double coordLT, double coordRB, double coordRT, boolean multiplePages, String title, boolean splitTextbook) {
		return addSegment(parentIndex, after, reference, pageNumber, "paragraph", startPage, endPage,fileContent, new CoordinatesSegment(coordLB, coordLT, coordRB, coordRT, multiplePages), title, splitTextbook);
	}
	
	public ArrayList<Integer> getSiblingSegments(int parentIndex) {
		Segment segParent = parentIndex == 0 ? this.hierarchy : findSegment(parentIndex, this.hierarchy.getChildren(), null);
		
		ArrayList<Integer> descendingSegs = new ArrayList<Integer>();
		if(segParent != null) {
			ArrayList<Segment> level = segParent.getChildren();
			Iterator<Segment> iter = level.iterator();
			
			while(iter.hasNext()){
				Segment seg = iter.next();
				//System.out.println("seg: " + seg.getId());
				if(seg.getChildren() != null && seg.getChildren().size() > 0) {
					descendingSegs.add(seg.getId());
					descendingSegs.add(seg.getChildren().get(0).getId());
					//System.out.println("\tchildren 0: " + seg.getChildren().get(0).getId());
				}
				
			}
		}
		
		return descendingSegs;
	}
	
	
	public ArrayList<Integer> getDescendingSegments(int parentIndex) {
		Segment segParent = parentIndex == 0 ? this.hierarchy : findSegment(parentIndex, this.hierarchy.getChildren(), null);
		
		ArrayList<Integer> descendingSegs = new ArrayList<Integer>();
		if(segParent != null)
			getDescendingSegments(descendingSegs, segParent.getChildren());
		
		return descendingSegs;
	}
	
	private void getDescendingSegments(ArrayList<Integer> desc,  ArrayList<Segment> level) {
		Iterator<Segment> iter = level.iterator();
		
		while(iter.hasNext()){
			Segment seg = iter.next();
			desc.add(seg.getId());
			getDescendingSegments(desc, seg.getChildren());
		}
	}
	
	public ArrayList<Integer> getSubChapters(Integer parentIndex) {
		
		ArrayList<Integer> subChapters = new ArrayList<Integer>();
		ArrayList<Integer> descSegs = getDescendingSegments(parentIndex);
		
		for (int id : descSegs) {
			
			boolean pararagraphChilds = true;
			for (int childId : getChildrenOfChapter(id))
				if (!isSegmentParagraph(childId)) {
					pararagraphChilds = false;
					break;
				}
			
			if (pararagraphChilds && getSegmentType(id).equals("chapter"))
				subChapters.add(id);
			
		}
		
		return subChapters;
		
	}
	
	public ArrayList<Integer> getChildrenOfChapter(Integer parentIndex){
		
		Segment parentSegment = parentIndex == 0 ? this.hierarchy : findSegment(parentIndex, this.hierarchy.getChildren(), null);
		
		ArrayList<Integer> indexArray = new ArrayList<Integer>();
		Iterator<Segment> iter = parentSegment.getChildren().iterator();
		
		while(iter.hasNext())
			indexArray.add(iter.next().getId());
		
		return indexArray;
	}
	
	public Segment getSegment(int segmentID){
		return  segmentID == 0 ? this.hierarchy : findSegment(segmentID, this.hierarchy.getChildren(), null);
	}
	
	public int getParentOfSegment(int index) {
		Segment seg = findSegment(index, this.hierarchy.getChildren(), null);
		
		if(seg != null)
			return seg.getParent();
		else
			return -1;
	}
	
	public int getNextSiblingOfChapter(int startIndex) {		
		Segment sibling  = findSibling(startIndex, 1, this.hierarchy.getChildren(), null);
		
		if(sibling != null)
			return sibling.getId();
		else
			return -1;		
	}
	
	public int getPreviousSiblingOfChapter(int startIndex) {		 
		Segment sibling  = findSibling(startIndex, -1, this.hierarchy.getChildren(), null);
		
		if(sibling != null)
			return sibling.getId();
		else
			return -1;		
	}
	
	public ArrayList<Integer> getSegmentsOnPage(int pageIndex) {
		ArrayList<Integer> segmentsOnPage = new ArrayList<Integer>();
		
//		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> getSegmentsOnPage");
//		System.out.println("content: " + this.hierarchy.getContent());
//		System.out.println("pageNumber: " + this.hierarchy.getPageNumber());
//		System.out.println("amount of childrens: " + this.hierarchy.getChildren().size());
		findSegmentOnPage(pageIndex,this.hierarchy.getChildren(), segmentsOnPage);
		
		return  segmentsOnPage;
	}
	
	private int addSegment(Integer parentIndex, Integer after, String reference, int pageNumber, String type, int startPage, int endPage, String fileContent, CoordinatesSegment coordinates, String title, boolean splitTextbook){
		
		Segment parentChapter = parentIndex == 0 ? this.hierarchy : findSegment(parentIndex, this.hierarchy.getChildren(), null);
		
		int index = this.indexCounter;
		parentChapter.addChild(new Segment(index, reference, type, parentIndex, parentChapter.getLevel()+1,startPage, endPage, coordinates, pageNumber, title));
		this.indexCounter++;
		
		if(parentChapter.getLevel()+1 > this.bookDepth)
			this.bookDepth = parentChapter.getLevel()+1;
		
//		if(type.equals("paragraph") && fileContent != null && !fileContent.equals("") && splitTextbook)
//			Persistence.getInstance().storeSegment(parentBookID, index, fileContent);
		
//		if (index == 437 || index == 438 || pageNumber == 548 || pageNumber == 549 || pageNumber == 550 || title.equals("14 zeitreihen")) {
//		System.out.println("!!!!!!!! addSegment");
//		System.out.println("parentIndex: " + parentIndex);
//		System.out.println("pageNumber: " + pageNumber);
//		System.out.println("reference: " + reference);
//		System.out.println("type: " + type);
//		System.out.println("startPage: " + startPage);
//		System.out.println("endPage: " + endPage);
//		System.out.println("fileContent: " + fileContent);
//		System.out.println("title: " + title);
//		System.out.println("@@segment: " + index);
//		
//		try {
//			SystemLogger.getInstance().log("Waiting 1 minutes");
//			TimeUnit.MINUTES.sleep(1);
//		} catch (InterruptedException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		}
		return index;
	}
	
	
	private Segment findSegment(int id, ArrayList<Segment> level, Segment found){
		Iterator<Segment> iter = level.iterator();
		
		while(iter.hasNext()){
			Segment currentSegment = iter.next();
			if(currentSegment.getId() == id)
				return currentSegment;
			else
				found = findSegment(id, currentSegment.getChildren(), found);

		}
		
		return found;
	}
	
	private void findSegmentOnPage(int pageIndex, ArrayList<Segment> level, ArrayList<Integer> found){
		Iterator<Segment> iter = level.iterator();
		
		while(iter.hasNext()){
			Segment currentSegment = iter.next();
//			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> currentSegment");
//			System.out.println("content: " + currentSegment.getContent());
//			System.out.println("pageNumber: " + currentSegment.getPageNumber());
//			System.out.println("amount of childrens: " + currentSegment.getChildren().size());
			/*if((currentSegment.getStartPageIndex() == pageIndex || currentSegment.getEndPageIndex() == pageIndex) ||
				(currentSegment.getStartPageIndex() < pageIndex && currentSegment.getEndPageIndex() > pageIndex)){
				found.add(currentSegment.getId());
			}*/
			if(pageIndex >= currentSegment.getStartPageIndex() && pageIndex <= currentSegment.getEndPageIndex()){
				found.add(currentSegment.getId());
			}
				
			
			findSegmentOnPage(pageIndex, currentSegment.getChildren(), found);

		}
	}
	

	public int getLevelOfSegment(Integer id) {
		Segment seg = findSegment(id, this.hierarchy.getChildren(), null);
		
		if(seg != null)
			return seg.getLevel();
					
		return -99;
	}
	
	private Segment findSibling(int id, int offset, ArrayList<Segment> level, Segment sibling){
		Segment seg = findSegment(id, this.hierarchy.getChildren(), null);
		Segment segParent = seg.getParent() == 0 ? this.hierarchy : findSegment(seg.getParent(), this.hierarchy.getChildren(), null);
		//Segment segParent = findSegment(seg.getParent(), this.hierarchy.getChildren(), null);
		Segment previous = null;
		
		Iterator<Segment> iter = segParent.getChildren().iterator();
		while(iter.hasNext()){
			Segment currSeg = iter.next();
			
			if(currSeg.getId() == id && offset == 1)
				if(iter.hasNext())
					return iter.next();
				else
					return null;
			if(currSeg.getId() == id && offset == -1) 
				return previous;
			
			previous = currSeg;
		}
			
		return null;
	}

	
	private void findAllLeaves(ArrayList<Segment> level, ArrayList<Segment> leaveList){
		Iterator<Segment> iter = level.iterator();
		
		while(iter.hasNext()){
			Segment currentSegment = iter.next();
			if(currentSegment.getType().equals("paragraph"))
				leaveList.add(currentSegment);
			
			findAllLeaves(currentSegment.getChildren(), leaveList);
			
		}
		
	}
	
	private void findAllLeavesOnLevel(ArrayList<Segment> levelSegments, ArrayList<Segment> leaveList, int level){
		Iterator<Segment> iter = levelSegments.iterator();
		
		while(iter.hasNext()){
			Segment currentSegment = iter.next();
			if(currentSegment.getType().equals("paragraph") && currentSegment.getLevel() == level)
				leaveList.add(currentSegment);
			
			findAllLeavesOnLevel(currentSegment.getChildren(), leaveList, level);
			
		}
		
	}
	
	
	/**
	 * Debugg Stuff
	 */
	public void debuggOutput(){
		if(this.hierarchy != null)
			printIds(this.hierarchy.getChildren());
		else
			SystemLogger.getInstance().log("Hierarchy ERROR while outputting structure model");
	}
	
	public void printIds(ArrayList<Segment> level){
		Iterator<Segment> iter = level.iterator();
		while(iter.hasNext()){
			Segment curr = iter.next();
			String bullets = "";
			for(int iterBullet=0;iterBullet <curr.getLevel();iterBullet++)
				bullets = bullets + "----";
			
			SystemLogger.getInstance().log(bullets+curr.getContent() + " ... ID: "+curr.getId()+ " ... ParentID: " +curr.getParent() + " ... Level: " + curr.getLevel());
			
			printIds(curr.getChildren());
		}
	}

	public String getContentOfSegment(int index) {
		Segment seg = findSegment(index, this.hierarchy.getChildren(), null);
		
		if(seg != null)
			return seg.getContent();
		else
			return null;
	}

	//Returns all children of type paragraph
	public ArrayList<Integer> getLeaves(int index) {
		Segment seg = index == 0 ? this.hierarchy : findSegment(index, this.hierarchy.getChildren(), null);
		
		ArrayList<Integer> leafList = new ArrayList<Integer>();
		Iterator<Segment> iter = seg.getChildren().iterator();
		
		while(iter.hasNext()){
			Segment currentChild = iter.next();
			if(currentChild.getType().equals("paragraph"))
				leafList.add(currentChild.getId());
		}
		
		return leafList;
			
	}
	
	//Returns all nodes of type paragraph for a given subtree
	public ArrayList<Integer> getAllLeaves(int index) {
		Segment seg = index == 0 ? this.hierarchy : findSegment(index, this.hierarchy.getChildren(), null);
		
		ArrayList<Segment> leafeList = new ArrayList<Segment>();
		ArrayList<Integer> indexList = new ArrayList<Integer>();
		
		if(seg.getType().equals("paragraph"))
			indexList.add(seg.getId());
		else{
			findAllLeaves(seg.getChildren(), leafeList);
			Iterator<Segment> iter = leafeList.iterator();
			while(iter.hasNext())
				indexList.add(iter.next().getId());
		}
		
		return indexList;
	}

	//Returns all paragraph nodes which are on the same level as the given segment
	public ArrayList<Integer> getAllLeavesOnSameLevel(int index) {
		Segment child = findSegment(index, this.hierarchy.getChildren(), null);
		
		ArrayList<Integer> leafeIndexList = new ArrayList<Integer>();
		ArrayList<Segment> leafeList = new ArrayList<Segment>();
		
		findAllLeavesOnLevel(this.hierarchy.getChildren(), leafeList, child.getLevel());

		Iterator<Segment> iter = leafeList.iterator();
		
		while(iter.hasNext()){
			Segment currentSegment = iter.next();
				if(!leafeIndexList.contains(currentSegment.getId()))
					leafeIndexList.add(currentSegment.getId());
		}
		
		return leafeIndexList;
	}

	public void store() {
		SystemLogger.getInstance().log("persisting structure model");
		Persistence.getInstance().storeStructureModel(parentBookID, this.hierarchy, this.indexCounter, this.bookDepth, this.model);
		SystemLogger.getInstance().log("Persisting TEI model");
		Persistence.getInstance().storeTEIModel(parentBookID, this.teiModel);
	}

	//used by the content manager after processing of a pdf is done
	public void splitPDF(){
		SystemLogger.getInstance().log("Spliting PDF into single documents");
		Persistence.getInstance().performPDFsplit(this.parentBookID, this.parentBookFileName, this.hierarchy);
		SystemLogger.getInstance().log("Documents created");
	}
	
	public void splitCleanPDF(){
		SystemLogger.getInstance().log("Spliting Clean PDF into single documents");
		Persistence.getInstance().performCleanPDFsplit(this.parentBookID, this.parentBookFileName, this.hierarchy);
		SystemLogger.getInstance().log("Clean Documents created");
	}
	
	//used by create pdf on demand
	public String createPersonalizedPDFsegment(int segmentID, String userName, LanguageEnum userLang, HashMap<String, Boolean> explainPresent) {
		SystemLogger.getInstance().log("Creating PDF of segment " + segmentID + " for user " + userName);
		Segment segment = findSegment(segmentID, this.hierarchy.getChildren(), null);
		String URL = "";
		if(segment != null){
			URL = Persistence.getInstance().performPDFsplit(this.parentBookID, this.parentBookFileName, segment, userName, userLang, explainPresent);
			SystemLogger.getInstance().log("PDF created");
		}

		return URL;
	}

	public boolean isSegmentParagraph(int index) {
		Segment seg = findSegment(index, this.hierarchy.getChildren(), null);
		
		if(seg != null)
			return seg.getType().equals("paragraph");
		else
			return false;
	}

	public String getSegmentType(int index) {
		
		if(index == 0)
			return "book";
		
		Segment seg = findSegment(index, this.hierarchy.getChildren(), null);
		
		if(seg != null)
			return seg.getType();
		else
			return "";
	}
	
	public String getSegmentTitle(int index) {
		
		if(index == 0)
			return "book";
		
		Segment seg = findSegment(index, this.hierarchy.getChildren(), null);
		
		if(seg != null)
			return seg.getTitle();
		else
			return "";
	}
	
	public HashMap<String, Double> getPositionOfSegment(int index) {

		Segment seg = findSegment(index, this.hierarchy.getChildren(), null);
		
		if(seg != null)
			return seg.getPosition();
		else
			return new HashMap<String,Double>();
	}

	public int getStartIndexOfSegment(int index) {
		Segment seg = findSegment(index, this.hierarchy.getChildren(), null);
		
		if(seg != null)
			return seg.getStartPageIndex();
		else
			return -1;
	}

	public int getEndIndexOfSegment(int index) {
		Segment seg = findSegment(index, this.hierarchy.getChildren(), null);
		
		if(seg != null)
			return seg.getEndPageIndex();
		else
			return -1;
	}

	public SKOSManager getSKOSModel(){
		return this.model;
	}
	
	public void setSKOSModel(SKOSManager newModel){
		this.model = newModel;
	}
	
	public void setTEIModel(Document teiModel){
		this.teiModel = teiModel;
	}
	
	public void addIndex(String indexName, List<Integer> segments, List<Integer> indices, List<Integer> pages, boolean artificial) {
		this.indices.addIndex(indexName, segments, indices, pages, artificial);	
	}

	/*OLD*/
//	public void addIndex(String indexName, List<Integer> segments, ArrayList<Integer> indices) {
//		this.indices.addIndex(indexName, segments, indices);	
//	}
//	
//	public void addIndex(String indexName, ArrayList<Integer> indices) {
//		this.indices.addIndex(indexName, indices);
//	}

	public void addConceptToIndex(String indexName, String conceptName) {
		this.indices.addConceptToIndexIndex(indexName, conceptName);
	}
	
	
	public ArrayList<Integer> getOccurrencesOfIndex(String indexName) {
		return this.indices.getOccurrencesOfIndex(indexName);
		
	}
	
	public ArrayList<String> getIndexTermsOnPage(int pageIndex) {
		return this.indices.getIndexTermsOnPage(pageIndex);
	}
	

	public ArrayList<String> getIndexTermsOfSegment(int segmentID) {
		return this.indices.getIndexTermsOfSegment(segmentID);
	}
	
	public Set<String>[] getIndexEntriesOfSegment(int segmentID) {
		return this.indices.getIndexEntriesOfSegment(segmentID);
	}
	
	public ArrayList<Integer> getSegmentsIdOfIndexTerm(String bookID, String indexName){
		return this.indices.getSegmentsIdOfIndexTerm(bookID, indexName);
	}
	
	public Integer getFirstOccurrenceOfIndex(String indexName) {
		ArrayList<Integer> occurrences = this.indices.getOccurrencesOfIndex(indexName);
		Iterator<Integer> iter = occurrences.iterator();
		int firstOccurrence = 99999999;
		
		while(iter.hasNext()){
			int current = iter.next();
			firstOccurrence = firstOccurrence < current ? firstOccurrence : current;
		}
		
		return firstOccurrence;
	}
	
	public void updateConceptNameOfIndexElement(String key, String conceptName) {
		this.indices.updateConceptNameOfIndexElement(key, conceptName);
	}
	
	public ArrayList<String> getListOfIndices() {
		return this.indices.getFullList();
	}
	
	public ArrayList<String> getListOfIndicesWithPages() {
		return this.indices.getWithPagesList();
	}
	
	/*OLD*/
//	public String getConceptOfIndexTerm(String indexTerm) {
//		return this.indices.getConceptOfIndexTerm(indexTerm);
//	}
	
	public String getConceptOfIndexElement(String indexElement) {
		return this.indices.getConceptOfIndexElement(indexElement);
	}
	
	public List<String> getIndexTermsOfConcept(String conceptName) {
		return this.indices.getIndexTermsOfConcept(conceptName);
	}

	public Integer getFirstOccurrenceOfIndexByConcept(String conceptName) {
		ArrayList<Integer> occurrences = this.indices.getOccurrencesOfIndexByConcept(conceptName);
		Iterator<Integer> iter = occurrences.iterator();
		int firstOccurrence = 99999999;
		
		while(iter.hasNext()){
			int current = iter.next();
			firstOccurrence = firstOccurrence < current ? firstOccurrence : current;
		}
		if(firstOccurrence == 99999999)
			return -1;
		
		return firstOccurrence;
	}
	
	
	public int getBookDepth() {
		return this.bookDepth;
	}

	public File getParagraphText(int index) {
		Segment seg = findSegment(index, this.hierarchy.getChildren(), null);
		
		if(seg != null && seg.getType().equals("paragraph"))
			return Persistence.getInstance().loadParagraphFile(this.parentBookID,index);
		else
			return null;
		
	}

	public int getSegmentOfWord(int pageIndex, double wordCoordinateTopLeftY, boolean parent) {
		ArrayList<Integer> possibleSegments = getSegmentsOnPage(pageIndex);
		SystemLogger.getInstance().debug(">> seg on page: " + possibleSegments);
		
		if(possibleSegments.size() == 0)
			return -1;
		
		int possibleSegment = possibleSegments.size() == 1 ? possibleSegments.get(0) : filterSegmentsForWord(possibleSegments, wordCoordinateTopLeftY, pageIndex);
		SystemLogger.getInstance().debug("possibleSegment: " + possibleSegment);
		
		if(parent) {
			SystemLogger.getInstance().debug("parent: true");
			return getParentOfSegment(possibleSegment);
		}
		
		return possibleSegment;
	}

	private int filterSegmentsForWord(ArrayList<Integer> possibleSegments, double wordCoordinatesTopLeftY, int pageIndex){
		Iterator<Integer> iter = possibleSegments.iterator();
		
		//start with smaller segments
		Collections.reverse(possibleSegments);
		
//		System.out.println("segments reverse: " + possibleSegments);
//		System.out.println("pageIndex: " + pageIndex);
		
		while(iter.hasNext()){
			
			Integer segmentN = iter.next();
			Segment currentSegment = getSegment(segmentN);
//			System.out.println("current segment : " + segmentN);
			HashMap<String, Double> segPosition = currentSegment.getPosition();
//			
//			System.out.println("currentSegment.getStartPageIndex(): " + currentSegment.getStartPageIndex());
//			System.out.println("currentSegment.getEndPageIndex() : " + currentSegment.getEndPageIndex());
			if(currentSegment.getStartPageIndex() == pageIndex && currentSegment.getEndPageIndex() == pageIndex){
//				System.out.println("1");
				if(segPosition.get("leftTopY") < wordCoordinatesTopLeftY && segPosition.get("rightBottomY") > wordCoordinatesTopLeftY) {
//					System.out.println("1.1");
					return currentSegment.getId();
				}
					
				
			}else if(currentSegment.getStartPageIndex() < pageIndex && currentSegment.getEndPageIndex() == pageIndex){
//				System.out.println("2");
				if(wordCoordinatesTopLeftY != -1) {
					if(segPosition.get("rightBottomY") > wordCoordinatesTopLeftY) {
//						System.out.println("2.1");
						return currentSegment.getId();
					}
				}
			}else if(currentSegment.getStartPageIndex() == pageIndex && currentSegment.getEndPageIndex() > pageIndex){
//				System.out.println("3");
//				System.out.println("segPosition: " + segPosition);
//				System.out.println("wordCoordinatesTopLeftY: " + wordCoordinatesTopLeftY);
				if(wordCoordinatesTopLeftY != -1 || segPosition.get("leftTopY") < wordCoordinatesTopLeftY) {
//					System.out.println("3.1");
					return currentSegment.getId();
				}
				
			}else if(currentSegment.getStartPageIndex() < pageIndex && currentSegment.getEndPageIndex() > pageIndex){
//				System.out.println("4");
				return currentSegment.getId();
			}
			
			
		}
		
		return -1;
	}

	
	
	



	

	

	
	

	

	
	

	

}
