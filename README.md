# AlquileresBackend · EquipAndes

API REST para el alquiler de equipos de sonido, proyección e iluminación para eventos.
Examen Parcial U1 · Lenguaje de Programación II · Versión D.

**Stack:** Spring Boot 4.0.9 · Java 21 · Maven · Spring Data JPA / Hibernate · Oracle · springdoc-openapi 3 · Bean Validation · SLF4J.

## Ejecución

1. **Oracle** (una sola vez), conectado como `SYSTEM`: ejecutar [`db/01_crear_usuario.sql`](db/01_crear_usuario.sql).
   Crea el usuario `ALQUILERESDB / alquileres123` en `FREEPDB1`.
2. **Arrancar la API** con el perfil `dev` (es el perfil por defecto):
   ```bash
   ./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
   ```
   Hibernate crea las tablas (`ddl-auto: update`).
3. **Datos semilla**, conectado como `ALQUILERESDB`: ejecutar [`db/datos_semilla.sql`](db/datos_semilla.sql).
   El script es re-ejecutable: limpia los datos y reinicia los ids, así el Anexo B siempre empieza con una base recién cargada.
4. **Swagger UI:** http://localhost:8080/swagger-ui.html (título «Alquileres API», versión v1).
5. **Postman:** importar [`postman/EquipAndes-AnexoB.postman_collection.json`](postman/EquipAndes-AnexoB.postman_collection.json)
   y ejecutar la colección en orden (Runner). Las fechas F (hoy + 7), ayer y el periodo se calculan solas.
   Las solicitudes de cambio de la Parte II se prueban con [`postman/EquipAndes-ParteII.postman_collection.json`](postman/EquipAndes-ParteII.postman_collection.json),
   también sobre una base recién cargada.
6. **Pruebas automatizadas:** `./mvnw test` (23 pruebas, 10 de ellas de la Parte II: Mockito sobre las reglas y `@WebMvcTest` del contrato HTTP; no requieren Oracle).

## Perfiles

| Perfil | Base de datos | `ddl-auto` | Swagger | CORS |
|---|---|---|---|---|
| `dev` | Oracle local `FREEPDB1` | `update` | habilitado | `http://localhost:4200` |
| `prod` | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (variables de entorno) | `validate` | deshabilitado | `FRONTEND_ORIGINS` |

```bash
java -jar target/AlquileresBackend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Estructura de paquetes

```
src/main/java/pe/edu/upeu/AlquileresBackend/
├── config/        CorsConfig, OpenApiConfig
├── controller/    Health, Categoria, Equipo, Cliente, Alquiler, Reporte (solo delegan al servicio)
├── dto/           *RequestDTO, *ResponseDTO, EquipoAlquiladoDTO e IngresoCategoriaDTO (proyecciones de los reportes)
├── entity/        Categoria, Equipo, Cliente, Alquiler, DetalleAlquiler
├── enums/         EstadoAlquiler, TipoCliente, CondicionEquipo
├── exception/     RecursoNoEncontradoException (404), ReglaNegocioException (409),
│   │              SolicitudInvalidaException (400), GlobalExceptionHandler
│   └── dto/       ErrorResponseDTO
├── repository/    Categoria/Equipo/Cliente/AlquilerRepository, EquipoSpecifications
└── service/
    ├── generic/   CrudService<Request, Response, ID>
    ├── service/   CategoriaService, EquipoService, ClienteService, AlquilerService
    └── impl/      *ServiceImpl (aquí viven todas las reglas)
```

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/health` | `{status, database, timestamp}`; 503 si Oracle no responde |
| GET/POST/PUT/DELETE | `/api/v1/categorias[/{id}]` | CRUD; nombre único sin distinguir mayúsculas ni espacios |
| GET | `/api/v1/categorias/{id}/equipos` | Equipos de una categoría |
| GET/POST/PUT/DELETE | `/api/v1/equipos[/{id}]` | CRUD con `categoriaId` / `categoriaNombre` |
| GET | `/api/v1/equipos/buscar` | `nombre`, `categoriaId`, `tarifaMax`, `disponible`, `condicion=NUEVO\|BUENO\|REGULAR` (**SC-D**), `orden=nombre\|tarifa\|unidades`, `dir=asc\|desc` |
| GET/POST/PUT | `/api/v1/clientes[/{id}]` | Registro, consulta, actualización y listado |
| GET | `/api/v1/clientes/{id}/alquileres?estado=VIGENTE\|DEVUELTO` | **SC-A.** Alquileres del cliente del más reciente al más antiguo por fecha de inicio; 404 si el cliente no existe |
| POST | `/api/v1/alquileres` | Cabecera + detalles en una transacción |
| GET | `/api/v1/alquileres[/{id}]` | Cabecera con detalles |
| PATCH | `/api/v1/alquileres/{id}/devolver` | Devuelve y repone unidades |
| DELETE | `/api/v1/alquileres/{id}/equipos/{equipoId}` | **SC-B.** Retira un equipo de un alquiler `VIGENTE`: repone unidades y recalcula unidades, subtotal, descuento y monto; 409 si el alquiler quedaría sin equipos |
| GET | `/api/v1/reportes/equipos-alquilados?periodo=AAAA-MM&categoriaId=` | Unidades e importe bruto por equipo |
| GET | `/api/v1/reportes/ingresos-por-categoria?periodo=AAAA-MM` | **SC-C.** Categoría, número de alquileres, unidades e importe bruto; JPQL con `GROUP BY` y proyección `IngresoCategoriaDTO`; 200 con `[]` si no hay datos |

## Decisiones de arquitectura

