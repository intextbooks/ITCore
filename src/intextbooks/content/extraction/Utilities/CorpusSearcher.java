package intextbooks.content.extraction.Utilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import intextbooks.SystemLogger;
import intextbooks.content.extraction.ExtractorController.resourceType;
import intextbooks.ontologie.LanguageEnum;
import intextbooks.tools.utility.StringUtils;

public class CorpusSearcher {

	private int hitsPerDoc;
	private float threshold = 0;
	private Directory index;
	private Analyzer analyzer;
	private IndexWriterConfig config;
	private resourceType type;
	private LanguageEnum lang;
	private Map<Integer, List<String>> slideCorpus;
	private List<String> bookCorpus;
	private FieldType typeStored = new FieldType();

	public CorpusSearcher(){
		index = new RAMDirectory();
		initializeFieldType();
	}
	
	private void initIndex(resourceType type, LanguageEnum lang) {
		
		this.type = type;
		this.lang = lang;
		
		if (this.type == resourceType.BOOK)
			this.hitsPerDoc = 10;
		else
			this.hitsPerDoc = this.slideCorpus.size();
		
		switch(lang) {
		case ENGLISH:
			analyzer = new EnglishAnalyzer(CharArraySet.EMPTY_SET);
			break;
		case GERMAN:
			analyzer = new GermanAnalyzer(CharArraySet.EMPTY_SET);
			/*TESTING*/
			SystemLogger.getInstance().log("##Analyzer GERMAN ");
			/*TESTING*/
			break;
		case FRENCH:
			analyzer = new FrenchAnalyzer(CharArraySet.EMPTY_SET);
			break;
		case SPANISH:
			analyzer = new SpanishAnalyzer(CharArraySet.EMPTY_SET);
			/*TESTING*/
			SystemLogger.getInstance().log("##Analyzer SPANISH");
			/*TESTING*/
			break;
		case DUTCH:
			analyzer = new DutchAnalyzer(CharArraySet.EMPTY_SET);
			/*TESTING*/
			SystemLogger.getInstance().log("##Analyzer DUTCH");
			/*TESTING*/
			break;
		default:
			analyzer = new StandardAnalyzer(CharArraySet.EMPTY_SET);
			/*TESTING*/
			SystemLogger.getInstance().log("##Analyzer Standard ");
			/*TESTING*/
		}
		
		if(type.equals(resourceType.BOOK)){
			threshold = 0.7f;
		}
		else if(type.equals(resourceType.SLIDE)){
			threshold = 0.3f;
		}
		
	}

	private void initializeFieldType() {

		this.typeStored.setIndexed(true);
		this.typeStored.setTokenized(true);
		this.typeStored.setStored(true);
		this.typeStored.setStoreTermVectors(true);
		this.typeStored.setStoreTermVectorPositions(true);
		this.typeStored.setStoreTermVectorOffsets(true);
		this.typeStored.freeze();

	}

	/**
	 * Treats whole pages as documents.
	 * 
	 * @param corpus
	 */
	
