package org.uteq.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
}
