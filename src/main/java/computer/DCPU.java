package computer;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.DataInputStream;
import java.io.EOFException;

import javax.swing.JFrame;

public class DCPU {
	public char[] ram = new char[65536];
	public char pc;
	public char sp;
	public char o;
	public char[] registers = new char[8];
	public int cycles;
	private static volatile boolean stop = false;
	private static final int khz = 100;

	public int getAddr(int type) {
		if (type >= 32)
			return 0x20000 | type & 0x1F;

		switch (type & 0xF8) {
		case 0:
			return 65536 + (type & 0x7);
		case 8:
			return this.registers[(type & 0x7)];
		case 16:
			this.cycles += 1;
			return this.ram[(this.pc++)] + this.registers[(type & 0x7)]
					& 0xFFFF;
		case 24:
			switch (type & 0x7) {
			case 0:
				return this.sp++ & 0xFFFF;
			case 1:
				return this.sp & 0xFFFF;
			case 2:
				return (this.sp = (char) (this.sp - '\001')) & 0xFFFF;
			case 3:
				return 65544;
			case 4:
				return 65545;
			case 5:
				return 65552;
			case 6:
				this.cycles += 1;
				return this.ram[(this.pc++)];
			}
			this.cycles += 1;
			return 0x20000 | this.ram[(this.pc++)];
		}

		throw new IllegalStateException("Illegal value type " + type
				+ "! How did you manage that!?");
	}

	public char get(int addr) {
		if (addr < 65536)
			return this.ram[(addr & 0xFFFF)];
		if (addr < 65544)
			return this.registers[(addr & 0x7)];
		if (addr >= 131072)
			return (char) addr;
		if (addr == 65544)
			return this.sp;
		if (addr == 65545)
			return this.pc;
		if (addr == 65552) {
			return this.o;
		}
		throw new IllegalStateException("Illegal address "
				+ Integer.toHexString(addr) + "! How did you manage that!?");
	}

	public void set(int addr, char val) {
		if (addr < 65536)
			this.ram[(addr & 0xFFFF)] = val;
		else if (addr < 65544)
			this.registers[(addr & 0x7)] = val;
		else if (addr < 131072) {
			if (addr == 65544)
				this.sp = val;
			else if (addr == 65545)
				this.pc = val;
			else if (addr == 65552)
				this.o = val;
			else
				throw new IllegalStateException("Illegal address "
						+ Integer.toHexString(addr)
						+ "! How did you manage that!?");
		}
	}

	public static int getInstructionLength(char opcode) {
		int len = 1;
		int cmd = opcode & 0xF;
		if (cmd == 0) {
			cmd = opcode >> '\004' & 0xF;
			if (cmd > 0) {
				int atype = opcode >> '\n' & 0x3F;
				if (((atype & 0xF8) == 16) || (atype == 31) || (atype == 30))
					len++;
			}
		} else {
			int atype = opcode >> '\004' & 0x3F;
			int btype = opcode >> '\n' & 0x3F;
			if (((atype & 0xF8) == 16) || (atype == 31) || (atype == 30))
				len++;
			if (((btype & 0xF8) == 16) || (btype == 31) || (btype == 30))
				len++;
		}
		return len;
	}

	public void skip() {
		this.cycles += 1;
		this.pc = (char) (this.pc + getInstructionLength(this.ram[(this.pc++)]));
	}

