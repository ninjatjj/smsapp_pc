package com.ninjatjj.smsapp.client.pc;

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import com.ninjatjj.smsapp.SmsApplication;
import com.ninjatjj.smsapp.core.MessageListener;
import com.ninjatjj.smsapp.core.Sms;
import com.ninjatjj.smsapp.core.client.Client;
import com.ninjatjj.smsapp.core.client.ClientConnectionListener;

public class SmsApplicationClient extends JFrame implements
		ClientConnectionListener, MessageListener, SmsApplication {
	private static final long serialVersionUID = 1L;

	private MyClient myClient = new MyClient(this);

	private static File connectionDetails;

	private Map<String, Set<Sms>> currentNotification = new HashMap<String, Set<Sms>>();

	private ConversationsJPanel conversationsActivity = new ConversationsJPanel(
			this);
	private ConversationJPanel conversationActivity = new ConversationJPanel(
			this);
	private TrayIcon trayIcon;
	private JMenuItem reconnectFile;
	private JMenuItem reconnectPopup;
	private Image connectedImage;
	private Image disconnectedImage;
	private Image newMessageImage;
	private Image noReceptionImage;
	private final JPopupMenu popup = new JPopupMenu();

	private SystemTray tray;

	private boolean phoneConnected;

	private boolean hideOnClose;

	private SmsApplicationClient() throws IOException {
		super("smsapp (not connected)");
		Box mainFrame = Box.createHorizontalBox();
		setSize(600, 600);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // HIDE_ON_CLOSE
		hideOnClose = false;
		setFocusable(true);
		addWindowListener(new WindowListener() {

			@Override
			public void windowClosing(WindowEvent arg0) {
				if (!hideOnClose) {
					SmsApplicationClient.this.setState(Frame.ICONIFIED);
				}
			}

			@Override
			public void windowOpened(WindowEvent arg0) {
			}

			@Override
			public void windowIconified(WindowEvent arg0) {
			}

			@Override
			public void windowDeiconified(WindowEvent arg0) {
				SmsApplicationClient.this.repaint();
			}

			@Override
			public void windowDeactivated(WindowEvent arg0) {
			}

			@Override
			public void windowClosed(WindowEvent arg0) {
			}

			@Override
			public void windowActivated(WindowEvent arg0) {
				clearNewMessageNotification();
			}
		});
		mainFrame.add(conversationsActivity);
		mainFrame.add(conversationActivity);

		myClient.addMessageListener(this);
		myClient.addClientConnectionListener(this);

		if (!SystemTray.isSupported()) {
			System.out.println(new Date() + "SystemTray is not supported");
		} else {
			BufferedImage connectedImg = ImageIO.read(this.getClass()
					.getClassLoader().getResourceAsStream("drawable/icon.png"));
			connectedImage = new ImageIcon(connectedImg).getImage()
					.getScaledInstance(24, 24, 0);
			BufferedImage disconnectedImg = ImageIO.read(this.getClass()
					.getClassLoader()
					.getResourceAsStream("drawable/icon_disconnected.png"));
			disconnectedImage = new ImageIcon(disconnectedImg).getImage()
					.getScaledInstance(24, 24, 0);
			BufferedImage trayImg = ImageIO.read(this.getClass()
					.getClassLoader()
					.getResourceAsStream("drawable/icon_msg.png"));
			newMessageImage = new ImageIcon(trayImg).getImage()
					.getScaledInstance(24, 24, 0);
			BufferedImage phoneDisconnectedImage = ImageIO
					.read(this
							.getClass()
							.getClassLoader()
							.getResourceAsStream(
									"drawable/phoneDisconnectedImage.png"));
			noReceptionImage = new ImageIcon(phoneDisconnectedImage).getImage()
					.getScaledInstance(24, 24, 0);

			final JCheckBoxMenuItem ongoingNotification = new JCheckBoxMenuItem(
					"Notifications?", true);
			ongoingNotification.addItemListener(new ItemListener() {

				public void itemStateChanged(ItemEvent e) {
					if (ongoingNotification.isSelected())
						addTrayIcon();
					else {
						tray.remove(trayIcon);
						trayIcon = null;
					}
				}
			});

			final JCheckBoxMenuItem closeHides = new JCheckBoxMenuItem(
					"Close hides?", false);
			closeHides.addItemListener(new ItemListener() {

				public void itemStateChanged(ItemEvent e) {
					if (closeHides.isSelected()) {
						setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
						hideOnClose = true;
					} else {
						setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
						hideOnClose = false;
					}
				}
			});

			// Create a pop-up menu components

			JMenu fileMenu = new JMenu("File");
			fileMenu.add(ongoingNotification);
			fileMenu.add(closeHides);
			reconnectFile = new ReconnectMenuItem();
			fileMenu.add(reconnectFile);
			fileMenu.add(new ExitMenuItem());
			reconnectPopup = new ReconnectMenuItem();
			popup.add(reconnectPopup);
			popup.add(new ExitMenuItem());

			addTrayIcon();

			JMenuBar menuBar = new JMenuBar();
			menuBar.add(fileMenu);
			setJMenuBar(menuBar);
		}
		JPanel panel = new JPanel();
		InputMap inputMap = panel
				.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inputMap.put(
				KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK),
				"shutdown");
		panel.getActionMap().put("shutdown", new AbstractAction() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		mainFrame.add(panel);
		getContentPane().add(mainFrame);
		// getContentPane().add(panel);
		// pack();
		setVisible(true);
	}

	private void addTrayIcon() {
		trayIcon = new TrayIcon(disconnectedImage, "smsappclient", null);
		processNotification();
		trayIcon.setImageAutoSize(true);
		trayIcon.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub
				System.out.println("mouseReleased");

				if (arg0.isPopupTrigger()
						|| arg0.getButton() == MouseEvent.BUTTON3) {
					popup.setLocation(arg0.getX(),
							arg0.getY() - popup.getHeight());
					popup.setInvoker(popup);
					popup.setVisible(true);
				}

			}
		});
		trayIcon.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("actionPerformed");

				clearNewMessageNotification();
				if (SmsApplicationClient.this.isVisible()) {
					SmsApplicationClient.this.setVisible(false);
				} else {
					SmsApplicationClient.this.setVisible(true);
				}
			}
		});

		trayIcon.setToolTip("smsappclient");

		tray = SystemTray.getSystemTray();
		try {
			tray.add(trayIcon);
		} catch (AWTException e) {
			System.out.println(new Date() + "TrayIcon could not be added.");
			e.printStackTrace();
		}

	}

	private void processNotification() {
		if (!myClient.connected()) {
			trayIcon.setImage(disconnectedImage);
		} else if (currentNotification.size() > 0) {
			trayIcon.setImage(newMessageImage);
		} else {
			if (phoneConnected) {
				trayIcon.setImage(connectedImage);
			} else {
				trayIcon.setImage(noReceptionImage);
			}
		}
	}

	public Client getClient() {
		return myClient;
	}

	private void clearNewMessageNotification() {
		if (hasUnread(conversationActivity.getAddress())) {
			try {
				myClient.clearMessageReceived();
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	@Override
	public final void clearMessageReceived() {
		boolean cleared = currentNotification.size() > 0;
		currentNotification.clear();
		if (cleared) {
			processNotification();
		}
	}

	@Override
	public void messageReceived(Sms sms) {
		if (!sms.isRead() && sms.getPrefix().length() == 0) {
			try {
				playSound(new File("src/main/resources/sound/new-message.wav"));
				Set<Sms> set2 = currentNotification.get(sms.getAddress());
				if (set2 == null) {
					set2 = new TreeSet<Sms>();
					currentNotification.put(sms.getAddress(), set2);
				}
				set2.add(sms);
				processNotification();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void messageRemoved(Sms sms) {
	}

	private static void playSound(File clipFile) throws IOException,
			UnsupportedAudioFileException, LineUnavailableException,
			InterruptedException {
		class AudioListener implements LineListener {
			private boolean done = false;

			@Override
			public synchronized void update(LineEvent event) {
				javax.sound.sampled.LineEvent.Type eventType = event.getType();
				if (eventType == javax.sound.sampled.LineEvent.Type.STOP
						|| eventType == javax.sound.sampled.LineEvent.Type.CLOSE) {
					done = true;
					notifyAll();
				}
			}

			public synchronized void waitUntilDone()
					throws InterruptedException {
				while (!done) {
					wait();
				}
			}
		}
		AudioListener listener = new AudioListener();
		AudioInputStream audioInputStream = AudioSystem
				.getAudioInputStream(clipFile);
		try {
			Clip clip = AudioSystem.getClip();
			clip.addLineListener(listener);
			clip.open(audioInputStream);
			try {
				clip.start();
				listener.waitUntilDone();
			} finally {
				clip.close();
			}
		} finally {
			audioInputStream.close();
		}
	}

	@Override
	public void connectionStateChange(boolean connected) {
		if (connected) {
			reconnectFile.setEnabled(false);
			reconnectPopup.setEnabled(false);
			setTitle("smsapp");
		} else {
			reconnectFile.setEnabled(true);
			reconnectPopup.setEnabled(true);
			setTitle("smsapp (not connected)");
		}
		processNotification();
	}

	public ConversationJPanel getConversationActivity() {
		return conversationActivity;
	}

	@Override
	public void cannotConnect(String reason) {
		// TODO Auto-generated method stub

	}

	@Override
	public void longTimeToConnect() {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteThread(String address) throws IOException {
		myClient.deleteThread(address);
	}

	@Override
	public Set<Sms> getMessages() {
		return myClient.getMessages();
	}

	@Override
	public boolean hasUnread(String address) {
		if (address != null) {
			if (address.startsWith("0")) {
				address = address.substring(1);
			}
			Iterator<String> iterator = currentNotification.keySet().iterator();
			while (iterator.hasNext()) {
				String next = iterator.next();
				String a = next.intern();
				String b = address.intern();
				if (a.endsWith(b)) {
					return true;
				}
			}
		}
		return false;
	}

	public static void main(String[] args) throws Exception {
		SmsApplicationClient smsApplicationClient = new SmsApplicationClient();

		BufferedReader bReader = new BufferedReader(new InputStreamReader(
				System.in));

		connectionDetails = new File("connection.txt");
		// final Object lock = new Object();
		// final Vector<RemoteDevice> devices = new Vector<RemoteDevice>();

		// DiscoveryListener listener = new MyDiscoveryListener();
		// boolean connectionDetailsRequired = true;
		String remoteDeviceName = null;
		String port = null;
		String uuid = null;
		if (connectionDetails.exists()) {
			// connectionDetailsRequired = false;

			BufferedReader fileReader = new BufferedReader(
					new InputStreamReader(
							new FileInputStream(connectionDetails)));
			boolean wireless = fileReader.readLine().startsWith("w");
			remoteDeviceName = fileReader.readLine();
			if (wireless) {
				port = fileReader.readLine();
			}
			uuid = fileReader.readLine();
			fileReader.close();
		}

		if (!connectionDetails.exists()) {

			InetAddress wifiAddress = null;
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						if (inetAddress instanceof Inet4Address) {
							wifiAddress = inetAddress;
						}
					}
				}
			}

			String type = "_workstation._tcp.local.";
			final JmDNS jmdns = JmDNS.create(wifiAddress);
			ServiceListener listener;
			jmdns.addServiceListener(type, listener = new ServiceListener() {
				public void serviceResolved(ServiceEvent ev) {
					System.out.println("Service resolved: "
							+ ev.getInfo().getQualifiedName() + " ip address: "
							+ ev.getInfo().getInetAddresses()[0]);
				}

				public void serviceRemoved(ServiceEvent ev) {
					System.out.println("Service removed: " + ev.getName());
				}

				public void serviceAdded(ServiceEvent event) {
					// Required to force serviceResolved to be
					// called again
					// (after the first search)
					jmdns.requestServiceInfo(event.getType(), event.getName(),
							1);
				}
			});

			// System.out.println(new Date() + " Waiting for 10 seconds");
			// synchronized (jmdns) {
			// jmdns.wait(10000);
			// }
			jmdns.removeServiceListener(type, listener);
			jmdns.close();

			// System.out.println("List of possible servers:");
			// // File file = new File("/proc/net/arp");
			// // int length = (int) file.length();
			// //
			// // FileReader reader = new FileReader(file);
			// // char[] filecontent = new char[length];
			// // for (int offset = 0; offset < length;) {
			// // offset += reader.read(filecontent, offset, length -
			// offset);
			// // }
			// ProcessBuilder pb = new ProcessBuilder("cat",
			// "/proc/net/arp");
			// Process start = pb.start();
			// BufferedReader reader = new BufferedReader(
			// new InputStreamReader(start.getErrorStream()));
			// while (true) {
			// if (1 != 1) {
			// break;
			// }
			// reader.readLine();
			// }
			//
			//
			// reader.close();
			// System.out.println("List complete");

			System.out
					.println("Enter the remote address (press enter for default of 192.168.43.1)");
			remoteDeviceName = bReader.readLine();
			System.out
					.println("Enter the remote port (press enter for default of 8765)");
			port = bReader.readLine();

			System.out
					.println("Enter a unique name for the server to identify you by (enter for a default)");
			uuid = bReader.readLine();

		}
		if (remoteDeviceName.length() == 0) {
			remoteDeviceName = "192.168.43.1";
		}
		if (port.length() == 0) {
			port = "8765";
		}
		if (uuid.length() == 0) {
			uuid = UUID.randomUUID().toString();
		}

		if (!connectionDetails.exists()) {
			connectionDetails.createNewFile();
			FileWriter fw = new FileWriter(connectionDetails);
			fw.append("w" + "\n");
			fw.append(remoteDeviceName + "\n");
			fw.append(port + "\n");
			fw.append(uuid + "\n");
			fw.close();
		}

		smsApplicationClient.getClient().setSocketConnectionDetails(
				remoteDeviceName, port, uuid);
	}

	@Override
	public void sendMessage(String address, String message) throws IOException,
			InterruptedException {
		getClient().sendMessage(address, message);

	}

	public void signalStrengthChanged(boolean connected) {
		System.out.println(new Date() + " Signal strength changed: "
				+ connected);
		this.phoneConnected = connected;
		processNotification();
	}

	private class ExitMenuItem extends JMenuItem {
		public ExitMenuItem() {
			super("Exit");
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					System.exit(0);
				}
			});
		}
	}

	private class ReconnectMenuItem extends JMenuItem {
		public ReconnectMenuItem() {
			super("Reconnect");
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (connectionDetails.exists()) {
						BufferedReader fileReader;
						try {
							fileReader = new BufferedReader(
									new InputStreamReader(new FileInputStream(
											connectionDetails)));
							boolean wireless = fileReader.readLine()
									.startsWith("w");
							String remoteDeviceName = fileReader.readLine();
							if (wireless) {
								String port = fileReader.readLine();
								String uuid = fileReader.readLine();
								fileReader.close();

								myClient.setSocketConnectionDetails(
										remoteDeviceName, port, uuid);
							}
						} catch (FileNotFoundException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					myClient.reconnect();

				}
			});
			setEnabled(!myClient.connected());
		}
	}
}