package com.gabrielflorencco.hotelimpact.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * Sessão expirada na navegação SPA: quando um pedido htmx chega sem
 * autenticação, o Spring Security não pode simplesmente redirecionar — o
 * htmx injetaria a tela de login dentro do conteúdo administrativo. Aqui a
 * resposta é um header HX-Redirect, que faz o htmx encaminhar o navegador
 * inteiro para o login. Sem header (acesso direto), o comportamento padrão
 * de redirecionamento permanece.
 */
public class HxRedirectEntryPoint extends LoginUrlAuthenticationEntryPoint {

    public HxRedirectEntryPoint(String loginFormUrl) {
        super(loginFormUrl);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException)
            throws java.io.IOException, jakarta.servlet.ServletException {
        if ("true".equals(request.getHeader("HX-Request"))) {
            response.setHeader("HX-Redirect", getLoginFormUrl());
            response.setStatus(HttpServletResponse.SC_SEE_OTHER);
            return;
        }
        super.commence(request, response, authException);
    }
}
