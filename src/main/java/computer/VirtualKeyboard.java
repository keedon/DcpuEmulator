package computer;

public class VirtualKeyboard
{
  public static final int KEY_UP = 128;
  public static final int KEY_DOWN = 129;
  public static final int KEY_LEFT = 130;
  public static final int KEY_RIGHT = 131;
  private final char[] ram;
  private final int offset;
  private int pp = 0;
  private KeyMapping keyMapping;

  public VirtualKeyboard(char[] ram, int offset, KeyMapping keyMapping)
  {
    this.ram = ram;
    this.offset = offset;
    this.keyMapping = keyMapping;
  }

  public void keyTyped(int i) {
    if ((i <= 0) || (i > 127)) return;
    if (this.ram[(this.offset + this.pp)] != 0) return;
    this.ram[(this.offset + this.pp)] = (char)i;
    this.pp = (this.pp + 1 & 0xF);
  }

  public void keyPressed(int key) {
    int i = this.keyMapping.getKey(key);
    if ((i < 80) || (i > 255)) return;
    if (this.ram[(this.offset + this.pp)] != 0) return;
    this.ram[(this.offset + this.pp)] = (char)i;
    this.pp = (this.pp + 1 & 0xF);
  }

  public void keyReleased(int key) {
    int i = this.keyMapping.getKey(key);
    if ((i < 80) || (i > 255)) return;
    if (this.ram[(this.offset + this.pp)] != 0) return;
    this.ram[(this.offset + this.pp)] = (char)(i | 0x100);
    this.pp = (this.pp + 1 & 0xF);
  }
}