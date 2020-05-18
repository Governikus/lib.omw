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
package net.vx4.lib.ivid;

/**
 * The ChannelTransportProvider deals with automatically negotiated channels on the underlying terminal interface. If a
 * channel has been opened successfully it is the first contact to the selected app, so the SELECT APDU response is
 * fetch belowed and used to adopt to protocol and implementation specifics of the secure element.
 *
 * @author kahlo, 2018
 * @version $Id$
 */
public class ChannelTransportProvider implements TransportProvider {

    private final byte channelId, protocol, appletState;
    private final SEAL.Channel channel;
    private int lastSW = -1;


    /**
     * Constructor based on SEAL.Channel. If the previously selected applet does not respond with proprietary FCI data
     * to determine logical channel ID, protocol and state defaults are used. (T = 0 and basic channel)
     *
     * @param channel
     */
    public ChannelTransportProvider(final SEAL.Channel channel) {
        this.channel = channel;
        byte[] info = this.channel.getSelectResponse();
        info = info == null ? null : TLV.get(info, (byte) 0x6F);
        info = info == null ? null : TLV.get(info, (byte) 0x85);

        if (info != null && info.length >= 3) {
            this.channelId = info[0];
            this.protocol = info[1];
            this.appletState = info[2];
        } else {
            this.channelId = this.protocol = this.appletState = 0;
        }

        // is often "null" anyway, might be a decision helper with some secure elements
        // atr = channel.getSession().getATR();
    }


    @Override
    public void close() {
        // System.out.println("ChannelTransportProvider.close(): WARNING, not closed");
        this.channel.close();
    }


    @Override
    public SEAL.Channel getParent() {
        return this.channel;
    }


    @Override
    public int lastSW() {
        return this.lastSW;
    }


    /**
     * @return
     */
    public byte getChannelId() {
        return this.channelId;
    }


    /**
     * @return
     */
    public byte getProtocol() {
        return this.protocol;
    }


    /**
     * @return
     */
    public byte getAppletState() {
        return this.appletState;
    }


    /**
     *
     */
    @Override
    public byte[] transmit(final byte[] apdu) {
        this.lastSW = -1;

        System.out.println("ChannelTrannsport: channel = " + this.channel + " open? "
                + (this.channel != null ? this.channel.isOpen() : "<null>"));
        if (this.channel != null && this.channel.isOpen()) {
            System.out.println("<[SE] apdu = [" + Hex.x(apdu) + "]");
            byte[] rpdu = this.channel.transmit(apdu);
            System.out.println(">[SE] rpdu = [" + Hex.x(rpdu) + "]");

            if (rpdu != null && rpdu.length >= 2) {
                this.lastSW = ((rpdu[rpdu.length - 2] & 0xFF) << 8) + (rpdu[rpdu.length - 1] & 0xFF);
                rpdu = ArrayTool.sub(rpdu, 0, rpdu.length - 2);
            }
            return rpdu;
        } else {
            return null;
        }
    }
}
