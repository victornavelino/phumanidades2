# SPEC — Sistema de Gestión Económico Financiero (PHumanidades)

Documento de especificación funcional y técnica para reescritura desde cero.

---

## 1. Propósito del sistema y contexto de negocio

El sistema gestiona las operaciones económico-financieras de la **Facultad de Humanidades — UNCA (Universidad Nacional de Catamarca)**. URL de producción: `https://dev-huma.unca.edu.ar`.

Permite registrar y controlar:

- El cobro de cuotas arancelarias a alumnos inscriptos en carreras/cohortes.
- El pago de honorarios y gastos a docentes y proveedores.
- El seguimiento de ingresos y egresos por carrera, tipo y período.
- El cierre contable de caja.
- Un portal web donde los alumnos pueden ver y acreditar sus pagos.

**Stack tecnológico actual** (referencia para el rewrite):
- Java EE 7, EJB 3, JPA 2.1 (EclipseLink), JSF 2.2 + PrimeFaces 5.2 + BootsFaces 0.7.0
- GlassFish 4/5, PostgreSQL (JNDI: `jdbc/Phumanidades`)
- JasperReports 6.9.0 (PDF y Excel), Apache POI 3.7 (importación Excel)
- MercadoPago SDK v2.x, Jersey JAX-RS (REST webhook)
- Hashing SHA-256 sin salt (debe reemplazarse por bcrypt/Argon2 en el rewrite)

---

## 2. Roles de usuario y permisos

| Rol | Descripción | Accesos |
|---|---|---|
| `superadmin` | Acceso total | Todo: usuarios, grupos, cuentas, modificar egresos, auditoría de cargas |
| `admin` | Operador general | Todo excepto usuarios, grupos, cuentas y modificación de egresos |
| `consulta` | Solo lectura acotada | Ver cobros por cuotas, cobros por alumno y última cuota |
| `alumno` | Portal propio | Solo ver y subir sus propios comprobantes de pago |

### Pantallas por rol

| Funcionalidad | superadmin | admin | consulta | alumno |
|---|---|---|---|---|
| Gestión de usuarios y grupos | Sí | No | No | No |
| Puntos de venta / Cuentas | Sí | No | No | No |
| ABM Alumnos, Docentes, Carreras, Proveedores, Cohortes | Sí | Sí | No | No |
| Inscripción de alumnos | Sí | Sí | No | No |
| Cierre de caja | Sí | Sí | No | No |
| ABM Ingresos (cobros de cuotas e ingresos generales) | Sí | Sí | No | No |
| Importación de archivos (RAPIPAGO, RED LINK, Excel) | Sí | Sí | No | No |
| ABM Egresos (registro de pagos) | Sí | Sí | No | No |
| Modificar egresos existentes | Sí | No | No | No |
| ABM Tipos de Ingreso/Egreso, Tarjetas | Sí | Sí | No | No |
| Consultas completas + todos los reportes | Sí | Sí | No | No |
| Consultas limitadas (cuotas/alumno, última cuota) | No | No | Sí | No |
| Revisión y aprobación de comprobantes de alumnos | Sí | Sí | No | No |
| Portal del alumno (ver y subir propios comprobantes) | No | No | No | Sí |

### Autenticación

**Usuarios administrativos:**
1. Ingresan usuario y contraseña en texto plano.
2. El sistema hashea la contraseña con SHA-256 y compara contra la base de datos.
3. Al autenticar exitosamente establece `session.userLogged = 1` y redirige a `index`.

**Alumnos:**
1. Ingresan solo su DNI más validación reCAPTCHA v2.
2. El sistema busca el alumno por DNI; si existe, inicia sesión.
3. No hay contraseña — **vulnerabilidad crítica a corregir en el rewrite**.

**Filtro de sesión:** un filtro HTTP verifica `session.userLogged == 1` en todas las rutas protegidas. La diferenciación de roles es exclusivamente en la capa de presentación (visibilidad de menú y botones). **En el rewrite debe implementarse autorización server-side por rol.**

---

## 3. Entidades del modelo de datos

> **Nota de diseño crítica:** En el sistema actual `Alumno`, `Docente` y `Persona` son tres entidades independientes con campos idénticos (dni, nombre, apellido, fechaNacimiento). No existe herencia ni tabla compartida. En el rewrite deben unificarse en una jerarquía correcta (tabla `persona` base o herencia JPA).

### 3.1 Personas

#### Alumno
| Campo | Tipo | Restricciones |
|---|---|---|
| id | Long (PK) | |
| dni | String | Único; formato: 6–9 dígitos o con puntos (ej: `12.345.678`) |
| nombre | String | Obligatorio |
| apellido | String | Obligatorio |
| fechaNacimiento | Date | |
| calidad | Enum | `ACTIVO`, `EGRESADO`, `INACTIVO` |
| condicion | Enum | `REGULAR`, `LIBRE` |
| domicilio | → Domicilio | OneToOne |
| telefonos | → List\<Telefono\> | OneToMany |
| correosElectronicos | → List\<CorreoElectronico\> | OneToMany |
| inscripciones | → List\<InscripcionAlumnos\> | OneToMany |

