package intextbooks.persistence;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import intextbooks.SystemLogger;
import intextbooks.content.extraction.ContentExtractor;
import intextbooks.content.extraction.Utilities.HyphenationResolver;
import intextbooks.content.extraction.Utilities.WordListCheck;
import intextbooks.content.extraction.buildingBlocks.format.Line;
import intextbooks.content.extraction.buildingBlocks.format.Page;
import intextbooks.content.extraction.buildingBlocks.format.Text;
import intextbooks.content.extraction.buildingBlocks.structure.IndexElement;
import intextbooks.content.extraction.buildingBlocks.structure.TOC;
import intextbooks.content.extraction.buildingBlocks.structure.TOCLogical;
import intextbooks.content.extraction.buildingBlocks.structure.TOCResource;
import intextbooks.content.extraction.buildingBlocks.structure.TOCResourceType;
import intextbooks.content.extraction.buildingBlocks.structure.TextBlock;
import intextbooks.content.extraction.format.FormatExtractor;
import intextbooks.content.extraction.structure.ColumnExtractor;
import intextbooks.content.extraction.structure.ColumnedPage;
import intextbooks.content.extraction.structure.SegmentData;
import intextbooks.content.extraction.structure.SegmentExtractor;
import intextbooks.content.models.formatting.FormattingDictionary;
import intextbooks.ontologie.LanguageEnum;
import intextbooks.tools.utility.GeneralUtils;

public class TEIBuilder {
	
	// TEI XMLNS
	final String xmlns = "http://www.tei-c.org/ns/1.0";
	
	//Text constants
	final String titleH = "Textbook Model: ";
	final String publisherT = "intextbooks -- Intelligent Textbooks Project";
	final String projectURL = "https://intextbooks.science.uu.nl/";
	
	//TEI XML DOM
	Document document;
	Element root;
	
	String textbookTitle;
	
	//Book Body Content
	ArrayList<SegmentData> segmentsData;
	List<Page> pages;
	SegmentExtractor segmentExtractor;
	int firstIndexPage;
	TOCLogical tocLogical;
	List<IndexElement> index;
	Map<String,String> metadata;
	FormattingDictionary formattingDictionary;
	
	//internal
	int id;
	HyphenationResolver hyphenResolver;
	
	class PageBreak{
		int pageNumber;
		int pageIndex;
		int linePos;
		
		public PageBreak(int pageNumber, int pageIndex, int linePos) {
			this.pageNumber = pageNumber;
			this.pageIndex = pageIndex;
			this.linePos = linePos;
		}
		
		public int getPageNumber() {
			return this.pageNumber;
		}
		
		public int getPageIndex() {
			return this.pageIndex;
		}
		
		public int getLinePos() {
			return this.linePos;
		}
	}
	
