/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Beans;

import DAO.InformePagoAlumnoFacade;
import Entidades.Carreras.Cohorte;
import Entidades.Ingresos.EstadoComprobanteAlumno;
import Entidades.Ingresos.InformePagoAlumno;
import Entidades.Persona.Alumno;
import javax.inject.Named;
import javax.faces.bean.ManagedBean;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.RequestScoped;
//MERCADO PAGO
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import java.util.Date;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedProperty;
//import javax.faces.bean.SessionScoped;
import javax.inject.Named;
import javax.persistence.Column;
import org.primefaces.context.RequestContext;

/**
 *
 * @author victo
 */
@ManagedBean(name = "mercadoPagoBean")
@RequestScoped
public class MercadoPagoBean implements Serializable {

    /**
     * Creates a new instance of MercadoPagoBean
     */
    private String preferenceId;
    private Long cohorteId;
    @Column(scale = 2, precision = 10)
    private BigDecimal monto;
    private Cohorte cohorte;
    @EJB
    private InformePagoAlumnoFacade informePagoAlumnoFacade;

    @PostConstruct
    void init() {
        MercadoPagoConfig.setAccessToken("TEST-1576757908614312-022716-3193c51969313e661e2b166e757795a9-200964240");
        this.setPreferenceId(new String());
    }

    public MercadoPagoBean() {
        MercadoPagoConfig.setAccessToken("TEST-1576757908614312-022716-3193c51969313e661e2b166e757795a9-200964240");
    }

    public String getPreferenceId() {
        return preferenceId;
    }

    public void setPreferenceId(String preferenceId) {
        this.preferenceId = preferenceId;
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
        // seteamos datos iniciales para el Pago del alumno previo pago desde MP
        InformePagoAlumno informePagoAlumno = new InformePagoAlumno();
        informePagoAlumno.setFecha(new Date());
        informePagoAlumno.setExternalReference(alumno.getId() + "-" + cohorte.getId());
        this.setPreferenceId(new String());
        System.out.println("Entro CargarPreferencia");
        System.out.print("Cohorte nombre = " + cohorte.getDescripcion());
        System.out.println("CohorteID= " + cohorte.getId());
        System.out.println("Token configurado: " + MercadoPagoConfig.getAccessToken());
        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(
                        "http://localhost:8080/PHumanidades-war/paginas/informePagoAlumno/List.xhtml?faces-redirect=true&update=true")
                .pending(
                        "http://localhost:8080/PHumanidades-war/paginas/informePagoAlumno/List.xhtml?faces-redirect=true&update=true")
                .failure(
                        "http://localhost:8080/PHumanidades-war/paginas/informePagoAlumno/List.xhtml?faces-redirect=true&update=true")
                .build();
        // configuramos ACCESS TOKEN (PRIVATE KEY)
        // MercadoPagoConfig.setAccessToken("TEST-1576757908614312-022716-3193c51969313e661e2b166e757795a9-200964240");

        PreferenceItemRequest itemRequest = null;
        itemRequest = PreferenceItemRequest.builder()
                .id("1234")
                .title(cohorte.getCarrera().getDescripcion())
                .description(cohorte.getDescripcion())
                .pictureUrl("http://picture.com/PS5")
                .categoryId(cohorte.getImporteCuota().toString())
                .quantity(1)
                .unitPrice(cohorte.getImporteCuota())
                .build();
        List<PreferenceItemRequest> items = new ArrayList<>();
        items.add(itemRequest);
        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(items)
                .externalReference(informePagoAlumno.getExternalReference())
                .notificationUrl("https://dev-huma.unca.edu.ar/api/webhooks/mercado-pago")
                .backUrls(backUrls)
                // .autoReturn("approved")
                .build();
        PreferenceClient client = new PreferenceClient();
        try {
            Preference preference = client.create(preferenceRequest);
            this.setPreferenceId(preference.getId());
            this.setCohorteId(cohorte.getId());
            this.setCohorte(cohorte);
            System.out.println("Cargo preferencia metodo cargarPreferencia;: " + this.getPreferenceId());
            System.out.println("lista de preferencias=== " + preferenceRequest.getItems());
            System.out.println("REFERENCIA EXTERNA=== " + informePagoAlumno.getExternalReference());
            // 3. Pre-guardar el registro
            // informePagoAlumno.setEstado("PENDIENTE"); // Agrega este campo a tu entidad
            // informePagoAlumno.setAlumno(alumno);
            // informePagoAlumno.setCohorte(cohorte);
            // informePagoAlumno.setEstadoComprobanteAlumno(EstadoComprobanteAlumno.PROCESANDO);
            // informePagoAlumno.setDescripcion("Pago MercadoPago:
            // "+cohorte.getDescripcion());
            // informePagoAlumno.setCantidadCuotas(1);
            // informePagoAlumnoFacade.create(informePagoAlumno);

        } catch (MPException ex) {
            Logger.getLogger(MercadoPagoBean.class.getName()).log(Level.SEVERE, null, ex);
            this.setPreferenceId("");
        } catch (MPApiException ex) {
            Logger.getLogger(MercadoPagoBean.class.getName()).log(Level.SEVERE, null, ex);
            this.setPreferenceId("");
        }

    }

}