#### Docente
| Campo | Tipo | Restricciones |
|---|---|---|
| id | Long (PK) | |
| dni | String | |
| nombre | String | |
| apellido | String | |
| fechaNacimiento | Date | |
| domicilio | → Domicilio | OneToOne |
| telefonos | → List\<Telefono\> | OneToMany |
| correosElectronicos | → List\<CorreoElectronico\> | OneToMany |
| carreras | → List\<Carrera\> | OneToMany |

#### Proveedor
| Campo | Tipo | Restricciones |
|---|---|---|
| id | Long (PK) | |
| cuit | String | |
| razonSocial | String | |
| domicilio | → Domicilio | OneToOne |
| telefonos | → List\<Telefono\> | OneToMany |

### 3.2 Contacto

#### Telefono
| Campo | Tipo |
|---|---|
| id | Long (PK) |
| numero | String |
| tipoTelefono | Enum: `CELULAR`, `TRABAJO`, `HOGAR` |

#### CorreoElectronico
| Campo | Tipo |
|---|---|
| id | Long (PK) |
| direccion | String |

#### Domicilio
| Campo | Tipo |
|---|---|
| id | Long (PK) |
| calle | String |
| numero | String |
| barrio | String |
| localidad | → Localidad |

### 3.3 Geografía (4 niveles jerárquicos)

```
Pais → Provincia → Departamento → Localidad → Domicilio
```

Cada nivel tiene: `id (Long, PK)`, `descripcion (String)`, y referencia al nivel superior (OneToOne).

### 3.4 Carreras

#### TipoCarrera
| Campo | Tipo |
|---|---|
| id | Long (PK) |
| descripcion | String |

#### Carrera
| Campo | Tipo | Restricciones |
|---|---|---|
| id | Long (PK) | |
| descripcion | String | Obligatorio |
| tipoCarrera | → TipoCarrera | OneToOne |
| cuenta | → Cuenta | Determina el canal contable / punto de venta |
| materias | → List\<Materia\> | OneToMany |
| cohortes | → List\<Cohorte\> | OneToMany |

#### Materia
| Campo | Tipo |
|---|---|
| id | Long (PK) |
| descripcion | String |

#### Anio
| Campo | Tipo |
|---|---|
| id | Long (PK) |
| descripcion | String |

#### Cohorte
Representa una edición/comisión de una carrera en un año lectivo. Define el plan de pagos.

| Campo | Tipo | Restricciones |
|---|---|---|
| id | Long (PK) | |
| descripcion | String | Obligatorio |
| cantidadCuotas | int | Total de cuotas del plan de pago |
| importeCuota | BigDecimal (10,2) | Importe de cada cuota |
| carrera | → Carrera | ManyToOne, obligatorio |
| anio | → Anio | OneToOne |
| inscripciones | → List\<InscripcionAlumnos\> | OneToMany |

#### InscripcionAlumnos
Tabla de unión entre Alumno y Cohorte.

| Campo | Tipo | Restricciones |
|---|---|---|
| id | Long (PK) | |
| fechaInscripcion | Date | |
| activo | Boolean | Si la inscripción está vigente |
| matricula | String | Número de resolución/matrícula |
| alumno | → Alumno | ManyToOne |
| cohorte | → Cohorte | ManyToOne |

**Restricción:** un alumno no puede estar inscripto dos veces en la misma cohorte.

### 3.5 Cuentas (Puntos de Venta)

| Campo | Tipo | Restricciones |
|---|---|---|
| id | Long (PK) | |
| descripcion | String | Obligatorio |
| codigo | String | Código contable numérico |

Valores conocidos en producción:
- `codigo = "025"` → Carreras a Distancia
- `codigo = "005"` → Gastos Generales

Cada carrera tiene una cuenta asignada. Los cobros de cuotas de esa carrera se imputan automáticamente a esa cuenta.

### 3.6 Ingresos (Cobros)

#### TipoIngreso
| Campo | Tipo | Restricciones |
|---|---|---|
| id | Long (PK) | |
| descripcion | String | |
| borrado | Boolean | Soft-delete |

#### TarjetaDeCredito
| Campo | Tipo |
|---|---|
| id | Long (PK) |
| descripcion | String |

