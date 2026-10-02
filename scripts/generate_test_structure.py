"""Generate the empty 5x5x5 vanilla structure used by Forge GameTests (no libraries)."""
from pathlib import Path
import gzip
import struct

def name(value):
    data = value.encode("utf-8")
    return struct.pack(">H", len(data)) + data

def ints(key, values):
    return b"\x09" + name(key) + b"\x03" + struct.pack(">i", len(values)) + b"".join(struct.pack(">i", v) for v in values)

root = b"\x0a\x00\x00"
root += b"\x03" + name("DataVersion") + struct.pack(">i", 2975)
root += ints("size", [5, 5, 5])
root += b"\x09" + name("palette") + b"\x0a" + struct.pack(">i", 1)
root += b"\x08" + name("Name") + name("minecraft:air") + b"\x00"
for key in ("blocks", "entities"):
    root += b"\x09" + name(key) + b"\x0a" + struct.pack(">i", 0)
root += b"\x00"
target = Path(__file__).resolve().parents[1] / "tests/gametest/resources/data/trashbin/structures/empty.nbt"
target.parent.mkdir(parents=True, exist_ok=True)
target.write_bytes(gzip.compress(root, mtime=0))
print("Generated empty GameTest structure.")
