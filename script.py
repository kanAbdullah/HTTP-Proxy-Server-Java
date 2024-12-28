import socket
import threading


# Helper function to generate exact-sized HTML content
def generate_html(size):
    header = "<HTML>\n<HEAD>\n<TITLE>I am {} bytes long</TITLE>\n</HEAD>\n<BODY>".format(size)
    footer = "</BODY>\n</HTML>"
    body_size = size - (len(header) + len(footer) + 2)  # Account for leading and trailing spaces

    # Generate body content
    body_content = "a " * (body_size // 2)
    body_content = body_content.strip()  # Ensure no trailing spaces

    if len(body_content) < body_size:
        body_content += "a"  # Add one 'a' if needed
    elif len(body_content) > body_size:
        body_content = body_content[:body_size]  # Trim excess

    return f"{header} {body_content} {footer}"


# HTTP Server
class HTTPServer:
    def __init__(self, host, port):
        self.host = host
        self.port = port

    def handle_client(self, client_socket):
        try:
            request = client_socket.recv(1024).decode()
            print(f"[SERVER] Received Request:\n{request}")

            if not request:
                return

            # Parse the request
            request_line = request.splitlines()[0]
            method, uri, _ = request_line.split()

            if method != "GET":
                self.send_response(client_socket, "501 Not Implemented", "Only GET is supported.")
                return

            # Extract size from URI
            try:
                if uri == "/":
                    raise ValueError("No size specified.")
                size = int(uri.lstrip("/"))
                if size < 100 or size > 20000:
                    raise ValueError
            except ValueError:
                self.send_response(client_socket, "400 Bad Request", "Invalid or unsupported URI.")
                return

            # Generate HTML content
            content = generate_html(size)
            self.send_response(client_socket, "200 OK", content, "text/html")
        finally:
            client_socket.close()

    def send_response(self, client_socket, status, content, content_type="text/plain"):
        response = f"HTTP/1.0 {status}\r\n" f"Content-Type: {content_type}\r\n" f"Content-Length: {len(content.encode())}\r\n" f"\r\n{content}"
        print(f"[SERVER] Sending Response:\n{response}")
        client_socket.sendall(response.encode())

    def start(self):
        print(f"[SERVER] Starting HTTP server on http://{self.host}:{self.port}")
        server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server.bind((self.host, self.port))
        server.listen(5)
        print("[SERVER] Waiting for connections...")

        while True:
            client_socket, _ = server.accept()
            client_thread = threading.Thread(target=self.handle_client, args=(client_socket,))
            client_thread.start()


# Proxy Server
class ProxyServer:
    def __init__(self, host, port, web_server_port):
        self.host = host
        self.port = port
        self.web_server_port = web_server_port

    def handle_client(self, client_socket):
        try:
            request = client_socket.recv(1024).decode()
            print(f"[PROXY] Received Request:\n{request}")

            if not request:
                return

            # Parse the request
            request_line = request.splitlines()[0]
            method, uri, _ = request_line.split()

            if method != "GET":
                self.send_response(client_socket, "501 Not Implemented", "Only GET is supported.")
                return

            # Check URI length
            try:
                size = int(uri.lstrip("/"))
                if size > 9999:
                    self.send_response(client_socket, "414 Request-URI Too Long", "URI exceeds allowed length.")
                    return
            except ValueError:
                self.send_response(client_socket, "400 Bad Request", "Invalid URI.")
                return

            # Forward request to web server
            server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            try:
                server_socket.connect((self.host, self.web_server_port))
                proxy_request = request.replace(f"http://{self.host}:{self.port}/", "/")
                print(f"[PROXY] Forwarding Request:\n{proxy_request}")
                server_socket.sendall(proxy_request.encode())

                while True:
                    response = server_socket.recv(4096)
                    if not response:
                        break
                    client_socket.sendall(response)
            except ConnectionRefusedError:
                self.send_response(client_socket, "404 Not Found", "Web server is unreachable.")
            finally:
                server_socket.close()
        finally:
            client_socket.close()

    def send_response(self, client_socket, status, content, content_type="text/plain"):
        response = f"HTTP/1.0 {status}\r\n" f"Content-Type: {content_type}\r\n" f"Content-Length: {len(content.encode())}\r\n" f"\r\n{content}"
        print(f"[PROXY] Sending Response:\n{response}")
        client_socket.sendall(response.encode())

    def start(self):
        print(f"[PROXY] Starting proxy server on {self.host}:{self.port}")
        server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server.bind((self.host, self.port))
        server.listen(5)
        print("[PROXY] Waiting for connections...")

        while True:
            client_socket, _ = server.accept()
            client_thread = threading.Thread(target=self.handle_client, args=(client_socket,))
            client_thread.start()


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="HTTP and Proxy Server")
    parser.add_argument("mode", choices=["http", "proxy"], help="Start as HTTP server or Proxy server")
    parser.add_argument("port", type=int, help="Port number for the server")
    parser.add_argument("--web-port", type=int, default=8080, help="Web server port for proxy (default: 8080)")

    args = parser.parse_args()

    if args.mode == "http":
        server = HTTPServer("127.0.0.1", args.port)
        server.start()
    elif args.mode == "proxy":
        server = ProxyServer("127.0.0.1", args.port, args.web_port)
        server.start()
