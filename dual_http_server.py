import http.server
import socket
import ssl


ROOT = "/workspaces/gitcards"
PORT = 3000
CERT = "/tmp/gitcards-http-server.crt"
KEY = "/tmp/gitcards-http-server.key"


class Handler(http.server.SimpleHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=ROOT, **kwargs)


class DualStackServer(http.server.ThreadingHTTPServer):
    address_family = socket.AF_INET6

    def server_bind(self):
        self.socket.setsockopt(socket.IPPROTO_IPV6, socket.IPV6_V6ONLY, 0)
        super().server_bind()


class DualProtocolServer(DualStackServer):
    def __init__(self, address, handler):
        super().__init__(address, handler)
        self.tls_context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        self.tls_context.load_cert_chain(CERT, KEY)

    def get_request(self):
        sock, address = self.socket.accept()
        sock.settimeout(2)
        try:
            first_byte = sock.recv(1, socket.MSG_PEEK)
            if first_byte and first_byte[0] == 22:
                sock = self.tls_context.wrap_socket(sock, server_side=True)
        except Exception:
            sock.close()
            raise
        return sock, address


server = DualProtocolServer(("::", PORT), Handler)
print(f"Serving HTTP and HTTPS on [::]:{PORT}", flush=True)
server.serve_forever()
