package intextbooks.content.extraction.format;


import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;

import intextbooks.SystemLogger;


public class PdfIntraLinkRemover {

	private PDDocument document;
	private String filePath;
	
	public PdfIntraLinkRemover(String filePath) {
		
		this.filePath = filePath;	
		
		File file = new File(filePath);
		
		try {
			
			this.document = PDDocument.load(file);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}
	}
	
	
	public void clearAll(){
		
		SystemLogger.getInstance().log("Bookmark removal started");
		
		removeBookmarks(true);
		SystemLogger.getInstance().log("Bookmark removal ends");
		
		
		try {
			
			SystemLogger.getInstance().log("Annotation removal started");
			removeAnnotations(true);
			SystemLogger.getInstance().log("Annotation removal ends");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}
		
		overwrite();	
		
	}
	
	public void removeAnnotations(boolean local) throws IOException{
		
		PDPageTree pages = this.document.getPages();
	
		removeAnnotations(pages, local);
	}
	
	private void removeAnnotations(PDPageTree pages, boolean local) throws IOException{
		
		for(int i = 0; i< pages.getCount(); i++){
		
			if(pages.get(i).getAnnotations() != null && !pages.get(i).getAnnotations().isEmpty())
					pages.get(i).getAnnotations().clear();
			
		}
		
		if(!local)
			overwrite();
	}
	
	public void removePageAnnotation(int i){
		
		PDPage page = document.getPages().get(i);
		

		try {
			
			if(page.getAnnotations() != null && !page.getAnnotations().isEmpty()){				
				page.getAnnotations().clear();
				overwrite();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}
	
	}
	
	public void removePageAnnotation(PDPage page){
		
		try {
			
			if(page.getAnnotations() != null && !page.getAnnotations().isEmpty()){
				page.getAnnotations().clear();
				overwrite();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}
		
	}
	
	public void removeBookmarks (boolean local){
		
		   PDDocumentOutline outline =  this.document.getDocumentCatalog().getDocumentOutline();
           
		   if(outline!= null){
		
			   COSDictionary dictionary =   outline.getCOSObject();
           
			   if(dictionary != null){
			
				   dictionary.clear();	
	           
		           if(!local)
		        	   overwrite();
			   }
		   }
           
	}
	
	private void overwrite(){
		
		try {
			document.save(this.filePath);
			document.close();
		} catch (IOException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}
		
	}
	
}