#### Ingreso
| Campo | Tipo | Restricciones |
|---|---|---|
| id | Long (PK) | |
| cuota | int | Número de cuota; `0` = ingreso general |
| importe | BigDecimal (10,2) | |
| concepto | String | |
| nombre | String | Nombre libre (para alumnos eventuales sin registro) |
| fechaPago | Date | Obligatorio |
| fechaDeposito | Date | |
| fechaCierre | Date | `null` = pendiente de cierre; con valor = cerrado |
| numeroRecibo | int | Único por (cuenta + año) |
| formaPago | Enum | `EFECTIVO`, `CHEQUE`, `DEPOSITO`, `TARJETA`, `RAPIPAGO`, `DEBITO` |
| borrado | boolean | Soft-delete, reversible |
| anulado | boolean | Anulación lógica, reversible |
| alumno | → Alumno | OneToOne; `null` para ingresos generales |
| cohorte | → Cohorte | OneToOne; `null` para ingresos generales |
| cuenta | → Cuenta | OneToOne, obligatorio |
| tipoIngreso | → TipoIngreso | OneToOne |
| tarjetaDeCredito | → TarjetaDeCredito | OneToOne; solo si formaPago = `TARJETA` |
| creadoPor | String | Auditoría (usuario que creó el registro) |
| fechaCreado | Date | Auditoría |
| modificadoPor | String | Auditoría |
| fechaModificado | Date | Auditoría |

**Discriminación por tipo:**
- `cuota > 0` y `cohorte != null` → pago de cuota de alumno
- `cuota = 0` y `cohorte = null` → ingreso general

### 3.7 Egresos (Pagos)

#### TipoEgreso
| Campo | Tipo | Restricciones |
|---|---|---|
| id | Long (PK) | |
| descripcion | String | |
| borrado | Boolean | Soft-delete |

#### PagosDocente (Egreso)
| Campo | Tipo | Restricciones |
|---|---|---|
| id | Long (PK) | |
| monto | BigDecimal (12,2) | Importe bruto, obligatorio, > 0 |
| iva | BigDecimal (12,2) | |
| retencionIB | BigDecimal (12,2) | Retención Ingresos Brutos |
| impuestoGanancia | BigDecimal (12,2) | |
| suss | BigDecimal (12,2) | |
| montoConDescuentos | BigDecimal (12,2) | Calculado: `monto - iva - retencionIB - impuestoGanancia - suss` |
| importeComprobante | BigDecimal (12,2) | Importe del comprobante fiscal |
| concepto | String (Lob) | |
| numeroComprobante | String | |
| numeroCheque | String | |
| numeroOrdenPago | int | Agrupa hasta 6 comprobantes; ≥ 1, único por año |
| fechaRegistro | Date | |
| fechaComprobante | Date | |
| fechaCierre | Date | `null` = pendiente de cierre |
| formaPago | Enum | `EFECTIVO`, `CHEQUE`, `DEPOSITO`, `TARJETA`, `RAPIPAGO`, `DEBITO` |
| tipoComprobante | Enum | `FACTURAB`, `FACTURAC`, `RECIBOB`, `RECIBOC`, `PASAJES`, `COMPROBANTEINTERNO` |
| rubroPresupuestario | Enum | `BIENES_DE_CONSUMO`, `SERVICIOS_NO_PERSONALES`, `BIENES_DE_CAPITAL`, `TRANSFERENCIAS` |
| borrado | boolean | Soft-delete |
| anulado | boolean | Anulación lógica |
| docente | → Docente | OneToOne; `null` si es pago a proveedor |
| proveedor | → Proveedor | OneToOne; `null` si es pago a docente |
| carrera | → Carrera | OneToOne; obligatorio si hay docente |
| cuenta | → Cuenta | OneToOne, obligatorio |
| tipoEgreso | → TipoEgreso | OneToOne |
| creadoPor | String | Auditoría |
| fechaCreado | Date | Auditoría |
| modificadoPor | String | Auditoría |
| fechaModificado | Date | Auditoría |

**Invariante:** un egreso puede tener docente O proveedor, nunca ambos ni ninguno.

### 3.8 Comprobantes de Pago del Alumno (Portal)

#### InformePagoAlumno
| Campo | Tipo | Restricciones |
|---|---|---|
| id | Long (PK) | |
| nroCuota | Integer | Número de cuota acreditada |
| cantidadCuotas | Integer | Total de cuotas del plan |
| descripcion | String | |
| fecha | Timestamp | |
| nombreComprobantePago | String | Nombre del archivo adjunto |
| comprobantePago | byte[] (Lob) | Archivo PDF/imagen |
| estadoComprobanteAlumno | Enum | `PROCESANDO`, `APROBADO`, `RECHAZADO` |
| mensajeAlumno | String (Lob) | Mensaje visible para el alumno |
| respuestaSistema | String (Lob) | Detalle interno del sistema |
| estado | String | Campo libre adicional |
| paymentId | String | Único; ID del pago en MercadoPago |
| externalReference | String | Referencia MP: `"{alumnoId}-{cohorteId}"` |
| alumno | → Alumno | OneToOne |
| cohorte | → Cohorte | OneToOne |
| tipoIngreso | → TipoIngreso | OneToOne |

