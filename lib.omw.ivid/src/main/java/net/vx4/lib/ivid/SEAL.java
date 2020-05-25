package net.vx4.lib.ivid;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import com.android.nfc_extras.NfcAdapterExtras;
import com.android.nfc_extras.NfcExecutionEnvironment;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.telephony.IccOpenLogicalChannelResponse;
import android.telephony.TelephonyManager;
import android.util.Log;


/**
 * Secure Element Abstraction Layer
 *
 * @author kahlo, 2020
 * @version $Id$
 */
public class SEAL {
	private static final String LOG_TAG = "SEAL";
	private final Context context;
	private final SEAL delegate;


	/**
	 * TODO comment
	 *
	 * @throws Exception
	 */
	private SEAL() throws Exception {
		this.context = null;
		this.delegate = null;
	}


	/**
	 * TODO comment
	 *
	 * @param context
	 * @throws Exception
	 */
	public SEAL(final Context context) {
		this.context = context;
		this.delegate = new Callable<SEAL>() {
			@Override
			public SEAL call() {
				try { // new OMAPI
					return getOMAPI2SEAL(context);
				} catch (final SecurityException e) {
					log("Binding not allowed, uses-permission SMARTCARD? " + e.getMessage());
					e.printStackTrace();
				} catch (final Exception e) {
					log("Exception: " + e.getMessage());
					e.printStackTrace();
				} catch (final Error e) {
					log("Error: " + e.getMessage());
					e.printStackTrace();
				}

				try { // old OMAPI
					return getOMAPI1SEAL(context);
				} catch (final SecurityException e) {
					log("Binding not allowed, uses-permission SMARTCARD? " + e.getMessage());
					e.printStackTrace();
				} catch (final Exception e) {
					log("Exception: " + e.getMessage());
					e.printStackTrace();
				} catch (final Error e) {
					log("Error: " + e.getMessage());
					e.printStackTrace();
				}

				return null;
			}
		}.call();
	}


	/**
	 * TODO comment
	 *
	 * @author kahlo, 2020
	 * @version $Id$
	 */
	public interface Channel {
		public void close();


		public Object getSession();


		public boolean isOpen();


		public byte[] transmit(final byte[] command);


		public byte[] getSelectResponse();


		public byte[] getATR();


		public String getName();


		public boolean isBasicChannel();
	}


	/**
	 * TODO comment
	 *
	 * @param context
	 * @return
	 * @throws Exception
	 */
	private SEAL getOMAPI1SEAL(final Context context) throws Exception {
		return new SEAL() {
			private final org.simalliance.openmobileapi.SEService seService = new org.simalliance.openmobileapi.SEService(
					context, new org.simalliance.openmobileapi.SEService.CallBack() {
				@Override
				public void serviceConnected(final org.simalliance.openmobileapi.SEService service2) {
					log("connected to OMAPI1: " + seService);

					if (service2 != seService) {
						log("WARNING: service in listener differs: " + service2);
					}
				}
			});


			@Override
			public boolean isConnected() {
				return this.seService != null && this.seService.isConnected();
			}


			@Override
			public void shutdown() {
				if (this.seService != null && this.seService.isConnected()) {
					this.seService.shutdown();
				}
			}


			@Override
			public ChannelTransportProvider open(final String slotName, final String AID) {
				for (final org.simalliance.openmobileapi.Reader reader : this.seService.getReaders()) {
					try {
						log("Reader: " + reader.getName() + " - SE present? " + reader.isSecureElementPresent());
						if (!reader.isSecureElementPresent()) {
							continue;
						}

						final org.simalliance.openmobileapi.Session session = reader.openSession();
						log("ATR: " + toHex(session.getATR()));
						log("Create logical channel to " + AID + " within the session...");

						final org.simalliance.openmobileapi.Channel channel = session.openLogicalChannel(Hex.x(AID));
						log("Channel: " + channel);
						if (channel != null) {
							log("SELECT: " + Hex.x(channel.getSelectResponse()));
							return new ChannelTransportProvider(new Channel() {
								@Override
								public void close() {
									channel.close();
								}


								@Override
								public Object getSession() {
									return channel.getSession();
								}


								@Override
								public boolean isOpen() {
									return !channel.isClosed();
								}


								@Override
								public byte[] transmit(final byte[] command) {
									return channel.transmit(command);
								}


								@Override
								public byte[] getSelectResponse() {
									return channel.getSelectResponse();
								}


								@Override
								public byte[] getATR() {
									return channel.getSession().getATR();
								}


								@Override
								public String getName() {
									return channel.getSession().getReader().getName();
								}


								@Override
								public boolean isBasicChannel() {
									return channel.isBasicChannel();
								}
							});
						} else {
							log("No free logical channel avaiable.");
						}
					} catch (final SecurityException e) { // not allowed
						log("Access not allowed on " + reader.getName() + " for " + AID);
					} catch (final Exception e) {
						log("Error using reader " + reader.getName() + " occured: " + e);
						e.printStackTrace();
					}
					reader.closeSessions();
				}

				this.seService.shutdown();
				return null;
			}
		};
	}


