package com.gabrielflorencco.hotelimpact.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementação do algoritmo oficial de validação de CPF (módulo 11).
 * Nulo ou vazio é considerado válido: a obrigatoriedade é responsabilidade
 * de @NotBlank, quando aplicável.
 */
public class CpfValidator implements ConstraintValidator<Cpf, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        if (valor == null || valor.isBlank()) {
            return true;
        }
        String digitos = valor.replaceAll("\\D", "");
        if (digitos.length() != 11 || todosDigitosIguais(digitos)) {
            return false;
        }
        int primeiroDv = digitoVerificador(digitos, 10);
        int segundoDv = digitoVerificador(digitos, 11);
        return digitos.charAt(9) - '0' == primeiroDv && digitos.charAt(10) - '0' == segundoDv;
    }

    private boolean todosDigitosIguais(String digitos) {
        return digitos.chars().distinct().count() == 1;
    }

    /** Calcula um dígito verificador: pesos decrescentes a partir de pesoInicial. */
    private int digitoVerificador(String digitos, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < pesoInicial - 1; i++) {
            soma += (digitos.charAt(i) - '0') * (pesoInicial - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
