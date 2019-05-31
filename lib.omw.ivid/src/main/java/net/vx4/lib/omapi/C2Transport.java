/*
 * Copyright 2017-2019 adesso AG
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
 * @author kahlo, 2018
 * @version $Id$
 */
public class C2Transport implements TransportProvider {

    private final TransportProvider parent;
    private final short APDULen = 261; // T=0 limit
    private byte envCLA = 0x10;
    private byte envCLAlast = 0x00;
    private byte envINS = (byte) 0xC2;
    private short C2Len = 255;


    public C2Transport(final TransportProvider parent) {
        if (parent == null) {
            throw new NullPointerException("parent transport provider required");
        }
        this.parent = parent;
    }


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
            String a2 = Hex.toString(apdu);
            if (apdu[5] == 0) {
                a2 = a2.substring(0, 8) + a2.substring(12);
            }

            apdu = Hex.fromString(a2.endsWith("0000") ? a2.substring(0, a2.length() - 4) : a2);

            if (apdu.length > APDULen) {
                int sent = 0;
                byte[] last = null;
                while (apdu.length - sent > 0) {
                    final int len = apdu.length - sent > C2Len ? C2Len : apdu.length - sent;
                    if (apdu.length - (sent + len) > 0) {
                        last = parent.transmit(
                                Hex.fromString(Hex.toString(new byte[]{envCLA, envINS, 0, 0, (byte) len})
                                        + Hex.toString(apdu, sent, len & 0xFF)));
                    } else {
                        last = parent.transmit(
                                Hex.fromString(
                                        Hex.toString(new byte[]{envCLAlast, envINS, 0, 0, (byte) len})
                                                + Hex.toString(apdu, sent, len & 0xFF)));
                    }
                    sent += len;
                }
                return last;
            }
        }

        return parent.transmit(apdu);
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
