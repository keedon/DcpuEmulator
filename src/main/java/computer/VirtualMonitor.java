package computer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

public class VirtualMonitor
{
  public static final int WIDTH_CHARS = 32;
  public static final int HEIGHT_CHARS = 12;
  public static final int WIDTH_PIXELS = 128;
  public static final int HEIGHT_PIXELS = 96;
  private final char[] ram;
  private final int offset;
  private final int charOffset;
  private final int miscDataOffset;
  private final int[] colorBase = new int[256];
  private final int[] colorOffs = new int[256];
  private int lightColor;
  public int[] pixels = new int[16384];

  public VirtualMonitor(char[] ram, int offset) {
    this.ram = ram;
    this.offset = offset;
    this.charOffset = (offset + 384);

    this.miscDataOffset = (this.charOffset + 256);

    for (int i = 0; i < 256; i++) {
      int bg = genColor(i % 16);
      int fg = genColor(i / 16);
      this.colorBase[i] = bg;
      this.colorOffs[i] = (fg - bg);
    }

    int[] pixels = new int[4096];
    try {
      ImageIO.read(VirtualMonitor.class.getResource("font.png")).getRGB(0, 0, 128, 32, pixels, 0, 128);
    } catch (IOException e) {
      e.printStackTrace();
    }

    for (int c = 0; c < 128; c++) {
      int ro = this.charOffset + c * 2;
      int xo = c % 32 * 4;
      int yo = c / 32 * 8;
      ram[(ro + 0)] = '\000';
      ram[(ro + 1)] = '\000';
      for (int xx = 0; xx < 4; xx++) {
        int bb = 0;
        for (int yy = 0; yy < 8; yy++)
          if ((pixels[(xo + xx + (yo + yy) * 128)] & 0xFF) > 128)
            bb |= 1 << yy;
        int tmp275_274 = (ro + xx / 2);
        char[] tmp275_267 = ram; tmp275_267[tmp275_274] = (char)(tmp275_267[tmp275_274] | bb << (xx + 1 & 0x1) * 8);
      }
    }
  }

  private static int genColor(int i) {
    int b = (i >> 0 & 0x1) * 170;
    int g = (i >> 1 & 0x1) * 170;
    int r = (i >> 2 & 0x1) * 170;
    if (i == 6) {
      b += 85;
    } else if (i >= 8) {
      r += 85;
      g += 85;
      b += 85;
    }
    return 0xFF000000 | r << 16 | g << 8 | b;
  }

  public void render() {
    long time = System.currentTimeMillis() / 16L;
    boolean blink = time / 20L % 2L == 0L;
    long reds = 0L;
    long greens = 0L;
    long blues = 0L;
    for (int y = 0; y < 12; y++) {
      for (int x = 0; x < 32; x++) {
        char dat = this.ram[(this.offset + x + y * 32)];
        int ch = dat & 0x7F;
        int colorIndex = dat >> '\b' & 0xFF;
        int co = this.charOffset + ch * 2;

        int color = this.colorBase[colorIndex];
        int colorAdd = this.colorOffs[colorIndex];
        if ((blink) && ((dat & 0x80) > 0)) colorAdd = 0;
        int pixelOffs = x * 4 + y * 8 * 128;

        for (int xx = 0; xx < 4; xx++) {
          int bits = this.ram[(co + (xx >> 1))] >> (xx + 1 & 0x1) * 8 & 0xFF;
          for (int yy = 0; yy < 8; yy++) {
            int col = color + colorAdd * (bits >> yy & 0x1);
            this.pixels[(pixelOffs + xx + yy * 128)] = col;
            reds += (col & 0xFF0000);
            greens += (col & 0xFF00);
            blues += (col & 0xFF);
          }
        }
      }
    }

    int color = this.colorBase[(this.ram[this.miscDataOffset] & 0xF)];
    for (int y = 96; y < 128; y++) {
      for (int x = 0; x < 128; x++) {
        this.pixels[(x + y * 128)] = color;
      }
    }

    int borderPixels = 100;
    reds += (color & 0xFF0000) * borderPixels;
    greens += (color & 0xFF00) * borderPixels;
    blues += (color & 0xFF) * borderPixels;

    reds = reds / (12288 + borderPixels) & 0xFF0000;
    greens = greens / (12288 + borderPixels) & 0xFF00;
    blues = blues / (12288 + borderPixels) & 0xFF;
    this.lightColor = (int)(reds | greens | blues);
  }

  public int getBackgroundColor() {
    return this.colorBase[(this.ram[this.miscDataOffset] & 0xF)];
  }

  public void setPixels(int[] pixels) {
    this.pixels = pixels;
  }

  public int getLightColor() {
    return this.lightColor;
  }
}