package com.gabrielflorencco.hotelimpact.dto;

import com.gabrielflorencco.hotelimpact.entity.Hospede;
import com.gabrielflorencco.hotelimpact.validation.Cpf;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Formulário de hóspede. CPF aceita pontuação e é normalizado para 11 dígitos
 * pelo service; @Cpf aplica o algoritmo oficial de dígitos verificadores junto
 * ao campo.
 */
@Getter
@Setter
public class HospedeForm {

    private Long id;

    @NotBlank
    @Size(min = 3, max = 120)
    private String nome;

    @Size(max = 14)
    @Pattern(regexp = "^(\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}|\\d{11})?$",
             message = "Use apenas números (e pontuação opcional) no CPF")
    @Cpf
    private String cpf;

    @Email
    @Size(max = 120)
    private String email;

    /** Telefones brasileiros, com DDD opcional e nono dígito aceito. */
    @Size(max = 20)
    @Pattern(regexp = "^(\\(?\\d{2}\\)?[\\s-]?)?(?:9?\\d{4})-?\\d{4}$",
             message = "Informe um telefone válido, ex.: (11) 98888-0001")
    private String telefone;

    @PastOrPresent
    private LocalDate dataNascimento;

    @Size(max = 500)
    private String observacoes;

    public static HospedeForm de(Hospede hospede) {
        HospedeForm form = new HospedeForm();
        form.setId(hospede.getId());
        form.setNome(hospede.getNome());
        form.setCpf(hospede.getCpf());
        form.setEmail(hospede.getEmail());
        form.setTelefone(hospede.getTelefone());
        form.setDataNascimento(hospede.getDataNascimento());
        form.setObservacoes(hospede.getObservacoes());
        return form;
    }
}
