package intextbooks.content.extraction.structure;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import intextbooks.Configuration;
import intextbooks.SystemLogger;
import jep.Jep;
import jep.MainInterpreter;

public class NounExtractor {
	private String pythonScriptPath;
	private Jep jep;
	
	private static NounExtractor instance = null;
	
	public NounExtractor() {
		SystemLogger.getInstance().log("Initializing NounExtractor...");
		try {
			//locate Python script
			pythonScriptPath = Configuration.getInstance().getPythonScriptPath();
			SystemLogger.getInstance().log("pythonScriptPath ok");
			//set local path to JEP library
			if(Configuration.getInstance().getJepLibraryPath() != null && Configuration.getInstance().getJepLibraryPath() != "")
				MainInterpreter.setJepLibraryPath(Configuration.getInstance().getJepLibraryPath());
			//run Python script
			try {		
				this.jep = new Jep();
			} catch (Exception e) {
				SystemLogger.getInstance().log("ERROR NOUN EXTRACTION " + e.getMessage());
			}
			SystemLogger.getInstance().log("jep ok");
			jep.eval("import sys");
			SystemLogger.getInstance().log("sys ok");
			jep.eval("sys.argv = ['']");
			SystemLogger.getInstance().log("sys.argv ok");
			jep.runScript(pythonScriptPath);
			SystemLogger.getInstance().log("runScript ok");
		} catch(Exception e) {
			SystemLogger.getInstance().log("Error while initializing NounExtractor");
			SystemLogger.getInstance().log(e.getLocalizedMessage());
			this.jep = null;
		}
		SystemLogger.getInstance().log("Initialization ended...");
	}
	
	synchronized public static NounExtractor getInstance() {
		if(instance == null) {
			instance = new NounExtractor();
		}
		return instance;
	}
	
	synchronized public Long getTextMatch(String term, ArrayList<String> candidates, String lang) {
		try {
			//get JSON from Python
			Object ret = this.jep.invoke("text_matcher",term, candidates, lang);
			
			Long res = 0L;
			
			try {
				res = (Long) ret;
			} catch (Exception e) {
				res = ((Double) ret).longValue();
			}
			
			return res;
		} catch(Exception e) {
			SystemLogger.getInstance().log("Error getTextMatch for: " + term);
			SystemLogger.getInstance().log(e.getMessage());
			return 0L;
		}
	}
			
	synchronized public JSONArray getJSON(ArrayList<String> termCandidates, ArrayList<String> sentences, String lang, boolean testPROPN) {
		try {
			//get JSON from Python
			Object ret = this.jep.invoke("noun_extractor_main", lang, termCandidates, sentences, testPROPN);
			//parse JSON
            JSONParser parser = new JSONParser();
            JSONArray array = (JSONArray) parser.parse(ret.toString());
            //return
            return array;
	
		} catch(Exception e) {
			SystemLogger.getInstance().log("Error while trying to get the reading label for: " + termCandidates);
			SystemLogger.getInstance().log(e.getMessage());
			SystemLogger.getInstance().log(e.getLocalizedMessage());
			return null;
		}
	}
	
	synchronized public JSONArray getJSON(ArrayList<String> termCandidates, ArrayList<String> sentences, String lang) {
		return getJSON(termCandidates, sentences, lang , false);
	}
			
	public static void main(String[] args) {
		ArrayList<String> termCandidates = new ArrayList<String>();
		ArrayList<String> sentences = new ArrayList<String>();
		String lang = "en";
		
		termCandidates.add("empirical bootstrap simulation for centered sample mean");
		termCandidates.add("for centered sample mean empirical bootstrap simulation");
		
		sentences.add("the empirical bootstrap simulation is described for the centered sample mean, but clearly a similar simulation procedure can be formulated for any(normal- ized) sample statistic.");
		sentences.add("mean for the bootstrap dataset: ¯x∗n−¯xn, where ¯x∗n= x 1+ x 2+···+ x n n . repeat steps 1 and 2 many times.");
		sentences.add("one of efron’s contributions was to point out how to combine the bootstrap with modern computational power.");
		
		NounExtractor ins = NounExtractor.getInstance();
//		System.out.println(ins.getJSON(termCandidates, sentences, lang));
//		System.out.println(ins.getJSON(termCandidates, sentences, lang));
//		System.out.println(ins.getJSON(termCandidates, sentences, lang));
//		System.out.println(ins.getJSON(termCandidates, sentences, lang));
		
		sentences = new ArrayList<String>();
		sentences.add("Probability density function (pdf) conditional");
		sentences.add("joint Probability density . function (pdf)");
//		
		System.out.println(ins.getTextMatch("Joint probability density functions", sentences, lang));

	}
}
