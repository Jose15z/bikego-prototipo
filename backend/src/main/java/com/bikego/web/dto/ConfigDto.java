package com.bikego.web.dto;

public record ConfigDto(int radioInicialMetros, int radioMaximoMetros, int bateriaMinima,
                        int tarifaBase, int tarifaMinuto, int bloqueoPagoMinutos, long iotTimeoutMs) { }
