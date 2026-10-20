package com.gabrielflorencco.hotelimpact;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * Sistema de gestão hoteleira (PMS) HotelImpact.
 */
@SpringBootApplication
public class HotelimpactApplication {

    /** Fuso fixo da aplicação: todas as datas/horas de eventos usam America/Sao_Paulo. */
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
    }

    public static void main(String[] args) {
        SpringApplication.run(HotelimpactApplication.class, args);
    }
}
