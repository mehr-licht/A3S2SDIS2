package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

/**
 * classe ServerToServerListener
 */
public class ServerToServerListener implements Runnable {
	
	private ServerSocket serverSocket;

	/**
	 * construtor ServerToServerListener
	 * @param socket socket
	 */
	public ServerToServerListener(ServerSocket socket) {
		this.serverSocket = socket;
	}

	/**
	 * run do ServerToServerListener
	 */
	@Override
	public void run() {
		while(true) {
			Socket socket = null;
			
			try
			{
				socket = serverSocket.accept();
			}
			catch (IOException e)
			{
				System.out.println("Erro à espera de um socket para se ligar.");
			}

			print_connected(socket);
		}
	}

	/**
	 * se ligado notifica
	 * @param socket socket
	 */
	private void print_connected(Socket socket) {
		if(socket != null) {
			System.out.println("Outro server ligou-se a mim.");
			Server.add_other_server(socket);
		}
	}

}
