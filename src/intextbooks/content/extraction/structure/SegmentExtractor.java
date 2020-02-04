package intextbooks.content.extraction.structure;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;


import org.apache.commons.lang3.tuple.Pair;

import intextbooks.SystemLogger;
import intextbooks.content.ContentManager;
import intextbooks.content.extraction.ContentExtractor;
import intextbooks.content.extraction.Utilities.HyphenationResolver;
import intextbooks.content.extraction.Utilities.StringOperations;
import intextbooks.content.extraction.Utilities.WordListCheck;
import intextbooks.content.extraction.buildingBlocks.format.Line;
import intextbooks.content.extraction.buildingBlocks.format.Page;
import intextbooks.content.extraction.buildingBlocks.format.ResourceUnit;
import intextbooks.content.extraction.buildingBlocks.format.Slide;
import intextbooks.content.extraction.buildingBlocks.structure.NodeDataContainer;
import intextbooks.content.extraction.buildingBlocks.structure.TOC;
import intextbooks.content.extraction.buildingBlocks.structure.TOCLogical;
import intextbooks.content.extraction.buildingBlocks.structure.TOCResource;
import intextbooks.content.extraction.format.FormatExtractor;
import intextbooks.content.models.Book;
import intextbooks.content.models.formatting.FormattingDictionary;
import intextbooks.ontologie.LanguageEnum;


public class SegmentExtractor {
	
	private ContentManager cm = ContentManager.getInstance(); 
	private List<SKOSModelSegment> SKOSModelSegments = new ArrayList<SKOSModelSegment>();
	private final double similarityThreshold = 0.7;
	private HyphenationResolver hyphenResolver;
	
	public SegmentExtractor(LanguageEnum lang) {
		try {
			hyphenResolver = HyphenationResolver.getInstance(lang);
		} catch (Exception e) {
			e.printStackTrace();
			hyphenResolver = null;
		}
	}
	
	private class SKOSModelSegment {
		int ID;
		boolean isParapgraph;
		int positionIndex;
		
		public SKOSModelSegment(int ID, boolean isParapgraph, int positionIndex) {
			this.ID = ID;
			this.isParapgraph = isParapgraph;
			this.positionIndex = positionIndex;
		}
	}


	private double[] getLeftTopCoords(ResourceUnit page, int lineOfInterest){		

		double topLeftX = page.getLineAt(lineOfInterest).getStartPositionX();

		double topLeftY = page.getLineAt(lineOfInterest).getPositionY() + page.getLineAt(lineOfInterest).getFontSize();

		double result[] = {topLeftX, topLeftY};

		return result;	
	}

	private double[] getRightBottomCoords(ResourceUnit page, int lineOfInterest){

		SystemLogger.getInstance().debug("Page: " + page.getPageIndex() + " pageN: " + page.getPageNumber() + " " + lineOfInterest);
		double bottomRightX = page.getLineAt(lineOfInterest).getEndPositionX();

		double bottomRightY = page.getLineAt(lineOfInterest).getPositionY();

		double result[] = {bottomRightX, bottomRightY};

		return result;	
	}
	
	/**
	 * 
	 * @param book
	 * @param pageNumber
	 * @return
	 */

	private ResourceUnit findPageWithPageNumber(List<ResourceUnit> book, int pageNumber){
		for(int i = 0; i<book.size(); i++){

			if(book.get(i) != null && book.get(i).getPageNumber() == pageNumber)
				return book.get(i);
		}
		
		for(int i = 0; i<book.size(); i++){
			if(book.get(i) != null )
				System.out.println("PI: " + i + " PN: " + book.get(i).getPageNumber() );
		}

		return null;
	}
	
	private ResourceUnit findPageUnitWithIndexNumber(List<ResourceUnit> book, int pageIndex){

		for(int i = 0; i<book.size(); i++){

			if(book.get(i) != null && book.get(i).getPageIndex() == pageIndex)
				return book.get(i);
		}

		return null;
	}

