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

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;


/**
 * @author ckahlo, 2018 - 2019
 * <p>
 * This file is a stub, proving a working prototype. Work in progress. As storage on the UICC is limited the OMAPITP
 * wraps away "default" data as EF DIRECTORY, EF CARD ACCESS and EF CARD SECURITY. This data is public and until
 * algorithms are added or changed quite constant. EF CARD SECURITY changes due its included public key for chip
 * authentication, so this will be the first part to be stored separately and in an updatable manner according to the
 * personalization of the applet(s) in use.
 *
 * The OMAPITP also implements the PACE-to-CCID vendor command mapping to trigger PACE' / PACElight on "EstablishPACE"
 * command using the interal unlock key mechanism. Afterwards data is tunneled transparently through the standard
 * ISO secure messaging transport provider.
 *
 */
public final class OMAPITP implements TransportProvider {
    public static final String AID_NPA = "E80704007F00070302";
    public static final String AID_VX4ID = "D2760000930101";

    private static final byte[] EF_DIR = Hex.x(
            "61324F0FE828BD080FA000000167455349474E500F434941207A752044462E655369676E5100730C4F0AA000000167455349474E61094F07A0000002471001610B4F09E80704007F00070302610C4F0AA000000167455349474E");

    private static final byte[] EF_ATR = Hex.x("");

    private static final byte[] EF_CA = Hex.x(
            "3181C13012060A04007F0007020204020202010202010D300D060804007F00070202020201023012060A04007F00070202030202020102020129301C060904007F000702020302300C060704007F0007010202010D020129303E060804007F000702020831323012060A04007F0007020203020202010202012D301C060904007F000702020302300C060704007F0007010202010D02012D302A060804007F0007020206161E687474703A2F2F6273692E62756E642E64652F6369662F6E70612E786D6C");

    private static final byte[] EF_CS = Hex.x(
            "308206B006092A864886F70D010702A08206A13082069D020103310F300D0609608648016503040204050030820188060804007F0007030201A082017A04820176318201723012060A04007F0007020204020202010202010D300D060804007F00070202020201023017060A04007F0007020205020330090201010201010101003019060904007F000702020502300C060704007F0007010202010D3017060A04007F0007020205020330090201010201020101FF3012060A04007F00070202030202020102020129301C060904007F000702020302300C060704007F0007010202010D0201293062060904007F0007020201023052300C060704007F0007010202010D0342000419D4B7447788B0E1993DB35500999627E739A4E5E35F02D8FB07D6122E76567F17758D7A3AA6943EF23E5E2909B3E8B31BFAA4544C2CBF1FB487F31FF239C8F8020129303E060804007F000702020831323012060A04007F0007020203020202010202012D301C060904007F000702020302300C060704007F0007010202010D02012D302A060804007F0007020206161E687474703A2F2F6273692E62756E642E64652F6369662F6E70612E786D6CA08203EE308203EA30820371A00302010202012D300A06082A8648CE3D0403033055310B3009060355040613024445310D300B060355040A0C0462756E64310C300A060355040B0C03627369310D300B0603550405130430303033311A301806035504030C115445535420637363612D6765726D616E79301E170D3134303732333036333034305A170D3235303232333233353935395A305C310B3009060355040613024445310C300A060355040A0C03425349310D300B06035504051304303035303130302E06035504030C275445535420446F63756D656E74205369676E6572204964656E7469747920446F63756D656E7473308201133081D406072A8648CE3D02013081C8020101302806072A8648CE3D0101021D00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000001303C041CFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFE041CB4050A850C04B3ABF54132565044B0B7D7BFD8BA270B39432355FFB4043904B70E0CBD6BB4BF7F321390B94A03C1D356C21122343280D6115C1D21BD376388B5F723FB4C22DFE6CD4375A05A07476444D5819985007E34021D00FFFFFFFFFFFFFFFFFFFFFFFFFFFF16A2E0B8F03E13DD29455C5C2A3D020101033A00043A79C3CBFDB8A6E569C9226CD54E81DE14381BC92A61AD554EBF349BFAFD72F18DC85D78E49742F37A75411E28E894308D6880D1380FBEB4A382016D30820169301F0603551D23041830168014A38DB7C0DBECF5A91FCA6B3D5EB2F328B5A5DC17301D0603551D0E04160414CF0A2AC150F28ADE4329F662E3D21CE5C78BCDE9300E0603551D0F0101FF040403020780302B0603551D1004243022800F32303134303732333036333034305A810F32303135303232333233353935395A30160603551D20040F300D300B060904007F000703010101302D0603551D1104263024821262756E646573647275636B657265692E6465A40E300C310A300806035504070C014430510603551D12044A30488118637363612D6765726D616E79406273692E62756E642E6465861C68747470733A2F2F7777772E6273692E62756E642E64652F63736361A40E300C310A300806035504070C01443019060767810801010602040E300C02010031071301411302494430350603551D1F042E302C302AA028A0268624687474703A2F2F7777772E6273692E62756E642E64652F746573745F637363615F63726C300A06082A8648CE3D040303036700306402300D90B1C6E52B5E20D8ECE1520981E11EF1AF02906A930420F87E90315588B70C0C9642160E877E42B1CE311849E388B802303450209749C1368D965CE879460F729E68BAB9D5D3269724721D0C564FB2752EC4C0F8F5542990CFDB7C848AA7D0A2BB3182010730820103020101305A3055310B3009060355040613024445310D300B060355040A0C0462756E64310C300A060355040B0C03627369310D300B0603550405130430303033311A301806035504030C115445535420637363612D6765726D616E7902012D300D06096086480165030402040500A046301706092A864886F70D010903310A060804007F0007030201302B06092A864886F70D010904311E041CC57AFB616E6837B63B22666F48547E3AD71795E33326C0CE5FF27C3A300A06082A8648CE3D040301043F303D021C58AE1E82475BE9C9167810593FCF7CA791DE45910380D5CF4FEB84D7021D00FFD316D91D85664479596BAFBBB2532540047334668E0C47EE99B826");
    private final TransportProvider plainTP;
    //
    int lastSW = -1;
    private TransportProvider tp;
    private CallbackHandler cbh;
    private byte[] efData = null;

