// CONFIGURAÇÕES GERAIS
const API_URL = "https://valuecomparator.onrender.com/api";
let markdownAtual = "";
let termoAtual = "";
// Telas Principais
const authScreen = document.getElementById('auth-screen');
const appScreen = document.getElementById('app-screen');
// Autenticação
const loginInput = document.getElementById('login-input');
const senhaInput = document.getElementById('senha-input');
const btnLogin = document.getElementById('btn-login');
const btnRegister = document.getElementById('btn-register');
const authMessage = document.getElementById('auth-message');
const btnLogout = document.getElementById('btn-logout');
// Navegação (Abas)
const tabSearch = document.getElementById('tab-search');
const tabHistory = document.getElementById('tab-history');
const viewSearch = document.getElementById('view-search');
const viewHistory = document.getElementById('view-history');
// Pesquisa
const searchInput = document.getElementById('search-input');
const btnSearch = document.getElementById('btn-search');
const loadingDiv = document.getElementById('loading');
const reportContainer = document.getElementById('report-container');
const reportContent = document.getElementById('report-content');
const btnExportPdf = document.getElementById('btn-export-pdf');
const historyList = document.getElementById('history-list');

// INICIALIZAÇÃO
document.addEventListener("DOMContentLoaded", () => {
    const token = localStorage.getItem("jwt_token");
    if (token) {
        mostrarApp();
    } else {
        mostrarAuth();
    }
});

btnLogin.addEventListener('click', fazerLogin);
btnRegister.addEventListener('click', fazerCadastro);
btnSearch.addEventListener('click', buscarRelatorio);
btnLogout.addEventListener('click', fazerLogout);

btnExportPdf.addEventListener('click', async () => {
    if (!markdownAtual) {
        alert("Não há relatório para exportar.");
        return;
    }

    // Muda o texto do botão para avisar que está gerando
    const textoOriginal = btnExportPdf.innerHTML;
    btnExportPdf.innerHTML = '<i class="ph ph-spinner-gap spin"></i> Gerando PDF Oficial...';
    btnExportPdf.disabled = true;

    const token = localStorage.getItem("jwt_token");

    try {
        const resposta = await fetch(`${API_URL}/pdf/export`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}` 
            },
            body: JSON.stringify({ 
                markdown: markdownAtual, 
                termo: termoAtual 
            })
        });

        if (!resposta.ok) throw new Error("Falha ao gerar PDF no servidor");

        //Transforma a resposta do Java em um arquivo "baixável"
        const blob = await resposta.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Relatorio_${termoAtual.replace(/\s+/g, '_')}.pdf`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);

    } catch (erro) {
        alert("Erro ao exportar PDF: " + erro.message);
    } finally {
        // Restaura o botão
        btnExportPdf.innerHTML = textoOriginal;
        btnExportPdf.disabled = false;
    }
});

tabSearch.addEventListener('click', () => trocarAba('search'));
tabHistory.addEventListener('click', () => {
    trocarAba('history');
    carregarHistorico();
});

senhaInput.addEventListener('keypress', (e) => { if (e.key === 'Enter') fazerLogin(); });
searchInput.addEventListener('keypress', (e) => { if (e.key === 'Enter') buscarRelatorio(); });

// FUNÇÕES DE AUTENTICAÇÃO
async function fazerCadastro() {
    const login = loginInput.value.trim();
    const senha = senhaInput.value.trim();
    if (!login || !senha) return mostrarMensagem("Preencha login e senha.", "var(--danger)");

    try {
        const resposta = await fetch(`${API_URL}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ login, senha })
        });
        const dados = await resposta.text();
        if (resposta.ok) {
            mostrarMensagem("✅ Sucesso! Agora clique em Entrar.", "var(--success)");
        } else {
            mostrarMensagem("❌ " + dados, "var(--danger)");
        }
    } catch (erro) {
        mostrarMensagem("Erro de conexão com o servidor.", "var(--danger)");
    }
}

async function fazerLogin() {
    const login = loginInput.value.trim();
    const senha = senhaInput.value.trim();
    if (!login || !senha) return mostrarMensagem("Preencha login e senha.", "var(--danger)");
    try {
        const resposta = await fetch(`${API_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ login, senha })
        });
        const token = await resposta.text();
        if (resposta.ok) {
            localStorage.setItem("jwt_token", token);
            loginInput.value = '';
            senhaInput.value = '';
            mostrarApp();
        } else {
            mostrarMensagem("❌ Login ou senha incorretos.", "var(--danger)");
        }
    } catch (erro) {
        mostrarMensagem("Erro de conexão com o servidor.", "var(--danger)");
    }
}

