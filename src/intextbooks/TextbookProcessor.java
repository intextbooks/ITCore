package intextbooks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.io.FileUtils;

import intextbooks.Configuration;
import intextbooks.SystemLogger;
import intextbooks.content.ContentManager;
import intextbooks.content.models.BookStatus;
import intextbooks.exceptions.BookWithoutPageNumbersException;
import intextbooks.exceptions.EarlyInterruptionException;
import intextbooks.exceptions.NoIndexException;
import intextbooks.exceptions.NotPDFFileException;
import intextbooks.exceptions.TOCNotFoundException;
import intextbooks.ontologie.LanguageEnum;
import intextbooks.persistence.Persistence;
import intextbooks.tools.utility.MD5Utils;

public class TextbookProcessor {
	List<String> allowedExtensions = Arrays.asList("pdf");
	ContentManager cm;
	
	String path;
	String id;
	LanguageEnum lang;
	boolean processReadingLabels;
	boolean linkWithDBpedia;
	String dbpediaCat;
	boolean linkToExternalGlossary;
	boolean splitTextbook;
	String senderEmail;
	
	String fullFileName;
	String fileName;
	String fileExtension;
	String filePath;
	String checksum;
	
	/**
	 * Constructs a object that can be used to process a textbook to create its knowledge model
	 * 
	 * @param path path to the PDF file of the textbook
	 * @param lang language of the textbook, use the LanguageEnum
	 * @param processReadingLabels true to find the reading labels of the the index terms
	 * @param linkWithDBpedia true to link the index terms to DBpedia resources
	 * @param dbpediaCat DBpedia category of the textbook
	 * @param linkToExternalGlossary true to link tho an external glossary
	 * @param splitTextbook true to split the textbook in PDF segments and TXT files
	 * @param senderEmail email of the user who is creating the kwowledge model
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws NotPDFFileException
	 */
	public TextbookProcessor(String path, LanguageEnum lang, boolean processReadingLabels, boolean linkWithDBpedia, String dbpediaCat, boolean linkToExternalGlossary, boolean splitTextbook, String senderEmail) throws IOException, FileNotFoundException, NotPDFFileException {
		cm = ContentManager.getInstance();
		this.path = path;
		this.lang = lang;
		this.processReadingLabels = processReadingLabels;
		this.linkWithDBpedia = linkWithDBpedia;
		this.linkToExternalGlossary = linkToExternalGlossary;
		this.splitTextbook =  splitTextbook;
		this.senderEmail = senderEmail;
		this.dbpediaCat = dbpediaCat;
		
		//check if the file exists
		File file = new File(this.path);
		if(file.exists() && !file.isDirectory()) {
			//get the filename and extention of textbook
			int posLastBreak = path.lastIndexOf('/');
			this.fullFileName = path.substring(posLastBreak+1);
			int posLastPeriod = this.fullFileName.lastIndexOf('.');
			this.fileName = this.fullFileName.substring(0, posLastPeriod);
			this.fileExtension = this.fullFileName.substring(posLastPeriod+1);
			//check the extension of the file
			if(this.allowedExtensions.contains(this.fileExtension.toLowerCase())){
				//create a new full file name using the date
				Calendar calendar = Calendar.getInstance();
				java.util.Date now = calendar.getTime();
				this.fullFileName = this.fileName + now.getTime() + "." + this.fileExtension;
				//copy the file into the folder for the repository
				this.filePath = Configuration.getInstance().getContentFolder()+this.fullFileName;
				try {
					FileUtils.copyFile(new File(path), new File(filePath));
				} catch (IOException e) {
					throw new  IOException ("There was an error copying the textbook file into the textbooks repository");
				}
				
				this.checksum = MD5Utils.getCheckum(filePath);
				
			} else {
				throw new NotPDFFileException ("Textbook file should be a PDF file");
			}
		} else {
			throw new FileNotFoundException ("Textbook file does not exists");
		}
	}
	
	public void createTextbook()  {
		this.id = cm.createBook(fullFileName, lang, fileName, checksum, "book", null, senderEmail);
	}
	
	public BookStatus processTextbook() throws NullPointerException, TOCNotFoundException, BookWithoutPageNumbersException, NoIndexException {	
		return this.cm.processBook(id, fullFileName, lang, fileName, false, "book", null, processReadingLabels, linkWithDBpedia, dbpediaCat, linkToExternalGlossary, splitTextbook, senderEmail);	
	}
	