### 3.9 Usuarios del Sistema

#### Grupo
| Campo | Tipo | Restricciones |
|---|---|---|
| id | Long (PK) | |
| descripcion | String | Obligatorio; valores: `superadmin`, `admin`, `consulta`, `alumno` |

#### Usuarios
| Campo | Tipo | Restricciones |
|---|---|---|
| id | Long (PK) | |
| apellido | String | |
| nombre | String | |
| usuario | String | Username para login |
| password | String | SHA-256 del texto plano (sin salt) |
| grupo | → Grupo | OneToOne |

---

## 4. Funcionalidades del sistema

### 4.1 Gestión de Alumnos
- Alta, modificación y baja lógica con datos personales completos.
- Gestión de domicilio, teléfonos y correos electrónicos desde la misma pantalla (diálogos).
- Búsqueda por DNI o nombre/apellido.
- **Importación masiva desde Excel** (`.xls`/`.xlsx`):
  - Mapea columnas por nombre de cabecera (tolerante a acentos y variantes: `dni`/`documento`, `email`/`correo`/`mail`, `telefono`/`tel`/`celular`, etc.).
  - Crea el alumno si no existe con `calidad = ACTIVO`, `condicion = REGULAR`.
  - Si el alumno existe y le faltan email/teléfono, los completa desde el Excel.
  - Si ya está inscripto en la cohorte destino, omite la fila.
  - Genera informe final: alumnos creados / inscripciones creadas / filas omitidas / errores.

### 4.2 Gestión de Docentes
- Alta y modificación con datos personales, domicilio, teléfonos, correos y carreras asociadas.

### 4.3 Gestión de Proveedores
- Alta y modificación con CUIT, razón social, domicilio y teléfonos.

### 4.4 Gestión de Carreras
- Alta y modificación con tipo, materias (diálogo) y cuenta contable asignada.
- Vista diferenciada para Carreras a Distancia.

### 4.5 Gestión de Cohortes
- Alta y modificación: descripción, cantidad de cuotas, importe por cuota, carrera y año.

### 4.6 Inscripción de Alumnos
- Inscribir un alumno en una cohorte verificando que no esté ya inscripto.
- Importación masiva desde Excel (crea alumnos e inscripciones en un solo paso).
- Modificación del estado de inscripción (activo/inactivo) y matrícula.

### 4.7 Cobro de Cuotas e Ingresos

#### Cuota individual
- Registrar el pago de una cuota para un alumno en una cohorte.
- La cuenta se asigna automáticamente desde `cohorte.carrera.cuenta`.
- El número de recibo se auto-incrementa por cuenta y año.
- Valida orden cronológico: `fechaPago ≥` última fecha de pago registrada del alumno.

#### Multi-cuota (catch-up de cuotas atrasadas)
- Registrar el cobro de varias cuotas atrasadas en un solo paso.
- Las cuotas intermedias se crean con `importe = 0`; solo la última recibe el importe real.
- Omite la validación de orden cronológico para el batch.

#### Cuotas iniciales en cero (saldo inicial)
- Crea cuotas con `importe = 0` desde la última registrada hasta un número objetivo.
- Útil para registrar cuotas ya cobradas anteriormente sin registrar el dinero.

#### Ingresos generales
- Ingresos no asociados a alumno ni cohorte (`cuota = 0`, `cohorte = null`).
- El operador elige libremente la cuenta destino.
- Puede incluir un nombre libre en lugar de un alumno registrado.

#### Importación desde archivo
- **RAPIPAGO**: importación de cobros desde archivo con formato propietario de RAPIPAGO.
- **RED LINK**: importación desde CSV con estructura `concepto, usuario, importe, fecha(yyyyMMdd), referencia`. El campo `referencia` se usa como DNI para vincular al alumno.

#### Anulación y borrado
- **Borrar** (soft-delete): marca `borrado = true`, reversible. El registro no aparece en reportes pero sigue en la base de datos.
- **Anular**: marca `anulado = true` en **todos** los ingresos con el mismo `numeroRecibo` + `cuenta` + año.
- Ambas acciones son reversibles mientras el registro no esté cerrado.

#### Reportes de ingresos (JasperReports)
- PDF de cobros por cuotas (por cuenta).
- Excel de cobros por cuotas.
- PDF sumarizado por tipo de ingreso (con y sin filtro de cuenta).
- PDF de última cuota pagada por alumno.

### 4.8 Pagos a Docentes y Proveedores (Egresos)

- Registrar un egreso con hasta **6 comprobantes** agrupados en la misma Orden de Pago.
- El sistema auto-incrementa el número de Orden de Pago (`MAX(numeroOrdenPago) + 1` del año).
- Calcula automáticamente `montoConDescuentos = monto - iva - retencionIB - impuestoGanancia - suss`.
- **Anulación**: marca `anulado = true` en todos los comprobantes con el mismo `numeroOrdenPago` del año.
- **Eliminación física** (`removeTotal`): borrado permanente, no soft-delete.
- Consulta avanzada con filtros: fecha desde/hasta, cuenta, tipo de egreso, carrera.

