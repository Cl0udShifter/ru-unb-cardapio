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

def select_active_pdf(links, today=None):
    if today is None:
        now_utc = datetime.datetime.now(datetime.timezone.utc)
        today = (now_utc - datetime.timedelta(hours=3)).date()

    date_pat = re.compile(r'(\d{2})/(\d{2})/(\d{4})')
    parsed = []
    for txt, href in links:
        matches = date_pat.findall(txt + " " + href)
        if len(matches) >= 2:
            try:
                d_start = datetime.date(int(matches[0][2]), int(matches[0][1]), int(matches[0][0]))
                d_end = datetime.date(int(matches[1][2]), int(matches[1][1]), int(matches[1][0]))
                parsed.append((d_start, d_end, txt, href))
            except Exception:
                pass

    if not parsed:
        return links[0] if links else (None, None)

    # 1. Prioriza o PDF da semana vigente (d_start <= today <= d_end)
    for d_start, d_end, txt, href in parsed:
        if d_start <= today <= d_end:
            return (txt, href)

    # 2. Se a data atual ainda for anterior ao início da próxima semana, busca o PDF mais recente
    past_links = [l for l in parsed if l[1] <= today]
    if past_links:
        past_links.sort(key=lambda x: x[1], reverse=True)
        return (past_links[0][2], past_links[0][3])

    # 3. Fallback: o PDF mais próximo no futuro
    future_links = [l for l in parsed if l[0] > today]
    if future_links:
        future_links.sort(key=lambda x: x[0])
        return (future_links[0][2], future_links[0][3])

    return (parsed[0][2], parsed[0][3])

def parse_pdf_menu(pdf_path, vigencia_label, pdf_url, campus_name="Darcy Ribeiro", campus_id="darcy"):
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

        # Extração de datas da tabela
        date_pattern = re.compile(r'\d{2}/\d{2}/\d{4}')
        extracted_dates = []
        for p in pages:
            tables = p.extract_tables()
            if not tables:
                continue
            for r in tables[0][:4]:
                found = [c.strip() for c in r if c and date_pattern.search(c)]
                if len(found) >= 3:
                    extracted_dates = found
                    break
            if extracted_dates:
                break

        # Reconstituição das 7 datas completas da semana (Segunda a Domingo)
        full_dates = []
        if extracted_dates:
            try:
                base_d = datetime.datetime.strptime(extracted_dates[0], "%d/%m/%Y").date()
                for i in range(7):
                    if i < len(extracted_dates):
                        full_dates.append(extracted_dates[i])
                    else:
                        calc_d = base_d + datetime.timedelta(days=i)
                        full_dates.append(calc_d.strftime("%d/%m/%Y"))
            except Exception:
                full_dates = [extracted_dates[i] if i < len(extracted_dates) else f"Dia {i+1}" for i in range(7)]
        else:
            full_dates = [f"Dia {i+1}" for i in range(7)]

        dias = []
        for i in range(7):
            dias.append({
                "dia_semana": days_names[i],
                "data": full_dates[i],
                "cafe": {},
                "almoco": {},
                "jantar": {}
            })

        operating_days_count = len(extracted_dates) if extracted_dates else 7

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
                    for day_idx in range(operating_days_count):
                        dias[day_idx][meal_key][cat_name] = merged_val
                else:
                    for day_idx in range(7):
                        if day_idx < operating_days_count and items[day_idx]:
                            dias[day_idx][meal_key][cat_name] = items[day_idx]

        # Page 1: Cafe
        process_page(pages[0].extract_tables()[0], "cafe")
        # Page 2: Almoco
        process_page(pages[1].extract_tables()[0], "almoco")
        # Page 3: Jantar
        process_page(pages[2].extract_tables()[0], "jantar")

    def map_cafe(raw, is_closed=False, closed_msg=""):
        if is_closed:
            return {
                "bebidas": "",
                "panificacao": "",
                "gordura": "",
                "opcao_extra": "",
                "prato_principal": closed_msg or "Café não servido aos fins de semana",
                "vegetariano": "",
                "vegano": "",
                "fruta": ""
            }
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

    def map_almoco_jantar(raw, is_jantar=False, is_closed=False, closed_msg=""):
        if is_closed:
            res = {
                "salada_1": "",
                "salada_2": "",
                "molho": "",
                "prato_principal": closed_msg or "Refeitório fechado",
                "vegetariano": "",
                "vegano": "",
                "guarnicao": "",
                "acompanhamentos": "",
                "sobremesa": "",
                "bebida": ""
            }
            if is_jantar:
                res["sopa"] = ""
                res["torrada"] = ""
            return res

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
    for idx, d in enumerate(dias):
        is_day_closed = idx >= operating_days_count
        if campus_id == "gama" and idx in (5, 6):
            is_day_closed = True
            closed_reason = "Refeitório fechado aos finais de semana"
        elif campus_id == "ceilandia" and idx == 6:
            is_day_closed = True
            closed_reason = "Refeitório fechado aos domingos"
        else:
            closed_reason = "Refeitório sem funcionamento neste dia"

        formatted_dias.append({
            "dia_semana": d["dia_semana"],
            "data": d["data"],
            "cafe": map_cafe(d["cafe"], is_closed=is_day_closed, closed_msg=closed_reason),
            "almoco": map_almoco_jantar(d["almoco"], is_jantar=False, is_closed=is_day_closed, closed_msg=closed_reason),
            "jantar": map_almoco_jantar(d["jantar"], is_jantar=True, is_closed=is_day_closed, closed_msg=closed_reason)
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

        campus_links = [l for l in scraped_links if ckey in l[1].lower()]
        active_txt, active_url = select_active_pdf(campus_links)

        if not active_url:
            active_url = cfallback
            vigencia = "26/10/2026 a 01/11/2026"
        else:
            vigencia = active_txt.replace("ISM –", "").replace("ISM -", "").strip()
            if not vigencia:
                vigencia = "26/10/2026 a 01/11/2026"

        print(f"Processando {cname} ({vigencia}): {active_url}")
        tmp_pdf = os.path.join(parser_dir, f"tmp_{cid}.pdf")
        try:
            urllib.request.urlretrieve(active_url, tmp_pdf)
            menu_data = parse_pdf_menu(tmp_pdf, vigencia, active_url, campus_name=cname, campus_id=cid)
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
