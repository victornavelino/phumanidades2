/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Controllers;

import Beans.InformePagoAlumnoController;
import Beans.LoginAlumnoBean;
import DAO.AlumnoFacade;
import DAO.AlumnoFacadeLocal;
import DAO.CohorteFacade;
import DAO.CohorteFacadeLocal;
import Recursos.GeneradorComprobanteMP;
import DAO.InformePagoAlumnoFacade;
import Entidades.Carreras.Cohorte;
import Entidades.Ingresos.EstadoComprobanteAlumno;
import Entidades.Ingresos.InformePagoAlumno;
import Entidades.Persona.Alumno;
import RN.AlumnoRN;
import RN.AlumnoRNLocal;
import RN.CohorteRNLocal;
import Recursos.GeneradorComprobanteMP;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import java.io.IOException;
import javax.ws.rs.POST;          // Import clave
import javax.ws.rs.Path;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.Response;
import javax.json.Json;
import javax.json.JsonObject;
import java.io.StringReader;
import javax.ejb.EJB;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.commons.codec.digest.DigestUtils;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ManagedProperty;
import net.sf.jasperreports.engine.export.JRPdfExporterParameter;

/**
 *
 * @author victo
 */
@Path("/webhooks/mercado-pago")
@Produces(MediaType.APPLICATION_JSON)
public class MercadoPagoWebHook {

    @EJB
    private InformePagoAlumnoFacade informePagoAlumnoFacade;
    @EJB
    private AlumnoFacadeLocal alumnoFacadeLocal;
    @EJB
    private CohorteFacadeLocal cohorteFacadeLocal;
    private Alumno alumno;
    private Cohorte cohorte;

    private InformePagoAlumno informePagoAlumno;
    private String paymentId;
    private String status;
    private String externalReference;
    private String eventType;
    private String date_created;
    private GeneradorComprobanteMP generadorComprobanteMP;
    private ByteArrayOutputStream comprobante;

    public Alumno getAlumno() {
        return alumno;
    }

    public void setAlumno(Alumno alumno) {
        this.alumno = alumno;
    }

    public Cohorte getCohorte() {
        return cohorte;
    }

    public void setCohorte(Cohorte cohorte) {
        this.cohorte = cohorte;
    }

    @POST
    public Response handleWebhook(
            @HeaderParam("X-Signature") String signature,
            String payload) {
        System.out.print("ENTRO HandleWebbhook con payload= " + payload);
        // 1. Validar firma (seguridad crítica)
//        if (!validarFirma(signature, payload)) {
//            return Response.status(401).build();
        //}

        // 2. Parsear JSON
        JsonObject json = Json.createReader(new StringReader(payload)).readObject();
        //imprimir todo aqui
        System.out.println("Webhook recibido: " + json.toString());
        eventType = json.getString("type"); // "payment" o "merchant_order"
        paymentId = json.getJsonObject("data").getString("id");
        //status = json.getJsonObject("data").getString("status");
        //externalReference = json.getString("external_reference");

        // 3. Procesar solo eventos de pago aprobados
        if ("payment".equals(eventType)) {
            System.out.print("ENTRO IF Payment con pymentId= " + paymentId);
            procesarPago(paymentId);
        }

        return Response.ok().build();
    }

    private boolean validarFirma(String signature, String payload) {
        String secret = "TU_WEBHOOK_SECRET"; // Configurar en variables de entorno
        String hash = DigestUtils.sha256Hex(payload + secret);
        return hash.equals(signature);
    }

    private void procesarPago(String paymentId) {

        MercadoPagoConfig.setAccessToken("TEST-1576757908614312-022716-3193c51969313e661e2b166e757795a9-200964240");

        System.out.print("ENTRO Metodo procesarPago");
        PaymentClient client = new PaymentClient();
        Payment payment = new Payment();
        System.out.println("Payment ID recibido: " + paymentId);
        System.out.println("Access Token en uso: " + MercadoPagoConfig.getAccessToken());

        try {
            payment = client.get(Long.parseLong(paymentId));
        } catch (MPException ex) {
            System.out.println("Error al Obtener payment JSON MP");
            Logger.getLogger(MercadoPagoWebHook.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MPApiException ex) {
            System.out.println("Error API al Obtener payment JSON MP");
            Logger.getLogger(MercadoPagoWebHook.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println(" FLUJO COMPLETADO!!! Respuesta get mercado pago =:" + payment);

        if ("approved".equals(payment.getStatus())) {
            System.out.print("ENTRO IF Approved Metodo procesarPago con externalReference =" + externalReference);
            informePagoAlumno = new InformePagoAlumno();
            
            // OBTENEMOS ID DE ALUMNO Y COHORTE DESDE EXTERNALREFERENCE PRIMERO
            try {
                String[] ids = payment.getExternalReference().split("\\-");
                this.alumno = alumnoFacadeLocal.find(Long.parseLong(ids[0]));
                this.cohorte = cohorteFacadeLocal.find(Long.parseLong(ids[1]));
            } catch (Exception ex) {
                System.out.println("Error al parsear o buscar Alumno/Cohorte: " + ex.getMessage());
                this.alumno = null;
                this.cohorte = null;
            }

            try {
                comprobante = GeneradorComprobanteMP.generarComprobante(payment, this.alumno, this.cohorte);
            } catch (Exception ex) {
                System.out.println("Error Comprobante!!! catch: " + ex.getMessage());
                comprobante = null;
            }

            //Seteamos valores
            informePagoAlumno.setAlumno(alumno);
            informePagoAlumno.setCohorte(cohorte);

            informePagoAlumno.setEstado("APROBADO"); // Agrega este campo a tu entidad
            informePagoAlumno.setEstadoComprobanteAlumno(EstadoComprobanteAlumno.PROCESANDO);
            informePagoAlumno.setDescripcion("Pago MercadoPago: " + payment.getDescription());
            informePagoAlumno.setCantidadCuotas(1);
            informePagoAlumno.setFecha(new Date());
            informePagoAlumno.setPaymentId(paymentId);
            informePagoAlumno.setNombreComprobantePago("MercadoPago_" + paymentId + ".pdf");
            if (comprobante != null) {
                informePagoAlumno.setComprobantePago(comprobante.toByteArray());
            }
            informePagoAlumno.setExternalReference("MP");

            informePagoAlumnoFacade.create(informePagoAlumno);
            System.out.println("Pago Realizado");

        }
        if ("rejected".equals(payment.getStatus())) {
            informePagoAlumno = informePagoAlumnoFacade.findByExternalRef(payment.getExternalReference());
            if (informePagoAlumno != null) {
                informePagoAlumno.setEstado("RECHAZADO");
                informePagoAlumno.setPaymentId(paymentId);
                informePagoAlumnoFacade.edit(informePagoAlumno);
            }
        }
    }

    private byte[] downloadComprobante(Payment payment) throws IOException {
        // 1. Obtener URL del comprobante (PDF)
        String comprobanteUrl = payment.getTransactionDetails().getExternalResourceUrl();

        if (comprobanteUrl == null) {
            throw new IOException("El pago no tiene comprobante asociado");
        }

        // 2. Configurar conexión HTTP
        URL url = new URL(comprobanteUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // 3. Descargar el PDF
        try (InputStream in = connection.getInputStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            // 4. Retornar como byte[] para guardar en BD
            return out.toByteArray();

        } finally {
            connection.disconnect();
        }
    }

}
