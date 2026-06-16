package java.awt.geom;

public abstract class Rectangle2D extends RectangularShape {
    public static class Double extends Rectangle2D {
        public double x, y, width, height;
        public Double() { }
        public Double(double x, double y, double w, double h) {
            this.x = x; this.y = y; this.width = w; this.height = h;
        }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getWidth() { return width; }
        public double getHeight() { return height; }
        public double getMinX() { return x; }
        public double getMinY() { return y; }
        public double getMaxX() { return x + width; }
        public double getMaxY() { return y + height; }
        public double getCenterX() { return x + width/2; }
        public double getCenterY() { return y + height/2; }
        public boolean isEmpty() { return width <= 0 || height <= 0; }
        public void setRect(double x, double y, double w, double h) { this.x=x; this.y=y; this.width=w; this.height=h; }
    }
    public static class Float extends Rectangle2D {
        public float x, y, width, height;
        public Float() { }
        public Float(float x, float y, float w, float h) {
            this.x = x; this.y = y; this.width = w; this.height = h;
        }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getWidth() { return width; }
        public double getHeight() { return height; }
        public double getMinX() { return x; }
        public double getMinY() { return y; }
        public double getMaxX() { return x + width; }
        public double getMaxY() { return y + height; }
        public double getCenterX() { return x + width/2f; }
        public double getCenterY() { return y + height/2f; }
        public boolean isEmpty() { return width <= 0 || height <= 0; }
        public void setRect(float x, float y, float w, float h) { this.x=x; this.y=y; this.width=w; this.height=h; }
    }
}
