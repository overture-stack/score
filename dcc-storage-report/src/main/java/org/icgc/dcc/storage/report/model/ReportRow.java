package org.icgc.dcc.storage.report.model;

import java.util.Date;

import org.joda.time.DateTime;

public class ReportRow {

	private DateTime	_eventDate;
	private String		_eventType;
	private int			_count = 0;
	private String		_accessToken;
	private String 		_userName;
	private boolean		_isDeleted = false;
	private String		_fileFormat;
	private String		_objectId;
	
	public ReportRow() {}

	/**
	 * @return the eventDate
	 */
	public DateTime getEventDate() {
		return _eventDate;
	}

	/**
	 * @param eventDate the eventDate to set
	 */
	public void setEventDate(DateTime eventDate) {
		_eventDate = eventDate;
	}
	
	/**
	 * @param eventDate the eventDate to set as a java.util.Date
	 */
	public void setEventDate(Date eventDate) {
		_eventDate = new DateTime(eventDate);
	}

	
	/**
	 * @return the eventType
	 */
	public String getEventType() {
		return _eventType;
	}

	/**
	 * @param eventType the eventType to set
	 */
	public void setEventType(String eventType) {
		_eventType = eventType;
	}

	/**
	 * @return the count
	 */
	public int getCount() {
		return _count;
	}

	/**
	 * @param count the count to set
	 */
	public void setCount(int count) {
		_count = count;
	}

	/**
	 * @return the accessToken
	 */
	public String getAccessToken() {
		return _accessToken;
	}

	/**
	 * @param accessToken the accessToken to set
	 */
	public void setAccessToken(String accessToken) {
		_accessToken = accessToken;
	}

	/**
	 * @return the objectId
	 */
	public String getObjectId() {
		return _objectId;
	}

	/**
	 * @param objectId the objectId to set
	 */
	public void setObjectId(String objectId) {
		_objectId = objectId;
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return _userName == null ? "<none>" : _userName;
	}

	/**
	 * @param userEmail the userEmail to set
	 */
	public void setUserName(String userName) {
		_userName = userName;
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
	 * @return the isDeleted
	 */
	public boolean isDeleted() {
		return _isDeleted;
	}

	/**
	 * @param isDeleted the isDeleted to set
	 */
	public void setDeleted(boolean isDeleted) {
		_isDeleted = isDeleted;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ReportRow [_eventDate=" + _eventDate + ", _eventType="
				+ _eventType + ", _count=" + _count + ", _accessToken="
				+ _accessToken + ", _userName=" + _userName + " _isDeleted" 
				+ _isDeleted + ", _objectId=" + _objectId + ", _fileFormat=" + _fileFormat + "]";
	}
}
