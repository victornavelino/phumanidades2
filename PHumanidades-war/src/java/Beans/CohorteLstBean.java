/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Beans;

import DAO.InformePagoAlumnoFacade;
import Entidades.Carreras.Carrera;
import Entidades.Carreras.Cohorte;
import Entidades.Carreras.Cuenta;
import Entidades.Ingresos.InformePagoAlumno;
import Entidades.Ingresos.Ingreso;
import Entidades.Persona.Alumno;
import RN.CarrerasRNLocal;
import RN.CohorteRNLocal;
import RN.IngresoRNLocal;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import org.primefaces.component.commandbutton.CommandButton;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.context.RequestContext;

/**
 *
 * @author vouilloz
 */
@ManagedBean
@SessionScoped
public class CohorteLstBean implements Serializable {

    @EJB
    private CohorteRNLocal cohorteRNLocal;//hacemos la referencia para poder utilizar el metodo findall

    @EJB
    private CarrerasRNLocal carreraRNLocal;//hacemos la referencia para poder utilizar el metodo findall

    @EJB
    private IngresoRNLocal ingresoCuotaRNLocal;
    @EJB
    private InformePagoAlumnoFacade informePagoAlumnoFacade;

    @ManagedProperty(value = "#{alumnoLstBean}")
    private AlumnoLstBean alumnoLstBean;

    private List<Cohorte> lstCohorte; //Cargamos la lista de Usuarios retornada po el metodo findAll del usuarioRNLocal
    private List<SelectItem> lstSICohorte;//lo usamos para cargar la lista de cohortes 

    private String anio;

    private String sMensaje;
    private Cohorte cohorte;
    CommandButton btnSelect;

    private Cohorte cohorteSelect;

    private int numeroRecibo;

    //INICIO SELECCION COHORTES ALUMNOS
    private List<Cohorte> lstCohortesAlumnos;
    private Cohorte cohorteSeleccionada;
    //INICIO SELECCION COHORTES ALUMNOS

    private List<Ingreso> lstCuotasAlumno;
    private int ultimaCuota;

    private boolean flag; // CUANDO ES TRUE LLEGO A LA ULTIMA CUOTA;
    private DataTable tablaCohorte;

    private List<Cohorte> lstCohortesAlumnosConsulta;

    private List<Ingreso> lstCuotasAlumnoConsulta;

    private List<Ingreso> lstCuotasAlumnoGeneral;
    private int cantidadCuotas;
    private int iTipoBoton;
    
    private List<InformePagoAlumno> lstInformePagoAlumno;

    /**
     * Creates a new instance of UsuarioLstBean
     */
    public CohorteLstBean() {
    }

    @PostConstruct
    private void init() {
        cohorteSeleccionada = new Cohorte();
        cohorteSeleccionada.setImporteCuota(BigDecimal.ZERO);
        this.cargarCohorte();
        flag = false;

    }

    public int getCantidadCuotas() {
        return cantidadCuotas;
    }

    public void setCantidadCuotas(int cantidadCuotas) {
        this.cantidadCuotas = cantidadCuotas;
    }

    public Cohorte getCohorte() {
        return cohorte;
    }

    public void setCohorte(Cohorte cohorte) {
        this.cohorte = cohorte;
    }

    public int getiTipoBoton() {
        return iTipoBoton;
    }

    public void setiTipoBoton(int iTipoBoton) {
        this.iTipoBoton = iTipoBoton;
    }

    public IngresoRNLocal getIngresoCuotaRNLocal() {
        return ingresoCuotaRNLocal;
    }

    public void setIngresoCuotaRNLocal(IngresoRNLocal ingresoCuotaRNLocal) {
        this.ingresoCuotaRNLocal = ingresoCuotaRNLocal;
    }

    public CommandButton getBtnSelect() {
        return btnSelect;
    }

    public void setBtnSelect(CommandButton btnSelect) {
        this.btnSelect = btnSelect;
    }

    public DataTable getTablaCohorte() {
        return tablaCohorte;
    }

    public void setTablaCohorte(DataTable tablaCohorte) {
        this.tablaCohorte = tablaCohorte;
    }

    public CohorteRNLocal getCohorteRNLocal() {
        return cohorteRNLocal;
    }

    public void setCohorteRNLocal(CohorteRNLocal cohorteRNLocal) {
        this.cohorteRNLocal = cohorteRNLocal;
    }

    public List<Cohorte> getLstCohorte() {
        return lstCohorte;
    }

    public void setLstCohorte(List<Cohorte> lstCohorte) {
        this.lstCohorte = lstCohorte;
    }

    public List<SelectItem> getLstSICohorte() {
        return lstSICohorte;
    }

    public void setLstSICohorte(List<SelectItem> lstSICohorte) {
        this.lstSICohorte = lstSICohorte;
    }

    public String getsMensaje() {
        return sMensaje;
    }

    public void setsMensaje(String sMensaje) {
        this.sMensaje = sMensaje;
    }

    public Cohorte getCohorteSelect() {
        return cohorteSelect;
    }

    public void setCohorteSelect(Cohorte cohorteSelect) {
        this.cohorteSelect = cohorteSelect;
    }

