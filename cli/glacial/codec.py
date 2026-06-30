"""RESP protocol encoder and decoder."""

from glacial.error import RespError


class RespCodec:
    """Stateless RESP encoder/decoder."""

    @staticmethod
    def encode(args):
        parts = [f"*{len(args)}\r\n"]
        for arg in args:
            data = arg.encode()
            parts.append(f"${len(data)}\r\n")
            parts.append(arg + "\r\n")
        return "".join(parts).encode()

    @staticmethod
    def decode(recv):
        header = recv(1)
        if not header:
            raise ConnectionError("Connection closed")
        first = header.decode()

        if first == "+":
            return RespCodec._decode_simple_string(recv)
        if first == "-":
            return RespCodec._decode_error(recv)
        if first == ":":
            return RespCodec._decode_integer(recv)
        if first == "$":
            return RespCodec._decode_bulk_string(recv)
        if first == "*":
            return RespCodec._decode_array(recv)
        return f"(unknown) {first}"

    @staticmethod
    def _read_line(recv):
        buf = []
        while True:
            c = recv(1)
            if not c:
                raise ConnectionError("Connection closed")
            buf.append(c)
            if buf[-2:] == [b"\r", b"\n"]:
                return b"".join(buf[:-2]).decode()

    @staticmethod
    def _decode_simple_string(recv):
        return RespCodec._read_line(recv)

    @staticmethod
    def _decode_error(recv):
        return RespError(RespCodec._read_line(recv))

    @staticmethod
    def _decode_integer(recv):
        return int(RespCodec._read_line(recv))

    @staticmethod
    def _decode_bulk_string(recv):
        raw = RespCodec._read_line(recv)
        if raw == "-1":
            return None
        length = int(raw)
        data = bytearray()
        while len(data) < length + 2:
            chunk = recv(length + 2 - len(data))
            if not chunk:
                raise ConnectionError("Connection closed")
            data.extend(chunk)
        return data[:length].decode()

    @staticmethod
    def _decode_array(recv):
        count = int(RespCodec._read_line(recv))
        if count == -1:
            return None
        return [RespCodec.decode(recv) for _ in range(count)]
