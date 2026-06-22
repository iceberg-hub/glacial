"""RESP value formatter for human display."""


class RespFormatter:
    """Formats decoded RESP values for terminal output."""

    @staticmethod
    def format(value, indent=0):
        prefix = " " * indent
        if value is None:
            return f"{prefix}(nil)"
        if isinstance(value, list):
            lines = [f"{prefix}{len(value)} items"]
            for i, item in enumerate(value):
                lines.append(f"{prefix}[{i}] {RespFormatter.format(item, indent + 2)}")
            return "\n".join(lines)
        return f"{prefix}{value}"
