package com.valuecomparison.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;

@Service
public class PdfService {

    public byte[] generatePdfFromMarkdown(String markdown, String nomeUsuario, String dataEmissao) throws Exception {

        //Configura o conversor de Markdown (Habilitando a leitura de Tabelas)
        List<Extension> extensions = Collections.singletonList(TablesExtension.create());
        Parser parser = Parser.builder().extensions(extensions).build();
        HtmlRenderer renderer = HtmlRenderer.builder().extensions(extensions).build();

        //Força uma linha em branco antes da tabela e antes da conclusão
        //caso a IA tenha esquecido, salvando a formatação do PDF.
        String markdownSeguro = markdown
                .replace("| Item | Fornecedor |", "\n\n| Item | Fornecedor |")
                .replace("**4. Metodologia", "\n\n**4. Metodologia")
                .replace("**5. Conclusão", "\n\n**5. Conclusão");

        //Converte o texto Markdown da IA para HTML
        Node document = parser.parse(markdownSeguro);
        String htmlDaIA = renderer.render(document);

        //Monta o "Papel Timbrado" do órgão com CSS (XHTML Estrito)
        //OpenHtmlToPdf exige tags fechadas, como <br/> e <meta />
        String htmlCompleto = """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8"/>
                    <style>
                        body {
                            font-family: sans-serif;
                            font-size: 12px;
                            color: #333;
                            line-height: 1.5;
                            margin: 20px;
                        }
                        .carimbo {
                            border-left: 4px solid #4f46e5;
                            background-color: #f8fafc;
                            padding: 15px;
                            margin-bottom: 20px;
                            font-family: monospace;
                        }
                        h1, h2, h3 {
                            color: #1e293b;
                        }
                        h1 {
                            border-bottom: 2px solid #4f46e5;
                            padding-bottom: 5px;
                            font-size: 18px;
                        }
                        table {
                            width: 100%%;
                            border-collapse: collapse;
                            margin-top: 15px;
                            margin-bottom: 15px;
                        }
                        th, td {
                            border: 1px solid #cbd5e1;
                            padding: 8px;
                            text-align: left;
                        }
                        th {
                            background-color: #f1f5f9;
                            font-weight: bold;
                        }
                        .footer {
                            position: fixed;
                            bottom: -20px;
                            width: 100%%;
                            text-align: center;
                            font-size: 10px;
                            color: #94a3b8;
                            border-top: 1px solid #e2e8f0;
                            padding-top: 10px;
                        }
                        /* Configuração para o tamanho da página A4 */
                        @page {
                            size: A4 portrait;
                            margin: 15mm;
                        }
                    </style>
                </head>
                <body>
                    <div class="carimbo">
                        <strong style="font-size: 14px;">Registro de Preços</strong><br/>
                        <br/>
                        <b>Pesquisador Responsável:</b> %s<br/>
                        <b>Data e Hora da Emissão:</b> %s
                    </div>
                
                    %s
                
                    <div class="footer">
                        Relatório de Preços gerado automaticamente por IA sob a égide da Lei nº 14.133/2021.
                    </div>
                </body>
                </html>
                """.formatted(nomeUsuario, dataEmissao, htmlDaIA);

        //Transforma o HTML Completo em um Arquivo PDF (Array de Bytes)
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlCompleto, null);
            builder.toStream(outputStream);
            builder.run();

            return outputStream.toByteArray();
        }
    }
}