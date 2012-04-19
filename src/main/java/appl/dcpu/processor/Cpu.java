package appl.dcpu.processor;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import appl.dcpu.frontend.Screen;
import appl.dcpu.utility.Utils;

import computer.DCPU;

public class Cpu implements Runnable {
	private Thread cpuThread;
	private boolean stopCpu;
	private long stopAddress;
	private List<StateChangedListener> listeners;
	private DCPU cpu;
	private int updates = 0;
	
	public Cpu(Screen screen, byte[] ringBuffer) {
		cpu = new DCPU();
		DCPU.attachDisplay(cpu);
		listeners = new ArrayList<StateChangedListener>();
	}

	public void loadFile(File dumpFile) {
		DataInputStream dis = null; 
		
		try {
			dis = new DataInputStream(new FileInputStream(dumpFile));
			for (int i = 0;; i++) {
				char ch = dis.readChar();
				cpu.ram[i] = ch;
			}
		} catch (EOFException e) {
			// IGnore - expected
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			Utils.closeQuietly(dis);
			informListeners();
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
		stopCpu = false;
		while(!stopCpu && cpu.pc != stopAddress) {
			step();
		}
		stopCpu = true;
	}
	
	public void step() {
		cpu.tick();
		updates++;
		if ((updates % 100) == 0) {
			informListeners();
		}
	}
	
	private char getMem(int addr, boolean clearKeyboard) {
		return cpu.ram[addr];
	}
	

	public String toString() {
		return String.format("A=%04x B=%04x C=%04x X=%04x Y=%04x Z=%04x I=%04x J=%04x PC=%04x SP=%04x O=%04x",
				(short)cpu.registers[0], (short)cpu.registers[1], 
				(short)cpu.registers[2], (short)cpu.registers[3], 
				(short)cpu.registers[4], (short)cpu.registers[5], 
				(short)cpu.registers[6], (short)cpu.registers[7], (short)cpu.pc, (short)cpu.sp, (short)cpu.o);
	}

	public boolean isRunning() {
		return cpuThread != null && stopCpu == false;
	}

	public int getWordAt(int i) {
		return getMem(i, false);
	}

	public void setStopAddress(long l) {
		this.stopAddress = l;
	}

	public void addStateChangedListener(StateChangedListener listener) {
		listeners.add(listener);
	}
	
	private void informListeners() {
		for (StateChangedListener listener : listeners) {
			listener.cpuStateChanged();
		}
	}

	public int getPC() {
		return cpu.pc;
	}

	public char[] getMem() {
		return cpu.ram;
	}

}
