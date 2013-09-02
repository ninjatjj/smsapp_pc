package com.ninjatjj.smsapp.client.pc;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class RecipientChooserJPanel extends JDialog {

	public RecipientChooserJPanel(final NewMessageJPanel newMessageJPanel) {
		setModal(true);
		Box box = Box.createVerticalBox();

		DefaultListModel<String> mConversationArrayAdapter = new DefaultListModel<String>();
		final JList<String> mConversationView = new JList<String>(
				mConversationArrayAdapter);
		mConversationView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		mConversationView.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					String displayName = mConversationView.getSelectedValue();
					if (displayName != null) {
						newMessageJPanel.setAddress(ContactManager
								.getAddress(displayName));
						dispose();
					}
				}
			}
		});
		List<String> names = new ArrayList<String>();
		Set<Object> displayNames2 = ContactManager.getDisplayNames();
		Iterator<Object> iterator = displayNames2.iterator();
		while (iterator.hasNext()) {
			names.add(iterator.next().toString());
		}
		java.util.Collections.sort(names);
		Iterator<String> displayNames = names.iterator();
		while (displayNames.hasNext()) {
			Object next = displayNames.next();
			mConversationArrayAdapter.addElement(next.toString());
		}
		JScrollPane jScrollPane = new JScrollPane(mConversationView);
		jScrollPane.setPreferredSize(new Dimension(350, 430));
		box.add(jScrollPane);

		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		box.add(cancel);

		add(box, BorderLayout.CENTER);

		pack();
		setVisible(true);
	}

}
