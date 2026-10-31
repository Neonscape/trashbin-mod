"""Check distributables for resources, server/client classes and leaked test code."""
from pathlib import Path
import hashlib
import json
import struct
import zipfile

ROOT = Path(__file__).resolve().parents[1]
TARGETS = [("forge", "1.18.2", 8), ("forge", "1.19.2", 9),
           ("forge", "1.20.1", 15), ("neoforge", "1.21.1", 34)]
properties = dict(line.split("=", 1) for line in (ROOT / "gradle.properties").read_text().splitlines()
                  if "=" in line and not line.startswith("#"))
version = properties["mod_version"]

checksums = []
for loader, mc, pack_format in TARGETS:
    path = ROOT / f"dist/trashbin-{loader}-{mc}-{version}.jar"
    with zipfile.ZipFile(path) as jar:
        names = set(jar.namelist())
        assert not any("/test/" in name for name in names), f"{mc}: test code in release"
        assert not any("/structures/empty.nbt" in name for name in names), f"{mc}: test structure in release"
        assert not any("/structure/empty.nbt" in name for name in names), f"{mc}: test structure in release"
        for name in ["core/ExperienceLedger", "block/TrashBinBlockEntity", "menu/TrashBinMenu", "client/TrashBinScreen"]:
            assert f"io/github/whiteviera/trashbin/{name}.class" in names, f"{mc}: missing {name}"
        metadata_path = "META-INF/neoforge.mods.toml" if loader == "neoforge" else "META-INF/mods.toml"
        metadata = jar.read(metadata_path).decode()
        assert "${" not in metadata and f'versionRange="[{mc}]"' in metadata
        assert f'version="{version}"' in metadata, f"{mc}: version does not match gradle.properties"
        assert 'license="MIT"' in metadata, f"{mc}: incorrect license metadata"
        assert jar.read("LICENSE") == (ROOT / "LICENSE").read_bytes(), f"{mc}: missing or outdated MIT notice"
        main_class = jar.read("io/github/whiteviera/trashbin/TrashBinMod.class")
        assert struct.unpack(">H", main_class[6:8])[0] == (65 if loader == "neoforge" else 61), f"{mc}: incorrect Java target"
        assert json.loads(jar.read("pack.mcmeta"))["pack"]["pack_format"] == pack_format
        states = json.loads(jar.read("assets/trashbin/blockstates/trash_bin.json"))["variants"]
        assert len(states) == 16, f"{mc}: missing block states"
        en = json.loads(jar.read("assets/trashbin/lang/en_us.json"))
        zh = json.loads(jar.read("assets/trashbin/lang/zh_cn.json"))
        assert en.keys() == zh.keys(), f"{mc}: untranslated keys"
        for state in states.values():
            namespace, model = state["model"].split(":")
            assert f"assets/{namespace}/models/{model}.json" in names
        textures = [name for name in names if name.endswith(".png")]
        assert len(textures) == 8
        for texture in textures:
            png = jar.read(texture)
            assert png[:8] == b"\x89PNG\r\n\x1a\n"
            assert struct.unpack(">II", png[16:24]) == ((160, 32) if "/gui/" in texture else (16, 16))
        # Original Minecraft GUI/sounds must be referenced, never bundled into the mod.
        assert not any(name.startswith("assets/minecraft/") for name in names)
        assert not any(name.endswith(".ogg") for name in names)
        if loader == "neoforge":
            assert json.loads(jar.read("pack.mcmeta"))["pack"]["supported_formats"] == [34, 48]
            assert "META-INF/mods.toml" not in names
            assert not any(b"net/minecraftforge/" in jar.read(name) for name in names if name.endswith(".class")), f"{mc}: Forge API reference in NeoForge jar"
            assert "data/minecraft/tags/block/mineable/pickaxe.json" in names
            assert "data/trashbin/loot_table/blocks/trash_bin.json" in names
            recipe = json.loads(jar.read("data/trashbin/recipe/trash_bin.json"))
            assert recipe["result"]["id"] == "trashbin:trash_bin"
            assert "data/trashbin/advancement/recipes/trash_bin.json" in names
            assert not any(name.startswith(("data/trashbin/recipes/", "data/trashbin/loot_tables/", "data/minecraft/tags/blocks/")) for name in names)
        else:
            assert "data/minecraft/tags/blocks/mineable/pickaxe.json" in names
            assert "data/trashbin/loot_tables/blocks/trash_bin.json" in names
    checksum = hashlib.sha256(path.read_bytes()).hexdigest()
    checksums.append(f"{checksum}  {path.name}")
    print(f"OK: {path.name} ({path.stat().st_size:,} bytes)")
(ROOT / "dist/SHA256SUMS.txt").write_text("\n".join(checksums) + "\n", encoding="utf-8", newline="\n")
print("Wrote dist/SHA256SUMS.txt")
