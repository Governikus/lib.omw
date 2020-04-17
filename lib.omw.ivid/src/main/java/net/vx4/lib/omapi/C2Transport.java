/*
 * Copyright 2017-2020 adesso SE
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence"); You may
 * not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the Licence for the
 * specific language governing permissions and limitations under the Licence.
 */
package net.vx4.lib.omapi;

/**
 * C2Transport is an implementation of a transport provider stack element to handle extended length APDU mapping to
 * ENVELOPE (C2) / GET RESPONSE (C0) APDUs. Hence its name as the response is handled by the underlying stack and this
 * class "only" cuts long APDUs into shorter ENVELOPE ADPUS.
 *
 * It is currently expected that GET RESPONSE handling is done under the hood. There exist only rare fails, which
 * are assumed to be implementation fails. Might be extended if there is a reasonable demand. (Don't support bugs.)
 *
 * @author kahlo, 2018
 * @version $Id$
 */
public class C2Transport implements TransportProvider {

    private final short APDULen = 261; // T=0 limit
    private final TransportProvider parent;
    private byte envCLA = 0x00; // stick to ETSI-style without command chaining as we're on T=0 anyway
    private byte envCLAlast = 0x00;
    private byte envINS = (byte) 0xC2;
    private short C2Len = 255;

    /**
     * Create an ENVELOPE transport provider from a physical layer transport provider such as
     * ChannelTransportProvider.
     *
     * @param parent
     */
    public C2Transport(final TransportProvider parent) {
        if (parent == null) {
            throw new NullPointerException("parent transport provider required");
        }
        this.parent = parent;
    }


    /**
     * Transmit an extended length APDU over a physical link layer.
     * (Usually and intended for T=0, but sometimes necessary for T=1 over SWP also.)
     *
     * NOTE: If not using secure messaging but plain-text extended length APDUs and the other end is a card that
     * responds "early" with intended no data, the additional envelope might result in an erroneous 6700.
     * Only real fix: don't do it.
     *
     * @param apdu - APDU to be transmitted
     * @return
     */
    @Override
    public byte[] transmit(byte[] apdu) {
        final byte channelId = ((ChannelTransportProvider) this.getParent()).getChannelId();

        if (channelId < 4) {
            apdu[0] = (byte) (apdu[0] & 0xBC | channelId);
        } else if (channelId < 20) {
            final boolean isSM = (apdu[0] & 0x0C) != 0;
            apdu[0] = (byte) (apdu[0] & 0xB0 | 0x40 | channelId - 4);
            if (isSM) {
                apdu[0] |= 0x20;
            }
        }

        if (apdu.length > APDULen || apdu.length > 5 && apdu[4] == 0) {
            // sanitize APDU encoding
            final short lc = (short) (((apdu[5] & 0xFF) << 8) + (apdu[6] & 0xFF));
            String a2 = Hex.toString(apdu, 0, 7 + lc); // shorten APDU, strip off Le

            // if (apdu[5] == 0) { // shorten APDU if length < 0x0100
            if (lc < 0x0100) { // shorten APDU if length < 0x0100
                a2 = a2.substring(0, 8) + a2.substring(12);
            }
            apdu = Hex.fromString(a2);

            if (apdu.length > APDULen) {
                int sent = 0;
                byte[] last = null;
                while (apdu.length - sent > 0) {
                    final int len = apdu.length - sent > C2Len ? C2Len : apdu.length - sent;
                    if (apdu.length - (sent + len) > 0) {
                        last = parent.transmit(Hex.fromString(Hex.toString(new byte[]{envCLA, envINS, 0, 0, (byte) len})
                                + Hex.toString(apdu, sent, len & 0xFF)));
                    } else {
                        last = parent.transmit(Hex.fromString(Hex.toString(new byte[]{envCLAlast, envINS, 0, 0, (byte) len})
                                + Hex.toString(apdu, sent, len & 0xFF)));
                    }
                    sent += len;
                }

                // ETSI-style, long variant with no data but SW OK send another empty ENVELOPE for EOF.
                if (envCLA == 0x00 && last != null && last.length == 0 && parent.lastSW() == 0x9000) {
                    last = parent.transmit(new byte[] { envCLAlast, envINS, 0, 0, 0 });
                }

                return last;
            }
        }

        return parent.transmit(apdu);
    }

    /**
     * change maximum length of ENVELOPE frame
     * @param len
     */
    public void setLength(final byte len) {
        if (len <= 255) {
            C2Len = (short) (len & 0xFF);
        }
    }

    /**
     * Set class byte values for "first" / "ongoing" frames and "only" / "last" frame.
     *
     * ETSI as well as JavaCard reference implementations stick to "00" for both, the last frame is detected
     * either by a length smaller than 0xFF or a single frame with length 00. Frames with length 00 are
     * mandatory for some implementations. That's why some assumptions are made if an empty frame should be
     * sent.
     *
     * ISO on the hand chooses command chaining mode, which is supported in hand crafted implementations, tolerated by
     * some JCREs and denied by some others. setCLA((byte) 0x10, (byte) 0x00) enforces ISO mode.
     *
     * @param firstCLA
     * @param lastCLA
     */
    public void setCLA(final byte firstCLA, final byte lastCLA) {
        envCLA = firstCLA;
        envCLAlast = lastCLA;
    }

    /**
     * Set a custom instruction byte values for ENVELOPEs. Very unusual.
     *
     * @param INS
     */
    public void setINS(final byte INS) {
        envINS = INS;
    }

    @Override
    public void close() {
        parent.close();
    }


    @Override
    public Object getParent() {
        return parent;
    }


    @Override
    public int lastSW() {
        return parent.lastSW();
    }
}