### 4.9 Cierre de Caja

- Calcula el saldo pendiente: `Σ ingresos no cerrados - Σ egresos no cerrados`.
- El operador elige una fecha de cierre.
- El cierre asigna `fechaCierre` a todos los registros pendientes con fecha ≤ fecha de cierre.
- Los registros cerrados quedan bloqueados: no pueden editarse, borrarse ni anularse.

### 4.10 Consultas y Reportes

| Consulta | Roles con acceso |
|---|---|
| Egresos (filtros: fecha, cuenta, tipo, carrera) | superadmin, admin |
| Ingresos por cuotas de alumnos (filtros: fecha, carrera, cohorte) | superadmin, admin, consulta |
| Ingresos generales (filtros: fecha, tipo de ingreso) | superadmin, admin |
| Historial de cobros por alumno (por DNI) | superadmin, admin, consulta |
| Alumnos por cohorte | superadmin, admin |
| Última cuota pagada por alumno (global o por cohorte) | superadmin, admin, consulta |
| Reporte de ingresos por mes | superadmin, admin |
| Ingresos y egresos por carrera (gráficos de barras) | superadmin, admin |
| Comprobantes de pago del portal del alumno | superadmin, admin |
| Auditoría de cargas por usuario (quién cargó cuántos registros en un período) | superadmin, admin |
| Egresos por rubro presupuestario | superadmin, admin |

### 4.11 Dashboard (Inicio)

- Dos gráficos de barras horizontales del año en curso:
  - Ingresos por carrera.
  - Egresos por carrera.
- Consulta de actividad de carga: cuántos registros creó cada operador en un rango de fechas.

### 4.12 Portal del Alumno

- El alumno inicia sesión con DNI + reCAPTCHA.
- Ve sus cohortes inscriptas y el historial de comprobantes por cohorte.
- Puede informar un nuevo pago subiendo un archivo (PDF, JPG, PNG) como comprobante.
- Los comprobantes informados quedan en estado `PROCESANDO` hasta que un administrador los aprueba o rechaza.
- El sistema envía un email al alumno notificando el resultado (APROBADO/RECHAZADO).
- El alumno puede descargar su comprobante (PDF o imagen).
- Opcionalmente: pago online directo vía MercadoPago Checkout Pro (ver sección de integraciones).

### 4.13 Administración del Sistema

- **Usuarios**: ABM con asignación de grupo/rol y gestión de contraseña.
- **Grupos**: ABM de grupos/roles.
- **Cuentas (Puntos de Venta)**: ABM de cuentas contables con código.
- **Tipos de Ingreso / Tipo de Egreso**: ABM con soft-delete.
- **Tarjetas de Crédito**: ABM.
- **Geografía**: ABM de países, provincias, departamentos y localidades.
- **Manual de usuario**: PDF estático descargable.

---

## 5. Reglas de negocio

### Validaciones de Alumno
- Nombre y apellido obligatorios.
- DNI obligatorio y con formato válido: 6–9 dígitos puros, `XX.XXX.XXX` o `X.XXX.XXX`.
- DNI único: no pueden existir dos alumnos con el mismo DNI.

### Validaciones de Cohorte
- La carrera es obligatoria.

### Validaciones de Ingreso (cuota de alumno)
1. `fechaPago` obligatoria.
2. `fechaPago ≥` última fecha de pago del alumno (orden cronológico). Excepción: en modo multi-cuota batch esta validación se omite.
3. `cuenta` y `cuenta.codigo` obligatorios.
4. Al **crear** (`id = null`): `numeroRecibo` no debe existir para la misma `cuenta` + año.
5. Al **editar** (`id != null`): se permite si el recibo existente pertenece al mismo ingreso.

### Validaciones de Egreso
1. No puede haber docente y proveedor seleccionados simultáneamente.
2. Debe haber exactamente uno (docente o proveedor).
3. Si hay docente, la carrera es obligatoria.
4. `monto > 0`.
5. `cuenta` obligatoria.
6. `numeroOrdenPago ≥ 1`.
7. No puede existir ya un comprobante para ese docente/proveedor con el mismo `numeroComprobante`.
8. No puede existir otro registro con ese `numeroOrdenPago` en el mismo año. Excepción: tipos `PASAJES` y `COMPROBANTEINTERNO` omiten esta validación.

### Regla del Número de Recibo
- Único por combinación `(cuenta, año)`.
- Se auto-incrementa: `MAX(numeroRecibo) + 1` para esa cuenta y año.
- La anulación de un recibo afecta a **todos** los ingresos con ese número en la misma cuenta y año.
- Un recibo puede agrupar múltiples cuotas (varios `Ingreso` con el mismo `numeroRecibo`), lo cual es válido.

