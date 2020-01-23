package intextbooks.tools.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class QuickFileWriter {
	
	File file;
    PrintWriter printWriter;
	
	public QuickFileWriter(String fileName) {
		file = new File(fileName);
	    try {
			printWriter = new PrintWriter(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void write(String text) {
		printWriter.println(text);
	}
	
	
	public void close() {
		if(printWriter != null) {
			printWriter.close();
		}
	}
	
	public static void main(String args[]) {
		QuickFileWriter f = new QuickFileWriter("test2019.txt");
		f.write("hola");
		f.close();
	}
	
}
