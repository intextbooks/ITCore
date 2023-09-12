package intextbooks.content.extraction.buildingBlocks.structure;

import java.util.ArrayList;
import java.util.List;

public class TOCResource {
	TOCResourceType type;
	String title;
	int page;
	List<TOCResource> children;
	String content;
	
	public TOCResource(TOCResourceType type, String title, int page) {
		this.type = type;
		this.title = title;
		this.page = page;
		this.children = new ArrayList<TOCResource>();
	}
	
	public void addChildren(TOCResource resource) {
		this.children.add(resource);
	}

	public TOCResourceType getType() {
		return type;
	}

	public void setType(TOCResourceType type) {
		this.type = type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public List<TOCResource> getChildren() {
		return this.children;
	}
	
	public int size() {
		return this.children.size();
	}
	
	public int totalSize() {
		int total = 1;
		for(TOCResource resource: children) {
			total += resource.totalSize();
		}
		return total;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	public String getContent() {
		return this.content;
	}

	@Override
	public String toString() {
		//return title + ", type=" + type + ", page=" + page;
		return getSimpleVersion();
	}
	
	public String toStringWithContent() {
		if(page > 0)
			return title + " " + page + " -: " + content;
		else
			return title + " -: " + content;
	}
	
	public String getSimpleVersion() {
		if(page > 0)
			return title + " " + page;
		else
			return title;
	}
	
	public String getSimpleVersion2() {
		if(page > 0)
			return title + " " + page + " " + type;
		else
			return title + " " + type;
	}
	
	public TOCResource findResource(String title) {
		if(this.title.equals(title)) {
			return this;
		} else {
			for(TOCResource resource: children) {
				TOCResource result = resource.findResource(title);
				if(result != null)
					return result;
			}
		}
		return null;
	}
	
	
}
