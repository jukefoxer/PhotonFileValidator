/*
 * MIT License
 *
 * Copyright (c) 2018 Bonosoft
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package photon.file.parts;

import photon.application.utilities.SupportPillar;
import photon.file.parts.photon.PhotonFileHeader;

import java.awt.*;
import java.util.*;

/**
 * by bn on 02/07/2018.
 */
public class PhotonLayer {
    public final static byte OFF = 0x00;
    public final static byte SUPPORTED = 0x01;
    public final static byte ISLAND = 0x02;
    public final static byte CONNECTED = 0x03;

    private int width;
    private int height;
    private int islandCount = 0;

    private byte[][] iArray;
    private int[] pixels;
    private int[] rowIslands;

    private static byte[] emptyRow;
    private static int[] emptyCol;
    
    private static byte[] scratchPad;

    public PhotonLayer(int width, int height) {
        this.width = width;
        this.height = height;

        iArray = new byte[height][width];
        pixels = new int[height];
        rowIslands = new int[height];

        if (emptyRow == null || emptyRow.length < width) {
            emptyRow = new byte[width];
        }

        if (emptyCol == null || emptyCol.length < height) {
            emptyCol = new int[height];
        }
        
        if (scratchPad == null || scratchPad.length < width * height) {
        	scratchPad = new byte[width * height];
        }
    }

    public void clear() {
        for (int y = 0; y < height; y++) {
        	System.arraycopy(emptyRow, 0, iArray[y], 0, width);
        }
        System.arraycopy(emptyCol, 0, pixels, 0, height);
        System.arraycopy(emptyCol, 0, rowIslands, 0, height);
    }

    public void supported(int x, int y) {
        iArray[y][x] = SUPPORTED;
        pixels[y]++;
    }

    public void unSupported(int x, int y) {
        iArray[y][x] = CONNECTED;
        pixels[y]++;
    }

    public void island(int x, int y) {
        iArray[y][x] = ISLAND;
        rowIslands[y]++;
        islandCount++;
        pixels[y]++;
    }

    public void remove(int x, int y, byte type) {
        iArray[y][x] = OFF;
        switch (type) {
            case ISLAND:
                rowIslands[y]--;
                islandCount--;
                break;
        }
        pixels[y]--;
    }


