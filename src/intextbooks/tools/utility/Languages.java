package intextbooks.tools.utility;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Languages {

	/**
	 * Holds a mapping from English language names to their corresponding ISO
	 * 639-1 codes
	 */

	public final static Map<String, String> langMap = constructIsoMap();

	private static Map<String, String> constructIsoMap() {

		Map<String, String> langMap = new HashMap<String, String>();

		for (String iso : Locale.getISOLanguages()) {

			Locale l = new Locale(iso);
			langMap.put(l.getDisplayLanguage(Locale.ENGLISH), iso);

		}

		return langMap;

	}

}
