-- ============================================================
-- SPs DE LÓGICA DE NEGOCIO
-- Archivo: src/main/resources/db/sp_negocio.sql
-- Se ejecuta automáticamente al arrancar el backend
-- ============================================================

CREATE OR REPLACE FUNCTION public.fn_detalle_convocatoria(p_id_convocatoria bigint)
 RETURNS TABLE(id_convocatoria bigint, titulo character varying, descripcion character varying, fecha_publicacion date, fecha_inicio date, fecha_fin date, fecha_limite_documentos date, estado_convocatoria character varying, documentos_abiertos boolean, id_solicitud bigint, nombre_materia character varying, nombre_carrera character varying, nombre_facultad character varying, cantidad_docentes bigint, nivel_academico character varying, estado_solicitud character varying)
 LANGUAGE plpgsql AS $fn$
BEGIN
RETURN QUERY
SELECT c.id_convocatoria, c.titulo, c.descripcion, c.fecha_publicacion, c.fecha_inicio, c.fecha_fin,
       c.fecha_limite_documentos, c.estado_convocatoria,
       (c.fecha_limite_documentos IS NULL OR CURRENT_DATE <= c.fecha_limite_documentos) AS documentos_abiertos,
       s.id_solicitud, m.nombre_materia, ca.nombre_carrera, f.nombre_facultad,
       s.cantidad_docentes, s.nivel_academico, s.estado_solicitud
FROM public.convocatoria c
         LEFT JOIN public.convocatoria_solicitud cs ON cs.id_convocatoria = c.id_convocatoria
         LEFT JOIN public.solicitud_docente s       ON s.id_solicitud = cs.id_solicitud
         LEFT JOIN public.materia m                 ON m.id_materia = s.id_materia
         LEFT JOIN public.carrera ca                ON ca.id_carrera = s.id_carrera
         LEFT JOIN public.facultad f                ON f.id_facultad = ca.id_facultad
WHERE c.id_convocatoria = p_id_convocatoria;
END; $fn$;

CREATE OR REPLACE FUNCTION public.fn_listar_convocatorias(p_estado character varying DEFAULT NULL::character varying, p_titulo character varying DEFAULT NULL::character varying)
 RETURNS TABLE(id_convocatoria bigint, titulo character varying, descripcion character varying, fecha_publicacion date, fecha_inicio date, fecha_fin date, fecha_limite_documentos date, estado_convocatoria character varying, total_solicitudes bigint, documentos_abiertos boolean)
 LANGUAGE plpgsql AS $fn$
BEGIN
RETURN QUERY
SELECT c.id_convocatoria, c.titulo, c.descripcion, c.fecha_publicacion, c.fecha_inicio, c.fecha_fin,
       c.fecha_limite_documentos, c.estado_convocatoria,
       COUNT(cs.id_solicitud) AS total_solicitudes,
       (c.fecha_limite_documentos IS NULL OR CURRENT_DATE <= c.fecha_limite_documentos) AS documentos_abiertos
FROM public.convocatoria c
         LEFT JOIN public.convocatoria_solicitud cs ON cs.id_convocatoria = c.id_convocatoria
WHERE (p_estado IS NULL OR c.estado_convocatoria = p_estado)
  AND (p_titulo IS NULL OR c.titulo ILIKE '%' || p_titulo || '%')
GROUP BY c.id_convocatoria
ORDER BY c.fecha_publicacion DESC;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_agregar_documento_prepostulacion(p_id_prepostulacion bigint, p_descripcion character varying, p_url_documento text)
 RETURNS TABLE(out_id_documento bigint)
 LANGUAGE plpgsql SECURITY DEFINER AS $fn$
DECLARE v_id BIGINT;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.prepostulacion WHERE id_prepostulacion = p_id_prepostulacion) THEN
        RAISE EXCEPTION 'Prepostulación no encontrada: %', p_id_prepostulacion;
END IF;
INSERT INTO public.prepostulacion_documentos (id_prepostulacion, descripcion, url_documento, fecha_subida)
VALUES (p_id_prepostulacion, p_descripcion, p_url_documento, NOW())
    RETURNING id_documento INTO v_id;
RETURN QUERY SELECT v_id;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_eliminar_documento(p_id_documento bigint, p_id_postulacion bigint, OUT v_eliminado boolean, OUT v_mensaje character varying)
 RETURNS record LANGUAGE plpgsql AS $fn$
BEGIN
  IF EXISTS (SELECT 1 FROM public.documento WHERE id_documento = p_id_documento AND id_postulacion = p_id_postulacion AND estado_validacion = 'pendiente') THEN
DELETE FROM public.documento WHERE id_documento = p_id_documento;
v_eliminado := TRUE;
    v_mensaje   := 'Documento eliminado correctamente.';
ELSE
    v_eliminado := FALSE;
    v_mensaje   := 'No se puede eliminar: el documento no existe, no pertenece a esta postulación, o ya fue validado.';
END IF;
EXCEPTION WHEN OTHERS THEN
  v_eliminado := FALSE;
  v_mensaje   := 'ERROR: ' || SQLERRM;
END; $fn$;

CREATE OR REPLACE PROCEDURE public.sp_eliminar_notificaciones_antiguas(OUT p_total integer, OUT p_mensaje text, IN p_dias_max integer DEFAULT 90)
 LANGUAGE plpgsql SECURITY DEFINER AS $proc$
BEGIN
DELETE FROM notificacion WHERE leida = TRUE AND fecha_creacion < NOW() - (p_dias_max || ' days')::INTERVAL;
GET DIAGNOSTICS p_total = ROW_COUNT;
p_mensaje := 'Eliminadas ' || p_total || ' notificaciones antiguas';
EXCEPTION WHEN OTHERS THEN
    p_total   := 0;
    p_mensaje := 'Error: ' || SQLERRM;
END; $proc$;

CREATE OR REPLACE FUNCTION public.sp_finalizar_carga_documentos(p_id_postulacion bigint)
 RETURNS TABLE(exitoso boolean, mensaje text) LANGUAGE plpgsql AS $fn$
DECLARE v_estado TEXT; v_faltantes INTEGER;
BEGIN
SELECT estado_postulacion INTO v_estado FROM postulacion WHERE id_postulacion = p_id_postulacion;
IF NOT FOUND THEN RETURN QUERY SELECT FALSE, 'La postulación no existe.'; RETURN; END IF;
    IF LOWER(TRIM(v_estado)) = 'en_revision' THEN RETURN QUERY SELECT FALSE, 'La postulación ya fue finalizada.'; RETURN; END IF;
    IF LOWER(TRIM(v_estado)) <> 'pendiente' THEN RETURN QUERY SELECT FALSE, 'La postulación no está en estado pendiente.'; RETURN; END IF;
SELECT COUNT(*) INTO v_faltantes FROM tipo_documento td WHERE td.obligatorio = TRUE
                                                          AND NOT EXISTS (SELECT 1 FROM documento d WHERE d.id_postulacion = p_id_postulacion AND d.id_tipo_documento = td.id_tipo_documento);
IF v_faltantes > 0 THEN RETURN QUERY SELECT FALSE, 'Faltan ' || v_faltantes || ' documento(s) obligatorio(s).'; RETURN; END IF;
UPDATE postulacion SET estado_postulacion = 'en_revision' WHERE id_postulacion = p_id_postulacion;
RETURN QUERY SELECT TRUE, 'Postulación finalizada correctamente.';
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_guardar_documento(p_id_postulacion bigint, p_id_tipo_documento bigint, p_ruta_archivo text, OUT v_id_documento bigint, OUT v_mensaje character varying)
 RETURNS record LANGUAGE plpgsql AS $fn$
DECLARE v_existe BIGINT;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM public.postulacion WHERE id_postulacion = p_id_postulacion) THEN
    v_id_documento := NULL; v_mensaje := 'ERROR: La postulación no existe.'; RETURN;
END IF;
  IF NOT EXISTS (SELECT 1 FROM public.tipo_documento WHERE id_tipo_documento = p_id_tipo_documento AND activo = TRUE) THEN
    v_id_documento := NULL; v_mensaje := 'ERROR: El tipo de documento no existe o está inactivo.'; RETURN;
END IF;
SELECT id_documento INTO v_existe FROM public.documento WHERE id_postulacion = p_id_postulacion AND id_tipo_documento = p_id_tipo_documento LIMIT 1;
IF v_existe IS NOT NULL THEN
UPDATE public.documento SET ruta_archivo = p_ruta_archivo, estado_validacion = 'pendiente', fecha_carga = CURRENT_TIMESTAMP WHERE id_documento = v_existe;
v_id_documento := v_existe; v_mensaje := 'ACTUALIZADO';
ELSE
    INSERT INTO public.documento (id_postulacion, id_tipo_documento, ruta_archivo, estado_validacion, fecha_carga)
    VALUES (p_id_postulacion, p_id_tipo_documento, p_ruta_archivo, 'pendiente', CURRENT_TIMESTAMP)
    RETURNING id_documento INTO v_id_documento;
    v_mensaje := 'INSERTADO';
END IF;
EXCEPTION WHEN OTHERS THEN v_id_documento := NULL; v_mensaje := 'ERROR: ' || SQLERRM;
END; $fn$;

CREATE OR REPLACE PROCEDURE public.sp_marcar_notificacion_leida(IN p_id_notificacion bigint, IN p_id_usuario bigint, OUT p_ok boolean, OUT p_mensaje text)
 LANGUAGE plpgsql SECURITY DEFINER AS $proc$
BEGIN
UPDATE notificacion SET leida = TRUE, fecha_leida = NOW()
WHERE id_notificacion = p_id_notificacion AND id_usuario = p_id_usuario AND leida = FALSE;
IF FOUND THEN p_ok := TRUE; p_mensaje := 'Notificación marcada como leída';
ELSE p_ok := FALSE; p_mensaje := 'No encontrada o ya estaba leída'; END IF;
EXCEPTION WHEN OTHERS THEN p_ok := FALSE; p_mensaje := 'Error: ' || SQLERRM;
END; $proc$;

CREATE OR REPLACE FUNCTION public.sp_notif_usuarios_por_rol(p_nombre_rol character varying)
 RETURNS TABLE(id_usuario bigint) LANGUAGE plpgsql SECURITY DEFINER AS $fn$
