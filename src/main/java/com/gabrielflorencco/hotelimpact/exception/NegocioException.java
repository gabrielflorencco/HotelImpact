package com.gabrielflorencco.hotelimpact.exception;

/**
 * Violação de regra de negócio. A mensagem é segura para exibição ao usuário
 * (não contém detalhes internos, SQL ou stack traces).
 */
public class NegocioException extends RuntimeException {

    public NegocioException(String mensagem) {
        super(mensagem);
    }
}
