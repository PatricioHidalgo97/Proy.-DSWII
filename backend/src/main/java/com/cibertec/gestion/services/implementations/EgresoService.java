package com.cibertec.gestion.services.implementations;

import com.cibertec.gestion.entities.EgresoEntity;
import com.cibertec.gestion.entities.UsuarioEntity;
import com.cibertec.gestion.enums.EstadoEgreso;
import com.cibertec.gestion.exceptions.BusinessException;
import com.cibertec.gestion.exceptions.NotFoundException;
import com.cibertec.gestion.models.EgresoRequest;
import com.cibertec.gestion.repositories.EgresoRepository;
import com.cibertec.gestion.repositories.UsuarioRepository;
import com.cibertec.gestion.services.IEgresoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EgresoService implements IEgresoService {
    private final EgresoRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final CorrelativoService correlativoService;

    @Override public List<EgresoEntity> getByMonth(YearMonth month) {
        return repository.findByFechaBetweenOrderByFechaDesc(month.atDay(1), month.atEndOfMonth());
    }

    @Override public EgresoEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Egreso no encontrado"));
    }

    @Override @Transactional
    public EgresoEntity create(EgresoRequest request, String username) {
        UsuarioEntity usuario = getUsuario(username);
        return repository.save(map(request, usuario));
    }

    @Override @Transactional
    public EgresoEntity process(Long id) {
        EgresoEntity entity = getById(id);
        if (entity.getEstado() == EstadoEgreso.ANULADO)
            throw new BusinessException("Un egreso anulado no puede procesarse");
        entity.setEstado(EstadoEgreso.PROCESADO);
        entity.setFechaActualizacion(LocalDateTime.now());
        return repository.save(entity);
    }

    @Override @Transactional
    public EgresoEntity annul(Long id) {
        EgresoEntity entity = getById(id);
        if (entity.getEstado() == EstadoEgreso.ANULADO)
            throw new BusinessException("El egreso ya está anulado");
        entity.setEstado(EstadoEgreso.ANULADO);
        entity.setFechaActualizacion(LocalDateTime.now());
        return repository.save(entity);
    }

    @Override @Transactional
    public List<EgresoEntity> importCsv(MultipartFile file, String username) {
        if (file == null || file.isEmpty()) throw new BusinessException("Seleccione un archivo CSV");
        UsuarioEntity usuario = getUsuario(username);
        List<EgresoEntity> imported = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int row = 0;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank() || (row == 1 && line.toLowerCase().contains("tipodocumento"))) continue;
                String[] values = line.split(";", -1);
                if (values.length < 8)
                    throw new BusinessException("Fila " + row + ": se requieren 8 columnas separadas por punto y coma");
                EgresoRequest request = new EgresoRequest();
                request.setTipoDocumento(values[0].trim());
                request.setNumeroDocumento(values[1].trim());
                request.setProveedor(values[2].trim());
                request.setFecha(LocalDate.parse(values[3].trim()));
                request.setSubtotal(new BigDecimal(values[4].trim()));
                request.setImpuesto(new BigDecimal(values[5].trim()));
                request.setTotal(new BigDecimal(values[6].trim()));
                request.setMotivo(values[7].trim());
                request.setDocumentoAsociado(values.length > 8 ? values[8].trim() : null);
                validarImportado(request, row);
                imported.add(repository.save(map(request, usuario)));
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw new BusinessException("No se pudo leer el CSV: " + ex.getMessage());
        }
        if (imported.isEmpty()) throw new BusinessException("El archivo no contiene egresos válidos");
        return imported;
    }

    private EgresoEntity map(EgresoRequest request, UsuarioEntity usuario) {
        return EgresoEntity.builder().comprobante(correlativoService.next("EGRESO", "EGR"))
                .tipoDocumento(request.getTipoDocumento().trim()).numeroDocumento(request.getNumeroDocumento().trim())
                .proveedor(request.getProveedor().trim()).fecha(request.getFecha()).subtotal(request.getSubtotal())
                .impuesto(request.getImpuesto()).total(request.getTotal()).motivo(request.getMotivo().trim())
                .documentoAsociado(blankToNull(request.getDocumentoAsociado())).estado(EstadoEgreso.REGISTRADO)
                .usuario(usuario).fechaRegistro(LocalDateTime.now()).build();
    }

    private UsuarioEntity getUsuario(String username) {
        return usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private void validarImportado(EgresoRequest r, int row) {
        if (r.getTipoDocumento().isBlank() || r.getNumeroDocumento().isBlank() || r.getProveedor().isBlank()
                || r.getMotivo().isBlank() || r.getSubtotal().signum() < 0 || r.getImpuesto().signum() < 0
                || r.getTotal().signum() <= 0)
            throw new BusinessException("Fila " + row + ": datos incompletos o montos inválidos");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