	public void tick() {
		this.cycles += 1;
		char opcode = this.ram[(this.pc++)];
		int cmd = opcode & 0xF;
		if (cmd == 0) {
			cmd = opcode >> '\004' & 0xF;
			if (cmd != 0) {
				int atype = opcode >> '\n' & 0x3F;
				int aaddr = getAddr(atype);
				char a = get(aaddr);

				switch (cmd) {
				case 1:
					char tmp104_103 = (char) (this.sp - '\001');
					this.sp = tmp104_103;
					this.ram[(tmp104_103 & 0xFFFF)] = (char) (this.pc - '\002' + getInstructionLength(opcode));
					this.pc = a;
				}
			}
		} else {
			int atype = opcode >> '\004' & 0x3F;
			int btype = opcode >> '\n' & 0x3F;

			int aaddr = getAddr(atype);
			char a = get(aaddr);
			int baddr = getAddr(btype);
			char b = get(baddr);

			switch (cmd) {
			case 1:
				a = b;
				break;
			case 2:
				this.cycles += 1;
				int val = a + b;
				a = (char) val;
				this.o = (char) (val >> 16);
				break;
			case 3:
				this.cycles += 1;
				int val3 = a - b;
				a = (char) val3;
				this.o = (char) (val3 >> 16);
				break;
			case 4:
				this.cycles += 1;
				int val4 = a * b;
				a = (char) val4;
				this.o = (char) (val4 >> 16);
				break;
			case 5:
				this.cycles += 2;
				if (b == 0) {
					a = this.o = 0;
				} else {
					long val5 = (a << '\020') / b;
					a = (char) (int) (val5 >> 16);
					this.o = (char) (int) val5;
				}
				break;
			case 6:
				this.cycles += 2;
				if (b == 0)
					a = '\000';
				else {
					a = (char) (a % b);
				}
				break;
			case 7:
				this.cycles += 1;
				long val7 = a << b;
				a = (char) (int) val7;
				this.o = (char) (int) (val7 >> 16);
				break;
			case 8:
				this.cycles += 1;
				long val8 = a << '\020' - b;
				a = (char) (int) (val8 >> 16);
				this.o = (char) (int) val8;
				break;
			case 9:
				a = (char) (a & b);
				break;
			case 10:
				a = (char) (a | b);
				break;
			case 11:
				a = (char) (a ^ b);
				break;
			case 12:
				this.cycles += 1;
				if (a != b)
					skip();
				return;
			case 13:
				this.cycles += 1;
				if (a == b)
					skip();
				return;
			case 14:
				this.cycles += 1;
				if (a <= b)
					skip();
				return;
			case 15:
				this.cycles += 1;
				if ((a & b) == 0)
					skip();
				return;
			}

			set(aaddr, a);
		}
	}

