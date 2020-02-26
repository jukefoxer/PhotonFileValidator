package photon.application.utilities;

public class SupportPillar {

    private int x;
    private int y;
    private int width;
    private int height;
    private int contactLayerNum;
    private int contactOffsetX = 0;
    private int contactOffsetY = 0;

    public SupportPillar(int x, int y, int contactLayerNum) {
        this.x = x;
        this.y = y;
        this.contactLayerNum = contactLayerNum;
    }

    public SupportPillar(int x, int y, int contactLayerNum, int contactOffsetX, int contactOffsetY) {
        this.x = x;
        this.y = y;
        this.contactLayerNum = contactLayerNum;
        this.contactOffsetX = contactOffsetX;
        this.contactOffsetY = contactOffsetY;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getContactLayerNum() {
        return contactLayerNum;
    }

    public void setContactLayerNum(int contactLayerNum) {
        this.contactLayerNum = contactLayerNum;
    }

    public int getContactOffsetX() {
        return contactOffsetX;
    }

    public void setContactOffsetX(int contactOffsetX) {
        this.contactOffsetX = contactOffsetX;
    }

    public int getContactOffsetY() {
        return contactOffsetY;
    }

    public void setContactOffsetY(int contactOffsetY) {
        this.contactOffsetY = contactOffsetY;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
