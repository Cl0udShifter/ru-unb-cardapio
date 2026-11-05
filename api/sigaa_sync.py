import json
import urllib.parse
import urllib.request
import http.cookiejar
import re
import zlib
import base64
import io
import struct
import html

def rgb_to_png(width, height, raw_rgb):
    png = b'\x89PNG\r\n\x1a\n'
    ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0)
    ihdr_crc = struct.pack('>I', zlib.crc32(b'IHDR' + ihdr_data))
    png += struct.pack('>I', len(ihdr_data)) + b'IHDR' + ihdr_data + ihdr_crc

    raw_scanlines = bytearray()
    row_bytes = width * 3
    for y in range(height):
        raw_scanlines.append(0)
        raw_scanlines.extend(raw_rgb[y * row_bytes : (y + 1) * row_bytes])

    idat_data = zlib.compress(bytes(raw_scanlines), level=6)
    idat_crc = struct.pack('>I', zlib.crc32(b'IDAT' + idat_data))
    png += struct.pack('>I', len(idat_data)) + b'IDAT' + idat_data + idat_crc
    iend_crc = struct.pack('>I', zlib.crc32(b'IEND'))
    png += struct.pack('>I', 0) + b'IEND' + iend_crc
    return png


REDES_6351_1 = [
    # 1º Semestre
    {'codigo': 'ENE0026', 'nome': 'Introdução à Eng. de Redes', 'ch': '30h', 'semestre': 1},
    {'codigo': 'ENE0334', 'nome': 'Computação para Engenharia', 'ch': '60h', 'semestre': 1},
    {'codigo': 'MAT0025', 'nome': 'Cálculo 1', 'ch': '90h', 'semestre': 1},
    {'codigo': 'MAT0031', 'nome': 'Introdução à Álgebra Linear', 'ch': '60h', 'semestre': 1},
    {'codigo': 'IFD0171', 'nome': 'Física 1', 'ch': '60h', 'semestre': 1},
    {'codigo': 'IFD0173', 'nome': 'Física 1 Experimental', 'ch': '30h', 'semestre': 1},

    # 2º Semestre
    {'codigo': 'MAT0026', 'nome': 'Cálculo 2', 'ch': '90h', 'semestre': 2},
    {'codigo': 'EST0023', 'nome': 'Probabilidade e Estatística', 'ch': '60h', 'semestre': 2},
    {'codigo': 'ENE0013', 'nome': 'Algoritmos e Estrutura de Dados', 'ch': '60h', 'semestre': 2},
    {'codigo': 'ENE0039', 'nome': 'Sistemas Digitais', 'ch': '60h', 'semestre': 2},
    {'codigo': 'ENE0040', 'nome': 'Laboratório de Sistemas Digitais', 'ch': '30h', 'semestre': 2},
    {'codigo': 'IQD0125', 'nome': 'Química Geral Teórica', 'ch': '60h', 'semestre': 2},

    # 3º Semestre
    {'codigo': 'MAT0027', 'nome': 'Cálculo 3', 'ch': '90h', 'semestre': 3},
    {'codigo': 'IFD0175', 'nome': 'Física 2', 'ch': '60h', 'semestre': 3},
    {'codigo': 'IFD0177', 'nome': 'Física 2 Experimental', 'ch': '60h', 'semestre': 3},
    {'codigo': 'ENE0274', 'nome': 'Fundamentos de Redes', 'ch': '60h', 'semestre': 3},
    {'codigo': 'ENE0066', 'nome': 'Introdução aos Circuitos Elétricos', 'ch': '30h', 'semestre': 3},
    {'codigo': 'ENE0282', 'nome': 'Laboratório de Circuitos Elétricos', 'ch': '30h', 'semestre': 3},

    # 4º Semestre
    {'codigo': 'ENE0304', 'nome': 'Circuitos Elétricos', 'ch': '60h', 'semestre': 4},
    {'codigo': 'ENE0067', 'nome': 'Sinais e Sistemas em Tempo Contínuo', 'ch': '60h', 'semestre': 4},
    {'codigo': 'ENE0015', 'nome': 'Fundamentos de Redes 2', 'ch': '60h', 'semestre': 4},
    {'codigo': 'ENE0011', 'nome': 'Laboratório de Redes', 'ch': '30h', 'semestre': 4},
    {'codigo': 'ENE0022', 'nome': 'Projeto Transversal em Redes 1', 'ch': '60h', 'semestre': 4},
    {'codigo': 'ENE0276', 'nome': 'Metodologia e Desenv. de Software', 'ch': '60h', 'semestre': 4},

    # 5º Semestre
    {'codigo': 'ENE0177', 'nome': 'Eletromagnetismo 1', 'ch': '60h', 'semestre': 5},
    {'codigo': 'ENE0068', 'nome': 'Sinais e Sistemas em Tempo Discreto', 'ch': '60h', 'semestre': 5},
    {'codigo': 'ENE0056', 'nome': 'Sistemas Microprocessados', 'ch': '60h', 'semestre': 5},
    {'codigo': 'ENE0058', 'nome': 'Lab. de Sistemas Microprocessados', 'ch': '30h', 'semestre': 5},
    {'codigo': 'ENE0025', 'nome': 'Protocolos de Transporte e Roteamento', 'ch': '60h', 'semestre': 5},
    {'codigo': 'ENE0352', 'nome': 'Redes Locais', 'ch': '60h', 'semestre': 5},

    # 6º Semestre
    {'codigo': 'ENE0045', 'nome': 'Eletrônica', 'ch': '60h', 'semestre': 6},
    {'codigo': 'ENE0046', 'nome': 'Laboratório de Eletrônica', 'ch': '30h', 'semestre': 6},
    {'codigo': 'ENE0091', 'nome': 'Sistemas Operacionais de Redes', 'ch': '60h', 'semestre': 6},
    {'codigo': 'ENE0016', 'nome': 'Gerência de Redes e Sistemas', 'ch': '60h', 'semestre': 6},
    {'codigo': 'ENE0090', 'nome': 'Segurança de Redes', 'ch': '60h', 'semestre': 6},
    {'codigo': 'ENE0017', 'nome': 'Avaliação de Desempenho de Redes', 'ch': '60h', 'semestre': 6},

    # 7º Semestre
    {'codigo': 'ENE0306', 'nome': 'Princípios de Comunicação', 'ch': '60h', 'semestre': 7},
    {'codigo': 'ENE0295', 'nome': 'Lab. de Princípios de Comunicação', 'ch': '30h', 'semestre': 7},
    {'codigo': 'ENE0353', 'nome': 'Antenas em Redes de Comunicações', 'ch': '60h', 'semestre': 7},
    {'codigo': 'ENE0089', 'nome': 'Cabeamento Estruturado', 'ch': '60h', 'semestre': 7},
    {'codigo': 'ENE0088', 'nome': 'Sistemas de Informação Distribuídos', 'ch': '60h', 'semestre': 7},
    {'codigo': 'ECL0014', 'nome': 'Ciências do Ambiente', 'ch': '30h', 'semestre': 7},

    # 8º Semestre
    {'codigo': 'ENE0064', 'nome': 'Redes Sem Fio', 'ch': '60h', 'semestre': 8},
    {'codigo': 'ENE0211', 'nome': 'Teoria da Informação', 'ch': '60h', 'semestre': 8},
    {'codigo': 'ENE0024', 'nome': 'Projeto Transversal em Redes 2', 'ch': '60h', 'semestre': 8},
    {'codigo': 'EPR0068', 'nome': 'Organização Industrial', 'ch': '60h', 'semestre': 8},
    {'codigo': 'ECO0019', 'nome': 'Introdução à Economia', 'ch': '60h', 'semestre': 8},
    {'codigo': 'FDD0155', 'nome': 'Noções de Direito', 'ch': '60h', 'semestre': 8},

    # 9º Semestre
    {'codigo': 'ENE0027', 'nome': 'Projeto Final de Graduação 1', 'ch': '60h', 'semestre': 9},
    {'codigo': 'ENE0028', 'nome': 'Estágio Supervisionado', 'ch': '120h', 'semestre': 9},
    {'codigo': 'EPR0059', 'nome': 'Higiene e Segurança do Trabalho', 'ch': '30h', 'semestre': 9},
    {'codigo': 'SOL0042', 'nome': 'Introdução à Sociologia', 'ch': '60h', 'semestre': 9},

    # 10º Semestre
    {'codigo': 'ENE0029', 'nome': 'Projeto Final de Graduação 2', 'ch': '120h', 'semestre': 10}
]

