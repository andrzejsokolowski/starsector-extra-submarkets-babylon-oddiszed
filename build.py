"""Compile the jar, stage the release folder and zip it folder-at-root for the mod manager.

Run from anywhere: python build.py
Needs javac/jar (JDK 17+) on PATH and the game plus the library mods under D:/Games/StarSector.
Writes jars/MM_ExtraSubMarkets.jar, the staging folder ExtraSubmarketsOddisz/ and
ExtraSubmarketsOddisz-<version>.zip in the mod root.
"""
import glob
import os
import re
import shutil
import subprocess
import sys
import zipfile

ROOT = os.path.dirname(os.path.abspath(__file__))
GAME = "D:/Games/StarSector"
MODS = GAME + "/mods"
STAGE_NAME = "ExtraSubmarketsOddisz"
STAGE = os.path.join(ROOT, STAGE_NAME)
JAR = os.path.join(ROOT, "jars", "MM_ExtraSubMarkets.jar")
SHIP = ["mod_info.json", "extra_submarkets_oddisz.version", "icon.png", "LICENSE-ChickenTechShop.txt", "data", "jars", "graphics"]

CORE_JARS = ["starfarer.api.jar", "json.jar", "lwjgl.jar", "lwjgl_util.jar", "log4j-1.2.9.jar"]
# Library mods, by folder name pattern. Exotica is compile-only: its classes are only
# touched after the mod is found enabled (see data.scripts.compat.ExoticaCompat).
LIB_GLOBS = ["MagicLib*/jars/*.jar", "LazyLib*/jars/*.jar", "LunaLib*/jars/*.jar",
             "Nexerelin*/jars/ExerelinCore.jar", "Exotica Technologies*/jars/*.jar"]


def version():
    with open(os.path.join(ROOT, "mod_info.json"), encoding="utf-8") as fh:
        return re.search(r'"version"\s*:\s*"([^"]+)"', fh.read()).group(1)


def classpath():
    jars = [GAME + "/starsector-core/" + j for j in CORE_JARS]
    for pattern in LIB_GLOBS:
        found = sorted(glob.glob(MODS + "/" + pattern))
        if not found:
            raise SystemExit("Missing library: " + pattern)
        jars += found
    return os.pathsep.join(jars)


def compile_jar():
    out = os.path.join(ROOT, "build", "classes")
    if os.path.isdir(out):
        shutil.rmtree(out)
    os.makedirs(out)
    sources = glob.glob(os.path.join(ROOT, "src", "**", "*.java"), recursive=True)
    # -sourcepath keeps javac from compiling the .java files some library jars carry
    result = subprocess.run(["javac", "--release", "17", "-nowarn", "-encoding", "UTF-8", "-d", out,
                             "-sourcepath", os.path.join(ROOT, "src"), "-cp", classpath()] + sources)
    if result.returncode != 0:
        raise SystemExit("javac failed")
    os.makedirs(os.path.dirname(JAR), exist_ok=True)
    if os.path.exists(JAR):
        os.remove(JAR)
    subprocess.run(["jar", "--create", "--file", JAR, "-C", out, "."], check=True)
    print("jar:", JAR, os.path.getsize(JAR), "bytes")


def stage_and_zip():
    if os.path.isdir(STAGE):
        shutil.rmtree(STAGE)
    os.makedirs(STAGE)
    for item in SHIP:
        src = os.path.join(ROOT, item)
        dst = os.path.join(STAGE, item)
        if os.path.isdir(src):
            shutil.copytree(src, dst, ignore=shutil.ignore_patterns("*.bak"))
        else:
            shutil.copy2(src, dst)

    # A UTF-8 BOM breaks every Starsector JSON/CSV; refuse to ship one.
    for dp, _, fn in os.walk(STAGE):
        for f in fn:
            if f.endswith((".json", ".csv", ".version")):
                with open(os.path.join(dp, f), "rb") as fh:
                    if fh.read(3) == b"\xef\xbb\xbf":
                        raise SystemExit("BOM in " + f)

    zip_path = os.path.join(ROOT, "%s-%s.zip" % (STAGE_NAME, version()))
    if os.path.exists(zip_path):
        os.remove(zip_path)
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as z:
        for dp, _, fn in os.walk(STAGE):
            for f in fn:
                p = os.path.join(dp, f)
                z.write(p, os.path.relpath(p, ROOT).replace(os.sep, "/"))
    with zipfile.ZipFile(zip_path) as z:
        print(len(z.namelist()), "files")
    print("zip:", zip_path, os.path.getsize(zip_path), "bytes")


if __name__ == "__main__":
    compile_jar()
    if "--jar-only" not in sys.argv:
        stage_and_zip()