BEGIN
RETURN QUERY
SELECT DISTINCT u.id_usuario FROM usuario u
                                      JOIN usuario_roles_app ura ON ura.id_usuario = u.id_usuario
                                      JOIN roles_app ra          ON ra.id_rol_app  = ura.id_rol_app
WHERE LOWER(ra.nombre) LIKE LOWER('%' || p_nombre_rol || '%') AND u.activo = TRUE;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_obtener_docs_prepostulacion(p_id_postulacion bigint)
 RETURNS TABLE(id_documento bigint, descripcion character varying, url_documento character varying, fecha_subida timestamp with time zone)
 LANGUAGE plpgsql AS $fn$
BEGIN
RETURN QUERY
SELECT pd.id_documento, pd.descripcion, pd.url_documento, pd.fecha_subida
FROM postulacion po
         JOIN postulante pt                ON pt.id_postulante = po.id_postulante
         JOIN prepostulacion_documentos pd ON pd.id_prepostulacion = pt.id_prepostulacion
WHERE po.id_postulacion = p_id_postulacion;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_obtener_documentos_convocatoria(p_id_postulacion bigint)
 RETURNS TABLE(id_tipo_documento bigint, nombre_tipo character varying, descripcion_tipo character varying, obligatorio boolean, id_documento bigint, estado_validacion character varying, ruta_archivo character varying, fecha_carga timestamp without time zone, observaciones_ia character varying)
 LANGUAGE plpgsql AS $fn$
BEGIN
RETURN QUERY
SELECT DISTINCT ON (td.id_tipo_documento)
    td.id_tipo_documento, td.nombre, td.descripcion, ctd.obligatorio,
    d.id_documento, d.estado_validacion, d.ruta_archivo, d.fecha_carga, NULL::VARCHAR AS observaciones_ia
FROM postulacion po
    JOIN convocatoria_solicitud cs       ON cs.id_solicitud = po.id_solicitud
    JOIN convocatoria_tipo_documento ctd ON ctd.id_convocatoria = cs.id_convocatoria
    JOIN tipo_documento td               ON td.id_tipo_documento = ctd.id_tipo_documento
    LEFT JOIN documento d                ON d.id_postulacion = po.id_postulacion AND d.id_tipo_documento = td.id_tipo_documento
WHERE po.id_postulacion = p_id_postulacion AND td.activo = TRUE
ORDER BY td.id_tipo_documento;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_obtener_documentos_postulacion(p_id_postulacion bigint)
 RETURNS TABLE(id_tipo_documento bigint, nombre_tipo character varying, descripcion_tipo character varying, obligatorio boolean, id_documento bigint, estado_validacion character varying, ruta_archivo character varying, fecha_carga timestamp without time zone, observaciones_ia character varying)
 LANGUAGE plpgsql AS $fn$
DECLARE v_id_convocatoria BIGINT; v_tiene_propios BOOLEAN;
BEGIN
SELECT cs.id_convocatoria INTO v_id_convocatoria FROM public.postulacion p
                                                          JOIN public.solicitud_docente sd ON sd.id_solicitud = p.id_solicitud
                                                          JOIN public.convocatoria_solicitud cs ON cs.id_solicitud = sd.id_solicitud
WHERE p.id_postulacion = p_id_postulacion LIMIT 1;
SELECT EXISTS (SELECT 1 FROM public.convocatoria_tipo_documento WHERE id_convocatoria = v_id_convocatoria) INTO v_tiene_propios;
IF v_tiene_propios AND v_id_convocatoria IS NOT NULL THEN
        RETURN QUERY SELECT DISTINCT ON (td.id_tipo_documento)
    td.id_tipo_documento, td.nombre, td.descripcion, ctd.obligatorio,
    d.id_documento, d.estado_validacion, d.ruta_archivo, d.fecha_carga, r.observaciones
                     FROM public.convocatoria_tipo_documento ctd
                            JOIN public.tipo_documento td ON td.id_tipo_documento = ctd.id_tipo_documento
                            LEFT JOIN public.documento d ON d.id_tipo_documento = td.id_tipo_documento AND d.id_postulacion = p_id_postulacion
                            LEFT JOIN public.resultados_ia_documento r ON r.id_documento = d.id_documento
                     WHERE ctd.id_convocatoria = v_id_convocatoria AND td.activo = TRUE
                     ORDER BY td.id_tipo_documento, ctd.obligatorio DESC;
ELSE
        RETURN QUERY SELECT DISTINCT ON (td.id_tipo_documento)
    td.id_tipo_documento, td.nombre, td.descripcion, td.obligatorio,
    d.id_documento, d.estado_validacion, d.ruta_archivo, d.fecha_carga, r.observaciones
                     FROM public.tipo_documento td
                         LEFT JOIN public.documento d ON d.id_tipo_documento = td.id_tipo_documento AND d.id_postulacion = p_id_postulacion
                         LEFT JOIN public.resultados_ia_documento r ON r.id_documento = d.id_documento
                     WHERE td.activo = TRUE ORDER BY td.id_tipo_documento, td.obligatorio DESC;
END IF;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_obtener_info_postulante(p_id_usuario bigint)
 RETURNS TABLE(id_postulante bigint, nombres character varying, apellidos character varying, identificacion character varying, correo character varying, id_postulacion bigint, estado_postulacion character varying, nombre_materia character varying, nombre_carrera character varying, nombre_area character varying, fecha_limite_documentos date, documentos_abiertos boolean)
 LANGUAGE plpgsql AS $fn$
BEGIN
RETURN QUERY
SELECT pt.id_postulante, pt.nombres_postulante::VARCHAR, pt.apellidos_postulante::VARCHAR,
    pt.identificacion::VARCHAR, pt.correo_postulante::VARCHAR,
    po.id_postulacion, po.estado_postulacion::VARCHAR,
    m.nombre_materia::VARCHAR, ca.nombre_carrera::VARCHAR, a.nombre_area::VARCHAR,
    c.fecha_limite_documentos,
    CASE WHEN c.fecha_limite_documentos IS NOT NULL THEN CURRENT_DATE <= c.fecha_limite_documentos
         ELSE CURRENT_DATE <= c.fecha_fin END AS documentos_abiertos
FROM postulante pt
         JOIN postulacion po     ON po.id_postulante = pt.id_postulante
         JOIN solicitud_docente sd ON sd.id_solicitud = po.id_solicitud
         JOIN convocatoria_solicitud cs ON cs.id_solicitud = sd.id_solicitud
         JOIN convocatoria c     ON c.id_convocatoria = cs.id_convocatoria
         JOIN materia m          ON m.id_materia = sd.id_materia
         JOIN carrera ca         ON ca.id_carrera = sd.id_carrera
         JOIN area_conocimiento a ON a.id_area = sd.id_area
WHERE pt.id_usuario = p_id_usuario LIMIT 1;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_registrar_autoridad_completo(p_usuario_app character varying, p_clave_app character varying, p_correo character varying, p_usuario_bd character varying, p_clave_bd_hash character varying, p_clave_bd_real character varying, p_nombres character varying, p_apellidos character varying, p_fecha_nac date, p_id_institucion bigint, p_roles_app character varying[])
 RETURNS TABLE(out_id_usuario bigint, out_id_autoridad bigint, out_usuario_app character varying, out_usuario_bd character varying)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $fn$
DECLARE v_id_usuario BIGINT; v_id_autoridad BIGINT; v_rol_app VARCHAR; v_id_rol_app INTEGER; v_rol_bd VARCHAR; v_roles_bd VARCHAR[];
BEGIN
INSERT INTO usuario (usuario_app, clave_app, correo, usuario_bd, clave_bd, activo, fecha_creacion)
VALUES (p_usuario_app, p_clave_app, p_correo, p_usuario_bd, p_clave_bd_hash, true, CURRENT_TIMESTAMP)
    RETURNING usuario.id_usuario INTO v_id_usuario;
IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = p_usuario_bd) THEN
        EXECUTE format('CREATE USER %I WITH PASSWORD %L', p_usuario_bd, p_clave_bd_real);
END IF;
EXECUTE format('GRANT base_app_permisos TO %I', p_usuario_bd);
FOREACH v_rol_app IN ARRAY p_roles_app LOOP
SELECT ra.id_rol_app INTO v_id_rol_app FROM roles_app ra WHERE ra.nombre = v_rol_app AND ra.activo = true;
IF v_id_rol_app IS NULL THEN RAISE EXCEPTION 'Rol de aplicación % no existe o está inactivo', v_rol_app; END IF;
SELECT ARRAY_AGG(rab.nombre_rol_bd) INTO v_roles_bd FROM roles_app_bd rab WHERE rab.id_rol_app = v_id_rol_app;
FOREACH v_rol_bd IN ARRAY v_roles_bd LOOP
            EXECUTE format('GRANT %I TO %I', v_rol_bd, p_usuario_bd);
END LOOP;
INSERT INTO usuario_roles_app (id_usuario, id_rol_app) VALUES (v_id_usuario, v_id_rol_app) ON CONFLICT (id_usuario, id_rol_app) DO NOTHING;
END LOOP;
INSERT INTO autoridad_academica (nombres, apellidos, correo, fecha_nacimiento, estado, id_institucion, id_usuario)
VALUES (p_nombres, p_apellidos, p_correo, p_fecha_nac, true, p_id_institucion, v_id_usuario)
    RETURNING autoridad_academica.id_autoridad INTO v_id_autoridad;
RETURN QUERY SELECT v_id_usuario, v_id_autoridad, p_usuario_app::VARCHAR, p_usuario_bd::VARCHAR;
EXCEPTION WHEN OTHERS THEN RAISE EXCEPTION 'Error en sp_registrar_autoridad_completo: %', SQLERRM;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_registrar_postulante(p_usuario_app character varying, p_clave_app character varying, p_correo character varying, p_usuario_bd character varying, p_clave_bd_hash character varying, p_clave_bd_real character varying)
 RETURNS TABLE(out_id_usuario bigint, out_usuario_app character varying, out_usuario_bd character varying)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $fn$
