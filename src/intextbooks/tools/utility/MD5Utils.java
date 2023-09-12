package intextbooks.tools.utility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

public class MD5Utils {

	public static String getCheckum(String path) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(Files.readAllBytes(Paths.get(path)));
			byte[] digest = md.digest();
			String checksum = DatatypeConverter
			  .printHexBinary(digest).toUpperCase();
			return checksum;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "error";
		} 
	}
	
	public static String getDigest(String text) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(text.getBytes());
			byte[] digest = md.digest();
			return DatatypeConverter
			  .printHexBinary(digest).toUpperCase();
		} catch (Exception e) {
			e.printStackTrace();
			return text;
		}
	}
}
