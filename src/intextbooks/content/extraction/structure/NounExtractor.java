package intextbooks.content.extraction.structure;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import intextbooks.Configuration;
import intextbooks.SystemLogger;
import jep.Jep;

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
	
	public static NounExtractor getInstance() {
		if(instance == null) {
			instance = new NounExtractor();
		}
		return instance;
	}
			
	synchronized public JSONArray getJSON(ArrayList<String> termCandidates, ArrayList<String> sentences, String lang) {
		try {
			//get JSON from Python
			Object ret = this.jep.invoke("noun_extractor_main", lang, termCandidates, sentences);
			//parse JSON
            JSONParser parser = new JSONParser();
            JSONArray array = (JSONArray) parser.parse(ret.toString());
            //return
            return array;
	
		} catch(Exception e) {
			SystemLogger.getInstance().log("Error while trying to get the reading label for: " + termCandidates);
			SystemLogger.getInstance().log(e.getMessage());
			return null;
		}
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
		System.out.println(ins.getJSON(termCandidates, sentences, lang));
		System.out.println(ins.getJSON(termCandidates, sentences, lang));
		System.out.println(ins.getJSON(termCandidates, sentences, lang));
		System.out.println(ins.getJSON(termCandidates, sentences, lang));

	}
}
