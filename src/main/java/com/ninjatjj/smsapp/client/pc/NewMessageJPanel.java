package com.ninjatjj.smsapp.client.pc;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class NewMessageJPanel extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// protected static final int CONTACT_PICKER_RESULT = 1001;
	// private TextView.OnEditorActionListener mWriteListener = new
	// TextView.OnEditorActionListener() {
	// public boolean onEditorAction(TextView view, int actionId,
	// KeyEvent event) {
	// if (actionId == EditorInfo.IME_NULL
	// && event.getAction() == KeyEvent.ACTION_UP) {
	// String message = view.getText().toString();
	// sendMessage(message);
	// }
	// return true;
	// }
	// };
	private JTextField address = new JTextField();
	private JButton mSendButton = new JButton("Send");
	private JTextField mOutEditText = new JTextField();
	private SmsApplicationClient application;

	public NewMessageJPanel(SmsApplicationClient main) {
		super(main, "New Conversation");
		application = main;

		Box box = Box.createVerticalBox();
		// setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout());
		setModal(true);
		KeyListener keyListener = new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
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
		};

		JButton setRecipient = new JButton("Set Recipient");
		setRecipient.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				new RecipientChooserJPanel(NewMessageJPanel.this);

			}
		});
		box.add(setRecipient);

		address.setPreferredSize(new Dimension(250, 20));
		address.addKeyListener(keyListener);
		JPanel to = new JPanel();
		to.add(new JLabel("To:"));
		to.add(address);
		box.add(to);

		mOutEditText.setPreferredSize(new Dimension(250, 20));
		mOutEditText.addKeyListener(keyListener);
		JPanel content = new JPanel();
		content.add(new JLabel("Message:"));
		content.add(mOutEditText);
		box.add(content);

		mSendButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				sendMessage();
			}
		});
		mSendButton.setEnabled(false);

		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		JPanel buttons = new JPanel();
		buttons.add(mSendButton);
		buttons.add(cancel);
		box.add(buttons);
		add(box, BorderLayout.CENTER);
		pack();

		setVisible(true);

	}

	private void sendMessage() {
		String message = mOutEditText.getText();
		if (application.getClient().connected()) {
			if (message.length() > 0) {
				try {
					mSendButton.setEnabled(false);
					application.getClient().sendMessage(address.getText(),
							message);
					dispose();
				} catch (Exception e) {
					AlertBox("Cannot send message", e.getMessage(), false);
				}
			}
		} else {
			AlertBox("Server not connected", "please try again later", false);
		}
	}

	public void AlertBox(String title, String message, final boolean finish) {
		final Dialog dialog = new Dialog(this, title);
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

	public void setAddress(final String address2) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				address.setText(address2);
			}
		});
	}
}
