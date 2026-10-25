package com.gabrielflorencco.hotelimpact.integration;

import com.gabrielflorencco.hotelimpact.config.DataSeeder;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.repository.CategoriaQuartoRepository;
import com.gabrielflorencco.hotelimpact.repository.HospedagemRepository;
import com.gabrielflorencco.hotelimpact.repository.HospedeRepository;
import com.gabrielflorencco.hotelimpact.repository.ItemCatalogoRepository;
import com.gabrielflorencco.hotelimpact.repository.OrdemManutencaoRepository;
import com.gabrielflorencco.hotelimpact.repository.PagamentoRepository;
import com.gabrielflorencco.hotelimpact.repository.ReservaRepository;
import com.gabrielflorencco.hotelimpact.repository.QuartoRepository;
import com.gabrielflorencco.hotelimpact.repository.ConsumoRepository;
import com.gabrielflorencco.hotelimpact.service.PagamentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Massa de demonstração (DataSeeder): idempotência (executar duas vezes não
 * duplica), integridade dos relacionamentos (hospedagens ligadas a reservas
 * válidas, consumos dentro do período, saldos coerentes) e coerência dos
 * cenários operacionais com a data corrente (chegadas e saídas do dia).
 * Container próprio e isolado, como no MigracaoFlywayTest.
 */
@Transactional
@SpringBootTest(properties = {
        "app.seed-demo=true",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=true",
        "spring.flyway.baseline-version=1",
        "spring.jpa.hibernate.ddl-auto=none"})
class DataSeederTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"))
                    .withDatabaseName("hotelimpact")
                    .withUsername("seed")
                    .withPassword("seed");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registrarBanco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private DataSeeder seeder;

    @Autowired
    private CategoriaQuartoRepository categorias;
    @Autowired
    private QuartoRepository quartos;
    @Autowired
    private HospedeRepository hospedes;
    @Autowired
    private ItemCatalogoRepository itens;
    @Autowired
    private ReservaRepository reservas;
    @Autowired
    private HospedagemRepository hospedagens;
    @Autowired
    private ConsumoRepository consumos;
    @Autowired
    private PagamentoRepository pagamentos;
    @Autowired
    private OrdemManutencaoRepository manutencoes;
    @Autowired
    private PagamentoService pagamentoService;

    @Test
    void segundaExecucaoNaoDuplicaRegistros() {
        long categoriasAntes = categorias.count();
        long quartosAntes = quartos.count();
        long hospedesAntes = hospedes.count();
        long itensAntes = itens.count();
        long reservasAntes = reservas.count();
        long hospedagensAntes = hospedagens.count();
        long consumosAntes = consumos.count();
        long pagamentosAntes = pagamentos.count();
        long manutencoesAntes = manutencoes.count();

        // O seeder já rodou na inicialização do contexto; executar de novo
        // (mesma transação de aplicação) deve ser um no-op completo.
        seeder.run(null);

        assertThat(categorias.count()).isEqualTo(categoriasAntes);
        assertThat(quartos.count()).isEqualTo(quartosAntes);
        assertThat(hospedes.count()).isEqualTo(hospedesAntes);
        assertThat(itens.count()).isEqualTo(itensAntes);
        assertThat(reservas.count()).isEqualTo(reservasAntes);
        assertThat(hospedagens.count()).isEqualTo(hospedagensAntes);
        assertThat(consumos.count()).isEqualTo(consumosAntes);
        assertThat(pagamentos.count()).isEqualTo(pagamentosAntes);
        assertThat(manutencoes.count()).isEqualTo(manutencoesAntes);
    }

    @Test
    void massaCobreTodasAsEntidadesComVolumesPlausiveis() {
        assertThat(categorias.count()).isEqualTo(3);
        assertThat(quartos.count()).isEqualTo(12);
        assertThat(hospedes.count()).isEqualTo(12);
        assertThat(itens.count()).isEqualTo(8);
        assertThat(reservas.count()).isEqualTo(12);
        assertThat(hospedagens.count()).isEqualTo(7);
        assertThat(consumos.count()).isGreaterThanOrEqualTo(6);
        assertThat(pagamentos.count()).isGreaterThanOrEqualTo(9);
        assertThat(manutencoes.count()).isEqualTo(4);
    }

    @Test
    void hospedagensReferenciamReservasValidasEEstadosCoerentes() {
        hospedagens.findAll().forEach(h -> {
            assertThat(h.getReserva()).isNotNull();
            assertThat(h.getReserva().getId()).isNotNull();
            assertThat(h.getQuarto().getId()).isEqualTo(h.getReserva().getQuarto().getId());
            if (h.getEstado() == EstadoHospedagem.EM_ANDAMENTO) {
                assertThat(h.getCheckOutReal()).isNull();
                assertThat(h.getReserva().getEstado()).isIn(EstadoReserva.PENDENTE, EstadoReserva.CONFIRMADA);
            } else {
                assertThat(h.getCheckOutReal()).isNotNull();
                assertThat(h.getReserva().getEstado()).isEqualTo(EstadoReserva.CONCLUIDA);
            }
        });
    }

    @Test
    void consumosDentroDoPeriodoDaHospedagemComPrecoHistorico() {
        consumos.findAll().forEach(c -> {
            var h = c.getHospedagem();
            assertThat(c.getDataHora()).isAfterOrEqualTo(h.getCheckInReal());
            if (h.getCheckOutReal() != null) {
                assertThat(c.getDataHora()).isBeforeOrEqualTo(h.getCheckOutReal());
            }
            assertThat(c.getValorTotal())
                    .isEqualTo(c.getPrecoUnitario().multiply(BigDecimal.valueOf(c.getQuantidade())));
        });
    }

    @Test
    void cenariosOperacionaisDoDiaAparecemNosFiltrosCorretos() {
        LocalDate hoje = LocalDate.now();

        // chegada de hoje com check-in realizado
        var chegadaHoje = reservas.findAll().stream()
                .filter(r -> r.getDataCheckIn().equals(hoje) && r.isAtiva())
                .toList();
        assertThat(chegadaHoje).hasSize(1);

        // saída de hoje: reserva ativa com check-out previsto para o dia
        // (critério do domínio: a lista mostra saídas ainda não realizadas)
        var saidaHoje = reservas.findAll().stream()
                .filter(r -> r.getDataCheckOut().equals(hoje) && r.isAtiva())
                .toList();
        assertThat(saidaHoje).hasSize(1);

        // em andamento: exatamente 4 estadias
        var emAndamento = hospedagens.findAll().stream()
                .filter(h -> h.getEstado() == EstadoHospedagem.EM_ANDAMENTO)
                .toList();
        assertThat(emAndamento).hasSize(4);
    }

    @Test
    void saldosDevedoresSaoCoerentesComOsCenarios() {
        var emAndamento = hospedagens.findAll().stream()
                .filter(h -> h.getEstado() == EstadoHospedagem.EM_ANDAMENTO)
                .toList();

        // Uma estadia quitada antecipadamente e uma com saldo devedor;
        // a recém-chegada não tem lançamentos (saldo = valor da reserva).
        long quitadas = emAndamento.stream()
                .filter(h -> pagamentoService.saldoDevedor(h.getId()).signum() <= 0)
                .count();
        long pendentes = emAndamento.stream()
                .filter(h -> pagamentoService.saldoDevedor(h.getId()).signum() > 0)
                .count();
        assertThat(quitadas).isGreaterThanOrEqualTo(1);
        assertThat(pendentes).isGreaterThanOrEqualTo(1);

        // Hospedagens encerradas: sem exceção registrada (conta quitada).
        hospedagens.findAll().stream()
                .filter(h -> h.getEstado() == EstadoHospedagem.ENCERRADA)
                .forEach(h -> assertThat(h.getMotivoExcecaoEncerramento()).isNull());
    }

    @Test
    void estadosDosQuartosCompativeisComOcupacaoEManutencao() {
        var ocupados = quartos.findAll().stream()
                .filter(q -> q.getEstado() == com.gabrielflorencco.hotelimpact.enums.EstadoQuarto.OCUPADO)
                .toList();
        // 4 hospedagens em andamento, 4 quartos ocupados
        assertThat(ocupados).hasSize(4);
        ocupados.forEach(q -> assertThat(
                hospedagens.existsByQuartoIdAndEstado(q.getId(), EstadoHospedagem.EM_ANDAMENTO))
                .isTrue());

        // quartos fora de operação possuem ordem de manutenção correspondente
        var interditado = quartos.findAll().stream()
                .filter(q -> q.getNumero().equals("302"))
                .findFirst().orElseThrow();
        assertThat(interditado.getEstado()).isEqualTo(com.gabrielflorencco.hotelimpact.enums.EstadoQuarto.INTERDITADO);
        assertThat(manutencoes.findAll().stream()
                .anyMatch(m -> m.getQuarto().getId().equals(interditado.getId())
                        && Boolean.TRUE.equals(m.getInterditouQuarto()))).isTrue();
    }
}
