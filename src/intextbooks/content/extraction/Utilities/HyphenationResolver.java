package intextbooks.content.extraction.Utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

import intextbooks.Configuration;
import intextbooks.content.extraction.buildingBlocks.format.Line;
import intextbooks.ontologie.LanguageEnum;
import net.davidashen.text.Hyphenator;
import net.davidashen.text.RuleDefinition;
import net.davidashen.text.Utf8TexParser;
import net.davidashen.text.Utf8TexParser.TexParserException;

public class HyphenationResolver {
	
	public final static char hyphen = '\u00ad';
	public final static char normalHyphen = '-';
	private static HyphenationResolver hyphenatorEN;
	private static HyphenationResolver hyphenatorGE;
	private static HyphenationResolver hyphenatorFR;
	private static HyphenationResolver hyphenatorES;
	private static HyphenationResolver hyphenatorNL;
	
	private Hyphenator hyphenator;
	private Hyphenator hyphenator2 = null;
	
	private HyphenationResolver(LanguageEnum lang) throws TexParserException, IOException {
		InputStream inputStream, inputStream2;
		switch (lang) {
        	case ENGLISH:
        		String fullPath = Configuration.getInstance().getEnglishTexPattern();
        		String path1 = fullPath.substring(0, fullPath.indexOf("|"));
        		String path2 = fullPath.substring(fullPath.indexOf("|") + 1);
        		inputStream = null;
			try {
				inputStream = new FileInputStream(path1);
			} catch (Exception e) {
				File f = new File(path1);
				System.out.print("# " + f.getAbsolutePath());
				e.printStackTrace();
				//System.exit(0);
			}
        		inputStream2 = new FileInputStream(path2);
        		Charset utf8 = Charset.forName("UTF-8");
        		Utf8TexParser parser = new Utf8TexParser();
        		final InputStreamReader ruleFileReader = new InputStreamReader(inputStream2, utf8);
        		RuleDefinition r = parser.parse(ruleFileReader);
        		hyphenator2 =new Hyphenator();
        		hyphenator2.setRuleSet(r);
        		ruleFileReader.close();
        		inputStream2.close();
				break;
			case GERMAN:
				inputStream = new FileInputStream(Configuration.getInstance().getGermanTexPattern());
				break;
			case FRENCH:
				inputStream = new FileInputStream(Configuration.getInstance().getFrenchTexPattern());
				break;
			case SPANISH:
				inputStream = new FileInputStream(Configuration.getInstance().getSpanishTexPattern());
				break;
			case DUTCH:
				inputStream = new FileInputStream(Configuration.getInstance().getDutchTexPattern());
				break;
			default:
				inputStream = new FileInputStream(Configuration.getInstance().getEnglishTexPattern());
		}
		Charset utf8 = Charset.forName("UTF-8");
		Utf8TexParser parser = new Utf8TexParser();
		final InputStreamReader ruleFileReader = new InputStreamReader(inputStream, utf8);
		RuleDefinition r = parser.parse(ruleFileReader);
		hyphenator =new Hyphenator();
		hyphenator.setRuleSet(r);
		ruleFileReader.close();
		inputStream.close();
	}
	
	public static HyphenationResolver getInstance(LanguageEnum lang) {
		try {
			switch (lang) {
				case ENGLISH:
					if(hyphenatorEN == null) {
						hyphenatorEN = new HyphenationResolver(lang);
					}
					return hyphenatorEN;
				case GERMAN:
					if(hyphenatorGE == null) {
						hyphenatorGE = new HyphenationResolver(lang);
					}
					return hyphenatorGE;
				case FRENCH:
					if(hyphenatorFR == null) {
						hyphenatorFR = new HyphenationResolver(lang);
					}
					return hyphenatorFR;
				case SPANISH:
					if(hyphenatorES == null) {
						hyphenatorES = new HyphenationResolver(lang);
					}
					return hyphenatorES;
				case DUTCH:
					if(hyphenatorNL == null) {
						hyphenatorNL = new HyphenationResolver(lang);
					}
					return hyphenatorNL;
				default:
					if(hyphenatorEN == null) {
						hyphenatorEN = new HyphenationResolver(lang);
					}
					return hyphenatorEN;
			}
		} catch (TexParserException | IOException e) {
			return null;
		}
	}
	
	public String hyphenateWordWithHyphen(String word) {
		return hyphenator.hyphenate(word).replace(hyphen, '-');
	}
	
	public boolean hyphenatedWord(String originalWordP1, String originalWordP2) {
		if(originalWordP1.charAt(originalWordP1.length()-1) == normalHyphen) {
			return hyphenatedWord(originalWordP1.replace('-', hyphen) + originalWordP2);
		} else {
			return false;
		}
	}
	