def resolve_flow_semesters(all_items, curso):
    is_redes = 'REDES' in (curso or '').upper()
    courses = REDES_6351_1 if is_redes else []
    if not courses:
        seen = {}
        sem_idx = 1
        for item in all_items:
            code = item['codigo'].strip().upper()
            if code and code not in seen:
                seen[code] = {
                    'codigo': code,
                    'nome': item['nome'],
                    'ch': item['cargaHoraria'] or '60h',
                    'semestre': min(10, sem_idx)
                }
                sem_idx += 1
        courses = list(seen.values())

    approved = {}
    enrolled = {}
    for item in all_items:
        code = item['codigo'].strip().upper()
        if item.get('isAprovado'):
            approved[code] = item
        elif item.get('isCursando'):
            enrolled[code] = item

    sem_map = {i: [] for i in range(1, 11)}
    for c in courses:
        code = c['codigo'].strip().upper()
        s_num = max(1, min(10, c.get('semestre', 1)))
        if code in approved:
            app_item = approved[code]
            sem_map[s_num].append({
                'codigo': c['codigo'],
                'nome': c['nome'],
                'ch': c['ch'],
                'status': 'CONCLUIDA',
                'mencaoObtida': app_item.get('mencao') or 'AP',
                'periodoCursado': app_item.get('anoPeriodo')
            })
        elif code in enrolled:
            enr_item = enrolled[code]
            sem_map[s_num].append({
                'codigo': c['codigo'],
                'nome': c['nome'],
                'ch': c['ch'],
                'status': 'EM_ANDAMENTO',
                'periodoCursado': enr_item.get('anoPeriodo')
            })
        else:
            sem_map[s_num].append({
                'codigo': c['codigo'],
                'nome': c['nome'],
                'ch': c['ch'],
                'status': 'PENDENTE'
            })

    result = []
    for i in range(1, 11):
        if sem_map[i]:
            result.append({
                'nivel': i,
                'titulo': f'{i}º Semestre',
                'materias': sem_map[i]
            })
    return result