DECLARE v_id_usuario BIGINT; v_id_rol_app INTEGER; v_rol_bd VARCHAR; v_roles_bd VARCHAR[];
BEGIN
SELECT ra.id_rol_app INTO v_id_rol_app FROM roles_app ra WHERE ra.nombre = 'POSTULANTE' AND ra.activo = true;
IF v_id_rol_app IS NULL THEN RAISE EXCEPTION 'Rol POSTULANTE no existe o está inactivo'; END IF;
SELECT ARRAY_AGG(rab.nombre_rol_bd) INTO v_roles_bd FROM roles_app_bd rab WHERE rab.id_rol_app = v_id_rol_app;
INSERT INTO usuario (usuario_app, clave_app, correo, usuario_bd, clave_bd, activo, fecha_creacion)
VALUES (p_usuario_app, p_clave_app, p_correo, p_usuario_bd, p_clave_bd_hash, true, CURRENT_TIMESTAMP)
    RETURNING usuario.id_usuario INTO v_id_usuario;
IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = p_usuario_bd) THEN
        EXECUTE format('CREATE USER %I WITH PASSWORD %L', p_usuario_bd, p_clave_bd_real);
END IF;
EXECUTE format('GRANT base_app_permisos TO %I', p_usuario_bd);
FOREACH v_rol_bd IN ARRAY v_roles_bd LOOP EXECUTE format('GRANT %I TO %I', v_rol_bd, p_usuario_bd); END LOOP;
INSERT INTO usuario_roles_app (id_usuario, id_rol_app) VALUES (v_id_usuario, v_id_rol_app) ON CONFLICT (id_usuario, id_rol_app) DO NOTHING;
RETURN QUERY SELECT v_id_usuario, p_usuario_app::VARCHAR, p_usuario_bd::VARCHAR;
EXCEPTION WHEN OTHERS THEN RAISE EXCEPTION 'Error en sp_registrar_postulante: %', SQLERRM;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_registrar_postulante(p_usuario_app character varying, p_clave_app character varying, p_correo character varying, p_usuario_bd character varying, p_clave_bd_hash character varying, p_clave_bd_real character varying, p_id_prepostulacion bigint)
 RETURNS TABLE(out_id_usuario bigint, out_usuario_app character varying, out_usuario_bd character varying)
 LANGUAGE plpgsql SECURITY DEFINER AS $fn$
DECLARE v_id_usuario BIGINT; v_id_rol_app INTEGER; v_rol_bd VARCHAR; v_roles_bd VARCHAR[]; v_id_postulante BIGINT; v_id_solicitud BIGINT;
BEGIN
SELECT ra.id_rol_app INTO v_id_rol_app FROM roles_app ra WHERE ra.nombre = 'POSTULANTE' AND ra.activo = true;
IF v_id_rol_app IS NULL THEN RAISE EXCEPTION 'Rol POSTULANTE no existe o está inactivo'; END IF;
SELECT ARRAY_AGG(rab.nombre_rol_bd) INTO v_roles_bd FROM roles_app_bd rab WHERE rab.id_rol_app = v_id_rol_app;
INSERT INTO usuario (usuario_app, clave_app, correo, usuario_bd, clave_bd, activo, fecha_creacion)
VALUES (p_usuario_app, p_clave_app, p_correo, p_usuario_bd, p_clave_bd_hash, true, CURRENT_TIMESTAMP)
    RETURNING usuario.id_usuario INTO v_id_usuario;
IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = p_usuario_bd) THEN
        EXECUTE format('CREATE USER %I WITH PASSWORD %L', p_usuario_bd, p_clave_bd_real);
END IF;
EXECUTE format('GRANT base_app_permisos TO %I', p_usuario_bd);
FOREACH v_rol_bd IN ARRAY v_roles_bd LOOP EXECUTE format('GRANT %I TO %I', v_rol_bd, p_usuario_bd); END LOOP;
INSERT INTO usuario_roles_app (id_usuario, id_rol_app) VALUES (v_id_usuario, v_id_rol_app) ON CONFLICT (id_usuario, id_rol_app) DO NOTHING;
INSERT INTO postulante (nombres_postulante, apellidos_postulante, correo_postulante, identificacion, id_prepostulacion, id_usuario)
SELECT pre.nombres, pre.apellidos, pre.correo, pre.identificacion, pre.id_prepostulacion, v_id_usuario
FROM prepostulacion pre WHERE pre.id_prepostulacion = p_id_prepostulacion
    RETURNING id_postulante INTO v_id_postulante;
SELECT ps.id_solicitud INTO v_id_solicitud FROM prepostulacion_solicitud ps WHERE ps.id_prepostulacion = p_id_prepostulacion LIMIT 1;
INSERT INTO postulacion (id_postulante, id_solicitud, estado_postulacion, fecha) VALUES (v_id_postulante, v_id_solicitud, 'pendiente', CURRENT_TIMESTAMP);
RETURN QUERY SELECT v_id_usuario, p_usuario_app::VARCHAR, p_usuario_bd::VARCHAR;
EXCEPTION WHEN OTHERS THEN RAISE EXCEPTION 'Error en sp_registrar_postulante: %', SQLERRM;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_registrar_prepostulacion(p_nombres character varying, p_apellidos character varying, p_identificacion character varying, p_correo character varying, p_url_cedula text, p_url_foto text, p_id_solicitud bigint)
 RETURNS TABLE(out_id_prepostulacion bigint) LANGUAGE plpgsql AS $fn$
DECLARE v_id BIGINT;
BEGIN
INSERT INTO public.prepostulacion (nombres, apellidos, identificacion, correo, url_cedula, url_foto, fecha_envio, estado_revision)
VALUES (p_nombres, p_apellidos, p_identificacion, p_correo, p_url_cedula, p_url_foto, NOW(), 'PENDIENTE')
    RETURNING id_prepostulacion INTO v_id;
INSERT INTO public.prepostulacion_solicitud (id_prepostulacion, id_solicitud) VALUES (v_id, p_id_solicitud);
RETURN QUERY SELECT v_id;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_repostular(p_identificacion text, p_id_solicitud bigint, p_url_cedula text, p_url_foto text)
 RETURNS TABLE(out_id_prepostulacion bigint) LANGUAGE plpgsql SECURITY DEFINER AS $fn$
DECLARE v_ultimo prepostulacion%ROWTYPE; v_id_nuevo BIGINT;
BEGIN
SELECT * INTO v_ultimo FROM prepostulacion WHERE identificacion = p_identificacion ORDER BY fecha_envio DESC LIMIT 1;
IF NOT FOUND THEN RAISE EXCEPTION 'No existe ninguna solicitud registrada con la cédula %.', p_identificacion; END IF;
    IF UPPER(v_ultimo.estado_revision) <> 'RECHAZADO' THEN RAISE EXCEPTION 'Solo puede re-postular si su solicitud fue RECHAZADA. Estado actual: %.', v_ultimo.estado_revision; END IF;
INSERT INTO prepostulacion (nombres, apellidos, identificacion, correo, url_cedula, url_foto, url_prerrequisitos, estado_revision, fecha_envio)
VALUES (v_ultimo.nombres, v_ultimo.apellidos, p_identificacion, v_ultimo.correo, p_url_cedula, p_url_foto, NULL, 'PENDIENTE', NOW())
    RETURNING id_prepostulacion INTO v_id_nuevo;
IF p_id_solicitud IS NOT NULL THEN
        INSERT INTO prepostulacion_solicitud (id_prepostulacion, id_solicitud) VALUES (v_id_nuevo, p_id_solicitud) ON CONFLICT DO NOTHING;
END IF;
    out_id_prepostulacion := v_id_nuevo; RETURN NEXT;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_repostular(p_identificacion character varying, p_id_solicitud bigint, p_url_cedula text, p_url_foto text)
 RETURNS TABLE(out_id_prepostulacion bigint) LANGUAGE plpgsql AS $fn$
DECLARE v_id_nuevo BIGINT; v_ultimo public.prepostulacion%ROWTYPE;
BEGIN
SELECT * INTO v_ultimo FROM public.prepostulacion WHERE identificacion = p_identificacion ORDER BY fecha_envio DESC LIMIT 1;
IF v_ultimo.id_prepostulacion IS NULL THEN RAISE EXCEPTION 'No se encontró postulación para la cédula %', p_identificacion; END IF;
    IF UPPER(v_ultimo.estado_revision) != 'RECHAZADO' THEN RAISE EXCEPTION 'La postulación tiene estado "%" y no puede repostular', v_ultimo.estado_revision; END IF;
INSERT INTO public.prepostulacion (nombres, apellidos, identificacion, correo, url_cedula, url_foto, fecha_envio, estado_revision)
VALUES (v_ultimo.nombres, v_ultimo.apellidos, v_ultimo.identificacion, v_ultimo.correo, p_url_cedula, p_url_foto, NOW(), 'PENDIENTE')
    RETURNING id_prepostulacion INTO v_id_nuevo;
INSERT INTO public.prepostulacion_solicitud (id_prepostulacion, id_solicitud) VALUES (v_id_nuevo, p_id_solicitud);
RETURN QUERY SELECT v_id_nuevo;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_repostular(p_identificacion character varying, p_id_solicitud bigint, p_url_cedula character varying, p_url_foto character varying, p_url_prerrequisitos character varying)
 RETURNS TABLE(out_id_prepostulacion bigint) LANGUAGE plpgsql AS $fn$
DECLARE v_id_nuevo BIGINT; v_ultimo public.prepostulacion%ROWTYPE;
BEGIN
SELECT * INTO v_ultimo FROM public.prepostulacion WHERE identificacion = p_identificacion ORDER BY fecha_envio DESC LIMIT 1;
IF v_ultimo.id_prepostulacion IS NULL THEN RAISE EXCEPTION 'No se encontró postulación para la cédula %', p_identificacion; END IF;
    IF UPPER(v_ultimo.estado_revision) != 'RECHAZADO' THEN RAISE EXCEPTION 'La postulación tiene estado "%" y no puede repostular', v_ultimo.estado_revision; END IF;
INSERT INTO public.prepostulacion (nombres, apellidos, identificacion, correo, url_cedula, url_foto, url_prerrequisitos, fecha_envio, estado_revision)
VALUES (v_ultimo.nombres, v_ultimo.apellidos, v_ultimo.identificacion, v_ultimo.correo, p_url_cedula, p_url_foto, p_url_prerrequisitos, NOW(), 'PENDIENTE')
    RETURNING id_prepostulacion INTO v_id_nuevo;
