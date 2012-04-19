package appl.dcpu.frontend;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import appl.dcpu.processor.Cpu;
import appl.dcpu.processor.StateChangedListener;
import appl.dcpu.utility.Disassembler;
import appl.dcpu.utility.Disassembler.Disassembled;
import appl.dcpu.utility.Utils;

public class DisassemblerGui extends JPanel implements StateChangedListener {
	private static final long serialVersionUID = 1L;
	private final Cpu cpu;
	private JTextArea disArea;
	private Disassembler disassembler;

	public DisassemblerGui(Cpu cpu) {
		this.cpu = cpu;
		cpu.addStateChangedListener(this);
		disArea = new JTextArea();
		JScrollPane scroll = new JScrollPane(disArea);
		Font font = new Font("Courier", Font.PLAIN, 14);
		disArea.setFont(font);
		this.add(scroll);
		this.setMaximumSize(new Dimension(400, 700));
		disassembler = new Disassembler();
		cpuStateChanged();
	}

	@Override
	public void cpuStateChanged() {
		StringBuilder sb = new StringBuilder();
		int pc = cpu.getPC();
		for (int line = 0; line < 20; line++) {
			Disassembled disassembled = disassembler.disassemble(cpu.getMem(), pc);
			sb.append(Utils.toHex(pc)).append(": ");
			for (int i = 0; i < 3; i++) {
				if (i < disassembled.words.length) {
					sb.append(Utils.toHex(disassembled.words[i])).append(" ");
				} else {
					sb.append("       ");
				}
			}
			pc = disassembled.nextInstruction;
			sb.append(disassembled.line).append('\n');
		}
		disArea.setText(sb.toString());
	}

}
