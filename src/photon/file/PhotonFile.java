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

package photon.file;

import photon.application.utilities.SupportPillar;
import photon.file.parts.*;
import photon.file.parts.photon.PhotonFileHeader;
import photon.file.parts.photons.PhotonsFileHeader;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * by bn on 30/06/2018.
 */
public class PhotonFile {
    private IFileHeader iFileHeader;
    private PhotonFilePreview previewOne;
    private PhotonFilePreview previewTwo;
    private List<PhotonFileLayer> layers;

    private StringBuilder islandList;
    private int islandLayerCount;
    private ArrayList<Integer> islandLayers;

    private int margin;
    private ArrayList<Integer> marginLayers;


    public PhotonFile readFile(File file, IPhotonProgress iPhotonProgress) throws Exception {
        if (file.getName().toLowerCase().endsWith(".photons")) {
            return readPhotonsFile(getBinaryData(file), iPhotonProgress);
        }
        return readFile(getBinaryData(file), iPhotonProgress);
    }

    private PhotonFile readPhotonsFile(byte[] file, IPhotonProgress iPhotonProgress) throws Exception {
        iPhotonProgress.showInfo("Reading Photon S file header information...");

        PhotonsFileHeader photonsFileHeader = new PhotonsFileHeader(file);
        iFileHeader = photonsFileHeader;



        return this;
    }

    private PhotonFile readFile(byte[] file, IPhotonProgress iPhotonProgress) throws Exception {
        iPhotonProgress.showInfo("Reading Photon file header information...");
        PhotonFileHeader photonFileHeader = new PhotonFileHeader(file);
        iFileHeader = photonFileHeader;

        iPhotonProgress.showInfo("Reading photon large preview image information...");
        previewOne = new PhotonFilePreview(photonFileHeader.getPreviewOneOffsetAddress(), file);
        iPhotonProgress.showInfo("Reading photon small preview image information...");
        previewTwo = new PhotonFilePreview(photonFileHeader.getPreviewTwoOffsetAddress(), file);
        if (photonFileHeader.getVersion() > 1) {
            iPhotonProgress.showInfo("Reading Print parameters information...");
            photonFileHeader.readParameters(file);
        }
        iPhotonProgress.showInfo("Reading photon layers information...");
        layers = PhotonFileLayer.readLayers(photonFileHeader, file, margin, iPhotonProgress);
        resetMarginAndIslandInfo();

        return this;
    }