INSERT INTO public.prepostulacion_solicitud (id_prepostulacion, id_solicitud) VALUES (v_id_nuevo, p_id_solicitud);
RETURN QUERY SELECT v_id_nuevo;
END; $fn$;

CREATE OR REPLACE FUNCTION public.fn_verificar_integridad(p_limite integer DEFAULT 1000)
 RETURNS TABLE(id_aud_cambio bigint, fecha timestamp with time zone, tabla character varying, operacion character varying, campo character varying, usuario_app character varying, hash_guardado character varying, hash_calculado character varying, estado character varying)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public', 'extensions' AS $fn$
BEGIN
RETURN QUERY
SELECT a.id_aud_cambio, a.fecha, a.tabla::VARCHAR, a.operacion::VARCHAR, a.campo::VARCHAR, a.usuario_app::VARCHAR,
    a.hash_integridad::VARCHAR AS hash_guardado,
    encode(digest(
                   a.id_aud_cambio::TEXT || a.fecha::TEXT || a.tabla || COALESCE(a.id_registro::TEXT,'') || a.operacion || a.campo ||
            COALESCE(a.valor_antes,'') || COALESCE(a.valor_despues,'') || COALESCE(a.usuario_app,'') ||
            COALESCE(a.usuario_bd,'') || COALESCE(a.ip_cliente,''), 'sha256'), 'hex')::VARCHAR AS hash_calculado,
    CASE
        WHEN a.hash_integridad IS NULL THEN 'SIN_HASH'
        WHEN a.hash_integridad = encode(digest(
                                                a.id_aud_cambio::TEXT || a.fecha::TEXT || a.tabla || COALESCE(a.id_registro::TEXT,'') || a.operacion || a.campo ||
                COALESCE(a.valor_antes,'') || COALESCE(a.valor_despues,'') || COALESCE(a.usuario_app,'') ||
                COALESCE(a.usuario_bd,'') || COALESCE(a.ip_cliente,''), 'sha256'), 'hex') THEN 'OK'
        ELSE 'ALTERADO'
        END::VARCHAR AS estado
FROM public.aud_cambio a ORDER BY a.id_aud_cambio DESC LIMIT p_limite;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_crear_notificacion(p_id_usuario bigint, p_tipo character varying, p_titulo character varying, p_mensaje text, p_entidad_tipo character varying DEFAULT NULL::character varying, p_entidad_id bigint DEFAULT NULL::bigint)
 RETURNS bigint LANGUAGE plpgsql SECURITY DEFINER AS $fn$
DECLARE v_id BIGINT;
BEGIN
INSERT INTO notificacion (id_usuario, tipo, titulo, mensaje, entidad_tipo, entidad_id)
VALUES (p_id_usuario, COALESCE(p_tipo,'info'), p_titulo, p_mensaje, p_entidad_tipo, p_entidad_id)
    RETURNING id_notificacion INTO v_id;
RETURN v_id;
EXCEPTION WHEN OTHERS THEN RAISE WARNING 'sp_crear_notificacion error: %', SQLERRM; RETURN -1;
END; $fn$;

CREATE OR REPLACE PROCEDURE public.sp_sync_roles_usuario_bd(IN p_id_usuario integer, IN p_revocar_sobrantes boolean DEFAULT true)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $proc$
DECLARE v_usuario_bd VARCHAR; v_rol_bd VARCHAR; v_roles_deberia VARCHAR[]; v_roles_tiene VARCHAR[];
BEGIN
SELECT usuario_bd INTO v_usuario_bd FROM usuario WHERE id_usuario = p_id_usuario;
IF v_usuario_bd IS NULL THEN RAISE EXCEPTION 'Usuario no encontrado: %', p_id_usuario; END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = v_usuario_bd) THEN RAISE EXCEPTION 'Usuario BD % no existe en PostgreSQL', v_usuario_bd; END IF;
SELECT ARRAY_AGG(DISTINCT rab.nombre_rol_bd) INTO v_roles_deberia FROM usuario_roles_app ura
                                                                           JOIN roles_app_bd rab ON rab.id_rol_app = ura.id_rol_app WHERE ura.id_usuario = p_id_usuario;
IF v_roles_deberia IS NOT NULL THEN
        FOREACH v_rol_bd IN ARRAY v_roles_deberia LOOP
            IF NOT EXISTS (SELECT 1 FROM pg_auth_members am JOIN pg_roles r ON r.oid=am.roleid JOIN pg_roles u ON u.oid=am.member
                WHERE r.rolname=v_rol_bd AND u.rolname=v_usuario_bd) THEN
                EXECUTE format('GRANT %I TO %I', v_rol_bd, v_usuario_bd);
END IF;
END LOOP;
END IF;
    IF p_revocar_sobrantes THEN
SELECT ARRAY_AGG(r.rolname) INTO v_roles_tiene FROM pg_auth_members am
                                                        JOIN pg_roles r ON r.oid=am.roleid JOIN pg_roles u ON u.oid=am.member
WHERE u.rolname=v_usuario_bd AND r.rolname LIKE 'role_%' AND r.rolcanlogin=false;
IF v_roles_tiene IS NOT NULL THEN
            FOREACH v_rol_bd IN ARRAY v_roles_tiene LOOP
                IF v_roles_deberia IS NULL OR NOT (v_rol_bd=ANY(v_roles_deberia)) THEN
                    EXECUTE format('REVOKE %I FROM %I', v_rol_bd, v_usuario_bd);
END IF;
END LOOP;
END IF;
END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_auth_members am JOIN pg_roles r ON r.oid=am.roleid JOIN pg_roles u ON u.oid=am.member
        WHERE r.rolname='base_app_permisos' AND u.rolname=v_usuario_bd) THEN
        EXECUTE format('GRANT base_app_permisos TO %I', v_usuario_bd);
END IF;
END; $proc$;

-- ============================================================
-- SPs ADICIONALES (evaluadores, matriz, decisiones, requisitos)
-- ============================================================

CREATE OR REPLACE FUNCTION public.fn_documentos_pendientes_postulante(p_id_postulante bigint, p_id_solicitud bigint)
 RETURNS TABLE(pendientes integer, tiene_documentos boolean, mensaje text)
 LANGUAGE plpgsql AS $fn$
DECLARE v_id_postulacion BIGINT; v_total_obligatorios INTEGER; v_validados INTEGER; v_subidos INTEGER;
BEGIN
SELECT pol.id_postulacion INTO v_id_postulacion FROM postulacion pol
                                                         JOIN postulante p ON pol.id_postulante = p.id_postulante
WHERE p.id_postulante = p_id_postulante LIMIT 1;
SELECT COUNT(*) INTO v_total_obligatorios FROM tipo_documento td WHERE td.obligatorio = TRUE;
IF v_id_postulacion IS NULL OR v_total_obligatorios = 0 THEN
        RETURN QUERY SELECT 0, FALSE, 'Sin postulación registrada'::TEXT; RETURN;
END IF;
SELECT COUNT(DISTINCT d.id_tipo_documento) INTO v_subidos FROM documento d WHERE d.id_postulacion = v_id_postulacion;
SELECT COUNT(DISTINCT d.id_tipo_documento) INTO v_validados FROM documento d
WHERE d.id_postulacion = v_id_postulacion AND d.estado_validacion = 'validado';
IF v_subidos = 0 THEN
        RETURN QUERY SELECT v_total_obligatorios, FALSE, 'El candidato no ha subido ningún documento'::TEXT; RETURN;
END IF;
    IF v_validados < v_total_obligatorios THEN
        RETURN QUERY SELECT (v_total_obligatorios - v_validados)::INTEGER, TRUE,
                            ('Tiene ' || v_subidos || ' documento(s) subido(s) pero ' || (v_total_obligatorios - v_validados) || ' pendiente(s) de validación')::TEXT;
RETURN;
END IF;
RETURN QUERY SELECT 0, TRUE, 'Documentos completos'::TEXT;
END; $fn$;

CREATE OR REPLACE FUNCTION public.fn_listar_decisiones_revisor(p_estado character varying DEFAULT 'pendiente'::character varying)
 RETURNS TABLE(id_decision bigint, id_solicitud bigint, nombre_materia text, nombre_ganador character varying, puntaje_final numeric, acta_comite text, estado character varying, fecha_envio timestamp without time zone)
 LANGUAGE plpgsql AS $fn$
BEGIN
RETURN QUERY
SELECT dr.id_decision, dr.id_solicitud, m.nombre_materia::TEXT, dr.nombre_ganador,
       dr.puntaje_final, dr.acta_comite, dr.estado, dr.fecha_envio
FROM decision_revisor dr
         JOIN solicitud_docente sd ON dr.id_solicitud = sd.id_solicitud
         JOIN materia m ON sd.id_materia = m.id_materia
WHERE dr.estado = p_estado ORDER BY dr.fecha_envio DESC;
END; $fn$;

CREATE OR REPLACE FUNCTION public.fn_listar_evaluadores_disponibles(p_id_proceso bigint, p_tipo character varying DEFAULT 'matriz'::character varying)
 RETURNS TABLE(v_id_usuario bigint, v_nombre_completo text, v_usuario_app character varying)
 LANGUAGE plpgsql SECURITY DEFINER AS $fn$
BEGIN
RETURN QUERY
SELECT aa.id_usuario, (aa.nombres || ' ' || aa.apellidos)::TEXT, u.usuario_app
FROM autoridad_academica aa
         JOIN usuario u ON aa.id_usuario = u.id_usuario
         JOIN usuario_roles_app ura ON u.id_usuario = ura.id_usuario
         JOIN roles_app ra ON ura.id_rol_app = ra.id_rol_app
WHERE ra.nombre = 'EVALUADOR' AND aa.estado = TRUE
  AND aa.id_usuario NOT IN (
    SELECT aa2.id_usuario FROM proceso_evaluacion pe
                                   JOIN solicitud_docente sd ON pe.id_solicitud = sd.id_solicitud
                                   JOIN autoridad_academica aa2 ON sd.id_autoridad = aa2.id_autoridad
    WHERE pe.id_proceso = p_id_proceso)
  AND aa.id_usuario NOT IN (
    SELECT pea.id_usuario FROM proceso_evaluador_asignado pea
    WHERE pea.id_proceso = p_id_proceso AND pea.tipo = p_tipo)
