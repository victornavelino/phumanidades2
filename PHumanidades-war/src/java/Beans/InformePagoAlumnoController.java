package Beans;

import Entidades.Ingresos.InformePagoAlumno;
import Beans.util.JsfUtil;
import Beans.util.JsfUtil.PersistAction;
import DAO.InformePagoAlumnoFacade;
import DAO.IngresoFacade;
import DAO.IngresoFacadeLocal;
import Entidades.Carreras.Cohorte;
import Entidades.Ingresos.EstadoComprobanteAlumno;
import Entidades.Persona.Alumno;
import RN.IngresoRNLocal;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Named;
//import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import org.primefaces.context.PrimeFacesContext;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;

//@Named("informePagoAlumnoController")
//@SessionScoped
@ManagedBean(name = "informePagoAlumnoController")
@SessionScoped
public class InformePagoAlumnoController implements Serializable {

    @EJB
    private InformePagoAlumnoFacade ejbFacade;
    private List<InformePagoAlumno> items = null;
    private InformePagoAlumno selected;
    @ManagedProperty(value = "#{loginAlumnoBean}")
    private LoginAlumnoBean loginAlumnoBean;
    @ManagedProperty(value = "#{cohorteLstBean}")
    private CohorteLstBean cohorteLstBean;
    @EJB
    private IngresoFacadeLocal ingresoFacadeLocal;
    //para subir comprobante
    private UploadedFile archivo;
    private String dropZoneText = "Drop zone p:inputTextarea demo.";
    @EJB
    private InformePagoAlumnoFacade InformePagoAlumnoFacade;
    private List<InformePagoAlumno> lstInformePagoAlumno;
    private Cohorte cohorteSeleccionada;
    private List<Cohorte> lstCohortesInformePagoAlumno;

    public LoginAlumnoBean getLoginAlumnoBean() {
        return loginAlumnoBean;
    }

    @PostConstruct
    private void init() {
        this.setItems(new ArrayList<InformePagoAlumno>());

    }

    public List<Cohorte> getLstCohortesInformePagoAlumno() {
        return lstCohortesInformePagoAlumno;
    }

    public void setLstCohortesInformePagoAlumno(List<Cohorte> lstCohortesInformePagoAlumno) {
        this.lstCohortesInformePagoAlumno = lstCohortesInformePagoAlumno;
    }

    public void setLoginAlumnoBean(LoginAlumnoBean loginAlumnoBean) {
        this.loginAlumnoBean = loginAlumnoBean;
    }

    public InformePagoAlumnoFacade getInformePagoAlumnoFacade() {
        return InformePagoAlumnoFacade;
    }

    public void setInformePagoAlumnoFacade(InformePagoAlumnoFacade InformePagoAlumnoFacade) {
        this.InformePagoAlumnoFacade = InformePagoAlumnoFacade;
    }

    public List<InformePagoAlumno> getLstInformePagoAlumno() {
        return lstInformePagoAlumno;
    }

    public void setLstInformePagoAlumno(List<InformePagoAlumno> lstInformePagoAlumno) {
        this.lstInformePagoAlumno = lstInformePagoAlumno;
    }

    public InformePagoAlumno getSelected() {
        return selected;
    }

    public void setSelected(InformePagoAlumno selected) {
        this.selected = selected;
    }

    protected void setEmbeddableKeys() {
    }

    protected void initializeEmbeddableKey() {
    }

    private InformePagoAlumnoFacade getFacade() {
        return ejbFacade;
    }

    public UploadedFile getArchivo() {
        return archivo;
    }

    public void setArchivo(UploadedFile archivo) {
        this.archivo = archivo;
    }

    public CohorteLstBean getCohorteLstBean() {
        return cohorteLstBean;
    }

    public void setCohorteLstBean(CohorteLstBean cohorteLstBean) {
        this.cohorteLstBean = cohorteLstBean;
    }

    public IngresoFacadeLocal getIngresoFacadeLocal() {
        return ingresoFacadeLocal;
    }

    public void setIngresoFacadeLocal(IngresoFacadeLocal ingresoFacadeLocal) {
        this.ingresoFacadeLocal = ingresoFacadeLocal;
    }

    public Cohorte getCohorteSeleccionada() {
        return cohorteSeleccionada;
    }

    public void setCohorteSeleccionada(Cohorte cohorteSeleccionada) {
        this.cohorteSeleccionada = cohorteSeleccionada;
    }

    public InformePagoAlumno prepareCreate() {
        selected = new InformePagoAlumno();
        System.out.println("ENTRO PREPARATE CREATEE");
        System.out.println(this.getCohorteSeleccionada());
        System.out.println(this.getLoginAlumnoBean().getAlumno());
        selected.setNroCuota(InformePagoAlumnoFacade.findUltimaCuota(this.getLoginAlumnoBean().getAlumno(), this.getCohorteSeleccionada()) + 1);
        //selected.setNroCuota(ingresoFacadeLocal.findUltimaCuotaAlumnoCohorte(this.getLoginAlumnoBean().getAlumno(), this.getCohorteSeleccionada()));
        initializeEmbeddableKey();
        return selected;
    }

