package java.awt;
public class Dimension implements java.io.Serializable {
    public int width;
    public int height;
    public Dimension() { this(0, 0); }
    public Dimension(Dimension d) { this(d.width, d.height); }
    public Dimension(int width, int height) { this.width = width; this.height = height; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
}
