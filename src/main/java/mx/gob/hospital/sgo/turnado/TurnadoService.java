package mx.gob.hospital.sgo.turnado;

import mx.gob.hospital.sgo.area.Area;
import mx.gob.hospital.sgo.area.AreaRepository;
import mx.gob.hospital.sgo.oficio.EstadoOficio;
import mx.gob.hospital.sgo.oficio.Oficio;
import mx.gob.hospital.sgo.oficio.OficioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class TurnadoService {

    private static final Logger log = LoggerFactory.getLogger(TurnadoService.class);
    private static final int MAX_INTENTOS = 3;

    private final TurnadoRepository turnadoRepository;
    private final OficioRepository oficioRepository;
    private final AreaRepository areaRepository;
    private final ServicioCorreo servicioCorreo;

    public TurnadoService(TurnadoRepository turnadoRepository,
                          OficioRepository oficioRepository,
                          AreaRepository areaRepository,
                          ServicioCorreo servicioCorreo) {
        this.turnadoRepository = turnadoRepository;
        this.oficioRepository = oficioRepository;
        this.areaRepository = areaRepository;
        this.servicioCorreo = servicioCorreo;
    }

    @Transactional
    public List<Turnado> turnar(Long oficioId, List<Long> areaIds) {
        Oficio oficio = oficioRepository.findById(oficioId)
                .orElseThrow(() -> new IllegalArgumentException("Oficio no encontrado: " + oficioId));

        List<Turnado> creados = areaIds.stream().map(areaId -> {
            Area area = areaRepository.findById(areaId)
                    .orElseThrow(() -> new IllegalArgumentException("Área no encontrada: " + areaId));
            if (!area.isActiva()) {
                throw new IllegalArgumentException("El área «" + area.getNombre() + "» no está activa.");
            }
            if (turnadoRepository.existsByOficioIdAndAreaId(oficioId, areaId)) {
                throw new IllegalArgumentException(
                        "El oficio ya fue turnado al área «" + area.getNombre() + "».");
            }

            Turnado turnado = new Turnado();
            turnado.setOficio(oficio);
            turnado.setArea(area);
            turnado.setEstado(EstadoTurnado.PENDIENTE);
            return turnadoRepository.save(turnado);
        }).toList();

        log.info("Oficio {} turnado a {} área(s)", oficio.getFolio(), creados.size());
        return creados;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void procesarPendiente(Long turnadoId) {
        Turnado turnado = turnadoRepository.findConOficioYAreaById(turnadoId).orElse(null);
        if (turnado == null) return;

        if (turnado.getEstado() == EstadoTurnado.ENVIADO) {
            log.debug("Turnado {} ya está ENVIADO, se omite", turnadoId);
            return;
        }
        if (turnado.getIntentos() >= MAX_INTENTOS) return;

        try {
            servicioCorreo.enviar(turnado);
            turnado.setEstado(EstadoTurnado.ENVIADO);
            turnado.setEnviadoEn(Instant.now());
            turnado.setUltimoError(null);
            turnadoRepository.save(turnado);

            marcarOficioTurnadoSiCorresponde(turnado.getOficio().getId());
            log.info("Turnado {} enviado a {}", turnadoId, turnado.getArea().getCorreo());
        } catch (Exception e) {
            turnado.setEstado(EstadoTurnado.FALLIDO);
            turnado.setIntentos(turnado.getIntentos() + 1);
            String error = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            turnado.setUltimoError(error.length() > 500 ? error.substring(0, 500) : error);
            turnadoRepository.save(turnado);
            log.warn("Turnado {} falló (intento {}): {}", turnadoId, turnado.getIntentos(), error);
        }
    }

    @Transactional(readOnly = true)
    public List<Turnado> listarPorOficio(Long oficioId) {
        return turnadoRepository.findByOficioIdOrderByIdAsc(oficioId);
    }

    @Transactional(readOnly = true)
    public List<Long> obtenerIdsPendientes() {
        return turnadoRepository
                .findByEstadoInAndIntentosLessThan(
                        List.of(EstadoTurnado.PENDIENTE, EstadoTurnado.FALLIDO), MAX_INTENTOS)
                .stream().map(Turnado::getId).toList();
    }

    private void marcarOficioTurnadoSiCorresponde(Long oficioId) {
        Oficio oficio = oficioRepository.findById(oficioId).orElse(null);
        if (oficio != null && oficio.getEstado() != EstadoOficio.TURNADO) {
            oficio.setEstado(EstadoOficio.TURNADO);
            oficioRepository.save(oficio);
        }
    }
}
