package mx.gob.hospital.sgo.oficio;

import mx.gob.hospital.sgo.storage.AlmacenArchivos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OficioSalidaServiceTest {

    @Mock OficioRepository oficioRepository;
    @Mock AlmacenArchivos almacenArchivos;
    @Mock GeneradorFolio generadorFolio;

    @InjectMocks OficioService oficioService;

    @Test
    void registrarSalida_asignaFolioGeneradoYDireccionSalida() throws Exception {
        setNombreDireccion("Dirección del Hospital General");

        when(generadorFolio.siguienteFolio(2026)).thenReturn("HG/DIR/0001/2026");
        when(almacenArchivos.guardar(any())).thenReturn("2026/07/uuid.pdf");
        when(oficioRepository.save(any(Oficio.class))).thenAnswer(inv -> {
            Oficio o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        Oficio resultado = oficioService.registrarSalida(
                LocalDate.of(2026, 7, 13),
                "Secretaría de Salud",
                "Respuesta a solicitud",
                archivoPdfValido());

        assertThat(resultado.getFolio()).isEqualTo("HG/DIR/0001/2026");
        assertThat(resultado.getDireccion()).isEqualTo(Direccion.SALIDA);
        assertThat(resultado.getRemitente()).isEqualTo("Dirección del Hospital General");
        assertThat(resultado.getEstado()).isEqualTo(EstadoOficio.RECIBIDO);
    }

    @Test
    void registrarSalida_formatoFolioCorrecto() throws Exception {
        setNombreDireccion("Dirección del Hospital General");

        when(generadorFolio.siguienteFolio(2026)).thenReturn("HG/DIR/0042/2026");
        when(almacenArchivos.guardar(any())).thenReturn("2026/07/uuid.pdf");
        when(oficioRepository.save(any(Oficio.class))).thenAnswer(inv -> inv.getArgument(0));

        Oficio resultado = oficioService.registrarSalida(
                LocalDate.of(2026, 7, 13),
                "Destinatario",
                "Asunto",
                archivoPdfValido());

        assertThat(resultado.getFolio()).matches("HG/DIR/\\d{4}/\\d{4}");
    }

    private MockMultipartFile archivoPdfValido() {
        return new MockMultipartFile(
                "archivo", "oficio.pdf", "application/pdf",
                "%PDF-1.4 contenido de prueba".getBytes());
    }

    private void setNombreDireccion(String nombre) throws Exception {
        Field campo = OficioService.class.getDeclaredField("nombreDireccion");
        campo.setAccessible(true);
        campo.set(oficioService, nombre);
    }
}