    public String getAnio() {
        return anio;
    }

    public void setAnio(String anio) {
        this.anio = anio;
    }

    public List<Cohorte> getLstCohortesAlumnos() {
        return lstCohortesAlumnos;
    }

    public void setLstCohortesAlumnos(List<Cohorte> lstCohortesAlumnos) {
        this.lstCohortesAlumnos = lstCohortesAlumnos;
    }

    public List<Cohorte> getLstCohortesAlumnosConsulta() {
        return lstCohortesAlumnosConsulta;
    }

    public void setLstCohortesAlumnosConsulta(List<Cohorte> lstCohortesAlumnosConsulta) {
        this.lstCohortesAlumnosConsulta = lstCohortesAlumnosConsulta;
    }

    public Cohorte getCohorteSeleccionada() {
        return cohorteSeleccionada;
    }

    public void setCohorteSeleccionada(Cohorte cohorteSeleccionada) {
        this.cohorteSeleccionada = cohorteSeleccionada;
    }

    public AlumnoLstBean getAlumnoLstBean() {
        return alumnoLstBean;
    }

    public void setAlumnoLstBean(AlumnoLstBean alumnoLstBean) {
        this.alumnoLstBean = alumnoLstBean;
    }

    public List<Ingreso> getLstCuotasAlumno() {
        return lstCuotasAlumno;
    }

    public void setLstCuotasAlumno(List<Ingreso> lstCuotasAlumno) {
        this.lstCuotasAlumno = lstCuotasAlumno;
    }

    public List<Ingreso> getLstCuotasAlumnoConsulta() {
        return lstCuotasAlumnoConsulta;
    }

    public void setLstCuotasAlumnoConsulta(List<Ingreso> lstCuotasAlumnoConsulta) {
        this.lstCuotasAlumnoConsulta = lstCuotasAlumnoConsulta;
    }

    public List<Ingreso> getLstCuotasAlumnoGeneral() {
        return lstCuotasAlumnoGeneral;
    }

    public void setLstCuotasAlumnoGeneral(List<Ingreso> lstCuotasAlumnoGeneral) {
        this.lstCuotasAlumnoGeneral = lstCuotasAlumnoGeneral;
    }

    public int getUltimaCuota() {
        return ultimaCuota;
    }

