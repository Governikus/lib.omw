package android.se.omapi;

import java.util.concurrent.Executor;

import android.content.Context;


public class SEService {
	public SEService(final Context context, final Executor executor, final SEService.OnConnectedListener listener) {
	}


	public Reader[] getReaders() {
		return null;
	}


	public Reader getUiccReader(final int slotNumber) throws IllegalArgumentException {
		return null;
	}


	public String getVersion() {
		return "VX4 OMAPI mock";
	}


	public boolean isConnected() {
		return true;
	}


	public void shutdown() {
	}


	public static interface OnConnectedListener {
		abstract void onConnected();
	}
}
