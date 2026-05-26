package Controllers;

import DAO.AlumnoFacadeLocal;
import DAO.InformePagoAlumnoFacade;
import DAO.IngresoFacadeLocal;
import DAO.TipoIngresoFacadeLocal;
import DAO.CohorteFacadeLocal;
import Entidades.Persona.Alumno;
import Entidades.Carreras.Cohorte;
import Entidades.Ingresos.InformePagoAlumno;
import Entidades.Ingresos.Ingreso;
import Entidades.Ingresos.TipoIngreso;
import Entidades.Ingresos.EstadoComprobanteAlumno;
import RN.AlumnoRNLocal;
import RN.InscripcionAlumnosRNLocal;
import com.google.gson.Gson;
import java.io.Serializable;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/alumno-portal")
@Produces(MediaType.APPLICATION_JSON)
public class AlumnoPortalController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AlumnoPortalController.class.getName());

    @EJB
    private AlumnoRNLocal alumnoRNLocal;

    @EJB
    private AlumnoFacadeLocal alumnoFacadeLocal;

    @EJB
    private InscripcionAlumnosRNLocal inscripcionAlumnosRNLocal;

    @EJB
    private InformePagoAlumnoFacade informePagoAlumnoFacade;

    @EJB
    private IngresoFacadeLocal ingresoFacadeLocal;

    @EJB
    private TipoIngresoFacadeLocal tipoIngresoFacadeLocal;

    @EJB
    private CohorteFacadeLocal cohorteFacadeLocal;

    // Helper to format Date objects as string
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    // Helper to build CORS-enabled JSON responses
    private Response buildResponse(Response.Status status, Object entity) {
        String json = entity instanceof String ? (String) entity : new Gson().toJson(entity);
        return Response.status(status)
                .entity(json)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With")
                .build();
    }

    private Response buildResponse(Response.Status status) {
        return Response.status(status)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With")
                .build();
    }

    @OPTIONS
    @Path("{path: .*}")
    public Response handleOptions() {
        return buildResponse(Response.Status.OK);
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(String body) {
        try {
            JsonObject json = Json.createReader(new StringReader(body)).readObject();
            String dni = json.containsKey("dni") ? json.getString("dni") : null;

            if (dni == null || dni.trim().isEmpty()) {
                return buildResponse(Response.Status.BAD_REQUEST, "{\"error\": \"El DNI es requerido\"}");
            }

            Alumno alumno = alumnoRNLocal.findByAlumnoDni(dni);
            if (alumno == null) {
                return buildResponse(Response.Status.NOT_FOUND, "{\"error\": \"Alumno no encontrado\"}");
            }

            AlumnoDTO dto = new AlumnoDTO();
            dto.id = alumno.getId();
            dto.dni = alumno.getDni();
            dto.nombre = alumno.getNombre();
            dto.apellido = alumno.getApellido();

            return buildResponse(Response.Status.OK, dto);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en login", e);
            return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @GET
    @Path("/{alumnoId}/cohortes")
    public Response getCohortes(@PathParam("alumnoId") Long alumnoId) {
        try {
            Alumno alumno = alumnoFacadeLocal.find(alumnoId);
            if (alumno == null) {
                return buildResponse(Response.Status.NOT_FOUND, "{\"error\": \"Alumno no encontrado\"}");
            }

            List<Cohorte> cohortes = inscripcionAlumnosRNLocal.alumnoFindCohortes(alumno);
            List<CohorteDTO> dtos = new ArrayList<>();

            if (cohortes != null) {
                for (Cohorte c : cohortes) {
                    CohorteDTO dto = new CohorteDTO();
                    dto.id = c.getId();
                    dto.descripcion = c.getDescripcion();
                    dto.cantidadCuotas = c.getCantidadCuotas();
                    dto.importeCuota = c.getImporteCuota() != null ? c.getImporteCuota().doubleValue() : 0.0;
                    dto.carrera = c.getCarrera() != null ? c.getCarrera().getDescripcion() : "";
                    dto.anio = c.getAnio() != null ? c.getAnio().getAnio() : "";
                    dtos.add(dto);
                }
            }

            return buildResponse(Response.Status.OK, dtos);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener cohortes", e);
            return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @GET
    @Path("/{alumnoId}/cohortes/{cohorteId}/comprobantes")
    public Response getComprobantes(@PathParam("alumnoId") Long alumnoId, @PathParam("cohorteId") Long cohorteId) {
        try {
            Alumno alumno = alumnoFacadeLocal.find(alumnoId);
            Cohorte cohorte = cohorteFacadeLocal.find(cohorteId);

            if (alumno == null || cohorte == null) {
                return buildResponse(Response.Status.NOT_FOUND, "{\"error\": \"Alumno o Cohorte no encontrado\"}");
            }

            List<InformePagoAlumno> items = informePagoAlumnoFacade.findPagosAlumnoCohorte(alumno, cohorte);
            List<InformePagoAlumnoDTO> dtos = new ArrayList<>();

            if (items != null) {
                for (InformePagoAlumno item : items) {
                    InformePagoAlumnoDTO dto = new InformePagoAlumnoDTO();
                    dto.id = item.getId();
                    dto.nroCuota = item.getNroCuota();
                    dto.cantidadCuotas = item.getCantidadCuotas();
                    dto.descripcion = item.getDescripcion();
                    dto.fecha = item.getFecha() != null ? DATE_FORMAT.format(item.getFecha()) : "";
                    dto.nombreComprobantePago = item.getNombreComprobantePago();
                    dto.estadoComprobanteAlumno = item.getEstadoComprobanteAlumno() != null ? item.getEstadoComprobanteAlumno().name() : "";
                    dto.tipoIngresoId = item.getTipoIngreso() != null ? item.getTipoIngreso().getId() : null;
                    dto.tipoIngresoDescripcion = item.getTipoIngreso() != null ? item.getTipoIngreso().getDescripcion() : "";
                    dto.mensajeAlumno = item.getMensajeAlumno();
                    dto.respuestaSistema = item.getRespuestaSistema();
                    dto.externalReference = item.getExternalReference();
                    dto.paymentId = item.getPaymentId();
                    dtos.add(dto);
                }
            }

            return buildResponse(Response.Status.OK, dtos);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener comprobantes", e);
            return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @GET
    @Path("/{alumnoId}/cohortes/{cohorteId}/pagos")
    public Response getPagos(@PathParam("alumnoId") Long alumnoId, @PathParam("cohorteId") Long cohorteId) {
        try {
            Alumno alumno = alumnoFacadeLocal.find(alumnoId);
            Cohorte cohorte = cohorteFacadeLocal.find(cohorteId);

            if (alumno == null || cohorte == null) {
                return buildResponse(Response.Status.NOT_FOUND, "{\"error\": \"Alumno o Cohorte no encontrado\"}");
            }

            List<Ingreso> pagos = ingresoFacadeLocal.findCuotasAlumnoCohorte(alumno, cohorte);
            List<IngresoDTO> dtos = new ArrayList<>();

            if (pagos != null) {
                for (Ingreso p : pagos) {
                    IngresoDTO dto = new IngresoDTO();
                    dto.id = p.getId();
                    dto.cuota = p.getCuota();
                    dto.importe = p.getImporte() != null ? p.getImporte().doubleValue() : 0.0;
                    dto.concepto = p.getConcepto();
                    dto.fechaPago = p.getFechaPago() != null ? DATE_FORMAT.format(p.getFechaPago()) : "";
                    dto.formaPago = p.getFormaPago() != null ? p.getFormaPago().name() : "";
                    dto.numeroRecibo = p.getNumeroRecibo();
                    dto.cohorteId = p.getCohorte() != null ? p.getCohorte().getId() : null;
                    dto.cohorteDescripcion = p.getCohorte() != null ? p.getCohorte().getDescripcion() : "";
                    dtos.add(dto);
                }
            }

            return buildResponse(Response.Status.OK, dtos);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener pagos", e);
            return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @POST
    @Path("/comprobantes")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response crearComprobante(String body) {
        try {
            JsonObject json = Json.createReader(new StringReader(body)).readObject();

            Long alumnoId = json.containsKey("alumnoId") ? Long.parseLong(json.get("alumnoId").toString()) : null;
            Long cohorteId = json.containsKey("cohorteId") ? Long.parseLong(json.get("cohorteId").toString()) : null;
            Long tipoIngresoId = json.containsKey("tipoIngresoId") ? Long.parseLong(json.get("tipoIngresoId").toString()) : null;
            Integer nroCuota = json.containsKey("nroCuota") ? Integer.parseInt(json.get("nroCuota").toString()) : null;
            Integer cantidadCuotas = json.containsKey("cantidadCuotas") ? Integer.parseInt(json.get("cantidadCuotas").toString()) : null;
            String descripcion = json.containsKey("descripcion") ? json.getString("descripcion") : null;
            String mensajeAlumno = json.containsKey("mensajeAlumno") ? json.getString("mensajeAlumno") : null;
            String comprobanteBase64 = json.containsKey("comprobanteBase64") ? json.getString("comprobanteBase64") : null;
            String nombreComprobante = json.containsKey("nombreComprobante") ? json.getString("nombreComprobante") : null;

            if (alumnoId == null || cohorteId == null || tipoIngresoId == null) {
                return buildResponse(Response.Status.BAD_REQUEST, "{\"error\": \"alumnoId, cohorteId y tipoIngresoId son requeridos\"}");
            }

            Alumno alumno = alumnoFacadeLocal.find(alumnoId);
            Cohorte cohorte = cohorteFacadeLocal.find(cohorteId);
            TipoIngreso tipoIngreso = tipoIngresoFacadeLocal.find(tipoIngresoId);

            if (alumno == null || cohorte == null || tipoIngreso == null) {
                return buildResponse(Response.Status.NOT_FOUND, "{\"error\": \"Alumno, Cohorte o TipoIngreso no encontrado\"}");
            }

            InformePagoAlumno ipa = new InformePagoAlumno();
            ipa.setAlumno(alumno);
            ipa.setCohorte(cohorte);
            ipa.setTipoIngreso(tipoIngreso);
            ipa.setNroCuota(nroCuota != null ? nroCuota : (informePagoAlumnoFacade.findUltimaCuota(alumno, cohorte) + 1));
            ipa.setCantidadCuotas(cantidadCuotas != null ? cantidadCuotas : 1);
            ipa.setDescripcion(descripcion);
            ipa.setMensajeAlumno(mensajeAlumno);
            ipa.setFecha(new Date());
            ipa.setEstadoComprobanteAlumno(EstadoComprobanteAlumno.PROCESANDO);

            if (comprobanteBase64 != null && !comprobanteBase64.trim().isEmpty()) {
                byte[] decodedBytes = Base64.getDecoder().decode(comprobanteBase64);
                ipa.setComprobantePago(decodedBytes);
                ipa.setNombreComprobantePago(nombreComprobante != null ? nombreComprobante : "comprobante.bin");
            } else {
                return buildResponse(Response.Status.BAD_REQUEST, "{\"error\": \"El archivo de comprobante es requerido\"}");
            }

            informePagoAlumnoFacade.create(ipa);

            return buildResponse(Response.Status.CREATED, "{\"message\": \"Comprobante creado correctamente\", \"id\": " + ipa.getId() + "}");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al crear comprobante", e);
            return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @PUT
    @Path("/comprobantes/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modificarComprobante(@PathParam("id") Long id, String body) {
        try {
            InformePagoAlumno ipa = informePagoAlumnoFacade.find(id);
            if (ipa == null) {
                return buildResponse(Response.Status.NOT_FOUND, "{\"error\": \"Comprobante no encontrado\"}");
            }

            if (ipa.getEstadoComprobanteAlumno() == EstadoComprobanteAlumno.APROBADO) {
                return buildResponse(Response.Status.BAD_REQUEST, "{\"error\": \"No se puede modificar un comprobante ya aprobado\"}");
            }

            JsonObject json = Json.createReader(new StringReader(body)).readObject();

            if (json.containsKey("tipoIngresoId")) {
                Long tipoIngresoId = Long.parseLong(json.get("tipoIngresoId").toString());
                TipoIngreso tipoIngreso = tipoIngresoFacadeLocal.find(tipoIngresoId);
                if (tipoIngreso != null) {
                    ipa.setTipoIngreso(tipoIngreso);
                }
            }

            if (json.containsKey("cantidadCuotas")) {
                ipa.setCantidadCuotas(Integer.parseInt(json.get("cantidadCuotas").toString()));
            }

            if (json.containsKey("descripcion")) {
                ipa.setDescripcion(json.getString("descripcion"));
            }

            if (json.containsKey("mensajeAlumno")) {
                ipa.setMensajeAlumno(json.getString("mensajeAlumno"));
            }

            if (json.containsKey("comprobanteBase64")) {
                String comprobanteBase64 = json.getString("comprobanteBase64");
                String nombreComprobante = json.containsKey("nombreComprobante") ? json.getString("nombreComprobante") : "comprobante.bin";
                if (comprobanteBase64 != null && !comprobanteBase64.trim().isEmpty()) {
                    byte[] decodedBytes = Base64.getDecoder().decode(comprobanteBase64);
                    ipa.setComprobantePago(decodedBytes);
                    ipa.setNombreComprobantePago(nombreComprobante);
                }
            }

            informePagoAlumnoFacade.edit(ipa);

            return buildResponse(Response.Status.OK, "{\"message\": \"Comprobante modificado correctamente\"}");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al modificar comprobante", e);
            return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @DELETE
    @Path("/comprobantes/{id}")
    public Response eliminarComprobante(@PathParam("id") Long id) {
        try {
            InformePagoAlumno ipa = informePagoAlumnoFacade.find(id);
            if (ipa == null) {
                return buildResponse(Response.Status.NOT_FOUND, "{\"error\": \"Comprobante no encontrado\"}");
            }

            if (ipa.getEstadoComprobanteAlumno() == EstadoComprobanteAlumno.APROBADO) {
                return buildResponse(Response.Status.BAD_REQUEST, "{\"error\": \"No se puede eliminar un comprobante ya aprobado\"}");
            }

            informePagoAlumnoFacade.remove(ipa);

            return buildResponse(Response.Status.OK, "{\"message\": \"Comprobante eliminado correctamente\"}");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar comprobante", e);
            return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @GET
    @Path("/comprobantes/{id}/archivo")
    public Response descargarArchivo(@PathParam("id") Long id) {
        try {
            InformePagoAlumno ipa = informePagoAlumnoFacade.find(id);
            if (ipa == null || ipa.getComprobantePago() == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .header("Access-Control-Allow-Origin", "*")
                        .build();
            }
            return Response.ok(ipa.getComprobantePago())
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                    .header("Access-Control-Allow-Headers", "Content-Type")
                    .header("Content-Disposition", "attachment; filename=\"" + ipa.getNombreComprobantePago() + "\"")
                    .header("Content-Type", "application/octet-stream")
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al descargar archivo", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .header("Access-Control-Allow-Origin", "*")
                    .build();
        }
    }

    @GET
    @Path("/conceptos")
    public Response getConceptos() {
        try {
            List<TipoIngreso> conceptos = tipoIngresoFacadeLocal.findNoBorrados();
            List<TipoIngresoDTO> dtos = new ArrayList<>();

            if (conceptos != null) {
                for (TipoIngreso ti : conceptos) {
                    TipoIngresoDTO dto = new TipoIngresoDTO();
                    dto.id = ti.getId();
                    dto.descripcion = ti.toString(); // Custom representation: anio + " " + descripcion
                    dtos.add(dto);
                }
            }

            return buildResponse(Response.Status.OK, dtos);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al obtener conceptos", e);
            return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // DTO Definitions to avoid circular references and lazy loading issues
    public static class AlumnoDTO {
        public Long id;
        public String dni;
        public String nombre;
        public String apellido;
    }

    public static class CohorteDTO {
        public Long id;
        public String descripcion;
        public int cantidadCuotas;
        public double importeCuota;
        public String carrera;
        public String anio;
    }

    public static class InformePagoAlumnoDTO {
        public Long id;
        public Integer nroCuota;
        public Integer cantidadCuotas;
        public String descripcion;
        public String fecha;
        public String nombreComprobantePago;
        public String estadoComprobanteAlumno;
        public Long tipoIngresoId;
        public String tipoIngresoDescripcion;
        public String mensajeAlumno;
        public String respuestaSistema;
        public String externalReference;
        public String paymentId;
    }

    public static class IngresoDTO {
        public Long id;
        public int cuota;
        public double importe;
        public String concepto;
        public String fechaPago;
        public String formaPago;
        public int numeroRecibo;
        public Long cohorteId;
        public String cohorteDescripcion;
    }

    public static class TipoIngresoDTO {
        public Long id;
        public String descripcion;
    }
}
