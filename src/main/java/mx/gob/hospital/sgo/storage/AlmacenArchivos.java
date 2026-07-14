package mx.gob.hospital.sgo.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface AlmacenArchivos {

    String guardar(MultipartFile archivo);

    Resource cargar(String rutaRelativa);

    void eliminar(String rutaRelativa);
}
