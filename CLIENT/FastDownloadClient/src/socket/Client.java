package socket;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Client implements Runnable {
	
	// for I/O
	public static final int defPort = 7890;
	
	private ObjectOutputStream sOutput;		// to write on the socket
	private Socket socket;
	private String ip;
	private DownloadRequestMessage message;
	private ClientDelegate delegate;
	private long partitionSize = -1;
	private long bytesDownloaded;
	
	// the server, the port and the user name
	
	public Client(String ip, DownloadRequestMessage message, ClientDelegate delegate) {
		this.ip = ip;
		this.message = message;
		this.delegate = delegate;
	}
	
	public void run() {
		delegate.clientDidFinishDownloading(this, downloadFile());
	}
	
	public boolean downloadFile() {
		// try to connect to the server
		try {
			socket = new Socket(ip, defPort);
		} 
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		
		String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
		System.out.println(msg);
	
		//open output file
        File file = new File("tmp"+message.partitionIndex+".fd");
        if (!file.exists())
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				disconnect();
				return false;
			}
        
        //send message to server
		try
		{
			synchronized(this) {
				message.offset = file.length(); // finds how much is already downloaded
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sOutput.writeObject(message);
			}
		}
		catch (IOException eIO) {
			disconnect();
			return false;
		}
		
		//read response
		OutputStream output = null;
		try {
            DataInputStream clientData = new DataInputStream(socket.getInputStream());
            
            output = new FileOutputStream(file, true);
            long size = clientData.readLong(); //read length of file
            synchronized(this) {
            	partitionSize = size+file.length();
            	bytesDownloaded = file.length();
            }
            
            //read file
            byte[] buffer = new byte[Globals.batchSize];
            int bytesRead;
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
                synchronized(this) {
                	bytesDownloaded += bytesRead;
                }
            }
        } catch (IOException e) {
        } finally {
            try {
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
		
		disconnect();
		// success we inform the caller that it worked
		return bytesDownloaded == partitionSize;
	}
	
	public void disconnect() {
		if (socket != null && !socket.isClosed()) {
			System.out.println(message+" logged out");
		}
		try {
			synchronized(this) {
				if(sOutput != null) sOutput.close();
			}
		}
		catch(Exception e) {e.printStackTrace();} // not much else I can do
        try{
			if(socket != null) {
				socket.close();		
			}
		}
		catch(Exception e) {e.printStackTrace();} // not much else I can do
	}

	public boolean isRunning() {
		return socket != null && socket.isConnected() && !socket.isClosed();
	}
	
	public long partitionSize() {
		synchronized(this) {
			return partitionSize;
		}
	}
	
	public long bytesDownloaded() {
		synchronized(this) {
			return bytesDownloaded;
		}
	}
	
	public DownloadRequestMessage message() {
		synchronized(this) {
			return message;
		}
	}
}
