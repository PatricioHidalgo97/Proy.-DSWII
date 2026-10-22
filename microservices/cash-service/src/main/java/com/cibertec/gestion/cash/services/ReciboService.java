package com.cibertec.gestion.cash.services;

import com.cibertec.gestion.cash.dto.IngresoExternoRequest;
import com.cibertec.gestion.cash.entities.ReciboEntity;
import com.cibertec.gestion.cash.enums.TipoRecibo;
import com.cibertec.gestion.cash.exceptions.BusinessException;
import com.cibertec.gestion.cash.exceptions.NotFoundException;
import com.cibertec.gestion.cash.mappers.ReciboMapper;
import com.cibertec.gestion.cash.repositories.ReciboRepository;
import com.cibertec.gestion.cash.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Recibos (ESP-04 §4.4). El usuario del snapshot sale del JWT; no se consulta
 * identidad (ESP-03 §4.1, §7).
 */
@Service
@RequiredArgsConstructor
public class ReciboService implements IReciboService {

    private static final String TIPO_CORRELATIVO = "RECIBO";
    private static final String PREFIJO_CORRELATIVO = "REC";

    private final ReciboRepository reciboRepository;
    private final CorrelativoService correlativoService;

    @Override
    public ReciboMapper.ReciboResponse getById(Long id) {
        return ReciboMapper.toResponse(reciboRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Recibo no encontrado")));
    }

    @Override
    public List<ReciboMapper.ReciboResponse> getByDate(LocalDate date, String tipo) {
        LocalDateTime inicio = date.atStartOfDay();
        LocalDateTime fin = date.plusDays(1).atStartOfDay().minusNanos(1);
        List<ReciboEntity> recibos = tipo == null || tipo.isBlank()
                ? reciboRepository.findByFechaBetweenOrderByFechaDesc(inicio, fin)
                : reciboRepository.findByTipoAndFechaBetweenOrderByFechaDesc(
                parseTipo(tipo), inicio, fin);
        return recibos.stream().map(ReciboMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public ReciboMapper.ReciboResponse createExternalIncome(IngresoExternoRequest request,
                                                            UsuarioPrincipal usuario) {
        validarUsuario(usuario);
        return ReciboMapper.toResponse(reciboRepository.save(ReciboEntity.builder()
                .correlativo(correlativoService.next(TIPO_CORRELATIVO, PREFIJO_CORRELATIVO))
                .fecha(LocalDateTime.now())
                .monto(request.getMonto())
                .tipo(TipoRecibo.INGRESO_EXTERNO)
                .depositante(request.getDepositante().trim())
                .categoria(request.getCategoria().trim())
                .concepto(request.getConcepto().trim())
                .usuarioId(usuario.id())
                .usuarioUsuario(usuario.usuario())
                .usuarioNombres(usuario.nombres())
                .usuarioApellidos(usuario.apellidos())
                .usuarioRol(usuario.rol())
                .build()));
    }

    private TipoRecibo parseTipo(String tipo) {
        try {
            return TipoRecibo.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Tipo de recibo inválido: " + tipo);
        }
    }

    private void validarUsuario(UsuarioPrincipal usuario) {
        if (usuario == null || usuario.id() == null
                || isBlank(usuario.usuario()) || isBlank(usuario.nombres())
                || isBlank(usuario.apellidos()) || isBlank(usuario.rol())) {
            throw new BusinessException(
                    "El token no contiene los datos del usuario necesarios para emitir el recibo");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
