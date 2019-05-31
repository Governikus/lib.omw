package org.simalliance.openmobileapi;

public class Channel {
    public void close() {
    }


    public Session getSession() {
        return null;
    }


    public boolean isBasicChannel() {
        return false;
    }


    public boolean isClosed() {
        return false;
    }


    public byte[] transmit(final byte[] command) {
        return null;
    }


    public byte[] getSelectResponse() {
        return null;
    }


}