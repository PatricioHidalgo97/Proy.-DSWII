package com.cibertec.gestion.cash.services;

import java.time.LocalDate;
import java.time.YearMonth;

/** Los cinco reportes XLSX de ESP-04 §4.6. */
public interface IReporteService {

    byte[] movimientosDiarios(LocalDate fecha);

    byte[] movimientosMensuales(YearMonth periodo);

    byte[] socios();

    byte[] egresos(YearMonth periodo);

    byte[] bancos();
}
