import os
import urllib.request
import zipfile

GRADLE_URL = "https://services.gradle.org/distributions/gradle-8.7-bin.zip"
DEST_DIR = r"C:\Users\Daniel\AppData\Local\Programs"
ZIP_PATH = os.path.join(DEST_DIR, "gradle-8.7.zip")
TARGET_DIR = os.path.join(DEST_DIR, "gradle-8.7")

if not os.path.exists(TARGET_DIR):
    print("Baixando Gradle 8.7...")
    urllib.request.urlretrieve(GRADLE_URL, ZIP_PATH)
    print("Extraindo Gradle...")
    with zipfile.ZipFile(ZIP_PATH, 'r') as z:
        z.extractall(DEST_DIR)
    os.remove(ZIP_PATH)
    print("Gradle 8.7 instalado com sucesso!")
else:
    print("Gradle 8.7 já instalado!")

gradle_bin = os.path.join(TARGET_DIR, "bin", "gradle.bat")
print("Gradle bat:", gradle_bin)