ORDER BY aa.apellidos, aa.nombres;
END; $fn$;

CREATE OR REPLACE FUNCTION public.fn_listar_evaluadores_proceso(p_id_proceso bigint, p_tipo character varying DEFAULT 'matriz'::character varying)
 RETURNS TABLE(v_id_usuario bigint, v_nombre_completo text, v_usuario_app character varying, v_es_dueno boolean)
 LANGUAGE plpgsql SECURITY DEFINER AS $fn$
BEGIN
RETURN QUERY
SELECT aa.id_usuario, (aa.nombres || ' ' || aa.apellidos)::TEXT, u.usuario_app, TRUE
FROM proceso_evaluacion pe
         JOIN solicitud_docente sd ON pe.id_solicitud = sd.id_solicitud
         JOIN autoridad_academica aa ON sd.id_autoridad = aa.id_autoridad
         JOIN usuario u ON aa.id_usuario = u.id_usuario
WHERE pe.id_proceso = p_id_proceso
UNION ALL
SELECT pea.id_usuario, (aa2.nombres || ' ' || aa2.apellidos)::TEXT, u2.usuario_app, FALSE
FROM proceso_evaluador_asignado pea
         JOIN autoridad_academica aa2 ON pea.id_usuario = aa2.id_usuario
         JOIN usuario u2 ON aa2.id_usuario = u2.id_usuario
WHERE pea.id_proceso = p_id_proceso AND pea.tipo = p_tipo;
END; $fn$;

CREATE OR REPLACE FUNCTION public.fn_puede_evaluar_proceso(p_id_proceso bigint, p_id_usuario bigint, p_tipo character varying DEFAULT 'matriz'::character varying)
 RETURNS boolean LANGUAGE plpgsql AS $fn$
DECLARE v_es_dueno BOOLEAN; v_es_asignado BOOLEAN;
BEGIN
SELECT EXISTS (SELECT 1 FROM proceso_evaluacion pe
                                 JOIN solicitud_docente sd ON pe.id_solicitud = sd.id_solicitud
                                 JOIN autoridad_academica aa ON sd.id_autoridad = aa.id_autoridad
               WHERE pe.id_proceso = p_id_proceso AND aa.id_usuario = p_id_usuario) INTO v_es_dueno;
IF v_es_dueno THEN RETURN TRUE; END IF;
SELECT EXISTS (SELECT 1 FROM proceso_evaluador_asignado
               WHERE id_proceso = p_id_proceso AND id_usuario = p_id_usuario AND tipo = p_tipo) INTO v_es_asignado;
RETURN v_es_asignado;
END; $fn$;

CREATE OR REPLACE FUNCTION public.fn_tiene_procesos_activos()
 RETURNS boolean LANGUAGE plpgsql AS $fn$
DECLARE v_count INTEGER;
BEGIN
SELECT COUNT(*) INTO v_count FROM proceso_evaluacion
WHERE estado_general NOT IN ('completado', 'cancelado');
RETURN v_count > 0;
END; $fn$;

CREATE OR REPLACE PROCEDURE public.sp_actualizar_requisito_prepostulacion(IN p_id_requisito bigint, IN p_nombre character varying, IN p_descripcion character varying, IN p_orden integer)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $proc$
BEGIN
UPDATE requisito_prepostulacion SET nombre=p_nombre, descripcion=p_descripcion, orden=p_orden
WHERE id_requisito = p_id_requisito;
END; $proc$;

CREATE OR REPLACE PROCEDURE public.sp_asignar_evaluador_proceso(IN p_id_proceso bigint, IN p_id_usuario bigint, IN p_tipo character varying DEFAULT 'matriz'::character varying)
 LANGUAGE plpgsql AS $proc$
BEGIN
INSERT INTO proceso_evaluador_asignado(id_proceso, id_usuario, tipo)
VALUES (p_id_proceso, p_id_usuario, p_tipo) ON CONFLICT DO NOTHING;
END; $proc$;

CREATE OR REPLACE PROCEDURE public.sp_confirmar_decision_final(IN p_id_solicitud bigint, IN p_id_proceso_ganador bigint, IN p_acta_comite text)
 LANGUAGE plpgsql AS $proc$
DECLARE v_nombre_ganador VARCHAR(300); v_puntaje_final NUMERIC(6,2);
BEGIN
SELECT p.nombres_postulante || ' ' || p.apellidos_postulante,
       pe.puntaje_matriz + COALESCE(pe.puntaje_entrevista, 0)
INTO v_nombre_ganador, v_puntaje_final
FROM proceso_evaluacion pe JOIN postulante p ON pe.id_postulante = p.id_postulante
WHERE pe.id_proceso = p_id_proceso_ganador;
UPDATE proceso_evaluacion SET bloqueado=TRUE,
                              decision_comite=CASE WHEN id_proceso=p_id_proceso_ganador THEN 'ganador' ELSE 'no_seleccionado' END,
                              acta_comite=p_acta_comite, fecha_decision_comite=NOW()
WHERE id_solicitud = p_id_solicitud;
INSERT INTO decision_revisor (id_solicitud, id_proceso_ganador, nombre_ganador, puntaje_final, acta_comite, estado)
VALUES (p_id_solicitud, p_id_proceso_ganador, v_nombre_ganador, v_puntaje_final, p_acta_comite, 'pendiente')
    ON CONFLICT DO NOTHING;
UPDATE proceso_evaluacion SET notificado_revisor=TRUE WHERE id_solicitud=p_id_solicitud;
END; $proc$;

CREATE OR REPLACE PROCEDURE public.sp_confirmar_distribucion_matriz(IN p_meritos numeric, IN p_experiencia numeric, IN p_entrevista numeric)
 LANGUAGE plpgsql AS $proc$
BEGIN
    IF p_meritos + p_experiencia + p_entrevista <> 100 THEN
        RAISE EXCEPTION 'La distribución debe sumar 100 puntos. Total actual: %', p_meritos+p_experiencia+p_entrevista;
END IF;
UPDATE matriz_seccion SET puntaje_maximo=p_meritos     WHERE tipo='meritos'    AND bloqueado=FALSE;
UPDATE matriz_seccion SET puntaje_maximo=p_experiencia WHERE tipo='experiencia' AND bloqueado=FALSE;
UPDATE matriz_seccion SET puntaje_maximo=p_entrevista  WHERE tipo='entrevista';
END; $proc$;

CREATE OR REPLACE PROCEDURE public.sp_eliminar_item_matriz(IN p_id_item bigint)
 LANGUAGE plpgsql AS $proc$
BEGIN
UPDATE matriz_item SET activo=FALSE WHERE id_item=p_id_item AND bloqueado=FALSE;
END; $proc$;

CREATE OR REPLACE PROCEDURE public.sp_eliminar_requisito_prepostulacion(IN p_id_requisito bigint)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $proc$
BEGIN
UPDATE requisito_prepostulacion SET activo=FALSE WHERE id_requisito=p_id_requisito;
END; $proc$;

CREATE OR REPLACE PROCEDURE public.sp_eliminar_seccion(IN p_id_seccion bigint)
 LANGUAGE plpgsql AS $proc$
BEGIN
UPDATE matriz_seccion SET activo=FALSE WHERE id_seccion=p_id_seccion AND bloqueado=FALSE;
END; $proc$;

CREATE OR REPLACE PROCEDURE public.sp_guardar_item_matriz(IN p_id_item bigint, IN p_id_seccion bigint, IN p_codigo character varying, IN p_label character varying, IN p_puntaje_max numeric, IN p_puntos_por character varying, IN p_orden integer)
 LANGUAGE plpgsql AS $proc$
BEGIN
    IF p_id_item IS NULL THEN
        INSERT INTO matriz_item(id_seccion,codigo,label,puntaje_maximo,puntos_por,orden)
        VALUES(p_id_seccion,p_codigo,p_label,p_puntaje_max,p_puntos_por,p_orden);
ELSE
UPDATE matriz_item SET label=p_label,puntaje_maximo=p_puntaje_max,puntos_por=p_puntos_por,orden=p_orden
WHERE id_item=p_id_item AND bloqueado=FALSE;
END IF;
END; $proc$;

CREATE OR REPLACE PROCEDURE public.sp_guardar_seccion(IN p_id_seccion bigint, IN p_codigo character varying, IN p_titulo character varying, IN p_descripcion text, IN p_puntaje_max numeric, IN p_orden integer, IN p_tipo character varying)
 LANGUAGE plpgsql AS $proc$
BEGIN
    IF p_id_seccion IS NULL THEN
        INSERT INTO matriz_seccion(codigo,titulo,descripcion,puntaje_maximo,orden,tipo)
        VALUES(p_codigo,p_titulo,p_descripcion,p_puntaje_max,p_orden,p_tipo);
ELSE
UPDATE matriz_seccion SET titulo=p_titulo,descripcion=p_descripcion,puntaje_maximo=p_puntaje_max,orden=p_orden
WHERE id_seccion=p_id_seccion AND bloqueado=FALSE;
END IF;
END; $proc$;

CREATE OR REPLACE FUNCTION public.sp_listar_requisitos_solicitud(p_id_solicitud bigint)
 RETURNS TABLE(id_requisito bigint, nombre character varying, descripcion character varying, orden integer)
 LANGUAGE sql STABLE SECURITY DEFINER SET search_path TO 'public' AS $fn$
SELECT id_requisito, nombre, descripcion, orden FROM requisito_prepostulacion
WHERE id_solicitud=p_id_solicitud AND activo=TRUE ORDER BY orden, id_requisito;
$fn$;

CREATE OR REPLACE PROCEDURE public.sp_marcar_decision_revisada(IN p_id_decision bigint)
 LANGUAGE plpgsql AS $proc$
BEGIN
UPDATE decision_revisor SET estado='revisado', fecha_revision=NOW() WHERE id_decision=p_id_decision;
END; $proc$;

CREATE OR REPLACE PROCEDURE public.sp_quitar_evaluador_proceso(IN p_id_proceso bigint, IN p_id_usuario bigint, IN p_tipo character varying DEFAULT 'matriz'::character varying)
 LANGUAGE plpgsql AS $proc$