### Regla de Orden de Pago
- Un `numeroOrdenPago` puede tener como máximo **6 comprobantes** (`PagosDocente`).
- La anulación afecta a **todos** los comprobantes de la misma orden en el mismo año.
- Los comprobantes 2 a 6 se crean, actualizan o eliminan en cascada al modificar la orden.

### Cálculo de Monto con Descuentos
```
montoConDescuentos = monto - iva - retencionIB - impuestoGanancia - suss
```

### Regla de Cierre de Caja
- Un registro con `fechaCierre != null` está cerrado contablemente y es inmutable.
- Solo participan del saldo: registros con `fechaCierre IS NULL AND borrado = false AND anulado = false`.

### Regla del Soft-Delete
- `borrado = true`: eliminado lógicamente, recuperable, no aparece en ningún reporte ni consulta.
- `anulado = true`: anulado lógicamente, recuperable, no aparece en reportes financieros.
- Un registro puede tener ambos flags activos simultáneamente.

### Regla del Estado del Comprobante del Alumno
- Un comprobante en estado `APROBADO` no puede ser editado ni eliminado.
- Solo se puede editar/eliminar si está en estado `PROCESANDO` o `RECHAZADO`.

---

## 6. Flujos principales

### 6.1 Cobro de cuota a alumno

```
1. Operador abre la pantalla de cobros y clickea "Nuevo Cobro Alumno"
2. Busca el alumno por DNI o nombre/apellido en el diálogo de búsqueda
3. Sistema carga automáticamente las cohortes en que el alumno está inscripto
4. Operador selecciona la cohorte; el sistema carga la última cuota pagada en esa cohorte
5. El formulario propone cuota = ultimaCuota + 1
6. Operador completa: importe, fechaPago, numeroRecibo (o acepta el auto-generado), formaPago
7. Si formaPago = TARJETA: aparece selector de tarjeta de crédito
8. Al guardar, el sistema valida:
   a. fechaPago >= última fechaPago del alumno
   b. numeroRecibo no existente para cuenta+año
   c. cuenta se asigna automáticamente desde cohorte.carrera.cuenta
9. Sistema persiste el Ingreso con cuota = ultimaCuota + 1
10. Opcionalmente se genera PDF de recibo via JasperReports
```

### 6.2 Cobro multi-cuota (deuda acumulada)

```
1. Igual al 6.1 hasta el paso 6
2. Operador indica cantidad de cuotas a saldar
3. Sistema crea cuotas intermedias (ultimaCuota+1 hasta penúltima) con importe = 0
4. Sistema crea la última cuota con el importe real ingresado
5. La validación de orden cronológico se omite para el batch completo
```

### 6.3 Ingreso general

```
1. Operador clickea "Nuevo Cobro General"
2. Ingresa: importe, fechaPago, concepto, tipoIngreso, cuenta, formaPago
3. Puede ingresar un nombre libre (alumno eventual, sin alumno registrado en el sistema)
4. Sistema persiste con cuota = 0, cohorte = null, alumno = null (o null si nombre libre)
```

### 6.4 Registro de egreso (pago a docente/proveedor)

```
1. Operador abre la pantalla de pagos y clickea "Nuevo"
2. Selecciona docente O proveedor (exclusivos entre sí)
3. Si docente: selecciona también la carrera
4. Ingresa: cuenta, numeroOrdenPago (o usa "Cargar Último Número +1"), tipoEgreso,
   rubroPresupuestario, monto, descuentos (iva/IB/ganancias/SUSS),
   tipoComprobante, numeroComprobante, fechaComprobante, numeroCheque si aplica
5. Sistema calcula montoConDescuentos automáticamente
6. Operador puede agregar hasta 5 comprobantes adicionales en la misma orden de pago
7. Sistema valida:
   a. Unicidad de numeroComprobante por docente/proveedor
   b. Unicidad de numeroOrdenPago en el año (salvo PASAJES/COMPROBANTEINTERNO)
   c. Al menos docente o proveedor; no ambos; si docente entonces carrera
8. Sistema persiste hasta 6 registros PagosDocente con el mismo numeroOrdenPago
```

### 6.5 Cierre de caja

```
1. Operador abre el panel de cierre en el dashboard
2. Selecciona la fecha de cierre
3. Clickea "Calcular Saldo": sistema suma todos los ingresos y egresos no cerrados
   saldo = Σ ingresos(fechaCierre=null) - Σ egresos(fechaCierre=null)
4. Operador confirma el cierre
5. Sistema asigna fechaCierre a todos los registros pendientes con fecha <= fechaCierre
6. Los registros cerrados quedan inmutables
```

### 6.6 Inscripción de alumno en cohorte