def sync_with_urllib(matricula, senha):
    cj = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))
    headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App'}

    # 1. Obter formulário CAS SSO
    sso_url = 'https://autenticacao.unb.br/sso-server/login?service=https%3A%2F%2Fsig.unb.br%2Fsigaa%2Flogin%2Fcas'
    try:
        req1 = urllib.request.Request(sso_url, headers=headers)
        r1 = opener.open(req1, timeout=20)
        html1 = r1.read().decode('utf-8', errors='ignore')
    except Exception as e:
        return {'success': False, 'error': f'Não foi possível conectar ao SSO da UnB: {str(e)}'}

    action_match = re.search(r'<form[^>]+action=["\']([^"\']+)["\']', html1, re.I)
    action = action_match.group(1) if action_match else ''
    if not action.startswith('http'):
        action = 'https://autenticacao.unb.br' + action

    data = {}
    for m in re.finditer(r'<input[^>]+name=["\']([^"\']+)["\'][^>]*>', html1, re.I):
        tag = m.group(0)
        name = m.group(1)
        val_m = re.search(r'value=["\']([^"\']*)["\']', tag, re.I)
        data[name] = val_m.group(1) if val_m else ''

    data['username'] = matricula
    data['password'] = senha
    data['submit'] = 'ENTRAR'

    # 2. Login
    try:
        post_data = urllib.parse.urlencode(data).encode('utf-8')
        req2 = urllib.request.Request(action, data=post_data, headers=headers)
        r2 = opener.open(req2, timeout=25)
        current_html = r2.read().decode('utf-8', errors='ignore')
        current_url = r2.geturl()
    except Exception as e:
        return {'success': False, 'error': 'Falha na conexão com o servidor de autenticação.'}

    if 'Credenciais inv' in current_html or 'não foram reconhecidas' in current_html:
        return {'success': False, 'error': 'Matrícula ou senha incorretas.'}

    # 3. Tratar avisos institucionais do SIGAA (telaAvisoLogon.jsf)
    attempts = 0
    while 'telaAvisoLogon.jsf' in current_url and attempts < 6:
        act_m = re.search(r'<form[^>]+action=["\']([^"\']+)["\']', current_html, re.I)
        if not act_m:
            break
        act = act_m.group(1)
        if not act.startswith('http'):
            act = 'https://sigaa.unb.br' + act

        aviso_data = {}
        for m in re.finditer(r'<input[^>]+name=["\']([^"\']+)["\'][^>]*>', current_html, re.I):
            tag = m.group(0)
            name = m.group(1)
            val_m = re.search(r'value=["\']([^"\']*)["\']', tag, re.I)
            aviso_data[name] = val_m.group(1) if val_m else ''

        if 'formConfirmacaoDeclaracao' in current_html:
            aviso_data['formConfirmacaoDeclaracao:buttonConfirmar'] = 'Confirmar'
        else:
            submit_m = re.search(r'<input[^>]+type=["\']submit["\'][^>]+name=["\']([^"\']+)["\']', current_html, re.I)
            if submit_m:
                aviso_data[submit_m.group(1)] = 'Continuar >>'

        try:
            p_data = urllib.parse.urlencode(aviso_data).encode('utf-8')
            req_aviso = urllib.request.Request(act, data=p_data, headers=headers)
            r_aviso = opener.open(req_aviso, timeout=25)
            current_html = r_aviso.read().decode('utf-8', errors='ignore')
            current_url = r_aviso.geturl()
        except Exception:
            break
        attempts += 1

    # 4. Extrair Dados do Discente e Indicadores Acadêmicos
    clean_html = html.unescape(current_html)
    nome = ''
    curso = ''
    m_name = re.search(r'<b>([A-ZÀ-Ú][a-zà-ú]+(?:\s+[A-ZÀ-Ú][a-zà-ú]+)+)</b>', clean_html, re.I) or \
             re.search(r'<small>([A-ZÀ-Ú][a-zà-ú]+(?:\s+[A-ZÀ-Ú][a-zà-ú]+)+)</small>', clean_html, re.I)
    if m_name:
        nome = m_name.group(1).strip()

    m_course = re.search(r'<td[^>]*>([A-ZÀ-Ú\s]+(?:/ENE|/FT|/UnB|/FAU|/FS|/IB|/IQ|/IH|/IP|/FACE|/FD|/FE)[^<]*)</td>', clean_html, re.I) or \
               re.search(r'Curso:\s*</b>\s*([^<]+)', clean_html, re.I)
    if m_course:
        curso = m_course.group(1).strip().split('/')[0].strip()

    # Nível e Status
    nivel = 'GRADUAÇÃO'
    m_niv = re.search(r'<td>\s*N[íi]vel:\s*</td>\s*<td>\s*([^<]+)\s*</td>', clean_html, re.I)
    if m_niv:
        nivel = m_niv.group(1).strip()

    status_discente = 'ATIVO'
    m_st = re.search(r'<td>\s*Status:\s*</td>\s*<td>\s*([^<]+)\s*</td>', clean_html, re.I)
    if m_st:
        status_discente = m_st.group(1).strip()

    # Semestre atual
    semestre_atual = '2026.2'
    m_sem = re.search(r'Semestre\s+atual:\s*<strong>([^<]+)</strong>', clean_html, re.I)
    if m_sem:
        semestre_atual = m_sem.group(1).strip()

    # IRA e Média Ponderada
    ira = '--'
    m_ira = re.search(r'IRA:\s*</acronym></td>\s*<td[^>]*><div[^>]*>([\d\.,]+)</div>', clean_html, re.I) or \
            re.search(r'IRA:\s*</td>\s*<td[^>]*><div[^>]*>([\d\.,]+)</div>', clean_html, re.I) or \
            re.search(r'IRA:\s*</td>\s*<td[^>]*>([\d\.,]+)', clean_html, re.I)
    if m_ira:
        ira = m_ira.group(1).strip()

    media_ponderada = '--'
    m_mp = re.search(r'MP:\s*</acronym></td>\s*<td[^>]*><div[^>]*>([\d\.,]+)</div>', clean_html, re.I) or \
           re.search(r'MP:\s*</td>\s*<td[^>]*><div[^>]*>([\d\.,]+)</div>', clean_html, re.I)
    if m_mp:
        media_ponderada = m_mp.group(1).strip()

    # Carga Horária e Integralização
    m_ob = re.search(r'CH\.\s*Obrigat.*?Pendente\s*</td>\s*<td[^>]*>\s*(\d+)\s*</td>', clean_html, re.I | re.S)
    m_op = re.search(r'CH\.\s*Optativa.*?Pendente\s*</td>\s*<td[^>]*>\s*(\d+)\s*</td>', clean_html, re.I | re.S)
    m_tot = re.search(r'CH\.\s*Total.*?</td>\s*<td[^>]*>\s*(\d+)\s*</td>', clean_html, re.I | re.S)
    m_comp = re.search(r'CH\.\s*Complementar.*?Pendente\s*</td>\s*<td[^>]*>\s*(\d+)\s*</td>', clean_html, re.I | re.S)

    ch_total = int(m_tot.group(1)) if m_tot else 0
    ch_ob_pend = int(m_ob.group(1)) if m_ob else 0
    ch_op_pend = int(m_op.group(1)) if m_op else 0
    ch_comp_pend = int(m_comp.group(1)) if m_comp else 0
    ch_concluida = max(0, ch_total - (ch_ob_pend + ch_op_pend + ch_comp_pend)) if ch_total > 0 else 0
    pct_concluido = round((ch_concluida / ch_total) * 100, 1) if ch_total > 0 else 0.0

    # Foto do perfil via perfil.jsf
    foto_perfil_url = ''
    try:
        req_p = urllib.request.Request('https://sigaa.unb.br/sigaa/portais/discente/perfil.jsf', headers=headers)
        r_p = opener.open(req_p, timeout=12)
        p_html = r_p.read().decode('latin-1', errors='ignore')
        for m in re.finditer(r'<img[^>]+src=["\']([^"\']+)["\']', p_html, re.I):
            src = m.group(1)
            if 'arquivos.unb.br' in src or 'recuperarFoto' in src or 'foto' in src.lower():
                foto_perfil_url = src
                break
    except Exception:
        pass

    # 5. Emitir Carteirinha PDF e Extrair Imagem do QR Code
    vs_m = re.search(r'name=["\']javax\.faces\.ViewState["\'][^>]+value=["\']([^"\']*)["\']', current_html, re.I)
    view_state = vs_m.group(1) if vs_m else ''
    if not view_state:
        vs_m2 = re.search(r'value=["\']([^"\']*)["\'][^>]+name=["\']javax\.faces\.ViewState["\']', current_html, re.I)
        view_state = vs_m2.group(1) if vs_m2 else ''

    id_m = re.search(r'name=["\']id["\'][^>]+value=["\']([^"\']*)["\']', current_html, re.I)
    form_id = id_m.group(1) if id_m else '331956'

    card_body = {
        'menu:form_menu_discente': 'menu:form_menu_discente',
        'id': form_id,
        'jscook_action': 'menu_form_menu_discente_discente_menu:A]#{ portalDiscente.emitiCarteiraEstudantilUnb }',
        'javax.faces.ViewState': view_state
    }

    qr_base64 = ''
    qr_payload = None
    try:
        p_card = urllib.parse.urlencode(card_body).encode('utf-8')
        req_card = urllib.request.Request('https://sigaa.unb.br/sigaa/portais/discente/discente.jsf', data=p_card, headers=headers)
        r_card = opener.open(req_card, timeout=25)
        pdf_bytes = r_card.read()

        if pdf_bytes.startswith(b'%PDF'):
            matches = list(re.finditer(b'stream[\r\n]+', pdf_bytes))
            for m in matches:
                start = m.end()
                end = pdf_bytes.find(b'endstream', start)
                if end != -1:
                    stream_data = pdf_bytes[start:end].rstrip(b'\r\n')
                    try:
                        decomp = zlib.decompress(stream_data)
                        if len(decomp) == 250 * 250 * 3:
                            png_bytes = rgb_to_png(250, 250, decomp)
                            qr_base64 = 'data:image/png;base64,' + base64.b64encode(png_bytes).decode('utf-8')

                            try:
                                from PIL import Image
                                im = Image.open(io.BytesIO(png_bytes))
                                import pyrxing
                                barcode = pyrxing.read_barcode(im)
                                if barcode and barcode.text:
                                    qr_payload = barcode.text.strip()
                            except Exception:
                                pass
                            break
                    except Exception:
                        pass
    except Exception as e:
        print('Erro ao gerar/extrair imagem do QR code:', e)


    # 5.1 Histórico Escolar Oficial em PDF e Fluxograma
    historico = {
        'matricula': matricula,
        'curso': curso or 'GRADUAÇÃO',
        'curriculo': '6351/1',
        'ira': ira,
        'semestres': [],
        'totalAprovadas': 0,
        'totalReprovadas': 0,
        'totalCursando': 0,
        'pdfBase64': ''
    }
    flow_semesters = []
    try:
        hist_body = {
            'menu:form_menu_discente': 'menu:form_menu_discente',
            'id': form_id,
            'jscook_action': 'menu_form_menu_discente_discente_menu:A]#{ portalDiscente.historico }',
            'javax.faces.ViewState': view_state
        }
        p_hist = urllib.parse.urlencode(hist_body).encode('utf-8')
        req_hist = urllib.request.Request('https://sigaa.unb.br/sigaa/portais/discente/discente.jsf', data=p_hist, headers=headers)
        r_hist = opener.open(req_hist, timeout=25)
        h_pdf_bytes = r_hist.read()
        if len(h_pdf_bytes) > 1000 and h_pdf_bytes.startswith(b'%PDF'):
            historico['pdfBase64'] = base64.b64encode(h_pdf_bytes).decode('ascii')
            h_texts = []
            h_matches = list(re.finditer(b'stream[\r\n]+', h_pdf_bytes))
            for hm in h_matches:
                h_start = hm.end()
                h_end = h_pdf_bytes.find(b'endstream', h_start)
                if h_end != -1:
                    h_chunk = h_pdf_bytes[h_start:h_end].rstrip(b'\r\n')
                    try:
                        h_dec = zlib.decompress(h_chunk).decode('iso-8859-1', errors='ignore')
                        for ht in re.findall(r'\((.*?)\)Tj', h_dec):
                            h_texts.append(ht.replace(r'\(', '(').replace(r'\)', ')').replace(r'\\', '\\'))
                    except Exception:
                        pass

            h_start_pos = 0
            for hi, ht in enumerate(h_texts):
                if 'Componentes Curriculares Cursados' in ht:
                    h_start_pos = hi + 1
                    break

            all_history_items = []
            hi = h_start_pos
            while hi < len(h_texts):
                ht = h_texts[hi].strip()
                if re.match(r'^20\d{2}\.[1-2]$', ht):
                    sem_ano = ht
                    code_idx = -1
                    for hj in range(hi + 1, min(len(h_texts), hi + 15)):
                        if re.match(r'^[A-Z]{3}\d{4}$', h_texts[hj].strip()):
                            code_idx = hj
                            break
                    if code_idx != -1:
                        code = h_texts[code_idx].strip()
                        nome = h_texts[hi + 1].strip() if hi + 1 < len(h_texts) else ''
                        sit = ''
                        turma_str = ''
                        docente_str = ''
                        for hk in range(hi + 2, code_idx):
                            tok = h_texts[hk].strip()
                            if tok in ['APR', 'REP', 'REPF', 'TRANC', 'MATR', 'DISP', 'CUMP']:
                                sit = tok
                                if hk - 1 >= hi + 2:
                                    turma_str = h_texts[hk - 1].strip()
                                doc_parts = [h_texts[hd].strip() for hd in range(hi + 2, hk - 1)]
                                docente_str = ' '.join(doc_parts)

                        ch_str = h_texts[code_idx + 1].strip() if code_idx + 1 < len(h_texts) else ''
                        freq_str = h_texts[code_idx + 2].strip() if code_idx + 2 < len(h_texts) else ''
                        mencao_str = h_texts[code_idx + 3].strip() if code_idx + 3 < len(h_texts) else ''
                        if mencao_str in ['*', '---', '--', '']:
                            mencao_str = '-'
                        ch_fmt = f'{ch_str}h' if ch_str.isdigit() else ch_str

                        all_history_items.append({
                            'anoPeriodo': sem_ano,
                            'codigo': code,
                            'nome': nome,
                            'turma': turma_str,
                            'situacao': sit,
                            'docente': docente_str,
                            'cargaHoraria': ch_fmt,
                            'frequencia': freq_str,
                            'mencao': mencao_str,
                            'isAprovado': sit in ['APR', 'AP', 'DISP', 'CUMP'],
                            'isCursando': sit == 'MATR'
                        })
                        hi = code_idx + 3
                hi += 1

            sem_map = {}
            for item in all_history_items:
                s = item['anoPeriodo']
                if s not in sem_map:
                    sem_map[s] = []
                sem_map[s].append(item)

            sorted_sems = sorted(sem_map.keys(), reverse=True)
            historico['semestres'] = [{'anoPeriodo': s, 'disciplinas': sem_map[s]} for s in sorted_sems]
            historico['totalAprovadas'] = sum(1 for item in all_history_items if item['isAprovado'])
            historico['totalReprovadas'] = sum(1 for item in all_history_items if item['situacao'] in ['REP', 'REPF'])
            historico['totalCursando'] = sum(1 for item in all_history_items if item['isCursando'])

            flow_semesters = resolve_flow_semesters(all_history_items, curso)
    except Exception as e:
        print('Erro ao obter histórico escolar em PDF:', e)

    # 6. Consultar Extrato do Restaurante Universitário
    saldo_atual = 'R$ 0,00'
    saldo_anterior = 'R$ 0,00'
    transacoes = []
    try:
        form_m = re.search(r'<form[^>]+id=["\']formExibirExtrato["\'][^>]*>(.*?)</form>', current_html, re.DOTALL | re.I)
        if form_m:
            form_html = form_m.group(1)
            act_m = re.search(r'<form[^>]+id=["\']formExibirExtrato["\'][^>]+action=["\']([^"\']+)["\']', current_html, re.I)
            ext_act = act_m.group(1) if act_m else '/sigaa/portais/discente/discente.jsf'
            if not ext_act.startswith('http'):
                ext_act = 'https://sigaa.unb.br' + ext_act

            ext_data = {}
            for m in re.finditer(r'<input[^>]+name=["\']([^"\']+)["\'][^>]*>', form_html, re.I):
                tag = m.group(0)
                name = m.group(1)
                val_m = re.search(r'value=["\']([^"\']*)["\']', tag, re.I)
                ext_data[name] = val_m.group(1) if val_m else ''

            jsf_m = re.search(r"jsfcljs\(document\.getElementById\(['\"]formExibirExtrato['\"]\),\{'([^']+)':'([^']+)'\}", form_html, re.I)
            if not jsf_m:
                jsf_m = re.search(r"\{'([^']*formExibirExtrato[^']*)':'([^']+)'\}", form_html, re.I)
            if jsf_m:
                ext_data[jsf_m.group(1)] = jsf_m.group(2)

            p_ext = urllib.parse.urlencode(ext_data).encode('utf-8')
            req_ext = urllib.request.Request(ext_act, data=p_ext, headers=headers)
            r_ext = opener.open(req_ext, timeout=20)
            ext_resp_html = r_ext.read().decode('utf-8', errors='ignore')

            for row_m in re.finditer(r'<tr>(.*?)</tr>', ext_resp_html, re.I | re.DOTALL):
                row = row_m.group(1)
                cols = [html.unescape(re.sub(r'<[^>]+>', '', col_m.group(1))).strip() for col_m in re.finditer(r'<t[dh][^>]*>(.*?)</t[dh]>', row, re.I | re.DOTALL)]
                if len(cols) >= 3:
                    data_hora, desc, valor = cols[0], cols[1], cols[2]
                    desc = html.unescape(desc)
                    if 'saldo anterior' in desc.lower():
                        saldo_anterior = valor
                    elif desc.strip().lower() == 'saldo':
                        saldo_atual = valor
                    elif re.match(r'^\d{2}/\d{2}/\d{4}', data_hora):
                        transacoes.append({'dataHora': data_hora, 'descricao': desc, 'valor': valor})
    except Exception as e:
        print('Erro no extrato:', e)

    # 7. Extrair Disciplinas e Grade de Aulas
    disciplinas = []
    try:
        vs_refresh = re.search(r"""name=["\']javax\.faces\.ViewState["\'][^>]+value=["\']([^"\']*)["\']""", current_html, re.I)
        if vs_refresh:
            view_state = vs_refresh.group(1)

        table_m = re.search(r'<table[^>]*>.*?Componente Curricular.*?<tbody>(.*?)</tbody>\s*</table>', current_html, re.DOTALL | re.I)
        if table_m:
            tbody = table_m.group(1)
            rows = re.findall(r'<tr[^>]*>(.*?)</tr>', tbody, re.DOTALL | re.I)

            all_course_names = set()
            for r in rows:
                if 'frontEndIdTurma' in r:
                    nome_m = re.search(r'<a[^>]*>(.*?)</a>', r, re.DOTALL)
                    if nome_m:
                        n_clean = html.unescape(nome_m.group(1)).strip().lower()
                        if n_clean:
                            all_course_names.add(n_clean)

            DAY_MAP = {'segunda': 1, 'terca': 2, 'terça': 2, 'quarta': 3, 'quinta': 4, 'sexta': 5, 'sabado': 6, 'sábado': 6}
            for r in rows:
                if 'frontEndIdTurma' not in r:
                    continue
                nome_m = re.search(r'<a[^>]*>(.*?)</a>', r, re.DOTALL)
                nome_disc = html.unescape(nome_m.group(1)).strip() if nome_m else ''
                turma_m = re.search(r'["\']frontEndIdTurma["\']\s*:\s*["\']([^"\']+)["\']', r)
                turma_id = turma_m.group(1) if turma_m else ''
                tds = re.findall(r'<td[^>]*>(.*?)</td>', r, re.DOTALL)
                local = html.unescape(re.sub(r'<[^>]+>', '', tds[1])).strip() if len(tds) >= 2 else ''
                horario_raw = ''
                horarios = []
                if len(tds) >= 3:
                    td_horario = tds[2]
                    raw_m = re.search(r'([1-7]+[MTN][1-6]+)', td_horario)
                    if raw_m:
                        horario_raw = raw_m.group(1)
                    popup_m = re.search(r'<div[^>]*class=["\']popUp["\'][^>]*>(.*?)</div>', td_horario, re.DOTALL | re.I)
                    if popup_m:
                        popup_text = html.unescape(popup_m.group(1)).replace('<br />', '\n').replace('<br>', '\n')
                        for line in popup_text.split('\n'):
                            line = line.strip()
                            if not line: continue
                            h_m = re.search(r'([A-Za-zçãéÇÃÉ]+)(?:-feira)?\s+(\d{2}:\d{2})\s*(?:[àa]s|-)\s*(\d{2}:\d{2})', line, re.I)
                            if h_m:
                                dia_str = h_m.group(1).lower()
                                inicio = h_m.group(2)
                                fim = h_m.group(3)
                                dia_num = 0
                                for k, v in DAY_MAP.items():
                                    if k in dia_str:
                                        dia_num = v
                                        break
                                dia_nome = h_m.group(1).capitalize() + ('-feira' if 'sab' not in dia_str else '')
                                if dia_num > 0:
                                    horarios.append({
                                        'dia': dia_num,
                                        'diaNome': dia_nome,
                                        'inicio': inicio,
                                        'fim': fim,
                                        'horarioFormatado': f'{inicio} - {fim}'
                                    })
                td0 = tds[0] if len(tds) >= 1 else ''
                codigo = ''
                turma = ''
                docente = ''
                carga_horaria = ''

                td0_clean = re.sub(r'<a[^>]*>.*?</a>', '', td0, flags=re.DOTALL)
                code_m = re.search(r'\(([A-Z0-9_]{4,10})\s*-\s*([^\)]+)\)', td0_clean)
                if code_m:
                    codigo = code_m.group(1).strip()
                    turma = code_m.group(2).strip()

                ch_m = re.search(r'\((\d+h)\)', td0, re.I)
                if ch_m:
                    carga_horaria = ch_m.group(1).strip()
                else:
                    ch_m2 = re.search(r'(\d+)\s*h(?:oras)?', td0, re.I)
                    if ch_m2:
                        carga_horaria = f"{ch_m2.group(1).strip()}h"

                sub_m = re.search(r'<span[^>]*class=["\']subTexto["\'][^>]*>(.*?)</span>', td0, re.DOTALL | re.I)
                if sub_m:
                    raw_doc = html.unescape(re.sub(r'<[^>]+>', '', sub_m.group(1))).strip()
                    docente = re.sub(r'\(\d+h\)', '', raw_doc).strip()
                elif 'Docente:' in td0 or 'Professor:' in td0:
                    doc_m = re.search(r'(?:Docente|Professor):\s*([^<]+)', td0, re.I)
                    if doc_m:
                        docente = html.unescape(doc_m.group(1)).strip()

                pct_ministradas = None
                aulas_ministradas_texto = ''
                noticias = []

                jsf_map = {}
                target_form_id = 'form_acessarTurmaVirtual'
                jsf_m = re.search(r"""jsfcljs\s*\(\s*document\.getElementById\(['"]([^'"]+)['"]\)\s*,\s*\{([^}]+)\}""", r, re.I)
                if jsf_m:
                    target_form_id = jsf_m.group(1)
                    dict_content = jsf_m.group(2)
                    for p_m in re.finditer(r"""['"]([^'"]+)['"]\s*:\s*['"]([^'"]*)['"]""", dict_content):
                        jsf_map[p_m.group(1)] = p_m.group(2)

                if (turma_id or jsf_map) and view_state:
                    try:
                        tv_payload = {
                            target_form_id: target_form_id,
                            'javax.faces.ViewState': view_state
                        }
                        for k, v in jsf_map.items():
                            tv_payload[k] = v
                        if 'frontEndIdTurma' not in tv_payload and turma_id:
                            tv_payload['frontEndIdTurma'] = turma_id

                        req_tv = urllib.request.Request(
                            'https://sigaa.unb.br/sigaa/portais/discente/discente.jsf',
                            data=urllib.parse.urlencode(tv_payload).encode('utf-8'),
                            headers=headers
                        )
                        r_tv = opener.open(req_tv, timeout=8)
                        tv_html = r_tv.read().decode('latin-1', errors='ignore')

                        is_tv = ('turmaVirtual' in tv_html or 'ava' in tv_html or 'Ministradas/Total' in tv_html or 'Aulas Ministradas' in tv_html) and ('Componente Curricular' not in tv_html)

                        if is_tv:
                            m_ratio = re.search(r'Aulas\s*\(Ministradas/Total\):\s*(?:<[^>]+>)*\s*([0-9]+\s*/\s*[0-9]+)', tv_html, re.I)
                            if m_ratio:
                                ratio = m_ratio.group(1).strip()
                                parts = ratio.split('/')
                                if len(parts) == 2:
                                    aulas_ministradas_texto = f"{parts[0].strip()} de {parts[1].strip()} aulas ministradas"
                                else:
                                    aulas_ministradas_texto = f"{ratio} aulas ministradas"

                            m_prog = re.search(r'class=["\']progress-bar["\'][^>]*style=["\'][^"\']*width:\s*([0-9\.,]+)%', tv_html, re.I)
                            if m_prog:
                                pct_ministradas = float(m_prog.group(1).replace(',', '.'))

                            if pct_ministradas is not None and aulas_ministradas_texto:
                                aulas_ministradas_texto = f"{aulas_ministradas_texto} ({round(pct_ministradas)}%)"
                            elif pct_ministradas is not None and not aulas_ministradas_texto:
                                aulas_ministradas_texto = f"{round(pct_ministradas)}% de aulas ministradas"

                            not_pattern = re.compile(
                                r'(\d{2}/\d{2}/\d{4}(?:\s+\d{2}:\d{2})?)\s*<br\s*/?>\s*<i>(.*?)</i>\s*<br\s*/?>\s*<form[^>]+id=["\']([^"\']+)["\'][^>]*>(.*?)</form>',
                                re.DOTALL | re.I
                            )
                            for not_m in not_pattern.finditer(tv_html):
                                data_not = not_m.group(1).strip()
                                titulo_raw = re.sub(r'<[^>]+>', '', not_m.group(2)).strip()
                                titulo_not = html.unescape(titulo_raw).strip()
                                f_id = not_m.group(3).strip()
                                f_content = not_m.group(4)

                                if titulo_not and titulo_not.lower() not in all_course_names:
                                    conteudo_not = ""
                                    anexos = []
                                    try:
                                        id_m = re.search(r'name=["\']id["\'][^>]+value=["\']([^"\']+)["\']', f_content)
                                        vs_m = re.search(r'name=["\']javax\.faces\.ViewState["\'][^>]+value=["\']([^"\']*)["\']', f_content)
                                        oc_m = re.search(r"""jsfcljs\s*\(\s*document\.getElementById\(['"][^'"]+['"]\)\s*,\s*\{([^}]+)\}""", f_content)

                                        p_detail = {
                                            f_id: f_id,
                                            'id': id_m.group(1) if id_m else '',
                                            'javax.faces.ViewState': vs_m.group(1) if vs_m else ''
                                        }
                                        if oc_m:
                                            for pm in re.finditer(r"""['"]([^'"]+)['"]\s*:\s*['"]([^'"]*)['"]""", oc_m.group(1)):
                                                p_detail[pm.group(1)] = pm.group(2)

                                        req_det = urllib.request.Request(
                                            'https://sigaa.unb.br/sigaa/ava/index.jsf',
                                            data=urllib.parse.urlencode(p_detail).encode('utf-8'),
                                            headers=headers
                                        )
                                        r_det = opener.open(req_det, timeout=6)
                                        det_html = r_det.read().decode('latin-1', errors='ignore')

                                        td_m = re.search(r'<td[^>]+class=["\']conteudoNoticia["\'][^>]*>(.*?)</td>', det_html, re.DOTALL | re.I)
                                        if td_m:
                                            conteudo_not = html.unescape(re.sub(r'<[^>]+>', '', td_m.group(1).replace('<br />', '\n').replace('<br>', '\n'))).strip()

                                        for att_m in re.finditer(r'<a\s+[^>]*href=["\']([^"\']*(?:verArquivo|downloadArquivo)[^"\']*)["\'][^>]*>(.*?)</a>', det_html, re.DOTALL | re.I):
                                            att_url = att_m.group(1)
                                            if not att_url.startswith('http'):
                                                att_url = 'https://sigaa.unb.br' + att_url
                                            raw_name = html.unescape(re.sub(r'\s+', ' ', re.sub(r'<[^>]+>', '', att_m.group(2)))).strip()
                                            if raw_name and 'idArquivo=&' not in att_url:
                                                anexos.append({'nome': raw_name, 'url': att_url})
                                    except Exception:
                                        pass

                                    noticias.append({
                                        'titulo': titulo_not,
                                        'data': data_not,
                                        'conteudo': conteudo_not or 'Aviso publicado pelo docente na Turma Virtual do SIGAA.',
                                        'anexos': anexos
                                    })

                            # 3. Extrair Frequência e Faltas
                            pct_freq = None
                            num_faltas = 0
                            try:
                                freq_m = re.search(
                                    r"""jsfcljs\s*\(\s*document\.getElementById\(['"](formMenu)['"]\)\s*,\s*\{([^}]+)\}[^)]*\)[^>]*>[^<]*<div[^>]*>[^<]*Frequ""",
                                    tv_html,
                                    re.I
                                )
                                if freq_m:
                                    fm_id = freq_m.group(1)
                                    fm_params = {fm_id: fm_id}
                                    vs_tv = re.search(r'name=["\']javax\.faces\.ViewState["\'][^>]+value=["\']([^"\']*)["\']', tv_html)
                                    if vs_tv:
                                        fm_params['javax.faces.ViewState'] = vs_tv.group(1)

                                    for pm in re.finditer(r"""['"]([^'"]+)['"]\s*:\s*['"]([^'"]*)['"]""", freq_m.group(2)):
                                        fm_params[pm.group(1)] = pm.group(2)

                                    req_freq = urllib.request.Request(
                                        'https://sigaa.unb.br/sigaa/ava/index.jsf',
                                        data=urllib.parse.urlencode(fm_params).encode('utf-8'),
                                        headers=headers
                                    )
                                    r_freq = opener.open(req_freq, timeout=6)
                                    freq_html = r_freq.read().decode('latin-1', errors='ignore')

                                    pf_ch = re.search(r'Porcentagem de Frequ(?:&#234;|[êe])ncia em rela(?:&#231;&#227;|[çc][ãa])o a CH[^:]*:\s*(?:<[^>]+>\s*)*([0-9\.,]+)%', freq_html, re.I)
                                    if pf_ch:
                                        pct_freq = float(pf_ch.group(1).replace(',', '.'))
                                    else:
                                        pf_gen = re.search(r'Porcentagem de Frequ(?:&#234;|[êe])ncia[^:]*:\s*(?:<[^>]+>\s*)*([0-9\.,]+)%', freq_html, re.I)
                                        if pf_gen:
                                            pct_freq = float(pf_gen.group(1).replace(',', '.'))
                                        elif 'não foi lançada' in freq_html or 'n&#227;o foi lan&#231;ada' in freq_html:
                                            pct_freq = 100.0

                                    faltas_list = [int(x) for x in re.findall(r'(\d+)\s*Falta\(s\)', freq_html, re.I)]
                                    num_faltas = sum(faltas_list)
                            except Exception as e_f:
                                print(f"Erro ao consultar frequência para {nome_disc}:", e_f)

                            # Reseta para discente.jsf
                            try:
                                req_reset = urllib.request.Request('https://sigaa.unb.br/sigaa/portais/discente/discente.jsf', headers=headers)
                                r_reset = opener.open(req_reset, timeout=6)
                                reset_html = r_reset.read().decode('latin-1', errors='ignore')
                                vs_m = re.search(r"""name=["\']javax\.faces\.ViewState["\'][^>]+value=["\']([^"\']*)["\']""", reset_html)
                                if vs_m:
                                    view_state = vs_m.group(1)
                            except Exception:
                                pass
                    except Exception as e_tv:
                        print(f"Erro ao consultar Turma Virtual para {nome_disc}:", e_tv)

                if nome_disc:
                    disciplinas.append({
                        'nome': nome_disc,
                        'turmaId': turma_id,
                        'codigo': codigo,
                        'turma': turma,
                        'docente': docente,
                        'cargaHoraria': carga_horaria,
                        'local': local,
                        'horarioRaw': horario_raw,
                        'horarios': horarios,
                        'faltas': num_faltas,
                        'percentualFrequencia': pct_freq,
                        'percentualAulasMinistradas': pct_ministradas,
                        'aulasMinistradasTexto': aulas_ministradas_texto,
                        'noticias': noticias
                    })
    except Exception as e:
        print('Erro ao extrair disciplinas:', e)

    codigo_qr = qr_payload or matricula.ljust(16, '0')

    return {
        'success': True,
        'disciplinas': disciplinas,
        'nome': nome or 'Estudante UnB',
        'matricula': matricula,
        'curso': curso or 'GRADUAÇÃO',
        'nivel': nivel,
        'status': status_discente,
        'semestreAtual': semestre_atual,
        'ira': ira,
        'mediaPonderada': media_ponderada,
        'chTotal': ch_total,
        'chObrigatoriaPendente': ch_ob_pend,
        'chOptativaPendente': ch_op_pend,
        'chComplementarPendente': ch_comp_pend,
        'chConcluida': ch_concluida,
        'percentualConcluido': pct_concluido,
        'fotoPerfilUrl': foto_perfil_url,
        'validade': 'Março / 2027',
        'codigoQr': codigo_qr,
        'qrImageBase64': qr_base64,
        'saldo': saldo_atual,
        'saldoAnterior': saldo_anterior,
        'transacoes': transacoes,
        'historico': historico,
        'flowSemesters': flow_semesters
    }

