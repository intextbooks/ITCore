package intextbooks.content.enrichment.books;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import intextbooks.content.extraction.buildingBlocks.format.Page;

public class WriterReader {
	//TODO use configutation
	static String path = "repository/content/pages/";
	
	public static boolean writeBookPages(String name, List<Page> resourcePages) {
		try {
			System.out.println("Writing BookPages: " + name + " to disk ...");
			FileOutputStream fileOut;
			ObjectOutputStream out;
			
			//writing BookPages
			fileOut = new FileOutputStream(path + name + ".pages.ser");
			out = new ObjectOutputStream(fileOut);
			BookPages bookPages = new BookPages(resourcePages);
			out.writeObject(bookPages);
			out.close();
			fileOut.close();
			System.out.println("BookPages: " + name + " written to disk successfully");
			return true;
		} catch (Exception e) {
			System.out.println("There was a problem while writing BookPages: " + name + " to disk");
			e.printStackTrace();
			return false;
		}
	}
	
	public static BookPages readBookPages(String name) {
		try {
			//read
			System.out.println("Reading BookPages: "+ name + " from disk ...");
			FileInputStream fileIn;
			ObjectInputStream in;
			fileIn = new FileInputStream(path + name + ".pages.ser");
			in = new ObjectInputStream(fileIn);
			BookPages bookPages = (BookPages) in.readObject();
			in.close();
	        fileIn.close();
	        System.out.println("BookPages: " + name + " read from disk successfully");
			return bookPages;
		} catch (Exception e) {
			System.out.println("There was a problem while reading BookPages: " + name + " from disk");
			e.printStackTrace();
			return null;
		}
	}
	
	public static void main (String args[]) {
		BookPages b = WriterReader.readBookPages("dekking_index");
		System.out.println(b.getTextFromPage(42));
	}
}
