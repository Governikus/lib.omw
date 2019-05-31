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

import org.simalliance.openmobileapi.Channel;

/**
 * The ChannelTransportProvider deals with automatically negotiated channels on the underlying terminal interface.
 * If a channel has been opened successfully it is the first contact to the selected app, so the SELECT APDU
 * response is fetch belowed and used to adopt to protocol and implementation specifics of the secure element.
 *
 * @author kahlo, 2018
 * @version $Id$
 */
public class ChannelTransportProvider implements TransportProvider {

    private final byte channelId;
    private Channel channel = null;
    private int lastSW = -1;

    /**
     *
     * @param channel
     */
    public ChannelTransportProvider(final Channel channel) {
        this.channel = channel;
        final byte[] selRes = this.channel.getSelectResponse();
        channelId = TLV.get(TLV.get(selRes, (byte) 0x6F), (byte) 0x85)[0];
    }


    @Override
    public void close() {
        // TODO: checking closing behaviour
        System.out.println("ChannelTransportProvider.close(): WARNING, not closed");
        // channel.close();
    }


    @Override
    public Object getParent() {
        return channel;
    }


    @Override
    public int lastSW() {
        return lastSW;
    }


    public byte getChannelId() {
        return channelId;
    }


    @Override
    public byte[] transmit(final byte[] apdu) {
        lastSW = -1;

        System.out.println("ChannelTrannsport: channel = " + channel + " open? " + (channel != null ? !channel.isClosed() : "<null>"));
        if (channel != null && !channel.isClosed()) {
            System.out.println("<[SE] apdu = [" + Hex.toString(apdu) + "]");
            byte[] rpdu = channel.transmit(apdu);
            System.out.println(">[SE] rpdu = [" + Hex.toString(rpdu) + "]");

            if (rpdu != null && rpdu.length >= 2) {
                lastSW = ((rpdu[rpdu.length - 2] & 0xFF) << 8) + (rpdu[rpdu.length - 1] & 0xFF);
                rpdu = ArrayTool.sub(rpdu, 0, rpdu.length - 2);
            }
            return rpdu;
        } else {
            return null;
        }
    }
}
