package br.edu.cesmac.frota_ops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Tratamento global de exceções para capturar erros comuns de entrada do usuário
 * (chaves duplicadas, IDs inválidos, erros de conversão de parâmetros) e apresentar
 * mensagens amigáveis em vez da Whitelabel Error Page (HTTP 500/400).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String tratarViolacaoDeIntegridade(DataIntegrityViolationException ex,
                                             HttpServletRequest request,
                                             RedirectAttributes redirectAttributes) {
        log.warn("Tentativa de violação de integridade de dados: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("erro",
                "Operação não permitida: os dados informados já existem no sistema ou violam regras de integridade (ex.: placa, CPF, CNH ou e-mail duplicado).");
        return redirecionarComSeguranca(request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String tratarTipoInvalido(MethodArgumentTypeMismatchException ex,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        log.warn("Parâmetro de requisição com tipo inválido: {} = {}", ex.getName(), ex.getValue());
        redirectAttributes.addFlashAttribute("erro", "Parâmetro informado na requisição é inválido.");
        return redirecionarComSeguranca(request);
    }

    private String redirecionarComSeguranca(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }
        return "redirect:/";
    }
}
