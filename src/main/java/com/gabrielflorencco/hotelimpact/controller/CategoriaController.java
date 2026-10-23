package com.gabrielflorencco.hotelimpact.controller;

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
    public String listar(Model model) {
        model.addAttribute("categorias", service.listar());
        model.addAttribute("titulo", "Categorias de quarto");
        return "categorias/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new CategoriaForm());
        model.addAttribute("titulo", "Nova categoria");
        return "categorias/formulario";
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") CategoriaForm form, BindingResult result, Model model,
                        RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("titulo", "Nova categoria");
            return "categorias/formulario";
        }
        try {
            service.salvar(form);
        } catch (NegocioException ex) {
            model.addAttribute("titulo", "Nova categoria");
            model.addAttribute("erro", ex.getMessage());
            return "categorias/formulario";
        }
        redirect.addAttribute("sucesso", "Categoria cadastrada com sucesso.");
        return "redirect:/categorias";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", CategoriaForm.da(service.buscarPorId(id)));
        model.addAttribute("titulo", "Editar categoria");
        return "categorias/formulario";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") CategoriaForm form,
                            BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("titulo", "Editar categoria");
            return "categorias/formulario";
        }
        form.setId(id);
        try {
            service.salvar(form);
        } catch (NegocioException ex) {
            model.addAttribute("titulo", "Editar categoria");
            model.addAttribute("erro", ex.getMessage());
            return "categorias/formulario";
        }
        redirect.addAttribute("sucesso", "Categoria atualizada com sucesso.");
        return "redirect:/categorias";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.excluir(id);
            redirect.addAttribute("sucesso", "Categoria excluída.");
        } catch (NegocioException ex) {
            redirect.addAttribute("erro", ex.getMessage());
        }
        return "redirect:/categorias";
    }
}
