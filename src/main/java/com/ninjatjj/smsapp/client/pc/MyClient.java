package com.ninjatjj.smsapp.client.pc;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ninjatjj.smsapp.core.client.Client;

public class MyClient extends Client {

	private AtomicBoolean reconnect = new AtomicBoolean(false);
	private SmsApplicationClient smsApplicationClient;

	public MyClient(SmsApplicationClient smsApplicationClient) {
		this.smsApplicationClient = smsApplicationClient;
		start();
	}

	@Override
	protected synchronized void reconnectImpl() {
		reconnect.set(true);
		notify();
	}

	@Override
	protected synchronized void myConnected() {
		reconnect.set(false);
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
	public void signalStrengthChanged(boolean readBoolean) {
		smsApplicationClient.signalStrengthChanged(readBoolean);
	}
}