	/**
	 * 
	 * @param book
	 * @param pageNumber
	 * @return
	 */
	
	private int findPageIndexWithPageNumber(List<ResourceUnit> book, int pageNumber){
		for(int i = 0; i<book.size(); i++){
			if(book.get(i) != null && book.get(i).getPageNumber() == pageNumber)
				return i;
		}

		return -1;
	}

	/**
	 * 
	 * @param page
	 * @param title
	 * @param toEndChapter
	 * @return
	 */
	
	private Pair<Integer, Double> checkMultiLine(ResourceUnit page, String title, double similarityBase, int startLine, int bodyFontSize) {
		String text = "";
		int line = -1;
		for(int j = startLine ; j<page.getLines().size(); j++){
			text += page.getLineAt(j).getText().toLowerCase();
			double similarity = StringOperations.similarity(title.toLowerCase(),text);
			if(similarity >= similarityBase) {
				line = j;
				similarityBase = similarity;
				SystemLogger.getInstance().debug("... IMPROVED: " + text + " s: " + similarityBase);
			} else {
				break;
			}
			
			text+= " ";
		}
		if(line != -1 && line != startLine) {
			return Pair.of(line, similarityBase);
		} else {
			return null;
		}
	}
	
	private Pair<Integer,Integer> titleLine(ResourceUnit page, String title,boolean toEndChapter, int bodyFontSize){
		SystemLogger.getInstance().debug("to end chapter: " + toEndChapter);
		SystemLogger.getInstance().debug("page: " + page);
		SystemLogger.getInstance().debug("title: " + title);
		double similarityBase=similarityThreshold;
		int startLineOfInterest=-1;
		int lineOfInterest=-1;		
		int secondLineOfInterest = -1;
		for(int j = 0 ; j<page.getLines().size(); j++){
			SystemLogger.getInstance().debug("************************************************************");
			startLineOfInterest = j;
			String temp = page.getLineAt(j).getText().toLowerCase();
			SystemLogger.getInstance().debug("temp: " + temp);
			SystemLogger.getInstance().debug("title: " + title);
			double similarity = StringOperations.similarity(title.toLowerCase(),temp );
			SystemLogger.getInstance().debug(" checking: " + title.toLowerCase() + " to: " + temp + " S: " + similarity);

			if(similarity>similarityBase && (page.getLineAt(j).getFontSize() > bodyFontSize || page.getLineAt(j).isBold() || page.getLineAt(j).isItalic()) || similarity == 1){					
				SystemLogger.getInstance().debug(" YES similarity>similarityBase");
				lineOfInterest = j;						
				similarityBase=similarity;
				
				if(similarity < 1) {
					Pair<Integer, Double> result = checkMultiLine(page, title, similarityBase, j, bodyFontSize);
					SystemLogger.getInstance().debug(" R multiline: " + result);
					if(result != null && result.getRight() > similarityBase) {
						secondLineOfInterest = result.getLeft();
						similarityBase = result.getRight();
					} else {
						secondLineOfInterest = -1;
					}
				}

				if(similarity == 1 || similarityBase == 1) {
					break;
				}
				//System.out.println(" l of interest: " + lineOfInterest);
			} else if (page.getLineAt(j).getFontSize() > bodyFontSize || page.getLineAt(j).isBold() || page.getLineAt(j).isItalic() ) {
				Pair<Integer, Double> result = checkMultiLine(page, title, 0, j, bodyFontSize);
				if(result != null && result.getRight() > similarityThreshold) {
					lineOfInterest = j;
					secondLineOfInterest = result.getLeft();
					similarityBase = result.getRight();
					if(similarityBase == 1) {
						break;
					}
				}
			}
		}
		
//		System.out.println("secondLineOfInterest: " + secondLineOfInterest);
//		System.out.println(" l of interest FINAL: " + lineOfInterest);
//		System.out.println(" toEndChapter: " + toEndChapter);
		
		if(lineOfInterest > 0 && !toEndChapter) {
			if(WordListCheck.containsChapterTitle(page.getLineAt(lineOfInterest - 1).getText()) && page.getLineAt(lineOfInterest - 1).getFontSize() > bodyFontSize) {
				lineOfInterest--;
			}
		}
		
		int originalLineOfInterest = lineOfInterest;
		
		if(lineOfInterest == -1){
		
			if(toEndChapter) {
				lineOfInterest = page.getLines().size()-1;			
			} else {
				lineOfInterest = 0;
			}
			
			//System.out.println("~~ case 1");
		} else if(toEndChapter) {
			lineOfInterest = lineOfInterest -1;
			
			//System.out.println("~~ case 2");
		}else if(lineOfInterest < page.getLines().size()-1 && !toEndChapter){

			if(secondLineOfInterest != -1) {
				lineOfInterest = secondLineOfInterest;
			}
			
			//System.out.println("secondLineOfInterest: " + secondLineOfInterest);
			//System.out.println("~~ case 3");
		} else {
			//System.out.println("~~ case 4");
		}

		return Pair.of(originalLineOfInterest,lineOfInterest);
	}