	public void setCorpus(List<String> corpus, resourceType type, LanguageEnum lang){
		
		this.bookCorpus = corpus;
		this.initIndex(type, lang);
		config = new IndexWriterConfig(Version.LATEST, analyzer);
		
		IndexWriter w;
		try {

			w = new IndexWriter(index, config);

			for(int i=0; i<corpus.size(); i++ )
				addDoc(w, corpus.get(i), String.valueOf(i), null);

			w.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Treats sentences as documents.
	 * 
	 * @param corpus
	 * @param type
	 * @param lang
	 */
	
	public void setCorpus(Map<Integer, List<String>> corpus, resourceType type, LanguageEnum lang){
		
		this.slideCorpus = corpus;
		this.initIndex(type, lang);
		config = new IndexWriterConfig(Version.LATEST, analyzer);
				
		IndexWriter w;
		try {

			w = new IndexWriter(index, config);
			
			for (Map.Entry<Integer, List<String>> corpusEntry : corpus.entrySet()) {
				
				int pageIndex = corpusEntry.getKey();
				int groupIndex = -1;
				List<String> sentences = corpusEntry.getValue();
				
				for (String sentence : sentences) {
					
					groupIndex++;
					
					if (!sentence.isEmpty() && sentence != null)
						addDoc(w, sentence, String.valueOf(pageIndex), String.valueOf(groupIndex));
					
				}
				
			}

			w.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}


	/**
	 * 
	 * @param queryStr
	 * @return
	 */
	public Map<Integer, Integer> corpusContains(String queryStr){

		int queryLength = queryStr.split(" ").length;
		Map<Integer, Integer> resultFreqs = new HashMap<Integer, Integer>();
		
		List<String> tokenizedQuery = new ArrayList<String>();
		String[] queryArray = queryStr.split(" ");
		
		for (String s : queryArray)
			tokenizedQuery.add(Stemming.stemText(this.lang, s));

		try {
				
			QueryParser qp = new QueryParser("text", analyzer);
			qp.setDefaultOperator(QueryParser.Operator.AND);
			Query q = null;
			try {
				q = qp.parse(queryStr);
			} catch (Exception e) {
				/*TESTING*/
				SystemLogger.getInstance().log("Problem with term, QS: " + queryStr + "returning no result");
				/*TESTING*/
				return resultFreqs;
			}
			
			IndexReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);
			TopScoreDocCollector collector = TopScoreDocCollector.create(this.hitsPerDoc, true);
			
			searcher.search(q, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;

			for(int i=0;i<hits.length;++i) {
				if(hits[i].score > threshold){
					
					int docId = hits[i].doc;
					Document d = searcher.doc(docId);
					int pageIndex = Integer.parseInt(d.get("pageIndex"));
					
					// prune candidate results for single term queries according to their pos tag
					if (queryLength == 1) {
						
						int groupIndex = -1;
						
						if (this.type == resourceType.SLIDE)
							groupIndex = Integer.parseInt(d.get("groupIndex"));
						
						String originalText = this.getOriginalText(pageIndex, groupIndex);
						boolean isNoun = POSTagger.isNoun(queryStr, originalText, this.lang);
						
						if (!isNoun)
							continue; // do not consider search result if it is not a noun
						
					}
					
					Map<String, Integer> freqMap = this.getTermFrequencies(reader, docId);
					int minOccurrence = 999999;
					
					// for composite query terms, all constituents have to appear equally often, i.e. the minimal occurrence count of the constituents is the total occurrence count of the composite query
					for (String token : tokenizedQuery) {						
						Integer tokenFreq = freqMap.get(token);
						if (tokenFreq != null)
							minOccurrence = tokenFreq < minOccurrence ? tokenFreq : minOccurrence;						
					}
					
					if (minOccurrence != 999999)
						resultFreqs.put(pageIndex, minOccurrence);
					
				}
			}

			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
		} 

		return resultFreqs;
	}

/**
 * 
 * @param w
 * @param title
 * @param pageIndex
 * @throws IOException
 */
	private void addDoc(IndexWriter w, String title, String pageIndex, String groupIndex) throws IOException {
		
		Document doc = new Document();
		
//		TextField content = new TextField("text", title, Field.Store.YES);
		Field content = new Field("text", title, this.typeStored);
		StringField id = new StringField("pageIndex", pageIndex, Field.Store.YES);
		
		if (this.type.equals(resourceType.SLIDE)) {
			
			int colCount = StringUtils.countOccurrences(title, ":");
			float boost = 1;
			
			if (colCount != 0)
				boost = colCount * 1.5f;
			
			Pattern pattern = Pattern.compile("[A-Z]\\w* [A-Z]");
			Matcher matcher = pattern.matcher(title);
			
			if (matcher.find())
				boost *= 2;
			
			content.setBoost(boost);
			
		}
		
		if (groupIndex != null) {
			StringField groupId = new StringField("groupIndex", groupIndex, Field.Store.YES);
			doc.add(groupId);
		}
		
		doc.add(content);
		doc.add(id);
		
		w.addDocument(doc);
	}
	
	public String getOriginalText(int pageIndex, int groupIndex) {
		
		if (groupIndex == -1)
			return this.bookCorpus.get(pageIndex);
		else
			return this.slideCorpus.get(pageIndex).get(groupIndex);
		
	}
	
	private Map<String, Integer> getTermFrequencies(IndexReader reader, int docID) {
		
		Map<String, Integer> freqMap = new HashMap<String, Integer>();
		
		try {
			
			Terms terms = reader.getTermVector(docID, "text");
			
			if (terms != null && terms.size() > 0) {
				
				TermsEnum termsEnum = terms.iterator(null);
				BytesRef term = null;
				
				while ((term = termsEnum.next()) != null) {
					
					DocsEnum docsEnum = termsEnum.docs(null, null);
					int docIDEnum;
					
					// Iterates only over the one document given by docID since getTermVector returns a single-document inverted index.
					while ((docIDEnum = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS)
						freqMap.put(term.utf8ToString(), docsEnum.freq());
					
				}
				
			}
			
		} catch (IOException e) {
			SystemLogger.getInstance().log(e.toString());
			e.printStackTrace();
		}
		
		return freqMap;
		
	}

}
