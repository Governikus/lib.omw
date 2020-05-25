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
/**
 * COPYRIGHT (C) 2010, 2011, 2012, 2013, 2014 AGETO Innovation GmbH
 * <p>
 * Authors Christian Kahlo, Ralf Wondratschek
 * <p>
 * All Rights Reserved.
 * <p>
 * Contact: PersoApp, http://www.persoapp.de
 *
 * @version 1.0, 30.07.2013 13:50:47
 * <p>
 * This file is part of PersoApp.
 * <p>
 * PersoApp is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * PersoApp is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public
 * License along with PersoApp. If not, see
 * <http://www.gnu.org/licenses/>.
 * <p>
 * Diese Datei ist Teil von PersoApp.
 * <p>
 * PersoApp ist Freie Software: Sie können es unter den Bedingungen der
 * GNU Lesser General Public License, wie von der Free Software
 * Foundation, Version 3 der Lizenz oder (nach Ihrer Option) jeder
 * späteren veröffentlichten Version, weiterverbreiten und/oder
 * modifizieren.
 * <p>
 * PersoApp wird in der Hoffnung, dass es nützlich sein wird, aber OHNE
 * JEDE GEWÄHRLEISTUNG, bereitgestellt; sogar ohne die implizite
 * Gewährleistung der MARKTFÄHIGKEIT oder EIGNUNG FÜR EINEN BESTIMMTEN
 * ZWECK. Siehe die GNU Lesser General Public License für weitere
 * Details.
 * <p>
 * Sie sollten eine Kopie der GNU Lesser General Public License
 * zusammen mit diesem Programm erhalten haben. Wenn nicht, siehe
 * <http://www.gnu.org/licenses/>.
 */
package net.vx4.lib.ivid;

/**
 * <p>
 * The <tt>TransportProvider</tt> interface defines basic functionality to be supported by every transport provider.
 * </p>
 *
 * @author Christian Kahlo
 * @author Rico Klimsa - added javadoc comments.
 * @see JSCIOTransport
 * @see ISOSMTransport
 * @see PersoSimTransport
 */
public interface TransportProvider {

    /**
     * Returns the parent of this TransportProvider if applicable.
     *
     * @return parent or null if no parent exists
     */
    Object getParent();


    /**
     * Transmit <em>APDU</em> through this TransportProvider and return response.
     *
     * @param apdu - APDU to be transmitted
     * @return response from card
     */
    byte[] transmit(byte[] apdu);


    // public byte[] transmit(String apdu);

    /**
     * Returns the last received status word.
     *
     * @return status word of last transmitted APDU
     */
    int lastSW();


    /**
     * Closes this transport provider
     */
    void close();
}
