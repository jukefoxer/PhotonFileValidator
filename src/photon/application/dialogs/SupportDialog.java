package photon.application.dialogs;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import photon.application.MainForm;
import photon.application.utilities.PhotonSupportWorker;
import photon.file.PhotonFile;
import photon.file.parts.photon.PhotonFileHeader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SupportDialog extends JDialog {

    public static final int CONTACT_SIZE_PIXELS_DEFAULT = 4;
    public static final int PILLAR_SIZE_PIXELS_DEFAULT = 8;
    public static final int SUPPORT_DIST_PIXELS_DEFAULT = 12;
    public static final int CONTACT_HEIGHT_LAYERS_DEFAULT = 30;
    public static final float LIFT_MODEL_MM_DEFAULT = 2.0f;

    private JPanel contentPane;
    public JButton buttonOK;
    private JScrollPane textPane;
    public JButton startButton;
    private JLabel progressInfo;
    private JTextField contactSizeTextFeld;
    private JTextField supportDistanceTextField;
    private JTextField contactHeightTextField;
    private JTextField pillarSizeTextField;
    private JTextField liftModelMmTextField;

    private SupportDialog me;
    private MainForm mainForm;
    private PhotonFile photonFile;
    private StringBuilder information;

    public SupportDialog(final MainForm mainForm) {
        super(mainForm.frame);
        this.mainForm = mainForm;
        this.me = this;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        setDefaultParameters();

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startButton.setEnabled(false);
                buttonOK.setEnabled(false);
                int contactSizePixels = CONTACT_SIZE_PIXELS_DEFAULT;
                int pillarSizePixels = PILLAR_SIZE_PIXELS_DEFAULT;
                int supportDistPixels = SUPPORT_DIST_PIXELS_DEFAULT;
                int contactHeightLayers = CONTACT_HEIGHT_LAYERS_DEFAULT;
                float liftModelMm = LIFT_MODEL_MM_DEFAULT;
                try {
                    contactSizePixels = Integer.parseInt(contactSizeTextFeld.getText());
                    pillarSizePixels = Integer.parseInt(pillarSizeTextField.getText());
                    contactHeightLayers = Integer.parseInt(contactHeightTextField.getText());
                    supportDistPixels = Integer.parseInt(supportDistanceTextField.getText());
                    liftModelMm = Float.parseFloat(liftModelMmTextField.getText());
                    PhotonSupportWorker photonSupportWorker = new PhotonSupportWorker(me, mainForm.photonFile, mainForm, contactSizePixels, pillarSizePixels, supportDistPixels, contactHeightLayers, liftModelMm);
                    photonSupportWorker.execute();
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(null, "Invalid parameters.", "Error", JOptionPane.ERROR_MESSAGE);
                    startButton.setEnabled(true);
                    buttonOK.setEnabled(true);
                }

            }
        });

    }

    private void setDefaultParameters() {
        contactSizeTextFeld.setText("" + CONTACT_SIZE_PIXELS_DEFAULT);
        pillarSizeTextField.setText("" + PILLAR_SIZE_PIXELS_DEFAULT);
        contactHeightTextField.setText("" + CONTACT_HEIGHT_LAYERS_DEFAULT);
        supportDistanceTextField.setText("" + SUPPORT_DIST_PIXELS_DEFAULT);
        liftModelMmTextField.setText("" + LIFT_MODEL_MM_DEFAULT);
        // Lifting is not available for Photon S files, because layer copying doesn't seem to be possible
        if (!(mainForm.photonFile.getPhotonFileHeader() instanceof PhotonFileHeader)) {
            liftModelMmTextField.setText("0");
            liftModelMmTextField.setEnabled(false);
        }
    }

    private void onOK() {
        dispose();
    }

    public void setInformation(PhotonFile photonFile) {
        information = null;
        this.photonFile = photonFile;
        setTitle("Create really fine supports!");

        showProgressHtml("Tweak your resin settings, so that the exposure is high enough <br/>" +
                "to keep the model connected to the supports during the <br/>" +
                "print, but low enough, so that the supports can easily be <br/>" +
                "removed before post-curing.<br/><br/>Press start to create the supports ....");

    }

    public void setProgressInformation(String str) {
        information = new StringBuilder();
        information.append("<h4>Support creation progress: ");
        information.append(str);
        information.append("</h4>");
        showProgressHtml(information.toString());
    }

    public void appendInformation(String str) {
        if (information == null) {
            information = new StringBuilder();
        }
        information.append(str);
        showProgressHtml(information.toString());
    }

    public void showProgress(String progress) {
        progressInfo.setText("<html>" + progress.replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\n", "<br/>") + "</html>");
    }

    public void showProgressHtml(String progress) {
        progressInfo.setText("<html>" + progress + "</html>");
    }

    {

    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(4, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("OK");
        panel2.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 5, 5, 5), -1, -1));
        contentPane.add(panel3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        textPane = new JScrollPane();
        panel3.add(textPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(480, 200), new Dimension(480, 200), null, 0, false));
        progressInfo = new JLabel();
        progressInfo.setText("");
        progressInfo.setVerticalAlignment(1);
        progressInfo.setVerticalTextPosition(1);
        textPane.setViewportView(progressInfo);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Create really fine support structures!");
        panel4.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        startButton = new JButton();
        startButton.setText("Start");
        panel4.add(startButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(5, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Contact width in pixels");
        label2.setToolTipText("How big the square is that connects the supports to the model.");
        panel5.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        contactSizeTextFeld = new JTextField();
        panel5.add(contactSizeTextFeld, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Support pillar distance in pixels");
        label3.setToolTipText("How far away the support pillars are from each other.");
        panel5.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        supportDistanceTextField = new JTextField();
        panel5.add(supportDistanceTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Contact height in layers");
        label4.setToolTipText("How high the thin part of the support pillar is in number of layers.");
        panel5.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        contactHeightTextField = new JTextField();
        panel5.add(contactHeightTextField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Support pillar width in pixels");
        panel5.add(label5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pillarSizeTextField = new JTextField();
        panel5.add(pillarSizeTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Lift model before adding supports (in mm)");
        panel5.add(label6, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        liftModelMmTextField = new JTextField();
        panel5.add(liftModelMmTextField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
