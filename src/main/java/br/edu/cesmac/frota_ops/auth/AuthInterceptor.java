package br.edu.cesmac.frota_ops.auth;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Garante que apenas usuários autenticados acessem as rotas protegidas.
 *
 * <p>O login grava o usuário na sessão sob o atributo {@code usuarioLogado}. Se esse
 * atributo não existir, a requisição é interrompida e o navegador é redirecionado para a
 * página de login. As rotas públicas (login, cadastro e recursos estáticos) ficam de fora
 * do interceptador — ver {@link WebConfig}.</p>
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        HttpSession session = request.getSession(false);
        Object usuario = session == null ? null : session.getAttribute("usuarioLogado");
        if (usuario != null) {
            return true;
        }

        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }
}