	/**
	 * 
	 * @param listToModel
	 * @param book
	 * @param toc
	 * @param counter
	 * @return
	 */
	
	private ChapterMetaData extractChapterMetaData(StructureBuilder listToModel, List<ResourceUnit> book, List<TOC> toc, int counter,FormattingDictionary styleLibrary){		
		
		
		//SystemLogger.getInstance().log("++++++++ > extractChapterMetaData: " + counter); 
		
		NodeDataContainer x = listToModel.getEntryAt(counter);
		
//		if(x.getTitle().equals("appendix 1: shortcut formulas for calculating variance and standard deviation")) {
//			SystemLogger.getInstance().setDebug(true);
//		} else {
//			SystemLogger.getInstance().setDebug(false);
//		}
		
			SystemLogger.getInstance().debug("*******************************************************************************"); 
			SystemLogger.getInstance().debug("++++++++ > title: " + x.getTitle()); 
			SystemLogger.getInstance().debug("++++++++ > getPageNumberStart: " + x.getPageNumberStart()); 
			SystemLogger.getInstance().debug("++++++++ > getPageNumberEnd: " + x.getPageNumberEnd()); 
			SystemLogger.getInstance().debug("++++++++ > getSectionNumber: " + x.getSectionNumber()); 
			SystemLogger.getInstance().debug("++++++++ > getHierarchyLevel: " + x.getHierarchyLevel()); 

		if(counter < toc.size()-1){

			SystemLogger.getInstance().debug("Counter get title text: " + toc.get(counter).getTitleText()); 
			String buff= toc.get(counter).getTitleText();

			int pageStart = toc.get(counter).getPageNumber();
			if(pageStart <= 0)
				pageStart = x.getPageNumberStart();			
			
			int pageStartIndex =  findPageIndexWithPageNumber(book, pageStart);

			//inside the page, the line number where the title of the chapter is
			SystemLogger.getInstance().debug("++++++++ > LINE START: looking: " +  buff); 
			Pair<Integer, Integer> titleLineResult = titleLine(findPageWithPageNumber(book, pageStart), buff,false, styleLibrary.getBodyFontSize());
			int lineStart = titleLineResult.getRight();
			int titleLineStart = titleLineResult.getLeft();
			SystemLogger.getInstance().debug("++++++++ > LINE START: " +  lineStart); 

			double topLeftCorner[] =  getLeftTopCoords(book.get(pageStartIndex),lineStart);

			//line to search for for the end of the chapter/subchapter
			buff = listToModel.getEntryAt(counter).getTitleNextNodeHierarchy();
			
			int pageEnd = listToModel.getEntryAt(counter).getPageNumberEnd(); 
			
			SystemLogger.getInstance().debug("listToModel.getEntryAt(counter).getPageNumberEnd(): " + listToModel.getEntryAt(counter).getPageNumberEnd());
			int pageEndIndex =  findPageIndexWithPageNumber(book,listToModel.getEntryAt(counter).getPageNumberEnd());
			SystemLogger.getInstance().debug("pageEndIndex" + pageEndIndex);
			
			//it means the last chapter of the TOC has subchapters, so there are elements in TOC left to process
			if( buff == null) {
				SystemLogger.getInstance().debug("TOC s" + toc.size());
				SystemLogger.getInstance().debug("counter :" + counter);
				SystemLogger.getInstance().debug("pageEnd :" + pageEnd);
				for(TOC tocL : toc) {
					SystemLogger.getInstance().debug("TOC :" + tocL.getTitleText());
				}

				int lineEnd = book.get(pageEndIndex).size()-1;
				if(lineEnd == -1) {
					pageEndIndex--;
					lineEnd = book.get(pageEndIndex).size()-1;
				} 
				
				double bottomRightCorner[] = getRightBottomCoords(book.get(pageEndIndex),lineEnd);
				
				buff= toc.get(counter).getTitleText();		
				
				return new ChapterMetaData(pageStartIndex, pageStart, lineStart,pageEndIndex, pageEnd, lineEnd,
						topLeftCorner[0], topLeftCorner[1], bottomRightCorner[0], bottomRightCorner[1],
						buff,true, titleLineStart);
			}
 
			SystemLogger.getInstance().debug("pageEndIndex: " + pageEndIndex);
			SystemLogger.getInstance().debug("findPageUnitWithIndexNumber(book, pageEndIndex): " + findPageUnitWithIndexNumber(book, pageEndIndex));
			
//			/*TESTING*/
//			for(Line r: findPageUnitWithIndexNumber(book, pageEndIndex).getLines()) {
//				System.out.println(r.getText());
//				
//			}
//			System.out.println("************************");
//			for(Line r: book.get(pageEndIndex).getLines()) {
//				System.out.println(r.getText());
//				
//			}
//			//System.exit(0);
//			/*TESTING*/			
			
			SystemLogger.getInstance().debug("++++++++ > LINE END: looking: " +  buff); 
			titleLineResult = titleLine(findPageUnitWithIndexNumber(book, pageEndIndex), buff,true, styleLibrary.getBodyFontSize());
			int lineEnd = titleLineResult.getRight();
			SystemLogger.getInstance().debug("++++++++ > RESULT LINE END:: " +  lineEnd); 
			if(lineEnd == -1) {
				pageEndIndex = pageEndIndex -1;
				SystemLogger.getInstance().debug("NEW PAGE END INDEX: "  +  pageEndIndex);
				if(findPageUnitWithIndexNumber(book, pageEndIndex).size() == 0) {
					boolean found = false;
					while(!found && pageEndIndex > 0) {
						pageEndIndex--;
						Integer size = findPageUnitWithIndexNumber(book, pageEndIndex).size();
						if(size != null && size > 0) {
							lineEnd = size - 1;
							found = true;
							break;
						}
					}
				} else {
					lineEnd = findPageUnitWithIndexNumber(book, pageEndIndex).size()-1;
				}
				
				SystemLogger.getInstance().debug(" findPageUnitWithIndexNumber(book, pageEndIndex): "  +  findPageUnitWithIndexNumber(book, pageEndIndex));
				SystemLogger.getInstance().debug(" findPageUnitWithIndexNumber(book, pageEndIndex).size(): "  + findPageUnitWithIndexNumber(book, pageEndIndex).size());
				SystemLogger.getInstance().debug("lineEnd "  + lineEnd);
			}
			
			SystemLogger.getInstance().debug("--- lineEnd: "+ lineEnd);
			SystemLogger.getInstance().debug("--- pageEndIndex: "+ pageEndIndex);
			double bottomRightCorner[] = getRightBottomCoords(findPageUnitWithIndexNumber(book, pageEndIndex),lineEnd);

			buff= toc.get(counter).getTitleText();		
			
			boolean hasParagraph = true;
			//original < 5
			if(pageStart == pageEnd && lineEnd-lineStart == 1){
				hasParagraph = false;
			}
			
			/*TESTING*/
			SystemLogger.getInstance().debug("------ Summary:");
			SystemLogger.getInstance().debug("Chapter title: " + buff);
			SystemLogger.getInstance().debug("page start: " + pageStart);
			SystemLogger.getInstance().debug("page startIndex: " + pageStartIndex);
			SystemLogger.getInstance().debug("page end: " + pageEnd);
			SystemLogger.getInstance().debug("page end SEARCH: " + listToModel.getEntryAt(counter).getPageNumberEnd());
			SystemLogger.getInstance().debug("page endIndex: " + pageEndIndex);
			SystemLogger.getInstance().debug("LINE Start index: " + lineStart);
			SystemLogger.getInstance().debug("LINE Start: " + book.get(pageStartIndex).getLineAt(lineStart).getText());
			SystemLogger.getInstance().debug("LINE End index: " + lineEnd);
			SystemLogger.getInstance().debug("LINE End: " + book.get(pageEndIndex).getLineAt(lineEnd).getText());
			if(lineEnd -1 > 0 ) {
				SystemLogger.getInstance().debug("LINE End before: " + book.get(pageEndIndex).getLineAt(lineEnd -1).getText());
			}
			/*TESTING*/
			
			/*if(buff.equals("19.3 neural networks in non-parametric regression analysis"))
				System.exit(0);*/
			/*TESTING*/
			
			return new ChapterMetaData(pageStartIndex, pageStart, lineStart,pageEndIndex, pageEnd, lineEnd,
					topLeftCorner[0], topLeftCorner[1], bottomRightCorner[0], bottomRightCorner[1],
					buff,hasParagraph, titleLineStart);
		}
		else if(counter == toc.size()-1){

			String buff= toc.get(counter).getTitleText();
			
			int pageStart = toc.get(counter).getPageNumber();

			int pageStartIndex =  findPageIndexWithPageNumber(book, pageStart);

			Pair<Integer, Integer> titleLineResult = titleLine(findPageWithPageNumber(book, pageStart), buff,false, styleLibrary.getBodyFontSize());
			int lineStart = titleLineResult.getRight();
			int titleLineStart = titleLineResult.getLeft();

			double topLeftCorner[] =  getLeftTopCoords(book.get(pageStartIndex-1),lineStart);

			int pageEnd = book.get(book.size()-1).getPageNumber();

			
			int pageEndIndex = book.size()-1 ;
			while(book.get(pageEndIndex).size() == 0) {
				pageEndIndex--;
			}
			int lineEnd = book.get(pageEndIndex).size()-1;

			double bottomRightCorner[] = getRightBottomCoords(book.get(pageEndIndex),lineEnd);
			
			/*TESTING*/
//			SystemLogger.getInstance().debug("------ LAST");
//			SystemLogger.getInstance().debug("Chapter title: " + buff);
//			SystemLogger.getInstance().debug("page start: " + pageStart);
//			SystemLogger.getInstance().debug("page startIndex: " + pageStartIndex);
//			SystemLogger.getInstance().debug("page end: " + pageEnd);
//			SystemLogger.getInstance().debug("page endIndex: " + pageEndIndex);
//			try {
//				TimeUnit.SECONDS.sleep(1);
//			} catch (InterruptedException e1) {}
			/*TESTING*/
			
			return new ChapterMetaData(pageStartIndex, pageStart, lineStart,pageEndIndex, pageEnd, lineEnd,
					topLeftCorner[0], topLeftCorner[1], bottomRightCorner[0], bottomRightCorner[1],
					buff,true, titleLineStart);
		}
		return null;
	}

/**
 * Extracts start and end page, and start and end line inside the page for each sub/chapter.
 * It also creates the chapter and paragraphs segments and adds that information to the book model (Book).
 * It also stores the chapter and paragraphs schemes elements for later create them in the SKOS MODEL using addSegmentsToSKOSModle function.
 * 
 * @param bookID
 * @param parse
 * @param listToModel
 * @throws IOException
 */

