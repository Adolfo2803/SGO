package mx.gob.hospital.sgo.common;

public class FolioDuplicadoException extends RuntimeException {

    public FolioDuplicadoException(String folio) {
        super("Ya existe un oficio registrado con el folio «" + folio + "».");
    }
}
