package com.smartstock.backend.storage;

/**
 * Onde ficam os arquivos do sistema (fotos dos itens, imagens das mensagens e anexos dos projetos).
 * Em uso normal é o Supabase Storage; no perfil "h2" (sem internet / testes) é uma pasta local.
 */
public interface Armazenamento {

    /** Fotos dos itens: bucket público (as imagens abrem direto pelo endereço). */
    String PASTILHAS = "pastilhas";
    /** Imagens das conversas: privado, só o backend lê (e entrega a quem enviou ou recebeu). */
    String MENSAGENS = "mensagens";
    /** Arquivos dos projetos: privado, só o backend lê (e entrega a quem tem acesso ao Progresso). */
    String PROJETOS = "projetos";

    void enviar(String bucket, String caminho, byte[] conteudo, String tipo);

    /** Conteúdo do arquivo; lança ResourceNotFoundException se ele não existir. */
    byte[] baixar(String bucket, String caminho);

    /** Melhor esforço: não lança erro se o arquivo já não existir. */
    void apagar(String bucket, String caminho);

    void copiar(String bucket, String origem, String destino);

    /** Endereço para exibir o arquivo de um bucket público (usado nas fotos dos itens). */
    String urlPublica(String bucket, String caminho);

    /** Nome do arquivo a partir de um endereço gerado por {@link #urlPublica}. */
    static String nomeNaUrl(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }
}
