package intextbooks.tools.utility;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ListOperations {
	public static <V> V findMostFrequentItem(final Collection<V> items)
	{
	  return items.stream()
	      .filter(Objects::nonNull)
	      .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).entrySet().stream()
	      .max(Comparator.comparing(Entry::getValue))
	      .map(Entry::getKey)
	      .orElse(null);
	}
	
	/* if equal, true is selected */
	public static <V> Boolean findMostFrequentItemBoolean(final Collection<Boolean> items)
	{
	  
		return items.stream()
	      .filter(Objects::nonNull)
	      .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).entrySet().stream().sorted((a,b) -> Boolean.compare(b.getKey(), a.getKey()))
	      .max(Comparator.comparing(Entry::getValue))
	      .map(Entry::getKey)
	      .orElse(null);
	}
	
	public static <V> V findMinItem(final Collection<V> items){
		return items
				.stream()
			    .min(Comparator.comparingDouble(v -> (Double) v))
			    .orElse(null);
	}
	
	public static <V> V findMaxItem(final Collection<V> items){
		return items
				.stream()
			    .max(Comparator.comparingDouble(v -> (Double) v))
			    .orElse(null);
	}
	
	public static <V> V findMinItemInteger(final Collection<V> items){
		return items
				.stream()
			    .min(Comparator.comparingInt(v -> (int) v))
			    .orElse(null);
	}
	
	public static <V> V findMaxItemInteger(final Collection<V> items){
		return items
				.stream()
				.max(Comparator.comparingInt(v -> (int) v))
			    .orElse(null);
	}
	
	public static Float findMaxFloatItem(final Collection<Float> items){
		return items
				.stream()
			    .max(Comparator.comparingDouble(v -> v))
			    .orElse(null);
	}
}
