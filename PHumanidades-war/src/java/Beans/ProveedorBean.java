/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Beans;

import Entidades.Persona.Domicilio;
import Entidades.Persona.Proveedor;
import Entidades.Persona.Telefono;
import RN.ProveedorRNLocal;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.primefaces.component.autocomplete.AutoComplete;
import org.primefaces.component.commandbutton.CommandButton;
import org.primefaces.context.RequestContext;

/**
 *
 * @author AFerSor
 */
@ManagedBean
@RequestScoped
public class ProveedorBean implements Serializable {

    @EJB
    private ProveedorRNLocal proveedorRNLocal;

    @ManagedProperty("#{proveedorLstBean}")
    private ProveedorLstBean proveedorLstBean;

    @ManagedProperty("#{domicilioBean}")
    private DomicilioBean domicilioBean;

    @ManagedProperty("#{listadoTelefonosBean}")
    private ListadoTelefonosBean listadoTelefonosBean;

    @ManagedProperty(value = "#{pagosDocenteLstBean}")
    private PagosDocenteLstBean pagosDocenteLstBean;

    @ManagedProperty(value = "#{docenteLstBean}")
    private DocenteLstBean docenteLstBean;

    private Proveedor proveedor;

    private Boolean bCamposSoloLectura;
    private int iActionBtnSelect;
    private CommandButton cbAction;

    private AutoComplete autoComplete;

    // Busqueda Proveedor
    private String tipoBusqueda;
    private List<String> lstTipoBusqueda;
    private String busqueda;

    private CommandButton btnSelect;

    public ProveedorBean() {
        proveedor = new Proveedor();
        this.cargarLstTipoBusqueda();
    }

    public AutoComplete getAutoComplete() {
        return autoComplete;
    }

    public String labelAutoCompletar() {
        return ((Proveedor) this.getAutoComplete().getItemValue()).getRazonSocial();

    }// fin labelAutoCompletar

    /*
     * public UsuarioLstBean getUsuarioLstBean() {
     * return usuarioLstBean;
     * }
     */
    /*
     * public void setUsuarioLstBean(UsuarioLstBean usuarioLstBean) {
     * this.usuarioLstBean = usuarioLstBean;
     * }
     */
    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public Boolean getbCamposSoloLectura() {
        return bCamposSoloLectura;
    }

    public void setbCamposSoloLectura(Boolean bCamposSoloLectura) {
        this.bCamposSoloLectura = bCamposSoloLectura;
    }

    public int getiActionBtnSelect() {
        return iActionBtnSelect;
    }

    public void setiActionBtnSelect(int iActionBtnSelect) {
        this.iActionBtnSelect = iActionBtnSelect;
    }

    public CommandButton getCbAction() {
        return cbAction;
    }

    public void setCbAction(CommandButton cbAction) {
        this.cbAction = cbAction;
    }

    public ProveedorLstBean getProveedorLstBean() {
        return proveedorLstBean;
    }

    public void setProveedorLstBean(ProveedorLstBean proveedorLstBean) {
        this.proveedorLstBean = proveedorLstBean;
    }

    public DomicilioBean getDomicilioBean() {
        return domicilioBean;
    }

    public void setDomicilioBean(DomicilioBean domicilioBean) {
        this.domicilioBean = domicilioBean;
    }

    public ListadoTelefonosBean getListadoTelefonosBean() {
        return listadoTelefonosBean;
    }

    public void setListadoTelefonosBean(ListadoTelefonosBean listadoTelefonosBean) {
        this.listadoTelefonosBean = listadoTelefonosBean;
    }

    public String getTipoBusqueda() {
        return tipoBusqueda;
    }

    public void setTipoBusqueda(String tipoBusqueda) {
        this.tipoBusqueda = tipoBusqueda;
    }

    public List<String> getLstTipoBusqueda() {
        return lstTipoBusqueda;
    }

    public void setLstTipoBusqueda(List<String> lstTipoBusqueda) {
        this.lstTipoBusqueda = lstTipoBusqueda;
    }

    public String getBusqueda() {
        return busqueda;
    }

    public void setBusqueda(String busqueda) {
        this.busqueda = busqueda;
    }

