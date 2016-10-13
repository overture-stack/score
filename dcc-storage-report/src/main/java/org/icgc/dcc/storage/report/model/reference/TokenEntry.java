package org.icgc.dcc.storage.report.model.reference;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenEntry {

	@JsonProperty("user_name")
	private String _userName;
	
	@JsonProperty("token_md5")
	private String _tokenMD5;
	
	@JsonProperty("deleted")
	private boolean _deleted;
	
	public TokenEntry() {
	}

	public TokenEntry(String userName, String token, boolean deleted) {
		_userName = userName;
		_tokenMD5 = token;
		_deleted = deleted;
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return _userName;
	}

	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		_userName = userName;
	}

	/**
	 * @return the tokenMD5
	 */
	public String getTokenMD5() {
		return _tokenMD5;
	}

	/**
	 * @param tokenMD5 the tokenMD5 to set
	 */
	public void setTokenMD5(String tokenMD5) {
		_tokenMD5 = tokenMD5;
	}

	/**
	 * @return the deleted
	 */
	public boolean isDeleted() {
		return _deleted;
	}

	/**
	 * @param deleted the deleted to set
	 */
	public void setDeleted(boolean deleted) {
		_deleted = deleted;
	}
	
	
}
