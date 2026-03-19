## 🏛️ Value Comparator

## 📖 Visão Geral do Projeto
O Value Comparator PRO é uma aplicação Full-Stack desenvolvida para modernizar, auditar e automatizar a etapa de Pesquisa de Preços em processos de licitação pública.
O sistema realiza buscas de preços em tempo real na internet e utiliza Inteligência Artificial (Google Gemini) para atuar como um Agente de Contratação. A IA filtra os resultados aplicando regras estritas da Lei de Licitações (excluindo marketplaces e fontes repetidas), calcula o valor de referência e gera um relatório técnico oficial exportável em PDF, com carimbo de auditoria vinculado ao usuário logado.

## 🛠️ Stack Tecnológica

| Camada | Tecnologia | Propósito |
| --- | --- | --- |
| **Back-end** | Java + Spring Boot | Motor principal, rotas da API RESTful e injeção de dependências. |
| **Segurança** | Spring Security + JWT | Autenticação, blindagem de rotas e identificação para auditoria. |
| **Banco de Dados** | MongoDB (NoSQL) | Armazenamento de usuários e persistência de relatórios. |
| **IA** | Google Gemini API | Processamento de linguagem natural e formatação. |
| **Integrações** | JSoup / SerpApi | Web Scraping para coleta automatizada de preços. |
| **Front-end** | HTML5, CSS3, JS | Interface Single Page Application (SPA) responsiva. |
| **Bibliotecas** | Marked.js, html2pdf.js | Conversão Markdown para HTML e exportação para PDF. |

## ⚙️ Arquitetura do Sistema
O projeto foi estruturado no formato Monorepo, mantendo o Front-end desacoplado e contido em uma pasta independente dentro do repositório Java.

O fluxo de dados obedece a seguinte ordem:
1. O Front-end (Cliente) envia as credenciais de login e recebe um Token JWT.
2. O usuário envia um termo de busca (ex: "Monitor 24 polegadas") com o Token no cabeçalho.
3. O Controller valida o Token e aciona o ScraperService para buscar preços na web.
4. A lista bruta de preços é enviada ao GeminiService, que aplica o Prompt Engineering para limpar os dados e formatar o texto.
5. O Controller intercepta o relatório pronto, identifica o usuário logado e salva tudo no MongoDB através do ReportRepository.
6. O texto volta para o Front-end, que desenha as tabelas, injeta cores (verde para válido, vermelho para inválido) e disponibiliza o download do PDF.


## ✨ Funcionalidades Principais

 ° Autenticação Segura: Sistema de Login e Registro com senhas criptografadas e controle de sessão via JSON Web Tokens (JWT).
 ° Auditoria de Preços com IA: O relatório gerado obedece à Lei nº 14.133/2021, justificando juridicamente a escolha do Menor Preço ou Mediana.
 ° Geração Dinâmica de PDF: Exportação do relatório em formato A4, contendo um cabeçalho oficial de auditoria (com nome do usuário responsável, data, hora e hash de validação).
 ° Histórico em Acordeão: Todo relatório gerado fica salvo na nuvem e pode ser consultado na aba "Histórico", exibindo quando e quem realizou a pesquisa.
 ° Tratamento de Exceções UI/UX: Respostas visuais amigáveis no Front-end para links expirados, pesquisas sem sentido ou falhas de conexão.

 
## ⚖️ Regras de Negócio Implementadas (IA)
 A Inteligência Artificial foi configurada com restrições severas para atuar no setor público:

 1. Regra de Sanidade: Textos sem nexo (ex: "asdasd") disparam um gatilho de invalidez instantânea.
 2. Vedação a Marketplaces: Produtos do Mercado Livre, Amazon, Magalu, etc., são mapeados e listados na tabela, mas são descartados do cálculo final de referência.
 3. Diversidade de Fontes: A IA é proibida de usar o mesmo fornecedor mais de uma vez na formação de preços, filtrando apenas a opção mais barata de cada loja.
