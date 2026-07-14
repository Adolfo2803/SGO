package mx.gob.hospital.sgo.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AlmacenArchivosLocalTest {

    @TempDir
    Path tempDir;

    AlmacenArchivosLocal almacen;

    @BeforeEach
    void setUp() {
        almacen = new AlmacenArchivosLocal(tempDir.toString());
    }

    @Test
    void guardarCreaArchivoConRutaEsperada() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "oficio.pdf", "application/pdf",
                "%PDF-1.4 contenido de prueba".getBytes());

        String ruta = almacen.guardar(archivo);

        LocalDate hoy = LocalDate.now();
        String prefijo = hoy.getYear() + "/" + String.format("%02d", hoy.getMonthValue());
        assertThat(ruta).startsWith(prefijo);
        assertThat(ruta).endsWith(".pdf");
        assertThat(tempDir.resolve(ruta)).exists();
    }

    @Test
    void cargarDevuelveRecursoExistente() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "oficio.pdf", "application/pdf",
                "%PDF-1.4 contenido".getBytes());

        String ruta = almacen.guardar(archivo);

        Resource recurso = almacen.cargar(ruta);
        assertThat(recurso.exists()).isTrue();
    }

    @Test
    void eliminarBorraElArchivo() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "oficio.pdf", "application/pdf",
                "%PDF-1.4 contenido".getBytes());

        String ruta = almacen.guardar(archivo);
        assertThat(tempDir.resolve(ruta)).exists();

        almacen.eliminar(ruta);
        assertThat(tempDir.resolve(ruta)).doesNotExist();
    }
}