function fazerLogout() {
    localStorage.removeItem("jwt_token");
    reportContainer.classList.add('hidden');
    searchInput.value = '';
    mostrarAuth();
}

// FUNÇÕES DE PESQUISA E FEEDBACK VISUAL (REFATORADO)
async function buscarRelatorio() {
    const query = searchInput.value.trim();
    const token = localStorage.getItem("jwt_token");
    
    if (!query) {
        alert("Por favor, digite o nome do objeto para pesquisar.");
        return;
    }

    // --- INÍCIO DO LOADING (TRAVA O BOTÃO E ESCURECE O FUNDO) ---
    loadingDiv.classList.remove('hidden');
    btnSearch.disabled = true;
    btnSearch.innerHTML = '<i class="ph ph-spinner-gap spin"></i> Analisando...';
    btnSearch.style.cursor = "wait";
    btnSearch.style.opacity = "0.7";
    
    // Se já havia um relatório na tela, deixa ele opaco para indicar que está atualizando
    if (!reportContainer.classList.contains('hidden')) {
        reportContainer.style.opacity = "0.3"; 
    }

    try {
        const resposta = await fetch(`${API_URL}/comparator/report?q=${encodeURIComponent(query)}`, {
            method: 'GET',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (resposta.status === 401 || resposta.status === 403) {
            alert("Sua sessão expirou por segurança. Faça login novamente.");
            fazerLogout();
            return;
        }
        
        const textoResposta = await resposta.text();
        
        if (resposta.ok) {
            markdownAtual = textoResposta;
            termoAtual = query;
            let nomeUsuario = "Responsável pela Pesquisa";
            try {
                const payloadDecodificado = JSON.parse(atob(token.split('.')[1]));
                nomeUsuario = payloadDecodificado.sub || payloadDecodificado.login || "Auditor"; 
            } catch(e) {}
            
            const dataAtual = new Date().toLocaleString('pt-BR');
            const carimboAuditoria = `
                <div style="margin-bottom: 25px; padding: 15px; background-color: var(--bg-light); border-left: 4px solid var(--primary); border-radius: 4px; font-family: monospace; font-size: 0.95rem;">
                    <strong style="color: var(--text-main); font-size: 1.1rem;">🏛️ RELATÓRIO TÉCNICO DE ESTIMATIVA DE PREÇOS</strong><br><br>
                    <b>Pesquisador Responsável:</b> <span style="color: var(--primary); text-transform: uppercase;">${nomeUsuario}</span><br>
                    <b>Data e Hora da Emissão:</b> ${dataAtual}<br>
                </div>
            `;
            
            reportContent.innerHTML = carimboAuditoria + marked.parse(textoResposta);
            reportContainer.classList.remove('hidden');
        } else {
            let mensagemLimpa = textoResposta;
            try {
                const jsonErro = JSON.parse(textoResposta);
                if (jsonErro.message) mensagemLimpa = jsonErro.message;
            } catch (e) {}
            reportContent.innerHTML = `<p style="color: var(--danger); font-weight: bold;">❌ ${mensagemLimpa}</p>`;
            reportContainer.classList.remove('hidden');
        }
    } catch (erro) {
        reportContent.innerHTML = `<p style="color: var(--danger);">Erro crítico de conexão com o servidor.</p>`;
        reportContainer.classList.remove('hidden');
    } finally {
        // --- FIM DO LOADING (LIBERA O BOTÃO INDEPENDENTE DE ERRO OU SUCESSO) ---
        loadingDiv.classList.add('hidden');
        btnSearch.disabled = false;
        btnSearch.innerHTML = 'Pesquisar';
        btnSearch.style.cursor = "pointer";
        btnSearch.style.opacity = "1";
        reportContainer.style.opacity = "1";
    }
}

// FUNÇÕES DE HISTÓRICO
async function carregarHistorico() {
    historyList.innerHTML = '<p class="text-muted"><i class="ph ph-spinner-gap spin"></i> Buscando histórico no banco de dados...</p>';
    const token = localStorage.getItem("jwt_token");
    try {
        const resposta = await fetch(`${API_URL}/comparator/history`, {
            method: 'GET',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (resposta.status === 401 || resposta.status === 403) {
            alert("Sua sessão expirou por segurança. Faça login novamente.");
            fazerLogout();
            return;
        }
        
        const relatorios = await resposta.json();
        
        if (relatorios.length === 0) {
            historyList.innerHTML = `
                <div style="text-align: center; padding: 40px; color: var(--text-muted);">
                    <i class="ph ph-folder-open" style="font-size: 3rem; margin-bottom: 10px;"></i>
                    <p>Você ainda não tem nenhum relatório salvo.</p>
                </div>`;
            return;
        }
        
        historyList.innerHTML = '';
        
        relatorios.forEach(relatorio => {
            const dataObjeto = new Date(relatorio.creationDate);
            const dataFormatada = dataObjeto.toLocaleDateString('pt-BR') + ' às ' + dataObjeto.toLocaleTimeString('pt-BR', {hour: '2-digit', minute:'2-digit'});

            const nomeUsuario = relatorio.username ? relatorio.username : 'Sistema';
            const card = document.createElement('div');
            card.style.border = '1px solid var(--border)';
            card.style.borderRadius = '8px';
            card.style.marginBottom = '10px';
            card.style.overflow = 'hidden';
            card.style.background = 'white';
            
            const header = document.createElement('div');
            header.style.padding = '15px';
            header.style.cursor = 'pointer';
            header.style.display = 'flex';
            header.style.justifyContent = 'space-between';
            header.style.alignItems = 'center';
            header.style.fontWeight = 'bold';
            
            header.innerHTML = `
                <span style="color: var(--primary);">
                    <i class="ph ph-file-text"></i> ${relatorio.searchedTerm}
                </span> 
                <span style="font-size: 0.85rem; color: var(--text-muted); font-weight: normal;">
                    <i class="ph ph-user"></i> <b>${nomeUsuario}</b> &nbsp;|&nbsp; ${dataFormatada} <i class="ph ph-caret-down"></i>
                </span>
            `;
            
            const content = document.createElement('div');
            content.className = 'markdown-body';
            content.style.padding = '20px';
            content.style.background = 'var(--bg-light)';
            content.style.display = 'none';
            content.style.borderTop = '1px solid var(--border)';
            content.innerHTML = marked.parse(relatorio.contentMarkdown);
            
            header.onclick = () => {
                const estaFechado = content.style.display === 'none';
                content.style.display = estaFechado ? 'block' : 'none';
                header.style.background = estaFechado ? 'var(--bg-light)' : 'white';
            };
            
            card.appendChild(header);
            card.appendChild(content);
            historyList.appendChild(card);
        });
    } catch (erro) {
        historyList.innerHTML = '<p style="color: var(--danger);">Erro ao carregar o histórico.</p>';
    }
}

// CONTROLE DE INTERFACE
function mostrarApp() {
    authScreen.classList.remove('active');
    authScreen.classList.add('hidden');
    appScreen.classList.remove('hidden');
    appScreen.classList.add('active');
    trocarAba('search'); 
}
function mostrarAuth() {
    appScreen.classList.remove('active');
    appScreen.classList.add('hidden');
    authScreen.classList.remove('hidden');
    authScreen.classList.add('active');
    authMessage.textContent = '';
}
function mostrarMensagem(texto, cor) {
    authMessage.textContent = texto;
    authMessage.style.color = cor;
}
function trocarAba(aba) {
    if (aba === 'search') {
        tabSearch.classList.add('active');
        tabHistory.classList.remove('active');
        viewSearch.classList.add('active');
        viewSearch.classList.remove('hidden');
        viewHistory.classList.add('hidden');
        viewHistory.classList.remove('active');
    } else if (aba === 'history') {
        tabHistory.classList.add('active');
        tabSearch.classList.remove('active');
        viewHistory.classList.add('active');
        viewHistory.classList.remove('hidden');
        viewSearch.classList.add('hidden');
        viewSearch.classList.remove('active');
    }
}