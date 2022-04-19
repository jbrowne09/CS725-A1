Note: ensure you start the server before attempting to start the client as the client immediately
tries to open a connection to the server

To setup/start client:
1: Open a command window
2: Navigate to <your-path>\jbro682-Assignment1\Client
3: Enter command: 'javac TCPServer.java'
4: Enter command: 'java TCPServer'

To setup/start server:
1: Open a command window
2: Navigate to <your-path>\jbro682-Assignment1\Server
3: Enter command: 'javac TCPClient.java'
4: Enter command: 'java TCPClient'


TEST CASES BELOW THE DOUBLE LINE:
- C = sent command from client (you enter these)
- R = response from server

----------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------
USER:
- user 5 has no pass or account and so is automatically logged in
- user 50 does not exist
- user 1 has both an account and a password
C: user 5
R: !5 logged in
C: user 50
R: -Invalid user-id, try again
C: user 1
R: +User-id valid, send account and password
----------------------------------------------------------------------------------------------------
ACCT:
- calling acct without registering a user returns '-' message
- user 5 has no pass or account and so acct is not required
- user 21 has no pass and so is logged in upon receiving a correct acct
- user 1 has a pass and so awaits a pass after receiving an acct
C: acct user1
R: -Valid user-id required
C: user 5
R: !5 logged in
C: acct user5
R: !Account not required, logged in
C: user 21
R: +User-id valid, send account and password
C: acct user21
R: !Account valid, logged in
C: user 1
R: +User-id valid, send account and password
C: acct user2
R: +Account valid, send password
----------------------------------------------------------------------------------------------------
PASS:
- calling pass without registering a user returns '-' message
- user 5 has no pass or account so pass is not required
- user 21 has no pass and so awaits an acct
- user 1 has a pass and an account and so awaits an acct
- user 22 has no acct and so is logged in upon receiving a correct pass
C: pass user1
R: -Valid user-id required
C: user 5
R: !5 logged in
C: pass password5
R: !Logged in
C: user 21
R: +User-id valid send account and password
C: pass password21
R: +Send account
C: user 1
R: +User-id valid send account and password
C: pass password1
R: +Send account
C: user 22
R: +User-id valid send account and password
C: pass password22
R: !Logged in
----------------------------------------------------------------------------------------------------
CDIR:
- user must be specified to access this command
- non-existing directory will return a '-' message
- correct directory will reply with '!' update message
- cdir .. goes down one level
- user 1 has an account and password, calling cdir without a pass/acct will leave the update pending
  until an acct and password is received
C: cdir subdir1
R: -Valid user-id required
C: user 5
R: !5 logged in
C: cdir fake
R: -Can't connect to directory because it doesn't exist
C: cdir dir1
R: !Changed working dir to <your-path>\jbro682-Assignment1\Server\baseServerDir\dir1
C: cdir ..
R: !Changed working dir to <your-path>\jbro682-Assignment1\Server\baseServerDir
C: user 1
R: +User-id valid send account and password
C: cdir dir1
R: +directory ok, send account/password
C: acct user2
R: +Account valid, send password
C: pass password1
R: !Changed working dir to <your-path>\jbro682-Assignment1\Server\baseServerDir
----------------------------------------------------------------------------------------------------
//NOTE: for the following commands client must have successfully logged in by
//receiving a ! command after executing one of the previous instructions if this is 
//not the case the user will receive '-Unauthorised user, log in using USER, ACCT and/or PASS'
//Recommend using C: 'user 5' to automatically log in a single command
----------------------------------------------------------------------------------------------------
DONE:
- both the client and the server close the connection, the client fully closes whilst the server goes back to
  listening to the welcomeSocket
C: done
R: +Closing Connection
----------------------------------------------------------------------------------------------------
KILL:
- if file does not exist server returns a '-' message
- if target file is a non-empty directory server returns a '-' message
- if target file is valid it is deleted
- if target file within a directory is valid it is deleted
- Note: attempting to delete TCPServer.java or TCPServer.class or USER-ACCT-PASS.txt will return a 
  '-' message to prevent the user from tampering with server files
C: kill fake.txt
R: -Note deleted because file does not exist
C: kill dir1
R: -Not deleted because directory contains 1 or more file(s)
C: kill serverfile2.txt
R: +serverfile2.txt deleted
C: kill dir1\serverdirfile2.txt
R: +dir1\serverdirfile2.txt deleted
----------------------------------------------------------------------------------------------------
NAME:
- not specifying a file returns a '-' message
- specifying a file that doesn't exist returns a '-' message
- server expects 'TOBE' command after replying '+File exists'
- new file name cannot include directory or path altering features
- new file name cannot contain special characters
- valid file name will result in file being renamed
C: name
R: -Unspecified file
C: name fake.txt
R: -Can't find fake.txt
C: name serverfile1.txt
R: +File exists
C: rename server.txt
R: -Unexpected command format, expected: TOBE <new-file-spec>
C: name serverfile1.txt
R: +File exists
C: tobe newdir\serverfile1.txt
R: -Specified name is invalid; cannot move file outside of existing directory
C: name serverfile1.txt
R: +File exists
C: tobe server:.txt
R: -File wasn't renamed because unknown error; check the new file name is valid
C: name serverfile1.txt
R: +File exists
C: tobe server.txt
R: +serverfile1.txt renamed to server.txt
----------------------------------------------------------------------------------------------------
TYPE:
- unspecified type returns a '-' message
- invalid type returns a '-' message
- A type updates to Ascii mode
- B type updates to Binary mode
- C type updates to Continous mode
- Note: only responses to this command are implemented; changing the type does NOT affect actual \
  transmission method (which is Binary by default)
