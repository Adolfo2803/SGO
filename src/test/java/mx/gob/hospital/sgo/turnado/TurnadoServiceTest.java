package mx.gob.hospital.sgo.turnado;

import mx.gob.hospital.sgo.area.Area;
import mx.gob.hospital.sgo.area.AreaRepository;
import mx.gob.hospital.sgo.oficio.Direccion;
import mx.gob.hospital.sgo.oficio.EstadoOficio;
import mx.gob.hospital.sgo.oficio.Oficio;
import mx.gob.hospital.sgo.oficio.OficioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TurnadoServiceTest {

    @Mock TurnadoRepository turnadoRepository;
    @Mock OficioRepository oficioRepository;
    @Mock AreaRepository areaRepository;
    @Mock ServicioCorreo servicioCorreo;

    @InjectMocks TurnadoService turnadoService;

    @Test
    void turnar_creaPendientesYNoEnviaCorreo() throws Exception {
        Oficio oficio = crearOficio(1L);
        Area area1 = crearArea(10L, "RH");
        Area area2 = crearArea(20L, "Enseñanza");

        when(oficioRepository.findById(1L)).thenReturn(Optional.of(oficio));
        when(areaRepository.findById(10L)).thenReturn(Optional.of(area1));
        when(areaRepository.findById(20L)).thenReturn(Optional.of(area2));
        when(turnadoRepository.existsByOficioIdAndAreaId(any(), any())).thenReturn(false);
        when(turnadoRepository.save(any(Turnado.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Turnado> resultado = turnadoService.turnar(1L, List.of(10L, 20L));

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allMatch(t -> t.getEstado() == EstadoTurnado.PENDIENTE);
        verify(servicioCorreo, never()).enviar(any());
    }

    @Test
    void procesarPendiente_marcaFallidoCuandoCorreoFalla() throws Exception {
        Turnado turnado = crearTurnado(1L, EstadoTurnado.PENDIENTE, 0);
        when(turnadoRepository.findConOficioYAreaById(1L)).thenReturn(Optional.of(turnado));
        when(turnadoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP timeout")).when(servicioCorreo).enviar(turnado);

        turnadoService.procesarPendiente(1L);

        assertThat(turnado.getEstado()).isEqualTo(EstadoTurnado.FALLIDO);
        assertThat(turnado.getIntentos()).isEqualTo(1);
        assertThat(turnado.getUltimoError()).contains("SMTP timeout");
    }

    @Test
    void procesarPendiente_ignoraTurnadoYaEnviado() throws Exception {
        Turnado turnado = crearTurnado(1L, EstadoTurnado.ENVIADO, 1);
        when(turnadoRepository.findConOficioYAreaById(1L)).thenReturn(Optional.of(turnado));

        turnadoService.procesarPendiente(1L);

        verify(servicioCorreo, never()).enviar(any());
        verify(turnadoRepository, never()).save(any());
    }

    private Oficio crearOficio(Long id) {
        Oficio o = new Oficio();
        o.setId(id);
        o.setFolio("OF-001");
        o.setDireccion(Direccion.ENTRADA);
        o.setEstado(EstadoOficio.RECIBIDO);
        o.setFecha(LocalDate.of(2026, 7, 13));
        o.setRemitente("Remitente");
        o.setDestinatario("Destinatario");
        o.setAsunto("Asunto");
        o.setCreadoPor("test");
        o.setCreadoEn(Instant.now());
        return o;
    }

    private Area crearArea(Long id, String nombre) {
        Area a = new Area();
        a.setId(id);
        a.setNombre(nombre);
        a.setCorreo(nombre.toLowerCase() + "@hospital.local");
        a.setActiva(true);
        return a;
    }

    private Turnado crearTurnado(Long id, EstadoTurnado estado, int intentos) {
        Turnado t = new Turnado();
        t.setId(id);
        t.setOficio(crearOficio(1L));
        t.setArea(crearArea(10L, "RH"));
        t.setEstado(estado);
        t.setIntentos(intentos);
        return t;
    }
}
