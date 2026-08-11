package Controllers;

import DAO.AlumnoFacadeLocal;
import DAO.CohorteFacadeLocal;
import DAO.InformePagoAlumnoFacade;
import Entidades.Carreras.Cohorte;
import Entidades.Ingresos.EstadoComprobanteAlumno;
import Entidades.Ingresos.InformePagoAlumno;
import Entidades.Persona.Alumno;
import Recursos.GeneradorComprobanteFiserv;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Recibe las notificaciones de Fiserv sobre el estado de un pago con QR
 * generado desde {@link Beans.PagoQRBean}.
 *
 * IMPORTANTE: los nombres de campo del payload ("status", "invoiceNumber",
 * "transactionId", etc.) están tomados de la misma convención que usamos
 * para armar la petición en PagoQRBean (Commerce Hub de Fiserv). Hay que
 * confirmarlos/ajustarlos contra el payload real que llegue desde el
 * sandbox de Fiserv apenas se disponga de la documentación/ejemplos
 * definitivos.
 *
 * @author victo
 */
@Path("/webhooks/fiserv-pago")
@Produces(MediaType.APPLICATION_JSON)
public class FiservWebHook {

    @EJB
    private InformePagoAlumnoFacade informePagoAlumnoFacade;
    @EJB
    private AlumnoFacadeLocal alumnoFacadeLocal;
    @EJB
    private CohorteFacadeLocal cohorteFacadeLocal;

    @POST
    public Response handleWebhook(
            @HeaderParam("Message-Signature") String signature,
            String payload) {

        System.out.println("ENTRO FiservWebHook con payload= " + payload);

        // Validación de firma: descomentar una vez confirmado el esquema real
        // de firma de los webhooks de Fiserv (hoy solo está documentado el
        // esquema de firma de las peticiones salientes, en PagoQRBean).
        // if (!validarFirma(signature, payload)) {
        //     return Response.status(401).build();
        // }

        JsonObject json;
        try {
            json = Json.createReader(new StringReader(payload)).readObject();
        } catch (Exception ex) {
            Logger.getLogger(FiservWebHook.class.getName()).log(Level.SEVERE, "Payload de Fiserv inválido", ex);
            return Response.status(400).build();
        }
        System.out.println("Webhook Fiserv recibido: " + json.toString());

        String status = json.getString("status", json.getString("transactionStatus", ""));
        String transactionId = json.getString("transactionId", json.getString("ipgTransactionId", ""));
        String externalReference = json.getString("invoiceNumber", "");
        BigDecimal monto = null;
        if (json.containsKey("transactionAmount") && !json.isNull("transactionAmount")) {
            JsonObject transactionAmount = json.getJsonObject("transactionAmount");
            if (transactionAmount.containsKey("total")) {
                try {
                    monto = new BigDecimal(transactionAmount.getString("total"));
                } catch (NumberFormatException ex) {
                    monto = null;
                }
            }
        }

        procesarPago(status, transactionId, externalReference, monto);

        return Response.ok().build();
    }

    private void procesarPago(String status, String transactionId, String externalReference, BigDecimal monto) {

        if (externalReference == null || externalReference.isEmpty()) {
            System.err.println("Webhook Fiserv sin invoiceNumber/externalReference, no se puede procesar: " + transactionId);
            return;
        }

        if ("APPROVED".equalsIgnoreCase(status) || "APROBADO".equalsIgnoreCase(status)) {
            System.out.println("ENTRO IF Approved procesarPago con externalReference =" + externalReference);

            Alumno alumno = null;
            Cohorte cohorte = null;
            try {
                String[] ids = externalReference.split("\\-");
                alumno = alumnoFacadeLocal.find(Long.parseLong(ids[0]));
                cohorte = cohorteFacadeLocal.find(Long.parseLong(ids[1]));
            } catch (Exception ex) {
                System.out.println("Error al parsear o buscar Alumno/Cohorte: " + ex.getMessage());
            }

            ByteArrayOutputStream comprobante;
            try {
                comprobante = GeneradorComprobanteFiserv.generarComprobante(transactionId, status, monto, "QR", alumno, cohorte);
            } catch (Exception ex) {
                System.out.println("Error generando comprobante Fiserv: " + ex.getMessage());
                comprobante = null;
            }

            InformePagoAlumno informePagoAlumno = new InformePagoAlumno();
            informePagoAlumno.setAlumno(alumno);
            informePagoAlumno.setCohorte(cohorte);
            informePagoAlumno.setEstado("APROBADO");
            informePagoAlumno.setEstadoComprobanteAlumno(EstadoComprobanteAlumno.PROCESANDO);
            informePagoAlumno.setDescripcion("Pago QR (Fiserv): " + (cohorte != null ? cohorte.getDescripcion() : ""));
            informePagoAlumno.setCantidadCuotas(1);
            informePagoAlumno.setFecha(new Date());
            informePagoAlumno.setPaymentId(transactionId);
            informePagoAlumno.setNombreComprobantePago("Fiserv_" + transactionId + ".pdf");
            if (comprobante != null) {
                informePagoAlumno.setComprobantePago(comprobante.toByteArray());
            }
            informePagoAlumno.setExternalReference(externalReference);

            informePagoAlumnoFacade.create(informePagoAlumno);
            System.out.println("Pago QR Fiserv registrado");

        } else if ("DECLINED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status) || "RECHAZADO".equalsIgnoreCase(status)) {
            InformePagoAlumno informePagoAlumno = informePagoAlumnoFacade.findByExternalRef(externalReference);
            if (informePagoAlumno != null) {
                informePagoAlumno.setEstado("RECHAZADO");
                informePagoAlumno.setPaymentId(transactionId);
                informePagoAlumnoFacade.edit(informePagoAlumno);
            }
        } else {
            System.out.println("Estado de pago Fiserv no manejado: " + status);
        }
    }
}
