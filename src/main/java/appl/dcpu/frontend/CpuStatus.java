package appl.dcpu.frontend;

import java.awt.Dimension;

import javax.swing.JPanel;

import appl.dcpu.processor.Cpu;

public class CpuStatus extends JPanel {

	private static final long serialVersionUID = 1L;
	private final Cpu cpu;

	public CpuStatus(Cpu cpu) {
		this.cpu = cpu;
		setPreferredSize(new Dimension(800, 100));
	}
}
