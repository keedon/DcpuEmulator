package appl.dcpu.utility;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class Assembler {
	public class AssemblyResult {
		public final String hexResult;
		public final String listing;
		public final boolean success;
		
		public AssemblyResult(String hex, String listing, boolean success) {
			hexResult = hex;
			this.listing = listing;
			this.success = success;
		}
	}
	
	private static final String REGISTERS = "abcxyzij";
	private static final Map<String, Integer> opCodes = new HashMap<String, Integer>();
    static {
    	opCodes.put("equ", -3);
    	opCodes.put("include", -2);
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
    
    private class Label {
    	public int value;
    	public boolean canBeShort;
    }

	private Map<String, Label> labelDictionary;
	private StreamTokenizer tokeniser;
	private final String program;
	private int currentPC;
	private StringBuilder listing;
	private String[] programForListing;
	private boolean success;
	private List<String> result;
	private int pass;
	private Label lastLabel;
	
	public Assembler(String program) {
		this.program = program;
		programForListing = program.split("\n");
		labelDictionary = new TreeMap<String, Label>();
		success = true;
	}
	
	public AssemblyResult assemble() {
		try {
			// Pass 1
			pass = 1;
			runPass();
			// Pass 2
			pass++;
			String hex = runPass();
			addLabelDictionary();
			return new AssemblyResult(hex, listing.toString(), success);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String runPass() throws IOException {
		result = new ArrayList<String>();
		listing = new StringBuilder();
		currentPC = 0;
		createTokeniser();
		success = true;
		while (tokeniser.ttype != StreamTokenizer.TT_EOF) {
			int beforePC = currentPC;
			int currentLine = tokeniser.lineno();
			int lastLine = currentLine;
			try {
				currentLine = tokeniser.lineno();
				checkForLabel();
				checkForOpCode();
				toListing(beforePC, lastLine, currentLine);
				skipRestOfLine();
				lastLine = currentLine;
			} catch (RuntimeException e) {
				toListing(beforePC, lastLine, currentLine);
				listing.append("\n*** ").append(e.getMessage());
				skipRestOfLine();
				success = false;
			}
		}
		return asString();
	}

	private void checkForOpCode() throws IOException {
		if (tokeniser.ttype == StreamTokenizer.TT_WORD) {
			processOpcode();
			tokeniser.nextToken();
		}
	}

	private void checkForLabel() throws IOException {
		if (tokenAsString().equals(":")) {
			labelDefinition();
			tokeniser.nextToken();
		}
	}

	private void toListing(int beforePC, int lastLine, int currentLine) {
		for (int i = lastLine; i <= currentLine; i++) {
			listing.append('\n').append(i).append(' ').append(toHex(beforePC)).append(": ").append(instWords(beforePC)).append(programForListing[i -1]);
		}
	}

	private String instWords(int beforePC) {
		StringBuilder words = new StringBuilder();
		for (int i = beforePC; i < currentPC; i++) {
			words.append(result.get(i)).append(' ');
		}
		return words.toString();
	}

	private void skipRestOfLine() throws IOException {
		while (tokeniser.ttype != StreamTokenizer.TT_EOL && tokeniser.ttype != StreamTokenizer.TT_EOF) {
			tokeniser.nextToken();
		}
		tokeniser.nextToken();
	}

	private String asString() {
		StringBuilder builder = new StringBuilder();
		for (String hex : result) {
			builder.append(hex).append(' ');
		}
		return builder.toString();
	}

	private void processOpcode() throws IOException {
		Integer opCode = opCodes.get(tokenAsString());
		if (opCode != null) {
			if (opCode < 0) {
				processDirective(opCode);
			} else if (opCode > 15) {
				processNonBasic(opCode);
			} else {
				processBasic(opCode);
			}
		} else {
			throw new RuntimeException("Unknown opcode " + tokeniser);
		}
	}

	private void processDirective(Integer opCode) throws IOException {
		switch (opCode) {
		case -1:
			// dat
			processDat();
			break;
		case -2:
			// include
			break;
		case -3:
			// equ
			equ();
			break;
		}
	}

	private void equ() throws IOException {
		tokeniser.nextToken();
		if (isNumeric(tokenAsString().substring(0, 1))) {
			lastLabel.value = convertToNumber(tokenAsString());
		} else if (labelDictionary.containsKey(tokenAsString())) {
			lastLabel.value = labelDictionary.get(tokenAsString()).value;
		}
	}

	private void processDat() throws IOException {
		tokeniser.nextToken();
		while (tokeniser.ttype != StreamTokenizer.TT_EOL) {
			if (tokenAsString().startsWith(";")) {
				skipRestOfLine();
				return;
			}
			if (tokenAsString().startsWith("\"")) {
				addString();
			} else if (isNumeric(tokenAsString().substring(0, 1))) {
				addNumber();
			} else if (labelDictionary.containsKey(tokenAsString())) {
				result.add(toHex(labelDictionary.get(tokenAsString()).value));
				currentPC++;
			} else {
				// Add a zero - assume we're in pass 1 and it's a label
				result.add("0000");
				currentPC++;
			}
			tokeniser.nextToken();
			if (tokenAsString().equals(",")) {
				tokeniser.nextToken();
			} else {
				tokeniser.pushBack();
				return;
			}
		}
	}

	private void addNumber() {
		int value = convertToNumber(tokenAsString());
		result.add(toHex(value));
		currentPC++;
	}

	private void addString() throws IOException {
		tokeniser.nextToken();
		while (!tokenAsString().equals("\"")) {
			for (byte ch : tokenAsString().getBytes()) {
				result.add(toHex(ch));
				currentPC++;
			}
			tokeniser.nextToken();
		}
	}

	private void processNonBasic(Integer opCode) throws IOException {
		int opcodeLocation = currentPC;
		result.add("");
		currentPC++;
		int a = processArg();
		int finalOp = opCode + (a << 10);
		result.set(opcodeLocation, toHex(finalOp));		
	}

	private void processBasic(Integer opCode) throws IOException {
		int opcodeLocation = currentPC;
		result.add("");
		currentPC++;
		int a = processArg();
		tokeniser.nextToken();
		if (tokeniser.ttype != ((int) ',')) {
			throw new RuntimeException("Missing comma");
		}
		int b = processArg();
		int finalOp = opCode + (a << 4) + (b << 10);
		result.set(opcodeLocation, toHex(finalOp));
	}

	private String toHex(int finalOp) {
		return String.format("%04x", finalOp);
	}

	private int processArg() throws IOException {
		tokeniser.nextToken();
		if (tokenAsString().startsWith(";")) {
			throw new RuntimeException("Missing argument");
		}
		if (labelDictionary.containsKey(tokenAsString())) {
			return addLabel(tokenAsString());
		}
		if (REGISTERS.contains(tokenAsString())) {
			return REGISTERS.indexOf(tokenAsString());
		}
		if (isNumeric(tokenAsString().substring(0, 1))) {
			int value = convertToNumber(tokenAsString());
			return processLiteral(value);
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
			return processIndirect();
		}
		if (pass == 1) {
			Label label = new Label();
			label.value = 0;
			label.canBeShort = false;
			labelDictionary.put(tokenAsString(), label);
			result.add("0000");
			currentPC++;
			return 0x1e;	// [Next word]
		} else {
			throw new RuntimeException("Unable to parse argument " + tokeniser);
		}
	}

	private int addLabel(String labelName) {
		Label label = labelDictionary.get(labelName);
		int value = label.value;
		if (label.canBeShort) {
			return processLiteral(value);
		}
		// For forward references, assume a long form literal
		result.add(toHex(value));
		currentPC++;
		return 0x1f;	// Next word
	}

	private int processLiteral(int value) {
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
		return "0123456789-".contains(character);
	}
	
	private String tokenAsString() {
		if (tokeniser.sval != null) {
			return tokeniser.sval;
		}
		return new String(new char[] {(char)tokeniser.ttype}, 0, 1);
	}

	private int processIndirect() throws IOException {
		String fullToken = "";
		while (!tokenAsString().endsWith("]")) {
			tokeniser.nextToken();
			if (tokeniser.ttype == StreamTokenizer.TT_EOL) {
				throw new RuntimeException("Unterminated indirect address");
			}
			fullToken += tokenAsString();
		}
		fullToken = fullToken.replaceAll("[\\]]", "");
		if (REGISTERS.contains(fullToken)) {
			return REGISTERS.indexOf(fullToken) + 8;
		}
		if (fullToken.contains("+")) {
			// Register indirect
			String[] parts = fullToken.split("\\+");
			if (labelDictionary.containsKey(parts[0])) {
				result.add(toHex(labelDictionary.get(parts[0]).value));
				currentPC++;
			} else if (isNumeric(parts[0])) {
				int value = convertToNumber(parts[0]);
				result.add(toHex(value));
				currentPC++;
			} else if (pass == 1) {
				result.add("0000");
				currentPC++;
			} else {
				throw new RuntimeException("Unable to parse register + offset");
			}
			return REGISTERS.indexOf(parts[1]) + 0x10;
		}
		if (isNumeric(fullToken.substring(0, 1))) {
			int value = convertToNumber(fullToken);
			result.add(toHex(value));
			currentPC++;
			return 0x1e;
		}
		if (labelDictionary.containsKey(fullToken)) {
			result.add(toHex(labelDictionary.get(fullToken).value));
			currentPC++;
		} else {
			// Assume pass 1
			Label label = new Label();
			label.value = 0;
			label.canBeShort = false;
			labelDictionary.put(fullToken, label);
			result.add("0000");
			currentPC++;
		}
		return 0x1e;	// [Next word]
	}

	private void labelDefinition() throws IOException {
		tokeniser.nextToken();
		if (tokeniser.ttype == StreamTokenizer.TT_WORD) {
			if (labelDictionary.containsKey(tokenAsString())) {
				labelDictionary.get(tokenAsString()).value = currentPC;
				lastLabel = labelDictionary.get(tokenAsString());
			} else {
				lastLabel = new Label();
				lastLabel.value = currentPC;
				lastLabel.canBeShort = true;
				labelDictionary.put(tokenAsString(), lastLabel);
			}
		}
	}

	private void createTokeniser() throws IOException {
		StringReader r = new StringReader(program);
		tokeniser = new StreamTokenizer(r);
		tokeniser.resetSyntax();
		tokeniser.eolIsSignificant(true);
		tokeniser.lowerCaseMode(true);
		tokeniser.wordChars('0', '9');
		tokeniser.wordChars('a', 'z');
		tokeniser.wordChars('A', 'Z');
		tokeniser.wordChars('_', '_');
		tokeniser.wordChars('-', '-');
		tokeniser.whitespaceChars(0, ' ');
		tokeniser.nextToken();
	}
	
	private void addLabelDictionary() {
		listing.append("\n\n======== Label Directory ========\n\n");
		for (Entry<String, Label> entry : labelDictionary.entrySet()) {
			listing.append(toHex(entry.getValue().value & 0xffff)).append('\t').append(entry.getKey()).append('\n');
		}
	}
}
