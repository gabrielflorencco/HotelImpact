package com.gabrielflorencco.hotelimpact.dto;

import com.gabrielflorencco.hotelimpact.entity.Hospede;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Hóspede exibido em listagens: o CPF aparece mascarado por privacidade
 * (somente os três dígitos finais).
 */
@Getter
public class HospedeResumoView {

    private final Long id;
    private final String nome;
    private final String cpfMascarado;
    private final String email;
    private final String telefone;
    private final LocalDate dataNascimento;
    private final LocalDateTime dataCadastro;

    private HospedeResumoView(Hospede hospede) {
        this.id = hospede.getId();
        this.nome = hospede.getNome();
        this.cpfMascarado = mascararCpf(hospede.getCpf());
        this.email = hospede.getEmail();
        this.telefone = hospede.getTelefone();
        this.dataNascimento = hospede.getDataNascimento();
        this.dataCadastro = hospede.getDataCadastro();
    }

    public static HospedeResumoView de(Hospede hospede) {
        return new HospedeResumoView(hospede);
    }

    /** •••.•••.•••-789 para preservar privacidade em listagens. */
    static String mascararCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return null;
        }
        return "•••.•••.•••-" + cpf.substring(8);
    }
}
