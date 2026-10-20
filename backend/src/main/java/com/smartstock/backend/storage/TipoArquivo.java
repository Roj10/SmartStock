package com.smartstock.backend.storage;

/** Identifica o tipo do arquivo pelo conteúdo (os primeiros bytes), não pelo nome nem pelo tipo informado. */
public final class TipoArquivo {

    private TipoArquivo() {
    }

    /** Tipo MIME de PDF e imagens; qualquer outra coisa é "application/octet-stream". */
    public static String detectar(byte[] b) {
        if (b.length >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') {
            return "application/pdf";
        }
        if (b.length >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 4 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
            return "image/gif";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W'
                && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    public static boolean ehImagem(String tipo) {
        return tipo.startsWith("image/");
    }

    /** Extensão (com ponto) usada no nome do arquivo guardado. */
    public static String extensao(String tipo) {
        return switch (tipo) {
            case "application/pdf" -> ".pdf";
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }

    /** Tipo pela extensão do nome (usado ao migrar arquivos antigos e imagens de exemplo). */
    public static String peloNome(String nome) {
        String n = nome.toLowerCase();
        if (n.endsWith(".pdf")) return "application/pdf";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".gif")) return "image/gif";
        if (n.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}
