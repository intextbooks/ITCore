package intextbooks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import intextbooks.persistence.Persistence;



public class SystemLogger {
	private static SystemLogger instance = null;
	
	protected SystemLogger() throws IOException {
		this.debug = false; 
		this.silent = false;
		
		Persistence.getInstance().checkFolder(Configuration.getInstance().getLogFolder());
		openLogFile();
		
	}
	
	public static SystemLogger getInstance() {
		if(instance == null) {
			try {
				instance = new SystemLogger();
			} catch (Exception e) {
				e.printStackTrace(); 
			}
			
		}
		return instance;
	}
	
	
	private boolean debug;
	private static boolean silent;
	private PrintWriter fileWriter;
	private String lastDate;
	
	public void setDebug(boolean mode){
		this.debug = mode;
		//this.debug = false;
	}
	
	public boolean isDebug(){
		return this.debug;
	}
	
	
	public void log(String input){
		String logOutput;
		Thread currentThread = Thread.currentThread();
		String name= currentThread.getName();
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		String dateString = dateFormat.format(date); //2014/08/06 15:59:48
		logOutput = "["+ dateString +"] #"+ name + " #: " +input;
		
		changeLogFile();
		
		if(!this.silent){	
			System.out.println(logOutput);
		}
		
		fileWriter.println(logOutput); 
		fileWriter.flush();
	}
	
	public void debug(String input){
		if(!debug) {
			return;
		}
		String logOutput;
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		String dateString = dateFormat.format(date); //2014/08/06 15:59:48
		logOutput = "["+ dateString +"] DEBUG : " +input;
		
		changeLogFile();
		
		if(!this.silent){	
			System.out.println(logOutput);
		}
		
		fileWriter.println(logOutput); 
		fileWriter.flush();
	}
	
	public void setSilent(boolean mode){
		this.silent = mode;
	}
	
	public void toggleSilent(){
		this.silent = !this.silent;
	}
	
	private void openLogFile(){
		
		try {
			DateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy");
			Date date = new Date();

			File logFile = (new File(Configuration.getInstance().getLogFolder(),"log-"+dateFormat.format(date)+".txt"));
			this.fileWriter = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
			this.lastDate = dateFormat.format(date);
			
		} catch (IOException e) {
			e.printStackTrace(); 
		}

	}
	
	private void changeLogFile(){
		DateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy");
		Date date = new Date();
		String dateString = dateFormat.format(date);
		
		if(!dateString.equals(lastDate)){
			this.fileWriter.println("["+ dateString +"] : CLOSING FILE ...... ");
			this.fileWriter.flush();
			this.fileWriter.close();
			openLogFile();
		}
	}
	
}
