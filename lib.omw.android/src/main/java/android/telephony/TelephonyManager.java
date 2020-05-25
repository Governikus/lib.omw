package android.telephony;

public class TelephonyManager {
    public boolean hasCarrierPrivileges() {
        return false;
    }

    public boolean hasIccCard() {
        return false;
    }

    public IccOpenLogicalChannelResponse iccOpenLogicalChannel(String AID) {
        return null;
    }

    public boolean iccCloseLogicalChannel(int channel) {
        return false;
    }

    public String iccTransmitApduLogicalChannel(int channel, int cla, int instruction, int p1, int p2, int p3, String data) {
        return null;
    }
}
