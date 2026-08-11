package Beans;

import DAO.InformePagoAlumnoFacade;
import Entidades.Carreras.Cohorte;
import Entidades.Ingresos.InformePagoAlumno;
import Entidades.Persona.Alumno;
import Entidades.Persona.CorreoElectronico;
import java.io.Serializable;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Genera el link/QR de pago a través de la pasarela Fiserv (Commerce Hub).
 * El QR en sí se pinta en el front-end a partir de la URL de pago devuelta
 * aquí, de la misma forma que MercadoPagoBean expone un preferenceId que
 * luego consume el Brick de Mercado Pago en el XHTML.
 *
 * @author victo
 */
@ManagedBean(name = "pagoQRBean")
@RequestScoped
public class PagoQRBean implements Serializable {

    private static final long serialVersionUID = 1L;

    // Sandbox de Fiserv, cambiar por la URL de producción cuando corresponda.
    private static final String API_URL = "https://sandbox.api.fiserv.com";
    private static final String NOTIFICATION_URL = "https://dev-huma.unca.edu.ar/api/webhooks/fiserv-pago";

    /** Id de la url de pago (paymentUrlId) devuelto por Fiserv, equivalente al preferenceId de MP. */
    private String preferenceId;
    /** Url de pago devuelta por Fiserv; el front-end la convierte en código QR. */
    private String paymentUrl;
    private Long cohorteId;
    private BigDecimal monto;
    private Cohorte cohorte;

    @EJB
    private InformePagoAlumnoFacade informePagoAlumnoFacade;

    public PagoQRBean() {
    }

    public String getPreferenceId() {
        return preferenceId;
    }

