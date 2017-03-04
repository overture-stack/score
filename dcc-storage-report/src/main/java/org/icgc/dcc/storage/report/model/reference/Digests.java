package org.icgc.dcc.storage.report.model.reference;

public class Digests {

	private TokenEntry[]	_digests;
	
	public Digests() {
	}

	/**
	 * @return the entries
	 */
	public TokenEntry[] getDigests() {
		return _digests;
	}

	/**
	 * @param entries the entries to set
	 */
	public void setDigests(TokenEntry[] digests) {
		_digests = digests;
	}
}
