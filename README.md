# 🏛️ Value Comparator

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-47A248.svg)](https://www.mongodb.com/)
[![Gemini API](https://img.shields.io/badge/Google_Gemini-2.5_Flash-blue.svg)](https://ai.google.dev/)

## 📖 Visão Geral do Projeto
O **Value Comparator** é uma aplicação Full-Stack desenvolvida para modernizar, auditar e automatizar a etapa do Parâmetro 3 do Decreto 67.888/2023 do estado de São Paulo de Pesquisa de Preços em processos de licitação pública. 

O sistema realiza buscas de preços em tempo real na internet e utiliza Inteligência Artificial (Google Gemini) para atuar como um Agente de Contratação. A IA filtra os resultados aplicando regras estritas da **Lei de Licitações (Lei nº 14.133/2021)**, calcula o valor de referência e gera um **relatório técnico oficial exportável em PDF**, gerado nativamente no back-end com carimbo de auditoria vinculado ao usuário logado, garantindo inviolabilidade do documento.

---

## 🛠️ Stack Tecnológica

| Camada | Tecnologia | Propósito |
| :--- | :--- | :--- |
| **Back-end** | Java 21 + Spring Boot 3 | Motor principal, rotas da API RESTful e injeção de dependências. |
| **Segurança** | Spring Security + JWT | Autenticação, blindagem de rotas com sessão de 8h e identificação para auditoria. |
| **Banco de Dados** | MongoDB (Atlas) | Armazenamento de usuários e persistência do histórico de relatórios (NoSQL). |
| **IA & Lógica** | Google Gemini API | Processamento de linguagem natural, filtragem de dados e formatação jurídica. |
| **Integrações** | JSoup / SerpApi | Web Scraping para coleta automatizada de preços do Google Shopping. |
| **Geração de PDF** | OpenHTMLToPDF + CommonMark | Conversão de Markdown para HTML e geração de PDF nativo A4 diretamente no servidor. |
| **Front-end** | HTML5, CSS3, Vanilla JS | Interface Single Page Application (SPA) responsiva consumindo Fetch API. |

---

## ⚙️ Arquitetura e Fluxo do Sistema
O projeto foi estruturado no formato Monorepo, mantendo o Front-end desacoplado em uma arquitetura orientada a serviços.

1. **Autenticação:** O Front-end envia as credenciais e recebe um Token JWT válido por um expediente de trabalho (8 horas).
2. **Coleta de Dados:** O usuário envia um termo de busca. O Controller valida o Token e aciona o `ScraperService` para raspar os preços reais na web.
3. **Análise Jurídica (IA):** A lista bruta é enviada ao `GeminiService`, que aplica um rigoroso *Prompt Engineering* para limpar os dados, formatando o texto em Markdown seguro.
4. **Persistência:** O Controller intercepta o relatório pronto, identifica o usuário logado via Contexto de Segurança e salva a operação no MongoDB (`ReportRepository`).
5. **Geração Inviolável de Documentos:** O cliente solicita a exportação. O `PdfService` atua no Back-end convertendo o Markdown, injetando CSS XHTML estrito com carimbos oficiais e devolvendo um PDF em `byte[]`, blindando o sistema contra edições no navegador (F12).

---

## ✨ Funcionalidades Principais
* **Segurança JWT Corporativa:** Sistema de Login/Registro com senhas criptografadas (BCrypt) e rotação de chaves via variáveis de ambiente.
* **Tratamento de Exceções Global:** Uso avançado de `@RestControllerAdvice` no Java para interceptar erros críticos de regras de negócio e formatá-los em JSON, refletindo alertas visuais amigáveis no Front-end (UI/UX).
* **Histórico em Acordeão:** Relatórios salvos na nuvem podem ser consultados na aba "Histórico", exibindo timestamp e o pesquisador responsável.
* **Feedback Visual Assíncrono:** Botões de loading interativos que acompanham o ciclo de vida das promessas HTTP (Fetch).

---

## ⚖️ Regras de Negócio Implementadas (IA)
A Inteligência Artificial atua como um funil jurídico com as seguintes travas:
* **Regra de Sanidade:** Textos sem nexo disparam um gatilho de invalidez instantânea.
* **Vedação a Marketplaces:** Itens do Mercado Livre, Amazon, etc., são sinalizados de vermelho e **descartados** do cálculo de média final.
* **Alerta Inexequível:** Preços absurdamente baixos fora do padrão promocional recebem a tag de alerta (`⚠️`) para diligência humana do Agente de Contratação.

---

## 📸 Demonstração do Sistema

📺 **[Clique aqui para assistir ao vídeo de demonstração do sistema rodando]** *(Insira o link do seu vídeo do YouTube aqui)*

*(adicionar aqui 2 ou 3 screenshots do projeto)*

---

## 💻 Como rodar este projeto localmente

*Nota de Arquitetura: Visando a gestão inteligente de recursos e limites de cotas gratuitas das APIs (SerpApi e Gemini), o deploy contínuo em produção não está exposto publicamente.*

**Pré-requisitos:** Java 21+, Maven, IDE (IntelliJ/Eclipse) e chaves de API (Google AI Studio e SerpApi).

1. Clone este repositório:
   ```bash
   git clone [https://github.com/SeuUsuario/Value-Comparison.git](https://github.com/SeuUsuario/Value-Comparison.git)

2. O arquivo application.properties está configurado para ler variáveis do sistema visando a segurança (DevSecOps). Na sua IDE, configure as seguintes Environment Variables (Variáveis de Ambiente) nas opções de inicialização do projeto:
   MONGODB_URI=mongodb+srv://<usuario>:<senha>@<seu-cluster>...
   JWT_SECRET=SuaSenhaSecretaSuperForte
   GEMINI_API_KEY=SuaChaveDoGeminiAqui
   SERP_API_KEY=SuaChaveDoSerpApiAqui

3. Execute a classe principal ValueComparisonApplication.java.

4. Abra o arquivo index.html (Front-end) diretamente no navegador ou utilize a extensão Live Server.