	public String getBookId() {
		return this.id;
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public LanguageEnum getLang() {
		return lang;
	}

	public void setLang(LanguageEnum lang) {
		this.lang = lang;
	}

	public boolean isProcessReadingLabels() {
		return processReadingLabels;
	}

	public void setProcessReadingLabels(boolean processReadingLabels) {
		this.processReadingLabels = processReadingLabels;
	}

	public boolean isLinkWithDBpedia() {
		return linkWithDBpedia;
	}

	public void setLinkWithDBpedia(boolean linkWithDBpedia) {
		this.linkWithDBpedia = linkWithDBpedia;
	}

	public boolean isLinkToExternalGlossary() {
		return linkToExternalGlossary;
	}

	public void setLinkToExternalGlossary(boolean linkToExternalGlossary) {
		this.linkToExternalGlossary = linkToExternalGlossary;
	}

	public boolean isSplitTextbook() {
		return splitTextbook;
	}

	public void setSplitTextbook(boolean splitTextbook) {
		this.splitTextbook = splitTextbook;
	}

	public String getSenderEmail() {
		return senderEmail;
	}

	public void setSenderEmail(String senderEmail) {
		this.senderEmail = senderEmail;
	}

	public String getFullFileName() {
		return fullFileName;
	}

	public void setFullFileName(String fullFileName) {
		this.fullFileName = fullFileName;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileExtension() {
		return fileExtension;
	}

	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	/**
	 * Static method to process a textbook to create its knowledge model
	 * 
	 * @param path path to the PDF file of the textbook
	 * @param lang language of the textbook, use the LanguageEnum
	 * @param processReadingLabels true to find the reading labels of the the index terms
	 * @param linkWithDBpedia true to link the index terms to DBpedia resources
	 * @param dbpediaCat DBpedia category of the textbook
	 * @param linkToExternalGlossary true to link tho an external glossary
	 * @param splitTextbook true to split the textbook in PDF segments and TXT files
	 * @param senderEmail email of the user who is creating the knowledge model
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws NotPDFFileException
	 */
	public static void processFullTextbook(String path, LanguageEnum lang, boolean processReadingLabels, boolean linkWithDBpedia, String dbpediaCat, boolean linkToExternalGlossary, boolean splitTextbook, String senderEmail) {
		
		try {
			TextbookProcessor tbProcessor = new TextbookProcessor(path, lang, processReadingLabels, linkWithDBpedia, dbpediaCat, linkToExternalGlossary, splitTextbook,  senderEmail);
			tbProcessor.createTextbook();
			try {
				BookStatus bs  =  tbProcessor.processTextbook();
				if(bs ==  BookStatus.Processed){
					Persistence.getInstance().updateBookStatus(tbProcessor.getBookId(), bs);
					SystemLogger.getInstance().log("RESULT: The textbook was successfully processed and the models are stored in the corresponding folder.");
				} else {
					Persistence.getInstance().updateBookStatus(tbProcessor.getBookId(), bs);
					SystemLogger.getInstance().log("RESULT: The textbook was processed and the models are stored in the corresponding folder, but there was an error and the Enriched Model or part of it was not created succesfully. ");
				}
				
			} catch (NullPointerException e) {
				Persistence.getInstance().updateBookStatus(tbProcessor.getBookId(), BookStatus.Error);
				
				SystemLogger.getInstance().log("RESULT: The textbook was not finished processing due to an unknown error.");
				throw e;
			} catch (TOCNotFoundException e) {
				Persistence.getInstance().updateBookStatus(tbProcessor.getBookId(), BookStatus.TOCNotFound);
				SystemLogger.getInstance().log(e.getLocalizedMessage());
				SystemLogger.getInstance().log("RESULT: The textbook was not finished processing because the textbook does not contain a Table of Contents.");
			} catch (BookWithoutPageNumbersException e) {
				Persistence.getInstance().updateBookStatus(tbProcessor.getBookId(), BookStatus.BookWithoutPageNumbers);
				SystemLogger.getInstance().log(e.getLocalizedMessage());
				SystemLogger.getInstance().log("RESULT: The textbook was not finished processing because the textbook does not contain page numbers.");
			} catch (NoIndexException e) {
				Persistence.getInstance().updateBookStatus(tbProcessor.getBookId(), BookStatus.NoIndex);
				SystemLogger.getInstance().log(e.getLocalizedMessage());
				SystemLogger.getInstance().log("RESULT: The textbook was not finished processing because the textbook does not contain an Index.");
			}
		} catch (FileNotFoundException e) {
			SystemLogger.getInstance().log(e.getLocalizedMessage());
			SystemLogger.getInstance().log("RESULT: The textbook could not be processed because the pdf file does not exist.");
		} catch (IOException e) {
			SystemLogger.getInstance().log(e.getLocalizedMessage());
			SystemLogger.getInstance().log("RESULT: The textbook could not be processed due to an I/O error.");
		} catch (NotPDFFileException e) {
			SystemLogger.getInstance().log(e.getLocalizedMessage());
			SystemLogger.getInstance().log("RESULT: The textbook could not be processed because the file is not in PDF format.");
		}
	}
	
	public static void main(String args[]) {
		//
		///home/alpiz001/Desktop/Walpole_Probability_and_Statistics.pdf
		TextbookProcessor.processFullTextbook("/home/alpiz001/Documents/INTERLINGUA_BOOKS/EduHintOVD/BRICKS-GEO-11-bwt.pdf", LanguageEnum.ENGLISH, false, false, null, false, false, "isaacalpizar@gmail.com");
	}
}
