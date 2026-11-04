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

    # 4. Extrair Nome e Curso do Discente
    nome = ''
    curso = ''
    m_name = re.search(r'<b>([A-ZÀ-Ú][a-zà-ú]+(?:\s+[A-ZÀ-Ú][a-zà-ú]+)+)</b>', current_html, re.I)
    if m_name:
        nome = m_name.group(1).strip()
    else:
        m_name2 = re.search(r'<small>([A-ZÀ-Ú][a-zà-ú]+(?:\s+[A-ZÀ-Ú][a-zà-ú]+)+)</small>', current_html, re.I)
        if m_name2:
            nome = m_name2.group(1).strip()

    m_course = re.search(r'<td[^>]*>([A-ZÀ-Ú\s]+(?:/ENE|/FT|/UnB|/FAU|/FS|/IB|/IQ|/IH|/IP|/FACE|/FD|/FE)[^<]*)</td>', current_html, re.I)
    if m_course:
        curso = m_course.group(1).strip().split('/')[0].strip()
    else:
        m_course2 = re.search(r'Curso:\s*</b>\s*([^<]+)', current_html, re.I)
        if m_course2:
            curso = m_course2.group(1).strip()

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

    codigo_qr = qr_payload or matricula.ljust(16, '0')

    return {
        'success': True,
        'nome': nome or 'Estudante UnB',
        'matricula': matricula,
        'curso': curso or 'GRADUAÇÃO',
        'validade': 'Março / 2027',
        'codigoQr': codigo_qr,
        'qrImageBase64': qr_base64,
        'saldo': saldo_atual,
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
