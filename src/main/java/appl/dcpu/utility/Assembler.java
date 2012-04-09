package appl.dcpu.utility;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Assembler {
	public class AssemblyResult {
		public final String hexResult;
		public final String listing;
		
		public AssemblyResult(String hex, String listing) {
			hexResult = hex;
			this.listing = listing;
		}
	}

	private static final String REGISTERS = "abcxyzij";
	private static final Map<String, Integer> opCodes = new HashMap<String, Integer>();
    static {
    	opCodes.put("dat", -1);
        opCodes.put("set", 1);
        opCodes.put("add", 2);
        opCodes.put("sub", 3);
        opCodes.put("mul", 4);
        opCodes.put("div", 5);
        opCodes.put("mod", 6);
        opCodes.put("shl", 7);
        opCodes.put("shr", 8);
        opCodes.put("and", 9);
        opCodes.put("bor", 10);
        opCodes.put("xor", 11);
        opCodes.put("ife", 12);
        opCodes.put("ifn", 13);
        opCodes.put("ifg", 14);
        opCodes.put("ifb", 15);
        opCodes.put("jsr", 16);
    }

	private Map<String, Integer> labelDictionary;
	private StreamTokenizer tokeniser;
	private final String program;
	private int currentPC;
	private StringBuilder listing;
	private String[] programForListing;
	
	public Assembler(String program) {
		this.program = program;
		programForListing = program.split("\n");
		labelDictionary = new HashMap<String, Integer>();
	}
	
	public AssemblyResult assemble() {
		try {
			// Pass 1
			listing = new StringBuilder();
			currentPC = 0;
			createTokeniser();
			runPass();
			// Pass 2
			listing = new StringBuilder();
			currentPC = 0;
			createTokeniser();
			return new AssemblyResult(runPass(), listing.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String runPass() throws IOException {
		List<String> result = new ArrayList<String>();
		tokeniser.nextToken();
		while (tokeniser.ttype != StreamTokenizer.TT_EOF) {
			try {
				if (tokeniser.ttype == StreamTokenizer.TT_EOL) {
					tokeniser.nextToken();
					continue;
				}
				int beforePC = currentPC;
				String currentLine = programForListing[tokeniser.lineno() - 1];
				if (tokenAsString().equals(":")) {
					labelDefinition();
				} else {
					if (tokeniser.ttype != StreamTokenizer.TT_EOF) {
						processOpcode(result);
					}
					listing.append('\n').append(toHex(beforePC)).append(": ");
					for (int i = beforePC; i < currentPC; i++) {
						listing.append(result.get(i)).append(' ');
					}
					listing.append('\t').append(currentLine);
				}
				tokeniser.nextToken();
			} catch (RuntimeException e) {
				listing.append('\n');
				listing.append(programForListing[tokeniser.lineno() - 1]).append('\n');
				listing.append("*** ").append(e.getMessage());
				while (tokeniser.ttype != StreamTokenizer.TT_EOL && tokeniser.ttype != StreamTokenizer.TT_EOF) {
					tokeniser.nextToken();
				}
			}
		}
		return asString(result);
	}

	private String asString(List<String> result) {
		StringBuilder builder = new StringBuilder();
		for (String hex : result) {
			builder.append(hex).append(' ');
		}
		return builder.toString();
	}

	private void processOpcode(List<String> result) throws IOException {
		Integer opCode = opCodes.get(tokenAsString());
		if (opCode != null) {
			if (opCode < 0) {
				processDirective(opCode, result);
			} else if (opCode > 15) {
				processNonBasic(opCode, result);
			} else {
				processBasic(opCode, result);
			}
		} else {
			throw new RuntimeException("Unknown opcode " + tokeniser);
		}
	}

	private void processDirective(Integer opCode, List<String> result) throws IOException {
		switch (opCode) {
		case -1:
			// dat
			tokeniser.nextToken();
			while (tokeniser.ttype != StreamTokenizer.TT_EOL) {
				if (tokenAsString().startsWith("\"")) {
					addString(result);
				} else if (isNumeric(tokenAsString().substring(0, 1))) {
					addNumber(result);
				} else if (labelDictionary.containsKey(tokenAsString())) {
					result.add(toHex(labelDictionary.get(tokenAsString())));
					currentPC++;
				} else {
					// Add a zero - assume we're in pass 1
					result.add("0000");
					currentPC++;
				}
				tokeniser.nextToken();
				tokeniser.nextToken();
			}
		}
	}

	private void addNumber(List<String> result) {
		int value = convertToNumber(tokenAsString());
		processLiteral(result, value);
	}

	private void addString(List<String> result) throws IOException {
		tokeniser.nextToken();
		while (!tokenAsString().equals("\"")) {
			for (byte ch : tokenAsString().getBytes()) {
				result.add(toHex(ch));
				currentPC++;
			}
			tokeniser.nextToken();
		}
	}

	private void processNonBasic(Integer opCode, List<String> result) throws IOException {
		int opcodeLocation = currentPC;
		result.add("");
		currentPC++;
		int a = processArg(result);
		int finalOp = opCode + (a << 10);
		result.set(opcodeLocation, toHex(finalOp));		
	}

	private void processBasic(Integer opCode, List<String> result) throws IOException {
		int opcodeLocation = currentPC;
		result.add("");
		currentPC++;
		int a = processArg(result);
		tokeniser.nextToken();
		if (tokeniser.ttype != ((int) ',')) {
			throw new RuntimeException("Missing comma");
		}
		int b = processArg(result);
		int finalOp = opCode + (a << 4) + (b << 10);
		result.set(opcodeLocation, toHex(finalOp));
	}

	private String toHex(int finalOp) {
		return String.format("%04x", finalOp);
	}

	private int processArg(List<String> result) throws IOException {
		tokeniser.nextToken();
		if (labelDictionary.containsKey(tokenAsString())) {
			int value = labelDictionary.get(tokenAsString());
			return processLiteral(result, value);
		}
		if (REGISTERS.contains(tokenAsString())) {
			return REGISTERS.indexOf(tokenAsString());
		}
		if (isNumeric(tokenAsString().substring(0, 1))) {
			int value = convertToNumber(tokenAsString());
			return processLiteral(result, value);
		}
		if (tokenAsString().equals("[sp++]") || tokenAsString().equals("pop")) {
			return 0x18;
		}
		if (tokenAsString().equals("[sp]") || tokenAsString().equals("peek")) {
			return 0x19;
		}
		if (tokenAsString().equals("[--sp]") || tokenAsString().equals("push")) {
			return 0x1a;
		}
		if (tokenAsString().equals("sp")) {
			return 0x1b;
		}
		if (tokenAsString().equals("pc")) {
			return 0x1c;
		}
		if (tokenAsString().equals("o")) {
			return 0x1d;
		}
		if (tokenAsString().startsWith("[")) {
			return processIndirect(result);
		}
		throw new RuntimeException("Unable to parse argument " + tokeniser);
	}

	private int processLiteral(List<String> result, int value) {
		if (value < 0x1f) {
			return value + 0x20;
		}
		result.add(toHex(value));
		currentPC++;
		return 0x1f;	// Next word
	}

	private int convertToNumber(String number) {
		//  maybe 0x????
		int value;
		if (number.substring(1).startsWith("x")) {
			value = Integer.parseInt(number.substring(2), 16);
		} else {
			value = Integer.parseInt(number, 10);
		}
		return value;
	}

	private boolean isNumeric(String character) {
		return "0123456789".contains(character);
	}
	
	private String tokenAsString() {
		if (tokeniser.sval != null) {
			return tokeniser.sval;
		}
		return new String(new char[] {(char)tokeniser.ttype}, 0, 1);
	}

	private int processIndirect(List<String> result) throws IOException {
		String fullToken = tokenAsString();
		while (!tokenAsString().endsWith("]")) {
			tokeniser.nextToken();
			if (tokeniser.ttype == StreamTokenizer.TT_EOL) {
				throw new RuntimeException("Unterminated indirect address");
			}
			fullToken += tokenAsString();
		}
		fullToken = fullToken.replaceAll("[\\[\\]]", "");
		if (REGISTERS.contains(fullToken)) {
			return REGISTERS.indexOf(fullToken) + 8;
		}
		if (isNumeric(fullToken.substring(0, 1))) {
			String[] parts = fullToken.split("\\+");
			int value = convertToNumber(parts[0]);
			result.add(toHex(value));
			currentPC++;
			if (parts.length > 1) {
				return REGISTERS.indexOf(parts[1]) + 0x10;
			} else {
				return 0x1e;
			}
		}
		return 0;
	}

	private void labelDefinition() throws IOException {
		tokeniser.nextToken();
		if (tokeniser.ttype == StreamTokenizer.TT_WORD) {
			labelDictionary.put(tokenAsString(), currentPC);
		}
	}

	private void createTokeniser() {
		StringReader r = new StringReader(program);
		tokeniser = new StreamTokenizer(r);
		tokeniser.resetSyntax();
		tokeniser.commentChar(';');
		tokeniser.eolIsSignificant(true);
		tokeniser.lowerCaseMode(true);
		tokeniser.wordChars('0', '9');
		tokeniser.wordChars('a', 'z');
		tokeniser.wordChars('A', 'Z');
		tokeniser.whitespaceChars(0, ' ');
	}
}
