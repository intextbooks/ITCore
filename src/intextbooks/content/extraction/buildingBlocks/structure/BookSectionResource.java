package intextbooks.content.extraction.buildingBlocks.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class BookSectionResource {
	List<BookSectionResource> children;
	BookSectionType type;
	String content;
	String fullContent;
	int number;
	
	public BookSectionResource(BookSectionType type, String content, int number, String fullContent) {
		this.type = type;
		this.content = content;
		this.number = number;
		this.children = new ArrayList<BookSectionResource>();
		this.fullContent = fullContent;
	}
	
	public void addChildren(BookSectionResource resource) {
		this.children.add(resource);
	}

	public List<BookSectionResource> getChildren() {
		return children;
	}
	
	public int getChildrenSize() {
		return children.size();
	}


	public void setChildren(List<BookSectionResource> children) {
		this.children = children;
	}

	public BookSectionType getType() {
		return type;
	}

	public void setType(BookSectionType type) {
		this.type = type;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
	
	public String getFullContent() {
		return fullContent;
	}

	public void setFullContent(String content) {
		this.fullContent = content;
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}
	
	public int size() {
		return this.children.size();
	}
	
	
	
	@Override
	public String toString() {
		return "" + type + " " + number + " " + content;
	}
	
	private static void printBookContentRecursive(List<BookSectionResource> resources, String tab) {
		for(BookSectionResource r : resources) {
			System.out.println(tab + r + " : " + r.extractTextContent());
			printBookContentRecursive(r.getChildren(), tab + "\t");
		}
	}

	public static void printBookContent(List<BookSectionResource> resources) {
		//1
		for(BookSectionResource l1 : resources) {
			System.out.println(l1 + " : "  + l1.extractTextContent());
			printBookContentRecursive(l1.getChildren(), "\t");
		}
	}
	
	private static List<BookSectionResourceLine> asBookSectionResourceLinesAux(List<BookSectionResource> resources, Vector<Integer> level, Vector<String> father) {
		List<BookSectionResourceLine> lines = new ArrayList<BookSectionResourceLine>();
		for(BookSectionResource r : resources) {
			if(r.type != BookSectionType.PARAGRAPH) {
				int chap = r.number;
				Vector<Integer> currLevel = new Vector<Integer>(level);
				currLevel.addElement(chap);
				BookSectionResourceLine tmpLine = new BookSectionResourceLine(r.getType(), r.getContent(), currLevel, father);
				Vector<String> currFather = new Vector<String>(father);
				currFather.add(r.getContent());
				lines.add(tmpLine);
				if(r.size() != 0) {
					lines.addAll(asBookSectionResourceLinesAux(r.getChildren(), currLevel, currFather));
				}
			}
		}
		return lines;
	}
	
	public static List<BookSectionResourceLine>  asBookSectionResourceLines(List<BookSectionResource> resources) {
		List<BookSectionResourceLine> lines = new ArrayList<BookSectionResourceLine>();
		for(BookSectionResource r : resources) {
			int chap = r.number;
			BookSectionResourceLine tmpLine = new BookSectionResourceLine(r.getType(), r.getContent(), chap);
			lines.add(tmpLine);
			if(r.size() != 0) {
				Vector<Integer> currLevel = new Vector<Integer>();
				currLevel.addElement(chap);
				Vector<String> currFather = new Vector<String>();
				currFather.add(r.getContent());
				lines.addAll(asBookSectionResourceLinesAux(r.getChildren(), currLevel, currFather));
			}
			chap++;
		}
		
		return lines;
	}
	
	public String extractTextContent() {
		String result = "";
		for(BookSectionResource bookSectionResource: children) {
			if(bookSectionResource.type == BookSectionType.PARAGRAPH) {
				//System.out.println("\t\ta paragraph: " + bookSectionResource.content);
				result += bookSectionResource.content + " ";
			} else {
				result += bookSectionResource.getContent() + " ";
				//System.out.println("\t\ta child: " + bookSectionResource.content);
				String childrenText  = bookSectionResource.extractTextContent();
				//System.out.println("\t\ta childTEXT: " + childrenText);
				if(childrenText != "") {
					result += childrenText + " ";
				}
			}
		}
		//System.out.println("\tText: " +result);
		return result.trim();
	}
	
	public String extractTextContent(int level) {
		String result = "";
		if(level <= 1) {
			System.out.println("\tNode: " + this.content);
		}
		for(BookSectionResource bookSectionResource: children) {
			System.out.println("processing: " + bookSectionResource);
			if(bookSectionResource.type == BookSectionType.PARAGRAPH) {
				//System.out.println("\t\ta paragraph: " + bookSectionResource.content);
				result += bookSectionResource.content + " ";
			} else {
				result += bookSectionResource.getContent() + " ";
				//System.out.println("\t\ta child: " + bookSectionResource.content);
				String childrenText  = bookSectionResource.extractTextContent(level+1);
				System.out.println("\t\ta childTEXT: " + childrenText);
				if(childrenText != "") {
					result += childrenText + " ";
				}
			}
		}
		if(level <= 1) {
			System.out.println("\tText: " +result);
		}

		return result.trim();
	}
}
