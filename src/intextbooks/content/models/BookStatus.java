package intextbooks.content.models;

public enum BookStatus {
	
	Created("Created"), 
	TOCNotFound("TOCNotFound"), 
	BookWithoutPageNumbers("BookWithoutPageNumbers"),
	NoIndex("NoIndex"), 
	Error("Error"), 
	Processed("Processed"),
	ProcessedNoDBpedia("ProcessedNoDBpedia"),
	NotPDFFile("NotPDFFile"),
	IOError("IOError");
	
	private final String value;

    private BookStatus(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    
    public String getExplanation() {
    	String explanation = "";
    	switch(this.value){
    		case "Created" : 
    			explanation = "the textbook was successfully uploaded and now it is being processed"; 
    			break;
    		case "NotPDFFile" : 
    		case "IOError" : 
	    		explanation = "the textbook was not uploaded because the file does not exist or due to an IO error"; 
	    		break;
	    	case "Processed" : 
	    		explanation = "the textbook was successfully processed and the models were created"; 
	    		break;
	    	case "ProcessedNoDBpedia" : 
	    		explanation = "the textbook was processed and the models were created, but the DBPedia category was not valid and the index terms were not linked"; 
	    		break;
	    	case "TOCNotFound" : 
	    		explanation = "the textbook does not contain a Table of Contents"; 
	    		break;
	    	case "BookWithoutPageNumbers" :
	    		explanation = "the textbook does not contain page numbers"; 
	    		break;
			case "NoIndex" : 
				explanation = "the textbook does not contain an Index"; 
	    		break;
	    	case "Error" : 
	    	default:
	    		explanation = "there was an unknown error while processing the textbook (it has been logged and reported)"; 
	    		break;
    	}
    	return explanation;
    }

    @Override
    public String toString() {
        return getValue();
    }

}
