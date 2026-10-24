package com.gabrielflorencco.hotelimpact.controller;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Decisão de renderização por requisição: quando o pedido vem do htmx
 * (header HX-Request), o controller devolve apenas o fragmento "conteudo"
 * da página — o layout (sidebar, topbar, tema) permanece intacto e a
 * navegação não recarrega o documento. Sem o header (acesso direto,
 * testes, links sem JavaScript), a página inteira é renderizada.
 */
public final class Pagina {

    private Pagina() {
    }

    public static String renderizar(HttpServletRequest request, String view) {
        return "true".equals(request.getHeader("HX-Request")) ? view + " :: conteudo" : view;
    }
}
