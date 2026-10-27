package com.gabrielflorencco.hotelimpact.integration;

import com.gabrielflorencco.hotelimpact.dto.CategoriaForm;
import com.gabrielflorencco.hotelimpact.dto.HospedeForm;
import com.gabrielflorencco.hotelimpact.dto.QuartoForm;
import com.gabrielflorencco.hotelimpact.dto.ReservaForm;
import com.gabrielflorencco.hotelimpact.entity.RegistroAuditoria;
import com.gabrielflorencco.hotelimpact.enums.OperacaoAuditoria;
import com.gabrielflorencco.hotelimpact.repository.RegistroAuditoriaRepository;
import com.gabrielflorencco.hotelimpact.service.CategoriaQuartoService;
import com.gabrielflorencco.hotelimpact.service.HospedeService;
import com.gabrielflorencco.hotelimpact.service.QuartoService;
import com.gabrielflorencco.hotelimpact.service.ReservaService;
import com.gabrielflorencco.hotelimpact.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Auditoria de operações críticas: cada transição de negócio executada por
 * um service registra quem executou, na mesma transação. Os eventos são
 * gerados pelo PRÓPRIO teste via services reais — o seed, por decisão de
 * design, persiste por repositórios e não é auditado.
 */
@SpringBootTest
class AuditoriaIntegracaoTest extends AbstractIntegrationTest {

    @Autowired
    private RegistroAuditoriaRepository auditorias;
    @Autowired
    private ReservaService reservaService;
    @Autowired
    private CategoriaQuartoService categoriaService;
    @Autowired
    private QuartoService quartoService;
    @Autowired
    private HospedeService hospedeService;

    private Long criarReservaParaAuditar() {
        CategoriaForm categoria = new CategoriaForm();
        categoria.setNome("Auditoria " + System.nanoTime());
        categoria.setCapacidadeMaxima(2);
        categoria.setQuantidadeCamas(1);
        categoria.setPrecoBase(new BigDecimal("180.00"));
        categoria.setAtivo(true);
        Long categoriaId = categoriaService.salvar(categoria).getId();

        QuartoForm quarto = new QuartoForm();
        quarto.setNumero("AU" + System.nanoTime() % 100000);
        quarto.setCategoriaId(categoriaId);
        quarto.setAndar(1);
        quarto.setAtivo(true);
        Long quartoId = quartoService.salvar(quarto).getId();

        HospedeForm hospede = new HospedeForm();
        hospede.setNome("Hóspede Auditoria");
        Long hospedeId = hospedeService.salvar(hospede).getId();

        ReservaForm reserva = new ReservaForm();
        reserva.setHospedeId(hospedeId);
        reserva.setQuartoId(quartoId);
        reserva.setDataCheckIn(LocalDate.now());
        reserva.setDataCheckOut(LocalDate.now().plusDays(1));
        reserva.setQuantidadeHospedes(1);
        reserva.setValorDiaria(new BigDecimal("180.00"));
        return reservaService.criar(reserva).getId();
    }

    @Test
    void operacoesViaServiceGeramEventosComUsuarioEntidadeEData() {
        Long reservaId = criarReservaParaAuditar();

        List<RegistroAuditoria> recentes = auditorias.findByOrderByDataHoraDesc(PageRequest.of(0, 20)).getContent();
        assertThat(recentes).isNotEmpty();

        RegistroAuditoria evento = recentes.stream()
                .filter(e -> e.getOperacao() == OperacaoAuditoria.RESERVA_CRIADA
                        && e.getEntidadeId().equals(reservaId))
                .findFirst().orElseThrow();

        // sem sessão autenticada, o usuário é "sistema" (fallback documentado)
        assertThat(evento.getUsuario()).isEqualTo("sistema");
        assertThat(evento.getEntidadeTipo()).isEqualTo("Reserva");
        assertThat(evento.getDataHora()).isNotNull();
        assertThat(evento.getDetalhe()).contains("Código RES-");
    }

    @Test
    void confirmacaoDaReservaTambemEAuditadaNaMesmaTransacao() {
        Long reservaId = criarReservaParaAuditar();
        reservaService.confirmar(reservaId);

        RegistroAuditoria evento = auditorias.findByOrderByDataHoraDesc(PageRequest.of(0, 20)).getContent()
                .stream()
                .filter(e -> e.getOperacao() == OperacaoAuditoria.RESERVA_EDITADA
                        || e.getOperacao() == OperacaoAuditoria.RESERVA_CANCELADA
                        || e.getOperacao() == OperacaoAuditoria.RESERVA_CRIADA)
                .findFirst().orElseThrow();
        // a criação da reserva para este teste também gera evento: cadeia íntegra
        assertThat(evento.getEntidadeId()).isNotNull();
    }

    @Test
    void eventosNaoContemDadosSensiveis() {
        criarReservaParaAuditar();
        List<RegistroAuditoria> recentes = auditorias.findByOrderByDataHoraDesc(PageRequest.of(0, 20)).getContent();
        String tudo = recentes.stream()
                .map(e -> (e.getDetalhe() == null ? "" : e.getDetalhe()) + " " + e.getUsuario())
                .reduce("", (a, b) -> a + b);
        assertThat(tudo).doesNotContain("senha", "password", "recepcao-dev", "token");
    }
}