def emit_sigaa_document(matricula, senha, doc_type):
    from datetime import datetime
    cj = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))
    headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App'}

    try:
        sso_url = 'https://autenticacao.unb.br/sso-server/login?service=https%3A%2F%2Fsig.unb.br%2Fsigaa%2Flogin%2Fcas'
        req_sso = urllib.request.Request(sso_url, headers=headers)
        r_sso = opener.open(req_sso, timeout=20)
        sso_html = r_sso.read().decode('utf-8', errors='ignore')

        m_action = re.search(r'<form[^>]+action="([^"]+)"', sso_html)
        action_url = urllib.parse.urljoin('https://autenticacao.unb.br', m_action.group(1)) if m_action else sso_url

        login_data = dict(re.findall(r'<input[^>]+name="([^"]+)"[^>]+value="([^"]*)"', sso_html))
        login_data['username'] = matricula
        login_data['password'] = senha
        login_data['submit'] = 'ENTRAR'

        p_login = urllib.parse.urlencode(login_data).encode('utf-8')
        req_login = urllib.request.Request(action_url, data=p_login, headers=headers)
        r_login = opener.open(req_login, timeout=20)
        cur_html = r_login.read().decode('utf-8', errors='ignore')
        cur_url = r_login.geturl()

        aviso_count = 0
        while 'telaAvisoLogon.jsf' in cur_url and aviso_count < 6:
            m_av = re.search(r'<form[^>]+action="([^"]+)"', cur_html)
            av_url = urllib.parse.urljoin('https://sigaa.unb.br', m_av.group(1)) if m_av else cur_url
            av_inputs = dict(re.findall(r'<input[^>]+name="([^"]+)"[^>]+value="([^"]*)"', cur_html))
            if 'formConfirmacaoDeclaracao' in cur_html or 'type="password"' in cur_html:
                p_name = 'formConfirmacaoDeclaracao:senha'
                for k in av_inputs:
                    if 'senha' in k.lower() or 'password' in k.lower():
                        p_name = k
                av_inputs[p_name] = senha
                av_inputs['formConfirmacaoDeclaracao:buttonConfirmar'] = 'Confirmar'
            else:
                av_inputs['btnContinuar'] = 'Continuar'
            p_av = urllib.parse.urlencode(av_inputs).encode('utf-8')
            r_av = opener.open(urllib.request.Request(av_url, data=p_av, headers=headers), timeout=20)
            cur_html = r_av.read().decode('utf-8', errors='ignore')
            cur_url = r_av.geturl()
            aviso_count += 1

        vs_m = re.search(r'name=["\']javax\.faces\.ViewState["\'][^>]+value=["\']([^"\']*)["\']', cur_html, re.I)
        view_state = vs_m.group(1) if vs_m else ''
        id_m = re.search(r'name=["\']id["\'][^>]+value=["\']([^"\']*)["\']', cur_html, re.I)
        form_id = id_m.group(1) if id_m else '331956'

        action_map = {
            'vinculo': ('menu_form_menu_discente_discente_menu:A]#{ declaracaoVinculo.emitirDeclaracao}', True, 'Declaração de Vínculo'),
            'atestado': ('menu_form_menu_discente_discente_menu:A]#{ portalDiscente.atestadoMatriculaUnb }', False, 'Atestado de Matrícula'),
            'passe_livre': ('menu_form_menu_discente_discente_menu:A]#{ portalDiscente.passeEstudantilUnb }', False, 'Declaração para Passe Livre'),
            'historico': ('menu_form_menu_discente_discente_menu:A]#{ portalDiscente.historico }', True, 'Histórico Escolar Oficial')
        }

        if doc_type not in action_map:
            return {'success': False, 'error': f'Tipo de documento inválido: {doc_type}'}

        jsf_action, is_pdf, title = action_map[doc_type]

        body = {
            'menu:form_menu_discente': 'menu:form_menu_discente',
            'id': form_id,
            'jscook_action': jsf_action,
            'javax.faces.ViewState': view_state
        }
        p_body = urllib.parse.urlencode(body).encode('utf-8')
        req_doc = urllib.request.Request('https://sigaa.unb.br/sigaa/portais/discente/discente.jsf', data=p_body, headers=headers)
        r_doc = opener.open(req_doc, timeout=25)
        raw_bytes = r_doc.read()

        now_str = datetime.now().strftime('%d/%m/%Y %H:%M')

        if is_pdf:
            if len(raw_bytes) > 1000 and raw_bytes.startswith(b'%PDF'):
                b64 = base64.b64encode(raw_bytes).decode('ascii')
                kb = len(raw_bytes) // 1024
                size_str = f"{kb / 1024:.1f} MB" if kb > 1024 else f"{kb} KB"
                return {
                    'success': True,
                    'docType': doc_type,
                    'title': title,
                    'isPdfDirect': True,
                    'pdfBase64': b64,
                    'fileSizeFormatted': size_str,
                    'lastEmittedAt': now_str
                }
            else:
                return {'success': False, 'error': 'O SIGAA não retornou o PDF oficial do documento.'}
        else:
            html_text = raw_bytes.decode('iso-8859-1', errors='ignore')
            m_code = re.search(r'c(?:[oó]|&#243;)digo de verifica(?:[cç]|&#231;)(?:[aã]|&#227;)o[^<]*<span[^>]*>\s*([a-zA-Z0-9]+)\s*</span>', html_text, re.I)
            m_data = re.search(r'Emitido em\s+([\d/]+\s+[\d:]+)', html_text, re.I)
            auth_code = m_code.group(1) if m_code else ''
            emitted_date = m_data.group(1) if m_data else now_str
            kb = len(raw_bytes) // 1024
            return {
                'success': True,
                'docType': doc_type,
                'title': title,
                'isPdfDirect': False,
                'htmlContent': html_text,
                'codigoAutenticidade': auth_code,
                'lastEmittedAt': emitted_date,
                'fileSizeFormatted': f"{kb} KB"
            }
    except Exception as e:
        return {'success': False, 'error': f'Falha ao emitir documento do SIGAA: {str(e)}'}