	public boolean hyphenatedWord(String originalWord) {
		
		int hyphenPos = originalWord.indexOf(hyphen);
		if(hyphenPos == -1) {
			return false;
		}
		String hyphenatedWord = hyphenator.hyphenate(originalWord.substring(0, hyphenPos) + originalWord.substring(hyphenPos+1));
//		System.out.println("Original word: " + originalWord.replace(hyphen, '-'));
//		System.out.println("One word: " + originalWord.substring(0, hyphenPos) + originalWord.substring(hyphenPos+1));
//		System.out.println("hyphenatedWord: " + hyphenatedWord.replace(hyphen, '-'));
//		System.out.println("pos: " + hyphenPos);
		
		boolean nextCheck = false;
		String leftPart = "";
		String rightPart = "";
		int currentChars = 0;
		for(int i = 0; i < hyphenatedWord.length(); i++) {
			if(currentChars < hyphenPos) {
				if(hyphenatedWord.charAt(i) != hyphen) {
					leftPart += hyphenatedWord.charAt(i);
					currentChars++;
				}
			}else if (currentChars == hyphenPos) {
				if(hyphenatedWord.charAt(i) != hyphen) {
					nextCheck = true;
				}
				currentChars++;
			} else {
				if(hyphenatedWord.charAt(i) != hyphen) {
					rightPart += hyphenatedWord.charAt(i);
				}
				currentChars++;
			}
		}
		
		//Check 
		if(leftPart.equals(originalWord.substring(0, hyphenPos)) && rightPart.equals(originalWord.substring(hyphenPos+1)) && !nextCheck)
			return true;
		else {
			if(hyphenator2 != null) {
				hyphenatedWord = hyphenator2.hyphenate(originalWord.substring(0, hyphenPos) + originalWord.substring(hyphenPos+1));
				leftPart = "";
				rightPart = "";
				currentChars = 0;
				for(int i = 0; i < hyphenatedWord.length(); i++) {
					if(currentChars < hyphenPos) {
						if(hyphenatedWord.charAt(i) != hyphen) {
							leftPart += hyphenatedWord.charAt(i);
							currentChars++;
						}
					}else if (currentChars == hyphenPos) {
						if(hyphenatedWord.charAt(i) != hyphen) {
							return false;
						}
						currentChars++;
					} else {
						if(hyphenatedWord.charAt(i) != hyphen) {
							rightPart += hyphenatedWord.charAt(i);
						}
						currentChars++;
					}
				}
				
				//Check 
				if(leftPart.equals(originalWord.substring(0, hyphenPos)) && rightPart.equals(originalWord.substring(hyphenPos+1)))
					return true;
				else {
					return false;
				}
			} else {
				return false;	
			}
		}
			
	}
	
	public String dehyphenateText(List<Line> lines) {
		StringBuilder text = new StringBuilder();
		int startOfNextLine = 0;
		for(int x = 0; x < lines.size(); x++) {
			Line line = lines.get(x);
			boolean found = false;
			for(int w=startOfNextLine; w< line.size(); w++) {
				if((w+1) == line.size()) {
					String word = line.getWordAt(w).getText();
					if(word.length() > 0 && word.charAt(word.length()-1) == '-') {
						int nextLine = x + 1;
						//while (nextLine )
						if((x+1) < lines.size()) {	
							String wholeWord = word.substring(0, word.length()-1) + hyphen + lines.get(x+1).getWordAt(0).getText();
							if(hyphenatedWord(wholeWord)) {
								text.append(word.substring(0, word.length()-1) + lines.get(x+1).getWordAt(0).getText() + " ");	
								found = true;
							} else {
								text.append(line.getWordAt(w).getText() + " ");
							}
						} else {
							text.append(line.getWordAt(w).getText() + " ");
						}
					} else {
						text.append(line.getWordAt(w).getText() + " ");
					}
				} else {
					text.append(line.getWordAt(w).getText() + " ");
				}
			}
			if(found) {
				startOfNextLine = 1;
			} else {
				startOfNextLine = 0;
			}
			text.append("\n");
		}
		
		return text.toString();
	}
	
	public boolean isLineBroken(Line line1, Line line2) {
		if(line1.size() > 0 && line2.size() > 0) {
			String word = line1.getWordAt(line1.size() - 1).getText();
			if(word.length() > 0 && word.charAt(word.length()-1) == '-') {
				String wholeWord = word.substring(0, word.length()-1) + hyphen + line2.getWordAt(0).getText();
				if(hyphenatedWord(wholeWord)) {
					return true;
				}
				else {
					return false;
				}
			}
		}
		return false;
	}
	
	

	public static void main(String[] args) throws FileNotFoundException, IOException, TexParserException {
		HyphenationResolver h = new HyphenationResolver(LanguageEnum.ENGLISH);
		System.out.println(h.hyphenateWordWithHyphen("denominator"));
		System.out.println(h.hyphenatedWord("pro-", "cess"));
		System.out.println(h.hyphenatedWord("publica-", "tions,"));
		/*System.out.println(h.hyphenatedWord("off\u00adcampus"));
		System.out.println(h.hyphenatedWord("two\u00adyear-old"));
		System.out.println(h.hyphenatedWord("family\u00adowned"));*/
		System.out.println(h.hyphenatedWord("individ\u00adual"));
		System.out.println(h.hyphenatedWord("antic\u00adipated"));
		System.out.println(h.hyphenateWordWithHyphen("process"));
		System.out.println(h.hyphenateWordWithHyphen("individuals"));
	}

}
