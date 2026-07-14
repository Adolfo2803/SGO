package mx.gob.hospital.sgo.oficio;

import mx.gob.hospital.sgo.common.ArchivoInvalidoException;
import mx.gob.hospital.sgo.common.FolioDuplicadoException;
import mx.gob.hospital.sgo.storage.AlmacenArchivos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OficioServiceTest {

    @Mock
    OficioRepository oficioRepository;

    @Mock
    AlmacenArchivos almacenArchivos;

    @InjectMocks
    OficioService oficioService;

    @Test
    void registrarEntrada_casoFeliz() {
        MockMultipartFile archivo = archivoPdfValido();
        when(oficioRepository.existsByFolio("OF-001")).thenReturn(false);
        when(almacenArchivos.guardar(archivo)).thenReturn("2026/07/abc.pdf");
        when(oficioRepository.save(any(Oficio.class))).thenAnswer(inv -> {
            Oficio o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        Oficio resultado = oficioService.registrarEntrada(
                "OF-001", LocalDate.of(2026, 7, 13),
                "Secretaría de Salud", "Director General",
                "Solicitud de informe", archivo);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getFolio()).isEqualTo("OF-001");
        assertThat(resultado.getDireccion()).isEqualTo(Direccion.ENTRADA);
        assertThat(resultado.getEstado()).isEqualTo(EstadoOficio.RECIBIDO);
        assertThat(resultado.getRutaArchivo()).isEqualTo("2026/07/abc.pdf");
        verify(almacenArchivos).guardar(archivo);
    }

    @Test
    void registrarEntrada_folioDuplicado() {
        MockMultipartFile archivo = archivoPdfValido();
        when(oficioRepository.existsByFolio("DUP-001")).thenReturn(true);

        assertThatThrownBy(() -> oficioService.registrarEntrada(
                "DUP-001", LocalDate.of(2026, 7, 13),
                "Remitente", "Destinatario", "Asunto", archivo))
                .isInstanceOf(FolioDuplicadoException.class)
                .hasMessageContaining("DUP-001");

        verify(almacenArchivos, never()).guardar(any());
    }

    @Test
    void registrarEntrada_archivoNoPdf() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "documento.txt", "text/plain",
                "Esto no es un PDF".getBytes());

        assertThatThrownBy(() -> oficioService.registrarEntrada(
                "OF-002", LocalDate.of(2026, 7, 13),
                "Remitente", "Destinatario", "Asunto", archivo))
                .isInstanceOf(ArchivoInvalidoException.class)
                .hasMessageContaining("PDF válido");

        verify(almacenArchivos, never()).guardar(any());
    }

    @Test
    void registrarEntrada_archivoVacio() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "vacio.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> oficioService.registrarEntrada(
                "OF-003", LocalDate.of(2026, 7, 13),
                "Remitente", "Destinatario", "Asunto", archivo))
                .isInstanceOf(ArchivoInvalidoException.class)
                .hasMessageContaining("adjuntar");

        verify(almacenArchivos, never()).guardar(any());
    }

    @Test
    void registrarEntrada_archivoMuyGrande() {
        byte[] contenido = new byte[11 * 1024 * 1024];
        contenido[0] = '%';
        contenido[1] = 'P';
        contenido[2] = 'D';
        contenido[3] = 'F';
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "grande.pdf", "application/pdf", contenido);

        assertThatThrownBy(() -> oficioService.registrarEntrada(
                "OF-004", LocalDate.of(2026, 7, 13),
                "Remitente", "Destinatario", "Asunto", archivo))
                .isInstanceOf(ArchivoInvalidoException.class)
                .hasMessageContaining("10 MB");

        verify(almacenArchivos, never()).guardar(any());
    }

    private MockMultipartFile archivoPdfValido() {
        return new MockMultipartFile(
                "archivo", "oficio.pdf", "application/pdf",
                "%PDF-1.4 contenido de prueba".getBytes());
    }
}
