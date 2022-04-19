//CS-725 Assignment1 jbro682
//TCPClient Implementation
//Author: Jonathan Browne

//Client-side specific command implementations:
//retr <file-spec>
//stor {NEW | OLD | APP} <file-spec>
//other valid commands are handled fully by TCPServer


import java.io.*; 
import java.net.*; 
import java.nio.*;

class TCPClient { 
    
    public static void main(String argv[]) throws Exception 
    { 
		String ip = "localhost";
		int port = 6789;

		int readByte;
		char readChar;
		String response = "";
		String send = "";
		String[] sendArgs;
		
		String retrFilePath = "";
		String storFilePath = "";
		File storFile;
		
		boolean retrievingFile = false;
		boolean storingFile = false;
		boolean closingConnection = false;
		boolean skipResponse = false;
	
		System.out.println("Connecting to "+ip+":"+port);
		
		try {
			Socket clientSocket = new Socket(ip, port); 
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream()); 
			BufferedInputStream inFromServer = new BufferedInputStream(clientSocket.getInputStream());
			BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
			
			while(clientSocket.isConnected() && !clientSocket.isClosed()) {
				
				//bad connection to server, in/out streams not valid
				if (inFromServer == null || outToServer == null) {
					break;
				}
				
				//Checking for server response, read bytes, compile into response (end of response characterised by
				//receieved null character)	
				if (!skipResponse) {
					readByte = (int)inFromServer.read();
					readChar = (char)readByte;
					while (readChar != '\0') {
						response += readChar;
						
						readByte = (int)inFromServer.read();
						readChar = (char)readByte;
					}
					System.out.println(response);
					
					if (closingConnection) {
						inFromServer.close();
						outToServer.close();
						clientSocket.close();
						break;
					}
				}
				skipResponse = false;
				
				//Check for server response to retr command, if return is integer value it is
				//the file size in bytes
				int fileSize = 0;
				if (retrievingFile) {
					if (!(response.charAt(0) == '-')) {
						try {
							response = response.replace("\n", "");
							fileSize = Integer.parseInt(response);
						} catch (NumberFormatException e) {
							retrievingFile = false;
							System.out.println("received size could not be parsed, sending stop command to server");
							outToServer.writeBytes("stop\n\0"); 
							continue;
						}
					}
				}
				
				//if previous sent command was size and a positive reponse was received send file data
				if (storingFile && response.charAt(0) == '+') {
					storFile = new File(storFilePath);
					byte[] fileBytes = new byte[(int) storFile.length()];	
					FileInputStream fileInput = new FileInputStream(storFile);
					fileInput.read(fileBytes);
					fileInput.close();
												
					outToServer.write(fileBytes, 0, fileBytes.length);
					System.out.println("STOR: Sent "+fileBytes.length+" bytes");
					
					//reset user sendArgs to prevent stor loop (sendArgs[0] = "send" at this point)
					sendArgs = new String[0];
					
				} else {
					//get user command input
					send = inFromUser.readLine();
					sendArgs = send.split(" ");
				}
				storingFile = false;
				response = "";
				
				//check for commands that require client-side action
				if (sendArgs.length > 0) {
					if (sendArgs[0].equals("send") && retrievingFile) {
						outToServer.writeBytes(send+"\n\0"); 
						//read in bytes from server
						byte[] fileBytes = new byte[fileSize];
						byte[] intBytes = new byte[4];
						//File retrFile = new File(retrFilePath);
						FileOutputStream saveFile = new FileOutputStream(retrFilePath);
						for (int i=0; i< fileSize; i++) {
							readByte = (int)inFromServer.read();
							intBytes = ByteBuffer.allocate(4).putInt(readByte).array();
							fileBytes[i] = intBytes[3];
						}
						
						//attempt to save file
						try {
							saveFile.write(fileBytes);
							saveFile.close();
							System.out.println("Client saved file "+retrFilePath);
						} catch (FileNotFoundException e) {
							System.out.println("Client failed to save file "+retrFilePath);
						}
						retrievingFile = false;
						
						//retr command complete, expecting user command next
						skipResponse = true;
						
					} else if (sendArgs[0].equals("size")) {
						outToServer.writeBytes(send+"\n\0"); 
						storingFile = true;
						
					} else if (sendArgs[0].equals("done")) {
						outToServer.writeBytes(send+"\n\0"); 
						closingConnection = true;
						
					} else if (sendArgs[0].equals("retr") && sendArgs.length >= 2) {
						outToServer.writeBytes(send+"\n\0"); 
						retrievingFile = true;
						
						//if specified file comes from a directory, only use file name to save file on the client-side
						//i.e. retr directory1\file.txt - save as file.txt here
						String[] splitPath = sendArgs[1].split("\\\\");
						retrFilePath = splitPath[splitPath.length-1];
						
					} else if (sendArgs[0].equals("stor") && sendArgs.length >= 3) {
						storFilePath = sendArgs[2];
						storFile = new File(storFilePath);
						
						if (!storFile.exists()) {
							System.out.println("Requested file does not exist on client\n");
							skipResponse = true;
						} else {
							String[] contents = storFile.list();
							if (!(contents == null)) {
								System.out.println("Cannot send a directory\n");
								skipResponse = true;
							} else {
								outToServer.writeBytes(send+"\n\0"); 
							}
						}
					} else {
						outToServer.writeBytes(send+"\n\0"); 
						retrievingFile = false;
					}
				}
			}
		} catch (NullPointerException e) {
			System.out.println("Error occurred that caused connection to be lost");
		} catch (SocketException e) {
			System.out.println("Failed/lost connection; is the server running?");
		}
    } 
} 
