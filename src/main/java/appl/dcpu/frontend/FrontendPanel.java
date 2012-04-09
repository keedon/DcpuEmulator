package appl.dcpu.frontend;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
import javax.swing.border.TitledBorder;

import appl.dcpu.processor.Cpu;
import appl.dcpu.utility.Assembler;
import appl.dcpu.utility.Assembler.AssemblyResult;
import appl.dcpu.utility.IOUtils;

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

	private JPanel upper;

	private MemoryDump memoryDump;

	private void start() {
		createFrame();
		addScreen();
		addKeyboard();
		createProcessor();
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
		upper.add(memoryDump);
	}

	private void addKeyboard() {
		addKeyListener(this);
		keyboardBuffer = new JTextField();
		keyboardBuffer.setMinimumSize(new Dimension(100, 20));
		keyboardBuffer.setBorder(new TitledBorder("Keyboard Input"));
		keyboardBuffer.setText("                ");
		keyboardBuffer.addKeyListener(this);
		content.add(keyboardBuffer);
	}

	private void createProcessor() {
		cpu = new Cpu(screen, ringBuffer);
	}

	private void createFrame() {
		content = getContentPane();
	    content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
	    upper = new JPanel();
	    upper.setLayout(new BoxLayout(upper, BoxLayout.LINE_AXIS));
	    content.add(upper);
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
		JMenuItem runAction = new JMenuItem("Run");
		runAction.addActionListener(this);
		fileMenu.add(runAction);
		JMenuItem assembleAction = new JMenuItem("Assemble");
		assembleAction.addActionListener(this);
		fileMenu.add(assembleAction);
		menuBar.add(fileMenu );
		setJMenuBar(menuBar);
	}

	private void addCpuStatus() {
		cpuStatus = new CpuStatus(cpu, memoryDump);
		content.add(cpuStatus);
	}

	private void drawBootMessage() {
		String message = "****** DCPU Emulator Booted V0.1 ******";
		for (int i = 0; i< message.length(); i++) {
			screen.setMem(i, message.charAt(i) + 0x6380);
		}
	}

	private void addScreen() {
		JPanel screenHolder = new JPanel();
		screenHolder.setBorder(BorderFactory.createTitledBorder("Screen"));
		screen = new Screen();
		screenHolder.add(screen);
		upper.add(screenHolder);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Load")) {
			loadHexFile();
		}
		if (e.getActionCommand().equals("Run")) {
			cpu.start();
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
			File listingFile = new File(sourceFile.getPath().replace(".asm", "") + ".list");
			writeFile(listingFile, assembledCode.listing);
			if (assembledCode.success) {
				File destFile = new File(sourceFile.getPath().replace(".asm", "") + ".hex");
				writeFile(destFile, assembledCode.hexResult);
				JOptionPane.showMessageDialog(this, "Assembly completed - output in " + destFile);
			} else {
				JOptionPane.showMessageDialog(this, "Assembly failed - see " + listingFile);
			}
		}
	}

	private void writeFile(File destFile, String assembledCode) {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(destFile));
			bw.write(assembledCode);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Unable to write " + destFile);
		} finally {
			IOUtils.closeQuietly(bw);
		}
	}

	private String loadFile(File sourceFile) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(sourceFile));
			StringBuilder sb = new StringBuilder();
			List<String> lines = IOUtils.readLines(br);
			for (String line : lines) {
				sb.append(line).append('\n');	
			}
			return sb.toString();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Couldn't load file " + sourceFile);
			return "";
		} finally {
			IOUtils.closeQuietly(br);
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
