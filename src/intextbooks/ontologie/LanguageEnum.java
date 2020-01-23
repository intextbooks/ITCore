package intextbooks.ontologie;

import java.util.ArrayList;
import java.util.List;

public enum LanguageEnum {
	
	GERMAN("GERMAN"), 
	ENGLISH("ENGLISH"), 
	FRENCH("FRENCH"),
	SPANISH("SPANISH"), 
	DUTCH("DUTCH"), 
	RUSSIAN("RUSSIAN");
	
    private final String value;

    private LanguageEnum(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getValue();
    }
    
    public String getLanguageCode(){
    	return getValue();
    }
    
    public String getShortendLanguageCode(){
    	String code;
    	
    	switch(this.value){
	    	case "RUSSIAN" : code = "ru";
	    	break;
	    	case "DUTCH" : code = "nl";
	    	break;
	    	case "SPANISH" : code = "es";
	    	break;
    		case "GERMAN" : code = "de";
	    	break;
	    	case "FRENCH" : code = "fr";
	    	break;
	    	case "ENGLISH" : code = "en";
	    	break;
	    	default: code = "en";
    	}
    	
    	return code;
    }
    
    public static LanguageEnum convertToEnum(String s) {
    	
    	LanguageEnum res;
    	
    	switch(s) {
	    	case "RUSSIAN" : res = RUSSIAN;
	    	break;
	    	case "DUTCH" : res = DUTCH;
	    	break;
	    	case "SPANISH" : res = SPANISH;
	    	break;
    		case "GERMAN" : res = GERMAN;
    		break;
    		case "FRENCH" : res = FRENCH;
    		break;
    		case "ENGLISH" : res = ENGLISH;
    		break;
    		default: res = ENGLISH;
    	}
    	
    	return res;
    	
    }
    
    public static LanguageEnum convertShortendCodeToEnum(String s) {
    	
    	LanguageEnum res;
    	
    	switch(s) {
	    	case "ru" : res = RUSSIAN;
	    	break;
	    	case "nl" : res = DUTCH;
	    	break;
	    	case "es" : res = SPANISH;
	    	break;
	    	case "de" : res = GERMAN;
    		break;
    		case "fr" : res = FRENCH;
    		break;
    		case "en" : res = ENGLISH;
    		break;
    		default: res = ENGLISH;
    	}
    	
    	return res;
    	
    }
    
    public static List<LanguageEnum> getOtherLanguages(LanguageEnum lang) {
    	
    	List<LanguageEnum> list = new ArrayList<LanguageEnum>();
    	
    	switch(lang) {
    		case ENGLISH:
    			list.add(FRENCH);
    			list.add(GERMAN);
    			list.add(SPANISH);
    			list.add(DUTCH);
    			break;
    		case GERMAN:
    			list.add(ENGLISH);
    			list.add(FRENCH);
    			list.add(SPANISH);
    			list.add(DUTCH);
    			break;
    		case FRENCH:
    			list.add(ENGLISH);
    			list.add(GERMAN);
    			list.add(SPANISH);
    			list.add(DUTCH);
    			break;
    		case SPANISH:
    			list.add(ENGLISH);
    			list.add(GERMAN);
    			list.add(FRENCH);
    			list.add(DUTCH);
    			break;
    		case DUTCH:
    			list.add(ENGLISH);
    			list.add(GERMAN);
    			list.add(SPANISH);
    			list.add(FRENCH);
    			break;
    		default:
    			list.add(FRENCH);
    			list.add(GERMAN);
    			list.add(SPANISH);
    			list.add(DUTCH);
    	}
    	
    	return list;
    	
    }
    
}
