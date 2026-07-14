package mx.gob.hospital.sgo.oficio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class OficioSalidaForm {

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotBlank(message = "El destinatario es obligatorio")
    @Size(max = 255, message = "El destinatario no puede exceder 255 caracteres")
    private String destinatario;

    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 500, message = "El asunto no puede exceder 500 caracteres")
    private String asunto;

    public OficioSalidaForm() {
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
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