    public void setPreferenceId(String preferenceId) {
        this.preferenceId = preferenceId;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public Long getCohorteId() {
        return cohorteId;
    }

    public void setCohorteId(Long cohorteId) {
        this.cohorteId = cohorteId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public Cohorte getCohorte() {
        return cohorte;
    }

    public void setCohorte(Cohorte cohorte) {
        this.cohorte = cohorte;
    }

    public void cargarPreferencia(Cohorte cohorte, Alumno alumno) {
        // Datos iniciales para el pago del alumno
        InformePagoAlumno informePagoAlumno = new InformePagoAlumno();
        informePagoAlumno.setFecha(new Date());
        informePagoAlumno.setExternalReference(alumno.getId() + "-" + cohorte.getId());
        this.setPreferenceId("");
        this.setPaymentUrl("");

        System.out.println("Entro CargarPreferencia Fiserv");
        System.out.println("Cohorte nombre = " + cohorte.getDescripcion());
        System.out.println("CohorteID = " + cohorte.getId());

        // 1. Credenciales de Fiserv (configuradas por variable de entorno).
        String apiKey = System.getenv("FISERV_API_KEY");
        String secretKey = System.getenv("FISERV_SECRET_KEY");
        if (apiKey == null || secretKey == null) {
            System.err.println("Faltan las variables de entorno FISERV_API_KEY / FISERV_SECRET_KEY");
            return;
        }

        // 2. Identificadores únicos de la petición.
        String clientRequestId = UUID.randomUUID().toString();
        String timestamp = String.valueOf(Instant.now().toEpochMilli());

        // 3. Payload de la petición.
        JsonObject transactionAmount = Json.createObjectBuilder()
                .add("total", cohorte.getImporteCuota().toString())
                .add("currency", "ARS")
                .build();

        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder()
                .add("transactionAmount", transactionAmount)
                .add("transactionType", "SALE")
                .add("transactionNotificationURL", NOTIFICATION_URL)
                .add("expiration", String.valueOf(Instant.now().getEpochSecond() + 86400)) // 24hs
                .add("authenticateTransaction", true)
                .add("dynamicMerchantName", cohorte.getCarrera().getDescripcion())
                .add("invoiceNumber", informePagoAlumno.getExternalReference())
                .add("hostedPaymentPageText", "Pago de cuota: " + cohorte.getDescripcion())
                .add("customer", Json.createObjectBuilder()
                        .add("email", obtenerEmailAlumno(alumno)));

        JsonObject payload = payloadBuilder.build();

        // 4. Firma de la petición (Message-Signature).
        String messageSignature;
        try {
            messageSignature = generarFirmaFiserv(secretKey, clientRequestId, timestamp, payload);
        } catch (Exception ex) {
            Logger.getLogger(PagoQRBean.class.getName()).log(Level.SEVERE, "Error firmando la petición a Fiserv", ex);
            this.setPreferenceId("");
            return;
        }

        // 5. Petición POST a Fiserv.
        Client client = ClientBuilder.newClient();
        Response response = null;
        try {
            response = client.target(API_URL)
                    .path("/payment-url")
                    .request(MediaType.APPLICATION_JSON)
                    .header("Content-Type", "application/json")
                    .header("Api-Key", apiKey)
                    .header("Client-Request-Id", clientRequestId)
                    .header("Timestamp", timestamp)
                    .header("Message-Signature", messageSignature)
                    .post(Entity.json(payload.toString()));

            // 6. Procesar respuesta.
            String responseBody = response.readEntity(String.class);
            if (response.getStatus() == 201) { // CREATED
                JsonObject responseJson = Json.createReader(new StringReader(responseBody)).readObject();

                String paymentUrlValue = responseJson.getString("paymentUrl", "");
                String paymentUrlId = responseJson.getString("paymentUrlId", "");

                this.setPaymentUrl(paymentUrlValue);
                this.setPreferenceId(paymentUrlId); // Guardamos el id como "preferencia"
                this.setCohorteId(cohorte.getId());
                this.setCohorte(cohorte);
                this.setMonto(cohorte.getImporteCuota());

                System.out.println("URL de pago Fiserv generada: " + paymentUrlValue);
                System.out.println("Payment URL ID: " + paymentUrlId);

                // Al igual que en MercadoPagoBean, el registro definitivo de InformePagoAlumno
                // se crea/confirma desde el webhook de Fiserv una vez aprobado el pago.
                // informePagoAlumno.setEstadoComprobanteAlumno(EstadoComprobanteAlumno.PROCESANDO);
                // informePagoAlumno.setDescripcion("Pago Fiserv: " + cohorte.getDescripcion());
                // informePagoAlumnoFacade.create(informePagoAlumno);

            } else {
                System.err.println("Error al generar pago Fiserv: " + response.getStatus() + " - " + responseBody);
                this.setPreferenceId("");
                this.setPaymentUrl("");
            }
        } catch (Exception ex) {
            Logger.getLogger(PagoQRBean.class.getName()).log(Level.SEVERE, "Error en pago Fiserv", ex);
            this.setPreferenceId("");
            this.setPaymentUrl("");
        } finally {
            if (response != null) {
                response.close();
            }
            client.close();
        }
    }

    /** Primer correo electrónico cargado del alumno, o un valor por defecto si no tiene ninguno. */
    private String obtenerEmailAlumno(Alumno alumno) {
        if (alumno.getCorreosElectronicos() != null) {
            for (CorreoElectronico correo : alumno.getCorreosElectronicos()) {
                if (correo != null && correo.getDireccion() != null && !correo.getDireccion().isEmpty()) {
                    return correo.getDireccion();
                }
            }
        }
        return "cliente@email.com";
    }

    // Genera la Message-Signature exigida por Fiserv (HMAC-SHA256 en Base64).
    private String generarFirmaFiserv(String secretKey, String clientRequestId,
            String timestamp, JsonObject payload) throws Exception {
        String jsonPayload = payload.toString();
        String stringToSign = clientRequestId + "|" + timestamp + "|" + jsonPayload;

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] signatureBytes = mac.doFinal(stringToSign.getBytes("UTF-8"));

        return Base64.getEncoder().encodeToString(signatureBytes);
    }
}
