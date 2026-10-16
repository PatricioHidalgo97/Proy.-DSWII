package com.cibertec.gestion.services.implementations;

import com.cibertec.gestion.entities.ReciboEntity;
import com.cibertec.gestion.entities.UsuarioEntity;
import com.cibertec.gestion.enums.TipoRecibo;
import com.cibertec.gestion.exceptions.NotFoundException;
import com.cibertec.gestion.models.IngresoExternoRequest;
import com.cibertec.gestion.repositories.ReciboRepository;
import com.cibertec.gestion.repositories.UsuarioRepository;
import com.cibertec.gestion.services.IReciboService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReciboService implements IReciboService {
    private final ReciboRepository reciboRepository;
    private final UsuarioRepository usuarioRepository;
    private final CorrelativoService correlativoService;

    @Override public ReciboEntity getById(Long id) {
        return reciboRepository.findById(id).orElseThrow(() -> new NotFoundException("Recibo no encontrado"));
    }

    @Override public List<ReciboEntity> getByDate(LocalDate date, TipoRecibo tipo) {
        LocalDateTime inicio = date.atStartOfDay();
        LocalDateTime fin = date.plusDays(1).atStartOfDay().minusNanos(1);
        return tipo == null ? reciboRepository.findByFechaBetweenOrderByFechaDesc(inicio, fin)
                : reciboRepository.findByTipoAndFechaBetweenOrderByFechaDesc(tipo, inicio, fin);
    }

    @Override @Transactional
    public ReciboEntity createExternalIncome(IngresoExternoRequest request, String username) {
        UsuarioEntity usuario = usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        return reciboRepository.save(ReciboEntity.builder()
                .correlativo(correlativoService.next("RECIBO", "REC"))
                .fecha(LocalDateTime.now()).monto(request.getMonto()).tipo(TipoRecibo.INGRESO_EXTERNO)
                .depositante(request.getDepositante().trim()).categoria(request.getCategoria().trim())
                .concepto(request.getConcepto().trim()).usuario(usuario).build());
    }
}
