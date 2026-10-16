package com.cibertec.gestion.services;

import java.time.LocalDate;
import java.time.YearMonth;

public interface IReporteService {
    byte[] movimientosDiarios(LocalDate fecha);
    byte[] movimientosMensuales(YearMonth periodo);
    byte[] socios();
    byte[] egresos(YearMonth periodo);
    byte[] bancos();
}