    public PagosDocenteLstBean getPagosDocenteLstBean() {
        return pagosDocenteLstBean;
    }

    public void setPagosDocenteLstBean(PagosDocenteLstBean pagosDocenteLstBean) {
        this.pagosDocenteLstBean = pagosDocenteLstBean;
    }

    public DocenteLstBean getDocenteLstBean() {
        return docenteLstBean;
    }

    public void setDocenteLstBean(DocenteLstBean docenteLstBean) {
        this.docenteLstBean = docenteLstBean;
    }

    public void actionBtn() {

        switch (this.getiActionBtnSelect()) {
            case 0:
                create();
                break;
            case 1:
                this.edit();
                break;
            case 2:
                // borra el campo
                this.delete();
                // this.delete(Boolean.TRUE);
                break;
        }// fin switch

    }// fin actionBtn

    public void setBtnSelect(ActionEvent e) {
        CommandButton btnSelect = (CommandButton) e.getSource();
        // activo el boton
        this.getCbAction().setDisabled(false);
        switch (btnSelect.getId()) {
            case "cbCreate":
                this.setiActionBtnSelect(0);
                this.setProveedor(new Proveedor());
                this.getProveedor().setDomicilio(new Domicilio());
                this.domicilioBean.setDomicilio(new Domicilio());
                this.listadoTelefonosBean.setLstTelefonos(new ArrayList<Telefono>());

                this.getCbAction().setValue("Guardar");
                break;
            case "cbDelete":
                this.setiActionBtnSelect(2);
                this.setbCamposSoloLectura(true);
                this.getCbAction().setValue("Eliminar");
                if (this.proveedor.getId() != null) {
                    cargar();
                    RequestContext.getCurrentInstance().execute("PF('dlgProveedor').show();");
                }
                break;
            case "cbEdit":
                this.setiActionBtnSelect(1);
                this.getCbAction().setValue("Modificar");
                if (this.proveedor.getId() != null) {
                    cargar();
                    RequestContext.getCurrentInstance().execute("PF('dlgProveedor').show();");
                }

                // FacesContext context = FacesContext.getCurrentInstance();
                // ProveedorLstBean lstProveedor = (ProveedorLstBean)
                // context.getApplication().evaluateExpressionGet(context,
                // "#{proveedorLstBean}", ProveedorLstBean.class);
                // this.proveedor = lstProveedor.getProveedorSeleccionado();
                // if (proveedor.getDomicilio() != null) {
                // this.domicilioBean.setDomicilio(proveedor.getDomicilio());
                // } else {
                // this.domicilioBean.setDomicilio(new Domicilio());
                // }
                // if (proveedor.getTelefonos() != null && !proveedor.getTelefonos().isEmpty())
                // {
                // proveedor.getTelefonos().get(0);
                // this.listadoTelefonosBean.setLstTelefonos(this.proveedor.getTelefonos());
                // } else {
                // this.listadoTelefonosBean.setLstTelefonos(new ArrayList<Telefono>());
                // }
                break;
        }

    }// fin setBtnSelect

