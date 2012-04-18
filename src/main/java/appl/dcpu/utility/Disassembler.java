package appl.dcpu.utility;

import scala.actors.threadpool.Arrays;

public class Disassembler {
	
	private class ProcessedArg {
		public final String arg;
		public final int newOffset;
		
		public ProcessedArg(String arg, int offset) {
			this.arg = arg;
			newOffset = offset;
		}
	}
	
	public class Disassembled {
		public final String line;
		public final int[] words;
		public final int nextInstruction;
		
		public Disassembled(String line, int next, int[] words) {
			this.line = line;
			nextInstruction = next;
			this.words = words;
		}
	}
	
	private String[] instructions = { "", "set", "add", "sub", "mul", "div", "mod", "shl", "shr", "and", "bor", "xor", "ife", "ifn", "ifg", "ifb" };
	private String[] registers = {"a", "b", "c", "x", "y", "z", "i", "j"};
	private String[] special = {"pop", "sp", "push", "sp", "pc", "o"};

	public Disassembled disassemble(int[] mem, int offset) {
		StringBuilder res = new StringBuilder();
		int opCode = mem[offset];
		res.append("    ");
		int op = opCode & 0xf;
		if (op == 0) {
			return handleNonBasic(opCode, mem, offset + 1);
		}
		res.append(instructions[op]).append("    ");
		ProcessedArg a = processArg((opCode >> 4) & 0x3f, mem, offset + 1);
		ProcessedArg b = processArg((opCode >> 10) & 0x3f, mem, a.newOffset);
		res.append(a.arg).append(',').append(b.arg);
		return new Disassembled(res.toString(), b.newOffset, Arrays.copyOfRange(mem, offset, b.newOffset));
	}


	private Disassembled handleNonBasic(int opCode, int[] mem, int offset) {
		int nonBasicOp = (opCode >> 4) & 0x3f;
		switch (nonBasicOp) {
		case 1:
			ProcessedArg a = processArg((opCode >> 10) & 0x3f, mem, offset);
			return new Disassembled("    JSR  " + a.arg, a.newOffset, Arrays.copyOfRange(mem, offset, a.newOffset));
		default:
			return new Disassembled(Utils.toHex(opCode), offset, Arrays.copyOfRange(mem, offset, offset));
		}
	}

	private ProcessedArg processArg(int arg, int[] mem, int offset) {
		if (arg <= 7) {
			return new ProcessedArg(registers[arg], offset);
		}
		if (arg <= 15) {
			return new ProcessedArg("[" + registers[arg - 8] + "]", offset);
		}
		if (arg <= 0x17) {
			return new ProcessedArg("[" + Utils.toHex(mem[offset]) + "+" + registers[arg - 16] + "]", offset + 1);
		}
		if (arg <= 0x1d) {
			return new ProcessedArg(special[arg - 0x18], offset);
		}
		if (arg == 0x1e) {
			return new ProcessedArg("[" + Utils.toHex(mem[offset]) + "]", offset + 1);
		}
		if (arg == 0x1f) {
			return new ProcessedArg(Utils.toHex(mem[offset]), offset + 1);
		}
		return new ProcessedArg(Utils.toHex(arg - 0x20), offset);
	}
}
