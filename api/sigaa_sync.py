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
        table_m = re.search(r'<table[^>]*>.*?Componente Curricular.*?<tbody>(.*?)</tbody>\s*</table>', current_html, re.DOTALL | re.I)
        if table_m:
            tbody = table_m.group(1)
            rows = re.findall(r'<tr[^>]*class=["'](?:odd)?["'][^>]*>(.*?)</tr>', tbody, re.DOTALL | re.I)
            DAY_MAP = {'segunda': 1, 'terca': 2, 'terça': 2, 'quarta': 3, 'quinta': 4, 'sexta': 5, 'sabado': 6, 'sábado': 6}
            for r in rows:
                nome_m = re.search(r'<a[^>]*>(.*?)</a>', r, re.DOTALL)
                nome_disc = html.unescape(nome_m.group(1)).strip() if nome_m else ''
                turma_m = re.search(r'["']frontEndIdTurma["']\s*:\s*["']([^"']+)["']', r)
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
                    popup_m = re.search(r'<div[^>]*class=["']popUp["'][^>]*>(.*?)</div>', td_horario, re.DOTALL | re.I)
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

                sub_m = re.search(r'<span[^>]*class=["']subTexto["'][^>]*>(.*?)</span>', td0, re.DOTALL | re.I)
                if sub_m:
                    raw_doc = html.unescape(re.sub(r'<[^>]+>', '', sub_m.group(1))).strip()
                    docente = re.sub(r'\(\d+h\)', '', raw_doc).strip()
                elif 'Docente:' in td0 or 'Professor:' in td0:
                    doc_m = re.search(r'(?:Docente|Professor):\s*([^<]+)', td0, re.I)
                    if doc_m:
                        docente = html.unescape(doc_m.group(1)).strip()

                pct_freq = None
                faltas = 0
                pct_ministradas = None
                aulas_ministradas_texto = ''
                noticias = []

                if turma_id and view_state:
                    try:
                        tv_payload = {
                            'form_acessarTurmaVirtual': 'form_acessarTurmaVirtual',
                            'form_acessarTurmaVirtual:frontEndIdTurma': turma_id,
                            'javax.faces.ViewState': view_state
                        }
                        req_tv = urllib.request.Request(
                            'https://sigaa.unb.br/sigaa/portais/discente/discente.jsf',
                            data=urllib.parse.urlencode(tv_payload).encode('utf-8'),
                            headers=headers
                        )
                        r_tv = opener.open(req_tv, timeout=6)
                        tv_html = r_tv.read().decode('latin-1', errors='ignore')

                        m_aulas = re.search(r'Aulas\s+Ministradas:\s*(?:<strong>)?\s*([0-9]+\s*(?:de\s*[0-9]+)?(?:\s*\([0-9\.,]+%\))?)', tv_html, re.I)
                        if m_aulas:
                            aulas_ministradas_texto = m_aulas.group(1).strip()
                            m_pct = re.search(r'([0-9\.,]+)%', aulas_ministradas_texto)
                            if m_pct:
                                pct_ministradas = float(m_pct.group(1).replace(',', '.'))

                        m_freq = re.search(r'Frequ[êe]ncia:\s*(?:<strong>)?\s*([0-9\.,]+)%', tv_html, re.I)
                        if m_freq:
                            pct_freq = float(m_freq.group(1).replace(',', '.'))

                        m_faltas = re.search(r'Faltas:\s*(?:<strong>)?\s*([0-9]+)', tv_html, re.I)
                        if m_faltas:
                            faltas = int(m_faltas.group(1))

                        for not_match in re.finditer(r'<tr[^>]*class=["'](?:linhaNoticia|odd|even)["'][^>]*>(.*?)</tr>', tv_html, re.DOTALL | re.I):
                            row_c = not_match.group(1)
                            date_m = re.search(r'(\d{2}/\d{2}/\d{4})', row_c)
                            title_m = re.search(r'<a[^>]*>(.*?)</a>', row_c, re.DOTALL)
                            if title_m:
                                n_titulo = html.unescape(re.sub(r'<[^>]+>', '', title_m.group(1))).strip()
                                n_data = date_m.group(1) if date_m else ''
                                anexos = []
                                for att_m in re.finditer(r'<a[^>]+href=["']([^"']*(?:verArquivo|downloadArquivo)[^"']*)["'][^>]*>(.*?)</a>', row_c, re.I):
                                    att_url = att_m.group(1)
                                    if not att_url.startswith('http'):
                                        att_url = 'https://sigaa.unb.br' + att_url
                                    att_name = html.unescape(re.sub(r'<[^>]+>', '', att_m.group(2))).strip() or 'Baixar anexo'
                                    anexos.append({'nome': att_name, 'url': att_url})
                                if n_titulo:
                                    noticias.append({
                                        'titulo': n_titulo,
                                        'data': n_data,
                                        'conteudo': '',
                                        'anexos': anexos
                                    })
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
                        'faltas': faltas,
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
        'transacoes': transacoes
    }

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

        if not matricula or not senha:
            start_response('400 Bad Request', headers)
            return [json.dumps({'success': False, 'error': 'Matrícula e senha são obrigatórias.'}).encode('utf-8')]

        result = full_sigaa_sync(matricula, senha)
        status_code = '200 OK' if result.get('success') else '401 Unauthorized'
        start_response(status_code, headers)
        return [json.dumps(result, ensure_ascii=False).encode('utf-8')]

    start_response('405 Method Not Allowed', headers)
    return [b'']

handler = app
