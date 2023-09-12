package intextbooks.content.models.formatting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PageMetadata {
	private HashMap<PageMetadataEnum, String> map;

	/**
	 * @param map
	 */
	public PageMetadata(HashMap<PageMetadataEnum, String> metadata) {
		this.map = metadata;
	}
	
	/**
	 * @param metadata
	 */
	
	public boolean containsMetadata(PageMetadataEnum metadataName){
		return this.map.containsKey(metadataName);
	}
	
	public String getMetadataValue(PageMetadataEnum metadataName){
		return this.map.get(metadataName);
	}
	
	public void addMetadata(PageMetadataEnum metadataName, String metadataValue){
		this.map.put(metadataName, metadataValue);
	}
	
	public void removeMetadata(PageMetadataEnum metadataName){
		this.map.remove(metadataName);
	}
	
	public HashMap<String,String> getMetadataAsMap(){
		HashMap<String, String> metaMap = new HashMap<String,String>();
		
		Iterator iter = map.entrySet().iterator();
	    while (iter.hasNext()) {
	        Map.Entry<PageMetadataEnum,String> pairs = (Map.Entry)iter.next();
	        //PageMetadataEnum key = pairs.getKey();
	        metaMap.put(pairs.getKey().toString(),pairs.getValue());
	    }
		return metaMap;
	}
	
}
