package ca.wise.main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import ca.wise.main.ConnectionHandler.ConnectionHandlerEventListener;
import ca.wise.fgm.tools.WISELogger;

public class Listener implements ConnectionHandlerEventListener {
	private ServerSocket m_server;
	private List<ConnectionHandler> m_handlers;

	public Listener() {
		m_handlers = new ArrayList<ConnectionHandler>();
	}

	public void start() {
		WISELogger.info("Starting server on " + CommandLine.get().getLocalPort());
		try
		{
			m_server = new ServerSocket(CommandLine.get().getLocalPort());
			for (;;) {
				Socket clientSocket = m_server.accept();
				ConnectionHandler handler = new ConnectionHandler(clientSocket, this);
				m_handlers.add(handler);
				new Thread(handler).start();
			}
		}
		catch (SocketException e) {
			if (e.getMessage().equals("socket closed"))
			WISELogger.info("Server socket terminated");
			else
				e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				m_server.close();
			} catch (IOException e) {
			}
		}
		WISELogger.info("Waiting for " + m_handlers.size() + " threads");
		WISELogger.info("Server shutdown");
	}

	@Override
	public void closed(ConnectionHandler handler) {
		m_handlers.remove(handler);
	}

	@Override
	public boolean adminMessage(Admin message) {
		boolean retval = false;
		if (message.kill) {
			for (ConnectionHandler h : m_handlers)
				h.terminate();
			try {
				m_server.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			retval = true;
		}
		else if (message.reloadDefaults) {
			Main.reloadDefaults();
			retval = true;
		}
		return retval;
	}
}
