package intextbooks.content.extraction.structure;

public class SegmentData {
	private String title;
	private int hierarchy;
	private int paragraphId;
	private int chpaterID;
	private ChapterMetaData chapterMedatada;
	private String text;
	
	public SegmentData(String title, int hierarchy, int paragraphId, int chapterId, ChapterMetaData chapterMedatada,
			String text) {
		this.title = title;
		this.hierarchy = hierarchy;
		this.paragraphId = paragraphId;
		this.chpaterID = chapterId;
		this.chapterMedatada = chapterMedatada;
		this.text = text;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getHierarchy() {
		return hierarchy;
	}

	public void setHierarchy(int hierarchy) {
		this.hierarchy = hierarchy;
	}

	public int getParagraphID() {
		return paragraphId;
	}

	public void setParagraphID(int paragraphId) {
		this.paragraphId = paragraphId;
	}

	public int getChapterID() {
		return chpaterID;
	}

	public void setChapterID(int chapterId) {
		this.chpaterID = chapterId;
	}

	public ChapterMetaData getChapterMedatada() {
		return chapterMedatada;
	}

	public void setChapterMedatada(ChapterMetaData chapterMedatada) {
		this.chapterMedatada = chapterMedatada;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
