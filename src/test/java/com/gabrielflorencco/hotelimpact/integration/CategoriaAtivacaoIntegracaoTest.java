package com.gabrielflorencco.hotelimpact.integration;

import com.gabrielflorencco.hotelimpact.dto.CategoriaForm;
import com.gabrielflorencco.hotelimpact.dto.QuartoForm;
import com.gabrielflorencco.hotelimpact.entity.CategoriaQuarto;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.repository.CategoriaQuartoRepository;
import com.gabrielflorencco.hotelimpact.service.CategoriaQuartoService;
import com.gabrielflorencco.hotelimpact.service.QuartoService;
import com.gabrielflorencco.hotelimpact.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ativação/desativação de categorias (BR-CAT-003): categoria inativa não
 * recebe novos quartos, mas quartos existentes e históricos são
 * preservados — a mensagem de exclusão finalmente tem a ação que recomenda.
 */
@SpringBootTest
class CategoriaAtivacaoIntegracaoTest extends AbstractIntegrationTest {

    @Autowired
    private CategoriaQuartoService categoriaService;
    @Autowired
    private QuartoService quartoService;
    @Autowired
    private CategoriaQuartoRepository categorias;

    private Long categoria(String nome) {
        CategoriaForm form = new CategoriaForm();
        form.setNome(nome + " " + System.nanoTime());
        form.setCapacidadeMaxima(2);
        form.setQuantidadeCamas(1);
        form.setPrecoBase(new BigDecimal("180.00"));
        form.setAtivo(true);
        return categoriaService.salvar(form).getId();
    }

    @Test
    void desativarCategoriaPreservaQuartosEbloqueiaNovosVinculos() {
        Long id = categoria("Ativacao");
        assertThat(categorias.findById(id).orElseThrow().getAtivo()).isTrue();

        // quartos existentes continuam vinculados
        assertThat(categoriaService.alternarAtivo(id)).isFalse(); // desativou
        assertThat(categorias.findById(id).orElseThrow().getAtivo()).isFalse();

        // novo quarto com categoria inativa é bloqueado
        QuartoForm quarto = new QuartoForm();
        quarto.setNumero("CA" + System.nanoTime() % 100000);
        quarto.setCategoriaId(id);
        quarto.setAndar(1);
        quarto.setAtivo(true);
        assertThatThrownBy(() -> quartoService.salvar(quarto))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("está inativa. Reative-a para utilizá-la em quartos.");

        // reativação libera novamente
        assertThat(categoriaService.alternarAtivo(id)).isTrue();
        assertThat(quartoService.salvar(quarto).getId()).isNotNull();
    }

    @Test
    void desativarNaoAlteraHistoricoDeQuartosExistentes() {
        Long id = categoria("Historico");
        QuartoForm quarto = new QuartoForm();
        quarto.setNumero("CH" + System.nanoTime() % 100000);
        quarto.setCategoriaId(id);
        quarto.setAndar(1);
        quarto.setAtivo(true);
        Long quartoId = quartoService.salvar(quarto).getId();

        categoriaService.alternarAtivo(id); // desativa

        // o quarto existente continua legível com a categoria preservada
        assertThat(quartoService.buscarPorId(quartoId).getCategoria().getId()).isEqualTo(id);
    }
}
