import os
import urllib.request
import zipfile
import subprocess

SDK_ROOT = r"C:\Users\Daniel\AppData\Local\Android\Sdk"
CMDLINE_ZIP_URL = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
ZIP_PATH = os.path.join(SDK_ROOT, "cmdline-tools.zip")
TARGET_DIR = os.path.join(SDK_ROOT, "cmdline-tools", "latest")

os.makedirs(os.path.join(SDK_ROOT, "cmdline-tools"), exist_ok=True)

if not os.path.exists(os.path.join(TARGET_DIR, "bin", "sdkmanager.bat")):
    print(f"Baixando Android Command Line Tools de {CMDLINE_ZIP_URL}...")
    urllib.request.urlretrieve(CMDLINE_ZIP_URL, ZIP_PATH)
    print("Extraindo arquivo...")
    with zipfile.ZipFile(ZIP_PATH, 'r') as zip_ref:
        zip_ref.extractall(os.path.join(SDK_ROOT, "cmdline-tools"))
    
    extracted_folder = os.path.join(SDK_ROOT, "cmdline-tools", "cmdline-tools")
    if os.path.exists(TARGET_DIR):
        import shutil
        shutil.rmtree(TARGET_DIR)
    os.rename(extracted_folder, TARGET_DIR)
    os.remove(ZIP_PATH)
    print("Command Line Tools instalado com sucesso!")
else:
    print("Command Line Tools já instalado!")

sdkmanager = os.path.join(TARGET_DIR, "bin", "sdkmanager.bat")
print(f"sdkmanager disponível em: {sdkmanager}")
