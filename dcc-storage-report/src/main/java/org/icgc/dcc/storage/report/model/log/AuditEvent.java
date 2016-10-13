package org.icgc.dcc.storage.report.model.log;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

public class AuditEvent {

	private DateTime	_timestamp;
	private String		_objectId;
	private String		_tokenDigest;
	private long		_objectSize = 0L;
	private String		_sourceIp;
	private String		_clientVersion;
	private EventType	_eventType;
	
	public AuditEvent() {}
	
	public AuditEvent(DateTime ts, String objectId, String digest, long objectSize, String sourceIp, String clientVersion, EventType eventType) {
		this(ts, objectId, digest, sourceIp, clientVersion, eventType);
		_objectSize = objectSize;
	}

	public AuditEvent(DateTime ts, String objectId, String digest, String sourceIp, String clientVersion, EventType eventType) {
		_timestamp = ts;
		_objectId = objectId;
		_tokenDigest = digest;
		_eventType = eventType;
		_sourceIp = sourceIp;
		_clientVersion = clientVersion;
	}

	/**
	 * @return the timestamp
	 */
	public DateTime getTimestamp() {
		return _timestamp;
	}

	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(DateTime timestamp) {
		_timestamp = timestamp;
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
	 * @return the tokenDigest
	 */
	public String getTokenDigest() {
		return _tokenDigest;
	}

	/**
	 * @param tokenDigest the tokenDigest to set
	 */
	public void setTokenDigest(String tokenDigest) {
		_tokenDigest = tokenDigest;
	}

	/**
	 * @return the objectSize
	 */
	public long getObjectSize() {
		return _objectSize;
	}

	/**
	 * @param objectSize the objectSize to set
	 */
	public void setObjectSize(long objectSize) {
		_objectSize = objectSize;
	}

	/**
	 * @return the Source IP
	 */
	public String getSourceIp() {
		return _sourceIp;
	}

	/**
	 * @param sourceIP the Source IP to set
	 */
	public void setSourceIp(String sourceIp) {
		_sourceIp = sourceIp;
	}

	/**
	 * @return the clientVersion
	 */
	public String getClientVersion() {
		return _clientVersion;
	}

	/**
	 * @param clientVersion the clientVersion to set
	 */
	public void setClientVersion(String clientVersion) {
		_clientVersion = clientVersion;
	}

	/**
	 * @return the eventType
	 */
	public EventType getEventType() {
		return _eventType;
	}

	/**
	 * @param eventType the eventType to set
	 */
	public void setEventType(EventType eventType) {
		_eventType = eventType;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "AuditEvent [_timestamp=" + _timestamp + ", _objectId="
				+ _objectId + ", _tokenDigest=" + _tokenDigest
				+ ", _objectSize=" + _objectSize + ", _sourceIp=" + _sourceIp
				+ ", _clientVersion=" + _clientVersion + ", _eventType="
				+ _eventType + "]";
	}

	public String getDateString() {
		return ISODateTimeFormat.basicDate().print(_timestamp);
	}
	
	public long getDateAsLong() {
		return _timestamp.dayOfMonth().roundFloorCopy().getMillis();
	}
}
