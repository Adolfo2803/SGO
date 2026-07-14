package mx.gob.hospital.sgo.usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;

@Component
public class UsuarioSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuarioSeedRunner.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public UsuarioSeedRunner(UsuarioRepository usuarioRepository,
                             PasswordEncoder passwordEncoder,
                             Environment environment) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        String password = environment.getProperty("SGO_ADMIN_PASSWORD");

        if (password == null || password.isBlank()) {
            boolean esProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
            if (esProd) {
                throw new IllegalStateException(
                        "La variable SGO_ADMIN_PASSWORD es obligatoria en producción. " +
                        "Defínala antes de arrancar la aplicación.");
            }
            password = "cambiar123";
            log.warn("SGO_ADMIN_PASSWORD no definida — se crea usuario admin con contraseña por defecto. " +
                     "NO usar en producción.");
        }

        Usuario admin = new Usuario();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setNombreCompleto("Administrador del Sistema");
        admin.setRol("SECRETARIA");
        admin.setCreadoEn(Instant.now());
        usuarioRepository.save(admin);

        log.info("Usuario semilla 'admin' creado exitosamente.");
    }
}
