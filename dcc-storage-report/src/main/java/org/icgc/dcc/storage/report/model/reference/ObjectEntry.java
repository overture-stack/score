package org.icgc.dcc.storage.report.model.reference;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ObjectEntry {

	@JsonProperty("object_id")
	private String _id;
	
	@JsonProperty("file_format")
	private String _fileFormat;
	
	@JsonProperty("file_name")
	private String _fileName;

	@JsonProperty("file_size")
	private long _fileSize;

	
	public ObjectEntry() {}

	public ObjectEntry(String objectId, String fileFormat, String fileName, long fileSize) {
		super();
		_id = objectId;
		_fileFormat = fileFormat;
		_fileName = fileName;
		_fileSize = fileSize;
	}
	
	/**
	 * @return the id
	 */
	public String getId() {
		return _id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		_id = id;
	}

	/**
	 * @return the fileFormat
	 */
	public String getFileFormat() {
		return _fileFormat;
	}

	/**
	 * @param fileFormat the fileFormat to set
	 */
	public void setFileFormat(String fileFormat) {
		_fileFormat = fileFormat;
	}

	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return _fileName;
	}

	/**
	 * @param fileName the fileName to set
	 */
	public void setFileName(String fileName) {
		_fileName = fileName;
	}

	/**
	 * @return the fileSize
	 */
	public long getFileSize() {
		return _fileSize;
	}

	/**
	 * @param fileSize the fileSize to set
	 */
	public void setFileSize(long fileSize) {
		_fileSize = fileSize;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ObjectEntry [id=" + _id + ", fileFormat=" + _fileFormat + ", fileName=" + _fileName + ", fileSize="
				+ _fileSize + "]";
	}

}
