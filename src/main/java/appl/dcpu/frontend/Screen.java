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
	
	private static final Color BLACK = new Color(0x000000);
	private static final Color RED = new Color(0x883932);
	private static final Color GREEN = new Color(0x55A049);
	private static final Color YELLOW = new Color(0xBFCE72);
	private static final Color BLUE = new Color(0x40318D);
	private static final Color PURPLE = new Color(0x8B3F96);
	private static final Color CYAN = new Color(0x67B6BD);
	private static final Color WHITE = new Color(0xFFFFFF);
	private static final Color DARK_GREY = new Color(0x505050);
	private static final Color GREY = new Color(0x787878);
	private static final Color LIGHT_RED = new Color(0xB86962);
	private static final Color ORANGE = new Color(0x8B5429);
	private static final Color BROWN = new Color(0x574200);
	private static final Color LIGHT_GREEN = new Color(0x94E089);
	private static final Color LIGHT_BLUE = new Color(0x7869C4);
	private static final Color LIGHT_GREY = new Color(0x9F9F9F);
	
	private Color[] ANSI_COLORS = {BLACK, RED, GREEN, YELLOW, BLUE, PURPLE, CYAN, WHITE,
									DARK_GREY, GREY, LIGHT_RED, ORANGE, BROWN, LIGHT_GREEN, LIGHT_BLUE, LIGHT_GREY};
	
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
