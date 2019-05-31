package org.simalliance.openmobileapi;

import android.content.Context;


public class SEService {
    public SEService(final Context context, final SEService.CallBack listener) {
    }


    public Reader[] getReaders() {
        return null;
    }


    public boolean isConnected() {
        return true;
    }


    public void shutdown() {
    }


    public interface CallBack {
        void serviceConnected(SEService service);
    }

}
