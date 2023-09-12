package intextbooks.content.extraction.buildingBlocks.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import intextbooks.SystemLogger;
import intextbooks.content.extraction.buildingBlocks.format.Line;
import intextbooks.content.extraction.buildingBlocks.format.Page;
import intextbooks.content.extraction.structure.IndexExtractor;
import intextbooks.persistence.Persistence;
import intextbooks.tools.utility.GeneralUtils;

public class IndexElement {
	private String key;
	private int databaseId;
	private Integer parentDatabaseId;
	private Integer crossDatabaseId;
	private String label;
	private boolean fullLabel;
	private String parentId;
	private List<String> parts;
	private List<String> permutations;
	private Set<String> nounPhrases;
	private List<Pair<Integer,Integer>> pages; //<pageNumber, pageIndex>
	//private List<Integer> pageNumbers;
	//private List<Integer> pageIndexes;
	private Map<Integer,List<Integer>> pageSegments;
	private Map<Pair<Integer,Integer>, Set<String>> pageToSentence;
	private boolean artificial;
	private IndexElement parent;
	private Byte crossreferenceType; // 0 see / 1 see also
	private String crossreferenceText;
	private IndexElement crossreference;
	private boolean propn;

	public IndexElement(String key, String parentId, List<String> parts, String label, List<Pair<Integer,Integer>> pages,
			 boolean artificial, IndexElement parent) {
		this.key = key;
		this.parentId = parentId;
		this.parts = parts;
		this.label = label;
		this.pages = pages;
		this.pageSegments = new LinkedHashMap<Integer,List<Integer>>();
		this.artificial = artificial;
		this.parent = parent;

		nounPhrases = new HashSet<String>();
		pageToSentence = new HashMap<Pair<Integer,Integer>, Set<String>> ();
		permutations = new ArrayList<String>();
	}
	
	public void addCrossreferenceInformation(String type, String text) {
		if(type != null) {
			crossreferenceType = Byte.valueOf(type);
			crossreferenceText = text;
		}
		
	}
	
	public void setCrossreference(IndexElement crossreference) {
		this.crossreference = crossreference;
	}

	public String getKey() {
		return key;
	}

	public String getNormalizedKey() {
		if(fullLabel)
			return label;
		else
			return key.replace(" <> ", " ");
	}
	