    /**
     *
     */
    public OMAPITP(final Channel channel) {
        this(new ChannelTransportProvider(channel));
    }


    /**
     * @param seTP
     */
    public OMAPITP(final TransportProvider seTP) {
        System.out.println("OMAPITP: seTP = [" + seTP + "]");
        System.out.println("OMAPITP: seTP.getParent = [" + seTP.getParent() + "]");

        // if ("T=0".equals(card.getProtocol())) {
        tp = new C2Transport(seTP);
        // tp = seTP;
        // }

        System.out.println(tp);
        plainTP = tp;
    }

    public final void setCallbackHandler(final CallbackHandler cbh) {
        this.cbh = cbh;
    }


    public final byte[] process(final byte[] apdu) {
        byte[] rpdu = new byte[0];
        short sw = (short) 0x9000;

        try {
            System.out.println("APDU: " + Hex.x(apdu));
            final String cmd = Hex.x(apdu);

            if (cmd.startsWith("FF9A")) { // reader commands
                if (cmd.startsWith("FF9A0101")) { // get vendor
                    rpdu = "VX4.NET".getBytes(StandardCharsets.ISO_8859_1);
                } else if (cmd.startsWith("FF9A0103")) { // get product
                    rpdu = "OMAPI-SE".getBytes(StandardCharsets.ISO_8859_1);
                } else if (cmd.startsWith("FF9A010600")) { // get firmware
                    // NOP
                } else if (cmd.startsWith("FF9A010700")) { // get driver
                    // NOP
                } else if (cmd.startsWith("FF9A0401")) {   // GetReaderPACE Capabilities
                    rpdu = new byte[]{ 0x03 };
                } else if (cmd.startsWith("FF9A0402")) {   // EstablishPACEChannel
                    if (tp != plainTP) { // reset transport provider if channel already exists
                        tp = plainTP;
                    }

                    final byte[] miniPACERes = miniPACE();

                    if (miniPACERes == null) {
                        rpdu = new byte[0];
                        sw = 0x6985;
                    } else {
                        //rpdu = miniPACERes;
//                        if(this.lastSW() == 0x9000) { // doesn't work here, because sw is not set, comes from HAL-SE

                        byte[] IDPICC = TLV.get(miniPACERes, (byte) 0x86);
                        byte[] CAR = TLV.get(miniPACERes, (byte) 0x87);

                        StringBuffer sb = new StringBuffer();
                        sb.append("9000"); // SW
                        sb.append(Hex.byteToString(EF_CA.length) + "00"); // len EF_CardAccess
                        sb.append(Hex.toString(EF_CA));
                        sb.append("0E").append(Hex.x(CAR));
                        sb.append("00");
                        sb.append("2000");
                        sb.append(Hex.x(IDPICC));

                        byte[] res = Hex.x(sb.toString());
                        int dataLen = res.length;
                        rpdu = ArrayTool.concat(new byte[]{0, 0, 0, 0, (byte) dataLen, (byte) (dataLen >> 8)}, res);
//                        } else {
//                            rpdu = new byte[]{0x01, 0x00, 0x20, (byte) 0xF0}; // status, little-endian, abort
//                        }

                        sw = (short) 0x9000;
                    }
                } else if (cmd.startsWith("FF9A0403")) {    // DestroyPACEChannel
                    // reset transport provider
                    tp = plainTP;
                    rpdu = new byte[0];
                } else if (cmd.startsWith("FF9A0410")) {    // VerifyPIN / ModifyPIN
                    // NOP
                    rpdu = new byte[0];
                }

                return ArrayTool.concat(rpdu, new byte[]{(byte) (sw >> 8 & 0xFF), (byte) (sw & 0xFF)});
            }

            if ("00A4040C09E80704007F00070302".equals(cmd)) {  // select DF_EID
                // return Hex.x("9000");
            } else if ("00A4000000".equals(cmd)) {    // select MF
                // return Hex.x("9000");
            } else if ("00A40000023F00".equals(cmd)) {    // select MF
                // return Hex.x("9000");
            } else if ("00A4000C023F00".equals(cmd)) {    // select MF
                // return Hex.x("9000");
            } else if ("00A4020C022F00".equals(cmd)) {    // select EF.DIR
                efData = EF_DIR;
                // return Hex.x("9000");
            } else if ("00A4020C022F01".equals(cmd)) {    // select EF.ATR
                efData = EF_ATR;
                // return Hex.x("9000");
            } else if ("00A4020C02011C".equals(cmd)) {    // select EF.CA
                efData = EF_CA;
                // return Hex.x("9000");
            } else if ("00A4020C02011D".equals(cmd)) {    // select EF.CS

                efData = EF_CS;
                // return Hex.x("9000");
            } else if (cmd.startsWith("00B0")) {    // read binary

                int ofs = ((apdu[2] & 0xFF) << 8) + (apdu[3] & 0xFF);
                int len = apdu[4] & 0xFF;
                if (cmd.startsWith("00B09C00")) {
                    efData = EF_CA;
                    ofs = 0;
                }
                len = len == 0 ? 255 : len;
                if (efData.length - ofs < len) {
                    len = efData.length - ofs;
                    sw = 0x6282;
                }

                rpdu = ArrayTool.sub(efData, ofs, len);
            } else if (cmd.startsWith("0022C1A4")) { // MSE PACE, 0022C1A4 12 80 0A 04007F00070202040202830103 84010D
                // NOP for now
            } else {
                System.out.println("TRANSMIT DOWN TO SE: " + Hex.x(apdu));

                if ((apdu[0] & 0x0C) == 0x0C) { // distinguish between PACE transfer and CA-SM transfer
                    if (cmd.startsWith("0CA4040C")) { // escape select DF with proprietary Type 4 APDU
                        apdu[0] ^= (byte) 0xA0;
                        apdu[1] ^= (byte) 0xAA;
                        apdu[2] ^= (byte) 0xAA;
                        apdu[3] ^= (byte) 0xAA;
                    }

                    rpdu = plainTP.transmit(apdu); // CA-SM
                } else {
                    rpdu = tp.transmit(apdu); // transmit with PACE channel
                }

                sw = (short) tp.lastSW();
            }
        } catch (final Exception e) {
            e.printStackTrace();
            return new byte[]{0x6F, (byte) 0xFF};
        }

        return ArrayTool.concat(rpdu, new byte[]{(byte) (sw >> 8 & 0xFF), (byte) (sw & 0xFF)});
    }


