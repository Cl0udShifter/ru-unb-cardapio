import datetime
import json
import os
import re
import urllib.request
from html.parser import HTMLParser
import pdfplumber

CAMPUS_CONFIGS = [
    {
        "id": "darcy",
        "name": "Darcy Ribeiro",
        "keyword": "darcy",
        "output_file": "menu_darcy.json",
        "fallback_url": "https://ru.unb.br/wp-content/uploads/2026/10/Darcy-Ribeiro-Semana-02-26-10-a-01-11-4.pdf"
    },
    {
        "id": "gama",
        "name": "FGA (Gama)",
        "keyword": "gama",
        "output_file": "menu_gama.json",
        "fallback_url": "https://ru.unb.br/wp-content/uploads/2026/10/Gama-Semana-02-26-10-a-01-11-1.pdf"
    },
    {
        "id": "ceilandia",
        "name": "FCE (Ceilândia)",
        "keyword": "ceilandia",
        "output_file": "menu_ceilandia.json",
        "fallback_url": "https://ru.unb.br/wp-content/uploads/2026/10/Ceilandia-Semana-02-26-10-a-01-11-1.pdf"
    },
    {
        "id": "planaltina",
        "name": "FUP (Planaltina)",
        "keyword": "planaltina",
        "output_file": "menu_planaltina.json",
        "fallback_url": "https://ru.unb.br/wp-content/uploads/2026/10/Planaltina-Semana-02-26-10-a-01-11-1.pdf"
    }
]

class LinkFinder(HTMLParser):
    def __init__(self):
        super().__init__()
        self.all_pdf_links = []
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
            if self._current_href and self._current_href.lower().endswith(".pdf"):
                self.all_pdf_links.append((self._current_text.strip(), self._current_href))
            self._current_tag = None
            self._current_href = None
            self._current_text = ""

def clean_text(txt):
    if not txt:
        return ""
    cleaned = re.sub(r'[\r\n\t]+', ' ', txt)
    cleaned = re.sub(r'\s+', ' ', cleaned).strip()
    return cleaned

def parse_pdf_menu(pdf_path, vigencia_label, pdf_url, campus_name="Darcy Ribeiro"):
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

                items = []
                for day_idx in range(7):
                    c_idx = start_col + day_idx
                    val = row[c_idx] if c_idx < len(row) and row[c_idx] else ""
                    val = clean_text(val)
                    items.append(val)

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
        "campus": campus_name,
        "vigencia": vigencia_label,
        "atualizado_em": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "pdf_url": pdf_url,
        "dias": formatted_dias
    }

def main():
    site_url = "https://ru.unb.br/cardapio-refeitorio/"
    print(f"Buscando links no site: {site_url}")
    req = urllib.request.Request(site_url, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            html = resp.read().decode('utf-8', errors='ignore')
        finder = LinkFinder()
        finder.feed(html)
        scraped_links = finder.all_pdf_links
    except Exception as e:
        print(f"Erro ao acessar site: {e}")
        scraped_links = []

    parser_dir = os.path.dirname(os.path.abspath(__file__))

    results = {}
    for campus in CAMPUS_CONFIGS:
        cid = campus["id"]
        cname = campus["name"]
        ckey = campus["keyword"]
        cfile = campus["output_file"]
        cfallback = campus["fallback_url"]

        matched_link = None
        for txt, href in scraped_links:
            if ckey in href.lower():
                matched_link = (txt, href)
                break

        if matched_link:
            txt, pdf_url = matched_link
            vigencia = txt.replace("ISM –", "").replace("ISM -", "").strip()
            if not vigencia:
                vigencia = "26/10/2026 a 01/11/2026"
        else:
            pdf_url = cfallback
            vigencia = "26/10/2026 a 01/11/2026"

        print(f"Processando {cname} ({vigencia}): {pdf_url}")
        tmp_pdf = os.path.join(parser_dir, f"tmp_{cid}.pdf")
        try:
            urllib.request.urlretrieve(pdf_url, tmp_pdf)
            menu_data = parse_pdf_menu(tmp_pdf, vigencia, pdf_url, campus_name=cname)
            out_path = os.path.join(parser_dir, cfile)
            with open(out_path, "w", encoding="utf-8") as f:
                json.dump(menu_data, f, ensure_ascii=False, indent=2)
            print(f"[OK] Salvo com sucesso: {out_path}")
            results[cid] = menu_data
        except Exception as err:
            print(f"[ERRO] Erro ao processar {cname}: {err}")
        finally:
            if os.path.exists(tmp_pdf):
                try:
                    os.remove(tmp_pdf)
                except Exception:
                    pass

    return results

if __name__ == "__main__":
    main()
