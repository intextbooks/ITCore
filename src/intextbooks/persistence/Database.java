package intextbooks.persistence;

import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import intextbooks.Configuration;
import intextbooks.SystemLogger;
import intextbooks.content.ContentManager;
import intextbooks.content.models.BookStatus;
import intextbooks.content.models.formatting.CoordinatesContainer;
import intextbooks.content.models.formatting.lists.ListingContainer;
import intextbooks.ontologie.LanguageEnum;

public class Database {

	private static Database instance = null;
	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver"; 
	static final String DB_URL = Configuration.getInstance().getDatabaseURL();
	static final String USER = Configuration.getInstance().getDbUser();
	static final String PASS = Configuration.getInstance().getDbPasswd();
	private static SystemLogger logger = SystemLogger.getInstance();
	private Connection conn;
	private PreparedStatement preStmt;
	private ComboPooledDataSource dataSource;
	private long lastConRefresh;
	
	
	
	
	protected Database() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			
			dataSource = new ComboPooledDataSource();
			dataSource.setDriverClass(JDBC_DRIVER);
			dataSource.setJdbcUrl(DB_URL);
			dataSource.setUser(USER);
			dataSource.setPassword(PASS); 
			
			this.lastConRefresh = Calendar.getInstance().getTime().getTime();
			
		} catch (ClassNotFoundException | PropertyVetoException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		} 
	}
	
	public static Database getInstance() {
		if(instance == null) {
			instance = new Database();
		}
		return instance;
	}
	
	synchronized public String getFileName(String bookID) {
		try {
			conn = getConnectionFromPool();

			preStmt = conn.prepareStatement("SELECT filename FROM content WHERE id=?");
			preStmt.setString(1, bookID);
			ResultSet res = preStmt.executeQuery();
			
			if(res.next())
				return res.getString("filename");	
			else
				return null;
				
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
		return null;
	}
	
	synchronized public String getType(String bookID) {
		try {
			conn = getConnectionFromPool();

			preStmt = conn.prepareStatement("SELECT type FROM content WHERE id=?");
			preStmt.setString(1, bookID);
			ResultSet res = preStmt.executeQuery();
			
			if(res.next())
				return res.getString("type");	
			else
				return null;
				
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
		return null;
	}
	
	synchronized public String getLanguage(String bookID) {
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT lang FROM content WHERE id=?");
			preStmt.setString(1, bookID);
			ResultSet res = preStmt.executeQuery();

			if(res.next())
				return res.getString("lang");	
			else
				return null;
				
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
		return null;
	}
	
	synchronized public String getBookName(String bookID) {
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT name FROM content WHERE id=?");
			preStmt.setString(1, bookID);
			ResultSet res = preStmt.executeQuery();

			if(res.next())
				return res.getString("name");	
			else
				return null;
				
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
		return null;
	}
	
	synchronized public ArrayList<String> getBookList(){
		ArrayList<String> bookList = new ArrayList<String>();
		
		try {
			conn = getConnectionFromPool();
			String sqlCreate = "CREATE TABLE IF NOT EXISTS content" +
					"(id VARCHAR(255) not NULL, " +
					" name VARCHAR(255) not NULL, " +
					" type VARCHAR(255) not NULL, " +
	                " filename VARCHAR(255) not NULL, " +
	                " uri VARCHAR(255) not NULL, " +
	                " lang VARCHAR(255),"+
	                " grouping VARCHAR(255),"+
	                " PRIMARY KEY ( id ))";
	
			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate(sqlCreate);
			
			
			ResultSet res = preStmt.executeQuery("SELECT * FROM content");
	
			while(res.next())
				bookList.add(res.getString("id"));
				
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return new ArrayList<String>();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
			
		return bookList;
	}
	
	synchronized public ArrayList<String> getGroupList(){
		ArrayList<String> groupList = new ArrayList<String>();
		
		try {
			conn = getConnectionFromPool();
			String sqlCreate = "CREATE TABLE IF NOT EXISTS groups" +
					"(id VARCHAR(255) not NULL, " +
					" name VARCHAR(255) not NULL, " +
					" language VARCHAR(255) not NULL, " +
	                " PRIMARY KEY ( id ))";
	
			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate(sqlCreate);
			
			
			ResultSet res = preStmt.executeQuery("SELECT * FROM groups");
	
			while(res.next())
				groupList.add(res.getString("id"));
				
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return new ArrayList<String>();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
			
		return groupList;
	}
	
	synchronized public String getGroupingName(String groupID) {
		try {
			conn = getConnectionFromPool();
			String sqlCreate = "CREATE TABLE IF NOT EXISTS groups" +
					"(id VARCHAR(255) not NULL, " +
					" name VARCHAR(255) not NULL, " +
					" language VARCHAR(255) not NULL, " +
	                " PRIMARY KEY ( id ))";
	
			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate(sqlCreate);
			
			preStmt = conn.prepareStatement("SELECT * FROM groups WHERE id=? ");
			preStmt.setString(1, groupID);
			ResultSet res = preStmt.executeQuery();
	
			while(res.next())
				return res.getString("name");
				
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return "";
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
			
		return "";
	}
	
	synchronized public String getGroupingLanguage(String groupID) {
		try {
			conn = getConnectionFromPool();
			String sqlCreate = "CREATE TABLE IF NOT EXISTS groups" +
					"(id VARCHAR(255) not NULL, " +
					" name VARCHAR(255) not NULL, " +
					" language VARCHAR(255) not NULL, " +
	                " PRIMARY KEY ( id ))";
	
			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate(sqlCreate);
			
			preStmt = conn.prepareStatement("SELECT * FROM groups WHERE id=? ");
			preStmt.setString(1, groupID);
			ResultSet res = preStmt.executeQuery();
	
			while(res.next())
				return res.getString("language");
				
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return "";
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
			
		return "";
	}
	
	synchronized public ArrayList<String> getGroupMembers(String groupID){
		ArrayList<String> groupList = new ArrayList<String>();
		
		try {
			conn = getConnectionFromPool();

			preStmt = conn.prepareStatement("SELECT id FROM content WHERE grouping = ?");
			preStmt.setString(1, groupID);
			ResultSet res = preStmt.executeQuery();

			while(res.next())
				groupList.add(res.getString("id"));
				
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return new ArrayList<String>();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
			
		return groupList;
	}
	
	synchronized public boolean removeBook(String bookID, Set<String> bookList) {
		try {
			conn = getConnectionFromPool();
			String sqlDrop = "DROP TABLE IF EXISTS "+bookID+"_formatMap ";
			preStmt = conn.prepareStatement(sqlDrop);
			preStmt.executeUpdate(sqlDrop);
			preStmt.close();
			
			sqlDrop = "DROP TABLE IF EXISTS "+bookID+"_indexMap ";
			preStmt = conn.prepareStatement(sqlDrop);
			preStmt.executeUpdate(sqlDrop);
			preStmt.close();
			
			sqlDrop = "DROP TABLE IF EXISTS "+bookID+"_relations ";
			preStmt = conn.prepareStatement(sqlDrop);
			preStmt.executeUpdate(sqlDrop);
			preStmt.close();
			
			sqlDrop = "DROP TABLE IF EXISTS "+bookID+"_highlightMap ";
			preStmt = conn.prepareStatement(sqlDrop);
			preStmt.executeUpdate(sqlDrop);
			preStmt.close();
			
			sqlDrop = "DROP TABLE IF EXISTS "+bookID+"_listingMap ";
			preStmt = conn.prepareStatement(sqlDrop);
			preStmt.executeUpdate(sqlDrop);
			preStmt.close();
			
			sqlDrop = "DELETE FROM content WHERE id=?";
			preStmt = conn.prepareStatement(sqlDrop);
			preStmt.setString(1, bookID);
			preStmt.execute();
			preStmt.close();
			
			sqlDrop = "DELETE FROM _indexCatalog WHERE content_id=?";
			preStmt = conn.prepareStatement(sqlDrop);
			preStmt.setString(1, bookID);
			preStmt.execute();
			preStmt.close();
			
			Iterator<String> iter = bookList.iterator();
			while(iter.hasNext()){
				String currBook = iter.next();
				if(!currBook.equals(bookID)){
					sqlDrop = "DELETE FROM "+currBook+"_relations WHERE toBook=?";
					preStmt = conn.prepareStatement(sqlDrop);
					preStmt.setString(1, bookID);
					preStmt.execute();
					preStmt.close();
				}
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
		return true;
	}

	
	synchronized public void createGrouping(String id, String name, LanguageEnum lang) {
		try {
			conn = getConnectionFromPool();
			
			preStmt = conn.prepareStatement("INSERT INTO groups VALUES(?,?,?)");
			preStmt.setString(1, id);
			preStmt.setString(2, name);
			preStmt.setString(3, lang.toString());
			
			preStmt.executeUpdate();
			preStmt.close();
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
	}
	
	synchronized public void createFormatMap(String parentBook){		
		try {
			conn = getConnectionFromPool();
			String sqlDrop = "DROP TABLE IF EXISTS "+parentBook+"_formatMap ";
			preStmt = conn.prepareStatement(sqlDrop);
			preStmt.executeUpdate(sqlDrop);
			preStmt.close();
			
			String sqlCreate = "CREATE TABLE "+parentBook+"_formatMap " +
	                "(id VARCHAR(255) not NULL, " +
	                " pageIndex INTEGER not NULL, " +
	                " line INTEGER not NULL, " +
	                " position INTEGER not NULL, " +
	                " format VARCHAR(255), " + 
	                " leftTopX DOUBLE, " + 
	                " leftTopY DOUBLE, " +
	                " rightBottomX DOUBLE, " +
	                " rightBottomY DOUBLE, " +
	                " PRIMARY KEY ( id ))";
			
			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate(sqlCreate);
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
	}
	
	synchronized public void createRelationMap(String parentBook){
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("DROP TABLE IF EXISTS "+parentBook+"_relations");
			preStmt.executeUpdate();
			preStmt.close();
			
			String sqlCreate = "CREATE TABLE IF NOT EXISTS "+parentBook+"_relations"+
					"(fromSegment INTEGER not NULL, " +
	                " toBook VARCHAR(255) not NULL, " +
					" language VARCHAR(255) not NULL, " +
	                " weight DOUBLE not NULL, " +
	                " toSegment VARCHAR(255) not NULL)";
	
			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate(sqlCreate);
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
	}
	
	synchronized public void createIndexMap(String parentBook){
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("DROP TABLE IF EXISTS "+parentBook+"_indexMap ");
			preStmt.executeUpdate();
			preStmt.close();
			
			String sql = "CREATE TABLE "+parentBook+"_indexMap " +
	                "(indexName VARCHAR(255) not NULL, " +
	                " segment INTEGER not NULL, " +
	                " pageIndex INTEGER not NULL, " +
	                " conceptName VARCHAR(255)) ";
			
			preStmt = conn.prepareStatement(sql);
			//preStmt.setString(1, parentBook+"_indexMap ");
			preStmt.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
	}
	
	synchronized public void createIndexModel(String parentBook){
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("DROP TABLE IF EXISTS "+parentBook+"_indexLocation");
			preStmt.executeUpdate();
			preStmt.close();
			
			preStmt = conn.prepareStatement("DROP TABLE IF EXISTS "+parentBook+"_indexCatalog");
			preStmt.executeUpdate();
			preStmt.close();
			
			String sql = "CREATE TABLE "+parentBook+"_indexCatalog " +
	                "(id int(11) NOT NULL AUTO_INCREMENT, " + 
	                "parent_id int(11) DEFAULT NULL, " + 
	                "key_name varchar(1000) NOT NULL, " + 
	                "label varchar(1000) NOT NULL, " + 
	                "concept_name varchar(1000) DEFAULT NULL, " + 
	                "PRIMARY KEY (id))";
			
			preStmt = conn.prepareStatement(sql);
			preStmt.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
		}
		
		try {
			String sql = "CREATE TABLE "+parentBook+"_indexLocation " +
	                "(index_id int(11) NOT NULL, " + 
	                "page int(11) NOT NULL, " + 
	                "segment int(11) NOT NULL, " + 
	                "PRIMARY KEY (index_id,page), " + 
	                "CONSTRAINT fk__indexLoc_1 FOREIGN KEY (index_id) REFERENCES " + parentBook+"_indexCatalog (id) ON DELETE CASCADE ON UPDATE CASCADE)";
			
			preStmt = conn.prepareStatement(sql);
			preStmt.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
	}
	
	//----------------------- NEW INDEX TABLE: IndexElement -----------------------\\
	synchronized public int addIndexCatalogEntry(String content_id, Integer parent_id, String key, String label, boolean full_name, boolean artificial) {
		int databaseId = 0;
		try {
			conn = getConnectionFromPool();
			String[] returnId = { "id" };
			preStmt = conn.prepareStatement("INSERT INTO _indexCatalog (content_id, parent_id, key_name, label, full_label, artificial) VALUES(?,?,?,?,?,?)", returnId);
			preStmt.setString(1, content_id);
			if(parent_id != null)
				preStmt.setInt(2, parent_id);
			else
				preStmt.setNull(2, java.sql.Types.INTEGER);
			preStmt.setString(3, key);
			if(label != null)
				preStmt.setString(4, label);
			else
				preStmt.setNull(4, java.sql.Types.VARCHAR);
			preStmt.setBoolean( 5, full_name);
			preStmt.setBoolean(6, artificial);
			preStmt.executeUpdate();
			try (ResultSet generatedKeys = preStmt.getGeneratedKeys()) {
	            if (generatedKeys.next()) {
	            	databaseId = generatedKeys.getInt(1);
	            }
	        }
			preStmt.close();
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		return databaseId;
	}
	
	synchronized public void addIndexLocationEntry(Integer index_id, Integer page_index, Integer page_number, Integer segment) {
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("INSERT INTO _indexLocation (index_id, page_index, page_number, segment) VALUES(?,?,?,?)");
			preStmt.setInt(1, index_id);
			preStmt.setInt(2, page_index);
			preStmt.setInt(3, page_number);
			preStmt.setInt(4, segment);
			preStmt.executeUpdate();
			preStmt.close();
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
	}
	
	synchronized public int addIndexTextEntry(String text) {
		int databaseId = 0;
		try {
			conn = getConnectionFromPool();
			String[] returnId = { "text_id" };
			preStmt = conn.prepareStatement("INSERT INTO _indexText (text) VALUES(?)", returnId);
			preStmt.setString(1, text);
			preStmt.executeUpdate();
			try (ResultSet generatedKeys = preStmt.getGeneratedKeys()) {
	            if (generatedKeys.next()) {
	            	databaseId = generatedKeys.getInt(1);
	            }
	        }
			preStmt.close();
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		return databaseId;
	}
	
	synchronized public void addIndexNounEntry(Integer index_id, String noun_text) {
		int noun_id = 0;
		try {
			conn = getConnectionFromPool();
			String[] returnId = { "text_id" };
			preStmt = conn.prepareStatement("INSERT INTO _indexText (text) VALUES(?)", returnId);
			preStmt.setString(1, noun_text);
			preStmt.executeUpdate();
			try (ResultSet generatedKeys = preStmt.getGeneratedKeys()) {
	            if (generatedKeys.next()) {
	            	noun_id = generatedKeys.getInt(1);
	            }
	        }
			preStmt = conn.prepareStatement("INSERT INTO _indexNoun (index_id, noun_id) VALUES(?,?)");
			preStmt.setInt(1, index_id);
			preStmt.setInt(2, noun_id);
			preStmt.executeUpdate();
			preStmt.close();
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
	}
	
	synchronized public void addIndexPartEntry(Integer index_id, String part_text) {
		int part_id = 0;
		try {
			conn = getConnectionFromPool();
			String[] returnId = { "text_id" };
			preStmt = conn.prepareStatement("INSERT INTO _indexText (text) VALUES(?)", returnId);
			preStmt.setString(1, part_text);
			preStmt.executeUpdate();
			try (ResultSet generatedKeys = preStmt.getGeneratedKeys()) {
	            if (generatedKeys.next()) {
	            	part_id = generatedKeys.getInt(1);
	            }
	        }
			preStmt = conn.prepareStatement("INSERT INTO _indexPart (index_id, part_id) VALUES(?,?)");
			preStmt.setInt(1, index_id);
			preStmt.setInt(2, part_id);
			preStmt.executeUpdate();
			preStmt.close();
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
	}
	
	synchronized public void addIndexSentenceEntry(Integer index_id, Integer page_index, String sentence_text) {
		int sentence_id = 0;
		try {
			conn = getConnectionFromPool();
			String[] returnId = { "text_id" };
			preStmt = conn.prepareStatement("INSERT INTO _indexText (text) VALUES(?)", returnId);
			preStmt.setString(1, sentence_text);
			preStmt.executeUpdate();
			try (ResultSet generatedKeys = preStmt.getGeneratedKeys()) {
	            if (generatedKeys.next()) {
	            	sentence_id = generatedKeys.getInt(1);
	            }
	        }
			preStmt = conn.prepareStatement("INSERT INTO _indexSentence (index_id, page_index, sentence_id) VALUES(?,?,?)");
			preStmt.setInt(1, index_id);
			preStmt.setInt(2, page_index);
			preStmt.setInt(3, sentence_id);
			preStmt.executeUpdate();
			preStmt.close();
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
	}
	
	synchronized public Map<Integer, Set<String>> getPagesAndSentecesForIndex(String content_id) {
		Map<Integer, Set<String>> map = new HashMap<Integer,Set<String>>();
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT loc.page_index, text.text\n" + 
					"FROM _indexCatalog as cat\n" + 
					"INNER JOIN _indexLocation as loc on (loc.index_id = cat.id)\n" + 
					"INNER JOIN _indexSentence as sen ON (loc.index_id = sen.index_id and loc.page_index = sen.page_index)\n" + 
					"INNER JOIN _indexText as text on (sentence_id = text.text_id)\n" + 
					"WHERE  loc.page_index != -1\n" + 
					"AND cat.content_id = ?;");
			preStmt.setString(1, content_id);
			ResultSet res = preStmt.executeQuery();
			while(res.next()) {
				Integer page = res.getInt("page_index");
				String sentence = res.getString("text");
				
				Set<String> sentencesSet = map.get(page);
				if(sentencesSet == null) {
					sentencesSet =  new HashSet<String>();
					map.put(page, sentencesSet);
				}
				sentencesSet.add(sentence);
			}
			return map;
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
			return null;
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
	}
	
	//------------------------------------------------------------------------------\\
	
	synchronized public void createHighlightMap(String parentBook){
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("DROP TABLE IF EXISTS "+parentBook+"_highlightMap ");
			preStmt.executeUpdate();
			preStmt.close();
			
			String sql = "CREATE TABLE "+parentBook+"_highlightMap " +
					"(pageIndex INTEGER not NULL, " +
					" conceptName VARCHAR(255) not NULL, " +
					" topLeftX DOUBLE not NULL, " +
					" topLeftY DOUBLE not NULL, " +
					" bottomRightX DOUBLE not NULL, " +
					" bottomRightY DOUBLE not NULL, " +
					" UNIQUE(pageIndex, topLeftX, topLeftY, bottomRightX, bottomRightY))" ;
					
			preStmt = conn.prepareStatement(sql);
			preStmt.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
	}
	
	synchronized public void createListingMap(String parentBook){
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("DROP TABLE IF EXISTS "+parentBook+"_listingMap ");
			preStmt.executeUpdate();
			preStmt.close();
			
			String sql = "CREATE TABLE "+parentBook+"_listingMap " +
					"(listID INTEGER not NULL, " +
					" listingID INTEGER NOT NULL, " +
					" pageIndex INTEGER NOT NULL, " +
					" segmentID INTEGER, " +
					" topY DOUBLE NOT NULL, " +
					" bottomY DOUBLE NOT NULL, " + 
					" PRIMARY KEY (listingID))" ;
					
			preStmt = conn.prepareStatement(sql);
			preStmt.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
	}
	
	synchronized public void addListing(String bookID, ListingContainer listing) {		
		try {
			
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("INSERT INTO "+bookID+"_listingMap VALUES(?,?,?,?,?,?)");
			preStmt.setInt(1, listing.getListID());
			preStmt.setInt(2, listing.getListingID());
			preStmt.setInt(3, listing.getPageIndex());
			preStmt.setInt(4, listing.getSegmentID());
			preStmt.setDouble(5, listing.getTopY());
			preStmt.setDouble(6, listing.getBottomY());
			
			preStmt.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
			logger.log(e.toString());
		} finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
	}
	
	synchronized public ArrayList<ListingContainer> getListingsInSegment(String bookID, int segmentID) {
		ArrayList<ListingContainer> result = new ArrayList<ListingContainer>();
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT * FROM "+bookID+"_listingMap WHERE segmentID = ?");
			preStmt.setInt(1, segmentID);
			ResultSet res = preStmt.executeQuery();
	
			while(res.next()) {
				
				int listID = res.getInt("listID");
				int listingID = res.getInt("listingID");
				int pageIndex = res.getInt("pageIndex");
				double topY = res.getDouble("topY");
				double bottomY = res.getDouble("bottomY");
				
				result.add(new ListingContainer(listID, listingID, pageIndex, segmentID, topY, bottomY));
				
			}
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return new ArrayList<ListingContainer>();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
		
		return result;
	}
	
	synchronized public ArrayList<ListingContainer> getListingsOnPage(String bookID, int pageIndex) {
		ArrayList<ListingContainer> result = new ArrayList<ListingContainer>();
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT * FROM "+bookID+"_listingMap WHERE pageIndex = ?");
			preStmt.setInt(1, pageIndex);
			ResultSet res = preStmt.executeQuery();
	
			while(res.next()) {
				
				int listID = res.getInt("listID");
				int listingID = res.getInt("listingID");
				int segmentID = res.getInt("segmentID");
				double topY = res.getDouble("topY");
				double bottomY = res.getDouble("bottomY");
				
				result.add(new ListingContainer(listID, listingID, pageIndex, segmentID, topY, bottomY));
				
			}
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return new ArrayList<ListingContainer>();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
		
		return result;
	}
	
	synchronized public void addIndexElement(String bookID, String indexName,List<Integer> segments, List<Integer> indices, List<Integer> pages, boolean artificial){
		try {
			int id = addIndexCatalogEntry(bookID, null, indexName, indexName, true, artificial);
			
			for(byte counter = 0; counter < indices.size(); counter++){
				Integer segment = Integer.valueOf(segments.get(counter));
				Integer index = Integer.valueOf(indices.get(counter));
				Integer page = Integer.valueOf(pages.get(counter));
				logger.log("Adding index ELEMENT " + index + " -- " + indexName);
				
				addIndexLocationEntry(id, index, page, segment);

			}
		} catch (Exception e) {
			e.printStackTrace();
			SystemLogger.getInstance().log(e.toString()); 
		}	
	}
	
	/*OLD*/
	synchronized public void addIndex(String bookID, String indexName,List<Integer> segments, ArrayList<Integer> indices){
		Iterator<Integer> iter = indices.iterator();
		byte counter = 0;
		
		try {
			conn = getConnectionFromPool();
			while(iter.hasNext()){
				String segment = String.valueOf(segments.get(counter));
				String index = String.valueOf(iter.next());
				logger.log("Adding index " + index + " -- " + indexName);
				preStmt = conn.prepareStatement("INSERT INTO "+bookID+"_indexMap VALUES(?,?,?, null)");
				preStmt.setString(1, indexName);
				preStmt.setString(2, segment);
				preStmt.setString(3, index);
				
				preStmt.executeUpdate();
				preStmt.close();
				
				counter++;
			}
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
	}
	
	/*OLD*/
	synchronized public void addIndex(String bookID, String indexName, ArrayList<Integer> indices){
		Iterator<Integer> iter = indices.iterator();
		byte counter = 0;
		
		try {
			conn = getConnectionFromPool();
			while(iter.hasNext()){
				
				String index = String.valueOf(iter.next());
				logger.log("Adding index " + index + " -- " + indexName);
				preStmt = conn.prepareStatement("INSERT INTO "+bookID+"_indexMap VALUES(?, 0,?, null)");
				preStmt.setString(1, indexName);
				preStmt.setString(2, index);
				
				preStmt.executeUpdate();
				preStmt.close();
				
				counter++;
			}
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
	}
	
	/*OLD*/
//	public void addConceptToIndexTerm(String bookID, String indexTerm, String conceptName){
//		try {
//			conn = getConnectionFromPool();
//			logger.log("Adding ConceptName " + conceptName + " to Index: " + indexTerm);
//			preStmt = conn.prepareStatement("UPDATE "+bookID+"_indexMap SET conceptName=? WHERE indexName=?");
//			
//			preStmt.setString(1, conceptName);
//			preStmt.setString(2, indexTerm);
//			
//			preStmt.executeUpdate();
//		} catch (SQLException e) {
//			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
//		}finally{
//			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
//			 try { if (conn != null) conn.close(); } catch (Exception e) {};
//		}
//	}
	
	synchronized public void addConceptToIndexElement(String bookID, String indexElementKey, String conceptName){
		try {
			conn = getConnectionFromPool();
			logger.log("Adding ConceptName " + conceptName + " to Index: " + indexElementKey + " (" + bookID + ")");
			preStmt = conn.prepareStatement("UPDATE _indexCatalog SET concept_name =? WHERE key_name =? and content_id =?;");
			
			preStmt.setString(1, conceptName);
			preStmt.setString(2, indexElementKey);
			preStmt.setString(3, bookID);
			
			preStmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
	}
	
	synchronized public ArrayList<Integer> getOccurrencesOfIndex(String bookID, String indexName){
		ArrayList<Integer> result = new ArrayList<Integer>();
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT page_index from _indexCatalog c " + 
					"INNER JOIN _indexLocation l on (c.id = l.index_id) " + 
					"WHERE key_name = ? and content_id = ?;");
			preStmt.setString(1, indexName);
			preStmt.setString(2, bookID);
			ResultSet res = preStmt.executeQuery();
	
			while(res.next())
				result.add(res.getInt("pageIndex"));
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return new ArrayList<Integer>();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
		
		return result;
	}
	
	synchronized public ArrayList<Integer> getOccurrencesOfIndexByConcept(String bookID, String conceptName){
		ArrayList<Integer> result = new ArrayList<Integer>();
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT page_index from _indexCatalog c " + 
					"INNER JOIN _indexLocation l on (c.id = l.index_id) " + 
					"WHERE concept_name = ? and content_id = ?;");
			preStmt.setString(1, conceptName);
			preStmt.setString(2, bookID);
			ResultSet res = preStmt.executeQuery();
	
			while(res.next())
				result.add(res.getInt("pageIndex"));
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return new ArrayList<Integer>();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
		
		return result;
	}
	
	/*OLD*/
//	public ArrayList<Integer> getOccurrencesOfIndex(String bookID, String indexName){
//		ArrayList<Integer> result = new ArrayList<Integer>();
//		try {
//			conn = getConnectionFromPool();
//			preStmt = conn.prepareStatement("SELECT pageIndex FROM "+bookID+"_indexMap WHERE indexName = ?");
//			preStmt.setString(1, indexName);
//			ResultSet res = preStmt.executeQuery();
//	
//			while(res.next())
//				result.add(res.getInt("pageIndex"));
//			
//		} catch (SQLException e) {
//			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
//			return new ArrayList<Integer>();
//		}finally{
//			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
//			 try { if (conn != null) conn.close(); } catch (Exception e) {};
//		}	
//		
//		return result;
//	}

	synchronized public ArrayList<Integer> getSegmentsIdOfIndexTerm(String bookID, String indexKey){
		ArrayList<Integer> result = new ArrayList<Integer>();
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT segment from _indexCatalog c " + 
					"INNER JOIN _indexLocation l on (c.id = l.index_id) " + 
					"WHERE key_name =? and content_id =?;");
			preStmt.setString(1, indexKey);
			preStmt.setString(2, bookID);
			ResultSet res = preStmt.executeQuery();
	
			while(res.next())
				result.add(res.getInt("segment"));
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return new ArrayList<Integer>();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
		
		return result;
		
	}
	
	/*OLD*/
//	public ArrayList<Integer> getSegmentsIdOfIndexTerm(String bookID, String indexName){
//		ArrayList<Integer> result = new ArrayList<Integer>();
//		try {
//			conn = getConnectionFromPool();
//			preStmt = conn.prepareStatement("SELECT segment FROM "+bookID+"_indexMap WHERE indexName = ?");
//			preStmt.setString(1, indexName);
//			ResultSet res = preStmt.executeQuery();
//	
//			while(res.next())
//				result.add(res.getInt("segment"));
//			
//		} catch (SQLException e) {
//			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
//			return new ArrayList<Integer>();
//		}finally{
//			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
//			 try { if (conn != null) conn.close(); } catch (Exception e) {};
//		}	
//		
//		return result;
//		
//	}
	

	
//	public ArrayList<Integer> getSegmentsIdOfConcept(String bookID, String conceptName){
//		ArrayList<Integer> result = new ArrayList<Integer>();
//		try {
//			conn = getConnectionFromPool();
//			preStmt = conn.prepareStatement("SELECT segment FROM "+bookID+"_indexMap WHERE conceptName = ? AND segment != -1");
//			if (conceptName != null)
//				conceptName = conceptName.replace("%2B", "+");
//			preStmt.setString(1, conceptName);
//			ResultSet res = preStmt.executeQuery();
//	
//			while(res.next())
//				result.add(res.getInt("segment"));
//			
//		} catch (SQLException e) {
//			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
//			return new ArrayList<Integer>();
//		}finally{
//			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
//			 try { if (conn != null) conn.close(); } catch (Exception e) {};
//		}	
//		
//		return result;
//	}
	
	/*OLD*/
//	public ArrayList<Integer> getSegmentsIdOfConcept(String bookID, String conceptName){
//		ArrayList<Integer> result = new ArrayList<Integer>();
//		try {
//			conn = getConnectionFromPool();
//			preStmt = conn.prepareStatement("SELECT segment FROM "+bookID+"_indexMap WHERE conceptName = ? AND segment != -1");
//			if (conceptName != null)
//				conceptName = conceptName.replace("%2B", "+");
//			preStmt.setString(1, conceptName);
//			ResultSet res = preStmt.executeQuery();
//	
//			while(res.next())
//				result.add(res.getInt("segment"));
//			
//		} catch (SQLException e) {
//			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
//			return new ArrayList<Integer>();
//		}finally{
//			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
//			 try { if (conn != null) conn.close(); } catch (Exception e) {};
//		}	
//		
//		return result;
//	}


	synchronized public ArrayList<String> getIndexTermsOnPage(String bookID, int pageIndex){
		ArrayList<String> indexTerms = new ArrayList<String>();
		
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT key_name from _indexCatalog c " + 
					"INNER JOIN _indexLocation l on (c.id = l.index_id) " + 
					"WHERE content_id = ? and page_index = ?;");
			preStmt.setString(1, bookID);
			preStmt.setInt(2, pageIndex);
			ResultSet res = preStmt.executeQuery();
		
		while(res.next())
			indexTerms.add(res.getString("key_name"));
		
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return new ArrayList<String>();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
		
		return indexTerms;
	}
	
	/*OLD*/
//	public ArrayList<String> getIndexTermsOnPage(String bookID, int pageIndex){
//		ArrayList<String> indexTerms = new ArrayList<String>();
//		
//		try {
//			conn = getConnectionFromPool();
//			preStmt = conn.prepareStatement("SELECT indexName FROM "+bookID+"_indexMap WHERE pageIndex = ?");
//			preStmt.setInt(1, pageIndex);
//			ResultSet res = preStmt.executeQuery();
//		
//		while(res.next())
//			indexTerms.add(res.getString("indexName"));
//		
//		} catch (SQLException e) {
//			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
//			return new ArrayList<String>();
//		}finally{
//			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
//			 try { if (conn != null) conn.close(); } catch (Exception e) {};
//		}	
//		
//		return indexTerms;
//	}
	
	synchronized public ArrayList<String> getIndexTermsOfSegment(String bookID, int segmentID) {
		ArrayList<String> indexTerms = new ArrayList<String>();
		
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT distinct(key_name) from _indexCatalog c " + 
					"INNER JOIN _indexLocation l on (c.id = l.index_id) " + 
					"WHERE content_id = ? and segment = ?;");
			preStmt.setString(1, bookID);
			preStmt.setInt(2, segmentID);
			ResultSet res = preStmt.executeQuery();
		
		while(res.next())
			indexTerms.add(res.getString("key_name"));
		
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return new ArrayList<String>();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
		
		return indexTerms;
	}
	
	synchronized public Set<String>[] getIndexEntriesOfSegment(String bookID, int segmentID) {
		Set<String> indexKeys = new HashSet<String>();
		Set<String> indexLabels = new HashSet<String>();
		Set<String> indexConcepts = new HashSet<String>();
		
		Set<String>[] array = (Set<String>[]) new Set[3];
		array[0] = indexKeys;
		array[1] = indexLabels;
		array[2] = indexConcepts;
		
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT key_name, label, concept_name from _indexCatalog c " + 
					"INNER JOIN _indexLocation l on (c.id = l.index_id) " + 
					"WHERE content_id = ? and segment = ?;");
			preStmt.setString(1, bookID);
			preStmt.setInt(2, segmentID);
			ResultSet res = preStmt.executeQuery();
		
		while(res.next()) {
			indexKeys.add(res.getString("key_name"));
			indexLabels.add(res.getString("label"));
			indexConcepts.add(res.getString("concept_name"));
		}
		
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return (Set<String>[]) new Set[3];
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
		
		return array;
	}
	
	

	
	synchronized public  void updateConceptNameOfIndexElement(String bookID, String key, String conceptName) {
		try {
			conn = getConnectionFromPool();
			
			String sqlUpdate = "UPDATE _indexCatalog SET concept_name = ? WHERE content_id = ?  AND key_name = ?;";
			preStmt = conn.prepareStatement(sqlUpdate);
			preStmt.setString(1, conceptName);
			preStmt.setString(2, bookID);
			preStmt.setString(3, key);
			
			preStmt.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		} finally {
			try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
	}
	
	synchronized public ArrayList<String> getListOfIndicesForBook(String bookID){
		ArrayList<String> result = new ArrayList<String>();
		
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT key_name FROM _indexCatalog WHERE content_id = ?;");
			preStmt.setString(1, bookID);
			ResultSet res = preStmt.executeQuery();
			
			while(res.next())
				result.add(res.getString("key_name"));
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return new ArrayList<String>();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
		
		return result;
	}
	
	synchronized public ArrayList<String> getListOfIndicesWithPageForBook(String bookID){
		ArrayList<String> result = new ArrayList<String>();
		
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT distinct(key_name) from _indexCatalog c " + 
					"INNER JOIN _indexLocation l on (c.id = l.index_id) " + 
					"WHERE content_id = ? and page_index != -1;");
			preStmt.setString(1, bookID);
			ResultSet res = preStmt.executeQuery();
			
			while(res.next())
				result.add(res.getString("indexName"));
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return new ArrayList<String>();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
		
		return result;
	}
	
	/*OLD*/
//	public ArrayList<String> getListOfIndicesWithPageForBook(String bookID){
//		ArrayList<String> result = new ArrayList<String>();
//		
//		try {
//			conn = getConnectionFromPool();
//			preStmt = conn.prepareStatement("SELECT distinct(indexName) FROM "+bookID+"_indexMap WHERE pageIndex != -1");
//			ResultSet res = preStmt.executeQuery();
//			
//			while(res.next())
//				result.add(res.getString("indexName"));
//			
//		} catch (SQLException e) {
//			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
//			return new ArrayList<String>();
//		}finally{
//			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
//			 try { if (conn != null) conn.close(); } catch (Exception e) {};
//		}	
//		
//		return result;
//	}
	
	synchronized public List<String[]> getListOfIndicesWithPageForBookV2(String bookID){
		List<String[]> result = new ArrayList<String[]>();
		
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT id, key_name, label, full_label, concept_name FROM _indexCatalog WHERE content_id = ?");
			preStmt.setString(1, bookID);
			ResultSet res = preStmt.executeQuery();
			while(res.next()) {
				String[] temp = new String[5];
				temp[0] = res.getString("id");
				temp[1] = res.getString("key_name");
				temp[2] = res.getString("label");
				temp[3] = res.getString("full_label");
				temp[4] = res.getString("concept_name");
				result.add(temp);
			}
			return result;
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return null;
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
	}
	
	synchronized public List<String> getListOfUsedConceptNames(String bookID){
		List<String> result = new ArrayList<String>();
		
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT distinct concept_name FROM _indexCatalog WHERE content_id = ?");
			preStmt.setString(1, bookID);
			ResultSet res = preStmt.executeQuery();
			while(res.next()) {
				String c = res.getString("concept_name");
				if(c != null)
					result.add(c);
			}
			return result;
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return null;
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
	}
	
	synchronized public List<String> getListOfIndexParts(String index_id){
		List<String> result = new ArrayList<String>();
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT text\n" + 
					"FROM _indexText as text\n" + 
					"INNER JOIN _indexPart as part ON (text.text_id = part.part_id)\n" + 
					"WHERE part.index_id = ?;");
			preStmt.setString(1, index_id);
			ResultSet res = preStmt.executeQuery();
			while(res.next()) {
				result.add(res.getString("text"));
			}
			return result;
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return null;
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
	}
	
	
	
	synchronized public void createBook(String bookID, String fileName, String uri, LanguageEnum language, String bookName, String checksum, String type, String group, String senderEmail){
		try {
			conn = getConnectionFromPool();
			String sqlCreate = "CREATE TABLE IF NOT EXISTS content" +
					"(id VARCHAR(255) not NULL, " +
					" name VARCHAR(255) not NULL, " +
					" type VARCHAR(255) not NULL, " +
	                " filename VARCHAR(255) not NULL, " +
	                " checksum VARCHAR(32) not NULL, " +
	                " uri VARCHAR(255) not NULL, " +
	                " lang VARCHAR(255), "+
	                " `grouping` VARCHAR(255), "+
	                " sender VARCHAR(255), "+
	                " status VARCHAR(50), "+
	                " created_at timestamp DEFAULT now(), "+
	                " updated_at timestamp DEFAULT now(), "+
	                " PRIMARY KEY ( id ))";

			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate();
			preStmt.close();
			
			
			preStmt = conn.prepareStatement("SELECT * FROM content WHERE id=?");
			preStmt.setString(1, bookID);
			ResultSet res = preStmt.executeQuery();
			
			if(!res.next()){
				preStmt = conn.prepareStatement("INSERT INTO content VALUES(?,?,?,?,?,?,?,?,?,?, now(), now())");
				preStmt.setString(1, bookID);
				preStmt.setString(2, bookName);
				preStmt.setString(3, type);
				preStmt.setString(4, fileName);
				preStmt.setString(5, checksum);
				preStmt.setString(6, uri);
				preStmt.setString(7, language.toString());
				preStmt.setString(8, group);
				preStmt.setString(9, senderEmail);
				preStmt.setString(10, BookStatus.Created.getValue());
				preStmt.executeUpdate();
			}
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
	} 
	
	synchronized public void updateBookStatus(String bookID, BookStatus status){
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("UPDATE content SET status = ?, updated_at = now() WHERE id = ? ");
			preStmt.setString(1, status.getValue());
			preStmt.setString(2, bookID);
			preStmt.executeUpdate();		
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
	}
	
	synchronized public boolean doesRelationExist(String parentBook, Integer segmentID,
			String referedBookID, Integer referedSegment){

		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT * FROM "+parentBook+"_relations WHERE fromSegment =? AND toBook=? AND toSegment=?");
			preStmt.setInt(1, segmentID);
			preStmt.setString(2, referedBookID);
			preStmt.setInt(3, referedSegment);
			
			
			ResultSet res = preStmt.executeQuery();
			return res.next();
		
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return false;
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
	}	
	
	synchronized public void addFormatEntry(String parentBook, String pageIndex, int lineNumber, int wordPos,
			Integer formatKey, CoordinatesContainer coords){
			
		try {
			conn = getConnectionFromPool();
			String id = parentBook+"_"+pageIndex+"_"+lineNumber+"_"+wordPos;
			String sql = "INSERT INTO "+parentBook+"_formatMap VALUES(?,?,?,?,?,?,?,?,?)";
			
			preStmt = conn.prepareStatement(sql);
			preStmt.setString(1, id);
			preStmt.setInt(2, Integer.parseInt(pageIndex));
			preStmt.setInt(3, lineNumber);
			preStmt.setInt(4, wordPos);
			preStmt.setString(5, String.valueOf(formatKey));
			preStmt.setDouble(6, coords.getLeftTopX());
			preStmt.setDouble(7, coords.getLeftTopY());
			preStmt.setDouble(8, coords.getRightBottomX());
			preStmt.setDouble(9, coords.getRightBottomY());

			preStmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
		
	}

	synchronized public Integer getWordFormatting(String parentBook, int pageNumber, int lineNumber, int wordPos) {
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT * FROM "+parentBook+"_formatMap " +"WHERE id=? ORDER BY position ASC");
			preStmt.setString(1, parentBook+"_"+pageNumber+"_"+lineNumber+"_"+wordPos);
			
			ResultSet res = preStmt.executeQuery();
			preStmt.close();
			
			if(res.first()){
				String key = res.getString("format");
				
				return Integer.parseInt(key);
			}else
				return null;
				
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
	
		return null;
	}
	
	
	synchronized public ArrayList<Integer> getFormatKeysInLine(String parentBook, int pageNumber, int lineNumber) {
		try {
			conn = getConnectionFromPool();
			String id = parentBook+"_"+pageNumber+"_"+lineNumber+"_";
			ArrayList<Integer> resList = new ArrayList<Integer>();
			
			preStmt = conn.prepareStatement("SELECT * FROM "+parentBook+"_formatMap " +"WHERE id LIKE'"+id+"%' ORDER BY position ASC");
			
			ResultSet res = preStmt.executeQuery();
			preStmt.close();
			
			while(res.next())
				resList.add(Integer.parseInt(res.getString("format")));
			
			return resList;
				
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
		
		
		return null;
	}
	
	synchronized public CoordinatesContainer getWordCoordinates(String parentBook,int pageNumber, int lineNumber, int wordPos) {
	
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT * FROM "+parentBook+"_formatMap " +"WHERE id=? ORDER BY position ASC");
			preStmt.setString(1, parentBook+"_"+pageNumber+"_"+lineNumber+"_"+wordPos);
			ResultSet res = preStmt.executeQuery();
			preStmt.close();
			
			CoordinatesContainer coords = null;
			
			if(res.first()){
				coords = new CoordinatesContainer(res.getDouble("leftTopX"), res.getDouble("leftTopY"), res.getDouble("rightBottomX"), res.getDouble("rightBottomY"));
				
				return coords;
			}else
				return null;
				
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	

		return null;
	}

	synchronized public String getConceptOfIndexTerm(String indexName, String bookID){
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT conceptName FROM "+bookID+"_indexMap WHERE indexName = ?");
			preStmt.setString(1, indexName);
			ResultSet res = preStmt.executeQuery();

			if(res.next())
				return res.getString("conceptName");
		
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
		return null;
	}
	
	synchronized public String getConceptOfIndexElement(String keyName, String bookID){
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT concept_name FROM _indexCatalog WHERE key_name = ? and content_id = ?;");
			preStmt.setString(1, keyName);
			preStmt.setString(2, bookID);
			ResultSet res = preStmt.executeQuery();

			if(res.next())
				return res.getString("concept_name");
		
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
		return null;
	}
	
	//@i.alpizarchacon: DISTINCT added
	synchronized public List<String> getIndexTermsOfConcept(String conceptName, String bookID) {
		
		try {
			
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT distinct(key_name) from _indexCatalog c " + 
					"INNER JOIN _indexLocation l on (c.id = l.index_id) " + 
					"WHERE content_id = ? and concept_name = ?;");
			preStmt.setString(1, bookID);
			preStmt.setString(2, conceptName);
			ResultSet res = preStmt.executeQuery();
			
			List<String> resList = new ArrayList<String>();
			
			while (res.next())
				resList.add(res.getString("indexName"));
			
			return resList;
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		} finally {
			try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
		return null;
	}

	/*OLD*/
//	//@i.alpizarchacon: DISTINCT added
//	public List<String> getIndexTermsOfConcept(String conceptName, String bookID) {
//		
//		try {
//			
//			conn = getConnectionFromPool();
//			preStmt = conn.prepareStatement("SELECT DISTINCT indexName FROM "+bookID+"_indexMap WHERE conceptName = ?");
//			preStmt.setString(1, conceptName);
//			ResultSet res = preStmt.executeQuery();
//			
//			List<String> resList = new ArrayList<String>();
//			
//			while (res.next())
//				resList.add(res.getString("indexName"));
//			
//			return resList;
//			
//		} catch (SQLException e) {
//			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
//		} finally {
//			try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
//			try { if (conn != null) conn.close(); } catch (Exception e) {};
//		}
//		
//		return null;
//	}
	
	//Statistics about index-concept mapping
	synchronized public List<Integer> getIndexConceptMappingStatistics(String bookID) {
		List<Integer> resList = new ArrayList<Integer>();
	
		try {
			
			conn = getConnectionFromPool();
			
			//query #1: Number of total and different index entries
			preStmt = conn.prepareStatement("SELECT count(*) as total_index_entries,  count(distinct(indexName)) as unique_index_entries FROM "+bookID+"_indexMap WHERE pageIndex != -1");
			ResultSet res = preStmt.executeQuery();
			
			while (res.next()) {
				resList.add(res.getInt("total_index_entries"));
				resList.add(res.getInt("unique_index_entries"));
			}
			
			//query #2: Number of distinct index terms mapped to concepts AND Number of different conceptNames mapped
			preStmt = conn.prepareStatement("SELECT count(distinct(indexName)) as number_entries, count(distinct(conceptName)) as numebr_concepts FROM "+bookID+"_indexMap WHERE conceptName is not null");
			res = preStmt.executeQuery();
			
			while (res.next()) {
				resList.add(res.getInt("number_entries"));
				resList.add(res.getInt("numebr_concepts"));
			}
			
			return resList;
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		} finally {
			try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
		return null;
		
	}
	//
	
	synchronized public ArrayList<Integer> getRelatedSegmentsInBook(String slideID, int segmentID, String bookID) {
		
		try {
			
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT DISTINCT book.segment FROM "+slideID+"_indexMap AS slide " +
											"JOIN "+bookID+"_indexMap AS book USING (conceptName) " + 
											"WHERE slide.segment = ?");
			preStmt.setInt(1, segmentID);
			ResultSet res = preStmt.executeQuery();
			
			ArrayList<Integer> resList = new ArrayList<Integer>();
			
			while (res.next())
				resList.add(res.getInt("segment"));
			
			return resList;
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		} finally {
			try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
		return null;
		
	}
	
	synchronized public HashMap<Integer, ArrayList<Integer>> getSemanticMatchingSegments(String sourceBookID, String targetBookID) {
		
		try {
			
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT DISTINCT locSRC.segment, locTARGET.segment " + 
					"FROM _indexLocation as locSRC, _indexLocation as locTARGET, _indexCatalog AS src " + 
					"JOIN _indexCatalog AS target USING (concept_name) " + 
					"WHERE src.content_id = ? " + 
					"  AND src.id = locSRC.index_id " + 
					"AND target.content_id = ? " + 
					"  AND target.id = locTARGET.index_id;");
			
			preStmt.setString(1, sourceBookID);
			preStmt.setString(2, targetBookID);
			ResultSet res = preStmt.executeQuery();
			
			HashMap<Integer, ArrayList<Integer>> resMap = new HashMap<Integer, ArrayList<Integer>>();
			
			while (res.next()) {
				
				int sourceSeg = res.getInt("locSRC.segment");
				int targetSeg = res.getInt("locTARGET.segment");
				
				if (!resMap.containsKey(sourceSeg))
					resMap.put(sourceSeg, new ArrayList<Integer>());
				
				resMap.get(sourceSeg).add(targetSeg);
				
			}
			
			return resMap;
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		} finally {
			try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
		return null;
		
	}
	
	synchronized public ArrayList<String> getAggregatedConcepts(String sourceBookID, ArrayList<Integer> allSegments) {
		
		try {
			
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT DISTINCT src.concept_name " + 
					"FROM _indexLocation as locSRC, _indexCatalog AS src " + 
					"WHERE src.content_id = ? " + 
					"  AND src.id = locSRC.index_id " + 
					"  AND locSRC.segment IN (" + StringUtils.join(allSegments, ',') + ")");
			
			preStmt.setString(1, sourceBookID);
			ResultSet res = preStmt.executeQuery();
			
			ArrayList<String> resList = new ArrayList<String>();
			
			while (res.next()) {
				String concept = res.getString("src.concept_name");
				resList.add(concept);
			}
			
			return resList;
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		} finally {
			try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
		return null;
		
	}
	
	synchronized public ArrayList<Pair<String,Integer>> getAggregatedConceptsWithFreq(String sourceBookID, ArrayList<Integer> allSegments) {
		
		try {
			
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT src.concept_name as name, count(src.concept_name) as freq " + 
					"FROM _indexLocation as locSRC, _indexCatalog AS src " + 
					"WHERE src.content_id = ? " + 
					"  AND src.id = locSRC.index_id " + 
					"  AND locSRC.segment IN (" + StringUtils.join(allSegments, ',') + ")" +
					" group by concept_name;");
			
			preStmt.setString(1, sourceBookID);
			ResultSet res = preStmt.executeQuery();
			
			ArrayList<Pair<String,Integer>> resList = new ArrayList<Pair<String,Integer>>();
			
			while (res.next()) {
				String concept = res.getString("name");
				Integer freq = res.getInt("freq");
				
				resList.add(Pair.of(concept, freq));
			}
			
			return resList;
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
		} finally {
			try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
		return null;
		
	}
	
	synchronized public Connection getConnectionFromPool() throws SQLException{
		long currentTime = Calendar.getInstance().getTime().getTime();
		long offset = currentTime - this.lastConRefresh;
		
		if(((offset / (1000*60*60)))>2){
			dataSource.close();
			try {
				dataSource = new ComboPooledDataSource();
				dataSource.setDriverClass(JDBC_DRIVER);
				dataSource.setJdbcUrl(DB_URL);
				dataSource.setUser(USER);
				dataSource.setPassword(PASS); 
				SystemLogger.getInstance().log("Reconnecting to Database");
			} catch (PropertyVetoException e) {
				e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			}

			this.lastConRefresh = Calendar.getInstance().getTime().getTime();	
		}
		return dataSource.getConnection();
	}

	
	/*----------------------USER OPERATIONS -------------------*/
	synchronized public ArrayList<String> getAccountList() {
		ArrayList<String> accountList = new ArrayList<String>();
	
		try {
			
			String sqlCreate = "CREATE TABLE IF NOT EXISTS users" +
					"(username VARCHAR(255) not NULL, " +
					" password VARCHAR(255) not NULL, " +
	                " language VARCHAR(255) not NULL," +
	                " targetlanguage VARCHAR(255) not NULL," +
	                " email VARCHAR(255)," +
	                " gender VARCHAR(255)," +
	                " age VARCHAR(255)," +
	                " majorSubject VARCHAR(255)," +
	                " PRIMARY KEY ( username ))";
			
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate(sqlCreate);
			preStmt.close();
			
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT username from users");
			ResultSet res = preStmt.executeQuery();

			while (res.next())
				accountList.add(res.getString("username"));
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		} finally {
			try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		return accountList;
	}

	
	synchronized public void createUser(String userName, String password,LanguageEnum originLang, LanguageEnum targetLang,
						   String email, String gender, int age, String majorSubject) {
		try {
			String sqlCreate = "CREATE TABLE IF NOT EXISTS users" +
					"(username VARCHAR(255) not NULL, " +
					" password VARCHAR(255) not NULL, " +
	                " language VARCHAR(255) not NULL," +
	                " targetlanguage VARCHAR(255) not NULL," +
	                " email VARCHAR(255)," +
	                " gender VARCHAR(1)," +
	                " age VARCHAR(255)," +
	                " majorSubject VARCHAR(255)," +
	                " PRIMARY KEY ( username ))";
	
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate(sqlCreate);
			preStmt.close();
			
			String sqlInsert = "INSERT INTO users VALUES(?,?,?,?,?,?,?,?)";
			preStmt = conn.prepareStatement(sqlInsert);
			preStmt.setString(1, userName);
			preStmt.setString(2, password);
			preStmt.setString(3, originLang.toString());
			preStmt.setString(4, targetLang.toString());
			preStmt.setString(5, email);
			preStmt.setString(6, gender);
			preStmt.setInt(7, age);
			preStmt.setString(8, majorSubject);
			
			preStmt.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		} finally {
			try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
	}
	
	
	
	synchronized public void createUserLogEntry(String userName, String timestamp, String action, String object, String book,
			   String description) {
		try {
			String sqlCreate = "CREATE TABLE IF NOT EXISTS userActivityLog" +
					"(username VARCHAR(255) not NULL, " +
					 "timestamp VARCHAR(255) not NULL, " +
				     " action VARCHAR(255) not NULL," +
				     " object VARCHAR(255) not NULL," +
				     " book VARCHAR(255)," +
				     " description VARCHAR(255))";
			
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate(sqlCreate);
			preStmt.close();
			
			String sqlInsert = "INSERT INTO userActivityLog VALUES(?,?,?,?,?,?)";
			preStmt = conn.prepareStatement(sqlInsert);
			preStmt.setString(1, userName);
			preStmt.setString(2, timestamp);
			preStmt.setString(3, action);
			preStmt.setString(4, object);
			preStmt.setString(5, book);
			preStmt.setString(6, description);
			
			preStmt.executeUpdate();
		
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		} finally {
			try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			try { if (conn != null) conn.close(); } catch (Exception e) {};
		}

}

	
	synchronized public void editUser(String userName, LanguageEnum originLang, LanguageEnum targetLang,
			String email, String gender, int age, String majorSubject) {
		
		try {
			conn = getConnectionFromPool();
			
			String sqlUpdate = "UPDATE users SET language=?, targetLanguage=?, email=?, gender=?, age=?, majorSubject=? WHERE username=?";
			preStmt = conn.prepareStatement(sqlUpdate);
			preStmt.setString(1, originLang.toString());
			preStmt.setString(2, targetLang.toString());
			preStmt.setString(3, email);
			preStmt.setString(4, gender);
			preStmt.setInt(5, age);
			preStmt.setString(6, majorSubject);
			preStmt.setString(7, userName);
			
			preStmt.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		} finally {
			try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
	
		
	}
	
	synchronized public HashMap<String, String> loadUser(String userName) {
		HashMap<String, String> userDetails = new HashMap<String,String>();
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("SELECT * from users WHERE username = ?");
			preStmt.setString(1, userName);
			ResultSet res = preStmt.executeQuery();
	
			while (res.next()){
				userDetails.put("password",res.getString("password"));
				userDetails.put("originLanguage", res.getString("language"));
				userDetails.put("targetLanguage", res.getString("targetlanguage"));
				userDetails.put("email", res.getString("email"));
				userDetails.put("gender", res.getString("gender"));
				userDetails.put("age", String.valueOf(res.getInt("age")));
				userDetails.put("majorSubject", res.getString("majorSubject"));
			}
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		} finally {
			try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
		return userDetails;
	}
	
	synchronized public boolean dropUserAccount(String userName) {
		try {
			conn = getConnectionFromPool();
			preStmt = conn.prepareStatement("DELETE FROM users WHERE username = ?");
			preStmt.setString(1, userName);
			preStmt.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString());
			return false;
		} finally {
			try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
		return true;
		
	}

	
	
	/*----------------------------------------- ontology versions ----------------------------------------------*/
	
	synchronized public void versionizeOntology(){
		String table = "ontologyVersion";
		PreparedStatement preUpdate = null;
		PreparedStatement preInsert = null;
		
		try {
			conn = getConnectionFromPool();
			String sqlCreate = "CREATE TABLE IF NOT EXISTS " + table +
					"(version INTEGER not NULL)";
	
			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate(sqlCreate);
			
			preStmt = conn.prepareStatement("SELECT version FROM " + table);
			ResultSet res = preStmt.executeQuery();
			
			int version;
			if (res.next()) {
				
				version = res.getInt("version");
				preUpdate = conn.prepareStatement("UPDATE " + table + " SET version=?");
				preUpdate.setString(1, Integer.toString(++version));
				preUpdate.executeUpdate();
				preUpdate.close();
				
			} else {
				
				preInsert = conn.prepareStatement("INSERT INTO " + table + " VALUES (?)");
				preInsert.setString(1, Integer.toString(1));
				preInsert.executeUpdate();
				preInsert.close();
				
			}
		
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (preUpdate != null) preStmt.close(); } catch (Exception e) {};
			 try { if (preInsert != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}
		
	}
	
	synchronized public Integer getOntologyVersion(){
		
		try {
			conn = getConnectionFromPool();
		
			String table = "ontologyVersion";
			
			String sqlCreate = "CREATE TABLE IF NOT EXISTS " + table +
					"(version INTEGER not NULL)";
			preStmt = conn.prepareStatement(sqlCreate);
			
			preStmt.executeUpdate(sqlCreate);
			
			preStmt = conn.prepareStatement("SELECT version FROM " + table);
			ResultSet res = preStmt.executeQuery();
			
			int version;
			if (res.next()) {
				
				version = res.getInt("version");
				return version;
				
			} else {
				
				version = 1;
				
				PreparedStatement preInsert = conn.prepareStatement("INSERT INTO " + table + " VALUES (?)");
				preInsert.setString(1, Integer.toString(version));
				preInsert.executeUpdate();
				preInsert.close();
				
				return version;
				
			}
		} catch (SQLException e) {
			e.printStackTrace();SystemLogger.getInstance().log(e.toString()); 
			return null;
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
	}

	
	/*----------------------------------------- assessment tests ----------------------------------------------*/
	synchronized public void createTestItem(LanguageEnum lang, String concept, String label) {
		try {
			conn = getConnectionFromPool();
			
			String sqlCreate = "CREATE TABLE IF NOT EXISTS test_items"+
					"(concept VARCHAR(255) not NULL, " +
	                " label VARCHAR(255) not NULL, " +
					" language VARCHAR(255) not NULL)";

			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate();
			
			preStmt = conn.prepareStatement("INSERT INTO test_items VALUES(?,?,?)");
			preStmt.setString(1, concept);
			preStmt.setString(2, label);
			preStmt.setString(3, lang.toString());
			
			preStmt.executeUpdate();
			preStmt.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
		
	}

	synchronized public void createAssessmentRun(String testId, String userName,
			String timestamp, String sourceLanguage, String targetLanguage) {

		try {
			conn = getConnectionFromPool();
			
			String sqlCreate = "CREATE TABLE IF NOT EXISTS assessment_runs"+
					"(testId VARCHAR(255) not NULL, " +
	                " userId VARCHAR(255) not NULL, " +
					" timestamp VARCHAR(255) not NULL," +
					" sourceLanguage VARCHAR(255) not NULL, " +
					" targetLanguage VARCHAR(255) not NULL )";
			
			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate();
			
			preStmt = conn.prepareStatement("INSERT INTO assessment_runs VALUES(?,?,?,?,?)");
			preStmt.setString(1, testId);
			preStmt.setString(2, userName);
			preStmt.setString(3, timestamp);
			preStmt.setString(4, sourceLanguage);
			preStmt.setString(5, targetLanguage);
			
			preStmt.executeUpdate();
			preStmt.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
	}
	
	synchronized public void createAssessmentRunQuestion(String testId, String questionId,
						String userName, String type, String sourceLanguage, String targetLanguage) {
		
		try {
			conn = getConnectionFromPool();
			
			String sqlCreate = "CREATE TABLE IF NOT EXISTS assessment_runs_questions"+
					"(testId VARCHAR(255) not NULL, " +
	                " questionId VARCHAR(255) not NULL, " +
	                " userId VARCHAR(255) not NULL, " +
	                " type VARCHAR(255) not NULL, " +
					" sourceLanguage VARCHAR(255) not NULL, " +
					" targetLanguage VARCHAR(255) not NULL)";
			
			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate();
			
			preStmt = conn.prepareStatement("INSERT INTO assessment_runs_questions VALUES(?,?,?,?,?,?)");
			preStmt.setString(1, testId);
			preStmt.setString(2, testId+"_"+questionId);
			preStmt.setString(3, userName);
			preStmt.setString(4, type);
			preStmt.setString(5, sourceLanguage);
			preStmt.setString(6, targetLanguage);
			
			preStmt.executeUpdate();
			preStmt.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
	}

	synchronized public void createAssessmentRunItem(String testId, String questionId,
										String concept, String label) {
		try {
			conn = getConnectionFromPool();
			
			String sqlCreate = "CREATE TABLE IF NOT EXISTS assessment_runs_items"+
					"(testId VARCHAR(255) not NULL, " +
	                " questionId VARCHAR(255) not NULL, " +
	                " concept VARCHAR(255) not NULL, " +
	                " presented_label VARCHAR(255) not NULL)";
			
			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate();
			
			preStmt = conn.prepareStatement("INSERT INTO assessment_runs_items VALUES(?,?,?,?)");
			preStmt.setString(1, testId);
			preStmt.setString(2, testId+"_"+questionId);
			preStmt.setString(3, concept);
			preStmt.setString(4, label);
			
			preStmt.executeUpdate();
			preStmt.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
		
	}

	synchronized public void createAssessmentRunSelectedItem(String testId,
					String questionId, String concept, String label, String correct) {
		try {
			conn = getConnectionFromPool();
			
			String sqlCreate = "CREATE TABLE IF NOT EXISTS assessment_runs_selected_items"+
					"(testId VARCHAR(255) not NULL, " +
	                " questionId VARCHAR(255) not NULL, " +
	                " concept VARCHAR(255) not NULL, " +
	                " presented_label VARCHAR(255) not NULL," + 
	                " correct VARCHAR(255) not NULL)";
			
			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate();
			
			preStmt = conn.prepareStatement("INSERT INTO assessment_runs_selected_items VALUES(?,?,?,?,?)");
			preStmt.setString(1, testId);
			preStmt.setString(2, testId+"_"+questionId);
			preStmt.setString(3, concept);
			preStmt.setString(4, label);
			preStmt.setString(5, correct);
			
			preStmt.executeUpdate();
			preStmt.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
		
	}

	synchronized public void createAssessmentRunCorrectItem(String testId,
						String questionId, String concept, String label) {
		try {
			conn = getConnectionFromPool();
			
			String sqlCreate = "CREATE TABLE IF NOT EXISTS assessment_runs_correct_items"+
					"(testId VARCHAR(255) not NULL, " +
	                " questionId VARCHAR(255) not NULL, " +
	                " concept VARCHAR(255) not NULL, " +
	                " presented_label VARCHAR(255) not NULL)";
			
			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate();
			
			preStmt = conn.prepareStatement("INSERT INTO assessment_runs_correct_items VALUES(?,?,?,?)");
			preStmt.setString(1, testId);
			preStmt.setString(2, testId+"_"+questionId);
			preStmt.setString(3, concept);
			preStmt.setString(4, label);
			
			preStmt.executeUpdate();
			preStmt.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}	
		
	}

	synchronized public ArrayList<String>  getAssessmentRuns(String userName) {
		ArrayList<String> runs = new ArrayList<String>();
		
		try {
			conn = getConnectionFromPool();
			
			String sqlCreate = "CREATE TABLE IF NOT EXISTS assessment_runs"+
					"(testId VARCHAR(255) not NULL, " +
	                " userId VARCHAR(255) not NULL, " +
					" timestamp VARCHAR(255) not NULL," +
					" sourceLanguage VARCHAR(255) not NULL, " +
					" targetLanguage VARCHAR(255) not NULL )";
			
			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate();
			
			preStmt = conn.prepareStatement("SELECT testId FROM assessment_runs WHERE userId=?");
			preStmt.setString(1, userName);
			ResultSet res = preStmt.executeQuery();
			while(res.next())
				runs.add(res.getString("testId"));
			
			return runs;
				
			
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}		
			
		return runs;
	}

	synchronized public ArrayList<String> getQuestionsOfRun(String testId) {
		ArrayList<String> questions = new ArrayList<String>();
		
		try {
			conn = getConnectionFromPool();
			
			String sqlCreate = "CREATE TABLE IF NOT EXISTS assessment_runs_questions"+
					"(testId VARCHAR(255) not NULL, " +
	                " questionId VARCHAR(255) not NULL, " +
	                " userId VARCHAR(255) not NULL, " +
	                " type VARCHAR(255) not NULL, " +
					" sourceLanguage VARCHAR(255) not NULL, " +
					" targetLanguage VARCHAR(255) not NULL)";
			
			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate();
			
			preStmt = conn.prepareStatement("SELECT questionId FROM assessment_runs_questions WHERE testId=?");
			preStmt.setString(1, testId);
			ResultSet res = preStmt.executeQuery();
			while(res.next())
				questions.add(res.getString("questionId"));
			
			return questions;
				
			
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}		
			
		return questions;
	}

	synchronized public ArrayList<String> getSelectedItemsInRun(String questionId, boolean correct) {
	ArrayList<String> correctItems = new ArrayList<String>();
		
		try {
			conn = getConnectionFromPool();
			
			String sqlCreate = "CREATE TABLE IF NOT EXISTS assessment_runs_selected_items"+
					"(testId VARCHAR(255) not NULL, " +
	                " questionId VARCHAR(255) not NULL, " +
	                " concept VARCHAR(255) not NULL, " +
	                " presented_label VARCHAR(255) not NULL," + 
	                " correct VARCHAR(255) not NULL)";
			
			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate();
			
			preStmt = conn.prepareStatement("SELECT concept FROM assessment_runs_selected_items WHERE questionId=? AND correct=?");
			preStmt.setString(1, questionId);
			preStmt.setString(2, String.valueOf(correct));
			ResultSet res = preStmt.executeQuery();
			while(res.next())
				correctItems.add(res.getString("concept"));
			
			return correctItems;
				
			
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}		
			
		return correctItems;
	}

	synchronized public ArrayList<String[]> getItemsInQuestion(String questionId) {
		ArrayList<String[]> items = new ArrayList<String[]>();
		
		try {
			conn = getConnectionFromPool();
			
			String sqlCreate = "CREATE TABLE IF NOT EXISTS assessment_runs_items"+
					"(testId VARCHAR(255) not NULL, " +
					" questionId VARCHAR(255) not NULL, " +
	                " concept VARCHAR(255) not NULL, " +
	                " presented VARCHAR(255) not NULL) ";
			
			preStmt = conn.prepareStatement(sqlCreate);
			preStmt.executeUpdate();
			
			preStmt = conn.prepareStatement("SELECT * FROM assessment_runs_items WHERE questionId=?");
			preStmt.setString(1, questionId);
			ResultSet res = preStmt.executeQuery();
			while(res.next()){
				String[] entry = {res.getString("concept"),res.getString("presented_label")};
				items.add(entry);
			}
			
			return items;
				
			
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}		
			
		return items;
	}
	
	synchronized public void updateBook(String b1, String b2) {		
		try {
			conn = getConnectionFromPool();

			
			preStmt = conn.prepareStatement("update _indexCatalog set content_id = 1561999326807 where content_id = ?;");
			preStmt.setString(1, b1);
			preStmt.executeUpdate();	
			
			preStmt = conn.prepareStatement("update _indexCatalog set content_id = 1561983466804 where content_id = ?;");
			preStmt.setString(1, b2);
			preStmt.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			 try { if (preStmt != null) preStmt.close(); } catch (Exception e) {};
			 try { if (conn != null) conn.close(); } catch (Exception e) {};
		}		
			
	}
	
	public static void main(String args[]) {
		Database db = Database.getInstance();
		System.out.println(db.getLanguage("41DD2B4F1FC9F0C5F2490BC172526581"));
	}
}
