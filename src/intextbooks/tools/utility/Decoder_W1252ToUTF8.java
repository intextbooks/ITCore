package intextbooks.tools.utility;

public class Decoder_W1252ToUTF8 {

	public Decoder_W1252ToUTF8(){
		
	}
	
	
	public String decode(String srcStr){
		String decStr = srcStr;
		
		decStr = decStr.replaceAll("%80", "%E2%82%AC");decStr = decStr.replaceAll("%82", "%E2%80%9A");decStr = decStr.replaceAll("%83", "%C6%92");decStr = decStr.replaceAll("%84", "%E2%80%9E");decStr = decStr.replaceAll("%85", "%E2%80%A6");decStr = decStr.replaceAll("%86", "%E2%80%A0");decStr = decStr.replaceAll("%87", "%E2%80%A1");decStr = decStr.replaceAll("%88", "%CB%86");decStr = decStr.replaceAll("%89", "%E2%80%B0");decStr = decStr.replaceAll("%8A", "%C5%A0");decStr = decStr.replaceAll("%8B", "%E2%80%B9");decStr = decStr.replaceAll("%8C", "%C5%92");decStr = decStr.replaceAll("%8D", "%C5%8D");decStr = decStr.replaceAll("%8E", "%C5%BD");decStr = decStr.replaceAll("%90", "%C2%90");decStr = decStr.replaceAll("%91", "%E2%80%98");decStr = decStr.replaceAll("%92", "%E2%80%99");decStr = decStr.replaceAll("%93", "%E2%80%9C");decStr = decStr.replaceAll("%94", "%E2%80%9D");decStr = decStr.replaceAll("%95", "%E2%80%A2");decStr = decStr.replaceAll("%96", "%E2%80%93");decStr = decStr.replaceAll("%97", "%E2%80%94");decStr = decStr.replaceAll("%98", "%CB%9C");decStr = decStr.replaceAll("%99", "%E2%84");decStr = decStr.replaceAll("%9A", "%C5%A1");decStr = decStr.replaceAll("%9B", "%E2%80");decStr = decStr.replaceAll("%9C", "%C5%93");decStr = decStr.replaceAll("%9E", "%C5%BE");decStr = decStr.replaceAll("%9F", "%C5%B8");decStr = decStr.replaceAll("%A0", "%C2%A0");decStr = decStr.replaceAll("%A1", "%C2%A1");decStr = decStr.replaceAll("%A2", "%C2%A2");decStr = decStr.replaceAll("%A3", "%C2%A3");decStr = decStr.replaceAll("%A4", "%C2%A4");decStr = decStr.replaceAll("%A5", "%C2%A5");decStr = decStr.replaceAll("%A6", "%C2%A6");decStr = decStr.replaceAll("%A7", "%C2%A7");decStr = decStr.replaceAll("%A8", "%C2%A8");decStr = decStr.replaceAll("%A9", "%C2%A9");decStr = decStr.replaceAll("%AA", "%C2%AA");decStr = decStr.replaceAll("%AB", "%C2%AB");decStr = decStr.replaceAll("%AC", "%C2%AC");decStr = decStr.replaceAll("%AD", "%C2%AD");decStr = decStr.replaceAll("%AE", "%C2%AE");decStr = decStr.replaceAll("%AF", "%C2%AF");decStr = decStr.replaceAll("%B0", "%C2%B0");decStr = decStr.replaceAll("%B1", "%C2%B1");decStr = decStr.replaceAll("%B2", "%C2%B2");decStr = decStr.replaceAll("%B3", "%C2%B3");decStr = decStr.replaceAll("%B4", "%C2%B4");decStr = decStr.replaceAll("%B5", "%C2%B5");decStr = decStr.replaceAll("%B6", "%C2%B6");decStr = decStr.replaceAll("%B7", "%C2%B7");decStr = decStr.replaceAll("%B8", "%C2%B8");decStr = decStr.replaceAll("%B9", "%C2%B9");decStr = decStr.replaceAll("%BA", "%C2%BA");decStr = decStr.replaceAll("%BB", "%C2%BB");decStr = decStr.replaceAll("%BC", "%C2%BC");decStr = decStr.replaceAll("%BD", "%C2%BD");decStr = decStr.replaceAll("%BE", "%C2%BE");decStr = decStr.replaceAll("%BF", "%C2%BF");decStr = decStr.replaceAll("%C0", "%C3%80");decStr = decStr.replaceAll("%C1", "%C3%81");decStr = decStr.replaceAll("%C2", "%C3%82");decStr = decStr.replaceAll("%C3", "%C3%83");decStr = decStr.replaceAll("%C4", "%C3%84");decStr = decStr.replaceAll("%C5", "%C3%85");decStr = decStr.replaceAll("%C6", "%C3%86");decStr = decStr.replaceAll("%C7", "%C3%87");decStr = decStr.replaceAll("%C8", "%C3%88");decStr = decStr.replaceAll("%C9", "%C3%89");decStr = decStr.replaceAll("%CA", "%C3%8A");decStr = decStr.replaceAll("%CB", "%C3%8B");decStr = decStr.replaceAll("%CC", "%C3%8C");decStr = decStr.replaceAll("%CD", "%C3%8D");decStr = decStr.replaceAll("%CE", "%C3%8E");decStr = decStr.replaceAll("%CF", "%C3%8F");decStr = decStr.replaceAll("%D0", "%C3%90");decStr = decStr.replaceAll("%D1", "%C3%91");decStr = decStr.replaceAll("%D2", "%C3%92");decStr = decStr.replaceAll("%D3", "%C3%93");decStr = decStr.replaceAll("%D4", "%C3%94");decStr = decStr.replaceAll("%D5", "%C3%95");decStr = decStr.replaceAll("%D6", "%C3%96");decStr = decStr.replaceAll("%D7", "%C3%97");decStr = decStr.replaceAll("%D8", "%C3%98");decStr = decStr.replaceAll("%D9", "%C3%99");decStr = decStr.replaceAll("%DA", "%C3%9A");decStr = decStr.replaceAll("%DB", "%C3%9B");decStr = decStr.replaceAll("%DC", "%C3%9C");decStr = decStr.replaceAll("%DD", "%C3%9D");decStr = decStr.replaceAll("%DE", "%C3%9E");decStr = decStr.replaceAll("%DF", "%C3%9F");decStr = decStr.replaceAll("%E0", "%C3%A0");decStr = decStr.replaceAll("%E1", "%C3%A1");decStr = decStr.replaceAll("%E2", "%C3%A2");decStr = decStr.replaceAll("%E3", "%C3%A3");decStr = decStr.replaceAll("%E4", "%C3%A4");decStr = decStr.replaceAll("%E5", "%C3%A5");decStr = decStr.replaceAll("%E6", "%C3%A6");decStr = decStr.replaceAll("%E7", "%C3%A7");decStr = decStr.replaceAll("%E8", "%C3%A8");decStr = decStr.replaceAll("%E9", "%C3%A9");decStr = decStr.replaceAll("%EA", "%C3%AA");decStr = decStr.replaceAll("%EB", "%C3%AB");decStr = decStr.replaceAll("%EC", "%C3%AC");decStr = decStr.replaceAll("%ED", "%C3%AD");decStr = decStr.replaceAll("%EE", "%C3%AE");decStr = decStr.replaceAll("%EF", "%C3%AF");decStr = decStr.replaceAll("%F0", "%C3%B0");decStr = decStr.replaceAll("%F1", "%C3%B1");decStr = decStr.replaceAll("%F2", "%C3%B2");decStr = decStr.replaceAll("%F3", "%C3%B3");decStr = decStr.replaceAll("%F4", "%C3%B4");decStr = decStr.replaceAll("%F5", "%C3%B5");decStr = decStr.replaceAll("%F6", "%C3%B6");decStr = decStr.replaceAll("%F7", "%C3%B7");decStr = decStr.replaceAll("%F8", "%C3%B8");decStr = decStr.replaceAll("%F9", "%C3%B9");decStr = decStr.replaceAll("%FA", "%C3%BA");decStr = decStr.replaceAll("%FB", "%C3%BB");decStr = decStr.replaceAll("%FC", "%C3%BC");decStr = decStr.replaceAll("%FD", "%C3%BD");decStr = decStr.replaceAll("%FE", "%C3%BE");decStr = decStr.replaceAll("%FF", "%C3%BF");
		
		return decStr;
	}
	
}
