package gui;
/*
 * BoxLayoutDemo.java requires no other files.
 */
 
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import manager.DownloadInfo;
import manager.DownloadManager;
 
public class DownloadPanel extends JFrame implements ActionListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6678537994500653484L;
	
	private DownloadManager downloadManager;
	
	private JTextField addressTextField;
	private JTextField fileNameTextField;
	private JTextField socketsCountTextField;
	private JButton downloadButton;
	private JButton pauseResumeButton;
	private JLabel messageLabel;
	private JProgressBar progressBar;
	
    public DownloadPanel() {
    	super("Fast download");
    	downloadManager = new DownloadManager(this);
    	setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    	addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                downloadManager.pause();
                ((JFrame)(e.getComponent())).dispose();
                System.exit(0);
            }
        });
    	
        Container pane = getContentPane();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
 
        DownloadInfo downloadInfo = downloadManager.downloadInfo();
        
        JLabel label = new JLabel("Host:");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        pane.add(label);
        addressTextField = new JTextField(downloadInfo == null ? "127.0.0.1" : downloadInfo.address);
        addressTextField.setAlignmentX(Component.LEFT_ALIGNMENT);
        pane.add(addressTextField);
        
        label = new JLabel("File name:");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        pane.add(label);
        fileNameTextField = new JTextField(downloadInfo == null ? "cs.zip" : downloadInfo.fileName);
        fileNameTextField.setAlignmentX(Component.LEFT_ALIGNMENT);
        pane.add(fileNameTextField);
        
        label = new JLabel("Sockets count:");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        pane.add(label);
        socketsCountTextField = new JTextField(downloadInfo == null ? "1" : ""+downloadInfo.partitionsCount);
        socketsCountTextField.setAlignmentX(Component.LEFT_ALIGNMENT);
        pane.add(socketsCountTextField);
        
        downloadButton = new JButton();
        downloadButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        downloadButton.addActionListener(this);
        pane.add(downloadButton);
        
        pauseResumeButton = new JButton();
        pauseResumeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        pauseResumeButton.addActionListener(this);
        pane.add(pauseResumeButton);
        
        messageLabel = new JLabel("Welcome");
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        pane.add(messageLabel);
        
        progressBar = new JProgressBar(0, 100000);
        setProgress(downloadInfo == null ? 0 :
        	(downloadInfo.fileSize == -1 ? 0 :
        		downloadInfo.bytesDownloaded/(float)downloadInfo.fileSize));
        pane.add(progressBar);
        
        updateGUI();
    }
    
    public void updateGUI() {
    	synchronized (this) {
            DownloadInfo downloadInfo = downloadManager.downloadInfo();
            boolean downloading = downloadManager.downloading();

            addressTextField.setEnabled(downloadInfo == null);
            fileNameTextField.setEnabled(downloadInfo == null);
            socketsCountTextField.setEnabled(downloadInfo == null);
            downloadButton.setText(downloadInfo == null ? "Download" : "Cancel"); 
            downloadButton.setActionCommand(downloadInfo == null ? "Download" : "Cancel"); 
            pauseResumeButton.setText(downloading ? "Pause" : "Resume"); 
            pauseResumeButton.setActionCommand(downloading ? "Pause" : "Resume"); 
            pauseResumeButton.setEnabled(downloadInfo != null);
    	}
    }
    
    public void setMessage(String message) {
    	synchronized (this) {
    		messageLabel.setText(message);
    	}
    }
    
    public void setProgress(float progress) {
    	synchronized (this) {
    		progressBar.setValue((int)(progress == -1 ? 0 : progress*100000));
    	}
    }
    
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Download")) {
        	int partitionsCount = Integer.parseInt(socketsCountTextField.getText());
        	if (partitionsCount < 1) {
        		setMessage("Socket count must be > 0");
        	}
        	DownloadInfo info = new DownloadInfo(addressTextField.getText(), fileNameTextField.getText(),partitionsCount);
        	downloadManager.download(info);
        }
        else if (e.getActionCommand().equals("Pause")) {
        	downloadManager.pause();
        }
        else if (e.getActionCommand().equals("Resume")) {
        	downloadManager.resume();
        }
        else if (e.getActionCommand().equals("Cancel")) {
        	downloadManager.stop();
        }
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        DownloadPanel frame = new DownloadPanel();
        
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
 
    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}