package pe.edu.upeu.AlquileresBackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upeu.AlquileresBackend.exception.ReglaNegocioException;
import pe.edu.upeu.AlquileresBackend.service.service.EquipoService;

/** Prueba de la capa web: validación (400) y traducción de reglas a 409 por el GlobalExceptionHandler. */
@WebMvcTest(EquipoController.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:4200")
class EquipoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EquipoService equipoService;

    @Test
    @DisplayName("CP-03: código, nombre, tarifa y unidades inválidos → 400 con los cuatro campos")
    void validacionDevuelve400() throws Exception {
        String body = """
                {"codigo":"eqp1","nombre":"TV","tarifaDiaria":0,"unidadesDisponibles":-1,"categoriaId":1}
                """;
        mockMvc.perform(post("/api/v1/equipos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.codigo").exists())
                .andExpect(jsonPath("$.validationErrors.nombre").exists())
                .andExpect(jsonPath("$.validationErrors.tarifaDiaria").exists())
                .andExpect(jsonPath("$.validationErrors.unidadesDisponibles").exists());
        verify(equipoService, never()).registrar(any());
    }

    @Test
    @DisplayName("Una ReglaNegocioException del servicio termina como 409 con ErrorResponseDTO")
    void reglaDeNegocioDevuelve409() throws Exception {
        when(equipoService.registrar(any())).thenThrow(new ReglaNegocioException("Ya existe un equipo con el código EQP-001"));
        String body = """
                {"codigo":"EQP-001","nombre":"Parlante","tarifaDiaria":80.00,"unidadesDisponibles":10,"categoriaId":1}
                """;
        mockMvc.perform(post("/api/v1/equipos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Ya existe un equipo con el código EQP-001"));
    }
}
