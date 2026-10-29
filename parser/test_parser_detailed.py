import pdfplumber
import json
import re

def parse_pdf_menu(pdf_path):
    days_names = ["Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"]
    
    with pdfplumber.open(pdf_path) as pdf:
        all_pages = pdf.pages
        # Page 1: Cafe, Page 2: Almoco, Page 3: Jantar
        
        # 1. Discover dates from page 2 (Almoco)
        p2_table = all_pages[1].extract_tables()[0]
        # find the row with dates
        dates = []
        date_pattern = re.compile(r'\d{2}/\d{2}/\d{4}')
        for r in p2_table[:3]:
            found_dates = [c.strip() for c in r if c and date_pattern.search(c)]
            if len(found_dates) == 7:
                dates = found_dates
                break
        
        print(f"Datas detectadas: {dates}")
        
        # Structure for 7 days
        days_data = []
        for i in range(7):
            d_str = dates[i] if i < len(dates) else f"Dia {i+1}"
            days_data.append({
                "dia_semana": days_names[i],
                "data": d_str,
                "cafe": {},
                "almoco": {},
                "jantar": {}
            })
            
        def extract_page_data(table, meal_key):
            # Find the header row that contains 2ª FEIRA
            header_row_idx = -1
            start_col = -1
            for r_idx, r in enumerate(table[:3]):
                for c_idx, cell in enumerate(r):
                    if cell and ("2ª" in cell or "2ª FEIRA" in cell.upper() or "2 FEIRA" in cell.upper()):
                        header_row_idx = r_idx
                        start_col = c_idx
                        break
                if header_row_idx != -1:
                    break
            
            # The category label column is immediately before start_col
            cat_col = start_col - 1
            print(f"Meal: {meal_key}, header_row: {header_row_idx}, cat_col: {cat_col}, start_col: {start_col}")
            
            for row in table[header_row_idx + 2:]:
                if not row:
                    continue
                cat_name = row[cat_col] if cat_col < len(row) and row[cat_col] else ""
                cat_name = re.sub(r'\s+', ' ', cat_name).strip()
                if not cat_name:
                    continue
                    
                # Collect items for each of the 7 days
                for day_idx in range(7):
                    c_idx = start_col + day_idx
                    val = row[c_idx] if c_idx < len(row) and row[c_idx] else ""
                    val = re.sub(r'\s+', ' ', val).strip()
                    if val:
                        days_data[day_idx][meal_key][cat_name] = val
                    elif c_idx == start_col and not val:
                        # Check if it was a spanned cell (e.g. Leite integral ou Bebida de soja across all days)
                        pass

        # Parse Page 1: Cafe
        p1_table = all_pages[0].extract_tables()[0]
        extract_page_data(p1_table, "cafe")
        
        # Parse Page 2: Almoco
        extract_page_data(p2_table, "almoco")
        
        # Parse Page 3: Jantar
        p3_table = all_pages[2].extract_tables()[0]
        extract_page_data(p3_table, "jantar")

        return days_data

data = parse_pdf_menu("C:/Users/Daniel/Desktop/ru-unb-app/parser/current_menu.pdf")
print("\n--- Exemplo do Almoço de Segunda-feira ---")
print(json.dumps(data[0], indent=2, ensure_ascii=False))
