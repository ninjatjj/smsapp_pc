package com.ninjatjj.smsapp.client.pc;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.ninjatjj.smsapp.core.MessageListener;
import com.ninjatjj.smsapp.core.Sms;
import com.ninjatjj.smsapp.core.client.ClientConnectionListener;
import com.ninjatjj.smsapp.core.ui.SmsWrapper;

public class ConversationsJPanel extends JPanel implements
		ClientConnectionListener, MessageListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// private JLabel customTitle = new JLabel();
	private DefaultListModel<SmsWrapper> mConversationArrayAdapter = new DefaultListModel<SmsWrapper>();
	private JList<SmsWrapper> mConversationView = new JList<SmsWrapper>(
			mConversationArrayAdapter);

	private JScrollPane jScrollPane;
	private SmsApplicationClient application;

	public ConversationsJPanel(final SmsApplicationClient main) {
		application = main;
		// setLayout(new BorderLayout());
		Box box = Box.createVerticalBox();

		application.getClient().addMessageListener(this);
		application.getClient().addClientConnectionListener(this);

		JPanel panel = new JPanel();
		JButton newMessageButton = new JButton("New conversation");
		newMessageButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				new NewMessageJPanel(main);

			}

		});
		panel.add(newMessageButton);
		box.add(panel);

		// Button newMessageButton = (Button)
		// findViewById(R.id.newmessage_create);
		// newMessageButton.setOnClickListener(new OnClickListener() {
		// public void onClick(View v) {
		//
		// Intent intent = new Intent(
		// "com.ninjatjj.smsapp.NewMessageActivity");
		// startActivity(intent);
		// }
		// });

		mConversationView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		mConversationView.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger())
					doPop(e);
			}

			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger())
					doPop(e);
			}

			private void doPop(MouseEvent e) {
				JPopupMenu menu = new JPopupMenu();

				JMenuItem hide = new JMenuItem("Delete");
				hide.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						SmsWrapper person = mConversationView
								.getSelectedValue();
						try {
							application.deleteThread(ContactManager
									.getAddress(person.toString()));
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				});
				menu.add(hide);
				menu.show(e.getComponent(), e.getX(), e.getY());
			}
		});

		mConversationView.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void keyReleased(KeyEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void keyPressed(KeyEvent arg0) {
				SmsWrapper displayName = mConversationView.getSelectedValue();
				if (displayName != null) {
					if (arg0.getKeyCode() == KeyEvent.VK_DELETE) {
						try {
							main.deleteThread(displayName.getSms().getAddress());
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}

			}
		});

		mConversationView.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					SmsWrapper displayName = mConversationView
							.getSelectedValue();
					if (displayName != null) {
						main.getConversationActivity().setAddress(
								ContactManager.getAddress(displayName
										.toString()));
					}
				}
			}
		});
		jScrollPane = new JScrollPane(mConversationView);
		jScrollPane.setPreferredSize(new Dimension(200, 485));
		box.add(jScrollPane);

		if (application != null) {
			for (Sms sms : application.getMessages()) {
				messageReceived(sms);
			}
		}

		add(box, BorderLayout.CENTER);
	}

	@Override
	public void connectionStateChange(boolean connected) {
		if (connected) {
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					for (Sms sms : application.getMessages()) {
						messageReceived(sms);
					}
				}
			});
		}
	}

	@Override
	public void messageRemoved(final Sms sms) {
		// We could clean the conversation panel if there are no messages
		final String address = sms.getAddress();

		Set<Sms> messages = application.getMessages();
		Iterator<Sms> iterator = messages.iterator();
		boolean found = false;
		while (iterator.hasNext()) {
			Sms next = iterator.next();
			if (next.getAddress().equals(address)) {
				found = true;
				break;
			}
		}

		if (!found) {
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					SmsWrapper smsWrapper = new SmsWrapper(sms, ContactManager
							.getDisplayName(sms.getAddress()));
					int position = mConversationArrayAdapter
							.indexOf(smsWrapper);
					if (position >= 0) {
						mConversationArrayAdapter.removeElement(smsWrapper);
					}
				}
			});
		}
	}

	@Override
	public void messageReceived(final Sms sms) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				synchronized (mConversationArrayAdapter) {
					SmsWrapper smsWrapper = new SmsWrapper(sms, ContactManager
							.getDisplayName(sms.getAddress()));
					int position = mConversationArrayAdapter
							.indexOf(smsWrapper);
					if (position >= 0) {
						SmsWrapper existingWrapper = mConversationArrayAdapter
								.get(position);
						mConversationArrayAdapter.removeElement(smsWrapper);
						if (existingWrapper.getSms().compareTo(sms) < 0) {
							smsWrapper = existingWrapper;
						}
					}

					int count = mConversationArrayAdapter.size();
					for (int i = 0; i < count; i++) {
						Sms sms2 = mConversationArrayAdapter.get(i).getSms();
						if (smsWrapper.getSms().compareTo(sms2) < 0) {
							mConversationArrayAdapter.insertElementAt(
									smsWrapper, i);
							break;
						}
					}
					if (mConversationArrayAdapter.indexOf(smsWrapper) < 0) {
						mConversationArrayAdapter.addElement(smsWrapper);
					}
				}
			}
		});

	}

	@Override
	public final void clearMessageReceived() {
	}

	@Override
	public void cannotConnect(String reason) {
	}

	@Override
	public void longTimeToConnect() {
	}
}