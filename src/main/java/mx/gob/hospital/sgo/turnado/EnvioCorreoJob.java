package mx.gob.hospital.sgo.turnado;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EnvioCorreoJob {

    private static final Logger log = LoggerFactory.getLogger(EnvioCorreoJob.class);

    private final TurnadoService turnadoService;

    public EnvioCorreoJob(TurnadoService turnadoService) {
        this.turnadoService = turnadoService;
    }

    @Scheduled(fixedDelayString = "${sgo.correo.job.intervalo:60000}")
    public void procesar() {
        List<Long> ids = turnadoService.obtenerIdsPendientes();
        if (ids.isEmpty()) return;

        log.info("Procesando {} turnado(s) pendiente(s)", ids.size());
        int enviados = 0;
        for (Long id : ids) {
            try {
                turnadoService.procesarPendiente(id);
                enviados++;
            } catch (Exception e) {
                log.error("Error inesperado al procesar turnado {}: {}", id, e.getMessage());
            }
        }
        log.info("Procesados {}/{} turnado(s)", enviados, ids.size());
    }
}
