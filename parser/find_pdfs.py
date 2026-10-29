import urllib.request
import re
from html.parser import HTMLParser

class LinkParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.links = []
        self.current_tag = None
        self.current_href = None
        self.current_text = ""

    def handle_starttag(self, tag, attrs):
        if tag == "a":
            self.current_tag = "a"
            for k, v in attrs:
                if k == "href":
                    self.current_href = v
            self.current_text = ""

    def handle_data(self, data):
        if self.current_tag == "a":
            self.current_text += data

    def handle_endtag(self, tag):
        if tag == "a":
            if self.current_href and ".pdf" in self.current_href.lower():
                self.links.append((self.current_text.strip(), self.current_href))
            self.current_tag = None
            self.current_href = None
            self.current_text = ""

req = urllib.request.Request(
    'https://ru.unb.br/cardapio-refeitorio/',
    headers={'User-Agent': 'Mozilla/5.0'}
)
with urllib.request.urlopen(req, timeout=15) as resp:
    html = resp.read().decode('utf-8', errors='ignore')

parser = LinkParser()
parser.feed(html)
print(f"Total PDFs encontrados: {len(parser.links)}")
for text, href in parser.links[:10]:
    print(f"- '{text}': {href}")