	public ArrayList<SegmentData> seperatedChapterExtractions(String bookID, FormatExtractor parse, StructureBuilder listToModel, TOCLogical tocLogical,FormattingDictionary styleLibrary, boolean splitTextbook) throws IOException{

		int chapterID=0;
		int paragraphID=0;
		
		boolean notBook = false;
		
		String paragraph;
		
		if(!cm.getBookType(bookID).equals("book")){
			notBook = true;
		}
		
		Stack <Integer> chapterOrder = new Stack <Integer>();
		Stack <Integer> chapterHiererachy = new Stack <Integer> ();

		Book bookInstance = cm.getInstanceOfBookByName(bookID);
		
		ArrayList<ResourceUnit> pagesAsResourceUnits = parse.getPagesAsResourceUnits();
		
		//to store the information of all the segments
		ArrayList<SegmentData> segmentsData = new ArrayList<SegmentData>();

//		/*TESTING*/	
//		System.out.println("RU size: " + pagesAsResourceUnits.size());
//		for(Line l: pagesAsResourceUnits.get(82).getLines()) {
//			System.out.println(l.getText());
//		}
//		System.out.println("Pages size: " + parse.getPages().size());
//		for(Line l: parse.getPages().get(82).getLines()) {
//			System.out.println(l.getText());
//		}
//		System.exit(0);
//		/*TESTING*/
			
		List<TOC> TOC = parse.getTableOfContents();

		/*
		 * for each TOC entry, add a chapter entry to BOOK following hierarchical structure 
		 */
		for(int i = 0; i <parse.getTableOfContents().size(); i++){
			
			if(TOC.get(i).getSection() || TOC.get(i).isErratum()) {
				continue;
			}
			
			ChapterMetaData chaptData = extractChapterMetaData(listToModel,pagesAsResourceUnits , parse.getTableOfContents(), i, styleLibrary);
			chaptData.setChapterHierarchy(listToModel.getHierarchyLevel(i));

			SystemLogger.getInstance().debug("-- : " + chaptData.pageTitle + " HL: " + listToModel.getHierarchyLevel(i) );
			SystemLogger.getInstance().debug("-- hasParagraph: " + chaptData.hasParagraph);
			
			paragraph="";

			if(listToModel.getHierarchyLevel(i)==1){

				chapterOrder.clear();
				chapterHiererachy.clear();

				chapterID = bookInstance.addChapter(0, null, chaptData.pageTitle, chaptData.pageStart, chaptData.pageStartIndex, chaptData.pageEndIndex,
						chaptData.leftTopX,chaptData.leftTopY,chaptData.rightBottomX,chaptData.rightBottomY);

				chapterOrder.push(chapterID);

				chapterHiererachy.push(1);

			}
			else if(listToModel.getHierarchyLevel(i-1)<listToModel.getHierarchyLevel(i)){

				chapterID = bookInstance.addChapter(chapterID, null, chaptData.pageTitle, chaptData.pageStart, chaptData.pageStartIndex, chaptData.pageEndIndex,
						chaptData.leftTopX,chaptData.leftTopY,chaptData.rightBottomX,chaptData.rightBottomY);

				chapterOrder.push(chapterID);
				chapterHiererachy.push(listToModel.getHierarchyLevel(i));

			}
			else if(listToModel.getHierarchyLevel(i-1) == listToModel.getHierarchyLevel(i)){

				chapterOrder.pop();

				chapterHiererachy.pop();

				chapterID = chapterOrder.peek();

				chapterID = bookInstance.addChapter(chapterID, null, chaptData.pageTitle, chaptData.pageStart, chaptData.pageStartIndex, chaptData.pageEndIndex,
						chaptData.leftTopX,chaptData.leftTopY,chaptData.rightBottomX,chaptData.rightBottomY);

				chapterOrder.push(chapterID);

				chapterHiererachy.push(listToModel.getHierarchyLevel(i));
			}
			else{

				while(chapterHiererachy.peek() >= listToModel.getHierarchyLevel(i)){

					chapterHiererachy.pop();
					chapterOrder.pop();
				}

				chapterID=chapterOrder.peek();

				chapterID = bookInstance.addChapter(chapterID, null, chaptData.pageTitle, chaptData.pageStart, chaptData.pageStartIndex, chaptData.pageEndIndex,
						chaptData.leftTopX,chaptData.leftTopY,chaptData.rightBottomX,chaptData.rightBottomY);

				chapterOrder.push(chapterID);

				chapterHiererachy.push(listToModel.getHierarchyLevel(i));
			}
			
			SegmentData segData = new SegmentData(chaptData.pageTitle, listToModel.getHierarchyLevel(i), 0, 0, chaptData, null);
			segmentsData.add(segData);
			
			if(chaptData.hasParagraph){
				
				//SystemLogger.getInstance().log("-----> adding chap.paragraph: "); 

				paragraph = extractChapter(pagesAsResourceUnits,chaptData);

				if(!notBook)				
					paragraphID = bookInstance.addParagraph(chapterID, null, String.valueOf(bookInstance.getStructureModelIndexCounter())+".txt", chaptData.pageStart, chaptData.pageStartIndex, findPageIndexWithPageNumber( pagesAsResourceUnits,chaptData.pageEnd)+1, paragraph,
							chaptData.leftTopX,chaptData.leftTopY,chaptData.rightBottomX,chaptData.rightBottomY, splitTextbook);
				else
					paragraphID = bookInstance.addParagraph(chapterID, null, String.valueOf(bookInstance.getStructureModelIndexCounter())+".txt", chaptData.pageStart, chaptData.pageStartIndex, chaptData.pageEndIndex, paragraph,
							chaptData.leftTopX,chaptData.leftTopY,chaptData.rightBottomX,chaptData.rightBottomY, splitTextbook);

				this.SKOSModelSegments.add(new SKOSModelSegment(paragraphID, true, i));
				
				//SystemLogger.getInstance().log("----->  chap.paragraph added: " + paragraphID); 
				
				//store content
				TOCResource resource = tocLogical.findResource(chaptData.pageTitle);
				if(resource != null) {
					SystemLogger.getInstance().debug("TOC r: " + resource.getTitle());
					resource.setContent(paragraph);
				}
				
				segData.setParagraphID(paragraphID);
				segData.setText(paragraph);
//				if(chaptData.pageTitle.equals("9.1.1 starting r under mac os, windows and under unix")) {
//					System.exit(0);
//				}
			}
				
			this.SKOSModelSegments.add(new SKOSModelSegment(chapterID, false, i));
			segData.setChapterID(chapterID);
		}
		//System.exit(0);
		return segmentsData;
	}
	
