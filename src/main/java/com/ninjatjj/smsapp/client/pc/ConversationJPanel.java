package com.ninjatjj.smsapp.client.pc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.DocumentFilter;

import com.ninjatjj.smsapp.core.MessageListener;
import com.ninjatjj.smsapp.core.Sms;
import com.ninjatjj.smsapp.core.client.ClientConnectionListener;

public class ConversationJPanel extends JPanel implements
		ClientConnectionListener, MessageListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected static final int SMS_LENGTH = 140;
	// private TextView mTitle;

	private DefaultListModel<String> mConversationArrayAdapter = new DefaultListModel<String>();
	private JList<String> mConversationView = new JList<String>(
			mConversationArrayAdapter);

	private String address;
	private JTextArea mOutEditText;
	private JButton mSendButton;
	private SmsApplicationClient application;
	private JScrollPane jScrollPane;
	private DefaultStyledDocument doc;
	private JLabel remaningLabel = new JLabel();

	public ConversationJPanel(SmsApplicationClient main) {
		// setLayout(new BorderLayout());

		Box box = Box.createVerticalBox();

		application = main;
		application.getClient().addMessageListener(this);
		application.getClient().addClientConnectionListener(this);

		mConversationView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		mConversationView.setCellRenderer(new MyCellRenderer());

		jScrollPane = new JScrollPane(mConversationView);
		jScrollPane.setPreferredSize(new Dimension(350, 430));
		box.add(jScrollPane);

		JPanel panel = new JPanel(new BorderLayout());
		mOutEditText = new JTextArea();
		doc = new DefaultStyledDocument();
		doc.setDocumentFilter(new DocumentFilter() {

			public void insertString(FilterBypass fb, int offs, String str,
					AttributeSet a) throws BadLocationException {

				// This rejects the entire insertion if it would make
				// the contents too long. Another option would be
				// to truncate the inserted string so the contents
				// would be exactly maxCharacters in length.
				if ((fb.getDocument().getLength() + str.length()) <= SMS_LENGTH)
					super.insertString(fb, offs, str, a);
				else
					Toolkit.getDefaultToolkit().beep();
			}

			public void replace(FilterBypass fb, int offs, int length,
					String str, AttributeSet a) throws BadLocationException {
				// This rejects the entire replacement if it would make
				// the contents too long. Another option would be
				// to truncate the replacement string so the contents
				// would be exactly maxCharacters in length.
				if ((fb.getDocument().getLength() + str.length() - length) <= SMS_LENGTH)
					super.replace(fb, offs, length, str, a);
				else
					Toolkit.getDefaultToolkit().beep();
			}
		});
		doc.addDocumentListener(new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent e) {
				updateCount();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				updateCount();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateCount();
			}
		});
		mOutEditText.setDocument(doc);
		mOutEditText.setLineWrap(true);
		mOutEditText.setPreferredSize(new Dimension(300, 80));
		mOutEditText.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {

				if (application.hasUnread(address)) {
					try {
						application.getClient().clearMessageReceived();
					} catch (Exception ioe) {
						ioe.printStackTrace();
					}
				}
				mSendButton.setEnabled(mOutEditText.getText().length() > 0);

				if (mSendButton.isEnabled()) {
					if (e.isControlDown()
							&& e.getKeyCode() == KeyEvent.VK_ENTER) {
						sendMessage();
					}
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}
		});
		mOutEditText.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void focusGained(FocusEvent arg0) {
				if (application.hasUnread(address)) {
					try {
						application.getClient().clearMessageReceived();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		panel.add(mOutEditText, BorderLayout.WEST);
		mSendButton = new JButton("Reply");
		mSendButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				sendMessage();
			}
		});
		mSendButton.setEnabled(false);
		panel.add(mSendButton, BorderLayout.EAST);

		updateCount();
		panel.add(remaningLabel, BorderLayout.SOUTH);

		box.add(panel);
		add(box, BorderLayout.CENTER);
	}

	private void updateCount() {
		remaningLabel.setText((SMS_LENGTH - doc.getLength())
				+ " characters remaining");
	}

	public void setAddress(String address) {
		if (!address.equals(this.address)) {
			this.address = address;

			mConversationArrayAdapter.clear();
			// mConversationView.removeAll();
			// repaint();

			TreeSet<Sms> treeSet = new TreeSet<Sms>(Collections.reverseOrder());
			treeSet.addAll(application.getMessages());
			for (Sms sms : treeSet) {
				messageReceivedImpl(sms, false);
			}
			int lastIndex = mConversationView.getModel().getSize() - 1;
			if (lastIndex >= 0) {
				mConversationView.ensureIndexIsVisible(lastIndex);
			}

			mSendButton.setEnabled(false);
		}
		if (application.hasUnread(address)) {
			try {
				application.getClient().clearMessageReceived();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	@Override
	public void messageRemoved(final Sms sms) {
		if (address != null) {
			if (ContactManager.getDisplayName(sms.getAddress()).equals(
					ContactManager.getDisplayName(address))) {

				javax.swing.SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						mConversationArrayAdapter.removeElement(sms.getPrefix()
								+ sms.getBody() + "\n" + sms.getReceived());
					}
				});
			}
		}
	}

	@Override
	public void messageReceived(final Sms sms) {

		try {
			javax.swing.SwingUtilities.invokeAndWait(new Runnable() {

				@Override
				public void run() {
					messageReceivedImpl(sms, true);
				}
			});
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void messageReceivedImpl(final Sms sms, final boolean newMessage) {
		if (address != null) {
			if (ContactManager.getDisplayName(sms.getAddress()).equals(
					ContactManager.getDisplayName(address))) {
				String element = sms.getPrefix() + sms.getBody() + "\n"
						+ sms.getReceived();
				if (!mConversationArrayAdapter.contains(element)) {
					mConversationArrayAdapter.addElement(element);

					if (newMessage) {
						int lastIndex = mConversationView.getModel().getSize() - 1;
						if (lastIndex >= 0) {
							mConversationView.ensureIndexIsVisible(lastIndex);

						}
					}

					mSendButton.setEnabled(mOutEditText.getText().length() > 0);
				}
			}
		}
	}

	@Override
	public final void clearMessageReceived() {
	}

	public void connectionStateChange(final boolean connected) {
		if (connected) {
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					// mConversationArrayAdapter.clear();
					TreeSet<Sms> treeSet = new TreeSet<Sms>(Collections
							.reverseOrder());
					treeSet.addAll(application.getMessages());
					for (Sms sms : treeSet) {
						messageReceivedImpl(sms, false);
					}
					int lastIndex = mConversationView.getModel().getSize() - 1;
					if (lastIndex >= 0) {
						mConversationView.ensureIndexIsVisible(lastIndex);
					}
				}
			});
		}
	}

	private void sendMessage() {
		if (application.getClient().connected()) {

			String message = mOutEditText.getText();
			if (message.length() > 0) {
				try {
					mSendButton.setEnabled(false);
					application.sendMessage(address, message);
					mOutEditText.setText("");
				} catch (Exception e) {
					AlertBox("Cannot send message", e.getMessage(), false);
				}
			}
		} else {
			AlertBox("Server not connected", "please try again later", false);
		}
	}

	public void AlertBox(String title, String message, final boolean finish) {
		final Dialog dialog = new Dialog(application, title);
		JTextArea area = new JTextArea();
		area.setText(message);
		dialog.add(area);
		JButton ok = new JButton();
		ok.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (finish) {
					System.exit(0);
				} else {
					dialog.setVisible(false);
				}
			}
		});
		dialog.add(ok);
		dialog.setModal(true);
		dialog.setVisible(true);
	}

	public class MyCellRenderer extends DefaultListCellRenderer {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		final JPanel p = new JPanel(new BorderLayout());
		// final JPanel IconPanel = new JPanel(new BorderLayout());
		// final JLabel l = new JLabel("icon"); // <-- this will be an icon
		// instead
		// of a text
		final JLabel lt = new JLabel();

		String pre = "<html><body style='width: 265px;'>";

		MyCellRenderer() {
			// icon
			// IconPanel.add(l, BorderLayout.NORTH);
			// p.add(IconPanel, BorderLayout.WEST);

			p.add(lt, BorderLayout.CENTER);
			// text
		}

		@Override
		public Component getListCellRendererComponent(final JList list,
				final Object value, final int index, final boolean isSelected,
				final boolean hasFocus) {
			final String text = (String) value;
			if (!text.startsWith("<me>")) {
				p.setBackground(Color.WHITE);
			} else {
				p.setBackground(Color.LIGHT_GRAY);
			}
			lt.setText(pre + text.replace("<me>", "&lt;me&gt;"));

			if (isSelected) {
				p.setBackground(Color.RED);
			}

			return p;
		}
	}

	@Override
	public void cannotConnect(String reason) {
		// TODO Auto-generated method stub

	}

	@Override
	public void longTimeToConnect() {
		// TODO Auto-generated method stub

	}

	public String getAddress() {
		return address;
	}
}