**Dónde vive cada regla de negocio.** Todas las reglas están en [`AlquilerServiceImpl`](src/main/java/pe/edu/upeu/AlquileresBackend/service/impl/AlquilerServiceImpl.java); ninguna en controladores, triggers ni procedimientos.

| Regla | Ubicación | Cómo |
|---|---|---|
| RN-01 | `registrar()` | cliente activo, `fechaInicio` entre hoy y hoy + 60, equipo activo |
| RN-02 | `registrar()` / `devolver()` | `cantidad ≤ unidadesDisponibles`; descuenta al alquilar, repone al devolver; solo devuelve un alquiler `VIGENTE` |
| RN-03 | `registrar()` + `AlquilerRepository.contarPorClienteYEstado` | consulta agregada `SELECT COUNT` |
| RN-04 | `registrar()`, `calcularImporte()`, `calcularTotales()` | sin equipos repetidos; importe = cantidad × tarifa × días; 15 % si días ≥ 7 |

**Atomicidad.** `registrar()` y `devolver()` llevan `@Transactional`. `ReglaNegocioException` es una `RuntimeException`, así que cualquier regla que falle provoca rollback: no se guarda la cabecera, ningún detalle ni el descuento de unidades.

**Sin `equipoRepository.save()` tras descontar unidades.** El equipo se lee dentro de la transacción, así que es una entidad *gestionada*. Hibernate compara su estado al hacer commit (*dirty checking*) y emite el `UPDATE` automáticamente.

**Retirar un equipo (SC-B).** `AlquilerServiceImpl.retirarEquipo()` es `@Transactional`: repone las unidades, quita el detalle de la lista (`Alquiler.quitarDetalle`) y vuelve a llamar a `calcularTotales()`, el mismo método que usa el registro, así el descuento semanal se recalcula sin duplicar lógica. Como la relación tiene `orphanRemoval = true`, Hibernate emite el `DELETE` del detalle al hacer commit. El controlador solo delega.

**Condición del equipo (SC-D).** El enum `CondicionEquipo` (NUEVO, BUENO, REGULAR) recorre las capas: entidad (`@Enumerated(STRING)`), `EquipoRequestDTO` (opcional, validado con `@Pattern` para que el error salga en `validationErrors.condicion`), `EquipoResponseDTO`, servicio (BUENO por defecto al registrar; al actualizar sin condición se conserva la actual), filtro de `EquipoSpecifications` y datos semilla. La columna admite null solo para que `ddl-auto: update` pueda agregarla a una tabla con filas; la aplicación siempre guarda un valor.

**Copia de la tarifa en el detalle.** `DetalleAlquiler.tarifaDiaria` guarda la tarifa vigente al momento del alquiler. Si EquipAndes sube sus precios en diciembre, los alquileres y el reporte de noviembre no cambian.

**BigDecimal con escala 2 y `HALF_UP`.** `double` es binario y no representa exactamente valores como 0.15, lo que acumula errores de centavos. `BigDecimal` es decimal exacto, y `HALF_UP` es el redondeo comercial.

**Concurrencia.** El equipo se lee con `@Lock(PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`) en `EquipoRepository.findByIdParaActualizar`. Si dos clientes alquilan a la vez las últimas unidades, el segundo espera a que termine la transacción del primero, ve el stock actualizado y recibe 409 por RN-02.

**DTO y relaciones.** Las relaciones `@ManyToOne` son `LAZY`. Los DTO son planos (`categoriaId`, `categoriaNombre`, `clienteCodigo`…): no hay referencias circulares ni entidades expuestas. `spring.jpa.open-in-view=false`, así que todo mapeo a DTO ocurre dentro de la transacción del servicio. Para el detalle del alquiler se usa `join fetch` y así se evita el problema N+1.

**Errores uniformes.** El [`GlobalExceptionHandler`](src/main/java/pe/edu/upeu/AlquileresBackend/exception/GlobalExceptionHandler.java) traduce las excepciones a `ErrorResponseDTO`: 400 (con `validationErrors`), 404, 409 y 500 genérico sin traza. Los logs usan WARN para las reglas de negocio y ERROR para los 500. Los servicios registran en INFO las operaciones que modifican datos y los reportes.

**Consultas.** La búsqueda usa `Specification`: cada filtro solo se agrega si llega el parámetro, y el campo de orden se valida con una lista blanca (si no está, responde 400). El reporte es una consulta JPQL con `GROUP BY` y la proyección `SELECT new EquipoAlquiladoDTO(...)`, sin bucles sobre `findAll()`.

**CORS.** Se configura de forma global para `/api/**`, solo para el origen del perfil (`http://localhost:4200` en dev). Un preflight desde otro origen recibe 403. Para probarlo sin frontend, envía `OPTIONS` con las cabeceras `Origin` y `Access-Control-Request-Method` (CP-16 en Postman, o `curl -i -X OPTIONS ...`).

**Cambiar Oracle por PostgreSQL.** En el `pom.xml` cambia `ojdbc11` por `org.postgresql:postgresql`. En `application-dev.yaml` y `application-prod.yaml` cambia la URL, el driver y las credenciales. En `db/` adapta los scripts (los `ALTER ... IDENTITY` y los booleanos). Entidades, repositorios, servicios y controladores no cambian, porque JPA/JPQL abstrae el motor.

## Trabajo colaborativo con Git

Ramas: `main` (solo fusiones) ← `develop` ← `feature/<funcionalidad>-<apellido>`, `fix/...`, `sc-<letra>-<apellido>`.
Commits en español con prefijos `feat`, `fix`, `refactor`, `test` y `docs`. Al cierre: `git tag -a v1.0-unidad1`.
