#!/usr/bin/env python3
"""Glacial Redis Lite CLI — send commands to the server via RESP."""

import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from glacial.app import CliApp


def main():
    CliApp().run()


if __name__ == "__main__":
    main()