	/**
	 * TODO comment
	 *
	 * @param context
	 * @return
	 * @throws Exception
	 */
	private SEAL getOMAPI2SEAL(final Context context) throws Exception {
		return new SEAL() {
			final android.se.omapi.SEService seService = new android.se.omapi.SEService(context,
					Executors.newSingleThreadExecutor(), new android.se.omapi.SEService.OnConnectedListener() {
				@Override
				public void onConnected() {
					log("connected to OMAPI2: " + seService);
				}
			});


			@Override
			public boolean isConnected() {
				return this.seService != null && this.seService.isConnected();
			}


			@Override
			public void shutdown() {
				if (this.seService != null && this.seService.isConnected()) {
					this.seService.shutdown();
				}
			}


			@Override
			public ChannelTransportProvider open(final String slotName, final String AID) {
				for (final android.se.omapi.Reader reader : this.seService.getReaders()) {
					try {
						log("Reader: " + reader.getName() + " - SE present? " + reader.isSecureElementPresent());
						if (!reader.isSecureElementPresent()) {
							continue;
						}

						final android.se.omapi.Session session = reader.openSession();
						log("ATR: " + toHex(session.getATR()));
						log("Create logical channel to " + AID + " within the session...");

						final android.se.omapi.Channel channel = session.openLogicalChannel(Hex.x(AID));
						log("Channel: " + channel);
						if (channel != null) {
							log("SELECT: " + Hex.x(channel.getSelectResponse()));
							return new ChannelTransportProvider(new Channel() {
								@Override
								public void close() {
									channel.close();
								}


								@Override
								public Object getSession() {
									return channel.getSession();
								}


								@Override
								public boolean isOpen() {
									return channel.isOpen();
								}


								@Override
								public byte[] transmit(final byte[] command) {
									return channel.transmit(command);
								}


								@Override
								public byte[] getSelectResponse() {
									return channel.getSelectResponse();
								}


								@Override
								public byte[] getATR() {
									return channel.getSession().getATR();
								}


								@Override
								public String getName() {
									return channel.getSession().getReader().getName();
								}


								@Override
								public boolean isBasicChannel() {
									return channel.isBasicChannel();
								}
							});
						} else {
							log("No free logical channel avaiable.");
						}
					} catch (final SecurityException e) { // not allowed
						log("Access not allowed on " + reader.getName() + " for " + AID);
					} catch (final Exception e) {
						log("Error using reader " + reader.getName() + " occured: " + e);
						e.printStackTrace();
					}
					reader.closeSessions();
				}

				this.seService.shutdown();
				return null;
			}
		};
	}


