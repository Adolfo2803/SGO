package mx.gob.hospital.sgo.oficio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class OficioForm {

    @NotBlank(message = "El folio es obligatorio")
    @Size(max = 50, message = "El folio no puede exceder 50 caracteres")
    private String folio;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotBlank(message = "El remitente es obligatorio")
    @Size(max = 255, message = "El remitente no puede exceder 255 caracteres")
    private String remitente;

    @NotBlank(message = "El destinatario es obligatorio")
    @Size(max = 255, message = "El destinatario no puede exceder 255 caracteres")
    private String destinatario;

    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 500, message = "El asunto no puede exceder 500 caracteres")
    private String asunto;

    public OficioForm() {
    }

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getRemitente() {
        return remitente;
    }

    public void setRemitente(String remitente) {
        this.remitente = remitente;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }
}