	/**
	 * It creates the chapter and paragraphs schemes elements in the SKOS MODEL.
	 * @param listToModel
	 */
	public void addSegmentsToSKOSModel(StructureBuilder listToModel) {
		for(SKOSModelSegment seg: SKOSModelSegments) {
			
			listToModel.addSegmentToSKOSModel(seg.ID, seg.isParapgraph, seg.positionIndex);
		}
	}

/**
 * 
 * @param book
 * @param chaptData
 * @return
 * @throws IOException
 */

	private String extractChapter(List<ResourceUnit> book, ChapterMetaData chaptData) throws IOException{

		//StringBuilder paragraph = new StringBuilder();
		ArrayList<Line> lines = new ArrayList<Line>();
		
		if(chaptData!=null){

			int startPageIndex = findPageIndexWithPageNumber(book, chaptData.pageStart) ;
			int endPageIndex = chaptData.pageEndIndex; 
			
			
	
			for(int i = startPageIndex; i <= endPageIndex ; i++ ){
				if (book.get(i) == null) {
					continue;
				}

				int beginningLine = 0;
				int endingLine = book.get(i).size()-1;


				if(i == startPageIndex) {
					beginningLine = chaptData.lineStart+1;
				}
					
				if(i ==endPageIndex){
					if (!(book.get(i) instanceof Slide))
						if(endingLine>=chaptData.lineEnd)
							endingLine = chaptData.lineEnd;
				
				}

				if(book.get(i)!=null && book.get(i).size()>0)
					for(int j = beginningLine; j<=endingLine; j++) {
						//paragraph.append(book.get(i).getLineAt(j).getText()+" ");
						lines.add(book.get(i).getLineAt(j));
					}			
			}
		}
		
		//erase empty lines
		Iterator<Line> it = lines.iterator();
		while(it.hasNext()) {
			Line l = it.next();
			if(l.size() == 0) {
				it.remove();
			}
		}
		
		//dehyphenateText
		String text = hyphenResolver.dehyphenateText(lines);

		SystemLogger.getInstance().debug(chaptData.pageTitle+ ": "+chaptData.pageStartIndex+ " ... " + chaptData.pageEndIndex );

		//return paragraph.toString();
		return text;
	}
	
