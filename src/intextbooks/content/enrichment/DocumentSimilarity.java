package intextbooks.content.enrichment;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import intextbooks.ontologie.LanguageEnum;

public class DocumentSimilarity {

	private Directory directory;
	private Analyzer analyzer;
	private IndexWriterConfig iwc;
	private IndexWriter writer;

	private Set<String> terms = new HashSet<String>();
	public RealVector v1;
	public RealVector v2;

	private int currentDocId = -1;
	private FieldType typeStored = new FieldType();

	public DocumentSimilarity(String s1, String s2, LanguageEnum lang) throws IOException {

		this.initializeIndex(lang);
		this.initializeFieldType();

		Directory directory = this.createIndex(s1, s2);
		IndexReader reader = DirectoryReader.open(directory);

		Map<String, Integer> f1 = this.getTermFrequencies(reader, 0);
		Map<String, Integer> f2 = this.getTermFrequencies(reader, 1);

		reader.close();

		this.v1 = this.toRealVector(f1);
		this.v2 = this.toRealVector(f2);

	}

	public DocumentSimilarity(String s) throws IOException {

		this.initializeIndex();
		this.initializeFieldType();

		this.createIndex(s);

	}

	public double getSimilarity(String s) throws IOException {

		this.addDocument(this.writer, s);
		this.writer.commit();
		IndexReader reader = DirectoryReader.open(this.directory);

		Map<String, Integer> f1 = this.getTermFrequencies(reader, 0);
		Map<String, Integer> f2 = this.getTermFrequencies(reader, 1);
		
		reader.close();

		this.v1 = this.toRealVector(f1);
		this.v2 = this.toRealVector(f2);

		Term idToDelete = new Term("docId", "1");
		this.writer.deleteDocuments(idToDelete);

		return this.getCosineSimilarity();

	}

	public static double getSimilarity(String s1, String s2, LanguageEnum lang) throws IOException {
		return new DocumentSimilarity(s1, s2, lang).getCosineSimilarity();
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

	private void initializeIndex() throws IOException {

		this.analyzer = new StandardAnalyzer();
		this.directory = new RAMDirectory();
		this.iwc = new IndexWriterConfig(Version.LUCENE_4_10_2, this.analyzer);
		this.writer = new IndexWriter(this.directory, this.iwc);

	}
	
	private void initializeIndex(LanguageEnum lang) throws IOException {
		
		switch (lang) {
			case ENGLISH:
				this.analyzer = new EnglishAnalyzer();
				break;
			case GERMAN:
				this.analyzer = new GermanAnalyzer();
				break;
			case FRENCH:
				this.analyzer = new FrenchAnalyzer();
				break;
			case SPANISH:
				this.analyzer = new SpanishAnalyzer();
				break;
			case DUTCH:
				this.analyzer = new DutchAnalyzer();
				break;
			default:
				this.analyzer = new StandardAnalyzer();
		}
		
		this.directory = new RAMDirectory();
		this.iwc = new IndexWriterConfig(Version.LUCENE_4_10_2, this.analyzer);
		this.writer = new IndexWriter(this.directory, this.iwc);
		
	}

	private Directory createIndex(String s1, String s2) throws IOException {

		this.addDocument(this.writer, s1);
		this.addDocument(this.writer, s2);

		this.writer.close();

		return this.directory;

	}

	private Directory createIndex(String s) throws IOException {

		this.addDocument(this.writer, s);
		this.writer.commit();
		return this.directory;

	}

	private void addDocument(IndexWriter writer, String content) throws IOException {

		Document doc = new Document();

		Field id = new Field("docId", Integer.toString(this.incrementDocId()), this.typeStored);
		Field field = new Field("content", content, this.typeStored);

		doc.add(id);
		doc.add(field);
		writer.addDocument(doc);

	}

	private double getCosineSimilarity() {
		return (this.v1.dotProduct(this.v2)) / (this.v1.getNorm() * this.v2.getNorm());
	}

	private Map<String, Integer> getTermFrequencies(IndexReader reader, int docId) throws IOException {

		Terms vector = reader.getTermVector(docId, "content");
		TermsEnum termsEnum = null;
		termsEnum = vector.iterator(termsEnum);
		
		Map<String, Integer> frequencies = new HashMap<>();
		BytesRef text = null;

		while ((text = termsEnum.next()) != null) {
			String term = text.utf8ToString();
			int freq = (int) termsEnum.totalTermFreq();
			frequencies.put(term, freq);
			this.terms.add(term);
		}

		return frequencies;

	}

	private RealVector toRealVector(Map<String, Integer> map) {

		RealVector vector = new ArrayRealVector(this.terms.size());
		int i = 0;

		for (String term : this.terms) {
			int value = map.containsKey(term) ? map.get(term) : 0;
			vector.setEntry(i++, value);
		}

		return vector.mapDivide(vector.getL1Norm());

	}

	private int incrementDocId() {

		this.currentDocId = this.currentDocId < 1 ? ++this.currentDocId : this.currentDocId;
		return this.currentDocId;

	}

	public void close() throws IOException {
		this.writer.close();
	}

}