BEGIN
DELETE FROM proceso_evaluador_asignado
WHERE id_proceso=p_id_proceso AND id_usuario=p_id_usuario AND tipo=p_tipo;
END; $proc$;

CREATE OR REPLACE FUNCTION public.sp_agregar_documento_prepostulacion(p_id_prepostulacion bigint, p_descripcion character varying, p_url_documento character varying, p_id_requisito bigint DEFAULT NULL::bigint)
 RETURNS TABLE(out_id_documento bigint)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $fn$
BEGIN
RETURN QUERY
    INSERT INTO prepostulacion_documentos(id_prepostulacion,descripcion,url_documento,id_requisito)
    VALUES(p_id_prepostulacion,p_descripcion,p_url_documento,p_id_requisito)
    RETURNING id_documento;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_obtener_info_postulante_por_postulacion(p_id_usuario bigint, p_id_postulacion bigint)
 RETURNS TABLE(id_postulante bigint, nombres character varying, apellidos character varying, identificacion character varying, correo character varying, id_postulacion bigint, estado_postulacion character varying, nombre_materia character varying, nombre_carrera character varying, nombre_area character varying, fecha_limite_documentos date, documentos_abiertos boolean)
 LANGUAGE plpgsql AS $fn$
BEGIN
RETURN QUERY
SELECT pt.id_postulante, pt.nombres_postulante::VARCHAR, pt.apellidos_postulante::VARCHAR,
    pt.identificacion::VARCHAR, pt.correo_postulante::VARCHAR,
    po.id_postulacion, po.estado_postulacion::VARCHAR,
    m.nombre_materia::VARCHAR, ca.nombre_carrera::VARCHAR, a.nombre_area::VARCHAR,
    c.fecha_limite_documentos,
    CASE WHEN c.fecha_limite_documentos IS NOT NULL THEN CURRENT_DATE <= c.fecha_limite_documentos
         ELSE CURRENT_DATE <= c.fecha_fin END AS documentos_abiertos
FROM postulante pt
         JOIN postulacion po      ON po.id_postulante = pt.id_postulante
         JOIN solicitud_docente sd ON sd.id_solicitud = po.id_solicitud
         JOIN convocatoria_solicitud cs ON cs.id_solicitud = sd.id_solicitud
         JOIN convocatoria c      ON c.id_convocatoria = cs.id_convocatoria
         LEFT JOIN materia m      ON m.id_materia = sd.id_materia
         LEFT JOIN carrera ca     ON ca.id_carrera = sd.id_carrera
         LEFT JOIN area_conocimiento a ON a.id_area = sd.id_area
WHERE pt.id_usuario = p_id_usuario AND po.id_postulacion = p_id_postulacion
    LIMIT 1;
END; $fn$;

-- ============================================================
-- SPs FINALES (admin, reportes, sesiones, entrevistas)
-- ============================================================

CREATE OR REPLACE PROCEDURE public.sp_actualizar_foto_perfil(IN p_usuario_app character varying, IN p_foto_url text)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $proc$
BEGIN
UPDATE public.usuario SET foto_perfil_url=p_foto_url WHERE usuario_app=p_usuario_app;
END; $proc$;

CREATE OR REPLACE FUNCTION public.sp_actualizar_rol_app(p_id_rol_app integer, p_nombre character varying, p_descripcion text, p_roles_bd character varying[])
 RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $fn$
DECLARE v_rol_bd VARCHAR;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM roles_app WHERE id_rol_app=p_id_rol_app) THEN
        RAISE EXCEPTION 'Rol de aplicación no encontrado: %', p_id_rol_app;
END IF;
UPDATE roles_app SET nombre=p_nombre, descripcion=p_descripcion WHERE id_rol_app=p_id_rol_app;
DELETE FROM roles_app_bd WHERE id_rol_app=p_id_rol_app;
IF p_roles_bd IS NOT NULL THEN
        FOREACH v_rol_bd IN ARRAY p_roles_bd LOOP
            PERFORM sp_asignar_rol_bd_a_rol_app(p_nombre, v_rol_bd);
END LOOP;
END IF;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_agregar_requisito_prepostulacion(p_id_solicitud bigint, p_nombre character varying, p_descripcion character varying DEFAULT NULL::character varying, p_orden integer DEFAULT 0)
 RETURNS bigint LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $fn$
DECLARE v_id BIGINT;
BEGIN
INSERT INTO requisito_prepostulacion(id_solicitud,nombre,descripcion,orden)
VALUES(p_id_solicitud,p_nombre,p_descripcion,p_orden) RETURNING id_requisito INTO v_id;
RETURN v_id;
END; $fn$;

CREATE OR REPLACE PROCEDURE public.sp_asignar_opcion_a_rol(IN p_id_rol_app integer, IN p_id_opcion integer, IN p_solo_lectura boolean DEFAULT false)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $proc$
BEGIN
INSERT INTO rol_app_opcion(id_rol_app,id_opcion,solo_lectura) VALUES(p_id_rol_app,p_id_opcion,p_solo_lectura)
    ON CONFLICT(id_rol_app,id_opcion) DO UPDATE SET solo_lectura=EXCLUDED.solo_lectura;
END; $proc$;

CREATE OR REPLACE FUNCTION public.sp_asignar_rol_app_a_usuario(p_id_usuario bigint, p_nombre_rol_app character varying)
 RETURNS void LANGUAGE plpgsql AS $fn$
DECLARE v_id_rol_app INTEGER;
BEGIN
SELECT id_rol_app INTO v_id_rol_app FROM roles_app WHERE nombre=p_nombre_rol_app;
IF v_id_rol_app IS NULL THEN RAISE EXCEPTION 'Rol de aplicación no encontrado: %', p_nombre_rol_app; END IF;
INSERT INTO usuario_roles_app(id_usuario,id_rol_app) VALUES(p_id_usuario,v_id_rol_app)
    ON CONFLICT(id_usuario,id_rol_app) DO NOTHING;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_asignar_rol_bd_a_rol_app(p_nombre_rol_app character varying, p_nombre_rol_bd character varying)
 RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $fn$
DECLARE v_id_rol_app INTEGER; v_existe_rol_bd BOOLEAN;
BEGIN
    IF LOWER(p_nombre_rol_bd) NOT LIKE 'role_%' THEN
        RAISE EXCEPTION 'El rol de BD debe tener el prefijo role_. Recibido: %', p_nombre_rol_bd;
END IF;
SELECT EXISTS(SELECT 1 FROM pg_roles WHERE rolname=p_nombre_rol_bd AND rolcanlogin=false) INTO v_existe_rol_bd;
IF NOT v_existe_rol_bd THEN RAISE EXCEPTION 'El rol de BD % no existe en PostgreSQL', p_nombre_rol_bd; END IF;
SELECT id_rol_app INTO v_id_rol_app FROM roles_app WHERE nombre=p_nombre_rol_app;
IF v_id_rol_app IS NULL THEN RAISE EXCEPTION 'El rol de aplicación % no existe', p_nombre_rol_app; END IF;
INSERT INTO roles_app_bd(id_rol_app,nombre_rol_bd,fecha_creacion) VALUES(v_id_rol_app,p_nombre_rol_bd,CURRENT_TIMESTAMP)
    ON CONFLICT(id_rol_app,nombre_rol_bd) DO NOTHING;
END; $fn$;

CREATE OR REPLACE PROCEDURE public.sp_cambiar_estado_autoridad(IN p_id_autoridad bigint, IN p_estado boolean)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $proc$
BEGIN
UPDATE autoridad_academica SET estado=p_estado WHERE id_autoridad=p_id_autoridad;
IF NOT FOUND THEN RAISE EXCEPTION 'Autoridad % no encontrada', p_id_autoridad; END IF;
END; $proc$;

CREATE OR REPLACE PROCEDURE public.sp_cambiar_estado_rol_app(IN p_id_rol_app integer, IN p_activo boolean)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $proc$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM roles_app WHERE id_rol_app=p_id_rol_app) THEN
        RAISE EXCEPTION 'Rol de aplicación no encontrado: %', p_id_rol_app;
END IF;
UPDATE roles_app SET activo=p_activo WHERE id_rol_app=p_id_rol_app;
END; $proc$;

CREATE OR REPLACE PROCEDURE public.sp_cambiar_estado_usuario(IN p_id_usuario bigint, IN p_activo boolean)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $proc$
BEGIN
UPDATE usuario SET activo=p_activo WHERE id_usuario=p_id_usuario;
IF NOT FOUND THEN RAISE EXCEPTION 'Usuario % no encontrado', p_id_usuario; END IF;
END; $proc$;

CREATE OR REPLACE PROCEDURE public.sp_cerrar_sesion(IN p_usuario_app character varying, IN p_motivo_cierre character varying DEFAULT 'LOGOUT'::character varying)
 LANGUAGE plpgsql AS $proc$
BEGIN
UPDATE public.sesiones_activas_app SET activo=FALSE, fecha_cierre=NOW(), motivo_cierre=p_motivo_cierre
WHERE usuario_app=p_usuario_app AND activo=TRUE;
END; $proc$;

CREATE OR REPLACE FUNCTION public.sp_crear_opcion(p_nombre_modulo character varying, p_nombre character varying, p_descripcion text, p_ruta character varying, p_orden integer DEFAULT 0)
 RETURNS integer LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $fn$
DECLARE v_id_modulo INTEGER; v_id_opcion INTEGER;
BEGIN
SELECT id_modulo INTO v_id_modulo FROM modulo WHERE nombre=p_nombre_modulo AND activo=TRUE;
IF v_id_modulo IS NULL THEN RAISE EXCEPTION 'Módulo % no encontrado', p_nombre_modulo; END IF;
INSERT INTO opcion(id_modulo,nombre,descripcion,ruta,orden,activo)
VALUES(v_id_modulo,p_nombre,p_descripcion,p_ruta,p_orden,TRUE) RETURNING id_opcion INTO v_id_opcion;
RETURN v_id_opcion;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_crear_rol_app(p_nombre character varying, p_descripcion text, p_roles_bd character varying[])
 RETURNS integer LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $fn$
