package com.ninjatjj.smsapp.client.pc;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import com.ninjatjj.smsapp.core.client.Client;

public class MyClient extends Client {

	private StreamConnection socket = null;
	private volatile RemoteDevice remoteDevice;
	private AtomicBoolean reconnect = new AtomicBoolean(false);
	private SmsApplicationClient smsApplicationClient;

	public MyClient(SmsApplicationClient smsApplicationClient) {
		this.smsApplicationClient = smsApplicationClient;
		start();
	}

	public synchronized void setRemoteDevice(RemoteDevice remoteDevice,
			String uuid) {
		this.remoteDevice = remoteDevice;
		this.uuid = uuid;
	}

	@Override
	protected synchronized void reconnectImpl() {
		reconnect.set(true);
		notify();
	}

	@Override
	public synchronized void waitToReconnect(long waitTime) {
		if (!reconnect.getAndSet(false)) {
			try {
				wait(waitTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void waitToPing(long timeSinceLastPing) {
		synchronized (this) {
			try {
				wait(timeSinceLastPing);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void wakeUpPingThread(Thread pingThread) {
		synchronized (this) {
			notify();
		}
	}

	@Override
	public synchronized void connectBT() throws Exception {

		class MyDiscoveryListener implements DiscoveryListener {

			private String connectionURL;

			public String getConnectionURL() {
				return connectionURL;
			}

			public void deviceDiscovered(RemoteDevice remoteDevice,
					DeviceClass deviceClass) {
			}

			public void servicesDiscovered(int transID,
					ServiceRecord[] servRecord) {
				if (servRecord != null && servRecord.length > 0) {
					connectionURL = servRecord[0].getConnectionURL(
							ServiceRecord.AUTHENTICATE_ENCRYPT, false);
				}
			}

			public synchronized void serviceSearchCompleted(int transID,
					int respCode) {
				notify();
			}

			public synchronized void inquiryCompleted(int discType) {
				notify();
			}
		}

		UUID[] uuidSet = new UUID[1];
		uuidSet[0] = new UUID("5B92EE2B75AB4C71AF225AF9861D182B", false);

		System.out.println(new Date() + "Searching for service...");
		MyDiscoveryListener myDiscoveryListener = new MyDiscoveryListener();
		LocalDevice
				.getLocalDevice()
				.getDiscoveryAgent()
				.searchServices(null, uuidSet, remoteDevice,
						myDiscoveryListener);

		try {
			synchronized (myDiscoveryListener) {
				myDiscoveryListener.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		String connectionURL = myDiscoveryListener.getConnectionURL();
		if (connectionURL == null) {
			throw new Exception("Device does not support smsapp");
		}

		System.out.println(new Date() + "Connecting to: " + connectionURL);
		socket = (StreamConnection) Connector.open(myDiscoveryListener
				.getConnectionURL());

		// connectImpl(socket.openOutputStream(), socket.openInputStream());
	}

	@Override
	public synchronized void disconnectBT() {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				System.out.println(new Date() + "could not close socket: "
						+ e.getMessage());
			}
			socket = null;
		}
	}

	@Override
	public boolean shouldConnectBT() {
		return remoteDevice != null;
	}

	@Override
	public void signalStrengthChanged(boolean readBoolean) {
		smsApplicationClient.signalStrengthChanged(readBoolean);
	}
}