```
1. Operador busca alumno por DNI en el formulario de inscripciones
2. Selecciona la cohorte destino
3. Sistema verifica que el alumno no esté ya inscripto en esa cohorte
4. Sistema crea InscripcionAlumnos con activo=true, fechaInscripcion=hoy
```

### 6.7 Importación masiva de alumnos desde Excel

```
1. Operador selecciona la cohorte destino
2. Sube archivo .xlsx o .xls
3. Sistema lee filas mapeando columnas por nombre de cabecera
   (normaliza mayúsculas, acentos y acepta variantes de nombres de columna)
4. Por cada fila:
   a. Busca alumno por DNI
   b. Si no existe: crea Alumno con calidad=ACTIVO, condicion=REGULAR
   c. Si existe y le faltan email/teléfono: los actualiza con los datos del Excel
   d. Si ya está inscripto en la cohorte: omite la fila (no error)
   e. Si no está inscripto: crea InscripcionAlumnos con activo=true
5. Al finalizar muestra resumen: alumnos creados / inscripciones creadas / omitidos / errores
```

### 6.8 Pago online del alumno vía MercadoPago

```
1. Alumno inicia sesión con DNI + reCAPTCHA en el portal
2. Ve sus cohortes inscriptas y el número de cuota a pagar
3. Clickea "Pagar Cuota"
4. Sistema crea una preferencia de pago en MercadoPago:
   - Item: descripción=cohorte.descripcion, precio=cohorte.importeCuota
   - externalReference: "{alumno.id}-{cohorte.id}"
   - Webhook URL: /api/webhooks/mercado-pago
5. Alumno es redirigido al Checkout Pro de MercadoPago
6. Tras el pago, MercadoPago invoca el webhook:
   a. Pago aprobado → sistema crea InformePagoAlumno con estado=PROCESANDO,
      genera PDF de recibo, lo guarda en el campo comprobantePago (Lob)
   b. Pago rechazado → actualiza InformePagoAlumno existente a estado=RECHAZADO
7. Administrador revisa el comprobante y cambia estado a APROBADO o RECHAZADO
8. Sistema envía email al alumno notificando el resultado
9. Alumno puede descargar el comprobante desde el portal
```

### 6.9 Anulación de ingreso

```
1. Operador selecciona un ingreso en la tabla y clickea "Anular"
2. Sistema busca TODOS los ingresos con el mismo numeroRecibo + cuenta + año
3. Marca todos como anulado=true
4. La acción es reversible (botón "Recuperar Anulado") mientras no esté cerrado
```

### 6.10 Importación de cobros desde archivo (RED LINK / RAPIPAGO)

```
1. Operador sube el archivo CSV con el formato del procesador
2. Sistema parsea cada fila (formato RED LINK: concepto, usuario, importe, fecha, referencia)
3. El campo "referencia" se usa como DNI para vincular al alumno
4. Crea un Ingreso por cada fila con la cuenta asignada por defecto
5. Muestra resultado de la importación
```

---

## 7. Integraciones externas

### MercadoPago (Checkout Pro)
- **SDK**: mercadopago-sdk-java
- **Modo actual**: TEST (access token hardcodeado — debe externalizarse en el rewrite)
- **Webhook**: `POST /api/webhooks/mercado-pago`
  - Recibe JSON: `{ "type": "payment", "data": { "id": "12345" } }`
  - Solo procesa eventos de tipo `"payment"`
  - Parsea `externalReference` (`"{alumnoId}-{cohorteId}"`) para identificar alumno y cohorte
  - En pago aprobado: crea `InformePagoAlumno` con estado `PROCESANDO` y PDF de comprobante
  - En pago rechazado: actualiza estado a `RECHAZADO`
- **Seguridad del webhook**: la verificación de firma HMAC está actualmente desactivada. **Debe implementarse en el rewrite** (verificar header `x-signature` con la clave secreta de MP).
- **URLs de retorno**: actualmente apuntan a `localhost` — deben configurarse correctamente.

### Google reCAPTCHA v2
- Claves configuradas (pública y privada).
- Se muestra en el formulario de login del alumno.
- **La verificación server-side no está implementada actualmente** — debe implementarse en el rewrite (llamar a `https://www.google.com/recaptcha/api/siteverify`).

### SMTP (Notificaciones por email)
- Envía emails al alumno cuando un administrador aprueba o rechaza su comprobante.
- Credenciales actualmente hardcodeadas en el código — **deben externalizarse** en el rewrite.

### Importación RED LINK
- Lectura de archivos CSV con formato propietario de RED LINK.
- Estructura: `concepto, usuario, importe, fecha(yyyyMMdd), referencia`.
- El campo `referencia` se interpreta como DNI del alumno.

### Importación RAPIPAGO
- Similar a RED LINK con formato diferente.
- Cada fila genera un `Ingreso` general (`cuota = 0`).

