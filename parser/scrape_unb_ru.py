import datetime
import json
import os
import re
import urllib.request
from html.parser import HTMLParser
import pdfplumber

class LinkFinder(HTMLParser):
    def __init__(self):
        super().__init__()
        self.darcy_links = []
        self._current_tag = None
        self._current_href = None
        self._current_text = ""

    def handle_starttag(self, tag, attrs):
        if tag == "a":
            self._current_tag = "a"
            for k, v in attrs:
                if k == "href":
                    self._current_href = v
            self._current_text = ""

    def handle_data(self, data):
        if self._current_tag == "a":
            self._current_text += data

    def handle_endtag(self, tag):
        if tag == "a":
            if self._current_href and "darcy" in self._current_href.lower() and self._current_href.lower().endswith(".pdf"):
                self.darcy_links.append((self._current_text.strip(), self._current_href))
            self._current_tag = None
            self._current_href = None
            self._current_text = ""

def clean_text(txt):
    if not txt:
        return ""
    # Normalize multiple whitespaces, newlines, tabs
    cleaned = re.sub(r'[\r\n\t]+', ' ', txt)
    cleaned = re.sub(r'\s+', ' ', cleaned).strip()
    return cleaned

def parse_pdf_menu(pdf_path, vigencia_label, pdf_url):
    days_names = [
        "Segunda-feira",
        "Terça-feira",
        "Quarta-feira",
        "Quinta-feira",
        "Sexta-feira",
        "Sábado",
        "Domingo"
    ]

    with pdfplumber.open(pdf_path) as pdf:
        pages = pdf.pages

        # Extract dates from Page 2 (Almoco)
        p2_table = pages[1].extract_tables()[0]
        date_pattern = re.compile(r'\d{2}/\d{2}/\d{4}')
        dates = []
        for r in p2_table[:3]:
            found = [c.strip() for c in r if c and date_pattern.search(c)]
            if len(found) == 7:
                dates = found
                break

        dias = []
        for i in range(7):
            d_str = dates[i] if i < len(dates) else f"Dia {i+1}"
            dias.append({
                "dia_semana": days_names[i],
                "data": d_str,
                "cafe": {},
                "almoco": {},
                "jantar": {}
            })

        def process_page(table, meal_key):
            # Locate row with "2ª" or "2ª FEIRA"
            header_row_idx = -1
            start_col = -1
            for r_idx, r in enumerate(table[:3]):
                for c_idx, cell in enumerate(r):
                    if cell and ("2ª" in cell or "2 FEIRA" in cell.upper()):
                        header_row_idx = r_idx
                        start_col = c_idx
                        break
                if header_row_idx != -1:
                    break

            cat_col = start_col - 1

            for row in table[header_row_idx + 2:]:
                if not row:
                    continue
                cat_raw = row[cat_col] if cat_col < len(row) and row[cat_col] else ""
                cat_name = clean_text(cat_raw).upper()
                if not cat_name:
                    continue

                # Check if this row is a merged row across all days
                items = []
                for day_idx in range(7):
                    c_idx = start_col + day_idx
                    val = row[c_idx] if c_idx < len(row) and row[c_idx] else ""
                    val = clean_text(val)
                    items.append(val)

                # If only column 0 has text and all others are empty, it's merged across the entire week
                non_empty = [it for it in items if it]
                if len(non_empty) == 1 and items[0]:
                    merged_val = items[0]
                    for day_idx in range(7):
                        dias[day_idx][meal_key][cat_name] = merged_val
                else:
                    for day_idx in range(7):
                        val = items[day_idx]
                        if val:
                            dias[day_idx][meal_key][cat_name] = val

        # Page 1: Cafe
        process_page(pages[0].extract_tables()[0], "cafe")
        # Page 2: Almoco
        process_page(pages[1].extract_tables()[0], "almoco")
        # Page 3: Jantar
        process_page(pages[2].extract_tables()[0], "jantar")

    # Format into friendly standardized keys
    def map_cafe(raw):
        return {
            "bebidas": raw.get("BEBIDAS", "Café, leite e chá"),
            "panificacao": raw.get("PANIFICAÇÃO", "Pães variados"),
            "gordura": raw.get("GORDURA", "Manteiga ou creme vegetal"),
            "opcao_extra": raw.get("OPÇÃO EXTRA", ""),
            "prato_principal": raw.get("COMPLEMENTO PADRÃO", "Padrão"),
            "vegetariano": raw.get("COMPLEMENTO OVOLACTOVEGETARIANO", ""),
            "vegano": raw.get("COMPLEMENTO VEGETARIANO ESTRITO", ""),
            "fruta": raw.get("FRUTA", "")
        }

    def map_almoco_jantar(raw, is_jantar=False):
        res = {
            "salada_1": raw.get("SALADA 1", ""),
            "salada_2": raw.get("SALADA 2", ""),
            "molho": raw.get("MOLHO PARA SALADA", ""),
            "prato_principal": raw.get("PRATO PRINCIPAL PADRÃO", ""),
            "vegetariano": raw.get("PRATO PRINCIPAL OVOLACTOVEGETARIANO", ""),
            "vegano": raw.get("PRATO PRINCIPAL VEGETARIANO ESTRITO", ""),
            "guarnicao": raw.get("GUARNIÇÃO", ""),
            "acompanhamentos": raw.get("ACOMPANHAMENTOS", "Arroz e feijão"),
            "sobremesa": raw.get("SOBREMESA", ""),
            "bebida": raw.get("BEBIDA (REFRESCO DE)", raw.get("BEBIDA", ""))
        }
        if is_jantar:
            res["sopa"] = raw.get("SOPA", "")
            res["torrada"] = raw.get("TORRADA", "")
        return res

    formatted_dias = []
    for d in dias:
        formatted_dias.append({
            "dia_semana": d["dia_semana"],
            "data": d["data"],
            "cafe": map_cafe(d["cafe"]),
            "almoco": map_almoco_jantar(d["almoco"], is_jantar=False),
            "jantar": map_almoco_jantar(d["jantar"], is_jantar=True)
        })

    return {
        "campus": "Darcy Ribeiro",
        "vigencia": vigencia_label,
        "atualizado_em": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "pdf_url": pdf_url,
        "dias": formatted_dias
    }

def main():
    site_url = "https://ru.unb.br/cardapio-refeitorio/"
    print(f"Buscando links no site: {site_url}")
    req = urllib.request.Request(site_url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req, timeout=15) as resp:
        html = resp.read().decode('utf-8', errors='ignore')

    finder = LinkFinder()
    finder.feed(html)
    if not finder.darcy_links:
        print("Nenhum PDF encontrado, usando URL de contingência...")
        pdf_url = "https://ru.unb.br/wp-content/uploads/2026/10/Darcy-Ribeiro-Semana-02-26-10-a-01-11-4.pdf"
        vigencia = "26/10/2026 a 01/11/2026"
    else:
        txt, pdf_url = finder.darcy_links[0]
        vigencia = txt.replace("ISM –", "").replace("ISM -", "").strip()

    print(f"Baixando PDF ({vigencia}): {pdf_url}")
    pdf_tmp = os.path.join(os.path.dirname(__file__), "current_menu.pdf")
    urllib.request.urlretrieve(pdf_url, pdf_tmp)

    result = parse_pdf_menu(pdf_tmp, vigencia, pdf_url)
    
    # Save local copy
    out_file = os.path.join(os.path.dirname(__file__), "menu_darcy.json")
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    print(f"Cardápio extraído com sucesso! Salvo em: {out_file}")
    return result

if __name__ == "__main__":
    main()