	public TEIBuilder(String textbookTitle, LanguageEnum lang, ArrayList<SegmentData> segmentsData, List<Page> pages, SegmentExtractor segmentExtractor, int firstIndexPage, TOCLogical tocLogical, List<IndexElement> index, Map<String,String> metadata, FormattingDictionary formattingDictionary) {
		try {
			this.textbookTitle = textbookTitle;
			
			this.segmentsData = segmentsData;
			this.pages = pages;
			this.segmentExtractor = segmentExtractor;
			this.firstIndexPage = firstIndexPage;
			this.tocLogical = tocLogical;
			this.index = index;
			this.metadata = metadata;
			this.formattingDictionary = formattingDictionary;
			
			DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
			document = documentBuilder.newDocument();
			
			id = 0;
			hyphenResolver = HyphenationResolver.getInstance(lang);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void construct() {
		try {
			// root element
            root = document.createElement("TEI");
            root.setAttribute("xlmns", this.xmlns);
            document.appendChild(root);
            
            this.addHeader();
            
            this.addFront();
            
            this.addBody();
            
            this.addBack();
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Document getModel() {
		return this.document;
	}
	
	//******************** PRIVATE
	
	private void addHeader() {
		Element teiHeader = document.createElement("teiHeader");
		root.appendChild(teiHeader);
		
		Element fileDesc = document.createElement("fileDesc");
		teiHeader.appendChild(fileDesc);
		
		Element titleStmt = document.createElement("titleStmt");
		fileDesc.appendChild(titleStmt);
		Element title = document.createElement("title");
		org.w3c.dom.Text titleText = document.createTextNode(this.titleH + this.textbookTitle);
		title.appendChild(titleText);
		titleStmt.appendChild(title);
		
		Element publicationStmt = document.createElement("publicationStmt");
		fileDesc.appendChild(publicationStmt);
		//Element distributor = document.createElement("distributor");
		//publicationStmt.appendChild(distributor);
		Element publisher = document.createElement("publisher");
		publisher.appendChild(document.createTextNode(this.publisherT));
		publicationStmt.appendChild(publisher);
		Element pubPlace = document.createElement("pubPlace");
		pubPlace.appendChild(document.createTextNode(this.projectURL));
		publicationStmt.appendChild(pubPlace);
		Element date = document.createElement("date");
		date.setAttribute("when", this.getDate());
		publicationStmt.appendChild(date);
		
		if(this.metadata.size() > 0) {
			Element sourceDesc = document.createElement("sourceDesc");
			fileDesc.appendChild(sourceDesc);
			Element biblFull = document.createElement("biblFull");
			sourceDesc.appendChild(biblFull);
			
			String titleBook = this.metadata.get("title");
			if(titleBook != null) {
				Element titleStmtBib = document.createElement("titleStmt");
				biblFull.appendChild(titleStmtBib);
				Element titleBib = document.createElement("title");
				titleStmtBib.appendChild(titleBib);
				if(this.metadata.get("subtitle") != null) {
					titleBook += " " + this.metadata.get("subtitle");
				}
				titleBib.appendChild(document.createTextNode(titleBook));
				titleText.setTextContent(this.titleH + titleBook);
				
				String authors = this.metadata.get("authors");
				if(authors != null) {
					if(authors.contains("|")) {
						String[] authorsNames = authors.split("|");
						for(String singleAuthor: authorsNames) {
							if(!singleAuthor.equals("")) {
								Element author = document.createElement("author");
								author.appendChild(document.createTextNode(singleAuthor));
								titleStmtBib.appendChild(author);
							}
						}
					} else {
						Element author = document.createElement("author");
						author.appendChild(document.createTextNode(authors));
						titleStmtBib.appendChild(author);
					}
				}
			}
			
			
			String publisherBook = this.metadata.get("publisher");
			if(publisherBook != null) {
				Element publicationStmtBook = document.createElement("publicationStmt");
				biblFull.appendChild(publicationStmtBook);
				Element publisherBookElement = document.createElement("publisher");
				publisherBookElement.appendChild(document.createTextNode(publisherBook));
				publicationStmtBook.appendChild(publisherBookElement);
				
				String dateBook = this.metadata.get("publish_date");
				if(dateBook != null) {
					Element dateBookElement = document.createElement("date");
					dateBookElement.setAttribute("when", dateBook);
					publicationStmtBook.appendChild(dateBookElement);
				}
				
			}
			
		}
		
		
	}
	
	private void addFront() {
		Element front = document.createElement("front");
		root.appendChild(front);
		
		Element section = document.createElement("div");
		section.setAttribute("type", "contents");
		front.appendChild(section);
		
		Element topLevel = document.createElement("list");
		section.appendChild(topLevel);
		
		for(TOCResource r: this.tocLogical.getChildren()) {
			addTOCRecursive(topLevel, r);
		}
	
	}
	
	private void addTOCRecursive(Element list, TOCResource r) {
		Element tocElement = document.createElement("item");
		tocElement.appendChild(document.createTextNode(r.getTitle()));
		if(r.getType() != TOCResourceType.BACK) {
			Element ref = document.createElement("ref");
			ref.appendChild(document.createTextNode(String.valueOf(r.getPage())));
			tocElement.appendChild(ref);
			
			int id = this.findID(r.getTitle());
			if(id != -1) {
				this.setTarget(ref, id);
			}
		}
		list.appendChild(tocElement);
		List<TOCResource> children = r.getChildren();
		if(children.size() > 0) {
			Element nextLevel = document.createElement("list");
			list.appendChild(nextLevel);
			for(TOCResource c: children) {
				addTOCRecursive(nextLevel, c);
			}
		}
		
	}
	
	private void addBack() {
		Element back = document.createElement("back");
		root.appendChild(back);
		
		Element section = document.createElement("div");
		section.setAttribute("type", "index");
		back.appendChild(section);
		
		Element topLevel = document.createElement("list");
		section.appendChild(topLevel);
		
		HashMap<String, Pair<Element, Element>> elements = new HashMap<String, Pair<Element, Element>> ();
		
		for(IndexElement indexTerm : this.index) {
			Element indexLabel = null;
			if(indexTerm.getLabel() != null && indexTerm.getLabel() != "") {
				indexLabel = document.createElement("label");
				indexLabel.appendChild(document.createTextNode(indexTerm.getLastPart()));
			}
			Element indexElement = document.createElement("item");
			indexElement.appendChild(document.createTextNode(indexTerm.getLastPart()));
			List<Integer> pageNumbers = indexTerm.getPageNumbers();
			List<Integer> pageSegments = indexTerm.getPageSegments();
			for(int pN = 0; pN< pageNumbers.size(); pN++) {
				int page = pageNumbers.get(pN);
				int segment = pN < pageSegments.size() ? pageSegments.get(pN) : -1;
				Element ref = document.createElement("ref");
				ref.appendChild(document.createTextNode(String.valueOf(page)));
				indexElement.appendChild(ref);
				if(segment != -1) {
					this.setTarget(ref, segment - 1);
				}
			}
			Element indexElementList = document.createElement("list");
			elements.put(indexTerm.getKey(), Pair.of(indexElement, indexElementList));
			IndexElement indexParent = indexTerm.getParent();
			if(indexParent == null) {
				if(indexLabel != null) {
					topLevel.appendChild(indexLabel);
				}
				topLevel.appendChild(indexElement);
			} else {
				Pair<Element, Element> indexParentElements = elements.get(indexParent.getKey());
				if(indexLabel != null) {
					indexParentElements.getRight().appendChild(indexLabel);
				}
				indexParentElements.getRight().appendChild(indexElement);
			}
		}
		
		for(Pair<Element, Element> entry: elements.values()) {
			if(entry.getRight().hasChildNodes()) {
				entry.getLeft().appendChild(entry.getRight());
			}
		}
		
	}
	
	private void addBody() {
		Element body = document.createElement("body");
		root.appendChild(body);
		
		Stack<Element> sections = new Stack<Element>();
		
		for(int i=0; i < segmentsData.size(); i++) {
			SegmentData s = segmentsData.get(i);
		
			/*TESTING*/
//			System.out.println("> " + s.getTitle()+ " ID: " + s.getChapterID() + " PID: " + s.getParagraphID() + " H: " + s.getHierarchy());	
//			System.out.println("\tTitleStart: " + s.getChapterMedatada().getPageStartIndex() + " startLine: " + s.getChapterMedatada().getTitleLineStart());
//			System.out.println("\tstartP: " + s.getChapterMedatada().getPageStartIndex() + " startLine: " + s.getChapterMedatada().getLineStart());		
//			if(i > 2) {
//				return;
//			}
			/*TESTING*/
			
			//check if we must stop for index
			if(s.getChapterMedatada().getPageStartIndex() >= firstIndexPage) {
				break;
			}
			
			//create section
			Element section = document.createElement("div");
			section.setAttribute("n", String.valueOf(s.getHierarchy()));
			this.setID(section, s.getChapterID());
			if(sections.size() != 0) {
				int peekH = Integer.valueOf(sections.peek().getAttribute("n"));
				if(peekH == s.getHierarchy()) {
					sections.pop();
				} else if (peekH > s.getHierarchy()) {
					boolean continueToPop = true;
					while(sections.size() > 0 && continueToPop) {
						peekH = Integer.valueOf(sections.peek().getAttribute("n"));
						if(peekH == s.getHierarchy())
							continueToPop = false;
						sections.pop();
					}
				}
			}
			
			//id level 1, add to body
			if(s.getHierarchy() == 1) {
				body.appendChild(section);
				section.setAttribute("type", "chapter");
			} else {
				sections.peek().appendChild(section);
				section.setAttribute("type", "section");
			}
			
			sections.add(section);
			
			//PAGE BEGINNING
			if(s.getChapterMedatada().getTitleLineStart() == 0) {
				appendPB(section, s.getChapterMedatada().getPageStart(), s.getChapterMedatada().getPageStartIndex());
			}
			
			//HEAD
			Element head = document.createElement("head");
			section.appendChild(head);
			
			//get title of section 
			Page currentPage = pages.get(s.getChapterMedatada().getPageStartIndex());
			if(s.getChapterMedatada().getTitleLineStart() != -1) {
				for(int lineIndex=s.getChapterMedatada().getTitleLineStart(); lineIndex <= s.getChapterMedatada().getLineStart(); lineIndex++) {
					appendLB(head, false);
					appendWords(head, currentPage.getLineAt(lineIndex));
				}
			}
			
			//#####################################################################################################################################
			//get content of section
			int startPageIndex = s.getChapterMedatada().getPageStartIndex();
			int endPageIndex = s.getChapterMedatada().getPageEndIndex();
			int startLine = s.getChapterMedatada().getLineStart()+1;
			int endLine= s.getChapterMedatada().getLineEnd();
			
			if(i != segmentsData.size() -1) {
				SegmentData nextS = segmentsData.get(i+1);
				endPageIndex = nextS.getChapterMedatada().getPageStartIndex();
				endLine = nextS.getChapterMedatada().getTitleLineStart() -1;
				if(endLine < 0) {
					endPageIndex = GeneralUtils.getValidPreviousPage(nextS.getChapterMedatada().getPageStartIndex() -1, pages);
					endLine = pages.get(endPageIndex).size()-1;
				}
			} 
			
//			/*TESTING*/
//			System.out.println("\tendP: " + endPageIndex + " endLine: " + endLine);
//			/*TESTING*/
			
			List<Line> lines = new ArrayList<Line>();
			List<PageBreak> pageBreaks = new ArrayList<PageBreak>();	
			
			int lineIndex = 0;
			for(int p = startPageIndex; p <= endPageIndex ; p++ ){
				if (pages.get(p) == null) {
					continue;
				}
				int beginningLine = 0;
				int endingLine = pages.get(p).size()-1;
				if(lineIndex != 0) {
					pageBreaks.add(new PageBreak(pages.get(p).getPageNumber(),pages.get(p).getPageIndex(), lineIndex));
				}

				if(p == startPageIndex) {
					beginningLine = startLine;
				}
					
				if(p ==endPageIndex){
					if(endingLine>endLine)
						endingLine = endLine;
				}

				if(pages.get(p)!=null && pages.get(p).size()>0) {
					for(int j = beginningLine; j<=endingLine; j++) {
						lines.add(pages.get(p).getLineAt(j));
						lineIndex++;
					}
				}
				
				if(p == startPageIndex && s.getChapterMedatada().getChapterHierarchy() == 1) {
					int before = lines.size();
					ContentExtractor.removeCopyRightLines(lines, s.getChapterMedatada().getPageStart());
					int after = lines.size();
					lineIndex -= (before - after);	
				}
			}
			
			//multicolumn
			boolean withColumns = false;
			List<ColumnedPage> linesAsColumns = null;
			ColumnExtractor columnExtractor = null;
			if(WordListCheck.isExerciseSection(s.getChapterMedatada().getPageTitle()) || WordListCheck.isAppendixSection(s.getChapterMedatada().getPageTitle()) || WordListCheck.containsIndex(s.getChapterMedatada().getPageTitle())) {
				columnExtractor = new ColumnExtractor(lines);
				withColumns = columnExtractor.identifyColumns();
				if(withColumns) {
					List<Integer> pageBreakLines = new ArrayList<Integer>();
					for(PageBreak pB: pageBreaks) {
						pageBreakLines.add(pB.getLinePos());
					}
					linesAsColumns = columnExtractor.asColumnedPage(pageBreakLines);
				} 
			}
			
			
			if(!withColumns) {
				List<TextBlock> textBlocks = FormatExtractor.identifyTextBlocksV2(this.formattingDictionary, lines);
				int lIndex = 0;
				for(TextBlock textBlock : textBlocks) {
					//is start of page?
					PageBreak pB = searchPB(pageBreaks, lIndex);
					if(pB != null)
						appendPB(section, pB.getPageNumber(), pB.getPageIndex());
					
					//create text block
					Element ab = document.createElement("ab");
					setID(ab);
					section.appendChild(ab);
					
					lines = textBlock.getLines();
					for(int l =0; l < lines.size(); l++, lIndex++) {
						
						boolean isLineBroken = false;
						if((l-1) >= 0) {
							isLineBroken = hyphenResolver.isLineBroken(lines.get(l-1), lines.get(l));
						}
						appendLB(ab, isLineBroken);
						appendWords(ab, lines.get(l));
					}
				}
				
			} else {
				Element ab = document.createElement("ab");
				setID(ab);
				section.appendChild(ab);
				boolean first = true;
				int pageIndex = 0;
				for(ColumnedPage cP: linesAsColumns) {
					if(!first) {
						appendPB(ab, pageBreaks.get(pageIndex).getPageNumber(), pageBreaks.get(pageIndex).getPageIndex());
						pageIndex++;
						
					}
					
					int currColumn = 1;
					for(Vector<Line> column : cP.getColumns()) {
						appendCB(ab, currColumn);
						for(int l =0; l < column.size(); l++) {
							boolean isLineBroken = false;
							if((l-1) >= 0) {
								isLineBroken = hyphenResolver.isLineBroken(column.get(l-1), column.get(l));
							}
							appendLB(ab, isLineBroken);
							appendWords(ab, column.get(l));
						}
						currColumn++;
					}
					first = false;
				}
			}
			
			
			//#####################################################################################################################################
		}
		
	}
	
	private PageBreak searchPB(List<PageBreak> pageBreaks , int lineIndex) {
		for(PageBreak pB: pageBreaks) {
			if(pB.getLinePos() == lineIndex) {
				return pB;
			}
		}
		return null;
	}
	
	private String getDate() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");  
	    Date date = new Date();  
	    return formatter.format(date);  
	}
	
	private int nextID() {
		int curr = id;
		id++;
		return curr;
	}
	
	private void setID(Element element) {
		element.setAttribute("xml:id", String.valueOf(nextID()));
	}
	
	private void setID(Element element, int target) {
		element.setAttribute("xml:id", "seg_"+ String.valueOf(target));
	}
	
	private void setTarget(Element element, int target) {
		element.setAttribute("target", "seg_"+ String.valueOf(target));
	}
	
	private int findID(String title) {
		for(int i=0; i < segmentsData.size(); i++) {
			if(segmentsData.get(i).getTitle().equals(title)) {
				return segmentsData.get(i).getChapterID();
			}
		}
		return -1;
	}
	
	private void appendLB(Element element, boolean brokeWord) {
		Element lb = document.createElement("lb");
		setID(lb);
		element.appendChild(lb);
		if(brokeWord) {
			lb.setAttribute("break", "no");
		}
	}
	
	private void appendPB(Element element, int pageNumber, int pageIndex) {
		Element pb = document.createElement("pb");
		pb.setAttribute("n", String.valueOf(pageNumber));
		pb.setAttribute("source", String.valueOf(pageIndex));
		element.appendChild(pb);
	}
	
	private void appendCB(Element element, int number) {
		Element cb = document.createElement("cb");
		cb.setAttribute("n", String.valueOf(number));
		element.appendChild(cb);
	}
	
	private void appendWords(Element element, Line l) {
		for(Text w: l.getWords()) {
			Element word = document.createElement("w");
			setID(word);
			word.appendChild(document.createTextNode(w.getText()));
			element.appendChild(word);
		}
	}
	
	
	
	public static void main(String args[]) {
		//TEIBuilder b = new TEIBuilder("/tmp/tei.xml", "Walpole");
		//b.construct();
		//b.store();
	}

}