DECLARE v_id_rol_app INTEGER; v_rol_bd VARCHAR;
BEGIN
INSERT INTO roles_app(nombre,descripcion,activo,fecha_creacion)
VALUES(p_nombre,p_descripcion,true,CURRENT_TIMESTAMP) RETURNING id_rol_app INTO v_id_rol_app;
IF p_roles_bd IS NOT NULL THEN
        FOREACH v_rol_bd IN ARRAY p_roles_bd LOOP
            PERFORM sp_asignar_rol_bd_a_rol_app(p_nombre,v_rol_bd);
END LOOP;
END IF;
RETURN v_id_rol_app;
END; $fn$;

CREATE OR REPLACE PROCEDURE public.sp_guardar_entrevista_docente(IN p_id_proceso bigint)
 LANGUAGE plpgsql AS $proc$
DECLARE v_puntaje NUMERIC;
BEGIN
SELECT SUM(fp.calificacion*(fe.peso/100.0)) INTO v_puntaje FROM fase_proceso fp
                                                                    JOIN fase_evaluacion fe ON fp.id_fase=fe.id_fase
WHERE fp.id_proceso=p_id_proceso AND fp.estado='completada';
UPDATE proceso_evaluacion SET puntaje_entrevista=ROUND(COALESCE(v_puntaje,0)*0.25,2)
WHERE id_proceso=p_id_proceso;
END; $proc$;

CREATE OR REPLACE PROCEDURE public.sp_guardar_matriz_meritos(IN p_id_proceso bigint, IN p_items character varying[], IN p_valores character varying[], IN p_puntaje_total double precision)
 LANGUAGE plpgsql AS $proc$
BEGIN
DELETE FROM matriz_meritos_puntaje WHERE id_proceso=p_id_proceso;
FOR i IN 1..array_length(p_items,1) LOOP
        INSERT INTO matriz_meritos_puntaje(id_proceso,item_id,valor) VALUES(p_id_proceso,p_items[i],p_valores[i]);
END LOOP;
UPDATE proceso_evaluacion SET puntaje_matriz=p_puntaje_total WHERE id_proceso=p_id_proceso;
END; $proc$;

CREATE OR REPLACE PROCEDURE public.sp_habilitar_entrevista(IN p_id_proceso bigint, IN p_justificacion text)
 LANGUAGE plpgsql AS $proc$
BEGIN
UPDATE proceso_evaluacion SET habilitado_entrevista=TRUE, justificacion_habilitacion=p_justificacion
WHERE id_proceso=p_id_proceso;
END; $proc$;

CREATE OR REPLACE PROCEDURE public.sp_invalidar_token_usuario(IN p_usuario_app character varying)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $proc$
BEGIN
UPDATE usuario SET token_version=token_version+1 WHERE usuario_app=p_usuario_app;
IF NOT FOUND THEN RAISE EXCEPTION 'Usuario % no encontrado', p_usuario_app; END IF;
END; $proc$;

CREATE OR REPLACE FUNCTION public.sp_listar_modulos()
 RETURNS TABLE(id_modulo integer, nombre character varying, descripcion text, ruta character varying)
 LANGUAGE plpgsql AS $fn$
BEGIN
RETURN QUERY SELECT m.id_modulo,m.nombre::VARCHAR,m.descripcion,m.ruta::VARCHAR
    FROM modulo m WHERE m.activo=TRUE ORDER BY m.orden;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_listar_postulantes_evaluador()
 RETURNS TABLE(id_postulacion bigint, id_postulante bigint, identificacion character varying, nombres_postulante character varying, apellidos_postulante character varying, correo_postulante character varying, estado_postulacion character varying, nombre_materia character varying, fecha_postulacion timestamp without time zone)
 LANGUAGE plpgsql AS $fn$
BEGIN
RETURN QUERY
SELECT p.id_postulacion, pt.id_postulante, pt.identificacion,
       pt.nombres_postulante, pt.apellidos_postulante, pt.correo_postulante,
       p.estado_postulacion, COALESCE(m.nombre_materia,'Sin materia')::VARCHAR, p.fecha
FROM postulacion p JOIN postulante pt ON pt.id_postulante=p.id_postulante
                   LEFT JOIN solicitud_docente sd ON sd.id_solicitud=p.id_solicitud
                   LEFT JOIN materia m ON m.id_materia=sd.id_materia
WHERE p.estado_postulacion IN ('pendiente','en_revision') ORDER BY p.fecha DESC;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_monitor_queries(p_orden character varying DEFAULT 'tiempo_promedio'::character varying, p_limite integer DEFAULT 20)
 RETURNS TABLE(query_texto character varying, llamadas bigint, tiempo_promedio_ms numeric, tiempo_total_ms numeric, filas_promedio numeric, porcentaje_tiempo numeric)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public', 'extensions' AS $fn$
DECLARE v_total_time NUMERIC:=1; v_disponible BOOLEAN:=FALSE;
BEGIN
BEGIN
EXECUTE 'SELECT COALESCE(SUM(total_exec_time),1) FROM extensions.pg_stat_statements' INTO v_total_time;
v_disponible:=TRUE;
EXCEPTION WHEN OTHERS THEN v_disponible:=FALSE; END;
    IF NOT v_disponible THEN
        RETURN QUERY SELECT 'EXTENSION_NO_DISPONIBLE'::VARCHAR,0::BIGINT,0::NUMERIC,0::NUMERIC,0::NUMERIC,0::NUMERIC; RETURN;
END IF;
RETURN QUERY EXECUTE
        'SELECT LEFT(query,200)::VARCHAR,calls,ROUND(mean_exec_time::NUMERIC,2),ROUND(total_exec_time::NUMERIC,2),
         ROUND((rows::NUMERIC/NULLIF(calls,0)),2),ROUND((total_exec_time/$1*100)::NUMERIC,2)
         FROM extensions.pg_stat_statements ORDER BY mean_exec_time DESC LIMIT $2'
    USING v_total_time,p_limite;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_opciones_rol_app_con_flag(p_id_rol_app integer)
 RETURNS TABLE(id_opcion integer, nombre character varying, descripcion text, ruta character varying, orden integer, asignada boolean, solo_lectura boolean)
 LANGUAGE plpgsql AS $fn$
DECLARE v_id_modulo INTEGER;
BEGIN
SELECT ra.id_modulo INTO v_id_modulo FROM roles_app ra WHERE ra.id_rol_app=p_id_rol_app;
IF v_id_modulo IS NULL THEN RAISE EXCEPTION 'El rol % no tiene módulo asignado', p_id_rol_app; END IF;
RETURN QUERY
SELECT o.id_opcion,o.nombre::VARCHAR,o.descripcion,o.ruta::VARCHAR,o.orden,
       EXISTS(SELECT 1 FROM rol_app_opcion rao WHERE rao.id_rol_app=p_id_rol_app AND rao.id_opcion=o.id_opcion),
       COALESCE((SELECT rao.solo_lectura FROM rol_app_opcion rao WHERE rao.id_rol_app=p_id_rol_app AND rao.id_opcion=o.id_opcion),FALSE)
FROM opcion o WHERE o.id_modulo=v_id_modulo ORDER BY o.orden;
END; $fn$;

CREATE OR REPLACE PROCEDURE public.sp_quitar_opcion_de_rol(IN p_id_rol_app integer, IN p_id_opcion integer)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $proc$
BEGIN
DELETE FROM rol_app_opcion WHERE id_rol_app=p_id_rol_app AND id_opcion=p_id_opcion;
END; $proc$;

CREATE OR REPLACE FUNCTION public.sp_remover_rol_bd_de_rol_app(p_nombre_rol_app character varying, p_nombre_rol_bd character varying)
 RETURNS void LANGUAGE plpgsql AS $fn$
DECLARE v_id_rol_app INTEGER;
BEGIN
SELECT id_rol_app INTO v_id_rol_app FROM roles_app WHERE nombre=p_nombre_rol_app;
IF v_id_rol_app IS NULL THEN RAISE EXCEPTION 'El rol de aplicación % no existe', p_nombre_rol_app; END IF;
DELETE FROM roles_app_bd WHERE id_rol_app=v_id_rol_app AND nombre_rol_bd=p_nombre_rol_bd;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_reporte_cambios(p_desde date DEFAULT NULL::date, p_hasta date DEFAULT NULL::date, p_tabla character varying DEFAULT NULL::character varying, p_operacion character varying DEFAULT NULL::character varying, p_usuario_app character varying DEFAULT NULL::character varying, p_limite integer DEFAULT 1000)
 RETURNS TABLE(id_aud_cambio bigint, fecha timestamp with time zone, tabla character varying, id_registro bigint, operacion character varying, campo character varying, valor_antes text, valor_despues text, usuario_app character varying, usuario_bd character varying, ip_cliente character varying)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $fn$
DECLARE v_desde TIMESTAMPTZ:=COALESCE(p_desde::TIMESTAMPTZ,NOW()-INTERVAL '30 days');
        v_hasta TIMESTAMPTZ:=COALESCE((p_hasta+INTERVAL '1 day')::TIMESTAMPTZ,NOW());
BEGIN
RETURN QUERY SELECT c.id_aud_cambio,c.fecha,c.tabla,c.id_registro,c.operacion,
        c.campo,c.valor_antes,c.valor_despues,c.usuario_app,c.usuario_bd,c.ip_cliente
    FROM public.aud_cambio c
    WHERE c.fecha BETWEEN v_desde AND v_hasta
      AND (p_tabla IS NULL OR c.tabla=p_tabla)
      AND (p_operacion IS NULL OR c.operacion=p_operacion)
      AND (p_usuario_app IS NULL OR LOWER(c.usuario_app) LIKE LOWER('%'||p_usuario_app||'%'))
    ORDER BY c.fecha DESC LIMIT p_limite;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_reporte_login(p_desde date DEFAULT NULL::date, p_hasta date DEFAULT NULL::date, p_usuario_app character varying DEFAULT NULL::character varying, p_resultado character varying DEFAULT NULL::character varying, p_limite integer DEFAULT 1000)
 RETURNS TABLE(id_aud bigint, fecha timestamp without time zone, usuario_app character varying, usuario_bd character varying, resultado character varying, motivo character varying, ip_cliente character varying, user_agent character varying)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $fn$
DECLARE v_desde TIMESTAMP:=COALESCE(p_desde::TIMESTAMP,NOW()-INTERVAL '30 days');
        v_hasta TIMESTAMP:=COALESCE((p_hasta+INTERVAL '1 day')::TIMESTAMP,NOW());
