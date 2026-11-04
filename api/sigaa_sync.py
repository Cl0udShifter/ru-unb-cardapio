from http.server import BaseHTTPRequestHandler
import json
import urllib.parse
import re
import requests
from bs4 import BeautifulSoup
import pypdfium2 as pdfium
import cv2
import numpy as np

def full_sigaa_sync(matricula, senha):
    session = requests.Session()
    session.headers.update({'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) RU-UnB-App'})
    
    # 1. Obter formulário CAS SSO
    sso_url = 'https://autenticacao.unb.br/sso-server/login?service=https%3A%2F%2Fsig.unb.br%2Fsigaa%2Flogin%2Fcas'
    try:
        r1 = session.get(sso_url, timeout=20)
    except Exception as e:
        return {'success': False, 'error': 'Não foi possível conectar ao SSO da UnB.'}
        
    soup1 = BeautifulSoup(r1.text, 'html.parser')
    form = soup1.find('form')
    if not form:
        return {'success': False, 'error': 'Não foi possível carregar o formulário do SSO da UnB.'}
        
    action = form.get('action', '')
    if not action.startswith('http'):
        action = 'https://autenticacao.unb.br' + action
        
    data = {}
    for inp in form.find_all('input'):
        name = inp.get('name')
        if name:
            data[name] = inp.get('value', '')
            
    data['username'] = matricula
    data['password'] = senha
    data['submit'] = 'ENTRAR'
    
    # 2. Login
    try:
        r2 = session.post(action, data=data, timeout=25)
    except Exception as e:
        return {'success': False, 'error': 'Falha na conexão com o servidor de autenticação.'}
        
    if 'Credenciais inv' in r2.text or 'não foram reconhecidas' in r2.text:
        return {'success': False, 'error': 'Matrícula ou senha incorretas.'}
        
    current_html = r2.text
    current_url = r2.url
    
    # 3. Tratar avisos institucionais do SIGAA (telaAvisoLogon.jsf)
    attempts = 0
    while 'telaAvisoLogon.jsf' in current_url and attempts < 6:
        soup_aviso = BeautifulSoup(current_html, 'html.parser')
        form_aviso = soup_aviso.find('form')
        if not form_aviso:
            break
        act = form_aviso.get('action', '')
        if not act.startswith('http'):
            act = 'https://sigaa.unb.br' + act
        aviso_data = {}
        for inp in form_aviso.find_all('input'):
            n = inp.get('name')
            if n:
                aviso_data[n] = inp.get('value', '')
        if 'formConfirmacaoDeclaracao' in current_html:
            aviso_data['formConfirmacaoDeclaracao:buttonConfirmar'] = 'Confirmar'
        else:
            submit_btn = form_aviso.find('input', {'type': 'submit'})
            if submit_btn and submit_btn.get('name'):
                aviso_data[submit_btn.get('name')] = submit_btn.get('value', 'Continuar >>')
        try:
            r_aviso = session.post(act, data=aviso_data, timeout=25)
            current_html = r_aviso.text
            current_url = r_aviso.url
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

    # 5. Emitir Carteirinha PDF
    view_state = ''
    vs_input = BeautifulSoup(current_html, 'html.parser').find('input', {'name': 'javax.faces.ViewState'})
    if vs_input:
        view_state = vs_input.get('value', '')
        
    form_id = '331956'
    id_input = BeautifulSoup(current_html, 'html.parser').find('input', {'name': 'id'})
    if id_input:
        form_id = id_input.get('value', '331956')
        
    card_body = {
        'menu:form_menu_discente': 'menu:form_menu_discente',
        'id': form_id,
        'jscook_action': 'menu_form_menu_discente_discente_menu:A]#{ portalDiscente.emitiCarteiraEstudantilUnb }',
        'javax.faces.ViewState': view_state
    }
    
    qr_payload = ''
    try:
        r_card = session.post('https://sigaa.unb.br/sigaa/portais/discente/discente.jsf', data=card_body, timeout=25)
        pdf_bytes = r_card.content
        if pdf_bytes.startswith(b'%PDF'):
            pdf = pdfium.PdfDocument(pdf_bytes)
            page = pdf.get_page(0)
            detector = cv2.QRCodeDetector()
            # Buscar imagem de tamanho aproximado do QR code
            for obj in page.get_objects():
                if obj.type == 3: # Imagem
                    bm = obj.get_bitmap()
                    pil_img = bm.to_pil()
                    if 150 <= pil_img.width <= 400 and 150 <= pil_img.height <= 400:
                        cv_img = cv2.cvtColor(np.array(pil_img), cv2.COLOR_RGB2BGR)
                        val, _, _ = detector.detectAndDecode(cv_img)
                        if val:
                            qr_payload = val
                            break
            # Fallback para renderização completa se necessário
            if not qr_payload:
                pil_image = page.render(scale=2).to_pil()
                cv_img = cv2.cvtColor(np.array(pil_image), cv2.COLOR_RGB2BGR)
                val, _, _ = detector.detectAndDecode(cv_img)
                if val:
                    qr_payload = val
    except Exception as e:
        print('Erro ao gerar/decodificar carteirinha:', e)

    # 6. Consultar Extrato do Restaurante Universitário
    saldo_atual = 'R$ 0,00'
    transacoes = []
    try:
        soup_ext = BeautifulSoup(current_html, 'html.parser')
        form_ext = soup_ext.find('form', id='formExibirExtrato') or soup_ext.find('form', {'id': re.compile(r'formExibirExtrato', re.I)})
        if form_ext:
            ext_act = form_ext.get('action', '')
            if not ext_act.startswith('http'):
                ext_act = 'https://sigaa.unb.br' + ext_act
            ext_data = {}
            for inp in form_ext.find_all('input'):
                n = inp.get('name')
                if n:
                    ext_data[n] = inp.get('value', '')
            m_jsf = re.search(r"jsfcljs\s*\(\s*document\.forms\['[^']+'\],\s*\{'([^']+)':'([^']+)'\}", current_html)
            if m_jsf:
                ext_data[m_jsf.group(1)] = m_jsf.group(2)
            r_ext = session.post(ext_act, data=ext_data, timeout=20)
            soup_res = BeautifulSoup(r_ext.text, 'html.parser')
            for row in soup_res.find_all('tr'):
                cols = [td.get_text(strip=True) for td in row.find_all(['td', 'th'])]
                if len(cols) >= 3:
                    data_hora, desc, valor = cols[0], cols[1], cols[2]
                    if desc.lower() == 'saldo':
                        saldo_atual = valor
                    elif re.match(r'^\d{2}/\d{2}/\d{4}', data_hora):
                        transacoes.append({'dataHora': data_hora, 'descricao': desc, 'valor': valor})
    except Exception as e:
        print('Erro no extrato:', e)

    return {
        'success': True,
        'nome': nome or 'Estudante UnB',
        'matricula': matricula,
        'curso': curso or 'GRADUAÇÃO',
        'validade': 'Março / 2027',
        'codigoQr': qr_payload or matricula.ljust(16, '0'),
        'saldo': saldo_atual,
        'transacoes': transacoes
    }

