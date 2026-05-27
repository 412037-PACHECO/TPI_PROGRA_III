# 01 - Requirements Map

## Clasificación

- **Obligatorio**: requerido para aprobar el TPI.
- **Opcional**: suma puntaje si lo obligatorio está completo.
- **Recomendado**: decisión técnica conveniente para mantener calidad.

## Requerimientos funcionales

| ID | Requerimiento | Tipo | Impacto backend | Impacto frontend | Impacto testing | Dependencias |
|---|---|---|---|---|---|---|
| RF-01 | Reglas del juego: setup, mulligan, turno, ataque, KO, estados, victoria | Obligatorio | Game Engine, comandos, validadores, resolvers | Vista de fases y acciones disponibles | Unit tests engine + integración | RF-02, RF-03, RF-05 |
| RF-02 | Tipos de cartas y restricciones | Obligatorio | Modelo carta, validaciones, efectos | Render por tipo/subtipo | Tests de deck y engine | RF-04, RF-01 |
| RF-03 | Gestión del juego y estados | Obligatorio | Match lifecycle, turnos, logs, snapshots | Lobby/tablero | Tests de flujo | RF-01, RF-05, RF-06 |
| RF-04 | Construcción de mazos | Obligatorio | Catálogo, caché, validación, CRUD mazos. Para alcance base `xy1`, no validar AS TÁCTICO / ACE SPEC porque el set no contiene esa mecánica. | Deck Builder futuro | Tests de validación XY1; ACE SPEC solo si se agregan sets opcionales | API externa, RF-02 |
| RF-05 | Persistencia del estado | Obligatorio | Snapshots, logs, reconstrucción | Reconexión consistente | Tests persistencia | RF-03, RF-06 |
| RF-06 | Comunicación en tiempo real | Obligatorio | WebSocket, eventos, vistas seguras | Sincronización tablero | Tests contrato/reconexión | RF-03, RF-05 |
| RF-07 | Interfaz de usuario | Obligatorio futuro | Contratos REST/WS | Angular tablero + drag/drop | E2E básico | RF-01 a RF-06 |

## Requerimientos no funcionales

| ID | Requerimiento | Tipo | Impacto backend | Impacto frontend | Impacto testing | Dependencias |
|---|---|---|---|---|---|---|
| RNF-01 | Rendimiento: acciones <200ms, catálogo <500ms | Obligatorio | Cache local, queries eficientes | Render fluido | Tests/perfiles puntuales | RF-04, RF-05 |
| RNF-02 | Calidad y buenas prácticas | Obligatorio | Capas limpias, engine aislado | Componentes/servicios claros | Revisión + tests | Todos |
| RNF-03 | Testing: 80% global, >90% críticos | Obligatorio | Diseño testeable | E2E mínimo | JaCoCo/JUnit/Mockito | RNF-02 |
| RNF-04 | Patrones adecuados | Obligatorio | State, Strategy, Chain, Observer, Repository, Facade | Observer vía WS/RxJS futuro | Tests de comportamiento | RF-01, RF-06 |
| RNF-05 | Seguridad | Obligatorio | Validación backend, ocultar información privada | No inferir reglas ni mostrar secretos | Tests vista segura | RF-05, RF-06 |
| RNF-06 | Usabilidad | Obligatorio futuro | Errores claros y accionables | Feedback visual | E2E/UX checks | RF-07 |

## Opcionales detectados

| ID | Ítem | Impacto | Recomendación |
|---|---|---|---|
| OP-01 | Megaevolución | Engine + efectos | Postergar hasta reglas base estables |
| OP-02 | Chat | WebSocket | Separar de eventos de juego |
| OP-03 | Ranking/historial | Persistencia | Postergar hasta cierre de partida estable |
| OP-04 | Animaciones | Frontend | No bloquear backend |
| OP-05 | Expansiones adicionales | Catálogo + efectos | Solo después de auditar XY1 |
| OP-06 | AS TÁCTICO / ACE SPEC | Deck Builder + catálogo | No aplica a `xy1`; implementar máximo 1 por mazo solo si se incorporan sets opcionales que incluyan ACE SPEC |

## Dependencias críticas

1. RF-01 depende de un modelo de dominio correcto.
2. RF-06 depende de RF-05: sin snapshot completo no hay reconexión robusta.
3. RF-04 debe existir antes de partidas reales porque el engine no debe consultar pokemontcg.io durante la partida.
4. RNF-05 atraviesa RF-05/RF-06: las vistas seguras no deben filtrar mano rival, premios ocultos ni orden de mazo.
5. La validación AS TÁCTICO / ACE SPEC no es dependencia del alcance base `xy1`; queda condicionada a expansiones futuras que contengan esa mecánica.
