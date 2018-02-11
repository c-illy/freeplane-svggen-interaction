package org.freeplane.plugin.collaboration.client.ui;

import static java.awt.Dialog.ModalityType.MODELESS;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.freeplane.collaboration.event.batch.ImmutableMapId;
import org.freeplane.collaboration.event.batch.ImmutableUserId;
import org.freeplane.collaboration.event.batch.MapId;
import org.freeplane.collaboration.event.batch.UpdateBlockCompleted;
import org.freeplane.collaboration.event.children.RootNodeIdUpdated;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.map.mindmapmode.MMapModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.plugin.collaboration.client.event.json.Jackson;
import org.freeplane.plugin.collaboration.client.event.json.UpdatesSerializer;
import org.freeplane.plugin.collaboration.client.server.Credentials;
import org.freeplane.plugin.collaboration.client.server.Server;
import org.freeplane.plugin.collaboration.client.server.Subscription;
import org.freeplane.plugin.collaboration.client.session.Session;
import org.freeplane.plugin.collaboration.client.session.SessionController;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class EventStreamDialog {

	SessionController sessionController = new SessionController();
	
	private class MyServer implements Server {
		private final MapId SENDER_MAP_ID =  ImmutableMapId.of("sender");
		private Subscription recieverSubscription;
		
		@Override
		public MapId createNewMap(Credentials credentials, String name) {
			text.setText("");
			return SENDER_MAP_ID;
		}

		@Override
		public UpdateStatus update(Credentials credentials, UpdateBlockCompleted ev) {
			if(ev.mapId().equals(SENDER_MAP_ID)) {
				UpdatesSerializer printer = UpdatesSerializer.of(this::updateTextArea);
				printer.prettyPrint(ev);
			}
			return UpdateStatus.ACCEPTED;
		}

		private void updateTextArea(String addedText) {
			final String oldText = text.getText();
			final String newText = oldText.isEmpty()  ? addedText : oldText + ",\n" + addedText;
			text.setText(newText);
		}

		@Override
		public void subscribe(Subscription subscription) {
			if(subscription.mapId().equals(RECEIVER_MAP_ID)) {
				this.recieverSubscription = subscription;
			}
		}

		void updateReceiver() throws IOException, JsonParseException, JsonMappingException {
			if(recieverSubscription != null) {
				final UpdateBlockCompleted[] updates = Jackson.objectMapper.readValue("[" + text.getText() + "]", UpdateBlockCompleted[].class);
				for(UpdateBlockCompleted u : updates)
					recieverSubscription.consumer().accept(u);
				text.setText("");
			}
		}

		@Override
		public void unsubscribe(Subscription subscription) {
		}
		
	}
	
	MyServer server = new MyServer();
	Credentials credentials = Credentials.of(ImmutableUserId.of("user-id"));
	
	private class Map2Json implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			MMapModel map = (MMapModel) Controller.getCurrentController().getMap();
			if(map.containsExtension(Session.class)) {
				sessionController.stopSession(map.getExtension(Session.class));
			}
			sessionController.startSession(server, credentials, map, "sender-map");
		}
	}

	private final MapId RECEIVER_MAP_ID = ImmutableMapId.of("receiver");
	public class Json2Map implements ActionListener {
		private MMapController mapController;
		public MMapModel map;

		public Json2Map() {
			final ModeController modeController = Controller.getCurrentController()
			    .getModeController(MModeController.MODENAME);
			mapController = (MMapController) modeController.getMapController();
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				if(map == null || ! text.getText().trim().isEmpty() 
						&& Jackson.objectMapper.readValue(text.getText(),UpdateBlockCompleted.class)
							.updateBlock().get(0) instanceof RootNodeIdUpdated) {
					map = (MMapModel) mapController.newMap();
					sessionController.joinSession(server, credentials, map, RECEIVER_MAP_ID);
				}
				server.updateReceiver();
			}
			catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	final private JDialog dialog;
	private JTextArea text;

	public EventStreamDialog(Window owner) {
		super();
		this.dialog = new JDialog(owner, MODELESS);
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.setTitle("freeplane collaboration events");
		text = new JTextArea();
		JScrollPane textPane = new JScrollPane(text, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_ALWAYS);
		text.setColumns(80);
		text.setRows(40);
		Container contentPane = dialog.getContentPane();
		contentPane.add(textPane, BorderLayout.CENTER);
		Box buttons = Box.createVerticalBox();
		JButton map2json = new JButton("map2json");
		map2json.addActionListener(new Map2Json());
		buttons.add(map2json);
		JButton json2map = new JButton("json2map");
		json2map.addActionListener(new Json2Map());
		buttons.add(json2map);
		contentPane.add(buttons, BorderLayout.WEST);
		dialog.pack();
	}

	public void show() {
		dialog.setVisible(true);
	}
}
