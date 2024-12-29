# HTTP-Proxy-Server-Java
Proxy server, web server implementation for Computer Networks course.

To run we need open 3 command prompts, and make them to projects directory.
After that run web server with this command on a command propmt:

--->   java Server.java <server_port>

And command prompt for running Proxy Server (*giving the same server port*):

--->   java Proxy.java <server_port> 

After running server run this command on the other command prompt to run the client:

-->    java Client.java

After running client, you can give a request three different types:
→Single URI (e.g. 5000)
→Relative Path:  GET /500 HTTP/1.0
→Full URI: GET http://localhost:8080/500 HTTP/1.0

