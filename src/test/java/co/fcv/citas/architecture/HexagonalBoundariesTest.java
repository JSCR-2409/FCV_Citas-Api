package co.fcv.citas.architecture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Verifica la restricción «arquitectura hexagonal: dominio/aplicación independientes de adaptadores».
 *
 * <p>Sin una prueba así, la restricción es una intención: nada impide que alguien importe un
 * `JdbcTemplate` en el dominio y el proyecto siga compilando. Esta clase lee el código fuente y falla
 * si una dependencia va en el sentido prohibido.
 *
 * <p>Se eligió leer los ficheros en lugar de añadir ArchUnit por una razón concreta: la restricción
 * también fija las dependencias del proyecto, y meter una librería nueva para comprobar el
 * cumplimiento de las restricciones sería una ironía innecesaria. Son treinta líneas.
 */
class HexagonalBoundariesTest {

  private static final Path SOURCES = Path.of("src/main/java/co/fcv/citas");

  /**
   * El dominio es el centro: no puede saber nada de frameworks ni de infraestructura.
   *
   * <p>Jakarta Persistence está en la lista a propósito. La restricción pide Spring Data JPA para
   * persistencia, y precisamente por eso las entidades viven en el adaptador: si el dominio llevara
   * `@Entity`, su forma quedaría dictada por el mapeo —constructor vacío, campos mutables, relaciones
   * perezosas— y dejaría de poder expresar invariantes.
   */
  @Test
  void elDominioNoDependeDeNingunFramework() throws IOException {
    var prohibidos = List.of(
        "org.springframework", "jakarta.persistence", "jakarta.servlet",
        "org.hibernate", "javax.sql", "java.sql");

    var infracciones = infracciones(SOURCES.resolve("domain"), prohibidos);

    Assertions.assertTrue(infracciones.isEmpty(),
        "el dominio debe ser independiente de la infraestructura:\n" + String.join("\n", infracciones));
  }

  /**
   * La aplicación orquesta y declara puertos. Puede usar Spring para transacciones e inyección, que es
   * una concesión consciente y documentada, pero no puede tocar persistencia ni HTTP: para eso están
   * los puertos.
   */
  @Test
  void laAplicacionNoDependeDePersistenciaNiDeHttp() throws IOException {
    var prohibidos = List.of(
        "org.springframework.jdbc", "org.springframework.data", "org.springframework.web",
        "org.springframework.http", "jakarta.persistence", "jakarta.servlet", "org.hibernate");

    var infracciones = infracciones(SOURCES.resolve("application"), prohibidos);

    Assertions.assertTrue(infracciones.isEmpty(),
        "la aplicación debe hablar con el exterior solo por puertos:\n" + String.join("\n", infracciones));
  }

  /** Ni el dominio ni la aplicación pueden importar un adaptador: la dependencia va al contrario. */
  @Test
  void elCentroNoImportaAdaptadores() throws IOException {
    var prohibidos = List.of("co.fcv.citas.adapters");

    var infracciones = new ArrayList<String>();
    infracciones.addAll(infracciones(SOURCES.resolve("domain"), prohibidos));
    infracciones.addAll(infracciones(SOURCES.resolve("application"), prohibidos));

    Assertions.assertTrue(infracciones.isEmpty(),
        "la dependencia debe apuntar hacia dentro, nunca hacia los adaptadores:\n"
        + String.join("\n", infracciones));
  }

  /**
   * El dominio tampoco puede depender de los paquetes heredados que todavía no se han migrado. Es la
   * prueba que impide que la deuda vuelva a entrar por la puerta de atrás mientras se completa la
   * migración.
   */
  @Test
  void elDominioNoDependeDeLosPaquetesHeredados() throws IOException {
    var prohibidos = List.of(
        "co.fcv.citas.appointment", "co.fcv.citas.auth", "co.fcv.citas.catalog",
        "co.fcv.citas.user", "co.fcv.citas.professional", "co.fcv.citas.integration",
        "co.fcv.citas.session", "co.fcv.citas.availability", "co.fcv.citas.config");

    var infracciones = infracciones(SOURCES.resolve("domain"), prohibidos);

    Assertions.assertTrue(infracciones.isEmpty(),
        "el dominio no debe depender de los paquetes heredados:\n" + String.join("\n", infracciones));
  }

  /** Comprueba que la estructura existe: si alguien borrara el dominio, el resto pasaría en vacío. */
  @Test
  void laEstructuraHexagonalExiste() {
    for (var esperado : List.of("domain/appointment", "domain/shared", "application/appointment",
        "application/appointment/port/out", "adapters/in/rest", "adapters/out/persistence")) {
      Assertions.assertTrue(Files.isDirectory(SOURCES.resolve(esperado)),
          "falta el paquete " + esperado);
    }
    Assertions.assertTrue(Files.exists(SOURCES.resolve("domain/appointment/Appointment.java")),
        "el dominio de citas debe existir");
  }

  /** Cada import prohibido encontrado, con su fichero y la línea, para que el fallo sea accionable. */
  private List<String> infracciones(Path raiz, List<String> prohibidos) throws IOException {
    if (!Files.isDirectory(raiz)) return List.of();
    var encontradas = new ArrayList<String>();
    try (Stream<Path> ficheros = Files.walk(raiz)) {
      for (Path fichero : ficheros.filter(p -> p.toString().endsWith(".java")).toList()) {
        List<String> lineas = Files.readAllLines(fichero);
        for (int i = 0; i < lineas.size(); i++) {
          String linea = lineas.get(i).trim();
          // Solo los imports y los usos cualificados en código, no los comentarios: un javadoc que
          // explique por qué algo está prohibido no es una infracción.
          if (linea.startsWith("//") || linea.startsWith("*") || linea.startsWith("/*")) continue;
          for (String prohibido : prohibidos) {
            if (linea.contains(prohibido)) {
              encontradas.add("  " + raiz.relativize(fichero) + ":" + (i + 1) + " → " + prohibido);
            }
          }
        }
      }
    }
    return encontradas;
  }
}