	public String getLastPart() {
		if(parts.size() > 1) {
			return parts.get(parts.size()-1);
		} else {
			return parts.get(0);
		}
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Set<String> getNounPhrases() {
		return nounPhrases;
	}

	public void addNounPhrase(String phrase) {
		this.nounPhrases.add(phrase);
	}

	public Set<String> getSentenceFromPage(Integer pageNumber, Integer segment) {
		return this.pageToSentence.get(Pair.of(pageNumber, segment));
	}

	public void addPageToSentence(Integer pageNumber, Integer segment, String sentence) {
		Set<String> sentences = this.pageToSentence.get(Pair.of(pageNumber, segment));
		if(sentences == null) {
			sentences = new HashSet<String>();
			this.pageToSentence.put(Pair.of(pageNumber, segment), sentences);
		}

		sentences.add(sentence);
	}

	public List<String> getParts() {
		return parts;
	}

	public ArrayList<String> getLastPartAsArrayList() {
		ArrayList<String> last = new ArrayList<String>();
		last.add(this.parts.get(this.parts.size()-1));
		return last;
	}

	public void setParts(List<String> parts) {
		this.parts = parts;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public List<Integer> getPageNumbers() {
		List<Integer> numbers = new ArrayList<Integer>();
		for(Pair<Integer, Integer> page: pages) {
			numbers.add(page.getLeft());
		}
		return numbers;
	}
	
	public List<Integer> getPageIndexes() {
		List<Integer> numbers = new ArrayList<Integer>();
		for(Pair<Integer, Integer> page: pages) {
			numbers.add(page.getRight());
		}
		return numbers;
	}
	
	public void setPages (List<Pair<Integer,Integer>> pages) {
		this.pages = pages;
	}
	
	public List<Pair<Integer,Integer>> getPages() {
		return this.pages;
	}

//	public int getAPageNumber(int position){
//
//		if(position < this.pageNumbers.size() && position >= 0 ){
//			return pageNumbers.get(position);
//		}
//		else{
//			return -1;
//		}
//	}
//
//	public int getAPageIndex(int position){
//
//		if(position < this.pageIndexes.size() && position >= 0 ){
//
//			return pageIndexes.get(position);
//		}
//		else{
//			return -1;
//		}
//	}

	public IndexElement getParent() {
		return parent;
	}

	public void setParent(IndexElement parent) {
		this.parent = parent;
	}

	public Integer getParentDatabaseId() {
		return parentDatabaseId;
	}

	public void setParentDatabaseId(int parentDatabaseId) {
		this.parentDatabaseId = parentDatabaseId;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public boolean isArtificial() {
		return artificial;
	}

	public void setArtificial(boolean artificial) {
		this.artificial = artificial;
	}

	public ArrayList<String> getLabelPermutations() {
		return getPermutations(this.parts);
	}

	public int getDatabaseId() {
		return databaseId;
	}

	public void setDatabaseId(int databaseId) {
		this.databaseId = databaseId;
	}

	public boolean isFullLabel() {
		return fullLabel;
	}

	public void setFullLabel(boolean fullLabel) {
		this.fullLabel = fullLabel;
	}

	public Map<Integer,List<Integer>> getPageSegments() {
		return pageSegments;
	}

	public void setPageSegments(Map<Integer,List<Integer>> pageSegments) {
		this.pageSegments = pageSegments;
	}
	
	public Integer getCrossDatabaseId() {
		return crossDatabaseId;
	}

	public void setCrossDatabaseId(Integer crossDatabaseId) {
		this.crossDatabaseId = crossDatabaseId;
	}

	public boolean hasCrossreference() {
		if(this.crossreferenceType != null) {
			return true;
		} else {
			return false;
		}
	}
	
	public String getCrossreferenceText() {
		return this.crossreferenceText;
	}
	
	public Byte getCrossreferenceType() {
		return this.crossreferenceType;
	}

	public boolean isPropn() {
		return propn;
	}

	public void setPropn(boolean propn) {
		this.propn = propn;
	}

	@Override
	public String toString() {
		return "IndexElement [key=\"" + key + "\", databaseId=" + databaseId + ", parentDatabaseId=" + parentDatabaseId + ", crossDatabaseId=" + crossDatabaseId + ", label=" + label + ", fullLabel="
				+ fullLabel + ", PROPN=" + propn + ", parentKey=" + parentId + ", parts=" + parts + ", permutations=" + permutations
				+ ", nounPhrases=" + nounPhrases + ", pages=" + pages
				+ ", pageSegments=" + pageSegments + ", pageToSentence=" + pageToSentence + ", artificial=" + artificial
				+ ", parent=" + parent
				+ ", cossreferenceType=" + crossreferenceType + ", cossreferenceText=" + crossreferenceText + ", crossreference=" + crossreference + "]";
	}

	private ArrayList<String> getPermutations(List<String> parts) {
	    ArrayList<String> results = new ArrayList<String>();

	    // the base case
	    if (parts.size() == 1) {
	        results.add(parts.get(0));
	        permutations = results;
	        return results;
	    }

	    for (int i = 0; i < parts.size(); i++) {
	        String first = parts.get(i);
	        List<String> remains = new ArrayList<String>();
	        for(int j = 0; j < parts.size(); j++) {
	        	if(j != i) {
	        		remains.add(parts.get(j));
	        	}
	        }

	        ArrayList<String> innerPermutations = getPermutations(remains);

	        for (int j = 0; j < innerPermutations.size(); j++)
	            results.add(first + " " + innerPermutations.get(j));
	    }

	    permutations = results;
	    return results;
	}

	public void storeInDB(String content_id) {
		//Index Catalog
		Persistence p = Persistence.getInstance();
		Integer parent_id = parent != null ? parent.getDatabaseId() : null;
		databaseId = p.addIndexCatalogEntry(content_id, parent_id, key, label, fullLabel, artificial);

		//Index Location
		for(int i = 0; i < this.pages.size(); i++) {
			Pair<Integer, Integer> page = pages.get(i);
			List<Integer> segments = this.pageSegments.get(page.getLeft());
			if(segments != null) {
				for(Integer seg: segments) {
					int location_id = p.addIndexLocationEntry(databaseId, page.getRight(), page.getLeft(), seg);
					//Index Sentence
					Set<String> sentences = this.getSentenceFromPage(page.getLeft(), seg);
					if(sentences != null) {
						for(String sentence : sentences) {
							p.addIndexSentenceEntry(location_id, sentence);
						}
					}
				}
			} else {
				p.addIndexLocationEntry(databaseId, page.getRight(), page.getLeft(), -1);
			}
		}

		//Index Noun
		for(String noun : nounPhrases) {
			p.addIndexNounEntry(databaseId, noun);
		}

		//Index Part
		for(String part : permutations) {
			p.addIndexPartEntry(databaseId, part);
		}
	}
	
	public void updateCrossreference() {
		Persistence p = Persistence.getInstance();
		try {
			p.addCrossreferenceToEntry(this.databaseId, this.crossreferenceType, this.crossreference.databaseId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//SystemLogger.getInstance().log("ERROR UPDATING CROSSREFERENCE: " + this.key + " " + this.getCrossreferenceType() + " " +  this.crossreference);
			//e.printStackTrace();
		}
	}

	// STATIC METHODS *******************************

	public static short findStartOfNumbers(Line line) {
		short counter =0 ;
		//find division
		for(short i=0 ;i<line.size()-1 ; i++){

			if(line.getWordAt(counter).getText().matches("[,]*[0-9]*[,.][,.]*")
					|| StringUtils.isNumeric(line.getWordAt(counter).getText())){

				if(StringUtils.isNumeric(line.getWordAt(counter+1).getText())
						|| line.getWordAt(counter+1).getText().matches("[,]*[0-9]*[,.][,.]*"))
					return counter;
			}

			counter++;
		}
		//final check
		if(line.size() > 0 && counter < line.size() && StringUtils.isNumeric(line.getWordAt(counter).getText().replaceAll("[^0-9]", ""))){
			return counter;
		} else {
			return -1;
		}
	}



	private static short findStartOfNumbers(List<String> line) {
		//find division
		short counter = 0;
		for(;counter<line.size()-1 ; counter++){

			if(line.get(counter).matches("[,]*[0-9]*[,.][,]*")
					|| StringUtils.isNumeric(line.get(counter))){

				if(StringUtils.isNumeric(line.get(counter+1))
						|| line.get(counter+1).matches("[,]*[0-9]*[,.][,]*"))
					return counter;
			}
		}

		//final check
		if(StringUtils.isNumeric(line.get(counter).replaceAll("[^0-9]", ""))){
			return counter;
		} else {
			return -1;
		}
	}

//	public static SortedSet<Integer> findPagesInLines(Vector<Line> lines){
//		SortedSet<Integer> results = new TreeSet<Integer>();
//		for(Line line: lines) {
//			short counter =
//			if(counter == -1) {
//				return results;
//			} else {
//				String buf;
//				for(int i=counter; i<line.size(); i++) {
//					buf = line.getWordAt(i).getText().replaceAll("[^0-9]", "");
//					if(!buf.equals("")){
//						if(StringUtils.isNumeric(buf)) {
//							SystemLogger.getInstance().debug("!@! " + line.getText());
//							try {
//								results.add(Integer.parseInt(buf));
//							} catch (NumberFormatException e) {
//								e.printStackTrace();
//							}
//						}
//					}
//				}
//			}
//		}
//		return results;
//	}


	public static IndexElement createIndexElement(Line line, String[] tokens, List<Page> book) {
		//data variables
		String key = null;
		String parentId = null;
		List<String> parts = new ArrayList<String>();
		List<Pair<Integer, Integer>> pagesList = new ArrayList<Pair<Integer, Integer>>();
		//assign value to data variables
		for(int i = 0; i < tokens.length -1; i++) {
			parts.add(tokens[i]);
		}
		if(parts.size() > 0) {
			parentId = String.join(" <> ", parts);
		}
		//last token
		String lastPart = tokens[tokens.length-1];
		String[] separated = lastPart.trim().split(IndexExtractor.termDividerRegex);
		parts.add(separated[0].trim());
		key = String.join(" <> ", parts);
		if(separated.length == 2) {
			String[] pages = separated[1].trim().split("\\s");
			for(String page:pages) {
				try {
					int pageNumber = Integer.parseInt(page.replaceAll("[;,]", ""));
					int pageIndex = findPageIndexWithPageNumber(book, pageNumber);
					if (pageIndex != -1 && !GeneralUtils.containsPageNumber(pagesList, pageNumber)) {
						pagesList.add(Pair.of(pageNumber, pageIndex));
					}
				} catch (NumberFormatException e) {
					e.printStackTrace();
					continue;
				}
			}
		} else {
			pagesList.add(Pair.of(-1, -1));
		}
		//construct and return the index element
		return new IndexElement(key, parentId, parts, null, pagesList,line.isArtificial(), null);
	}

	private static int findPageIndexWithPageNumber(List<Page> book, int pageNumber){

		for(int i = 0; i<book.size(); i++){

			if(book.get(i) != null && book.get(i).getPageNumber() == pageNumber)
				return i;
		}

		return -1;
	}

	public static void main(String args[]) {
		System.out.println(Integer.parseInt("5,"));
	}

}
