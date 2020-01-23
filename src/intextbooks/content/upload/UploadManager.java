package intextbooks.content.upload;

import java.io.File;
import java.io.IOException;

import org.apache.commons.fileupload.FileItem;

import intextbooks.Configuration;
import intextbooks.SystemLogger;
import intextbooks.persistence.Persistence;

public class UploadManager {

	private static UploadManager instance = null;
	private String contentFolder;
	
	protected UploadManager() {
		this.contentFolder = Configuration.getInstance().getContentFolder();
	}
	
	
	public static UploadManager getInstance() {
		if(instance == null) {
			instance = new UploadManager();
		}
		return instance;
	}
	


	public String store(FileItem item) throws IOException {

         if (!item.isFormField()) {
        	SystemLogger.getInstance().log("Writing file to server");
         	String fileName = new File(item.getName()).getName();       	
         	return Persistence.getInstance().storeContentFile(fileName, item);
  
         }
         
         return null;
    	
	}


}
