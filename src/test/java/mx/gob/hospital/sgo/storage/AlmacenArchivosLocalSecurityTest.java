package mx.gob.hospital.sgo.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlmacenArchivosLocalSecurityTest {

    @TempDir
    Path tempDir;

    AlmacenArchivosLocal almacen;

    @BeforeEach
    void setUp() {
        almacen = new AlmacenArchivosLocal(tempDir.toString());
    }

    @Test
    void cargar_pathTraversal_lanzaExcepcion() throws IOException {
        Files.writeString(tempDir.resolve("secreto.txt"), "datos sensibles");

        assertThatThrownBy(() -> almacen.cargar("../../application.yml"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("fuera del almacén");
    }

    @Test
    void cargar_pathTraversalConBackslash_lanzaExcepcion() {
        assertThatThrownBy(() -> almacen.cargar("..\\..\\application.yml"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("no permitido");
    }

    @Test
    void cargar_rutaValidaDentroDeRaiz_funciona() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "test.pdf", "application/pdf",
                "%PDF-1.4 test".getBytes());
        String ruta = almacen.guardar(archivo);

        assertThat(almacen.cargar(ruta).exists()).isTrue();
    }
}
