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

package photon.application.utilities;

import photon.application.MainForm;
import photon.application.dialogs.SupportDialog;
import photon.file.PhotonFile;
import photon.file.parts.IPhotonProgress;

import javax.swing.*;

/**
 * by bn on 14/07/2018.
 */
public class PhotonSupportWorker extends SwingWorker<Integer, String> implements IPhotonProgress {
    private final int contactSizePixels;
    private final int pillarSizePixels;
    private final int supportDistPixels;
    private final int contactHeightLayers;
    private final float liftModelMm;
    private final boolean doErode;
    private photon.application.dialogs.SupportDialog SupportDialog;
    private PhotonFile photonFile;
    private MainForm mainForm;

    public PhotonSupportWorker(photon.application.dialogs.SupportDialog SupportDialog, PhotonFile photonFile, MainForm mainForm, int contactSizePixels, int pillarSizePixels, int supportDistPixels, int contactHeightLayers, float liftModelMm, boolean doErode) {
        this.SupportDialog = SupportDialog;
        this.photonFile = photonFile;
        this.mainForm = mainForm;
        this.contactSizePixels = contactSizePixels;
        this.pillarSizePixels = pillarSizePixels;
        this.supportDistPixels = supportDistPixels;
        this.contactHeightLayers = contactHeightLayers;
        this.liftModelMm = liftModelMm;
        this.doErode = doErode;
    }

    @Override
    protected void process(java.util.List<String> chunks) {
        for (String str : chunks) {
            SupportDialog.setProgressInformation(str);
        }
    }

    @Override
    protected void done() {
        SupportDialog.buttonOK.setEnabled(true);
        SupportDialog.startButton.setEnabled(true);
        SupportDialog.appendInformation("<p>Done.</p>");
        mainForm.showFileInformation();
    }

    @Override
    public void showInfo(String str) {
        publish(str);
    }

    @Override
    protected Integer doInBackground() throws Exception {
        try {
            if (this.doErode) {
                photonFile.erodeLayers(this);
            }
            photonFile.addFineSupport(this, this.supportDistPixels, this.contactSizePixels, this.contactHeightLayers, this.pillarSizePixels, this.liftModelMm);
        } catch (Exception e) {
            e.printStackTrace();
            publish("<br><p>" + e.toString()+ "</p>");
            return 0;
        }
        return 1;
    }
}