    public void setUltimaCuota(int ultimaCuota) {
        this.ultimaCuota = ultimaCuota;
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public int getNumeroRecibo() {
        return numeroRecibo;
    }

    public void setNumeroRecibo(int numeroRecibo) {
        this.numeroRecibo = numeroRecibo;
    }

    public InformePagoAlumnoFacade getInformePagoAlumnoFacade() {
        return informePagoAlumnoFacade;
    }

    public void setInformePagoAlumnoFacade(InformePagoAlumnoFacade informePagoAlumnoFacade) {
        this.informePagoAlumnoFacade = informePagoAlumnoFacade;
    }

    public List<InformePagoAlumno> getLstInformePagoAlumno() {
        return lstInformePagoAlumno;
    }

    public void setLstInformePagoAlumno(List<InformePagoAlumno> lstInformePagoAlumno) {
        this.lstInformePagoAlumno = lstInformePagoAlumno;
    }
    
    
    
    public void cargarCohorte() {
        try {
            this.setLstCohorte(this.cohorteRNLocal.findCohortes());
            this.setLstSICohorte(new ArrayList<SelectItem>());

            //Agrupamos partiendo de las cohortes y no de Carrera.getCohortes(), porque esa
            //coleccion es el lado inverso de la relacion y queda desactualizada (cache) cuando
            //se da de alta una cohorte, con lo que la carrera aparecia sin sus cohortes.
            Map<Long, List<SelectItem>> itemsPorCarrera = new LinkedHashMap<>();
            Map<Long, String> descripcionPorCarrera = new LinkedHashMap<>();

            for (Carrera car : this.carreraRNLocal.findCarreras()) {
                if (car.getId() != null) {
                    itemsPorCarrera.put(car.getId(), new ArrayList<SelectItem>());
                    descripcionPorCarrera.put(car.getId(), car.getDescripcion());
                }
            }

            List<SelectItem> sinCarrera = new ArrayList<>();
            for (Cohorte cor : this.getLstCohorte()) {
                SelectItem selectItem = new SelectItem(cor, cor.getDescripcion());
                Carrera car = cor.getCarrera();
                if (car == null || car.getId() == null) {
                    sinCarrera.add(selectItem);
                    continue;
                }
                List<SelectItem> items = itemsPorCarrera.get(car.getId());
                if (items == null) {
                    //carrera que no vino en el listado de carreras
                    items = new ArrayList<>();
                    itemsPorCarrera.put(car.getId(), items);
                    descripcionPorCarrera.put(car.getId(), car.getDescripcion());
                }
                items.add(selectItem);
            }

            for (Map.Entry<Long, List<SelectItem>> entry : itemsPorCarrera.entrySet()) {
                List<SelectItem> items = entry.getValue();
                if (items.isEmpty()) {
                    continue;//no mostramos carreras sin cohortes
                }
                SelectItemGroup group = new SelectItemGroup(descripcionPorCarrera.get(entry.getKey()));
                group.setSelectItems(items.toArray(new SelectItem[items.size()]));
                this.getLstSICohorte().add(group);
            }

            if (!sinCarrera.isEmpty()) {
                SelectItemGroup group = new SelectItemGroup("Sin carrera");
                group.setSelectItems(sinCarrera.toArray(new SelectItem[sinCarrera.size()]));
                this.getLstSICohorte().add(group);
            }
        } catch (Exception ex) {
            FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error: " + ex,
                    null);
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null, fm);
        }
    }//fin cargarCarreras

    public void definirActionBoton(ActionEvent e) {

        btnSelect = (CommandButton) e.getSource();

    }

    public void findCohorte(String nombre) throws Exception {

        if (!nombre.equals("")) {
            this.setLstCohorte(cohorteRNLocal.findCohorteNombre(nombre));
        } else {
            this.setLstCohorte(cohorteRNLocal.findCohortes());
        }
    }

    public void obtenerDatos(Cohorte cort) throws Exception {

        if (cort != null) {
            ultimaCuota = 1;
            cohorteSeleccionada = cort;
            try {
                this.ultimaCuota = this.ingresoCuotaRNLocal.findUltimaCuotaAlumnoCohorte(this.getAlumnoLstBean().getAlumnoSelect(),
                        cohorteSeleccionada);
            } catch (Exception e) {
            }
            if (ultimaCuota == cohorteSeleccionada.getCantidadCuotas()) {
                flag = true;
            } else {
                this.setUltimaCuota(ultimaCuota + 1);
                flag = false;
            }

            Cuenta codigoCuenta = cohorteSeleccionada.getCarrera().getCuenta();
            this.setNumeroRecibo(ingresoCuotaRNLocal.numeroReciboSegunCuenta(codigoCuenta) + 1);
            RequestContext.getCurrentInstance().update("frmPri:dtCortesCobros");
            RequestContext.getCurrentInstance().update("frmPri:btnActionCobroCuota");
            RequestContext.getCurrentInstance().update("frmPri:otCuenta");
            RequestContext.getCurrentInstance().update("frmPri:itNumeroCuenta");
            RequestContext.getCurrentInstance().update("frmPri:itImporte");
            RequestContext.getCurrentInstance().update("frmPri:itCuota");
            RequestContext.getCurrentInstance().update("frmPri:itCuotaInicial");

        }
    }

    public void obtenerDatosPagosCohorte(Alumno a, Cohorte cort) throws Exception {
        FacesMessage fm;
        if (cort != null) {
            System.out.println("alumno cohorte: " + a);
            cohorteSeleccionada = cort;
            try {

                this.setLstCuotasAlumnoConsulta(ingresoCuotaRNLocal.findCuotasAlumnoCohorte(a, cohorteSeleccionada));
                this.setLstCuotasAlumnoGeneral(ingresoCuotaRNLocal.findCuotasAlumnoGeneral(a));
                if (this.getLstCuotasAlumnoConsulta().isEmpty()) {
                    fm = new FacesMessage(FacesMessage.SEVERITY_INFO, "No se encontraron registros", null);
                    FacesContext fc = FacesContext.getCurrentInstance();
                    fc.addMessage(null, fm);
                }//fin if

            } catch (Exception ex) {
                fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error: " + ex.getMessage(), null);
                FacesContext fc = FacesContext.getCurrentInstance();
                fc.addMessage("frmPri:cbBuscarAlumnoCobro", fm);
            }//fin catch

            RequestContext.getCurrentInstance().update("frmPri:dtCobroCuotasConsulta");
            RequestContext.getCurrentInstance().update("frmPri:dtCobroCuotasConsultaGeneral");

        }
    }

    public void obtenerComprobantePagoAlumno(Alumno a, Cohorte cort) throws Exception {
        FacesMessage fm;
        if (cort != null) {
            System.out.println("alumno cohorte: " + a);
            cohorteSeleccionada = cort;
            try {

                this.setLstInformePagoAlumno(informePagoAlumnoFacade.findPagosAlumnoCohorte(a, cohorteSeleccionada));
                //this.setLstCuotasAlumnoGeneral(ingresoCuotaRNLocal.findCuotasAlumnoGeneral(a));
                if (this.getLstInformePagoAlumno().isEmpty()) {
                    fm = new FacesMessage(FacesMessage.SEVERITY_INFO, "No se encontraron registros", null);
                    FacesContext fc = FacesContext.getCurrentInstance();
                    fc.addMessage(null, fm);
                }//fin if

            } catch (Exception ex) {
                fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error: " + ex.getMessage(), null);
                FacesContext fc = FacesContext.getCurrentInstance();
                fc.addMessage("frmPri:cbBuscarAlumnoCobro", fm);
            }//fin catch

            RequestContext.getCurrentInstance().update("frmPri:dtCobroCuotasConsulta");
            //RequestContext.getCurrentInstance().update("frmPri:dtCobroCuotasConsultaGeneral");

        }
    }

}
