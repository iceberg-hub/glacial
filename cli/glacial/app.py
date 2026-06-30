"""CLI application — wires together codec, client, and formatter."""

import argparse
import sys

from glacial.client import RespClient
from glacial.codec import RespCodec
from glacial.error import RespError
from glacial.formatter import RespFormatter


class CliApp:
    """Orchestrates the CLI: parse args, send command, display result."""

    def run(self, argv=None):
        args = self._parse_args(argv)
        try:
            with RespClient(host=args.host, port=args.port) as client:
                client.send(RespCodec.encode(args.command))
                result = RespCodec.decode(client.recv)
        except (ConnectionError, OSError) as e:
            print(f"Could not connect to {args.host}:{args.port} — {e}",
                  file=sys.stderr)
            sys.exit(1)

        if isinstance(result, RespError):
            print(f"(error) {result}", file=sys.stderr)
            sys.exit(1)

        print(RespFormatter.format(result))

    @staticmethod
    def _parse_args(argv):
        parser = argparse.ArgumentParser(description="Glacial Redis CLI")
        parser.add_argument("-H", "--host", default="localhost",
                            help="Server host")
        parser.add_argument("-p", "--port", type=int, default=6379,
                            help="Server port")
        parser.add_argument("command", nargs="+",
                            help="Command and arguments")
        return parser.parse_args(argv)
