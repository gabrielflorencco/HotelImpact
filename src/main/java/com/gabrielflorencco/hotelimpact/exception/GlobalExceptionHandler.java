package com.gabrielflorencco.hotelimpact.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Tratamento centralizado de erros. Mensagens exibidas ao usuário nunca revelam
 * detalhes internos; erros inesperados são registrados no log com stack trace completo.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RegistroNaoEncontradoException.class)
    public ModelAndView registroNaoEncontrado(RegistroNaoEncontradoException ex, HttpServletRequest req) {
        return paginaErro(HttpStatus.NOT_FOUND, "Registro não encontrado", ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView recursoInexistente(NoResourceFoundException ex) {
        return paginaErro(HttpStatus.NOT_FOUND, "Página não encontrada",
                "O endereço acessado não existe ou foi movido.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView argumentoInvalido(MethodArgumentTypeMismatchException ex) {
        return paginaErro(HttpStatus.BAD_REQUEST, "Requisição inválida",
                "Um parâmetro da requisição está em formato incorreto.");
    }

    @ExceptionHandler(NegocioException.class)
    public ModelAndView regraDeNegocio(NegocioException ex) {
        return paginaErro(HttpStatus.UNPROCESSABLE_ENTITY, "Operação não permitida", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView erroInesperado(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado ao processar {} {}", req.getMethod(), req.getRequestURI(), ex);
        return paginaErro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente e, se o problema persistir, contate o suporte.");
    }

    private ModelAndView paginaErro(HttpStatus status, String titulo, String mensagem) {
        ModelAndView mav = new ModelAndView("error/erro-generico");
        mav.setStatus(status);
        mav.addObject("titulo", titulo);
        mav.addObject("mensagem", mensagem);
        return mav;
    }
}
