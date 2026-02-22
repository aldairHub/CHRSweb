package org.uteq.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.uteq.backend.dto.RegistroSpResultDTO;

import java.sql.Array;
import java.util.List;
import java.util.Map;

/**
 * Repository para ejecutar stored procedures de PostgreSQL
 * Maneja toda la interacción con roles de BD nativos de PostgreSQL
 */
@Repository
public class PostgresProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Valida login y obtiene credenciales BD del usuario
     * Procedure: sp_login_validar(usuario_app, clave_app)
     */
    public Map<String, Object> loginValidar(String usuarioApp) {
        String sql = "SELECT * FROM sp_login_validar(?, '')";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, usuarioApp);
        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * Obtiene roles de aplicación del usuario conectado
     * ⚠️ EJECUTAR DESPUÉS DEL SWITCH DE CONEXIÓN
     * Procedure: sp_obtener_roles_app_usuario()
     */
    public List<String> obtenerRolesAppUsuario() {
        String sql = "SELECT * FROM sp_obtener_roles_app_usuario()";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    /**
     * Lista todos los roles BD (ROLE_*) disponibles en PostgreSQL
     * Procedure: sp_listar_roles_bd_disponibles()
     */
    public List<Map<String, Object>> listarRolesBdDisponibles() {
        String sql = "SELECT * FROM sp_listar_roles_bd_disponibles()";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Lista solo los nombres de roles BD
     */
    public List<String> listarNombresRolesBd() {
        String sql = "SELECT rol_nombre FROM sp_listar_roles_bd_disponibles()";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    /**
     * Asigna un rol BD a un rol de aplicación
     * Procedure: sp_asignar_rol_bd_a_rol_app(nombre_rol_app, nombre_rol_bd)
     */
    public void asignarRolBdARolApp(String nombreRolApp, String nombreRolBd) {
        String sql = "SELECT sp_asignar_rol_bd_a_rol_app(?, ?)";
        jdbcTemplate.execute((java.sql.Connection conn) -> {
            var ps = conn.prepareStatement(sql);
            ps.setString(1, nombreRolApp);
            ps.setString(2, nombreRolBd);
            ps.execute();
            return null;
        });
    }

    /**
     * Remueve un rol BD de un rol de aplicación
     * Procedure: sp_remover_rol_bd_de_rol_app(nombre_rol_app, nombre_rol_bd)
     */
    public void removerRolBdDeRolApp(String nombreRolApp, String nombreRolBd) {
        String sql = "SELECT sp_remover_rol_bd_de_rol_app(?, ?)";
        jdbcTemplate.execute((java.sql.Connection conn) -> {
            var ps = conn.prepareStatement(sql);
            ps.setString(1, nombreRolApp);
            ps.setString(2, nombreRolBd);
            ps.execute();
            return null;
        });
    }

    /**
     * Crea un rol de aplicación con sus roles BD asociados
     * Procedure: sp_crear_rol_app(nombre, descripcion, roles_bd[])
     * @return ID del rol creado
     */
    public Integer crearRolApp(String nombre, String descripcion, List<String> rolesBd) {
        String sql = "SELECT sp_crear_rol_app(?, ?, ?)";

        return jdbcTemplate.execute((java.sql.Connection conn) -> {
            var ps = conn.prepareStatement(sql);
            ps.setString(1, nombre);
            ps.setString(2, descripcion);

            if (rolesBd != null && !rolesBd.isEmpty()) {
                Array sqlArray = conn.createArrayOf("VARCHAR", rolesBd.toArray());
                ps.setArray(3, sqlArray);
            } else {
                ps.setArray(3, conn.createArrayOf("VARCHAR", new String[0]));
            }

            var rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return null;
        });
    }

    /**
     * Actualiza un rol de aplicación y sus roles BD
     * Procedure: sp_actualizar_rol_app(id_rol_app, nombre, descripcion, roles_bd[])
     */
    public void actualizarRolApp(Integer idRolApp, String nombre, String descripcion, List<String> rolesBd) {
        String sql = "SELECT sp_actualizar_rol_app(?, ?, ?, ?)";

        jdbcTemplate.execute((java.sql.Connection conn) -> {
            var ps = conn.prepareStatement(sql);
            ps.setInt(1, idRolApp);
            ps.setString(2, nombre);
            ps.setString(3, descripcion);

            if (rolesBd != null && !rolesBd.isEmpty()) {
                Array sqlArray = conn.createArrayOf("VARCHAR", rolesBd.toArray());
                ps.setArray(4, sqlArray);
            } else {
                ps.setArray(4, conn.createArrayOf("VARCHAR", new String[0]));
            }

            ps.execute();
            return null;
        });
    }

    /**
     * Obtiene los roles BD asignados a un rol de aplicación
     * Procedure: sp_obtener_roles_bd_de_rol_app(nombre_rol_app)
     */
    public List<String> obtenerRolesBdDeRolApp(String nombreRolApp) {
        String sql = "SELECT * FROM sp_obtener_roles_bd_de_rol_app(?)";
        return jdbcTemplate.queryForList(sql, String.class, nombreRolApp);
    }

    /**
     * Asigna un rol de aplicación a un usuario
     * Procedure: sp_asignar_rol_app_a_usuario(id_usuario, nombre_rol_app)
     */
    public void asignarRolAppAUsuario(Long idUsuario, String nombreRolApp) {
        String sql = "SELECT sp_asignar_rol_app_a_usuario(?, ?)";
        jdbcTemplate.execute((java.sql.Connection conn) -> {
            var ps = conn.prepareStatement(sql);
            ps.setLong(1, idUsuario);
            ps.setString(2, nombreRolApp);
            ps.execute();
            return null;
        });
    }

    /**
     * Lista todos los roles de aplicación
     * Procedure: sp_listar_roles_app()
     */
    public List<Map<String, Object>> listarRolesApp() {
        String sql = "SELECT * FROM sp_listar_roles_app()";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Registra autoridad académica completa
     * SP: sp_registrar_autoridad_completo
     */
    public RegistroSpResultDTO registrarAutoridadCompleto(
            String usuarioApp, String claveApp, String correo,
            String usuarioBd, String claveBdHash, String claveBdReal,
            String nombres, String apellidos, java.time.LocalDate fechaNac,
            Long idInstitucion, List<String> rolesApp) {  // ✅ List<String> en vez de String

        String sql = "SELECT * FROM sp_registrar_autoridad_completo(?,?,?,?,?,?,?,?,?,?,?)";

        return jdbcTemplate.execute((java.sql.Connection conn) -> {
            var ps = conn.prepareStatement(sql);
            ps.setString(1, usuarioApp);
            ps.setString(2, claveApp);
            ps.setString(3, correo);
            ps.setString(4, usuarioBd);
            ps.setString(5, claveBdHash);
            ps.setString(6, claveBdReal);
            ps.setString(7, nombres);
            ps.setString(8, apellidos);
            ps.setObject(9, fechaNac);
            ps.setLong(10, idInstitucion);
            // ✅ Convertir List a Array SQL
            Array rolesArray = conn.createArrayOf("VARCHAR", rolesApp.toArray());
            ps.setArray(11, rolesArray);

            var rs = ps.executeQuery();
            if (rs.next()) {
                RegistroSpResultDTO result = new RegistroSpResultDTO();
                result.setIdUsuario(rs.getLong("out_id_usuario"));       // ✅ out_
                result.setIdAutoridad(rs.getLong("out_id_autoridad"));   // ✅ out_
                result.setUsuarioApp(rs.getString("out_usuario_app"));   // ✅ out_
                result.setUsuarioBd(rs.getString("out_usuario_bd"));     // ✅ out_
                return result;
            }
            throw new RuntimeException("sp_registrar_autoridad_completo no retornó datos");
        });
    }

    /**
     * Registra usuario simple
     * SP: sp_registrar_usuario_simple
     */
    public RegistroSpResultDTO registrarUsuarioSimple(
            String usuarioApp, String claveApp, String correo,
            String usuarioBd, String claveBdHash, String claveBdReal,
            List<String> rolesApp) {  // ✅ List<String> en vez de String

        String sql = "SELECT * FROM sp_registrar_usuario_simple(?,?,?,?,?,?,?)";

        return jdbcTemplate.execute((java.sql.Connection conn) -> {
            var ps = conn.prepareStatement(sql);
            ps.setString(1, usuarioApp);
            ps.setString(2, claveApp);
            ps.setString(3, correo);
            ps.setString(4, usuarioBd);
            ps.setString(5, claveBdHash);
            ps.setString(6, claveBdReal);
            // ✅ Convertir List a Array SQL
            Array rolesArray = conn.createArrayOf("VARCHAR", rolesApp.toArray());
            ps.setArray(7, rolesArray);

            var rs = ps.executeQuery();
            if (rs.next()) {
                RegistroSpResultDTO result = new RegistroSpResultDTO();
                result.setIdUsuario(rs.getLong("out_id_usuario"));
                result.setUsuarioApp(rs.getString("out_usuario_app"));
                result.setUsuarioBd(rs.getString("out_usuario_bd"));
                return result;
            }
            throw new RuntimeException("sp_registrar_usuario_simple no retornó datos");
        });
    }

    /**
     * Registra postulante aprobado
     * SP: sp_registrar_postulante
     */
    public RegistroSpResultDTO registrarPostulante(
            String usuarioApp, String claveApp, String correo,
            String usuarioBd, String claveBdHash, String claveBdReal) {

        String sql = "SELECT * FROM sp_registrar_postulante(?,?,?,?,?,?)";

        return jdbcTemplate.execute((java.sql.Connection conn) -> {
            var ps = conn.prepareStatement(sql);
            ps.setString(1, usuarioApp);
            ps.setString(2, claveApp);
            ps.setString(3, correo);
            ps.setString(4, usuarioBd);
            ps.setString(5, claveBdHash);
            ps.setString(6, claveBdReal);

            var rs = ps.executeQuery();
            if (rs.next()) {
                RegistroSpResultDTO result = new RegistroSpResultDTO();
                result.setIdUsuario(rs.getLong("out_id_usuario"));
                result.setUsuarioApp(rs.getString("out_usuario_app"));
                result.setUsuarioBd(rs.getString("out_usuario_bd"));
                return result;
            }
            throw new RuntimeException("sp_registrar_postulante no retornó datos");
        });
    }

    /**
     * Procedure: sp_cambiar_clave_app(usuario_app, clave_app_hash)
     * Actualiza clave_app y marca primer_login = false
     */
    public void cambiarClaveApp(String usuarioApp, String claveAppHash) {
        String sql = "CALL public.sp_cambiar_clave_app(?, ?)";
        jdbcTemplate.update(sql, usuarioApp, claveAppHash);
    }

    /**
     * Procedure: sp_recuperar_clave_app(usuario_app, clave_app_hash)
     * Actualiza clave_app y marca primer_login = true
     */
    public void recuperarClaveApp(String usuarioApp, String claveAppHash) {
        String sql = "CALL public.sp_recuperar_clave_app(?, ?)";
        jdbcTemplate.update(sql, usuarioApp, claveAppHash);
    }
    public void primerLoginCambiarClaveApp(String claveAppHash) {
        String sql = "CALL public.sp_primer_login_cambiar_clave_app(?)";
        try {
            jdbcTemplate.update(sql, claveAppHash);
        } catch (org.springframework.dao.DataAccessException ex) {
            Throwable root = org.springframework.core.NestedExceptionUtils.getMostSpecificCause(ex);
            System.err.println("ERROR SP primer login: " + (root != null ? root.getMessage() : ex.getMessage()));
            throw ex; // para que siga fallando, pero con mensaje real en consola
        }
    }

}
