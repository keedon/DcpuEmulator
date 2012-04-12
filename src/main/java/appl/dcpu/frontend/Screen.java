package appl.dcpu.frontend;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.JPanel;

public class Screen extends JPanel {

	private static final long serialVersionUID = 1L;
	public static final int SCREEN_WIDTH = 32;
	public static final int SCREEN_HEIGHT = 16;
	
	private static final Color BLACK = new Color(0, 0, 0);
	private static final Color RED = new Color(205, 0, 0);
	private static final Color GREEN = new Color(0, 205, 0);
	private static final Color YELLOW = new Color(205, 205, 0);
	private static final Color BLUE = new Color(0, 0, 238);
	private static final Color MAGENTA = new Color(205, 0, 205);
	private static final Color CYAN = new Color(0, 205, 205);
	private static final Color WHITE = new Color(229, 229, 229);
	private static final Color LIGHT_BLACK = new Color(127, 127, 127);
	private static final Color LIGHT_RED = new Color(255, 0, 0);
	private static final Color LIGHT_GREEN = new Color(0, 255, 0);
	private static final Color LIGHT_YELLOW = new Color(255, 255, 0);
	private static final Color LIGHT_BLUE = new Color(92, 92, 255);
	private static final Color LIGHT_MAGENTA = new Color(255, 0, 255);
	private static final Color LIGHT_CYAN = new Color(0, 255, 255);
	private static final Color LIGHT_WHITE = new Color(255, 255, 255);

	
	private Color[] ANSI_COLORS = {BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE,
			LIGHT_BLACK, LIGHT_RED, LIGHT_GREEN, LIGHT_YELLOW, LIGHT_BLUE, LIGHT_MAGENTA, LIGHT_CYAN, LIGHT_WHITE};
	
	private int[] screenText = new int[SCREEN_WIDTH * SCREEN_HEIGHT];
	private Font font;
	private int charWidth;
	private int charHeight;

	public Screen() {
		font = new Font("Courier", Font.PLAIN, 14);
		this.setFont(font);
		setPreferredSize(new Dimension(500, 500));
	}
	
	public void setMem(int pos, int ch) {
		screenText[pos] = ch;
		repaint();
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		FontMetrics fontMetrics = g.getFontMetrics(font);
		charHeight = fontMetrics.getHeight();
		charWidth = fontMetrics.charWidth(' ');
		for (int pos = 0; pos < screenText.length; pos++) {
			int ch = screenText[pos];
			char character = (char) (ch & 0x7f);
			if (character == 0) character = ' ';
			int x = (pos * charWidth) % (SCREEN_WIDTH * charWidth);
			int y = ((pos * charWidth) / (SCREEN_WIDTH * charWidth)) * charHeight;
			g.setColor(ANSI_COLORS[(ch >> 7) & 15]);
			g.fillRect(x, y, charWidth, charHeight);
			g.setColor(ANSI_COLORS[ch >> 12]);
			g.drawChars(new char[] {character}, 0, 1, x, y + charHeight);
		}
	}

	public int getMem(int pos) {
		return screenText[pos];
	}
	
}