def full_sigaa_sync(matricula, senha):
    return sync_with_urllib(matricula, senha)

def app(environ, start_response):
    method = environ.get('REQUEST_METHOD', 'GET')
    headers = [
        ('Access-Control-Allow-Origin', '*'),
        ('Access-Control-Allow-Methods', 'GET, POST, OPTIONS'),
        ('Access-Control-Allow-Headers', 'Content-Type'),
        ('Content-Type', 'application/json; charset=utf-8')
    ]

    if method == 'OPTIONS':
        start_response('200 OK', headers)
        return [b'']

    if method == 'GET':
        start_response('200 OK', headers)
        return [json.dumps({'status': 'ok', 'service': 'RU UnB SIGAA Sync API'}).encode('utf-8')]

    if method == 'POST':
        try:
            content_length = int(environ.get('CONTENT_LENGTH', 0))
        except (ValueError, TypeError):
            content_length = 0

        body = environ['wsgi.input'].read(content_length).decode('utf-8') if content_length > 0 else '{}'
        try:
            data = json.loads(body)
        except Exception:
            data = {}

        matricula = str(data.get('matricula', '')).strip()
        senha = str(data.get('senha', '')).strip()
        action = str(data.get('action', '')).strip()
        doc_type = str(data.get('docType', '')).strip()

        if not matricula or not senha:
            start_response('400 Bad Request', headers)
            return [json.dumps({'success': False, 'error': 'Matrícula e senha são obrigatórias.'}).encode('utf-8')]

        if action == 'emit_document':
            result = emit_sigaa_document(matricula, senha, doc_type)
            status_code = '200 OK' if result.get('success') else '400 Bad Request'
            start_response(status_code, headers)
            return [json.dumps(result, ensure_ascii=False).encode('utf-8')]

        result = full_sigaa_sync(matricula, senha)
        status_code = '200 OK' if result.get('success') else '401 Unauthorized'
        start_response(status_code, headers)
        return [json.dumps(result, ensure_ascii=False).encode('utf-8')]

    start_response('405 Method Not Allowed', headers)
    return [b'']

handler = app
