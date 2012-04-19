package appl.dcpu.utility;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class Utils {

	public static List<String> readLines(BufferedReader br) throws IOException {
		List<String> result = new ArrayList<String>();
		String line;
		while ((line = br.readLine()) != null) {
			result.add(line);
		}
		return result;
	}

	public static void closeQuietly(Writer bw) {
		try {
			if (bw != null) {
				bw.close();
			}
		} catch (IOException e) {
			
		}
	}

	public static void closeQuietly(Reader br) {
		try {
			if (br != null) {
				br.close();
			}
		} catch (IOException e) {
			
		}	
	}
	
	public static int fromHex(String text) {
		if (text.startsWith("0x") || text.startsWith("0X")) {
			return Integer.parseInt(text.substring(2), 16);
		}
		return Integer.parseInt(text);
	}
	
	public static String toHex(int value) {
		return String.format("0x%04x", value);
	}

	public static void closeQuietly(DataInputStream br) {
		try {
			if (br != null) {
				br.close();
			}
		} catch (IOException e) {
			
		}
	}

	public static void closeQuietly(DataOutputStream dos) {
		try {
			if (dos != null) {
				dos.close();
			}
		} catch (IOException e) {
			
		}
	}

}
