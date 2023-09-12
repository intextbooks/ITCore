package intextbooks.content.extraction.buildingBlocks.structure;

import java.util.ArrayList;
import java.util.List;

public class TOCLogical {
	List<TOCResource> children;
	String title;
	List<Integer> pages;
	
	public TOCLogical() {
		this.children = new ArrayList<TOCResource>();
		title = "";
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public void setPages(List<Integer> pages) {
		this.pages = pages;
	}
	
	public List<Integer> getPages() {
		return this.pages;
	}
	
	public boolean isTOCIndexPage(int i) {
		return pages.contains(i);
	}
	
	public int getLastTOCPage() {
		return pages.get(pages.size()-1);
	}
	
	public int size() {
		return this.children.size();
	}
	
	public int totalSize() {
		int total = 0;
		for(TOCResource resource: children) {
			total += resource.totalSize();
		}
		return total;
	}
	
	public void addChildren(TOCResource children) {
		this.children.add(children);
	}
	
	public List<TOCResource> getChildren() {
		return this.children;
	}
	
	public TOCResource findResource(String title) {
		for(TOCResource resource: children) {
			TOCResource result = resource.findResource(title);
			if(result != null)
				return result;
		}
		return null;
	}
	
	private String toStringHelper(List<TOCResource> list, String tab) {
		String finalString = "";
		tab += "\t";
		for(TOCResource r : list) {
			finalString += tab + r.toString() + "\n";
			if(r.size() != 0) {
				finalString += toStringHelper(r.getChildren(), tab);
			}
		}
		return finalString;
	}
	
	private String toStringHelperWithContent(List<TOCResource> list, String tab) {
		String finalString = "";
		tab += "\t";
		for(TOCResource r : list) {
			finalString += tab + r.toStringWithContent() + "\n";
			if(r.size() != 0) {
				finalString += toStringHelperWithContent(r.getChildren(), tab);
			}
		}
		return finalString;
	}


	@Override
	public String toString() {
		String finalString = "";
		String tab = "";
		for(TOCResource r : children) {
			finalString += r.toString() + "\n";
			if(r.size() != 0) {
				finalString += toStringHelper(r.getChildren(), tab);
			}
		}
		return title + "\n" + finalString;
	}
	
	public String toSectionString() {
		return toString().replace("\t", "");
	}
	
	public String toStringWithContent() {
		String finalString = "";
		String tab = "";
		for(TOCResource r : children) {
			finalString += r.toStringWithContent() + "\n";
			if(r.size() != 0) {
				finalString += toStringHelperWithContent(r.getChildren(), tab);
			}
		}
		return finalString;
	}
	
	private List<TOCResourceLine> asTOCResoruceLinesAux(List<TOCResource> list, int level) {
		List<TOCResourceLine> lines = new ArrayList<TOCResourceLine>();
		int childrenLevel = level + 1;
		for(TOCResource r : list) {
			TOCResourceLine tmpLine = new TOCResourceLine(r.getType(), r.getSimpleVersion(), level);
			lines.add(tmpLine);
			if(r.size() != 0) {
				lines.addAll(asTOCResoruceLinesAux(r.getChildren(), childrenLevel));
			}
		}
		return lines;
	}
	
	public List<TOCResourceLine> asTOCResourceLines(){
		List<TOCResourceLine> lines = new ArrayList<TOCResourceLine>();
		int level = 2;
		for(TOCResource r : children) {
			TOCResourceLine tmpLine = new TOCResourceLine(r.getType(), r.getSimpleVersion(), 1);
			lines.add(tmpLine);
			if(r.size() != 0) {
				lines.addAll(asTOCResoruceLinesAux(r.getChildren(), level));
			}
		}
		
		return lines;
	}
}