C: type
R: -Type not valid
C: type F
R: -Type not valid
C: type A
R: +Using Ascii mode
C: type B
R: +Using Binary mode
C: type C
R: +Using Continuous mode
----------------------------------------------------------------------------------------------------
LIST:
- unspecified list format returns '-' message
- list f returns shortened list format (without <directory-path> current directory is used)
- list v returns verbose list format (dates may be different but format will remain the same)
- list on a non-existing directory will return a '-' message
- list with a specified directory will list files corresponding to specified path
- Note: test outputs here assume you have not added/deleted/changed files from the server directories
C: list
R: -Incorrect command format, expected: LIST <F | V> <director-path>
C: list f
R: +\baseServerDir
   dir1
   server.txt
C: list v
R: +\baseServerDir
        <file/directory-name>  <size(int-bytes)>     <last-edit-date>
              clientfile1.txt                  0  02/09/2020 13:47:35
                         dir1                  0  02/09/2020 11:53:39
                   server.txt                 13  01/09/2020 23:24:07
C: list f fakedir
R: -Directory does not exist
C: list f dir1
R: +\baseServerDir\dir1
   serverdirfile1.txt
----------------------------------------------------------------------------------------------------
RETR:
- command without file-spec argument returns a '-' message
- command with invalid file returns a '-' message
- command refering to a directory returns a '-' message
- response to valid file is fize size (in bytes), if user responds with an invalid response a 
  '-' message is returned, if the user responds with 'stop' the retr is aborted (with '+' message)
  if the user responds with 'send' then the file is sent and the client handles the saving and
  provides a message to the user indicating whether the file was successfully saved or not
- Note: Reponses below assume you have not added/changed/deleted files in the server directories and
  run previous command tests
C: retr
R: -Incorrect command format, expected: RETR <file-spec>
C: retr fake.txt
R: -File doesn't exist
C: retr dir1
R: -File-spec provided refers to a directory
C: retr server.txt
R: 13
C: fakecommand
R: -Unexpected response, RETR aborted
C: retr server.txt
R: 13
C: stop
R: +ok, RETR aborted
C: retr server.txt
R: 13
C: send
Client Prints: Client saved file server.txt
----------------------------------------------------------------------------------------------------
STOR:
- stor with missing arguments returns '-' message
- 'stor new' with an existing server-side file returns '-' message
- stor with valid file waits for size command, other sent commands return '-' message
- stor with a valid file followed by size sent from client server returns '+' message
  then client sends the file, 'stor old' will replace the existing file (leaving "client file 1" 
  as the contents), 'stor app' will append to the existing file (leaving 'client file 1client file 1" 
  as the contents; assuming you ran the stor old command first).
- 'stor new' with a non-existing server-side file will create a new file server-side (same behaviour
  for 'stor old' and 'stor app' when the file doesn't already exist server-side)
- Note: attempting to call stor on a directory from the client will result in the client printing an
  error message (same is true if the file doesn't exist on the client), this is because the server
  has no way of knowing whether the bytes the client sends is a valid file or not.
- Note: Responses below assume you have not added/changed/deleted files in the server directories,
  you may wish to look at the .txt files involved before proceeding and after completing commands
  to confirm the file transfer was correct (also assumed that you have run all previous tests up until
  now).
C: stor
R: -Incorrect command format, expected: STOR {NEW | OLD | APP} <file-spec>
C: stor new clientfile1.txt
R: -File exists, but system doesn't support generations
C: stor old clientfile1.txt
R: +Will write over old file
C: fakecommand
R: -Unexpected reponse; expected SIZE <number-of-bytes>, STOR aborted
C: stor app clientfile1.txt
R: +Will append to file
C: fakecommand
R: -Unexpected reponse; expected SIZE <number-of-bytes>, STOR aborted
C: stor old clientfile1.txt
R: +Will write over old file
C: size 13
R: +ok, waiting for file
Client Prints: STOR: Sent 13 bytes
R: +Saved clientfile1.txt
C: stor app clientfile1.txt
R: +Will append to file
C: size 13
R: +ok, waiting for file
Client Prints: STOR: Sent 13 bytes
R: +Saved appended clientfile1.txt
C: stor new clientfile2.txt
R: +File does not exist, will create new file
C: size 13
R: +ok, waiting for file
Client Prints: STOR: Sent 13 bytes
R: +Saved clientfile2.txt