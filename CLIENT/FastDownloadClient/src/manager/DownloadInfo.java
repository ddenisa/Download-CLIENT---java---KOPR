package manager;

import java.io.Serializable;

public class DownloadInfo implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8540731328463039506L;

	public String address;
	public String fileName;
	public int partitionsCount;
	public long fileSize = -1;
	public long bytesDownloaded;
	
	public DownloadInfo(String address, String fileName, int partitionsCount) {
		super();
		this.address = address;
		this.fileName = fileName;
		this.partitionsCount = partitionsCount;
	}
}
