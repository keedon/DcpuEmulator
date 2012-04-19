package appl.dcpu.frontend;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import appl.dcpu.processor.Cpu;
import appl.dcpu.utility.Assembler;
import appl.dcpu.utility.Assembler.AssemblyResult;
import appl.dcpu.utility.Utils;

public class FrontendPanel extends JFrame implements ActionListener, KeyListener {

	private static final long serialVersionUID = 1L;
	
	private final JFileChooser fc = new JFileChooser();

	public static void main(String args[]) {
		FrontendPanel fep = new FrontendPanel();
		fep.start();
	}

	private Screen screen;
	private CpuStatus cpuStatus;
	private Container content;
	private JMenuBar menuBar;
	private Cpu cpu;
	private JTextField keyboardBuffer;

	private byte[] ringBuffer = new byte[16];
	private int position = 0;

	private MemoryDump memoryDump;
	private DisassemblerGui disassembler;

	private JPanel topPanel;

	private JPanel bottomPanel;

	private JPanel sidePanel;

	private void start() {
		createFrame();
		addScreen();
		addKeyboard();
		createProcessor();
		addDisassembler();
		addMemoryDump();
		addMenu();
		addCpuStatus();
		setPreferredSize(new Dimension(1024, 768));
	    pack();
	    setVisible(true);
	    drawBootMessage();
	}

	private void addMemoryDump() {
		memoryDump = new MemoryDump(cpu);
		topPanel.add(memoryDump);
	}

	private void addKeyboard() {
		addKeyListener(this);
	}

	private void createProcessor() {
		cpu = new Cpu(screen, ringBuffer);
	}

	private void createFrame() {
		content = getContentPane();
	    content.setLayout(new BorderLayout());
	    topPanel = new JPanel();
	    topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.LINE_AXIS));
	    sidePanel = new JPanel();
	    sidePanel.setLayout(new GridLayout(2, 1));
	    topPanel.add(sidePanel);
	    content.add(topPanel, BorderLayout.CENTER);
	    bottomPanel = new JPanel();
	    bottomPanel.setLayout(new GridLayout(1, 1));
	    content.add(bottomPanel, BorderLayout.SOUTH);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		try {
			fc.setCurrentDirectory(new File(new File(".").getCanonicalPath()));
		} catch (IOException e) {

		}
	}

	private void addMenu() {
		menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenuItem loadAction = new JMenuItem("Load");
		loadAction.addActionListener(this);
		fileMenu.add(loadAction );
		JMenuItem assembleAction = new JMenuItem("Assemble");
		assembleAction.addActionListener(this);
		fileMenu.add(assembleAction);
		menuBar.add(fileMenu );
		setJMenuBar(menuBar);
	}

	private void addCpuStatus() {
		cpuStatus = new CpuStatus(cpu, memoryDump);
		bottomPanel.add(cpuStatus);
	}

	private void drawBootMessage() {
		String message = "** DCPU Emulator Booted V0.1 **";
		for (int i = 0; i< message.length(); i++) {
			screen.setMem(i, message.charAt(i) + 0xf000);
		}
	}

	private void addScreen() {
		screen = new Screen();
		sidePanel.add(screen);
	}
	
	private void addDisassembler() {
		JPanel disasmHolder = new JPanel();
		disasmHolder.setBorder(BorderFactory.createTitledBorder("Disassembly"));
		disassembler = new DisassemblerGui(cpu);
		disasmHolder.add(disassembler);
		sidePanel.add(disasmHolder);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Load")) {
			loadHexFile();
		}
		if (e.getActionCommand().equals("Assemble")) {
			runAssembler();
		}
	}

	private void loadHexFile() {
		if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(this)) {
			File dumpFile = fc.getSelectedFile();
			cpu.loadFile(dumpFile);
		}
		cpu.reset();
		cpuStatus.cpuLoaded();
	}

	private void runAssembler() {
		if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(this)) {
			File sourceFile = fc.getSelectedFile();
			Assembler asm = new Assembler(loadFile(sourceFile));
			AssemblyResult assembledCode = asm.assemble();
			File listingFile = new File(sourceFile.getPath().replace(".dasm16", "") + ".list");
			writeFile(listingFile, assembledCode.listing);
			if (assembledCode.success) {
				File destFile = new File(sourceFile.getPath().replace(".dasm16", "") + ".hex");
				writeFile(destFile, assembledCode.hexResult);
				JOptionPane.showMessageDialog(this, "Assembly completed - output in " + destFile);
			} else {
				JOptionPane.showMessageDialog(this, "Assembly failed - see " + listingFile);
			}
		}
	}

	private void writeFile(File destFile, String hexResult) {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(destFile));
			bw.write(hexResult);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Unable to write " + destFile);
		} finally {
			Utils.closeQuietly(bw);
		}
	}
	
	private void writeFile(File destFile, char[] hexResult) {
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new FileOutputStream(destFile));
			for (char hex : hexResult) {
				dos.writeChar(hex);
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Unable to write " + destFile);
		} finally {
			Utils.closeQuietly(dos);
		}
	}

	private String loadFile(File sourceFile) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(sourceFile));
			StringBuilder sb = new StringBuilder();
			List<String> lines = Utils.readLines(br);
			for (String line : lines) {
				sb.append(line).append('\n');	
			}
			return sb.toString();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Couldn't load file " + sourceFile);
			return "";
		} finally {
			Utils.closeQuietly(br);
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
		synchronized (keyboardBuffer) {
			ringBuffer[position] = (byte) e.getKeyChar();
			position = (position + 1) % 16;
			keyboardBuffer.setText(new String(ringBuffer));
		}
	}
}
