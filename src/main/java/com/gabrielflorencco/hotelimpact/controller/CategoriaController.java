package com.gabrielflorencco.hotelimpact.controller;

import jakarta.servlet.http.HttpServletRequest;
import com.gabrielflorencco.hotelimpact.dto.CategoriaForm;
import jakarta.validation.Valid;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.service.CategoriaQuartoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Navegação e formulários de categorias de quarto. Regras de negócio no service;
 * aqui apenas validação de formulário, mensagens e redirecionamentos (POST-redirect-GET).
 */
@Controller
@RequestMapping("/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaQuartoService service;

    @GetMapping
    public String listar(HttpServletRequest request, Model model) {
        model.addAttribute("categorias", service.listar());
        model.addAttribute("titulo", "Categorias de quarto");
        return Pagina.renderizar(request, "categorias/lista");
    }

    @GetMapping("/novo")
    public String novo(HttpServletRequest request, Model model) {
        model.addAttribute("form", new CategoriaForm());
        model.addAttribute("titulo", "Nova categoria");
        return Pagina.renderizar(request, "categorias/formulario");
    }

    @PostMapping
    public String criar(HttpServletRequest request, @Valid @ModelAttribute("form") CategoriaForm form, BindingResult result, Model model,
                        RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("titulo", "Nova categoria");
            return Pagina.renderizar(request, "categorias/formulario");
        }
        try {
            service.salvar(form);
        } catch (NegocioException ex) {
            model.addAttribute("titulo", "Nova categoria");
            model.addAttribute("erro", ex.getMessage());
            return Pagina.renderizar(request, "categorias/formulario");
        }
        redirect.addAttribute("sucesso", "Categoria cadastrada com sucesso.");
        return "redirect:/categorias";
    }

    @GetMapping("/{id}/editar")
    public String editar(HttpServletRequest request, @PathVariable Long id, Model model) {
        model.addAttribute("form", CategoriaForm.da(service.buscarPorId(id)));
        model.addAttribute("titulo", "Editar categoria");
        return Pagina.renderizar(request, "categorias/formulario");
    }

    @PostMapping("/{id}")
    public String atualizar(HttpServletRequest request, @PathVariable Long id, @Valid @ModelAttribute("form") CategoriaForm form,
                            BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("titulo", "Editar categoria");
            return Pagina.renderizar(request, "categorias/formulario");
        }
        form.setId(id);
        try {
            service.salvar(form);
        } catch (NegocioException ex) {
            model.addAttribute("titulo", "Editar categoria");
            model.addAttribute("erro", ex.getMessage());
            return Pagina.renderizar(request, "categorias/formulario");
        }
        redirect.addAttribute("sucesso", "Categoria atualizada com sucesso.");
        return "redirect:/categorias";
    }

    @PostMapping("/{id}/alternar-ativo")
    public String alternarAtivo(HttpServletRequest request, @PathVariable Long id, RedirectAttributes redirect) {
        boolean ativo = service.alternarAtivo(id);
        redirect.addAttribute("sucesso",
                ativo ? "Categoria reativada." : "Categoria desativada — quartos existentes foram preservados.");
        return "redirect:/categorias";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(HttpServletRequest request, @PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.excluir(id);
            redirect.addAttribute("sucesso", "Categoria excluída.");
        } catch (NegocioException ex) {
            redirect.addAttribute("erro", ex.getMessage());
        }
        return "redirect:/categorias";
    }
}