    private byte[] miniPACE() {
        if (cbh == null) {
            System.err.println("OMAPI-TP: miniPACE: no callback handler for secret registered.");
        }

        System.out.println("OMAPI-TP: using callback handler: " + cbh.getClass() + " / " + cbh.toString());
        final byte[] ulk = cbh.getSecret();
        if (ulk == null) {
            System.out.println("OMAPI-TP: secret is null, aborting and returning with null.");
            return null;
        }

        System.out.println("OMAPI-TP: got secret: " + Hex.x(ulk));

        try {
            final SecretKeySpec pinKey = new SecretKeySpec(ulk, "AES");

            final Cipher c = Cipher.getInstance("AES/CBC/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, pinKey, new IvParameterSpec(new byte[16]));

            final byte[] hsRandom = new byte[32];
            new SecureRandom().nextBytes(hsRandom);
            byte[] hsRandEnc = c.doFinal(hsRandom, 0, hsRandom.length);
            hsRandEnc = TLV.build(0x7C, TLV.build(0x81, hsRandEnc));

            byte[] plRes = tp.transmit(Hex.fromString(
                    "80CE0000" + Hex.byteToString((byte) hsRandEnc.length) + Hex.toString(hsRandEnc) + "00"));

            // TODO: lastSW != 0x9000 -> error

            plRes = TLV.get(TLV.get(plRes, (byte) 0x7C), (byte) 0x82);

            final byte[] IDPICC = plRes.clone(); // encrypted card random is IDPICC

            System.out.println("PL-RES 1: " + Hex.toString(IDPICC));

            //
            final MessageDigest mdSHA256 = MessageDigest.getInstance("SHA-256");
            c.init(Cipher.DECRYPT_MODE, pinKey, new IvParameterSpec(new byte[16]));
            mdSHA256.update(c.doFinal(plRes));
            plRes = mdSHA256.digest(hsRandom);

            System.out.println("PL-RES 2: " + Hex.toString(IDPICC));

            System.out.println("PACE-light secret2: " + Hex.toString(plRes));

            System.out.println("SE-TP parent: " + tp + " / " + tp.getParent());

            final MessageDigest mdSHA1 = MessageDigest.getInstance("SHA-1");
            final ISOSMTransport sesmTP = new ISOSMTransport(tp);
            sesmTP.setupKeys(KDF(mdSHA1, plRes, 1, 16), KDF(mdSHA1, plRes, 2, 16));
            tp = sesmTP;

            byte[] ceres = tp.transmit(Hex.fromString("80CE0000"));
            ceres = TLV.get(ceres, (byte) 0x7C);

            final List<byte[]> CAReferences = new ArrayList<byte[]>();
            final byte[] CARef = TLV.get(ceres, (byte) 0x87);
            if (CARef != null) {
                CAReferences.add(CARef);
            }
            // CARef = TLV.get(ceres, (byte) 0x88);
            // if (CARef != null) {
            // this.CAReferences.add(CARef);
            // }

            return TLV.concat(TLV.build(0x80, Hex.x("9000")), TLV.build(0x86, IDPICC), TLV.build(0x87, CAReferences.get(0)), Hex.x("9000"));
        } catch (final GeneralSecurityException e) {
            e.printStackTrace();
        }

        return Hex.x("80026985860087006985");
    }


    private byte[] KDF(final MessageDigest md, final byte[] secret, final int counter, final int limit) {
        // Temporary storage for key derivation.
        final ByteBuffer temp = ByteBuffer.allocate(secret.length + (counter != -1 ? 4 : 0));
        temp.order(ByteOrder.BIG_ENDIAN);
        temp.put(secret);
        if (counter != -1) {
            temp.putInt(counter);
        }
        return ArrayTool.sub(md.digest(temp.array()), 0, limit);
    }

    @Override
    public Object getParent() {
        return null;
    }

    @Override
    public byte[] transmit(final byte[] apdu) {
        byte[] rpdu = process(apdu);

        if (rpdu != null && rpdu.length >= 2) {
            lastSW = ((rpdu[rpdu.length - 2] & 0xFF) << 8) + (rpdu[rpdu.length - 1] & 0xFF);
            rpdu = ArrayTool.sub(rpdu, 0, rpdu.length - 2);
        }
        return rpdu;
    }

    @Override
    public int lastSW() {
        return lastSW;
    }

    @Override
    public void close() {
    }


    public interface CallbackHandler {
        byte[] getSecret();
    }
}
