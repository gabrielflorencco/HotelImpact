package com.gabrielflorencco.hotelimpact.integration;

import com.gabrielflorencco.hotelimpact.dto.CategoriaForm;
import com.gabrielflorencco.hotelimpact.dto.HospedeForm;
import com.gabrielflorencco.hotelimpact.dto.QuartoForm;
import com.gabrielflorencco.hotelimpact.dto.ReservaForm;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.enums.EstadoQuarto;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.exception.NegocioException;
import com.gabrielflorencco.hotelimpact.repository.QuartoRepository;
import com.gabrielflorencco.hotelimpact.service.CategoriaQuartoService;
import com.gabrielflorencco.hotelimpact.service.HospedeService;
import com.gabrielflorencco.hotelimpact.service.QuartoService;
import com.gabrielflorencco.hotelimpact.service.ReservaService;
import com.gabrielflorencco.hotelimpact.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Edição segura de reservas (BR-RES-009): apenas reservas ativas sem
 * check-in são reprogramáveis, com todas as validações da criação —
 * inclusive a disponibilidade ignorando a própria reserva.
 */
@SpringBootTest(properties = {"app.seed-demo=true",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none"})
class ReservaEdicaoIntegracaoTest extends AbstractIntegrationTest {

    @Autowired
    private ReservaService reservaService;
    @Autowired
    private CategoriaQuartoService categoriaService;
    @Autowired
    private QuartoService quartoService;
    @Autowired
    private HospedeService hospedeService;
    @Autowired
    private QuartoRepository quartos;

    private record Cenario(Long categoriaId, Long quartoId, Long outroQuartoId, Long hospedeId, Long reservaId) { }

    private Cenario cenario() {
        CategoriaForm categoria = new CategoriaForm();
        categoria.setNome("Edição " + System.nanoTime());
        categoria.setCapacidadeMaxima(2);
        categoria.setQuantidadeCamas(1);
        categoria.setPrecoBase(new BigDecimal("180.00"));
        categoria.setAtivo(true);
        Long categoriaId = categoriaService.salvar(categoria).getId();

        QuartoForm q = new QuartoForm();
        q.setNumero("E" + System.nanoTime() % 100000);
        q.setCategoriaId(categoriaId);
        q.setAndar(1);
        q.setAtivo(true);
        Long quartoId = quartoService.salvar(q).getId();
        q.setNumero("F" + System.nanoTime() % 100000);
        Long outroQuartoId = quartoService.salvar(q).getId();

        HospedeForm h = new HospedeForm();
        h.setNome("Hóspede Edição");
        Long hospedeId = hospedeService.salvar(h).getId();

        ReservaForm r = new ReservaForm();
        r.setHospedeId(hospedeId);
        r.setQuartoId(quartoId);
        r.setDataCheckIn(LocalDate.now().plusDays(10));
        r.setDataCheckOut(LocalDate.now().plusDays(12));
        r.setQuantidadeHospedes(2);
        r.setValorDiaria(new BigDecimal("180.00"));
        Long reservaId = reservaService.criar(r).getId();
        return new Cenario(categoriaId, quartoId, outroQuartoId, hospedeId, reservaId);
    }

    private ReservaForm formEdicao(Cenario c, LocalDate checkIn, LocalDate checkOut, Long quartoId) {
        ReservaForm form = new ReservaForm();
        form.setHospedeId(c.hospedeId());
        form.setQuartoId(quartoId);
        form.setDataCheckIn(checkIn);
        form.setDataCheckOut(checkOut);
        form.setQuantidadeHospedes(2);
        form.setValorDiaria(new BigDecimal("180.00"));
        return form;
    }

    @Test
    void edicaoValidaAlteraDatasQuartoERecalculaTotal() {
        Cenario c = cenario();
        ReservaForm form = formEdicao(c, LocalDate.now().plusDays(20), LocalDate.now().plusDays(23), c.quartoId());
        form.setDesconto(new BigDecimal("50.00"));

        var reserva = reservaService.atualizar(c.reservaId(), form);

        assertThat(reserva.getDataCheckIn()).isEqualTo(LocalDate.now().plusDays(20));
        assertThat(reserva.getValorTotal()).isEqualByComparingTo("490.00"); // 3 × 180 − 50
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.PENDENTE);
    }

    @Test
    void edicaoPodeManterAsPropriasDatasSemConflitoConsigoMesma() {
        Cenario c = cenario();
        ReservaForm form = formEdicao(c, LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), c.quartoId());
        // mesmas datas da própria reserva: não é conflito consigo mesma
        assertThat(reservaService.atualizar(c.reservaId(), form).getId()).isEqualTo(c.reservaId());
    }

    @Test
    void edicaoBloqueiaConflitoComOutraReservaAtiva() {
        Cenario c = cenario();
        // outra reserva ocupa o quarto DEPOIS do período atual (15-17):
        ReservaForm outra = formEdicao(c, LocalDate.now().plusDays(15), LocalDate.now().plusDays(17), c.quartoId());
        reservaService.criar(outra);

        // editar a primeira para invadir o período da outra é bloqueado
        ReservaForm conflitante = formEdicao(c, LocalDate.now().plusDays(16), LocalDate.now().plusDays(18), c.quartoId());
        assertThatThrownBy(() -> reservaService.atualizar(c.reservaId(), conflitante))
                .isInstanceOf(NegocioException.class)
                .hasMessage("Já existe uma reserva ativa para o quarto "
                        + quartos.findById(c.quartoId()).orElseThrow().getNumero()
                        + " no período informado.");
    }

    @Test
    void edicaoPermiteMudarParaOutroQuartoLivreNoMesmoPerioda() {
        Cenario c = cenario();
        ReservaForm form = formEdicao(c, LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), c.outroQuartoId());
        assertThat(reservaService.atualizar(c.reservaId(), form).getQuarto().getId())
                .isEqualTo(c.outroQuartoId());
    }

    @Test
    void edicaoBloqueiaEstadosFinaisEComCheckIn() {
        Cenario c = cenario();
        reservaService.confirmar(c.reservaId());
        reservaService.cancelar(c.reservaId());
        ReservaForm form = formEdicao(c, LocalDate.now().plusDays(15), LocalDate.now().plusDays(16), c.quartoId());
        assertThatThrownBy(() -> reservaService.atualizar(c.reservaId(), form))
                .hasMessage("Somente reservas pendentes ou confirmadas podem ser editadas.");
    }

    @Test
    void edicaoBloqueiaCapacidadeExcedidaEQuartoIndisponivel() {
        Cenario c = cenario();
        ReservaForm estourando = formEdicao(c, LocalDate.now().plusDays(15), LocalDate.now().plusDays(16), c.quartoId());
        estourando.setQuantidadeHospedes(9);
        assertThatThrownBy(() -> reservaService.atualizar(c.reservaId(), estourando))
                .hasMessageContaining("Capacidade excedida");

        // quarto interditado não recebe reserva editada
        QuartoForm dados = new QuartoForm();
        Quarto quarto = quartos.findById(c.outroQuartoId()).orElseThrow();
        quarto.interditar();
        quartos.save(quarto);
        ReservaForm form = formEdicao(c, LocalDate.now().plusDays(15), LocalDate.now().plusDays(16), c.outroQuartoId());
        assertThatThrownBy(() -> reservaService.atualizar(c.reservaId(), form))
                .hasMessageContaining("está fora de operação");
    }

    @Test
    void edicaoParaDataPassadaExigeJustificativaComoNaCriacao() {
        Cenario c = cenario();
        ReservaForm form = formEdicao(c, LocalDate.now().minusDays(3), LocalDate.now().plusDays(1), c.quartoId());
        assertThatThrownBy(() -> reservaService.atualizar(c.reservaId(), form))
                .hasMessage("Reservas para datas passadas exigem uma justificativa de exceção.");

        form.setMotivoExcecao("Correção administrativa de agenda");
        assertThat(reservaService.atualizar(c.reservaId(), form).getMotivoExcecao())
                .isEqualTo("Correção administrativa de agenda");
    }
}