	/**
	 * 
	 * @param book
	 * @param startPageIndex
	 * @param lineStart it is included
	 * @param endPageIndex
	 * @param lineEnd it is included
	 * @return
	 * @throws IOException
	 */
	public String extractChapterLines(List<Page> book, int startPageIndex, int lineStart, int endPageIndex, int lineEnd, String pageTitle, int startPageNumber, boolean checkForCoopyright) {

		//StringBuilder paragraph = new StringBuilder();
		List<Line> lines = new ArrayList<Line>();
		List<Integer> pageBreaks = new ArrayList<Integer>();	
	
		int lineIndex = 0;
		for(int i = startPageIndex; i <= endPageIndex ; i++ ){
			if (book.get(i) == null) {
				continue;
			}
			
			if(i == (startPageIndex + 1) && checkForCoopyright) {
				int before = lines.size();
				ContentExtractor.removeCopyRightLines(lines, startPageNumber);
				int after = lines.size();
				lineIndex -= (before - after);
				
			}

			int beginningLine = 0;
			int endingLine = book.get(i).size()-1;
			if(lineIndex != 0) {
				pageBreaks.add(lineIndex);
			}

			if(i == startPageIndex) {
				beginningLine = lineStart;
			}
				
			if(i ==endPageIndex){
				if(endingLine>lineEnd)
					endingLine = lineEnd;
			}

			if(book.get(i)!=null && book.get(i).size()>0)
				for(int j = beginningLine; j<=endingLine; j++) {
					lines.add(book.get(i).getLineAt(j));
					lineIndex++;
				}			
		}
		
		if(WordListCheck.isExerciseSection(pageTitle) || WordListCheck.isAppendixSection(pageTitle) || WordListCheck.containsIndex(pageTitle)) {
			//SystemLogger.getInstance().setDebug(true);
			ColumnExtractor columnExtractor = new ColumnExtractor(lines);
			boolean withColumns = columnExtractor.identifyColumns();
			if(withColumns) {
				lines = columnExtractor.getLines(pageBreaks);
			} 
			SystemLogger.getInstance().setDebug(false);
		}
		
		//erase empty lines
		Iterator<Line> it = lines.iterator();
		while(it.hasNext()) {
			Line l = it.next();
			if(l.size() == 0) {
				it.remove();
			}
		}
		
		//dehyphenateText
		String text = hyphenResolver.dehyphenateText(lines);

		return text;
	}
	
	
	
}