### JasperReports (reportes PDF y Excel)
- Archivos `.jasper` precompilados.
- Se parametrizan en runtime con filtros de fecha, cuenta, imágenes institucionales (escudos/logo).
- Formatos de salida: PDF y Excel (`.xls`).

---

## 8. Validaciones y restricciones transversales

| Entidad / Regla | Detalle |
|---|---|
| Alumno — DNI | Único en el sistema; formato: 6–9 dígitos o con puntos |
| Alumno — nombre/apellido | Obligatorios |
| Cohorte — carrera | Obligatoria |
| Ingreso — numeroRecibo | Único por (cuenta, año); la anulación es en cascada para todos los ingresos con el mismo recibo |
| Ingreso — fechaPago | Orden cronológico obligatorio en modo single-cuota |
| Ingreso — cuenta | Obligatoria con código válido |
| PagosDocente — docente/proveedor | Exactamente uno de los dos; si es docente, carrera obligatoria |
| PagosDocente — numeroOrdenPago | Único por año; máximo 6 comprobantes por orden |
| PagosDocente — monto | > 0 |
| InformePagoAlumno — paymentId | Único (constraint de BD) |
| InformePagoAlumno — estado APROBADO | Inmutable: no se puede editar ni eliminar |
| Cierre de caja | Cualquier registro con fechaCierre != null es inmutable |
| InscripcionAlumnos | Un alumno no puede inscribirse dos veces en la misma cohorte |
| Usuarios — password | SHA-256 del texto plano (en rewrite: reemplazar por bcrypt/Argon2 con salt) |

---

## 9. Glosario del dominio

| Término | Significado |
|---|---|
| Cohorte | Edición/comisión de una carrera en un año lectivo. Define el plan de pagos (cantidad y valor de cuotas). |
| Cuenta / Punto de Venta | Canal contable identificado por un código numérico (ej: "025" = Carreras a Distancia). |
| Orden de Pago | Agrupación de hasta 6 comprobantes correspondientes a un mismo egreso. |
| Comprobante | Un registro individual de egreso (factura B/C, recibo, pasajes, etc.). |
| Cierre de Caja | Proceso contable que congela los registros de un período asignando una fecha de cierre. |
| Ingreso General | Cobro no asociado a cuota de alumno (cuota=0, cohorte=null). |
| Calidad del Alumno | Estado académico: `ACTIVO`, `EGRESADO`, `INACTIVO`. |
| Condición del Alumno | Situación de regularidad: `REGULAR`, `LIBRE`. |
| Rubro Presupuestario | Clasificación del gasto: `BIENES_DE_CONSUMO`, `SERVICIOS_NO_PERSONALES`, `BIENES_DE_CAPITAL`, `TRANSFERENCIAS`. |
| Informe de Pago | Comprobante subido/generado por el alumno a través del portal o MercadoPago. |
| Cargas | Auditoría de cuántos registros creó cada operador en un período. |
| Cuota Inicial / Saldo Inicial | Cuotas creadas con importe=0 para registrar deuda histórica sin registrar cobros. |

---

## 10. Notas para el rewrite

1. **Unificar modelo de personas.** Crear una entidad base `Persona` de la que hereden `Alumno` y `Docente`. `Proveedor` puede mantenerse separado al tener naturaleza jurídica distinta.

2. **Módulo único.** Eliminar la duplicación EJB/WAR. El rewrite debe tener una sola capa de dominio y persistencia.

3. **Autenticación del alumno.** Implementar contraseña real, link mágico por email, o código por SMS en lugar de solo DNI.

4. **reCAPTCHA server-side.** Verificar el token contra la API de Google antes de procesar el login del alumno.

5. **Webhook MercadoPago.** Implementar verificación de firma HMAC-SHA256 usando el header `x-signature`.

6. **Configuración externalizada.** Access token de MP, credenciales SMTP, URLs de retorno y claves de reCAPTCHA deben ir en variables de entorno o vault — nunca en el código.

7. **Autorización server-side.** Implementar control de acceso por rol en el backend, no solo ocultando elementos en la UI.

8. **Password segura.** Reemplazar SHA-256 sin salt por bcrypt o Argon2 con salt aleatorio.

9. **Límite de importación CSV.** El límite de tamaño del archivo debe configurarse a un valor real (10 MB mínimo) para soportar archivos de RED LINK de producción.

10. **Entidades huérfanas.** `Contador005`, `Contador025` y `GastoGeneral` están referenciadas en el `persistence.xml` original pero no tienen código fuente. Investigar si eran funcionalidades activas antes de decidir si incluirlas.

11. **Email de notificación.** La función de envío de email al alumno está rota en el sistema actual (credenciales hardcodeadas, ruta de archivo local de Windows). Debe reimplementarse correctamente con configuración externalizada.

12. **PASAJES y COMPROBANTEINTERNO.** Son tipos de comprobante que omiten ciertas validaciones de egreso. Documentar con precisión qué validaciones se omiten para cada uno y por qué.
