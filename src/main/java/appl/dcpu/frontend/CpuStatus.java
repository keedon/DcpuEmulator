package appl.dcpu.frontend;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import appl.dcpu.processor.Cpu;

import com.sun.tools.example.debug.bdi.Utils;

public class CpuStatus extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;
	private final Cpu cpu;
	private JButton step;
	private JButton run;
	private JLabel registers;
	private final MemoryDump memoryDump;
	private JButton stop;
	private JTextField stopAddress;

	public CpuStatus(final Cpu cpu, final MemoryDump memoryDump) {
		this.memoryDump = memoryDump;
		this.setBorder(new TitledBorder("Cpu Status"));
		this.cpu = cpu;
		setPreferredSize(new Dimension(800, 100));
		setMinimumSize(new Dimension(800, 100));
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		addButtons();
		addStopAddress();
		addRegisterPanel();
		Timer updateTimer = new Timer();
		updateTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				memoryDump.setDump();
				registers.setText(cpu.toString());
			}
		}, 100L, 100L);
	}
	
	private void addStopAddress() {
		stopAddress = new JTextField("0x0000");
		stopAddress.setMinimumSize(new Dimension(50, 40));
		this.add(stopAddress);
	}

	public void cpuLoaded() {
		step.setEnabled(true);
		run.setEnabled(true);
		registers.setText(cpu.toString());
		memoryDump.setDump();
	}

	private void addRegisterPanel() {
		registers = new JLabel();
		this.add(registers);
		registers.setText(cpu.toString());
	}

	private void addButtons() {
		step = new JButton("Step");
		this.add(step);
		step.setEnabled(false);		// Enable when a program is loaded
		step.addActionListener(this);
		run = new JButton("Run");
		run.setEnabled(false);
		this.add(run);
		run.addActionListener(this);
		stop = new JButton("Stop");
		stop.setEnabled(false);
		this.add(stop);
		stop.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == step) {
			if (cpu.isRunning()) {
				cpu.stop();
				stop.setEnabled(false);
			}
			cpu.step();
		} else if (e.getSource() == run){ 
			Document document = stopAddress.getDocument();
			long addr;
			try {
				addr = Utils.fromHex(document.getText(0, document.getLength()));
				if (addr != 0) {
					cpu.setStopAddress(addr);
				}
			} catch (BadLocationException e2) {
			}
			if (!cpu.isRunning()) {
				cpu.start();
				stop.setEnabled(true);
			}	
		} else if (e.getSource() == stop) {
			stop.setEnabled(false);
			cpu.stop();
		}
	}
}
