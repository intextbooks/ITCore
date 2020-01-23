package intextbooks.content.extraction.buildingBlocks.structure;

import java.util.List;
import java.util.Vector;

import intextbooks.content.models.formatting.FormattingDictionary;

public class BookStructure {
	
	private List<IndexElement> index = new Vector <IndexElement> ();
	private TOCLogical toc = new TOCLogical();
	private FormattingDictionary formattingDictionary;
	private List<BookSectionResource> bookContent;
	private String rawText;
	private int numberPages;
	
	private int firstIndexPage;
	
	public List<IndexElement> getIndex() {
		return index;
	}
	public void setIndex(List<IndexElement> index) {
		this.index = index;
	}
	public TOCLogical getToc() {
		return toc;
	}
	public void setToc(TOCLogical toc) {
		this.toc = toc;
	}
	public FormattingDictionary getFormattingDictionary() {
		return formattingDictionary;
	}
	public void setFormattingDictionary(FormattingDictionary formattingDictionary) {
		this.formattingDictionary = formattingDictionary;
	}
	public List<BookSectionResource> getBookContent() {
		return bookContent;
	}
	public void setBookContent(List<BookSectionResource> bookContent) {
		this.bookContent = bookContent;
	}
	public String getRawText() {
		return rawText;
	}
	public void setRawText(String rawText) {
		this.rawText = rawText;
	}
	
	public void setFirstIndexPage(int firstIndexPage) {
		this.firstIndexPage = firstIndexPage;
	}
	public int getFirstIndexPage() {
		return this.firstIndexPage;
	}
	public int getNumberPages() {
		return numberPages;
	}
	public void setNumberPages(int numberPages) {
		this.numberPages = numberPages;
	}
	
	
}
