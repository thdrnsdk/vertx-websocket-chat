package com.lukeindykiewicz.workspace.vertx.vertxwebsocketchat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.deploy.Verticle;

public class WebsocketChatVertice extends Verticle {

	private static final int PORT = 8080;
	private static final String HTTP_PREFIX = "http://";
	private static final String CHANGE_NAME_COMMAND = "#n";
	private static final String LINK_COMMAND = "#l";
	private static final String APPLICATION_NAME = "/chat";

	public void start() {

		final Map<String, ServerWebSocket> websockets = new HashMap<String, ServerWebSocket>();
		final List<String> history = new ArrayList<String>();

		vertx.createHttpServer().websocketHandler(new Handler<ServerWebSocket>() {
			public void handle(final ServerWebSocket ws) {

				final StringBuilder nameB = new StringBuilder(ws.textHandlerID);

				if (ws.path.equals(APPLICATION_NAME)) {

					if (!websockets.containsKey(ws.textHandlerID)) {
						websockets.put(ws.textHandlerID, ws);
					}

					for (String message : history) {
						ws.writeTextFrame(message);
					}

					ws.dataHandler(new Handler<Buffer>() {
						public void handle(final Buffer data) {
							String message = data.toString();

							if (message.startsWith(CHANGE_NAME_COMMAND)) {
								message = executeChangeNameMessage(ws, nameB, message);
							} else {
								if (message.contains(LINK_COMMAND)) {
									message = createMessageWithLink(message);
								}
								message = prepareMessage(message);
							}

							history.add(message);
							for (Entry<String, ServerWebSocket> wsEntry : websockets.entrySet()) {
								wsEntry.getValue().writeTextFrame(message);
							}

						}

						private String executeChangeNameMessage(final ServerWebSocket ws, final StringBuilder nameB, final String message) {
							String tempMessage = message;
							String oldName = nameB.toString();
							nameB.replace(0, nameB.length(), tempMessage.replace(CHANGE_NAME_COMMAND, "").trim());
							if (oldName.toString().equals(ws.textHandlerID)) {
								tempMessage = prepareInfoMessage(nameB.toString() + " joined the chat.");
							} else {
								tempMessage = prepareInfoMessage(oldName + " changed name to " + nameB.toString());
							}
							return tempMessage;
						}

						private String createMessageWithLink(final String message) {
							String[] parts = message.split(" ");
							String tempMessage = "";
							for (int i = 0; i < parts.length; i++) {
								if (LINK_COMMAND.equals(parts[i])) {
									parts[i] = "<a href=\"";
									if (!parts[i + 1].contains(HTTP_PREFIX)) {
										parts[i + 1] = HTTP_PREFIX + parts[i + 1];
									}
									parts[i + 1] = parts[i + 1] + "\">" + parts[i + 1].substring(HTTP_PREFIX.length()) + "</a>";
								}
								tempMessage += parts[i] + " ";
							}
							return tempMessage.trim();
						}

						private String prepareInfoMessage(final String message) {
							return "<span class=\"chatinfo\">" + message + "</span>";
						}

						private String prepareMessage(final String message) {
							return prepareInfoMessage(nameB.toString() + ": ") + message;
						}
					});
					ws.endHandler(new Handler<Void>() {
						public void handle(final Void event) {
							websockets.remove(ws.textHandlerID);
						}
					});
				} else {
					ws.reject();
				}
			}
		}).requestHandler(new Handler<HttpServerRequest>() {
			public void handle(final HttpServerRequest req) {
				if (req.path.equals("/")) {
					req.response.sendFile("ws.html");
				}
			}
		}).listen(PORT);

	}

}