    public void reduce() {
        // Double reduce to handle single line connections.
        for (int i = 0; i < 2; i++) {
            if (islandCount > 0) {
                for (int y = 0; y < height; y++) {
                    if (rowIslands[y] > 0) {
                        for (int x = 0; x < width; x++) {
                            if (iArray[y][x] == ISLAND) {
                                if (connected(x, y)) {
                                    makeConnected(x, y);
                                    checkUp(x, y);
                                    if (rowIslands[y] == 0) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkUp(int x, int y) {
        if (y > 0 && rowIslands[y - 1] > 0 && iArray[y - 1][x] == ISLAND) {
            makeConnected(x, y - 1);
            checkUp(x, y - 1);
        }
        if (x > 0 && rowIslands[y] > 0 && iArray[y][x - 1] == ISLAND) {
            makeConnected(x - 1, y);
            checkBackUp(x - 1, y);
        }
        if (x < (width-1) && rowIslands[y] > 0 && iArray[y][x + 1] == ISLAND) {
            makeConnected(x + 1, y);
            checkFrontUp(x + 1, y);
        }
    }

    private void checkBackUp(int x, int y) {
        if (y > 0 && rowIslands[y - 1] > 0 && iArray[y - 1][x] == ISLAND) {
            makeConnected(x, y - 1);
            checkBackUp(x, y - 1);
        }
        if (x > 0 && rowIslands[y] > 0 && iArray[y][x - 1] == ISLAND) {
            makeConnected(x - 1, y);
            checkBackUp(x - 1, y);
        }
    }

    private void checkFrontUp(int x, int y) {
        if (y > 0 && rowIslands[y - 1] > 0 && iArray[y - 1][x] == ISLAND) {
            makeConnected(x, y - 1);
            checkFrontUp(x, y - 1);
        }
        if (x < (width-1) && rowIslands[y] > 0 && iArray[y][x + 1] == ISLAND) {
            makeConnected(x + 1, y);
            checkFrontUp(x + 1, y);
        }
    }

    private void makeConnected(int x, int y) {
        iArray[y][x] = CONNECTED;
        rowIslands[y]--;
        islandCount--;
    }

    private boolean connected(int x, int y) {
        return x > 0 && (iArray[y][x - 1] & 0x01) == SUPPORTED
                || x < (width - 1) && (iArray[y][x + 1] & 0x01) == SUPPORTED
                || y > 0 && (iArray[y - 1][x] & 0x01) == SUPPORTED
                || (y < (height - 1) && (iArray[y + 1][x] & 0x01) == SUPPORTED);
    }

    public int setIslands(ArrayList<BitSet> islandRows) {
        int islands = 0;
        for (int y = 0; y < height; y++) {
            BitSet bitSet = new BitSet();
            if (rowIslands[y] > 0) {
                for (int x = 0; x < width; x++) {
                    if (iArray[y][x] == ISLAND) {
                        bitSet.set(x);
                    }
                }
            }
            islandRows.add(bitSet);
            islands += rowIslands[y];
        }
        return islands;
    }

    public void unLink() {
        iArray = null;
        pixels = null;
        rowIslands = null;
    }

    public byte[] packLayerImage() {
    	int ptr = 0;
        for (int y = 0; y < height; y++) {
            if (pixels[y] == 0) {
                ptr = add(ptr, OFF, width);
            } else {
                byte current = OFF;
                int length = 0;
                for (int x = 0; x < width; x++) {
                    byte next = iArray[y][x];
                    if (next != current) {
                        if (length > 0) {
                            ptr = add(ptr, current, length);
                        }
                        current = next;
                        length = 1;
                    } else {
                        length++;
                    }
                }
                if (length > 0) {
                    ptr = add(ptr, current, length);
                }
            }
        }
        byte[] img = new byte[ptr];
        System.arraycopy(scratchPad, 0, img, 0, ptr);
        return img;
    }

    public void unpackLayerImage(byte[] packedLayerImage) {
        clear();
        int x = 0;
        int y = 0;
        int imageLength = packedLayerImage.length;
        for (int i = 0; i < imageLength; i++) {
            byte rle = packedLayerImage[i];
            byte colorCode = (byte) ((rle & 0x60) >> 5);

            boolean extended = (rle & 0x80) == 0x80;
            int length = rle & 0x1F;
            if (extended) {
                i++;
                length = (length << 8) | packedLayerImage[i] & 0x00ff;
            }

            Arrays.fill(iArray[y], x, x + length, colorCode);

            switch (colorCode) {
            case SUPPORTED:
                pixels[y]+=length;
                break;
            case CONNECTED:
                pixels[y]+=length;
                break;
            case ISLAND:
                rowIslands[y]+= length;
                islandCount+=length;
                pixels[y]+=length;
                break;

            }
            

            x += length;
            if (x >= width) {
                y++;
                x = 0;
            }
        }

    }
    
    
    private int add(int ptr, byte current, int length) {
        if (length < 32) {
            scratchPad[ptr++] = (byte) ((current << 5) | (length & 0x1f));
        } else {
            scratchPad[ptr++] = (byte) (0x80 | (current << 5) | (length >> 8 & 0x00FF));
            scratchPad[ptr++] = (byte) (length & 0x00FF);
        }
        return ptr;
    }

    /**
     * Get a layer image for drawing.
     * <p/>
     * This will decode the RLE packed layer information and return a list of rows, with color and length information
     *
     * @param packedLayerImage The packed layer image information
     * @param width            The width of the current layer, used to change rows
     * @return A list with the
     */
    public static ArrayList<PhotonRow> getRows(byte[] packedLayerImage, int width, boolean isCalculated) {
        Hashtable<Byte, Color> colors = new Hashtable<>();
        colors.put(OFF, Color.black);
        if (isCalculated) {
            colors.put(SUPPORTED, Color.decode("#008800"));
        } else {
            colors.put(SUPPORTED, Color.decode("#000088"));
        }
        colors.put(CONNECTED, Color.decode("#FFFF00"));
        colors.put(ISLAND, Color.decode("#FF0000"));
        ArrayList<PhotonRow> rows = new ArrayList<>();
        int resolutionX = width - 1;
        PhotonRow currentRow = new PhotonRow();
        rows.add(currentRow);
        int x = 0;
        if (packedLayerImage!=null) { // when user tries to show a layer before its calculated
            for (int i = 0; i < packedLayerImage.length; i++) {
                byte rle = packedLayerImage[i];
                byte colorCode = (byte) ((rle & 0x60) >> 5);
                Color color = colors.get(colorCode);
                boolean extended = (rle & 0x80) == 0x80;
                int length = rle & 0x1F;
                if (extended) {
                    i++;
                    length = (length << 8) | packedLayerImage[i] & 0x00ff;
                }
                currentRow.lines.add(new PhotonLine(color, length));
                x += length;

                if (x >= resolutionX) {
                    currentRow = new PhotonRow();
                    rows.add(currentRow);
                    x = 0;
                }
            }
        }
        return rows;
    }

    public int removeIslands() {
        int count = 0;
        if (islandCount > 0) {
            for (int y = 0; y < height; y++) {
                if (rowIslands[y] > 0) {
                    for (int x = 0; x < width; x++) {
                        if (iArray[y][x] == ISLAND) {
                            remove(x, y, ISLAND);
                            ++count;
                        }
                    }
                }
            }
        }
        return count;
    }

    public int fixlayer() {
        PhotonMatix photonMatix = new PhotonMatix();
        ArrayList<PhotonDot> dots = new ArrayList<>();
        if (islandCount > 0) {
            for (int y = 0; y < height; y++) {
                if (rowIslands[y] > 0) {
                    for (int x = 0; x < width; x++) {
                        if (iArray[y][x] == ISLAND) {
                            photonMatix.clear();
                            int blanks = photonMatix.set(x, y, iArray, width, height);
                            if (blanks>0) { // one or more neighbour pixels are OFF
                                photonMatix.calc();
                                photonMatix.level();
                                photonMatix.calc();

                                for(int ry=0; ry<3; ry++) {
                                    for (int rx = 0; rx < 3; rx++) {
                                        int iy = y-1+ry;
                                        int ix = x-1+rx;
                                        if (iArray[iy][ix] == OFF) {
                                            if (photonMatix.calcMatrix[1+ry][1+rx]>3) {
                                                dots.add(new PhotonDot(ix, iy));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        for(PhotonDot dot : dots) {
            island(dot.x, dot.y);
        }
        return dots.size();
    }

    public byte[] packImageData() {
    	
    	int ptr = 0;
    
        for (int y = 0; y < height; y++) {
            if (pixels[y] == 0) {
                ptr = addPhotonRLE(ptr, true, width);
            } else {
                byte current = OFF;
                int length = 0;
                for (int x = 0; x < width; x++) {
                    byte next = iArray[y][x];
                    if (next != current) {
                        if (length > 0) {
                            ptr = addPhotonRLE(ptr, current==OFF, length);
                        }
                        current = next;
                        length = 1;
                    } else {
                        length++;
                    }
                }
                if (length > 0) {
                    ptr = addPhotonRLE(ptr, current==OFF, length);
                }
            }
        }
        byte[] img = new byte[ptr];
        System.arraycopy(scratchPad, 0, img, 0, ptr);
        return img;
    }

    private int addPhotonRLE(int ptr, boolean off, int length) {
    	
        while (length > 0) {
            int lineLength = length < 125 ? length : 125; // max storage length of 0x7D (125) ?? Why not 127?
            scratchPad[ptr++] = (byte) ((off ? 0x00: 0x80) | (lineLength & 0x7f));
            length -= lineLength;
        }
        
        return ptr;
    }

    public byte get(int x, int y) {
        return iArray[y][x];
    }

    public int updateModelXYProjection(int[][] profile, LinkedList<SupportPillar> supports, int supportDist, int contactHeight, int layerNum, int contactSize, int pillarSize) {
        int addedPixels = 0;
        int minDist = 3;
        int maxContactPixels;
        int contactX = contactSize;
        int contactY = contactSize;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (iArray[y][x] > 0) {
                    for (int dy = -pillarSize/2; dy <= pillarSize/2-1; dy++) {
                        for (int dx = -pillarSize / 2; dx <= pillarSize / 2 - 1; dx++) {
                            if (y + dy > 0 && y + dy < height && x + dx > 0 && x + dx < width && profile[y + dy][x + dx] <= 0) {
                                profile[y + dy][x + dx] = layerNum;
                                addedPixels++;
                            }
                        }
                    }
                }
            }
        }
        for (int y = 0; y < height-supportDist; y+=supportDist) {
            for (int x = 0 + (supportDist / 2) * ((y / supportDist) % 2); x < width - supportDist; x += supportDist) {
                if (profile[y][x] == layerNum) {
                    boolean shouldSupport = shouldSupport(profile, contactHeight, minDist, y, x, pillarSize / 2, pillarSize / 2 - 1);
                    if (shouldSupport) {
                        maxContactPixels = 1;
                        SupportPillar support = new SupportPillar(x, y, layerNum);
                        support.setWidth(pillarSize);
                        support.setHeight(pillarSize);
                        for (int dy = -support.getHeight()/2; dy < support.getHeight()/2-contactY; dy++) {
                            for (int dx = -support.getWidth()/2; dx < support.getWidth()/2-contactX; dx++) {
                                int numContactPixels = getNumContactPixels(x+dx, y+dy, contactX, contactY);
                                if (numContactPixels > maxContactPixels) {
                                    maxContactPixels = numContactPixels;
                                    support.setContactOffsetX(dx);
                                    support.setContactOffsetY(dy);
                                }
                            }
                        }
                        supports.add(support);
                    }
                }
            }
        }
        return addedPixels;
    }

    int getNumContactPixels(int x, int y, int w, int h) {
        int numContactPixels = 0;
        for (int y1 = 0; y1 < h; y1++) {
            for (int x1 = 0; x1 < w; x1++) {
                if (y + y1 > 0 && y + y1 < height && x + x1 > 0 && x + x1 < width && iArray[y + y1][x + x1] > 0) {
                    numContactPixels++;
                }
            }
        }
        return numContactPixels;
    }

    public int addSupport(int[][] profile, LinkedList<SupportPillar> supports, int contactSize, int contactHeight, int layerNum) {
        int addedPixels = 0;
        int contactX = contactSize;
        int contactY = contactSize;
        if (layerNum < 5) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (profile[y][x] > 0) {
                        for (int dy = -5; dy <= 5; dy++) {
                            for (int dx = -5 ; dx <= 5; dx++) {
                                if (y + dy > 0 && y + dy < height && x + dx > 0 && x + dx < width) {
                                    supported(x+dx, y+dy);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            for (SupportPillar supportPillar : supports) {
                int x = supportPillar.getX();
                int y = supportPillar.getY();
                if (layerNum <= profile[y][x] - contactHeight) {
                    for (int dy = -supportPillar.getHeight()/2; dy <= supportPillar.getHeight()/2-1; dy++) {
                        for (int dx = -supportPillar.getHeight()/2; dx <= supportPillar.getWidth()/2-1; dx++) {
                            if (y + dy > 0 && y + dy < height && x + dx > 0 && x + dx < width) {// && iArray[y+dy][x+dx] != CONNECTED) {
                                supported(x + dx, y + dy);
                                addedPixels++;
                            }
                        }
                    }
                } else if (layerNum <= profile[y][x]+2) {
                    addedPixels += createContact(x + supportPillar.getContactOffsetX(), y+supportPillar.getContactOffsetY(), contactX, contactY, layerNum == profile[y][x]);
                }
            }
        }
        return addedPixels;
    }

    private boolean shouldSupport(int[][] profile, int contactHeight, int minDist, int y, int x, int lowerPillarSize, int upperPillarSize) {
        boolean shouldSupport = true;
        for (int dy = -lowerPillarSize - minDist; dy <= upperPillarSize+minDist; dy++) {
            for (int dx = -lowerPillarSize - minDist; dx <= upperPillarSize+minDist; dx++) {
                if (profile[y + dy][x + dx] > 0 && profile[y + dy][x + dx] < profile[y][x]-contactHeight/2) {
                    shouldSupport = false;
                }
            }
        }
        return shouldSupport;
    }

    private int createContact(int x, int y, int w, int h, boolean isContactLayer) {
        int addedPixels = 0;
        for (int dy = 0; dy <= w-1; dy++) {
            for (int dx = -0; dx <= h-1; dx++) {
                if (y + dy > 0 && y + dy < height && x + dx > 0 && x + dx < width) {
                    supported(x + dx, y + dy);
                    addedPixels++;
                }
            }
        }
        return addedPixels;
    }

}
