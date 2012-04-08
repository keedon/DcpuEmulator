package appl.dcpu.frontend;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.commons.io.IOUtils;

import appl.dcpu.processor.Cpu;
import appl.dcpu.utility.Assembler;

public class FrontendPanel extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	private final JFileChooser fc = new JFileChooser();

	public static void main(String args[]) {
		FrontendPanel fep = new FrontendPanel();
		fep.start();
	}

	private JPanel mainFrame;
	private Screen screen;
	private CpuStatus cpuStatus;
	private Container content;
	private JMenuBar menuBar;
	private Cpu cpu;

	private void start() {
		createFrame();
		addScreen();
		createProcessor();
		addMenu();
		addCpuStatus();
		setPreferredSize(new Dimension(1024, 768));
	    pack();
	    setVisible(true);
	    drawBootMessage();
	}

	private void createProcessor() {
		cpu = new Cpu(screen);
	}

	private void createFrame() {
		content = getContentPane();
	    content.setLayout(new BorderLayout());
		mainFrame = new JPanel();
		mainFrame.setBackground(Color.lightGray);
		mainFrame.setLayout(new BorderLayout());
		mainFrame.setVisible(true);
		content.add(mainFrame, BorderLayout.CENTER);
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
		cpuStatus = new CpuStatus(cpu);
		content.add(cpuStatus, BorderLayout.SOUTH);
	}

	private void drawBootMessage() {
		String message = "****** DCPU Emulator Booted V0.1 ******";
		for (int i = 0; i< message.length(); i++) {
			screen.setMem(i, message.charAt(i) + 0x6380);
		}
	}

	private void addScreen() {
		screen = new Screen();
	    content.add(screen, BorderLayout.CENTER);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Load")) {
			if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(this)) {
				File dumpFile = fc.getSelectedFile();
				cpu.loadFile(dumpFile);
			}
			cpu.reset();
		}
		if (e.getActionCommand().equals("Run")) {
			cpu.start();
		}
		if (e.getActionCommand().equals("Assemble")) {
			if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(this)) {
				File sourceFile = fc.getSelectedFile();
				Assembler asm = new Assembler(loadFile(sourceFile));
				String assembledCode = asm.assemble();
				File destFile = new File(sourceFile.getPath().replace(".asm", "") + ".hex");
				writeFile(destFile, assembledCode);
				JOptionPane.showMessageDialog(this, "Assembly completed - output in " + destFile);
			}
		}
	}

	private void writeFile(File destFile, String assembledCode) {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(destFile));
			IOUtils.write(assembledCode, bw);
		} catch (IOException e) {
			throw new RuntimeException(e);
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
			throw new RuntimeException(e);
		} finally {
			IOUtils.closeQuietly(br);
		}
	}
}
