package com.gabrielflorencco.hotelimpact.integration;

import com.gabrielflorencco.hotelimpact.config.DataSeeder;
import com.gabrielflorencco.hotelimpact.entity.CategoriaQuarto;
import com.gabrielflorencco.hotelimpact.entity.Hospede;
import com.gabrielflorencco.hotelimpact.entity.Hospedagem;
import com.gabrielflorencco.hotelimpact.entity.Quarto;
import com.gabrielflorencco.hotelimpact.entity.Reserva;
import com.gabrielflorencco.hotelimpact.enums.EstadoHospedagem;
import com.gabrielflorencco.hotelimpact.enums.EstadoReserva;
import com.gabrielflorencco.hotelimpact.repository.CategoriaQuartoRepository;
import com.gabrielflorencco.hotelimpact.repository.HospedeRepository;
import com.gabrielflorencco.hotelimpact.repository.HospedagemRepository;
import com.gabrielflorencco.hotelimpact.repository.ItemCatalogoRepository;
import com.gabrielflorencco.hotelimpact.repository.QuartoRepository;
import com.gabrielflorencco.hotelimpact.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Valida a fundação do sistema: carga de demonstração idempotente e as restrições
 * de integridade aplicadas diretamente no PostgreSQL.
 *
 * Container PostgreSQL PRÓPRIO e isolado (padrão já usado por DataSeederTest e
 * MigracaoFlywayTest): este teste pressupõe o estado criado pelo seed no
 * startup e chamadas idempotentes — dependência de ordem entre suítes no
 * container compartilhado foi eliminada sem tocar nas asserções.
 */
