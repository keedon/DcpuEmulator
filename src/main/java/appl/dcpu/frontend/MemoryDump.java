package appl.dcpu.frontend;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import appl.dcpu.processor.Cpu;
import appl.dcpu.utility.Utils;

public class MemoryDump extends JPanel implements ActionListener {

	private static final int WORDS_PER_LINE = 8;

	private static final long serialVersionUID = 1L;

	private final Cpu cpu;
	private JTextArea dumpArea;
	private JTextField address;

	public MemoryDump(Cpu cpu) {
		this.setBorder(new TitledBorder("Memory"));
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		this.cpu = cpu;
		address = new JTextField(10);
		address.setText("0x0000");
		address.addActionListener(this);
		address.setMaximumSize(new Dimension(100, 30));
		add(address);
		dumpArea = new JTextArea();
		Font font = new Font("Courier", Font.PLAIN, 14);
		dumpArea.setFont(font);
		add(dumpArea);
		setDump();
	}

	public void setDump() {
		try {
			int startAddress = Utils.fromHex(address.getText());
			StringBuilder sb = new StringBuilder();
			// 32 lines of 8 words
			for (int i = 0; i < 32; i++) {
				StringBuffer hex = new StringBuffer();
				hex.append(String.format("%04x: ", startAddress + (i*8)));
				StringBuffer characters = new StringBuffer();
				characters.append("*");
				for (int j = 0; j < 8; j++) {
					int word = cpu.getWordAt(startAddress + (i*WORDS_PER_LINE) + j);
					hex.append(String.format("%04x ", word));
					addCharacter(characters, word);
				}
				characters.append("*");
				sb.append(hex.toString());
				sb.append(characters.toString());
				sb.append("\n");
			}
			dumpArea.setText(sb.toString());
		} catch (NumberFormatException e) {
			// Ignore - dodgy character
		}
	}

	private void addCharacter(StringBuffer characters, int word) {
		Character ch = new Character((char) (word & 0xff));
		if (!Character.isISOControl(ch)) {
			characters.append(ch);
		} else {
			characters.append('.');
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		setDump();
	}
}
