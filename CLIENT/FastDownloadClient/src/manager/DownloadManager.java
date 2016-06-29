package manager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import javax.swing.Timer;

import gui.DownloadPanel;
import socket.Client;
import socket.ClientDelegate;
import socket.DownloadRequestMessage;

public class DownloadManager implements ClientDelegate, ActionListener {
	private DownloadInfo downloadInfo;
	private Client[] clients;
	private DownloadPanel downloadPanel;
	private Timer timer;
	
	public DownloadManager(DownloadPanel downloadPanel) {
		this.downloadPanel = downloadPanel;
		loadDownloadInfo();
		
		timer = new Timer(1000, this);
		timer.setInitialDelay(1000);
		timer.start(); 
	}
	
	public boolean downloading() {
		synchronized(this) {
			return clients != null;
		}
	}
	public DownloadInfo downloadInfo() {
		return downloadInfo;
	}
	
	private void loadDownloadInfo() {
		ObjectInputStream ois = null;
		try {
			FileInputStream fis = new FileInputStream("download-info.fd");
			ois = new ObjectInputStream(fis);
			downloadInfo = (DownloadInfo)ois.readObject();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				if (ois != null) {
					ois.close();
				}
			} catch (IOException e) {
			}
		}
	}
	
	public void saveDownloadInfo() {
		ObjectOutputStream ois = null;
		try {
			FileOutputStream fis = new FileOutputStream("download-info.fd");
			ois = new ObjectOutputStream(fis);
			ois.writeObject(downloadInfo);
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				ois.close();
			} catch (IOException e) {
			}
		}
	}
	
	public void download(DownloadInfo downloadInfo) {
		this.downloadInfo = downloadInfo;
		saveDownloadInfo();
		
		resume();
	}
	
	public void resume() {
		synchronized (this) {
			clients = new Client[downloadInfo.partitionsCount];
		}
		
		//start clients
		for (int i = 0; i < clients.length; i++) {
			DownloadRequestMessage message = new DownloadRequestMessage(downloadInfo.fileName,
					downloadInfo.partitionsCount, i, 0);
			clients[i] = new Client(downloadInfo.address, message, this);
			new Thread(clients[i]).start();
		}
		
		downloadPanel.updateGUI();
		downloadPanel.setMessage("Downloading");
	}
	
	public void pause() {
		synchronized (this) {
			if (clients == null) 
				return;
			
			// stop all sockets
			for (int i = 0; i < clients.length; i++) {
				clients[i].disconnect();
			}
			synchronized (this) {
				clients = null;
			}
		}
		downloadPanel.updateGUI();
		downloadPanel.setMessage("Paused");
		
		saveDownloadInfo();
	}
	
	public void stop() {
		pause();
		downloadPanel.setProgress(0);
		synchronized (this) {			
			//delete temporary files
			for (int i = 0; i < downloadInfo.partitionsCount; i++) {
				File file = new File("tmp"+i+".fd");
				file.delete();
			}
			File file = new File("download-info.fd");
			file.delete();
			downloadInfo = null;
			downloadPanel.updateGUI();
			downloadPanel.setMessage("Stopped");
		}
	}
	
	private void finishDownloading() {
		downloadPanel.setProgress(1);
		//merge files
		OutputStream output = null;
		FileInputStream input = null;
		try {
            output = new FileOutputStream(downloadInfo.fileName);
            for (int i = 0; i < this.downloadInfo().partitionsCount; i++) {
            	input = new FileInputStream("tmp"+i+".fd");
            	byte[] buffer = new byte[1024];
                int length;
                while ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
            }
    		stop(); //cleanup
    		downloadPanel.setMessage("Downloaded");
		}
		catch (IOException e) {
			e.printStackTrace();
			stop(); //cleanup
			downloadPanel.setMessage("Error");
		}
		finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void clientDidFinishDownloading(Client client, Boolean success) {
		if (!success) {
			boolean noError;
			synchronized (this) {
				noError = clients == null; //error may be caused by pausing or stopping, but it's OK
			}
			if (!noError) { 
				stop();
				downloadPanel.setMessage("Error");
			}
		}
		else {
			boolean allDone = true;
			synchronized (this) {
				//find if all clients are removed
				if (clients != null) {
					for (int i = 0; i < clients.length; i++) {
						if (clients[i].isRunning()) {
							allDone = false;
						}
					}
				}
			}
			if (allDone) {
				finishDownloading();
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		synchronized (this) {
			if (clients == null)
				return;
			
			// update progress bar
			long fileSize = 0;
			long bytesDownloaded = 0;
			for (Client client : clients) {
				long partitionSize = client.partitionSize();
				if (partitionSize == -1)
					fileSize = -1;
				else if (fileSize != -1)
					fileSize += partitionSize;
				bytesDownloaded += client.bytesDownloaded();
			}
			downloadInfo.fileSize = fileSize;
			downloadInfo.bytesDownloaded = bytesDownloaded;
			downloadPanel.setProgress(downloadInfo == null ? 0 :
	        	(downloadInfo.fileSize == -1 ? 0 :
	        		downloadInfo.bytesDownloaded/(float)downloadInfo.fileSize));
		}
	}
}