@SpringBootTest(properties = "app.seed-demo=true")
class FundacoesIntegracaoTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"))
                    .withDatabaseName("hotelimpact")
                    .withUsername("fundacoes")
                    .withPassword("fundacoes");

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
    private JdbcTemplate jdbcTemplate;

    @Test
    void seederNaoDuplicaDadosEmReexecucoes() throws Exception {
        long categoriasAntes = categorias.count();
        long quartosAntes = quartos.count();
        long hospedesAntes = hospedes.count();
        long itensAntes = itens.count();

        seeder.run(null);

        assertThat(categorias.count()).isEqualTo(categoriasAntes);
        assertThat(quartos.count()).isEqualTo(quartosAntes);
        assertThat(hospedes.count()).isEqualTo(hospedesAntes);
        assertThat(itens.count()).isEqualTo(itensAntes);

        assertThat(categorias.findByNomeIgnoreCase("Standard")).isPresent();
        assertThat(quartos.findByNumero("101")).isPresent();
        assertThat(hospedes.findAll()).anyMatch(h -> h.getCpf() != null && h.getCpf().length() == 11);
    }

    @Test
    void constraintDeExclusaoImpedeReservasAtivasSobrepostasNoMesmoQuarto() {
        Quarto quarto = novoQuarto("C-Exclusao", "901");
        Hospede hospede = novoHospede("Hóspede Exclusão");

        reservas.saveAndFlush(reserva("EXC-1", quarto, hospede,
                LocalDate.now(), LocalDate.now().plusDays(2), EstadoReserva.CONFIRMADA));

        assertThatThrownBy(() -> reservas.saveAndFlush(reserva("EXC-2", quarto, hospede,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), EstadoReserva.CONFIRMADA)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void reservasDePeriodosAdjacentesNaoConflitam() {
        Quarto quarto = novoQuarto("C-Adjacente", "902");
        Hospede hospede = novoHospede("Hóspede Adjacente");

        Reserva primeira = reservas.saveAndFlush(reserva("ADJ-1", quarto, hospede,
                LocalDate.now(), LocalDate.now().plusDays(2), EstadoReserva.CONFIRMADA));
        Reserva segunda = reservas.saveAndFlush(reserva("ADJ-2", quarto, hospede,
                LocalDate.now().plusDays(2), LocalDate.now().plusDays(4), EstadoReserva.CONFIRMADA));

        assertThat(primeira.getId()).isNotNull();
        assertThat(segunda.getId()).isNotNull();
    }

    @Test
    void reservaCanceladaNaoBloqueiaODisponibilidadeDoPeriodo() {
        Quarto quarto = novoQuarto("C-Cancelada", "903");
        Hospede hospede = novoHospede("Hóspede Cancelada");

        reservas.saveAndFlush(reserva("CANC-1", quarto, hospede,
                LocalDate.now(), LocalDate.now().plusDays(3), EstadoReserva.CANCELADA));

        Reserva nova = reservas.saveAndFlush(reserva("CANC-2", quarto, hospede,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), EstadoReserva.CONFIRMADA));

        assertThat(nova.getId()).isNotNull();
    }

    @Test
    void partialUniqueIndexImpedeDuasHospedagensSimultaneasNoMesmoQuarto() {
        Quarto quarto = novoQuarto("C-Hospedagem", "904");
        Hospede hospede = novoHospede("Hóspede Hospedagem");

        Reserva reserva = reservas.saveAndFlush(reserva("HOSP-1", quarto, hospede,
                LocalDate.now(), LocalDate.now().plusDays(2), EstadoReserva.CONFIRMADA));

        Hospedagem primeira = novaHospedagem(reserva, quarto);
        hospedagens.saveAndFlush(primeira);

        Reserva reserva2 = reservas.saveAndFlush(reserva("HOSP-2", quarto, hospede,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(7), EstadoReserva.CONFIRMADA));

        assertThatThrownBy(() -> hospedagens.saveAndFlush(novaHospedagem(reserva2, quarto)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkImpedeEstoqueNegativo() {
        var item = itens.findAll().get(0);
        Long id = item.getId();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE item_catalogo SET estoque_atual = -1 WHERE id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Quarto novoQuarto(String nomeCategoria, String numero) {
        CategoriaQuarto categoria = new CategoriaQuarto();
        categoria.setNome(nomeCategoria);
        categoria.setCapacidadeMaxima(2);
        categoria.setQuantidadeCamas(1);
        categoria.setPrecoBase(new BigDecimal("200.00"));
        categoria.setAtivo(true);
        categorias.saveAndFlush(categoria);

        Quarto quarto = new Quarto();
        quarto.setNumero(numero);
        quarto.setCategoria(categoria);
        quarto.setAndar(9);
        quarto.setAtivo(true);
        return quartos.saveAndFlush(quarto);
    }

    private Hospede novoHospede(String nome) {
        Hospede hospede = new Hospede();
        hospede.setNome(nome);
        return hospedes.saveAndFlush(hospede);
    }

    private Reserva reserva(String codigo, Quarto quarto, Hospede hospede,
                            LocalDate checkIn, LocalDate checkOut, EstadoReserva estado) {
        Reserva reserva = new Reserva();
        reserva.setCodigo(codigo);
        reserva.setHospede(hospede);
        reserva.setQuarto(quarto);
        reserva.setDataCheckIn(checkIn);
        reserva.setDataCheckOut(checkOut);
        reserva.setQuantidadeHospedes(1);
        reserva.setValorDiaria(new BigDecimal("200.00"));
        reserva.setDesconto(BigDecimal.ZERO);
        reserva.setValorTotal(new BigDecimal("400.00"));
        reserva.setEstado(estado);
        return reserva;
    }

    private Hospedagem novaHospedagem(Reserva reserva, Quarto quarto) {
        Hospedagem hospedagem = new Hospedagem();
        hospedagem.setReserva(reserva);
        hospedagem.setQuarto(quarto);
        hospedagem.setCheckInReal(LocalDateTime.now());
        hospedagem.setEstado(EstadoHospedagem.EM_ANDAMENTO);
        return hospedagem;
    }
}
