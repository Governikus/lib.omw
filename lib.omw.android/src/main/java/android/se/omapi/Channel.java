package android.se.omapi;

public class Channel {
	public void close() {
	}


	public Session getSession() {
		return null;
	}


	public boolean isBasicChannel() {
		return false;
	}


	public boolean isOpen() {
		return false;
	}


	public boolean selectNext() {
		return false;
	}


	public byte[] transmit(final byte[] command) {
		return null;
	}


	public byte[] getSelectResponse() {
		return null;
	}
}