class handler(BaseHTTPRequestHandler):
    def _send_cors_headers(self):
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')

    def do_OPTIONS(self):
        self.send_response(200)
        self._send_cors_headers()
        self.end_headers()

    def do_POST(self):
        try:
            content_length = int(self.headers.get('Content-Length', 0))
            post_data = self.rfile.read(content_length)
            body = json.loads(post_data.decode('utf-8'))
            matricula = str(body.get('matricula', '')).strip()
            senha = str(body.get('senha', '')).strip()

            if not matricula or not senha:
                self.send_response(400)
                self._send_cors_headers()
                self.send_header('Content-Type', 'application/json; charset=utf-8')
                self.end_headers()
                self.wfile.write(json.dumps({'success': False, 'error': 'Matrícula e senha são obrigatórias.'}).encode('utf-8'))
                return

            result = full_sigaa_sync(matricula, senha)
            
            status_code = 200 if result.get('success') else 401
            self.send_response(status_code)
            self._send_cors_headers()
            self.send_header('Content-Type', 'application/json; charset=utf-8')
            self.end_headers()
            self.wfile.write(json.dumps(result, ensure_ascii=False).encode('utf-8'))
        except Exception as e:
            self.send_response(500)
            self._send_cors_headers()
            self.send_header('Content-Type', 'application/json; charset=utf-8')
            self.end_headers()
            self.wfile.write(json.dumps({'success': False, 'error': f'Erro interno no servidor: {str(e)}'}).encode('utf-8'))

if __name__ == '__main__':
    from http.server import HTTPServer
    server = HTTPServer(('127.0.0.1', 8765), handler)
    print('Servidor SIGAA Sync iniciado em http://127.0.0.1:8765')
    server.serve_forever()
