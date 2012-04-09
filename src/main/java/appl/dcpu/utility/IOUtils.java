package appl.dcpu.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class IOUtils {

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
			
		}	}

}
