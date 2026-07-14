package mx.gob.hospital.sgo.oficio;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class GeneradorFolioIntegrationTest {

    @Autowired
    GeneradorFolio generadorFolio;

    @Test
    void diezHilosConcurrentes_producenDiezFoliosDistintos() throws InterruptedException {
        int hilos = 10;
        int anio = 9999;
        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch arranque = new CountDownLatch(1);
        List<String> folios = new CopyOnWriteArrayList<>();

        for (int i = 0; i < hilos; i++) {
            executor.submit(() -> {
                listos.countDown();
                try {
                    arranque.await();
                    String folio = generadorFolio.siguienteFolio(anio);
                    folios.add(folio);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        listos.await();
        arranque.countDown();
        executor.shutdown();
        executor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(folios).hasSize(hilos);
        assertThat(Set.copyOf(folios)).hasSize(hilos);
    }

    @Test
    void contadorReiniciaAlCambiarDeAnio() {
        int anioA = 3050 + (int) (System.nanoTime() % 1000);
        int anioB = anioA + 1;

        String folioA = generadorFolio.siguienteFolio(anioA);
        assertThat(folioA).isEqualTo(String.format("HG/DIR/0001/%d", anioA));

        String folioB = generadorFolio.siguienteFolio(anioB);
        assertThat(folioB).isEqualTo(String.format("HG/DIR/0001/%d", anioB));
    }
}
