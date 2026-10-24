package com.gabrielflorencco.hotelimpact.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Integração htmx: em cada resposta de fragmento (HX-Request presente), informa
 * ao htmx a URL que o navegador deve exibir. Assim o histórico, o voltar e os
 * links diretos continuam corretos mesmo com POST que redireciona (o header é
 * emitido na requisição final, já na URL de destino).
 */
public class HxUrlInterceptor implements HandlerInterceptor {

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        if ("true".equals(request.getHeader("HX-Request"))) {
            String uri = request.getRequestURI();
            String query = request.getQueryString();
            response.setHeader("HX-Push-Url", query == null ? uri : uri + "?" + query);
        }
    }
}
