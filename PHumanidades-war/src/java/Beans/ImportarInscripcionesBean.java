package Beans;

import Entidades.Carreras.Cohorte;
import Entidades.Carreras.InscripcionAlumnos;
import Entidades.Persona.Alumno;
import Entidades.Persona.Calidad;
import Entidades.Persona.Condicion;
import Entidades.Persona.CorreoElectronico;
import Entidades.Persona.Telefono;
import RN.AlumnoRNLocal;
import RN.InscripcionAlumnosRNLocal;
import DAO.CohorteFacadeLocal;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import org.apache.poi.ss.usermodel.*;
import org.primefaces.model.UploadedFile;
// Imports básicos de Apache POI
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.DateUtil;

@ManagedBean
@SessionScoped
public class ImportarInscripcionesBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ImportarInscripcionesBean.class.getName());

    @EJB
    private AlumnoRNLocal alumnoRNLocal;

    @EJB
    private InscripcionAlumnosRNLocal inscripcionAlumnosRNLocal;

    @EJB
    private CohorteFacadeLocal cohorteFacadeLocal;

    @ManagedProperty(value = "#{inscripcionAlumnosLstBean}")
    private InscripcionAlumnosLstBean inscripcionAlumnosLstBean;

    private UploadedFile file;

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public InscripcionAlumnosLstBean getInscripcionAlumnosLstBean() {
        return inscripcionAlumnosLstBean;
    }

    public void setInscripcionAlumnosLstBean(InscripcionAlumnosLstBean inscripcionAlumnosLstBean) {
        this.inscripcionAlumnosLstBean = inscripcionAlumnosLstBean;
    }

    public void importar() {
        if (file == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Debe seleccionar un archivo Excel."));
            return;
        }

        Workbook workbook = null;
        try {
            workbook = WorkbookFactory.create(file.getInputstream());
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getPhysicalNumberOfRows() < 2) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Advertencia",
                                "El archivo Excel está vacío o no contiene filas de datos."));
                return;
            }

            // Find header indexes
            Row headerRow = sheet.getRow(0);
            int idxNombre = getColumnIndex(headerRow, "nombre");
            int idxApellido = getColumnIndex(headerRow, "apellido");
            int idxDni = getColumnIndex(headerRow, "dni", "documento");
            int idxEmail = getColumnIndex(headerRow, "email", "correo", "mail");
            int idxTelefono = getColumnIndex(headerRow, "telefono", "tel", "celular");
            int idxFechaIns = getColumnIndex(headerRow, "fecha de inscripcion", "fecha");
            int idxMatricula = getColumnIndex(headerRow, "matricula/resolucion", "matricula");
            int idxCohorte = getColumnIndex(headerRow, "cohorte");

            if (idxDni == -1 || idxCohorte == -1) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error de Columnas",
                                "No se pudieron mapear las columnas obligatorias (DNI y Cohorte). Verifique los encabezados del archivo."));
                return;
            }

            List<Cohorte> allCohortes = cohorteFacadeLocal.findAll();
            int creadosAlumnos = 0;
            int creadasInscripciones = 0;
            int omitidos = 0;
            int errores = 0;
            StringBuilder logErrores = new StringBuilder();

            int totalRows = sheet.getLastRowNum();
            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String dni = idxDni != -1 ? getCellValueAsString(row.getCell(idxDni)) : "";
                if (dni.isEmpty()) {
                    errores++;
                    logErrores.append("Fila ").append(i + 1).append(": DNI vacío. <br/>");
                    continue;
                }

                String cohorteStr = idxCohorte != -1 ? getCellValueAsString(row.getCell(idxCohorte)) : "";
                Cohorte cohorte = findCohorte(cohorteStr, allCohortes);
                if (cohorte == null) {
                    errores++;
                    logErrores.append("Fila ").append(i + 1).append(": Cohorte '").append(cohorteStr)
                            .append("' no encontrada. <br/>");
                    continue;
                }

                try {
                    // Check if Alumno exists
                    Alumno alumno = alumnoRNLocal.findByAlumnoDni(dni);
                    boolean nuevoAlumno = false;
                    if (alumno == null) {
                        alumno = new Alumno();
                        alumno.setDni(dni);
                        alumno.setNombre(idxNombre != -1 ? getCellValueAsString(row.getCell(idxNombre)) : "");
                        alumno.setApellido(idxApellido != -1 ? getCellValueAsString(row.getCell(idxApellido)) : "");
                        alumno.setCalidad(Calidad.ACTIVO);
                        alumno.setCondicion(Condicion.REGULAR);

                        // Email
                        String emailVal = idxEmail != -1 ? getCellValueAsString(row.getCell(idxEmail)) : "";
                        if (!emailVal.isEmpty()) {
                            List<CorreoElectronico> emails = new ArrayList<>();
                            CorreoElectronico email = new CorreoElectronico();
                            email.setDireccion(emailVal);
                            emails.add(email);
                            alumno.setCorreosElectronicos(emails);
                        }

                        // Telefono
                        String telVal = idxTelefono != -1 ? getCellValueAsString(row.getCell(idxTelefono)) : "";
                        if (!telVal.isEmpty()) {
                            List<Telefono> telfs = new ArrayList<>();
                            Telefono tel = new Telefono();
                            tel.setNumero(telVal);
                            telfs.add(tel);
                            alumno.setTelefonos(telfs);
                        }

                        alumnoRNLocal.create(alumno);
                        creadosAlumnos++;
                        nuevoAlumno = true;
                    } else {
                        // Optional: update email or phone if existing student doesn't have them
                        boolean editNeeded = false;
                        if ((alumno.getCorreosElectronicos() == null || alumno.getCorreosElectronicos().isEmpty())) {
                            String emailVal = idxEmail != -1 ? getCellValueAsString(row.getCell(idxEmail)) : "";
                            if (!emailVal.isEmpty()) {
                                List<CorreoElectronico> emails = new ArrayList<>();
                                CorreoElectronico email = new CorreoElectronico();
                                email.setDireccion(emailVal);
                                emails.add(email);
                                alumno.setCorreosElectronicos(emails);
                                editNeeded = true;
                            }
                        }
                        if ((alumno.getTelefonos() == null || alumno.getTelefonos().isEmpty())) {
                            String telVal = idxTelefono != -1 ? getCellValueAsString(row.getCell(idxTelefono)) : "";
                            if (!telVal.isEmpty()) {
                                List<Telefono> telfs = new ArrayList<>();
                                Telefono tel = new Telefono();
                                tel.setNumero(telVal);
                                telfs.add(tel);
                                alumno.setTelefonos(telfs);
                                editNeeded = true;
                            }
                        }
                        if (editNeeded) {
                            alumnoRNLocal.edit(alumno);
                        }
                    }

                    // Check if already inscripto in this Cohorte
                    boolean inscripto = false;
                    try {
                        if (!inscripcionAlumnosRNLocal.findAlumnoCohorte(dni, cohorte.getId()).isEmpty()) {
                            inscripto = true;
                        }
                    } catch (Exception e) {
                        inscripto = false;
                    }

                    if (inscripto) {
                        omitidos++;
                    } else {
                        InscripcionAlumnos inscripcion = new InscripcionAlumnos();
                        inscripcion.setAlumno(alumno);
                        inscripcion.setCohorte(cohorte);
                        inscripcion.setActivo(true);

                        Date fecha = idxFechaIns != -1 ? parseDate(row.getCell(idxFechaIns)) : new Date();
                        inscripcion.setFechaInscripcion(fecha);

                        String mat = idxMatricula != -1 ? getCellValueAsString(row.getCell(idxMatricula)) : "";
                        inscripcion.setMatricula(mat);

                        inscripcionAlumnosRNLocal.create(inscripcion);
                        creadasInscripciones++;
                    }

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error al importar fila " + (i + 1), e);
                    errores++;
                    logErrores.append("Fila ").append(i + 1).append(": Error al registrar Alumno/Inscripción: ")
                            .append(e.getMessage()).append("<br/>");
                }
            }

            // Refresh table list
            if (inscripcionAlumnosLstBean != null) {
                inscripcionAlumnosLstBean.cargarInscripciones();
            }

            String summary = String.format(
                    "Procesamiento Finalizado.<br/>" +
                            "- Alumnos Nuevos: %d<br/>" +
                            "- Inscripciones Nuevas: %d<br/>" +
                            "- Fila Omitidas (ya inscriptos): %d<br/>" +
                            "- Filas con errores: %d",
                    creadosAlumnos, creadasInscripciones, omitidos, errores);

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Importación Finalizada", summary));

            if (errores > 0) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Detalle de Errores", logErrores.toString()));
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error general en la importación de Excel", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error de Importación",
                            "No se pudo procesar el archivo Excel: " + e.getMessage()));
        } finally {
            // POI 3.7 Workbook doesn't implement Closeable / close() method
        }
    }

    private int getColumnIndex(Row headerRow, String... aliases) {
        if (headerRow == null) {
            return -1;
        }
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String val = cell.getStringCellValue().trim();
                String normalizedVal = normalizeString(val);
                for (String alias : aliases) {
                    String normalizedAlias = normalizeString(alias);
                    if (normalizedVal.equals(normalizedAlias) || normalizedVal.contains(normalizedAlias)
                            || normalizedAlias.contains(normalizedVal)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private String normalizeString(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase()
                .replaceAll("[áàäâ]", "a")
                .replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i")
                .replaceAll("[óòöô]", "o")
                .replaceAll("[úùüû]", "u")
                .replaceAll("[^a-z0-9]", "");
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_STRING:
                return cell.getStringCellValue().trim();
            case Cell.CELL_TYPE_NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat("dd/MM/yyyy").format(cell.getDateCellValue());
                }
                double numericVal = cell.getNumericCellValue();
                if (numericVal == (long) numericVal) {
                    return String.format("%d", (long) numericVal);
                } else {
                    return String.format("%s", numericVal);
                }
            case Cell.CELL_TYPE_BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case Cell.CELL_TYPE_FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    try {
                        double val = cell.getNumericCellValue();
                        if (val == (long) val) {
                            return String.format("%d", (long) val);
                        } else {
                            return String.format("%s", val);
                        }
                    } catch (Exception ex) {
                        return "";
                    }
                }
            case Cell.CELL_TYPE_BLANK:
            default:
                return "";
        }
    }

    private Date parseDate(Cell cell) {
        if (cell == null) {
            return new Date();
        }
        if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue();
        }
        String str = getCellValueAsString(cell);
        if (str == null || str.trim().isEmpty()) {
            return new Date();
        }
        String[] patterns = { "dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy", "d/M/yyyy", "yyyy/MM/dd" };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                sdf.setLenient(false);
                return sdf.parse(str);
            } catch (Exception e) {
                // ignore and try next
            }
        }
        return new Date();
    }

    private Cohorte findCohorte(String description, List<Cohorte> allCohortes) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }
        String normalizedDesc = normalizeString(description);
        for (Cohorte c : allCohortes) {
            String cDesc = normalizeString(c.getDescripcion());
            if (cDesc.equals(normalizedDesc) || cDesc.contains(normalizedDesc) || normalizedDesc.contains(cDesc)) {
                return c;
            }
        }
        return null;
    }
}
