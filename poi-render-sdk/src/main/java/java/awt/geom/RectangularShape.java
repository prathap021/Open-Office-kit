package java.awt.geom;

public abstract class RectangularShape implements Cloneable {
    public abstract double getX();
    public abstract double getY();
    public abstract double getWidth();
    public abstract double getHeight();
    public abstract double getMinX();
    public abstract double getMinY();
    public abstract double getMaxX();
    public abstract double getMaxY();
    public abstract double getCenterX();
    public abstract double getCenterY();
    public abstract boolean isEmpty();
}
