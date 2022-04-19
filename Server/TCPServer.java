//CS-725 Assignment1 jbro682
//TCPServer Implementation
//Author: Jonathan Browne

//Implemented Commands:
//user <user-id>
//acct <acct-name>
//pass <password>
//cdir <directory>
//done
//kill <file-spec>
//name <file-spec>
//type {A | B | C}
//list {F | V} <file-spec> (file-spec is optional)
//retr <file-spec>
//stor {NEW | OLD | APP} <file-spec>


import java.io.*; 
import java.net.*; 
import java.util.*;
import java.nio.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;

class TCPServer { 
    
    public static void main(String argv[]) throws Exception 
    { 
	String clientSentence = ""; 
	String lowerSentence = "";
	String[] fileNameElements;
	int readByte;
	char readChar;
	
	ServerSocket welcomeSocket = new ServerSocket(6789); 
	
		while(true) { 
	    
			//fork a connectionSocket, open data in/output streams
			Socket connectionSocket = welcomeSocket.accept(); 
			
			BufferedInputStream inFromClient = new BufferedInputStream(connectionSocket.getInputStream());
			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream()); 
			
			if (inFromClient != null) {
				String welcomeMessage = "+jbro682 SFTP Service\n\0";
				outToClient.writeBytes(welcomeMessage);
			} else {
				String badMessage = "-jbro682 Bad Connection\n\0";
				outToClient.writeBytes(badMessage);
				connectionSocket.close(); 
				continue;
			}
			
			//setup session variables
			String userId = "";
			String account = "";
			String password = "";
			boolean loggedIn = false;
			File currentDir = new File(System.getProperty("user.dir")+"\\baseServerDir");
			File updateDir = new File(System.getProperty("user.dir")+"\\baseServerDir");
			String tType = "B";
			
			try{
				while (connectionSocket.isConnected()){									
					//Wait for client message, read bytes, compile into full message (unique response characterised by
					//receieved null character)	
					readByte = (int)inFromClient.read();
					readChar = (char)readByte;
					while (readChar != '\0') {
						clientSentence += readChar;
					
						readByte = (int)inFromClient.read();
						readChar = (char)readByte;
					}
					
					lowerSentence = clientSentence.toLowerCase();
					lowerSentence = lowerSentence.replace("\n", "");
					clientSentence = "";
					String[] args = lowerSentence.split(" ");
					String returnMessage = "";
					
					BufferedReader infoReader = new BufferedReader(new FileReader("USER-ACCT-PASS.txt"));
					String readLine = infoReader.readLine();
					
					switch (args[0]) {
						case "user": 
							userId = "";
							account = "";
							password = "";
							loggedIn = false;
							
							if (args.length < 2) {
								returnMessage = "-Unexpected command format, expected: USER <user-id>";
								break;
							}
							
							//attempt to match user-id with those saved in "userInfo" file, if user-id has no corresponding accounts and password
							//automatically log in (no pass/acct is denoted in file by ";;"
							while(readLine != null) {
								String[] userInfo = readLine.split(":");
								if (args[1].equals(userInfo[0])) {
									userId = args[1];
									if (userInfo[1].equals(";;") && userInfo[2].equals(";;")) {
										returnMessage = "!"+args[1]+" logged in";
										loggedIn = true;
									} else {
										returnMessage = "+"+"User-id valid, send account and password";
									}
									break;
								}
								
								readLine = infoReader.readLine();
							}
							
							if (returnMessage.equals("")) {
								returnMessage = "-Invalid user-id, try again";
							}
							break;
						case "acct":
							account = "";
							loggedIn = false;
							
							if (userId.equals("")) {
								returnMessage = "-Valid user-id required";
								break;
							}
							
							//attempt to match account with any specified with user-id, if user-id does not have password and account
							//user is automatically logged in
							while(readLine != null) {
								String[] userInfo = readLine.split(":");		
								if (userInfo[0].equals(userId)) {
									if (userInfo[1].equals(";;")) {
										if (userInfo[2].equals(";;") || !password.equals("")) {
											returnMessage = "!Account not required, logged in";
											loggedIn = true;
										} else {
											returnMessage = "+Account valid, send password";
										}
									} else {
										String[] accounts = userInfo[1].split(",");
										
										for (int i=0; i < accounts.length; i++) {
											if (accounts[i].equals(args[1])) {
												account = accounts[i];
												if (userInfo[2].equals(";;") || !password.equals("")) {
													returnMessage = "!Account valid, logged in";
													loggedIn = true;
												} else {
													returnMessage = "+Account valid, send password";
												}
												break;
											}
										}
									}
									break;
								}
								
								readLine = infoReader.readLine();
							}
							
							if (returnMessage.equals("")) {
								returnMessage = "-Invalid account, try again";
							}
							break;
						case "pass":
							password = "";
							loggedIn = false;
							
							if (userId.equals("")) {
								returnMessage = "-Valid user-id required";
								break;
							}
							
							//attempt to match password with specified user-id, if user-id does not have password and account
							//user is automatically logged in
							while(readLine != null) {
								String[] userInfo = readLine.split(":");		
								if (userInfo[0].equals(userId)) {
									if (userInfo[2].equals(";;")) {
										if (userInfo[1].equals(";;") || !account.equals("")) {
											returnMessage = "!Logged in";
											loggedIn = true;
										} else {
											returnMessage = "+Send account";
										}
									} else {
										if (userInfo[2].equals(args[1])) {
											password = args[1];
											if (userInfo[1].equals(";;") || !account.equals("")) {
												returnMessage = "!Logged in";
												loggedIn = true;
											} else {
												returnMessage = "+Send account";
											}
										}
									}
									break;
								}
								
								readLine = infoReader.readLine();
							}
							
							if (returnMessage.equals("")) {
								returnMessage = "-Wrong password, try again";
							}
						
							break;
						case "done":
							if (!loggedIn) {
								returnMessage = "-Unauthorised user, log in using USER, ACCT and/or PASS";
								break;
							}
							returnMessage = "+Closing Connection";
							outToClient.writeBytes(returnMessage+"\n\0");
							inFromClient.close();
							outToClient.close();
							connectionSocket.close();
							
							break;
						case "cdir":
							if (userId.equals("")) {
								returnMessage = "-Valid user-id required";
								break;
							}
							
							//does not check for user login, sets requested dir to pending update variable, this is updated at end of code
							//once the client is confirmed to be logged in
							if (args.length >= 2) {
								File newDir = new File(currentDir.getCanonicalPath()+"\\"+args[1]);
								if (newDir.exists()) {
									updateDir = newDir;
									if(!loggedIn) {
										returnMessage = "+directory ok, send account/password";
									}
									break;
								}
							}
							returnMessage = "-Can't connect to directory because it doesn't exist";
						
							break;
						case "kill":
							if (!loggedIn) {
								returnMessage = "-Unauthorised user, log in using USER, ACCT and/or PASS";
								break;
							}
							if (args.length >= 2) {
								
								//Prevent deletion of base server files
								fileNameElements = args[1].split("\\\\");
								int index = fileNameElements.length-1;
								if (fileNameElements[index].equals("tcpserver.java") || fileNameElements[index].equals("tcpserver.class") || fileNameElements[index].equals("user-acct-pass.txt")) {
									returnMessage = "-You don't have permission to delete this file";
									break;
								}
								
								//Attempt to delete file as long as it exists and is not a directory containing files
								File delFile = new File(currentDir.getCanonicalPath()+"\\"+args[1]);
								if (delFile.exists()) {
									String[] dirContents = delFile.list();
									if (dirContents == null || dirContents.length < 1) {
										if (delFile.delete()) {
											returnMessage = "+"+args[1]+" deleted";
										} else {
											returnMessage = "-Not deleted because unknown reason";
										}
										break;
									} else {
										returnMessage = "-Not deleted because directory contains 1 or more file(s)";
										break;
									}
								}
							}
							
							returnMessage = "-Not deleted because file does not exist";
							break;
						case "name":
							if (!loggedIn) {
								returnMessage = "-Unauthorised user, log in using USER, ACCT and/or PASS";
								break;
							}
							
							if (args.length >= 2) {
								
								//Prevent name change of base server files
								fileNameElements = args[1].split("\\\\");
								int index = fileNameElements.length-1;
								if (fileNameElements[index].equals("tcpserver.java") || fileNameElements[index].equals("tcpserver.class") || fileNameElements[index].equals("user-acct-pass.txt")) {
									returnMessage = "-You don't have permission to edit this file";
									break;
								}
								
								File originalFile = new File(currentDir.getCanonicalPath()+"\\"+args[1]);
								String original = args[1];
								if (originalFile.exists()) {
									returnMessage = "+File exists";
									outToClient.writeBytes(returnMessage+"\n\0");
									
									//get user response, expects "tobe"
									readByte = (int)inFromClient.read();
									readChar = (char)readByte;
									while (readChar != '\0') {
										clientSentence += readChar;
									
										readByte = (int)inFromClient.read();
										readChar = (char)readByte;
									}
									
									//attempt to change file name as long as command is confirmed to be tobe and contains a second 
									//argument for the <new-file-spec>
									lowerSentence = clientSentence.toLowerCase();
									clientSentence = "";									
									args = lowerSentence.split(" ");							
									if (args.length >= 2) {
										args[1] = args[1].replace("\n", "");
										if (args[0].equals("tobe")) {
											
											//user is not allowed to specify the name as being in another directory, Files.move throws an
											//IOException in this case but this provides more specific feedback
											File renameFile = new File(originalFile.getParent(), args[1]);
											String newParent = renameFile.getParent();
											String oldParent = originalFile.getParent();
											if (!newParent.equals(oldParent)) {
												returnMessage = "-Specified name is invalid; cannot move file outside of existing directory";
												break;
											}
											
											try {
												Path source = Paths.get(originalFile.getCanonicalPath());
												Path target = Paths.get(originalFile.getParent(), args[1]);
												Files.move(source, target);
												returnMessage = "+"+original+" renamed to "+args[1];
											} catch(IOException|InvalidPathException e) {
												returnMessage = "-File wasn't renamed because unknown error; check the new file name is valid";
											}
											break;
										}
									}
									returnMessage = "-Unexpected command format, expected: TOBE <new-file-spec>";
									break;
								} else {
									returnMessage = "-Can't find "+args[1];
									break;
								}
							}
							
							returnMessage = "-Unspecified file";
							
							break;
						case "type":
						//IMPLEMENTATION INCOMPLETE: global variable is updated but server always uses binary mode
							if (!loggedIn) {
								returnMessage = "-Unauthorised user, log in using USER, ACCT and/or PASS";
								break;
							}
							
							if (args.length >= 2) {
								switch (args[1]) {
									case "a":
										returnMessage = "+Using Ascii mode";
										tType = "A";
										break;
									case "b":
										returnMessage = "+Using Binary mode";
										tType = "B";
										break;
									case "c":
										returnMessage = "+Using Continuous mode";
										tType = "C";
										break;
									default:
										returnMessage = "-Type not valid";
										break;
								}
							} else {
								returnMessage = "-Type not valid";
							}
							
							break;
						case "list":
							if (!loggedIn) {
								returnMessage = "-Unauthorised user, log in using USER, ACCT and/or PASS";
								break;
							}
							
							//grab requested list directory
							File reqDir;
							String path = currentDir.getCanonicalPath();
							String[] pathElements = path.split("\\\\");
							returnMessage = "+\\"+pathElements[pathElements.length-1];
							if (args.length == 2 && (args[1].equals("f") || args[1].equals("v"))) {
								reqDir = new File(currentDir.getCanonicalPath());
							} else if (args.length >= 3 && (args[1].equals("f") || args[1].equals("v"))) {
								reqDir = new File(currentDir.getCanonicalPath()+"\\"+args[2]);
								if (reqDir.exists()) {
									returnMessage += "\\"+args[2];
								} else {
									returnMessage = "-Directory does not exist";
									break;
								}
							} else {
								returnMessage = "-Incorrect command format, expected: LIST <F | V> <directory-path>";
								break;
							}
							returnMessage = returnMessage+'\n';
							
							//check to ensure requested list-source is a directory
							String[] dirContents = reqDir.list();
							if (dirContents == null) {
								returnMessage = "-Specified directory is a file";
								break;
							}
							
							//send list of files as single response seperated by '\n' character
							if (args[1].equals("v")) {
								returnMessage += String.format("%30s %17s %20s\n","<file/directory-name>", "<size(in-bytes)>", "<last-edit-date>");
							}
							File listFile;
							SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
							String convertedDate = "";
							String fileName = "";
							for (int i=0;i<dirContents.length;i++) {
								if (args[1].equals("f")) {
									returnMessage += dirContents[i]+'\n';
								} else {
									listFile = new File(reqDir.getCanonicalPath()+"\\"+dirContents[i]);
									convertedDate = sdf.format(new Date(listFile.lastModified()));
									fileName = dirContents[i];
									
									if (i == dirContents.length-1) {
										returnMessage += String.format("%30s %17d %20s", fileName, listFile.length(), convertedDate);
									} else {
										returnMessage += String.format("%30s %17d %20s\n", fileName, listFile.length(), convertedDate);
									}
								}
							}
							
							break;
						case "retr":
							if (!loggedIn) {
								returnMessage = "-Unauthorised user, log in using USER, ACCT and/or PASS";
								break;
							}
							if (args.length >= 2) {
								File retrDir = new File(currentDir.getCanonicalPath()+"\\"+args[1]);
								
								//check for valid non-directory file
								if (retrDir.exists()) {
									String[] retrDirContents = retrDir.list();
									if (retrDirContents == null) {
										byte[] fileBytes = new byte[(int) retrDir.length()];						
										returnMessage = String.valueOf(fileBytes.length);
										
										//send response with number of bytes, wait for user response
										outToClient.writeBytes(returnMessage+"\n\0");
										readByte = (int)inFromClient.read();
										readChar = (char)readByte;
										while (readChar != '\0') {
											clientSentence += readChar;
										
											readByte = (int)inFromClient.read();
											readChar = (char)readByte;
										}
										lowerSentence = clientSentence.toLowerCase();
										lowerSentence = lowerSentence.replace("\n", "");
										clientSentence = "";
										args = lowerSentence.split(" ");
										
										//check for "send" or "stop response, if "send" is received attempt to read the file and transmit
										if (args.length > 0) {
											if (args[0].equals("send")) {
												FileInputStream fileInput = new FileInputStream(retrDir);
												fileInput.read(fileBytes);
												fileInput.close();
												
												outToClient.write(fileBytes, 0, fileBytes.length);
												System.out.println("RETR: Sent "+fileBytes.length+" bytes");
												returnMessage = "";
												break;
											} else if (args[0].equals("stop")) {
												returnMessage = "+ok, RETR aborted";
												break;
											}
										}
										returnMessage = "-Unexpected response, RETR aborted";
										
										break;
									} else {
										returnMessage = "-File-spec provided refers to a directory";
										break;
									}
								}
							} else {
								returnMessage = "-Incorrect command format, expected: RETR <file-spec>";
								break;
							}
							returnMessage = "-File doesn't exist";
							
							break;
						case "stor":
							if (!loggedIn) {
								returnMessage = "-Unauthorised user, log in using USER, ACCT and/or PASS";
								break;
							}
							
							//determing stor type, system does not support generations for "stor new"
							boolean deleteFile = false;
							boolean appendFile = false;
							String fileSpec = "";
							File storDir = new File(currentDir.getCanonicalPath());
							if (args.length >= 3) {
								String[] splitPath = args[2].split("\\\\");
								args[2] = splitPath[splitPath.length-1];
								storDir = new File(currentDir.getCanonicalPath()+"\\"+args[2]);
								fileSpec = args[2];
								
								if (args[1].equals("new")) {
									if (storDir.exists()) {
										returnMessage = "-File exists, but system doesn't support generations";
										break;
									} else {
										returnMessage = "+File does not exist, will create new file";
									}
								} else if (args[1].equals("old")) {
									if (storDir.exists()) {
										returnMessage = "+Will write over old file";
										deleteFile = true;
									} else {
										returnMessage = "+Will create new file";
									}
								} else if (args[1].equals("app")) {
									if (storDir.exists()) {
										returnMessage = "+Will append to file";
										appendFile = true;
									} else {
										returnMessage = "+Will create file";
									}
								} else {
									returnMessage = "Invalid argument, valid arguments are {NEW | OLD | APP}";
									break;
								}
							} else {
								returnMessage = "-Incorrect command format, expected: STOR {NEW | OLD | APP} <file-spec>";
								break;
							}
							
							//send response and wait for user SIZE command
							outToClient.writeBytes(returnMessage+"\n\0");		
							readByte = (int)inFromClient.read();
							readChar = (char)readByte;
							while (readChar != '\0') {
								clientSentence += readChar;
										
								readByte = (int)inFromClient.read();
								readChar = (char)readByte;
							}	
							lowerSentence = clientSentence.toLowerCase();
							lowerSentence = lowerSentence.replace("\n", "");
							clientSentence = "";
							args = lowerSentence.split(" ");
							
							int fileSize = 0;
							
							//arbitray space allowance, this would be set to correct system value in developed system
							int availableSize = 1000000;
							boolean goodSize = false;
							
							//check for response "size" then grab user specified byte-size
							if (args.length >= 2) {
								if (args[0].equals("size")) {
									try {
										fileSize = Integer.parseInt(args[1]);
										
										if (fileSize < availableSize) {
											returnMessage = "+ok, waiting for file";
											goodSize = true;
										} else {
											returnMessage = "-Not enough room, don't sent it";
											break;
										}
										
									} catch (NumberFormatException e) {}
								}
							}
							if (!goodSize) {
								returnMessage = "-Unexpected response; expected SIZE <number-of-bytes>, STOR aborted";
								break;
							}
							
							outToClient.writeBytes(returnMessage+"\n\0");

							//read in bytes from client
							byte[] fileBytes = new byte[fileSize];
							byte[] intBytes = new byte[4];
							for (int i=0; i< fileSize; i++) {
								readByte = (int)inFromClient.read();
								intBytes = ByteBuffer.allocate(4).putInt(readByte).array();
								fileBytes[i] = intBytes[3];
							}			
							
							String storFilePath = storDir.getCanonicalPath();
							
							//append to file or delete existing file for overwrite
							if (appendFile) {
								FileOutputStream aFile = new FileOutputStream(storFilePath, true);
								
								try {
									aFile.write(fileBytes);
									aFile.close();
									returnMessage = "+Saved appended "+fileSpec;
									
								} catch (FileNotFoundException e) {
									returnMessage = "Couldn't append because unknown reason";
									aFile.close();
									
								}
								break;
								
							} else if (deleteFile) {
								if (!storDir.delete()) {
									returnMessage = "-Unable to delete existing file";
									break;
								}
							}
							
							//attempt to save file in new file creation/delete scenarios (append handled above)
							FileOutputStream saveFile = new FileOutputStream(storFilePath);
							try {
								saveFile.write(fileBytes);
								saveFile.close();
								returnMessage = "+Saved "+fileSpec;
							} catch (FileNotFoundException e) {
								returnMessage = "Couldn't save because unknown reason";
								saveFile.close();
							}
							
							break;
						default:
							returnMessage = "-Bad CMD";
							break;
					}
					
					//Check for pending CDIR update, login required
					if (!updateDir.getCanonicalPath().equals(currentDir.getCanonicalPath()) && loggedIn) {
						returnMessage = "!Changed working dir to "+updateDir.getCanonicalPath();
						currentDir = updateDir;
					}
					
					infoReader.close();
					
					//output message to client if returnMessage has been set (all case statements above should leave
					//returnMessage as their final resultant message once they have finished operations)
					if (!returnMessage.equals("")) {
						outToClient.writeBytes(returnMessage+"\n\0");
					}
				}
			} catch (SocketException|NullPointerException e) {
				System.out.println("Client disconnected");
			}
        } 
    } 
} 

