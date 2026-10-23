package com.gabrielflorencco.hotelimpact.dto;

import com.gabrielflorencco.hotelimpact.entity.Hospede;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Hóspede exibido na tela de detalhes, com dados formatados para leitura.
 */
@Getter
public class HospedeDetalheView {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Long id;
    private final String nome;
    private final String cpfCompleto;
    private final String email;
    private final String telefone;
    private final String dataNascimento;
    private final String dataCadastro;
    private final String observacoes;

    private HospedeDetalheView(Hospede hospede) {
        this.id = hospede.getId();
        this.nome = hospede.getNome();
        this.cpfCompleto = formatarCpf(hospede.getCpf());
        this.email = hospede.getEmail();
        this.telefone = hospede.getTelefone();
        this.dataNascimento = hospede.getDataNascimento() == null ? null : hospede.getDataNascimento().format(DATA);
        this.dataCadastro = hospede.getDataCadastro() == null ? null : hospede.getDataCadastro().format(DATA_HORA);
        this.observacoes = hospede.getObservacoes();
    }

    public static HospedeDetalheView de(Hospede hospede) {
        return new HospedeDetalheView(hospede);
    }

    static String formatarCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return null;
        }
        return "%s.%s.%s-%s".formatted(cpf.substring(0, 3), cpf.substring(3, 6),
                cpf.substring(6, 9), cpf.substring(9));
    }
}
