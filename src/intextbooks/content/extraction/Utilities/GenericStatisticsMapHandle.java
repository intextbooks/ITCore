package intextbooks.content.extraction.Utilities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public class GenericStatisticsMapHandle {
	
	public static Pair<Number,Number> getBiggestKeys(Map<? extends Number,? extends Number> map) {
		//remove outcasts
		Iterator<Number> it = (Iterator<Number>) map.keySet().iterator();
		while(it.hasNext()) {
			if(map.get(it.next()).intValue() == 1) {
				it.remove();
			}
		}
		
		//find biggest key
		Number biggestKey = null;
		boolean flag = true;
		for(Number key: map.keySet()) {
			if(flag) {
				biggestKey = key;
				flag = false;
			}else {
				if(key.doubleValue() > biggestKey.doubleValue()) {
					biggestKey = key;
				}
			}		
		}
		
		//find key of the biggest value
		Number biggestValue = null;
		Number keyBiggestValue = null;
		flag = true;
		for(Number key: map.keySet()) {
			if(flag) {
				biggestValue = map.get(key);
				flag = false;
				keyBiggestValue = key;
			}else {
				if(map.get(key).doubleValue() > biggestValue.doubleValue()) {
					biggestValue = map.get(key);
					keyBiggestValue = key;
				}
			}
		}
		
		return Pair.of(biggestKey, keyBiggestValue);
	}
	
	public static Number getSmallestKey(Map<? extends Number,? extends Number> map) {
		//remove outcasts
		Iterator<Number> it = (Iterator<Number>) map.keySet().iterator();
		while(it.hasNext()) {
			if(map.get(it.next()).intValue() == 1) {
				it.remove();
			}
		}
		
		//find biggest key
		Number biggestKey = null;
		boolean flag = true;
		for(Number key: map.keySet()) {
			if(flag) {
				biggestKey = key;
				flag = false;
			}else {
				if(key.doubleValue() < biggestKey.doubleValue()) {
					biggestKey = key;
				}
			}		
		}
		
		return biggestKey;
	}

	public static void main(String[] args) {
		HashMap<Float, Integer> map = new HashMap<Float, Integer>();
		map.put(40f, 8);
		map.put(60f, 1);
		map.put(9f, 30);
		
		Pair<Number,Number> p = GenericStatisticsMapHandle.getBiggestKeys(map);
		System.out.println(p);

	}

}