	private static void testCpus(int cpuCount, char[] ram) {
		DCPU[] cpus = new DCPU[cpuCount];
		for (int i = 0; i < cpuCount; i++) {
			cpus[i] = new DCPU();
			for (int j = 0; j < 65536; j++) {
				cpus[i].ram[j] = ram[j];
			}
		}

		long ops = 0L;
		int hz = 100000;
		int cyclesPerFrame = hz / 60;

		long nsPerFrame = 16666666L;
		long nextTime = System.nanoTime();

		double tick = 0.0D;
		double total = 0.0D;

		long startTime = System.currentTimeMillis();
		while (!stop) {
			long a = System.nanoTime();
			while (System.nanoTime() < nextTime) {
				try {
					Thread.sleep(1L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			long b = System.nanoTime();
			for (int j = 0; j < cpuCount; j++) {
				while (cpus[j].cycles < cyclesPerFrame) {
					cpus[j].tick();
				}
				cpus[j].cycles -= cyclesPerFrame;
			}
			long c = System.nanoTime();
			ops += cyclesPerFrame;
			nextTime += nsPerFrame;

			tick += (c - b) / 1000000000.0D;
			total += (c - a) / 1000000000.0D;
		}

		long passedTime = System.currentTimeMillis() - startTime;
		System.out.println(cpuCount + " DCPU at " + ops / passedTime + " khz, "
				+ tick * 100.0D / total + "% cpu use");
	}

	private static void attachDisplay(final DCPU cpu) {
		final VirtualMonitor display = new VirtualMonitor(cpu.ram, 32768);
		final VirtualKeyboard keyboard = new VirtualKeyboard(cpu.ram, 36864,
				new AWTKeyMapping());
		Thread t = new Thread() {
			public void run() {
				try {
					int SCALE = 3;
					JFrame frame = new JFrame();

					Canvas canvas = new Canvas();
					canvas.setPreferredSize(new Dimension(160 * SCALE,
							128 * SCALE));
					canvas.setMinimumSize(new Dimension(160 * SCALE,
							128 * SCALE));
					canvas.setMaximumSize(new Dimension(160 * SCALE,
							128 * SCALE));
					canvas.setFocusable(true);
					canvas.addKeyListener(new KeyListener() {
						public void keyPressed(KeyEvent ke) {
							keyboard.keyPressed(ke.getKeyCode());
						}

						public void keyReleased(KeyEvent ke) {
							keyboard.keyReleased(ke.getKeyCode());
						}

						public void keyTyped(KeyEvent ke) {
							keyboard.keyTyped(ke.getKeyChar());
						}
					});
					frame.add(canvas);
					frame.pack();
					frame.setLocationRelativeTo(null);
					frame.setResizable(false);
					frame.setDefaultCloseOperation(3);
					frame.setVisible(true);

					BufferedImage img2 = new BufferedImage(160, 128, 2);
					BufferedImage img = new BufferedImage(128, 128, 2);
					display.setPixels(((DataBufferInt) img.getRaster()
							.getDataBuffer()).getData());

					canvas.requestFocus();
					while (true) {
						display.render();
						Graphics g = img2.getGraphics();
						g.setColor(new Color(display.getBackgroundColor()));
						g.fillRect(0, 0, 160, 128);
						g.drawImage(img, 16, 16, 128, 128, null);
						g.dispose();

						g = canvas.getGraphics();
						g.drawImage(img2, 0, 0, 160 * SCALE, 128 * SCALE, null);
						g.dispose();
						Thread.sleep(1L);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		t.start();
	}

	private static void testCpu(char[] ram) {
		DCPU cpu = new DCPU();
		for (int j = 0; j < 65536; j++) {
			cpu.ram[j] = ram[j];
		}
		attachDisplay(cpu);

		long ops = 0L;
		int hz = 100000;
		int cyclesPerFrame = hz / 60;

		long nsPerFrame = 16666666L;
		long nextTime = System.nanoTime();

		double tick = 0.0D;
		double total = 0.0D;

		long time = System.currentTimeMillis();
		while (!stop) {
			long a = System.nanoTime();
			while (System.nanoTime() < nextTime) {
				try {
					Thread.sleep(1L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			long b = System.nanoTime();
			while (cpu.cycles < cyclesPerFrame) {
				cpu.tick();
			}
			cpu.cycles -= cyclesPerFrame;
			long c = System.nanoTime();
			ops += cyclesPerFrame;
			nextTime += nsPerFrame;

			tick += (c - b) / 1000000000.0D;
			total += (c - a) / 1000000000.0D;

			while (System.currentTimeMillis() > time) {
				time += 1000L;
				System.out.println("1 DCPU at " + ops / 1000.0D + " khz, "
						+ tick * 100.0D / total + "% cpu use");
				tick = total = ops = 0L;
			}
		}
	}

	public static void main(String[] args) throws Exception {
		final DCPU cpu = new DCPU();

		DataInputStream dis = new DataInputStream(
				DCPU.class.getResourceAsStream("mem.dmp"));
		try {
			for (int i = 0;; i++) {
				char ch = dis.readChar();
				cpu.ram[i] = ch;
			}
		} catch (EOFException e) {
			e.printStackTrace();

			dis.close();

			dump(cpu.ram, 0, 768);
			if (args.length == 0) {
				testCpu(cpu.ram);
				return;
			}

			int threads = args.length > 0 ? Integer.parseInt(args[0]) : 1;
			final int cpusPerCore = args.length > 1 ? Integer.parseInt(args[1])
					: 100;
			int seconds = args.length > 2 ? Integer.parseInt(args[2]) : 5;

			System.out.println("Aiming at 100 khz, with " + cpusPerCore
					+ " DCPUs per thread, on " + threads + " threads.");
			System.out.println("");
			System.out.println("Running test for " + seconds + " seconds..");

			for (int i = 0; i < threads; i++) {
				new Thread() {
					public void run() {
						DCPU.testCpu(cpu.ram);
					}
				}.start();
			}

			for (int i = seconds; i > 0; i--) {
				System.out.println(i + "..");
				Thread.sleep(1000L);
			}
			stop = true;
		}
	}

	private static void dump(char[] ram, int start, int len) {
		for (int i = 0; i < len;) {
			String str = Integer.toHexString(i);
			while (str.length() < 4)
				str = "0" + str;
			System.out.print(str + ":");

			for (int j = 0; (j < 8) && (i < len); i++) {
				String str2 = Integer.toHexString(ram[i]);
				while (str2.length() < 4)
					str2 = "0" + str;
				System.out.print(" " + str);

				j++;
			}

			System.out.println();
		}
	}
}