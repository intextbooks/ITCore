package intextbooks.persistence;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import intextbooks.SystemLogger;
import intextbooks.content.extraction.ContentExtractor;
import intextbooks.content.extraction.Utilities.WordListCheck;
import intextbooks.content.extraction.buildingBlocks.format.Line;
import intextbooks.content.extraction.buildingBlocks.format.Page;
import intextbooks.content.extraction.buildingBlocks.format.Text;
import intextbooks.content.extraction.buildingBlocks.structure.TOC;
import intextbooks.content.extraction.buildingBlocks.structure.TOCLogical;
import intextbooks.content.extraction.structure.ColumnExtractor;
import intextbooks.content.extraction.structure.ColumnedPage;
import intextbooks.content.extraction.structure.SegmentData;
import intextbooks.content.extraction.structure.SegmentExtractor;
import intextbooks.tools.utility.GeneralUtils;

public class TEIBuilder {
	
	// TEI XMLNS
	final String xmlns = "http://www.tei-c.org/ns/1.0";
	
	//Text constants
	final String titleH = "Textbook Model: ";
	final String publisherT = "intextbooks -- Intelligent Textbooks Project";
	final String projectURL = "https://github.com/intextbooks/ITCore";
	
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
	
	//internal
	int id;
	
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
	
	public TEIBuilder(String textbookTitle, ArrayList<SegmentData> segmentsData, List<Page> pages, SegmentExtractor segmentExtractor, int firstIndexPage, TOCLogical tocLogical) {
		try {
			this.textbookTitle = textbookTitle;
			
			this.segmentsData = segmentsData;
			this.pages = pages;
			this.segmentExtractor = segmentExtractor;
			this.firstIndexPage = firstIndexPage;
			this.tocLogical = tocLogical;
			
			
			DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
			document = documentBuilder.newDocument();
			
			id = 0;
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
	
//	public void store() {
//
//        try {
//			// create the xml file
//			//transform the DOM Object to an XML File
//			TransformerFactory transformerFactory = TransformerFactory.newInstance();
//			Transformer transformer = transformerFactory.newTransformer();
//			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
//			DOMSource domSource = new DOMSource(document);
//			StreamResult streamResult = new StreamResult(new File(this.destinationPath));
//			//StreamResult streamResult = new StreamResult(System.out);
//			transformer.transform(domSource, streamResult);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	
	//******************** PRIVATE
	
	private void addHeader() {
		Element teiHeader = document.createElement("teiHeader");
		root.appendChild(teiHeader);
		
		Element fileDesc = document.createElement("fileDesc");
		teiHeader.appendChild(fileDesc);
		
		Element titleStmt = document.createElement("titleStmt");
		fileDesc.appendChild(titleStmt);
		Element title = document.createElement("title");
		title.appendChild(document.createTextNode(this.titleH + this.textbookTitle));
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
	}
	
	private void addFront() {
		Element front = document.createElement("front");
		root.appendChild(front);
	}
	
	private void addBack() {
		Element back = document.createElement("back");
		root.appendChild(back);
	}
	
	private void addBody() {
		Element body = document.createElement("body");
		root.appendChild(body);
		
		Stack<Element> sections = new Stack<Element>();
		
		for(int i=0; i < segmentsData.size(); i++) {
			SegmentData s = segmentsData.get(i);
			
//			System.out.println("> " + s.getTitle()+ " ID: " + s.getChapterID() + " PID: " + s.getParagraphID() + " H: " + s.getHierarchy());	
//			System.out.println("\tTitleStart: " + s.getChapterMedatada().getPageStartIndex() + " startLine: " + s.getChapterMedatada().getTitleLineStart());
//			System.out.println("\tstartP: " + s.getChapterMedatada().getPageStartIndex() + " startLine: " + s.getChapterMedatada().getLineStart());
//			System.out.println("\tendP: " + s.getChapterMedatada().getPageEndIndex() + " endLine: " + s.getChapterMedatada().getLineEnd());
			
			//if(s.getChapterID() == 11) {
			//	return;
			//}
			
			//check if we must stop for index
			if(s.getChapterMedatada().getPageStartIndex() >= firstIndexPage) {
				break;
			}
			
			//create section
			Element section = document.createElement("div");
			section.setAttribute("n", String.valueOf(s.getHierarchy()));
			this.setID(section);
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
					appendLB(head);
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
				if(endLine <= 1) {
					endPageIndex = GeneralUtils.getValidPreviousPage(nextS.getChapterMedatada().getPageStartIndex() -1, pages);
					endLine = pages.get(endPageIndex).size()-1;
				}
			} 
			
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
			
			Element ab = document.createElement("ab");
			setID(ab);
			section.appendChild(ab);
			if(!withColumns) {
				for(int l =0; l < lines.size(); l++) {
					PageBreak pB = searchPB(pageBreaks, l);
					if(pB != null)
						appendPB(ab, pB.getPageNumber(), pB.getPageIndex());
					appendLB(ab);
					appendWords(ab, lines.get(l));
				}
			} else {
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
							appendLB(ab);
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
	
	private void appendLB(Element element) {
		Element lb = document.createElement("lb");
		setID(lb);
		element.appendChild(lb);
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
