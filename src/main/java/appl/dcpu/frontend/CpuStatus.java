package appl.dcpu.frontend;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import appl.dcpu.processor.Cpu;

public class CpuStatus extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;
	private final Cpu cpu;
	private JButton step;
	private JButton run;
	private JLabel registers;
	private final MemoryDump memoryDump;
	private JButton stop;

	public CpuStatus(Cpu cpu, MemoryDump memoryDump) {
		this.memoryDump = memoryDump;
		this.setBorder(new TitledBorder("Cpu Status"));
		this.cpu = cpu;
		setPreferredSize(new Dimension(800, 100));
		setMinimumSize(new Dimension(800, 100));
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		addButtons();
		addRegisterPanel();
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
			memoryDump.setDump();
			registers.setText(cpu.toString());
		} else if (e.getSource() == run){ 
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
