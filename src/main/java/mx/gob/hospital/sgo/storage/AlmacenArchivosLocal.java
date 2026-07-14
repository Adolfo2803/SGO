package mx.gob.hospital.sgo.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class AlmacenArchivosLocal implements AlmacenArchivos {

    private final Path raiz;

    public AlmacenArchivosLocal(@Value("${sgo.almacen.raiz}") String raiz) {
        this.raiz = Path.of(raiz);
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(raiz);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear el directorio de almacenamiento: " + raiz, e);
        }
    }

    @Override
    public String guardar(MultipartFile archivo) {
        LocalDate hoy = LocalDate.now();
        String directorio = hoy.getYear() + "/" + String.format("%02d", hoy.getMonthValue());
        String nombre = UUID.randomUUID() + ".pdf";
        String rutaRelativa = directorio + "/" + nombre;

        Path destino = raiz.resolve(rutaRelativa);
        try {
            Files.createDirectories(destino.getParent());
            archivo.transferTo(destino);
        } catch (IOException e) {
            throw new UncheckedIOException("Error al guardar el archivo", e);
        }

        return rutaRelativa;
    }

    @Override
    public Resource cargar(String rutaRelativa) {
        validarRuta(rutaRelativa);
        Path archivo = raiz.resolve(rutaRelativa).normalize();
        if (!archivo.startsWith(raiz)) {
            throw new SecurityException("Acceso denegado: ruta fuera del almacén");
        }
        Resource recurso = new FileSystemResource(archivo);
        if (!recurso.exists()) {
            throw new RuntimeException("Archivo no encontrado: " + rutaRelativa);
        }
        return recurso;
    }

    @Override
    public void eliminar(String rutaRelativa) {
        try {
            validarRuta(rutaRelativa);
            Path archivo = raiz.resolve(rutaRelativa).normalize();
            if (archivo.startsWith(raiz)) {
                Files.deleteIfExists(archivo);
            }
        } catch (SecurityException ignored) {
        } catch (IOException ignored) {
        }
    }

    private void validarRuta(String rutaRelativa) {
        if (rutaRelativa.contains("\\")) {
            throw new SecurityException("Acceso denegado: carácter no permitido en la ruta");
        }
    }
}
