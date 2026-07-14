package mx.gob.hospital.sgo.oficio;

import mx.gob.hospital.sgo.common.ArchivoInvalidoException;
import mx.gob.hospital.sgo.common.FolioDuplicadoException;
import mx.gob.hospital.sgo.storage.AlmacenArchivos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@Service
public class OficioService {

    private static final long TAMANO_MAXIMO = 10L * 1024 * 1024;
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F'};
    private static final int TAMANO_PAGINA = 20;
    private static final Sort ORDEN_POR_DEFECTO = Sort.by(
            Sort.Order.desc("fecha"), Sort.Order.desc("id"));

    private final OficioRepository oficioRepository;
    private final AlmacenArchivos almacenArchivos;
    private final GeneradorFolio generadorFolio;
    private final String nombreDireccion;

    public OficioService(OficioRepository oficioRepository,
                         AlmacenArchivos almacenArchivos,
                         GeneradorFolio generadorFolio,
                         @Value("${sgo.direccion.nombre:Dirección del Hospital General}") String nombreDireccion) {
        this.oficioRepository = oficioRepository;
        this.almacenArchivos = almacenArchivos;
        this.generadorFolio = generadorFolio;
        this.nombreDireccion = nombreDireccion;
    }

    @Transactional(readOnly = true)
    public Page<Oficio> buscar(FiltroOficioForm filtro, Pageable pageable) {
        Specification<Oficio> spec = Specification.where(
                        OficioSpecifications.fechaDesde(filtro.getFechaDesde()))
                .and(OficioSpecifications.fechaHasta(filtro.getFechaHasta()))
                .and(OficioSpecifications.direccion(filtro.getDireccion()))
                .and(OficioSpecifications.estado(filtro.getEstado()))
                .and(OficioSpecifications.textoLibre(filtro.getTexto()))
                .and(OficioSpecifications.areaTurnada(filtro.getAreaId()));

        Pageable paginado = PageRequest.of(
                pageable.getPageNumber(), TAMANO_PAGINA, ORDEN_POR_DEFECTO);

        return oficioRepository.findAll(spec, paginado);
    }

    @Transactional(readOnly = true)
    public Oficio buscarPorId(Long id) {
        return oficioRepository.findById(id).orElse(null);
    }

    /*
     * Estrategia de consistencia archivo-BD:
     * 1. Se guarda el PDF en disco ANTES del commit de BD.
     * 2. Se registra un TransactionSynchronization que elimina el archivo
     *    si la transacción hace rollback.
     * 3. Si el commit tiene éxito, el archivo permanece.
     * Riesgo aceptado: si la app cae entre el commit y antes de responder,
     * el archivo ya existe y la BD también — estado consistente.
     */
    @Transactional
    public Oficio registrarEntrada(String folio, LocalDate fecha, String remitente,
                                   String destinatario, String asunto,
                                   MultipartFile archivo) {

        if (oficioRepository.existsByFolio(folio)) {
            throw new FolioDuplicadoException(folio);
        }

        Oficio oficio = new Oficio();
        oficio.setFolio(folio);
        oficio.setDireccion(Direccion.ENTRADA);
        oficio.setFecha(fecha);
        oficio.setRemitente(remitente);
        oficio.setDestinatario(destinatario);
        oficio.setAsunto(asunto);

        return guardarOficio(oficio, archivo);
    }

    @Transactional
    public Oficio registrarSalida(LocalDate fecha, String destinatario, String asunto,
                                  MultipartFile archivo) {

        String folio = generadorFolio.siguienteFolio(fecha.getYear());

        Oficio oficio = new Oficio();
        oficio.setFolio(folio);
        oficio.setDireccion(Direccion.SALIDA);
        oficio.setFecha(fecha);
        oficio.setRemitente(nombreDireccion);
        oficio.setDestinatario(destinatario);
        oficio.setAsunto(asunto);

        return guardarOficio(oficio, archivo);
    }

    private Oficio guardarOficio(Oficio oficio, MultipartFile archivo) {
        validarArchivo(archivo);

        String rutaRelativa = almacenArchivos.guardar(archivo);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        almacenArchivos.eliminar(rutaRelativa);
                    }
                }
            });
        }

        oficio.setEstado(EstadoOficio.RECIBIDO);
        oficio.setRutaArchivo(rutaRelativa);

        try {
            return oficioRepository.save(oficio);
        } catch (DataIntegrityViolationException e) {
            throw new FolioDuplicadoException(oficio.getFolio());
        }
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException("Debe adjuntar un archivo PDF.");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new ArchivoInvalidoException("El archivo no debe exceder 10 MB.");
        }
        try {
            byte[] cabecera = new byte[4];
            int leidos = archivo.getInputStream().read(cabecera);
            if (leidos < 4
                    || cabecera[0] != PDF_MAGIC[0]
                    || cabecera[1] != PDF_MAGIC[1]
                    || cabecera[2] != PDF_MAGIC[2]
                    || cabecera[3] != PDF_MAGIC[3]) {
                throw new ArchivoInvalidoException("El archivo debe ser un PDF válido.");
            }
        } catch (IOException e) {
            throw new ArchivoInvalidoException("No se pudo leer el archivo.");
        }
    }
}