BEGIN
RETURN QUERY SELECT a.id_aud,a.fecha,a.usuario_app,a.usuario_bd,a.resultado,a.motivo,a.ip_cliente,a.user_agent
    FROM public.aud_login_app a
    WHERE a.fecha BETWEEN v_desde AND v_hasta
      AND (p_usuario_app IS NULL OR LOWER(a.usuario_app) LIKE LOWER('%'||p_usuario_app||'%'))
      AND (p_resultado IS NULL OR a.resultado=p_resultado)
    ORDER BY a.fecha DESC LIMIT p_limite;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_reporte_resumen(p_desde date DEFAULT NULL::date, p_hasta date DEFAULT NULL::date)
 RETURNS TABLE(login_total bigint, login_exitosos bigint, login_fallidos bigint, login_tasa numeric, cambios_total bigint, cambios_insert bigint, cambios_update bigint, cambios_delete bigint, top_tablas json, top_usuarios json)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $fn$
DECLARE v_desde TIMESTAMP:=COALESCE(p_desde::TIMESTAMP,NOW()-INTERVAL '30 days');
        v_hasta TIMESTAMP:=COALESCE((p_hasta+INTERVAL '1 day')::TIMESTAMP,NOW());
BEGIN
RETURN QUERY
    WITH login_kpi AS (SELECT COUNT(*) AS total,COUNT(*) FILTER(WHERE resultado='SUCCESS') AS exitosos,
        COUNT(*) FILTER(WHERE resultado='FAIL') AS fallidos FROM public.aud_login_app WHERE fecha BETWEEN v_desde AND v_hasta),
    cambio_kpi AS (SELECT COUNT(*) AS total,COUNT(*) FILTER(WHERE operacion='INSERT') AS inserts,
        COUNT(*) FILTER(WHERE operacion='UPDATE') AS updates,COUNT(*) FILTER(WHERE operacion='DELETE') AS deletes
        FROM public.aud_cambio WHERE fecha BETWEEN v_desde AND v_hasta),
    tablas AS (SELECT json_agg(t ORDER BY t.cambios DESC) AS data FROM
        (SELECT tabla,COUNT(*) AS cambios FROM public.aud_cambio WHERE fecha BETWEEN v_desde AND v_hasta
         GROUP BY tabla ORDER BY cambios DESC LIMIT 5) t),
    usuarios AS (SELECT json_agg(u ORDER BY u.cambios DESC) AS data FROM
        (SELECT COALESCE(usuario_app,'(externo)') AS usuario,COUNT(*) AS cambios FROM public.aud_cambio
         WHERE fecha BETWEEN v_desde AND v_hasta GROUP BY usuario_app ORDER BY cambios DESC LIMIT 5) u)
SELECT l.total,l.exitosos,l.fallidos,
       CASE WHEN l.total>0 THEN ROUND((l.exitosos*100.0)/l.total,1) ELSE 0 END,
       c.total,c.inserts,c.updates,c.deletes,
       COALESCE(tablas.data,'[]'::JSON),COALESCE(usuarios.data,'[]'::JSON)
FROM login_kpi l,cambio_kpi c,tablas,usuarios;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_resetear_queries()
 RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public', 'extensions' AS $fn$
BEGIN
BEGIN PERFORM extensions.pg_stat_statements_reset();
EXCEPTION WHEN OTHERS THEN RAISE NOTICE 'pg_stat_statements no disponible: %', SQLERRM; END;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_stats_cambios(p_desde date DEFAULT NULL::date, p_hasta date DEFAULT NULL::date, p_tabla character varying DEFAULT NULL::character varying, p_operacion character varying DEFAULT NULL::character varying, p_usuario_app character varying DEFAULT NULL::character varying)
 RETURNS TABLE(total_cambios bigint, total_insert bigint, total_update bigint, total_delete bigint, por_tabla json, tendencia_diaria json, top_usuarios json, campos_frecuentes json, cambios_externos json)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $fn$
DECLARE v_desde TIMESTAMPTZ:=COALESCE(p_desde::TIMESTAMPTZ,NOW()-INTERVAL '30 days');
        v_hasta TIMESTAMPTZ:=COALESCE((p_hasta+INTERVAL '1 day')::TIMESTAMPTZ,NOW());
BEGIN
RETURN QUERY
    WITH base AS (SELECT * FROM public.aud_cambio WHERE fecha BETWEEN v_desde AND v_hasta
        AND(p_tabla IS NULL OR tabla=p_tabla) AND(p_operacion IS NULL OR operacion=p_operacion)
        AND(p_usuario_app IS NULL OR LOWER(usuario_app) LIKE LOWER('%'||p_usuario_app||'%'))),
    kpis AS (SELECT COUNT(*) AS total,COUNT(*) FILTER(WHERE operacion='INSERT') AS ins,
        COUNT(*) FILTER(WHERE operacion='UPDATE') AS upd,COUNT(*) FILTER(WHERE operacion='DELETE') AS del FROM base),
    x_tabla AS (SELECT json_agg(t ORDER BY t.cambios DESC) AS data FROM
        (SELECT tabla,COUNT(*) AS cambios FROM base GROUP BY tabla ORDER BY cambios DESC) t),
    tendencia AS (SELECT json_agg(d ORDER BY d.dia) AS data FROM
        (SELECT DATE(fecha)::TEXT AS dia,COUNT(*) AS total,COUNT(*) FILTER(WHERE operacion='INSERT') AS inserts,
         COUNT(*) FILTER(WHERE operacion='UPDATE') AS updates,COUNT(*) FILTER(WHERE operacion='DELETE') AS deletes
         FROM base GROUP BY DATE(fecha) ORDER BY dia) d),
    top_usr AS (SELECT json_agg(u ORDER BY u.cambios DESC) AS data FROM
        (SELECT COALESCE(usuario_app,'(externo)') AS usuario,usuario_bd,COUNT(*) AS cambios
         FROM base GROUP BY usuario_app,usuario_bd ORDER BY cambios DESC LIMIT 10) u),
    campos AS (SELECT json_agg(c ORDER BY c.veces DESC) AS data FROM
        (SELECT campo,tabla,COUNT(*) AS veces FROM base GROUP BY campo,tabla ORDER BY veces DESC LIMIT 15) c),
    externos AS (SELECT json_agg(e ORDER BY e.cambios DESC) AS data FROM
        (SELECT tabla,COUNT(*) AS cambios FROM base WHERE usuario_app IS NULL GROUP BY tabla ORDER BY cambios DESC) e)
SELECT kpis.total,kpis.ins,kpis.upd,kpis.del,
       COALESCE(x_tabla.data,'[]'::JSON),COALESCE(tendencia.data,'[]'::JSON),
       COALESCE(top_usr.data,'[]'::JSON),COALESCE(campos.data,'[]'::JSON),COALESCE(externos.data,'[]'::JSON)
FROM kpis,x_tabla,tendencia,top_usr,campos,externos;
END; $fn$;

CREATE OR REPLACE FUNCTION public.sp_stats_login(p_desde date DEFAULT NULL::date, p_hasta date DEFAULT NULL::date, p_usuario_app character varying DEFAULT NULL::character varying, p_resultado character varying DEFAULT NULL::character varying)
 RETURNS TABLE(total_registros bigint, total_exitosos bigint, total_fallidos bigint, tasa_exito numeric, tendencia_diaria json, top_fallidos json, top_exitosos json, por_hora json)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $fn$
DECLARE v_desde TIMESTAMPTZ:=COALESCE(p_desde::TIMESTAMPTZ,NOW()-INTERVAL '30 days');
        v_hasta TIMESTAMPTZ:=COALESCE((p_hasta+INTERVAL '1 day')::TIMESTAMPTZ,NOW());
BEGIN
RETURN QUERY
    WITH base AS (SELECT * FROM public.aud_login_app WHERE fecha BETWEEN v_desde AND v_hasta
        AND(p_usuario_app IS NULL OR LOWER(usuario_app) LIKE LOWER('%'||p_usuario_app||'%'))
        AND(p_resultado IS NULL OR resultado=p_resultado)),
    kpis AS (SELECT COUNT(*) AS total_reg,COUNT(*) FILTER(WHERE resultado='SUCCESS') AS total_exit,
        COUNT(*) FILTER(WHERE resultado='FAIL') AS total_fall FROM base),
    tendencia AS (SELECT json_agg(t ORDER BY t.dia) AS data FROM
        (SELECT DATE(fecha)::TEXT AS dia,COUNT(*) AS total,COUNT(*) FILTER(WHERE resultado='SUCCESS') AS exitosos,
         COUNT(*) FILTER(WHERE resultado='FAIL') AS fallidos FROM base GROUP BY DATE(fecha) ORDER BY dia) t),
    top_fall AS (SELECT json_agg(f ORDER BY f.intentos DESC) AS data FROM
        (SELECT usuario_app AS usuario,COUNT(*) AS intentos FROM base WHERE resultado='FAIL'
         GROUP BY usuario_app ORDER BY intentos DESC LIMIT 10) f),
    top_exit AS (SELECT json_agg(e ORDER BY e.accesos DESC) AS data FROM
        (SELECT usuario_app AS usuario,COUNT(*) AS accesos FROM base WHERE resultado='SUCCESS'
         GROUP BY usuario_app ORDER BY accesos DESC LIMIT 10) e),
    horas AS (SELECT json_agg(h ORDER BY h.hora) AS data FROM
        (SELECT EXTRACT(HOUR FROM fecha)::INT AS hora,COUNT(*) AS total FROM base
         GROUP BY EXTRACT(HOUR FROM fecha) ORDER BY hora) h)
SELECT kpis.total_reg,kpis.total_exit,kpis.total_fall,
       CASE WHEN kpis.total_reg>0 THEN ROUND((kpis.total_exit*100.0)/kpis.total_reg,1) ELSE 0 END,
       COALESCE(tendencia.data,'[]'::JSON),COALESCE(top_fall.data,'[]'::JSON),
       COALESCE(top_exit.data,'[]'::JSON),COALESCE(horas.data,'[]'::JSON)
FROM kpis,tendencia,top_fall,top_exit,horas;
END; $fn$;