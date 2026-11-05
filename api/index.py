import sys
import os

# Ensure api directory is in python path
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)

try:
    from sigaa_sync import app, handler
except ImportError:
    from api.sigaa_sync import app, handler
