package intextbooks.content.extraction.buildingBlocks.format;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import intextbooks.content.extraction.structure.TableOfContentsExtractor;

public class Paragraph {
	private List <Line> lines = new ArrayList<Line> ();
	
	public void addLine(Line line) {
		line.extractText();
		this.lines.add(line);
	}
	
	public List <Line> getLines() {
		return this.lines;
	}
	
	public void clean() {
		Iterator<Line> it = lines.iterator();
		while(it.hasNext()) {
			Line line = it.next();
			if(line.size() == 0) {
				it.remove();
			}
		}
	}

	@Override
	public String toString() {
		return "Paragraph [lines=" + lines + "]";
	}
}
