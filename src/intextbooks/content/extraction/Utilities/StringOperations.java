package intextbooks.content.extraction.Utilities;

import java.util.List;

import intextbooks.content.extraction.buildingBlocks.format.Line;

public class StringOperations {
	
	private static final String regexNumber = "[0-9]+";
	private static final String regexNumberRange = "[0-9]+([–]|[-])[0-9]+"; 
	private static final String regexNumberRangeSplitted = "[0-9]+([–]|[-])"; 
	private static final String regexNumberNumberRange = "[0-9]+[,][0-9]+([–]|[-])[0-9]+"; 
	//private static final String regexRomanNumber = "^(?=[MDCLXVI])M*(C[MD]|D?C{0,3})(X[CL]|L?X{0,3})(I[XV]|V?I{0,3})$";
	//private static final String regexRomanNumberRange = "^(?=[MDCLXVI])M*(C[MD]|D?C{0,3})(X[CL]|L?X{0,3})(I[XV]|V?I{0,3})([–]|[-])(?=[MDCLXVI])M*(C[MD]|D?C{0,3})(X[CL]|L?X{0,3})(I[XV]|V?I{0,3})$";
	private static final String regexRomanNumber = "^(X?X?X?)(IX|IV|V?I?I?I?)$";
	private static final String regexRomanNumberRange = "^(X?X?X?)(IX|IV|V?I?I?I?)([–]|[-])(X?X?X?)(IX|IV|V?I?I?I?)$";
	private static final String regexNoteNumber  = "[0-9]+[nN]{1}[0-9]*";
	private static final String regexNoteParcialNumber  = "[0-9]+[nN]{1}";
	private static final String regexISBN =  "^(?=[0-9X]{10}$|(?=(?:[0-9]+[-●]){3})[-●0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[-●]){4})[-●0-9]{17}$)(?:97[89][-●]?)?[0-9]{1,5}[-●]?[0-9]+[-●]?[0-9]+[-●]?[0-9X]$";
	
	public static String getRegexNumber() {
		return regexNumber;
	}
	
	public static String getRegexNumberRangeSplitted() {
		return regexNumberRangeSplitted;
	}
	
	public static String getRegexNumberRange() {
		return regexNumberRange;
	}
	
	public static String getRegexNumberNumberRange() {
		return regexNumberNumberRange;
	}
	
	public static String getRegexRomanNumber() {
		return regexRomanNumber;
	}
	
	public static String getRegexRomanNumberRange() {
		return regexRomanNumberRange;
	}
	
    public static String getRegexNoteNumber() {
		return regexNoteNumber;
	}
    
    public static String getRegexNoteParcialNumber() {
		return regexNoteParcialNumber;
	}
    
    public static String getRegexISBN() {
    	return regexISBN;
    }
    
    public static String cleanStringForRegex(String text) {
    	return text.replace(",", "").replace(";", "").replace(".", "").replace(" ", "").toUpperCase();
    }

	
	public static double similarity(String s1, String s2) {
       
    	s1 = s1.replaceAll("\\W", "");
    	s2 = s2.replaceAll("\\W", "");
    	
    	if (s1.length() < s2.length()) { // s1 should always be bigger
            String swap = s1; s1 = s2; s2 = swap;
        }
        int bigLen = s1.length();
        if (bigLen == 0) { return 1.0; /* both strings are zero length */ }
        return (bigLen - computeEditDistance(s1, s2)) / (double) bigLen;
    }

    public static int computeEditDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0)
                    costs[j] = j;
                else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1))
                            newValue = Math.min(Math.min(newValue, lastValue),
                                    costs[j]) + 1;
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0)
                costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }
    
    public static boolean isIndexPageNumber(String token) {
    	
    	return token.matches(regexNoteNumber);
    }
    
    public static boolean isIndexPageNumber(Line line) {
    	List<String> seeList = WordListCheck.getSeeList();
    	for(int i = 0; i < line.size(); i++) {
    		String tmpText = line.getWordAt(i).getText();
    		tmpText = tmpText.replaceAll("[,.]", "").replaceAll(" ","");
    		if(seeList.contains(tmpText.toLowerCase())) {
    			break;
    		}
    		if(!tmpText.matches(regexNoteNumber)) {
    			return false;
    		}
    	}
    	return true;
    }

    public static void printDistance(String s1, String s2) {
        System.out.println(s1 + "-->" + s2 + ": " +
                    computeEditDistance(s1, s2) + " ("+similarity(s1, s2)+")");
    }
	    
	    
	   
	
}