    public void saveFile(File file) throws Exception {
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        writeFile(fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    private void writeFile(OutputStream outputStream) throws Exception {
        int antiAliasLevel = iFileHeader.getAALevels();

        int headerPos = 0;
        int previewOnePos = headerPos + iFileHeader.getByteSize();
        int previewTwoPos = previewOnePos + previewOne.getByteSize();
        int layerDefinitionPos = previewTwoPos + previewTwo.getByteSize();

        int parametersPos = 0;
        int machineInfoPos = 0;
        if (iFileHeader.getVersion() > 1) {
            parametersPos = layerDefinitionPos;
            if (((PhotonFileHeader)iFileHeader).photonFileMachineInfo.getByteSize() > 0) {
	            machineInfoPos = parametersPos + ((PhotonFileHeader)iFileHeader).photonFilePrintParameters.getByteSize();
                layerDefinitionPos = machineInfoPos + ((PhotonFileHeader)iFileHeader).photonFileMachineInfo.getByteSize();
            } else {
                layerDefinitionPos = parametersPos + ((PhotonFileHeader)iFileHeader).photonFilePrintParameters.getByteSize();
            }
        }

        int dataPosition = layerDefinitionPos + (PhotonFileLayer.getByteSize() * iFileHeader.getNumberOfLayers() * antiAliasLevel);


        PhotonOutputStream os = new PhotonOutputStream(outputStream);

        ((PhotonFileHeader)iFileHeader).save(os, previewOnePos, previewTwoPos, layerDefinitionPos, parametersPos, machineInfoPos);
        previewOne.save(os, previewOnePos);
        previewTwo.save(os, previewTwoPos);

        if (iFileHeader.getVersion() > 1) {
            ((PhotonFileHeader)iFileHeader).photonFilePrintParameters.save(os);
            ((PhotonFileHeader)iFileHeader).photonFileMachineInfo.save(os, machineInfoPos);
        }

        // Optimize order for speed read on photon
        for (int i = 0; i < iFileHeader.getNumberOfLayers(); i++) {
            PhotonFileLayer layer = layers.get(i);
            dataPosition = layer.savePos(dataPosition);
            if (antiAliasLevel > 1) {
                for (int a = 0; a < (antiAliasLevel - 1); a++) {
                    dataPosition = layer.getAntiAlias(a).savePos(dataPosition);
                }
            }
        }

        // Order for backward compatibility with photon/cbddlp version 1
        for (int i = 0; i < iFileHeader.getNumberOfLayers(); i++) {
            layers.get(i).save(os);
        }

        if (antiAliasLevel > 1) {
            for (int a = 0; a < (antiAliasLevel - 1); a++) {
                for (int i = 0; i < iFileHeader.getNumberOfLayers(); i++) {
                    layers.get(i).getAntiAlias(a).save(os);
                }
            }
        }

        // Optimize order for speed read on photon
        for (int i = 0; i < iFileHeader.getNumberOfLayers(); i++) {
            PhotonFileLayer layer = layers.get(i);
            layer.saveData(os);
            if (antiAliasLevel > 1) {
                for (int a = 0; a < (antiAliasLevel - 1); a++) {
                    layer.getAntiAlias(a).saveData(os);
                }
            }
        }
    }


    private byte[] getBinaryData(File entry) throws Exception {
        if (entry.isFile()) {
            int fileSize = (int) entry.length();
            byte[] fileData = new byte[fileSize];

            InputStream stream = new FileInputStream(entry);
            int bytesRead = 0;
            while (bytesRead < fileSize) {
                int readCount = stream.read(fileData, bytesRead, fileSize - bytesRead);
                if (readCount < 0) {
                    throw new IOException("Could not read all bytes of the file");
                }
                bytesRead += readCount;
            }

            return fileData;
        }
        return null;
    }

    public String getInformation() {
        if (iFileHeader == null) return "";
        return iFileHeader.getInformation();
    }


    public int getIslandLayerCount() {
        if (islandList == null) {
            findIslands();
        }
        return islandLayerCount;
    }

    public ArrayList<Integer> getIslandLayers() {
        if (islandList == null) {
            findIslands();
        }
        return islandLayers;
    }

    public void setMargin(int margin) {
        this.margin = margin;
    }

    public ArrayList<Integer> getMarginLayers() {
        if (marginLayers == null) {
            return new ArrayList<>();
        }
        return marginLayers;
    }

    public String getMarginInformation() {
        if (marginLayers == null) {
            return "No safety margin set, printing to the border.";
        } else {
            if (marginLayers.size() == 0) {
                return "The model is within the defined safety margin (" + this.margin + " pixels).";
            } else if (marginLayers.size() == 1) {
                return "The layer " + marginLayers.get(0) + " contains model parts that extend beyond the margin.";
            }
            StringBuilder marginList = new StringBuilder();
            int count = 0;
            for (int layer : marginLayers) {
                if (count > 10) {
                    marginList.append(", ...");
                    break;
                } else {
                    if (marginList.length() > 0) marginList.append(", ");
                    marginList.append(layer);
                }
                count++;
            }
            return "The layers " + marginList.toString() + " contains model parts that extend beyond the margin.";
        }
    }

    public String getLayerInformation() {
        if (islandList == null) {
            findIslands();
        }
        if (islandLayerCount == 0) {
            return "Whoopee, all is good, no unsupported areas";
        } else if (islandLayerCount == 1) {
            return "Unsupported islands found in layer " + islandList.toString();
        }
        return "Unsupported islands found in layers " + islandList.toString();
    }

    private void findIslands() {
        if (islandLayers != null) {
            islandLayers.clear();
            islandList = new StringBuilder();
            islandLayerCount = 0;
            if (layers != null) {
                for (int i = 0; i < iFileHeader.getNumberOfLayers(); i++) {
                    PhotonFileLayer layer = layers.get(i);
                    if (layer.getIsLandsCount() > 0) {
                        if (islandLayerCount < 11) {
                            if (islandLayerCount == 10) {
                                islandList.append(", ...");
                            } else {
                                if (islandList.length() > 0) islandList.append(", ");
                                islandList.append(i);
                            }
                        }
                        islandLayerCount++;
                        islandLayers.add(i);
                    }
                }
            }
        }
    }

    public int getWidth() {
        return iFileHeader.getResolutionY();
    }

    public int getHeight() {
        return iFileHeader.getResolutionX();
    }

    public int getLayerCount() {
        return iFileHeader.getNumberOfLayers();
    }

    public PhotonFileLayer getLayer(int i) {
        if (layers != null && layers.size() > i) {
            return layers.get(i);
        }
        return null;
    }

    public long getPixels() {
        long total = 0;
        if (layers != null) {
            for (PhotonFileLayer layer : layers) {
                total += layer.getPixels();
            }
        }
        return total;
    }

    public IFileHeader getPhotonFileHeader() {
        return iFileHeader;
    }

    public PhotonFilePreview getPreviewOne() {
        return previewOne;
    }

    public PhotonFilePreview getPreviewTwo() {
        return previewTwo;
    }


    public void unLink() {
        while (!layers.isEmpty()) {
            PhotonFileLayer layer = layers.remove(0);
            layer.unLink();
        }
        if (islandLayers != null) {
            islandLayers.clear();
        }
        if (marginLayers != null) {
            marginLayers.clear();
        }
        iFileHeader.unLink();
        iFileHeader = null;
        previewOne.unLink();
        previewOne = null;
        previewTwo.unLink();
        previewTwo = null;
        System.gc();
    }


    public void adjustLayerSettings() {
        for (int i = 0; i < layers.size(); i++) {
            PhotonFileLayer layer = layers.get(i);
            if (i < iFileHeader.getBottomLayers()) {
                layer.setLayerExposure(iFileHeader.getBottomExposureTimeSeconds());
            } else {
                layer.setLayerExposure(iFileHeader.getNormalExposure());
            }
            layer.setLayerOffTimeSeconds(iFileHeader.getOffTimeSeconds());
        }
    }

    public void fixLayers(IPhotonProgress progres) throws Exception {
        PhotonLayer layer = null;
        for (int layerNo : islandLayers) {
            progres.showInfo("Checking layer " + layerNo);

            // Unpack the layer data to the layer utility class
            PhotonFileLayer fileLayer = layers.get(layerNo);
            if (layer == null) {
                layer = fileLayer.getLayer();
            } else {
                fileLayer.getUpdateLayer(layer);
            }

            int changed = fixit(progres, layer, fileLayer, 10);
            if (changed == 0) {
                progres.showInfo(", but nothing could be done.");
            } else {
                fileLayer.saveLayer(layer);
                calculate(layerNo);
            }

            progres.showInfo("<br>");

        }
        findIslands();
    }

    public int erodeLayers(IPhotonProgress progres) throws Exception {
        PhotonLayer layer = null;
        int removedPixels = 0;
        //for (int layerNo = 0; layerNo < layers.size(); layerNo++) {
        for (int layerNo = 0; layerNo < iFileHeader.getBottomLayers(); layerNo++) {
            progres.showInfo("Eroded layers 0 to " +layerNo + ". Removed " + removedPixels + " pixels.");

            // Unpack the layer data to the layer utility class
            PhotonFileLayer fileLayer = layers.get(layerNo);
            if (layer == null) {
                layer = fileLayer.getLayer();
            } else {
                fileLayer.getUpdateLayer(layer);
            }

            removedPixels += layer.erodePixels();
            fileLayer.saveLayer(layer);
            calculate(layerNo);
            System.gc();
        }
        return removedPixels;
    }

    public void addFineSupport(IPhotonProgress progres, int supportDist, int contactSize, int contactHeight, int pillarSize, float liftModelMm) throws Exception {
        LinkedList<SupportPillar> supportPillars = new LinkedList<>();
        int numLiftLayers = Math.round(liftModelMm/iFileHeader.getLayerHeight());

        // First lift the model if requested
        liftModel(numLiftLayers);

        // Store the projection of the model on the XY plane in the profile variable
        int[][] profile = getModelXYProjectionAndSupportPillars(progres, supportDist, contactHeight, contactSize, pillarSize, supportPillars);

        // Create the support structures
        PhotonLayer layer = null;
        for (int layerNo = 0; layerNo < layers.size(); layerNo++) {
            progres.showInfo("Supported layers 0 to " +layerNo + ".");

            // Unpack the layer data to the layer utility class
            PhotonFileLayer fileLayer = layers.get(layerNo);
            if (layer == null) {
                layer = fileLayer.getLayer();
            } else {
                fileLayer.getUpdateLayer(layer);
            }

            layer.addSupport(profile, supportPillars, contactSize, contactHeight, layerNo, iFileHeader.getBottomLayers());

            fileLayer.saveLayer(layer);
            calculate(layerNo);
        }
        System.gc();
    }

    private void liftModel(int numLiftLayers) throws Exception {
        if (iFileHeader instanceof PhotonFileHeader) {
            PhotonFileHeader header = (PhotonFileHeader) iFileHeader;

            PhotonLayer layer = null;
            for (int layerNo = 0; layerNo < numLiftLayers; layerNo++) {
                // just copy layer 0 and correct the exposure and z height information afterwards
                PhotonFileLayer fileLayer = layers.get(layerNo);
                if (layer == null) {
                    layer = fileLayer.getLayer();
                } else {
                    fileLayer.getUpdateLayer(layer);
                }
                PhotonFileLayer photonFileLayer = new PhotonFileLayer(fileLayer, header);
                layer.clear();
                photonFileLayer.saveLayer(layer);

                layers.add(0, photonFileLayer);
            }
            header.setNumberOfLayers(header.getNumberOfLayers() + numLiftLayers);
            correctLayerData(layer);
        }
    }

    private void correctLayerData(PhotonLayer layer) throws Exception {
        for (int layerNo = 0; layerNo < layers.size(); layerNo++) {

            // Unpack the layer data to the layer utility class
            PhotonFileLayer fileLayer = layers.get(layerNo);
            if (layer == null) {
                layer = fileLayer.getLayer();
            } else {
                fileLayer.getUpdateLayer(layer);
            }

            if (layerNo < iFileHeader.getBottomLayers()) {
                fileLayer.setLayerExposure(iFileHeader.getBottomExposureTimeSeconds());
            } else {
                fileLayer.setLayerExposure(iFileHeader.getExposureTimeSeconds());
            }
            fileLayer.setLayerPositionZ(layerNo*iFileHeader.getLayerHeight());
        }
    }

    private int[][] getModelXYProjectionAndSupportPillars(IPhotonProgress progres, int supportDist, int contactHeight, int contactSize, int pillarSize, LinkedList<SupportPillar> supports) throws Exception {
        PhotonLayer layer = null;

        // The profile variable contains the layer number of the lowest solid model part at this x/y position
        int[][] profile = new int[getWidth()][getHeight()];
        for (int y = 0; y < getWidth(); y++) {
            for (int x = 0; x < getHeight(); x++) {
                profile[y][x] = -1;
            }
        }
        for (int layerNo = 0; layerNo < layers.size(); layerNo++) {
            progres.showInfo("Getting shape profile for layer " +layerNo + ".");

            // Unpack the layer data to the layer utility class
            PhotonFileLayer fileLayer = layers.get(layerNo);
            if (layer == null) {
                layer = fileLayer.getLayer();
            } else {
                fileLayer.getUpdateLayer(layer);
            }

            layer.updateModelXYProjection(profile, supports, supportDist, contactHeight, layerNo, contactSize, pillarSize);

            layer.addNecessarySupportPillars(profile, supports, supportDist, contactHeight, layerNo, contactSize, pillarSize);
        }
        return profile;
    }

    private int fixit(IPhotonProgress progres, PhotonLayer layer, PhotonFileLayer fileLayer, int loops) throws Exception {
        int changed = layer.fixlayer();
        if (changed > 0) {
            layer.reduce();
            fileLayer.updateLayerIslands(layer);
            progres.showInfo(", " + changed + " pixels changed");
            if (loops > 0) {
                changed += fixit(progres, layer, fileLayer, loops - 1);
            }
        }
        return changed;
    }

    public void calculateAaLayers(IPhotonProgress progres, PhotonAaMatrix photonAaMatrix) throws Exception {
        PhotonFileLayer.calculateAALayers((PhotonFileHeader) iFileHeader, layers, photonAaMatrix, progres);
    }

    public void calculate(IPhotonProgress progres) throws Exception {
        PhotonFileLayer.calculateLayers((PhotonFileHeader)iFileHeader, layers, margin, progres);
        resetMarginAndIslandInfo();
    }

    public void calculate(int layerNo) throws Exception {
        PhotonFileLayer.calculateLayers((PhotonFileHeader)iFileHeader, layers, margin, layerNo);
        resetMarginAndIslandInfo();
    }

    private void resetMarginAndIslandInfo() {
        islandList = null;
        islandLayerCount = 0;
        islandLayers = new ArrayList<>();

        if (margin > 0) {
            marginLayers = new ArrayList<>();
            int i = 0;
            for (PhotonFileLayer layer : layers) {
                if (layer.doExtendMargin()) {
                    marginLayers.add(i);
                }
                i++;
            }
        }
    }

    public float getZdrift() {
        float expectedHeight = iFileHeader.getLayerHeight() * (iFileHeader.getNumberOfLayers() - 1);
        float actualHeight = layers.get(layers.size() - 1).getLayerPositionZ();
        return expectedHeight - actualHeight;

    }

    public void fixLayerHeights() {
        int index = 0;
        for (PhotonFileLayer layer : layers) {
            layer.setLayerPositionZ(index * iFileHeader.getLayerHeight());
            index++;
        }
    }

    public int getVersion() {
        return iFileHeader.getVersion();
    }

    public boolean hasAA() {
        return iFileHeader.hasAA();
    }

    public int getAALevels() {
        return iFileHeader.getAALevels();
    }

    public void changeToVersion2() {
        iFileHeader.setFileVersion(2);
    }

    // only call this when recalculating AA levels
    public void setAALevels(int levels) {
        iFileHeader.setAALevels(levels, layers);
    }

}

