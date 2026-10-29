import pdfplumber

with pdfplumber.open("C:/Users/Daniel/Desktop/ru-unb-app/parser/current_menu.pdf") as pdf:
    for i, page in enumerate(pdf.pages):
        print(f"--- PAGE {i+1} ---")
        tables = page.extract_tables()
        print(f"Tables found: {len(tables)}")
        if tables:
            for r in tables[0][:5]:
                print("Row:", r)
        else:
            print("No default table. Checking lines:")
            print("Lines count:", len(page.lines))
            print("Curves count:", len(page.curves))
            print("Text sample:\n", page.extract_text()[:300])
