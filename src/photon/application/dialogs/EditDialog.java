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

package photon.application.dialogs;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import photon.application.MainForm;
import photon.file.PhotonFile;
import photon.file.parts.PhotonDot;
import photon.file.parts.PhotonFileLayer;
import photon.file.parts.PhotonLayer;
import photon.file.ui.PhotonEditPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class EditDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JLabel infoText;
    private JPanel editArea;
    private JComboBox modeCombo;
    private JLabel editModeHelp;

    private EditDialog me;
    private MainForm mainForm;
    private PhotonFile photonFile;
    private PhotonFileLayer fileLayer;
    private PhotonLayer layer;
    private int layerNo;
    private int layerX;
    private int layerY;
    private HashSet<PhotonDot> dots;

    private boolean mirrored;
    private PhotonDot cursorDot;

    private MouseAdapter currentEditModeHandler;

    private class ModePencilHandler extends MouseAdapter {
        PhotonDot lastCell;

        @Override
        public void mousePressed(MouseEvent e) {
            handleCellChange(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            handleCellChange(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            lastCell = null;
        }

        private void handleCellChange(MouseEvent e) {
            PhotonDot currentCell = getPosition(e);
            if (currentCell != null) {
                if (!currentCell.equals(lastCell)) {
                    if (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3) {
                        boolean on = e.getButton() == MouseEvent.BUTTON1;
                        byte original = layer.get(layerY + currentCell.y, layerX + currentCell.x);
                        boolean isOriginalOn = original != PhotonLayer.OFF;
                        Color originalColor = getOriginalColor(currentCell);

                        PhotonDot dot = new PhotonDot(layerY + currentCell.y, layerX + currentCell.x);

                        if (dots.contains(dot)) {
                            dots.remove(dot);
                        }

                        Color color = originalColor;
                        if (on) {
                            if (!isOriginalOn) {
                                color = Color.cyan;
                                dots.add(dot);
                            }
                        } else {
                            if (isOriginalOn) {
                                color = Color.darkGray;
                                dots.add(dot);
                            }
                        }

                        ((PhotonEditPanel) editArea).drawDot(currentCell.x, currentCell.y, layer, color);
                        editArea.repaint();

                    }
                    lastCell = currentCell;
                }
            }
        }

    }

    ;

    private class ModeSwapHandler extends MouseAdapter {
        private PhotonDot pressedDot;

        @Override
        public void mousePressed(MouseEvent e) {
            pressedDot = getPosition(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            PhotonDot releasedDot = getPosition(e);
            if (pressedDot != null && releasedDot != null) {

                int x1 = Integer.min(pressedDot.x, releasedDot.x);
                int x2 = Integer.max(pressedDot.x, releasedDot.x);
                int y1 = Integer.min(pressedDot.y, releasedDot.y);
                int y2 = Integer.max(pressedDot.y, releasedDot.y);

                for (int x = x1; x <= x2; x++) {
                    for (int y = y1; y <= y2; y++) {
                        Color color = isSet(x, y);
                        ((PhotonEditPanel) editArea).drawDot(x, y, layer, color);
                        editArea.repaint();
                    }
                }
            }
        }
    }

    ;

    private enum EditMode {
        pencil("Tip: Use left mouse to set and right mouse to unset pixels."),
        swap("Tip: Use left mouse to toggle pixels. Drag for toggling within rectangle area.");
        String help;

        EditMode(String help) {
            this.help = help;
        }
    }

    ;
    private EditMode editMode = EditMode.pencil;

    private Map<EditMode, MouseAdapter> editModeHandlerRegistry;

    public EditDialog(MainForm mainForm) {
        super(mainForm.frame);
        this.me = this;
        this.mainForm = mainForm;

        editModeHandlerRegistry = new HashMap<>();
        editModeHandlerRegistry.put(EditMode.pencil, new ModePencilHandler());
        editModeHandlerRegistry.put(EditMode.swap, new ModeSwapHandler());

        $$$setupUI$$$();

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        modeCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox) e.getSource();
                String modeName = ((String) cb.getSelectedItem()).toLowerCase();
                editMode = EditMode.valueOf(modeName);
                setEditMode(editMode);
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        setEditMode(EditMode.pencil);

        editArea.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleCursor(e);
            }
        });
    }

    private void setEditMode(EditMode newEditMode) {
        editArea.removeMouseListener(currentEditModeHandler);
        editArea.removeMouseMotionListener(currentEditModeHandler);
        editMode = newEditMode;
        editModeHelp.setText(editMode.help);
        currentEditModeHandler = editModeHandlerRegistry.get(editMode);
        editArea.addMouseListener(currentEditModeHandler);
        editArea.addMouseMotionListener(currentEditModeHandler);
    }

    private void handleCursor(MouseEvent e) {
        PhotonDot lastCursorDot = cursorDot;

        cursorDot = getPosition(e);
        if (lastCursorDot != null && !lastCursorDot.equals(cursorDot)) {
            Color color = getColor(lastCursorDot);
            ((PhotonEditPanel) editArea).drawDot(lastCursorDot.x, lastCursorDot.y, layer, color);
            editArea.repaint();
        }
        if (cursorDot != null && !cursorDot.equals(lastCursorDot)) {
            Color original = getColor(cursorDot);
            Color color = original.brighter();
            if (original.equals(Color.black)) {
                color = Color.lightGray;
            }
            ((PhotonEditPanel) editArea).drawDot(cursorDot.x, cursorDot.y, layer, color);
            editArea.repaint();
        }
    }

    private Color isSet(int x, int y) {
        byte original = layer.get(layerY + y, layerX + x);
        boolean result = original != PhotonLayer.OFF;

        PhotonDot dot = new PhotonDot(layerY + y, layerX + x);
        if (dots.contains(dot)) {
            dots.remove(dot);

            switch (layer.get(layerY + y, layerX + x)) {
                case PhotonLayer.SUPPORTED:
                    return Color.decode("#008800");

                case PhotonLayer.CONNECTED:
                    return Color.decode("#FFFF00");

                case PhotonLayer.ISLAND:
                    return Color.decode("#FF0000");

                default:
                    return Color.black;

            }

        } else {
            dots.add(dot);
            return result ? Color.darkGray : Color.cyan;
        }
    }

    private Color getOriginalColor(PhotonDot dst) {
        byte original = layer.get(layerY + dst.y, layerX + dst.x);
        switch (original) {
            case PhotonLayer.SUPPORTED:
                return Color.decode("#008800");

            case PhotonLayer.CONNECTED:
                return Color.decode("#FFFF00");

            case PhotonLayer.ISLAND:
                return Color.decode("#FF0000");

            default:
                return Color.black;
        }
    }

    private Color getColor(PhotonDot dst) {
        byte original = layer.get(layerY + dst.y, layerX + dst.x);
        boolean result = original != PhotonLayer.OFF;

        PhotonDot dot = new PhotonDot(layerY + dst.y, layerX + dst.x);
        if (!dots.contains(dot)) {

            switch (original) {
                case PhotonLayer.SUPPORTED:
                    return Color.decode("#008800");

                case PhotonLayer.CONNECTED:
                    return Color.decode("#FFFF00");

                case PhotonLayer.ISLAND:
                    return Color.decode("#FF0000");

                default:
                    return Color.black;

            }

        } else {
            return result ? Color.darkGray : Color.cyan;
        }
    }

    private void onOK() {
        if (dots.size() > 0) {
            for (PhotonDot dot : dots) {
                byte type = layer.get(dot.x, dot.y);
                if (type == PhotonLayer.OFF) {
                    layer.island(dot.x, dot.y);
                } else {
                    layer.remove(dot.x, dot.y, type);
                }
            }
            try {
                fileLayer.saveLayer(layer);
                photonFile.calculate(layerNo);

                // check next layer, just in case we created new islands
                if (layerNo < photonFile.getLayerCount() - 1) {
                    photonFile.calculate(layerNo + 1);
                }

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            mainForm.changeLayer();
            mainForm.showMarginAndIslandInformation();
        }
        dispose();
    }

    private void onCancel() {
        dispose();
    }


    public void setInformation(PhotonFile photonFile, int layerNo, int mouseX, int mouseY) {
        this.dots = new HashSet<>();
        this.layerNo = layerNo;
        this.photonFile = photonFile;
        this.fileLayer = photonFile.getLayer(layerNo);
        this.layer = fileLayer.getLayer();
        mirrored = photonFile.getPhotonFileHeader().isMirrored();

        int indexX = (mouseX < 38) ? 1 : mouseX - 38;
        int indexY = (mouseY < 23) ? 1 : mouseY - 23;

        if (indexX + 74 >= photonFile.getWidth()) {
            indexX = photonFile.getWidth() - 74;
        }
        if (indexY + 44 >= photonFile.getHeight()) {
            indexY = photonFile.getHeight() - 44;
        }

        if (mirrored) {
            indexY = photonFile.getHeight() - indexY - 44;
        }

        ((PhotonEditPanel) editArea).setMirrored(mirrored);

        layerX = indexX - 1;
        layerY = indexY - 1;

        infoText.setText("Showing column " + indexX + " to " + (indexX + 74) + ", in row " + indexY + " to " + (indexY + 44) + ")");
        ((PhotonEditPanel) editArea).drawLayer(layerX, layerY, layer);
        editArea.repaint();
    }


    private void createUIComponents() {
        editArea = new PhotonEditPanel(780, 480);
    }

    private PhotonDot getPosition(MouseEvent e) {
        int my = e.getY();
        if (mirrored) {
            my = editArea.getHeight() - e.getY() - 1;
        }
        if (e.getX() > 15 && my > 15) {
            int x = (e.getX() - 15) / 10;
            int y = (my - 15) / 10;
            if (x < 75 && y < 45) {
                return new PhotonDot(x, y);
            }
        }
        return null;
    }


    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("OK");
        panel2.add(buttonOK, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel2.add(buttonCancel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        editModeHelp = new JLabel();
        Font editModeHelpFont = this.$$$getFont$$$(null, -1, 10, editModeHelp.getFont());
        if (editModeHelpFont != null) editModeHelp.setFont(editModeHelpFont);
        editModeHelp.setText("Use left button to set and right button to unset pixels");
        panel1.add(editModeHelp, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 30), new Dimension(-1, 30), new Dimension(-1, 30), 0, false));
        infoText = new JLabel();
        Font infoTextFont = this.$$$getFont$$$(null, -1, 12, infoText.getFont());
        if (infoTextFont != null) infoText.setFont(infoTextFont);
        infoText.setText("");
        panel3.add(infoText, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        modeCombo = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Pencil");
        defaultComboBoxModel1.addElement("Swap");
        modeCombo.setModel(defaultComboBoxModel1);
        panel3.add(modeCombo, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Mode");
        panel3.add(label1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        editArea.setBackground(new Color(-1250068));
        editArea.setEnabled(true);
        panel4.add(editArea, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(300, 100), new Dimension(300, 100), null, 0, false));
        infoText.setLabelFor(modeCombo);
        label1.setLabelFor(modeCombo);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
