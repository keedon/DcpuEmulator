package appl.dcpu.processor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import appl.dcpu.frontend.Screen;
import appl.dcpu.utility.Disassembler;
import appl.dcpu.utility.IOUtils;

public class Cpu implements Runnable {
	private static final int SCREEN_ADDRESS = 0x8000;
	private static final int MEM_SIZE = 65536;
	private static final int MAX_INT = 32767;
	private static final int MIN_INT = -32768;
	private static final int KEYBOARD_ADDRESS = 0x9000;
	
	private Thread cpuThread = null;
	private boolean stopCpu;
	private int[] memory;
	private int[] registers;
	private int PC;
	private int SP;
	private int O;
	private boolean skip = false;
	private final Screen screen;
	private final byte[] ringBuffer;
	private Disassembler disassembler;
	
	public Cpu(Screen screen, byte[] ringBuffer) {
		this.screen = screen;
		this.ringBuffer = ringBuffer;
		memory = new int[MEM_SIZE];
		registers =  new int[8];
		disassembler = new Disassembler();
	}

	public void loadFile(File dumpFile) {
		BufferedReader reader = null; 
		
		try {
			reader = new BufferedReader(new FileReader(dumpFile));
			int addr = 0;
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				String[] words = line.split(" ");
				for (String word : words) {
					setMem(addr++, Integer.parseInt(word, 16));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	public void reset() {
		try {
			if (cpuThread != null) {
				stopCpu = true;
				cpuThread.join(0);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		stopCpu = false;
	}
	
	public void start() {
		cpuThread = new Thread(this);
		cpuThread.start();
	}
	public void stop() {
		try {
			stopCpu = true;
			cpuThread.join(0);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while(!stopCpu) {
			step();
		}
	}
	
	public void step() {
		int val, ea, eb;
		int eaNext = 0, ebNext = 0;
		int inst = getMem(PC++, true);
		int op = inst & 15;
		int a = (inst >> 4) & 0x3f;
		int b = (inst >> 10) & 0x3f;
		if (addressModeNeedsNextWord(a)) {
			// Get next
			eaNext = PC++;
		}
		if (addressModeNeedsNextWord(b)) {
			ebNext = PC++;
		}
		if (skip) {
			skip = false;
		} else {
			switch(op) {
			case 1:
				setEa(a, eaNext, getEa(b, ebNext));
				break;
			case 2:
				val = getEa(a, eaNext) + getEa(b, ebNext);
				setO(val);
				setEa(a, eaNext, val);
				break;
			case 3:
				val = getEa(a, eaNext) - getEa(b, ebNext);
				setO(val);
				setEa(a, eaNext, val);
				break;
			case 4:
				val = getEa(a, eaNext) * getEa(b, ebNext);
				O = (val>>16) & 0xffff;
				setEa(a, eaNext, val);
				break;
			case 5:
				ea = getEa(a, eaNext);
				eb = getEa(b, ebNext);
				if (eb == 0) {
					O = 0;
					setEa(a, eaNext, 0);
				} else {
					val = ea / eb;
					O = ((ea<<16) / eb) & 0xffff;
					setEa(a, eaNext, val);
				}
				break;
			case 6:
				eb = getEa(b, ebNext);
				if (eb == 0) {
					setEa(a, eaNext, 0);
				} else {
					setEa(a, eaNext, getEa(a, eaNext) % eb);
				}
				break;
			case 7:
				eb = getEa(b, ebNext);
				ea = getEa(a, eaNext);
				setEa(a, eaNext, ea << eb);
				O = ((ea<<eb)>>16) & 0xffff;
				break;
			case 8:
				eb = getEa(b, ebNext);
				ea = getEa(a, eaNext);
				setEa(a, eaNext, ea >> eb);
				O = ((ea<<16)>>eb)&0xffff;
				break;
			case 9:
				setEa(a, eaNext, getEa(a, eaNext) & getEa(b, ebNext));
				break;
			case 0xa:
				setEa(a, eaNext, getEa(a, eaNext) | getEa(b, ebNext));
				break;
			case 0xb:
				setEa(a, eaNext, getEa(a, eaNext) ^ getEa(b, ebNext));
				break;
			case 0xc:
				skip = getEa(a, eaNext) != getEa(b, ebNext);
				break;
			case 0xd:
				skip = getEa(a, eaNext) == getEa(b, ebNext);
			case 0xe:
				skip = getEa(a, eaNext) <= getEa(b, ebNext);
				break;
			case 0xf:
				skip = (getEa(a, eaNext) & getEa(b, ebNext)) == 0;
				break;
			case 0:
				nonBasic(inst, ebNext);
				break;
			default:
				throw new RuntimeException("Invalid opcode " + op);
			}
		}
		PC = wrapMemory(PC);
	}
	
	private boolean addressModeNeedsNextWord(int mode) {
		return (mode > 0xf && mode <= 0x17) || mode == 0x1e || mode == 0x1f;
	}

	private void nonBasic(int inst, int eaNext) {
		int op = (inst >> 4) & 0x3f;
		int a = (inst >> 10) & 0x3f;
		switch (op) {
		case 1:
			setMem(--SP, PC);
			PC = getEa(a, eaNext);
			break;
		default:
			System.out.println("Reserved opcode " + op);
		}
	}
	
	private int getEa(int ea, int eaNext) {
		if (ea <= 7) {
			return registers[ea];
		}
		if (ea <= 0xf) {
			return getMem(registers[ea - 8], true);
		}
		if (ea <= 0x17) {
			return getMem(registers[ea - 16] + getMem(eaNext, true), true);
		}
		switch (ea) {
		case 0x18:
			return getMem(SP++, true);
		case 0x19:
			return getMem(SP, true);
		case 0x1a:
			return getMem(--SP, true);
		case 0x1b:
			return SP;
		case 0x1c:
			return PC;
		case 0x1d:
			return O;
		case 0x1e:
			return getMem(getMem(eaNext, true), true);
		case 0x1f:
			return getMem(eaNext, true);
		default:
			return ea - 0x20;
		}
	}
	
	private void setO(int value) {
		O = 0;
		if (value > MAX_INT) {
			O = 1;
		}
		if (value < MIN_INT) {
			O = 0xffff;
		}
	}
	
	private void setEa(int ea, int eaNext, int value) {
		value = wrapMemory(value);
		if (ea <= 7) {
			registers[ea] = value;
			return;
		}
		if (ea <= 0xf) {
			setMem(registers[ea - 8], value);
			return;
		}
		if (ea <= 0x17) {
			setMem(registers[ea - 16] + getMem(eaNext, true), value);
			return;
		}
		switch (ea) {
		case 0x18:
			setMem(SP++, value);
			break;
		case 0x19:
			setMem(SP, value);
			break;
		case 0x1a:
			setMem(--SP, value);
			break;
		case 0x1b:
			SP = value;
			break;
		case 0x1c:
			PC = value;
			break;
		case 0x1d:
			O = value;
			break;
		case 0x1e:
			setMem(getMem(eaNext, true), value);
			break;
		case 0x1f:
			setMem(eaNext, value);
			break;
		default:
			setMem(ea - 32, value);
			break;
		}
	}
	
	private int getMem(int addr, boolean clearKeyboard) {
		addr = wrapMemory(addr);
		if (addr >= SCREEN_ADDRESS && addr <= (SCREEN_ADDRESS + 960)) {
			return screen.getMem(addr - SCREEN_ADDRESS);
		}
		if (addr >= KEYBOARD_ADDRESS && (addr <  KEYBOARD_ADDRESS + ringBuffer.length)) {
			synchronized (ringBuffer) {
				byte b = ringBuffer[addr - KEYBOARD_ADDRESS];
				if (clearKeyboard) {
					ringBuffer[addr - KEYBOARD_ADDRESS] = 0;
				}
				return b;
			}
		}
		return memory[addr];
	}
	

	private int wrapMemory(int addr) {
		return addr & (MEM_SIZE - 1);
	}
	
	private void setMem(int addr, int val) {
		addr = wrapMemory(addr);
		if (addr >= SCREEN_ADDRESS && addr <= (SCREEN_ADDRESS + 959)) {
			screen.setMem(addr - SCREEN_ADDRESS, val);
		}
		memory[addr] = val;
	}
	
	public String toString() {
		return String.format("A=%04x B=%04x C=%04x X=%04x Y=%04x Z=%04x I=%04x J=%04x PC=%04x SP=%04x O=%04x Skip=%s, next=%s",
				registers[0], registers[1], registers[2], registers[3], registers[4], registers[5], registers[6], registers[7], PC, SP, O, skip,
				disassembler.disassemble(memory, PC).line);
	}

	public boolean isRunning() {
		return cpuThread != null && stopCpu == false;
	}

	public int getWordAt(int i) {
		return getMem(i, false);
	}

}