	/**
	 * TODO comment
	 *
	 * @param context
	 * @return
	 * @throws Exception
	 */
	private SEAL getTMSEAL(final Context context) throws Exception {
		return new SEAL() {
			final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);


			@Override
			public boolean isConnected() {
				try {
					return this.tm != null && this.tm.hasCarrierPrivileges() && this.tm.hasIccCard();
				} catch (final NoSuchMethodError nsme) {
					nsme.printStackTrace();
				}
				return false;
			}


			@Override
			public void shutdown() {
				//
			}


			@Override
			public ChannelTransportProvider open(final String slotName, final String AID) {
				if (!isConnected()) {
					return null;
				}

				final IccOpenLogicalChannelResponse iccOpenLCres = this.tm.iccOpenLogicalChannel(AID);
				if (iccOpenLCres == null || iccOpenLCres.getStatus() != IccOpenLogicalChannelResponse.STATUS_NO_ERROR) {
					return null;
				}

				return new ChannelTransportProvider(new Channel() {
					boolean open = true;


					@Override
					public void close() {
						this.open = false;
						tm.iccCloseLogicalChannel(iccOpenLCres.getChannel());
					}


					@Override
					public Object getSession() {
						return iccOpenLCres;
					}


					@Override
					public boolean isOpen() {
						return this.open;
					}


					@Override
					public byte[] transmit(final byte[] command) {
						byte[] res = isOpen() ? Hex.x(tm.iccTransmitApduLogicalChannel(iccOpenLCres.getChannel(),
								command[0] & 0xFF, command[1] & 0xFF, command[2] & 0xFF, command[3] & 0xFF,
								command[4] & 0xFF, Hex.x(command, 5, command[4] & 0xFF))) : null;

						while (res != null && res.length >= 2 && res[res.length - 2] == 0x61) {
							res = ArrayTool.concat(ArrayTool.sub(res, 0, res.length - 2),
									Hex.x(tm.iccTransmitApduLogicalChannel(iccOpenLCres.getChannel(),
											0x00, 0xC0, 0x00, 0x00, res[res.length - 1] & 0xFF, "")));
						}

						return res;
					}


					@Override
					public byte[] getSelectResponse() {
						return iccOpenLCres.getSelectResponse();
					}


					@Override
					public byte[] getATR() {
						return null;
					}


					@Override
					public String getName() {
						return "TM-UICC";
					}


					@Override
					public boolean isBasicChannel() {
						return false;
					}
				});
			}
		};
	}


	//
	protected void log(final String msg) {
		Log.i(LOG_TAG, msg);
	}


	protected static String toHex(final byte[] in) {
		return in != null ? Hex.x(in) : "null";
	}


	/**
	 * TODO comment
	 *
	 * @return
	 */
	public boolean isConnected() {
		return this.delegate != null && this.delegate.isConnected();
	}


	/**
	 * TODO comment
	 */
	public void shutdown() {
		if (this.delegate != null) {
			this.delegate.shutdown();
		}
	}


	/**
	 * TODO comment
	 *
	 * @param AID
	 * @return
	 */
	public ChannelTransportProvider open(final String AID) {
		return open(null, AID);
	}


	/**
	 * TODO comment
	 *
	 * @param slotName
	 * @param AID
	 * @return
	 */
	public ChannelTransportProvider open(final String slotName, final String AID) {
		ChannelTransportProvider channel = null;
		if (this.delegate != null) {
			if (!this.delegate.isConnected()) {
				try {
					for (int i = 0; !this.delegate.isConnected() && i < 10; i++) {
						Thread.sleep(100);
					}
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}

			if (this.delegate.isConnected()) {
				channel = this.delegate.open(slotName, AID);
			}
		}
		return channel;
	}


	/**
	 * TODO comment
	 *
	 * @param AID
	 * @return
	 */
	public ChannelTransportProvider openTM(final String AID) {
		SEAL tmSEAL = null;
		try { // Google TelephonyManager API
			tmSEAL = getTMSEAL(this.context);
		} catch (final SecurityException e) {
			log("Binding not allowed, uses-permission READ PHONE STATE? " + e.getMessage());
			e.printStackTrace();
		} catch (final Exception e) {
			log("Exception: " + e.getMessage());
			e.printStackTrace();
		} catch (final Error e) {
			log("Error: " + e.getMessage());
			e.printStackTrace();
		}

		if (tmSEAL != null && tmSEAL.isConnected()) {
			return tmSEAL.open(AID);
		}
		return null;
	}


	/**
	 * TODO comment
	 *
	 * @return
	 */
	public ChannelTransportProvider openNFCEE() {
		try {
			Class.forName("com.android.nfc_extras.NfcAdapterExtras");
		} catch (final Throwable e) {
			log("No NfcAdapterExtras Support");
			return null;
		}

		final NfcAdapterExtras ne = NfcAdapterExtras.get(NfcAdapter.getDefaultAdapter(this.context));
		log("NFC Extras: " + ne);

		try { // XPeria Z1 throws NullPointer Exception from internal stuff
			log("NFC driver: " + ne.getDriverName());
		} catch (final Exception e) {
			e.printStackTrace(); // print and ignore
		}

		final NfcExecutionEnvironment nee = ne.getEmbeddedExecutionEnvironment();
		log("EE: " + nee);
		try {
			nee.open();
			// } catch (final EeIOException e) {
		} catch (final Exception e) { // reduce dependencies for some implementations
			log("EE.open error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}

		return new ChannelTransportProvider(new Channel() {
			private boolean open = true;


			@Override
			public void close() {
				this.open = false;
				try {
					nee.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}


			@Override
			public Object getSession() {
				return nee;
			}


			@Override
			public boolean isOpen() {
				return this.open;
			}


			@Override
			public byte[] transmit(final byte[] command) {
				try {
					return nee.transceive(command);
				} catch (final IOException e) {
					e.printStackTrace();
					return null;
				}
			}


			@Override
			public byte[] getSelectResponse() {
				return null;
			}


			@Override
			public byte[] getATR() {
				return null;
			}


			@Override
			public String getName() {
				return "NFC-EE";
			}


			@Override
			public boolean isBasicChannel() {
				return true;
			}
		});
	}
}