    public void create() throws Exception {
        System.out.println("ENTRO CREATE INFORMEPAAGOALUMNO");
        //SETEAMOS VALORES PERSONALIZADOS
        try {
            validar();
            System.out.println(this.getLoginAlumnoBean().getAlumno());
            selected.setFecha(new Date());
            selected.setAlumno(this.getLoginAlumnoBean().getAlumno());
            selected.setCohorte(this.getCohorteSeleccionada());

            selected.setEstadoComprobanteAlumno(EstadoComprobanteAlumno.PROCESANDO);
            System.out.println("imprimiendoooooooo estado COMPROBANTE: " + selected.getEstadoComprobanteAlumno().name());

            persist(PersistAction.CREATE, ResourceBundle.getBundle("/BundleInformePagoAlumno").getString("InformePagoAlumnoCreated"));
            try {
                this.obtenerComprobantesAlumno(this.getLoginAlumnoBean().getAlumno(), cohorteSeleccionada);
            } catch (Exception ex) {
                Logger.getLogger(InformePagoAlumnoController.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IllegalArgumentException e) {

            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }

    }

    private void validar() {
        if (selected.getComprobantePago() == null) {
            throw new IllegalArgumentException("Debe seleccionar un Comprobante");
        }
        if (selected.getTipoIngreso() == null) {
            throw new IllegalArgumentException("Debe seleccionar un Concepto");
        }
        if (this.getCohorteSeleccionada() == null) {
            throw new IllegalArgumentException("Debe seleccionar una Cohorte");
        }

    }

    public void update() throws Exception {
        try {
            validar();
            persist(PersistAction.UPDATE, ResourceBundle.getBundle("/BundleInformePagoAlumno").getString("InformePagoAlumnoUpdated"));
        } catch (IllegalArgumentException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }

    }

    public void destroy() {
        persist(PersistAction.DELETE, ResourceBundle.getBundle("/BundleInformePagoAlumno").getString("InformePagoAlumnoDeleted"));
//        if (!JsfUtil.isValidationFailed()) {
//            selected = null; // Remove selection
//            items = null;    // Invalidate list of items to trigger re-query.
//        }
        try {
            this.obtenerComprobantesAlumno(this.getLoginAlumnoBean().getAlumno(), cohorteSeleccionada);
        } catch (Exception ex) {
            Logger.getLogger(InformePagoAlumnoController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void eliminarRegistro() {
        this.destroy();
    }

    public List<InformePagoAlumno> getItems() {
//        if (items == null) {
//            //items = getFacade().findAll();
//            items = lstInformePagoAlumno;
//        }
        return items;
    }

    public void setItems(List<InformePagoAlumno> lstInformePagoAlumnos) {
        this.items = lstInformePagoAlumnos;
    }

    private void persist(PersistAction persistAction, String successMessage) {
        if (selected != null) {
            setEmbeddableKeys();
            try {
                if (persistAction != PersistAction.DELETE) {
                    getFacade().edit(selected);
                } else {
                    getFacade().remove(selected);
                }
                JsfUtil.addSuccessMessage(successMessage);
            } catch (EJBException ex) {
                String msg = "";
                Throwable cause = ex.getCause();
                if (cause != null) {
                    msg = cause.getLocalizedMessage();
                }
                if (msg.length() > 0) {
                    JsfUtil.addErrorMessage(msg);
                } else {
                    JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("/BundleInformePagoAlumno").getString("PersistenceErrorOccured"));
                }
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("/BundleInformePagoAlumno").getString("PersistenceErrorOccured"));
            }
        }
    }

    public InformePagoAlumno getInformePagoAlumno(java.lang.Long id) {
        return getFacade().find(id);
    }

    public List<InformePagoAlumno> getItemsAvailableSelectMany() {
        return getFacade().findAll();
    }

    public List<InformePagoAlumno> getItemsAvailableSelectOne() {
        return getFacade().findAll();
    }

    @FacesConverter(forClass = InformePagoAlumno.class)
    public static class InformePagoAlumnoControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            InformePagoAlumnoController controller = (InformePagoAlumnoController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "informePagoAlumnoController");
            return controller.getInformePagoAlumno(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof InformePagoAlumno) {
                InformePagoAlumno o = (InformePagoAlumno) object;
                return getStringKey(o.getId());
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "object {0} is of type {1}; expected type: {2}", new Object[]{object, object.getClass().getName(), InformePagoAlumno.class.getName()});
                return null;
            }
        }

    }

