package examples;

import intextbooks.TextbookProcessor;
import intextbooks.ontologie.LanguageEnum;

public class ProcessingExample {

	public static void main(String args[]) {
		TextbookProcessor.processFullTextbook("pah/to/textbook.pdf", LanguageEnum.ENGLISH, false, false, null, false, false, "email@email.com");
	}
}