    private boolean existeCuit(String cuit) {
        try {
            return !proveedorRNLocal.findByCuit(cuit).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean existeRazonSocial(String razon) {
        try {
            return proveedorRNLocal.findByRazonSocial(razon) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean existeCbuAlias(String cbuAlias) {
        if (cbuAlias.isEmpty() || cbuAlias.equals("")) {
            return false;
        }
        try {
            return proveedorRNLocal.findByCbuAlias(cbuAlias) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public void create() {
        String sMensaje = "";
        FacesMessage fm;
        FacesMessage.Severity severity = null;
        try {
            if (this.getProveedor().getCuit().isEmpty() || this.getProveedor().getCuit().equals("")) {
                sMensaje = "Debe ingresar un CUIT";
                severity = FacesMessage.SEVERITY_ERROR;
            } else if (this.getProveedor().getRazonSocial().isEmpty()
                    || this.getProveedor().getRazonSocial().equals("")) {
                sMensaje = "Debe ingresar una razon Social";
                severity = FacesMessage.SEVERITY_ERROR;
            } else if (existeCuit(this.getProveedor().getCuit())) {
                sMensaje = "Ya existe un proveedor con el cuit: " + this.getProveedor().getCuit();
                severity = FacesMessage.SEVERITY_ERROR;
            } else if (existeCbuAlias(this.getProveedor().getCbuAlias())) {
                sMensaje = "Cuidado: Ya existe un proveedor con el CBU /Alias: " + this.getProveedor().getCbuAlias();
                severity = FacesMessage.SEVERITY_ERROR;
            } else if (existeRazonSocial(this.getProveedor().getRazonSocial())) {
                sMensaje = "Cuidado: Ya existe un proveedor con la razón social: " + this.getProveedor().getCuit();
                severity = FacesMessage.SEVERITY_ERROR;
            } else {
                this.getProveedor().setDomicilio(this.getDomicilioBean().getDomicilio());
                this.getProveedor().setTelefonos(this.getListadoTelefonosBean().getLstTelefonos());
                proveedorRNLocal.create(this.getProveedor());
                sMensaje += "El Proveedor fue guardado";
                severity = FacesMessage.SEVERITY_INFO;
                getProveedorLstBean().findAllProveedores();
                RequestContext.getCurrentInstance().update("frmPri:dtProveedor");
                RequestContext.getCurrentInstance().update("frmPri:growl");
                this.getCbAction().setValue("Agregar");
                this.getCbAction().setDisabled(true);
                // agregar a la lista
                // this.getUsuarioLstBean().getLstUsuario().add(this.getUsuario());
                // limíar campos
                this.limpiar();
            }

        } catch (Exception ex) {

            severity = FacesMessage.SEVERITY_ERROR;
            sMensaje = "Error: " + ex.getMessage();

        } finally {
            fm = new FacesMessage(severity, sMensaje, null);
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null, fm);
        }
    }// fin create

    public void edit() {

        String sMensaje = "";
        FacesMessage fm;
        FacesMessage.Severity severity = null;
        try {
            this.getProveedor().setDomicilio(this.getDomicilioBean().getDomicilio());
            this.getProveedor().setTelefonos(this.listadoTelefonosBean.getLstTelefonos());
            proveedorRNLocal.edit(this.getProveedor());

            sMensaje = "El dato fue modificado";
            severity = FacesMessage.SEVERITY_INFO;
            getProveedorLstBean().findAllProveedores();
            RequestContext.getCurrentInstance().update("frmPri:dtProveedor");

            // elimino y agrego a la lista
            // int iPos =
            // this.getProveedorLstBean().getLstProveedor().indexOf(this.getProveedor());
            //
            // this.getProveedorLstBean().getLstProveedor().remove(iPos);
            // this.getProveedorLstBean().getLstProveedor().add(iPos, this.getProveedor());
            this.getCbAction().setValue("Editar");
            this.getCbAction().setDisabled(true);
            limpiar();

        } catch (Exception ex) {

            if (ex.getMessage().trim().toLowerCase().equals("transaction aborted")) {
                sMensaje = "Error: No se puede eliminar";
            } else {
                sMensaje = "Error: " + ex.getMessage();
            }

            severity = FacesMessage.SEVERITY_ERROR;

        } finally {
            fm = new FacesMessage(severity, sMensaje, null);
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null, fm);
        }

    }// fin edit

    public void delete() {
        String sMensaje = "";
        FacesMessage fm;
        FacesMessage.Severity severity = null;
        try {

            severity = FacesMessage.SEVERITY_INFO;

            if (proveedorRNLocal.buscarEgresosProveedor(this.getProveedor()).isEmpty()) {
                sMensaje = "El dato fue eliminado";
                proveedorRNLocal.remove(this.getProveedor());
                getProveedorLstBean().findAllProveedores();

                limpiar();

            } else {
                sMensaje = "Error: No se puede eliminar, ya tiene egresos cargados";
            }

            this.getCbAction().setValue("Eliminar");
            this.getCbAction().setDisabled(true);
        } catch (Exception ex) {
            if (ex.getMessage().trim().toLowerCase().equals("transaction aborted")) {
                sMensaje = "Error: No se puede eliminar, ya tiene egresos cargados";
            } else {
                sMensaje = "Error: " + ex.getMessage();
            }
            severity = FacesMessage.SEVERITY_ERROR;

        } finally {
            fm = new FacesMessage(severity, sMensaje, null);
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null, fm);
        }
    }// fin delete

    public void limpiar() {
        this.setProveedor(new Proveedor());
        this.getProveedor().setDomicilio(new Domicilio());
        this.domicilioBean.setDomicilio(new Domicilio());
        this.listadoTelefonosBean.setLstTelefonos(new ArrayList<Telefono>());
        this.setbCamposSoloLectura(false);
        RequestContext.getCurrentInstance().update("frmPri:growl");
        RequestContext.getCurrentInstance().update("frmPri:dtProveedor");
        RequestContext.getCurrentInstance().execute("PF('dtProveedor').filter();");
    }// fin limpiar

    public void buscarCuitRazon() {

        try {
            if (tipoBusqueda.equals("CUIT")) {
                if (busqueda.equals("")) {
                    this.getProveedorLstBean().findAllProveedores();
                } else {
                    this.getProveedorLstBean().findDocenteCuit(busqueda);
                }
            } else {
                if (busqueda.equals("")) {
                    this.getProveedorLstBean().findAllProveedores();
                } else {
                    this.getProveedorLstBean().findDocenteRazonSocial(busqueda);
                }
            }
        } catch (Exception e) {
            // this.getMensajeBean().setMensaje("Error: " + e.getMessage());
            // RequestContext.getCurrentInstance().update("dMensaje");
            // RequestContext.getCurrentInstance().execute("dlgMensaje.show()");
        }

    }

    // metodo cargar selectOneMenu DocenteFindDlg (opciones DNI, Apellido)
    private void cargarLstTipoBusqueda() {
        lstTipoBusqueda = new ArrayList();
        lstTipoBusqueda.add("CUIT");
        lstTipoBusqueda.add("Razon Social");
    }

    public void actualizarProveedor() {
        RequestContext.getCurrentInstance().update("frmPri:otProveedor");
        RequestContext.getCurrentInstance().update("frmPri:otDocente");
        this.getProveedorLstBean().setLstProveedor(new ArrayList<Proveedor>());
        this.getProveedorLstBean().cargarProveedor();
    }

    public void abrirDlgFindProveedor(ActionEvent e) {
        btnSelect = (CommandButton) e.getSource();
        this.getProveedorLstBean().setLstProveedor(new ArrayList<Proveedor>());
        RequestContext.getCurrentInstance().execute("PF('dlgFindProveedor').show();");
    }

    public Proveedor findById(Long id) {
        try {
            return proveedorRNLocal.findById(id);
        } catch (Exception ex) {
            FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error al buscar el Proveedor: " + ex,
                    null);
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null, fm);
        }
        return null;
    }// fin dinfByid

    public Proveedor findRazonSocial(String razonSocial) {
        try {
            return proveedorRNLocal.findByRazonSocial(razonSocial);
        } catch (Exception ex) {
            FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error al buscar el Proveedor: " + ex,
                    null);
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.addMessage(null, fm);
        }
        return null;
    }// fin dinfByid

    private void cargar() {
        this.proveedor = this.proveedorRNLocal.findById(this.getProveedor().getId());
        this.domicilioBean.setDomicilio(new Domicilio());
        this.listadoTelefonosBean.setLstTelefonos(new ArrayList<Telefono>());

        if (this.proveedor.getDomicilio() != null) {
            this.domicilioBean.setDomicilio(proveedor.getDomicilio());
        } else {
            this.domicilioBean.setDomicilio(new Domicilio());
        }

        if (this.proveedor.getTelefonos() != null && !proveedor.getTelefonos().isEmpty()) {
            this.proveedor.getTelefonos().get(0);
            this.listadoTelefonosBean.setLstTelefonos(this.proveedor.getTelefonos());
        } else {
            this.listadoTelefonosBean.setLstTelefonos(new ArrayList<Telefono>());
        }
        RequestContext.getCurrentInstance().update("frmPri:dProveedor");

    }
}