    public void obtenerComprobantesAlumno(Alumno alumno, Cohorte cohorte) throws Exception {
        FacesMessage fm;
        System.out.println("Entro obtenerComprobantesAlumno");
        
        if (cohorte != null) {

            cohorteSeleccionada = cohorte;
               //cargarPreferencia(cohorteSeleccionada);
            try {
                //System.out.println("entro a setearlistade comprobantes");
                this.setItems(InformePagoAlumnoFacade.findPagosAlumnoCohorte(alumno, cohorteSeleccionada));
                //this.setLstCuotasAlumnoGeneral(ingresoCuotaRNLocal.findCuotasAlumnoGeneral(a));
                if (this.getItems().isEmpty()) {
                    fm = new FacesMessage(FacesMessage.SEVERITY_INFO, "No se encontraron registros", null);
                    FacesContext fc = FacesContext.getCurrentInstance();
                    fc.addMessage(null, fm);
                }//fin if

            } catch (Exception ex) {
                fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error: " + ex.getMessage(), null);
                FacesContext fc = FacesContext.getCurrentInstance();
                fc.addMessage("frmPri:cbBuscarAlumnoCobro", fm);
            }//fin catch
            RequestContext.getCurrentInstance().update("informePagoAlumnoListForm:datalist");
            
        }
    }

    public void upload() {
        if (archivo != null) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, archivo.getFileName() + " se subio correctamente!", null);
            FacesContext.getCurrentInstance().addMessage(null, message);
        }
    }

    public void handleFileUpload(FileUploadEvent event) {
        //SETEAMOS VALORES DEL COMPROBANTE
        System.out.println("ENTROO METODO handleFileUpload");
        archivo = event.getFile();
        System.out.println(archivo.getFileName());
        String fileName = archivo.getFileName();
        byte[] fileContent = archivo.getContents();
        //NOMBRE ARCHIVO
        selected.setNombreComprobantePago(fileName);
        //COMPROBANTE EN BYTE
        selected.setComprobantePago(fileContent);
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, fileName + " se subio correctamente!", null);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public String getDropZoneText() {
        return dropZoneText;
    }

    public void setDropZoneText(String dropZoneText) {
        this.dropZoneText = dropZoneText;
    }

    public StreamedContent descargarArchivo(InformePagoAlumno informePagoAlumno) {
        InputStream stream = new ByteArrayInputStream(informePagoAlumno.getComprobantePago());
        StreamedContent file = new DefaultStreamedContent(stream, "application/octet-stream", informePagoAlumno.getNombreComprobantePago());
        System.out.println("fileeee para descargar: ");
        System.out.println(file);
        return file;
    }

    private StreamedContent file;

    public void FileDownloadView(InformePagoAlumno informePagoAlumno) {
        if (informePagoAlumno != null && informePagoAlumno.getEstadoComprobanteAlumno() != null) {
            System.out.println("ESTADOOOOOOOOOO: " + informePagoAlumno.getEstadoComprobanteAlumno().name());
        }
        InputStream stream = new ByteArrayInputStream(informePagoAlumno.getComprobantePago());
        file = new DefaultStreamedContent(stream, "application/octet-stream", informePagoAlumno.getNombreComprobantePago());

    }

    public StreamedContent getFile() {
        return file;
    }

    public void cargarLista() {
        System.out.println("ENTRO CARGAR LISTAAAAAA");
        RequestContext.getCurrentInstance().update(":informePagoAlumnoListForm:datalist");
        //RequestContext.getCurrentInstance().execute("PF('informePagoAlumnoListForm:datalist').filter();");

    }

    public void limpiarCampos() {
        this.setItems(null);
        RequestContext.getCurrentInstance().update(":informePagoAlumnoListForm:datalist");

    }

    public boolean esAprobado() {
        try {
            return selected.getEstadoComprobanteAlumno().name().equals("APROBADO");
        } catch (Exception e) {
            return false;
        }
    }

//    public void cargarPreferencia(Cohorte cohorte) {
//        //configuramos ACCESS TOKEN (PRIVATE KEY)
//        MercadoPagoConfig.setAccessToken("TEST-1576757908614312-022716-3193c51969313e661e2b166e757795a9-200964240");
//        PreferenceItemRequest itemRequest
//                = PreferenceItemRequest.builder()
//                .id("1234")
//                .title("WDDDDDD")
//                .description("WDDWDWDWDWD")
//                .pictureUrl("http://picture.com/PS5")
//                .categoryId("Curso")
//                .quantity(1)
//                .currencyId("BRL")
//                .unitPrice(new BigDecimal("90000"))
//                .build();
//        List<PreferenceItemRequest> items = new ArrayList<>();
//        items.add(itemRequest);
//        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
//                .items(items).build();
//        PreferenceClient client = new PreferenceClient();
//        try {
//            Preference preference = client.create(preferenceRequest);
//            this.setPreferenceId(preference.getId());
//            System.out.println("Cargo preferencia metodo cargarPreferencia;: " + this.getPreferenceId());
//        } catch (MPException ex) {
//            Logger.getLogger(MercadoPagoBean.class.getName()).log(Level.SEVERE, null, ex);
//            this.setPreferenceId("");
//        } catch (MPApiException ex) {
//            Logger.getLogger(MercadoPagoBean.class.getName()).log(Level.SEVERE, null, ex);
//            this.setPreferenceId("");
//        }
//
//    }
}
