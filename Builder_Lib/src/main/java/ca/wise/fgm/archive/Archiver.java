package ca.wise.fgm.archive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Archiver {
	private String windowsIp;
	private int windowsPort;

	public Archiver(String windowsIp, int windowsPort) {
		this.windowsIp = windowsIp;
		this.windowsPort = windowsPort;
	}

	public String archiveJobZip(String jobname) throws UnknownHostException, IOException {
		Socket socket = new Socket(windowsIp, windowsPort);
		BufferedReader read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter write = new PrintWriter(socket.getOutputStream(), true);
		write.write("EXECUTE,ZIP," + jobname);
		write.write("\n");
		write.flush();
		String line;
		while ((line = read.readLine()) != null) {
			if (line.equals("COMPLETE"))
				break;
			else if (line.equals("NODIR")) {
				socket.close();
				return "Job does not exist";
			}
		}
		write.close();
		read.close();
		socket.close();
		return "";
	}

	public String archiveJobTar(String jobname) throws UnknownHostException, IOException {
		Socket socket = new Socket(windowsIp, windowsPort);
		BufferedReader read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter write = new PrintWriter(socket.getOutputStream(), true);
		write.write("EXECUTE,TAR," + jobname);
		write.write("\n");
		write.flush();
		String line;
		while ((line = read.readLine()) != null) {
			if (line.equals("COMPLETE"))
				break;
			else if (line.equals("NODIR")) {
				socket.close();
				return "Job does not exist";
			}
		}
		write.close();
		read.close();
		socket.close();
		return "";
	}

	public String archiveJobDelete(String jobname) throws UnknownHostException, IOException {
		Socket socket = new Socket(windowsIp, windowsPort);
		BufferedReader read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter write = new PrintWriter(socket.getOutputStream(), true);
		write.write("EXECUTE,DELETE," + jobname);
		write.write("\n");
		write.flush();
		String line;
		while ((line = read.readLine()) != null) {
			if (line.equals("COMPLETE"))
				break;
			else if (line.equals("NODIR")) {
				socket.close();
				return "Job does not exist";
			}
		}
		write.close();
		read.close();
		socket.close();
		return "";
	}
}
