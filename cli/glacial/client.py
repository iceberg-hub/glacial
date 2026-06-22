"""Low-level TCP client for the Glacial Redis Lite server."""

import socket


class RespClient:
    """Manages a TCP connection to the Redis server."""

    def __init__(self, host="localhost", port=6379, timeout=5):
        self._host = host
        self._port = port
        self._timeout = timeout
        self._socket = None

    def connect(self):
        self._socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self._socket.settimeout(self._timeout)
        self._socket.connect((self._host, self._port))

    def send(self, data):
        self._socket.sendall(data)

    def recv(self, n):
        return self._socket.recv(n)

    def close(self):
        if self._socket:
            self._socket.close()
            self._socket = None

    def __enter__(self):
        self.connect()
        return self

    def __exit__(self, *args):
        self.close()
