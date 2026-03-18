# SSDC — Sistema de Selección Docente y Convocatorias

> Plataforma web para la automatización del proceso de selección de docentes no titulares en la Universidad Técnica Estatal de Quevedo (UTEQ)

---

## Descripción

El **SSDC** es una aplicación web institucional desarrollada como proyecto de aula para la asignatura **Aplicaciones Web** de la Carrera de Ingeniería en Software — UTEQ. El sistema centraliza y automatiza el ciclo completo del proceso de selección docente, desde el registro inicial de candidatos externos hasta la generación de reportes y la auditoría del proceso.

---

## Repositorios

| Repositorio | Descripción |
|---|---|
| [`aldairHub/CHRSweb`](https://github.com/aldairHub/CHRSweb) | Repositorio de desarrollo activo (commits por sprint) |
| [`ereinosov/CHRS`](https://github.com/ereinosov/CHRS) | Versión estable consolidada |

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Frontend | Angular 17 (standalone components, RxJS, signals, lazy loading) |
| Backend | Spring Boot 3.2 / Java 17 LTS |
| Base de datos | PostgreSQL 18.1 (dev: Supabase, prod: servidor local UTEQ) |
| Almacenamiento | Supabase Storage (PDFs e imágenes de portada) |
| IA — texto | Groq API (generación de descripciones de convocatorias) |
| IA — imagen | HuggingFace / FLUX.1-schnell (imágenes de portada) |
| Seguridad | JWT + BCrypt + AES-256 + roles nativos PostgreSQL |
| Notificaciones | JavaMailSender + SMTP configurable |
| Respaldos | pg_dump automatizado |
| Build | Maven |

---

## Módulos funcionales (10)

| # | Módulo | Actor principal |
|---|---|---|
| 1 | Prepostulación | Candidato externo |
| 2 | Gestión de solicitudes docentes | Vicerrectorado (REVISOR) |
| 3 | Gestión de convocatorias | Vicerrectorado (REVISOR) |
| 4 | Módulo del postulante | Postulante |
| 5 | Evaluación de méritos | Evaluador académico |
| 6 | Entrevistas y clases demostrativas | Evaluador académico |
| 7 | Reportes y notificaciones | Vicerrectorado / Evaluador |
| 8 | Auditoría (SHA-256) | Administrador |
| 9 | Respaldos (pg_dump) | Administrador |
| 10 | Administración del sistema | Administrador |

---

## Roles del sistema

| Rol | Descripción |
|---|---|
| `ADMINISTRADOR` | Gestión técnica completa: usuarios, roles, facultades, respaldos, configuración institucional |
| `REVISOR` | Vicerrectorado Académico: solicitudes docentes, convocatorias, prepostulaciones |
| `EVALUADOR` | Evaluador académico: Coordinadores de Carrera, Decanos y Subdecanos |
| `POSTULANTE` | Candidato aprobado: carga de documentos y seguimiento de proceso |

---

## Arquitectura de seguridad

El sistema implementa seguridad en **4 capas independientes**:

1. **JWT con claims extendidos** — `usuario_bd` y `token_version` para invalidación inmediata al desactivar cuentas
2. **Pool HikariCP dinámico por usuario** — cada usuario se conecta a PostgreSQL con su propio rol nativo y permisos granulares
3. **AuditContextInterceptor** — propaga el identificador del usuario autenticado como variable de sesión PostgreSQL (`set_config()`), leída por triggers de auditoría
4. **Hash de integridad SHA-256** — cada registro de cambio campo a campo en entidades críticas incluye un hash calculado en el momento del cambio, detectable si se manipula posteriormente

Las credenciales de base de datos por usuario y las credenciales SMTP se cifran con **AES-256**.

---

## Inteligencia artificial implementada

| Funcionalidad | Tecnología | Supervisión humana |
|---|---|---|
| Generación de descripciones de convocatorias | Groq API | Revisión y aprobación obligatoria antes de publicar |
| Generación de imagen de portada | FLUX.1-schnell (HuggingFace) | Revisión, regeneración o sustitución manual disponible |

> **Nota:** La detección asistida de inconsistencias documentales (RF-019) está especificada como trabajo futuro.

---

## Cumplimiento de requisitos

- **Requisitos funcionales:** 29/30 implementados (96,7%) — 1 en trabajo futuro (RF-019)
- **Requisitos no funcionales:** 35/35 (100%)
- **Normativa:** Alineado con Arts. 15–16 de la *Normativa para la Selección y Contratación de Docentes No Titulares de la UTEQ* y con la **LOPDP** (Ley Orgánica de Protección de Datos Personales del Ecuador)

---

## Metodología de desarrollo

**Scrum** — 8 sprints × 2 semanas (24 nov 2025 – 15 mar 2026)

| Sprint | Periodo | Objetivo principal |
|---|---|---|
| 1 | 24/11 – 07/12/2025 | Arquitectura, BD PostgreSQL, entorno de desarrollo |
| 2 | 08/12 – 21/12/2025 | JWT + RBAC + doble capa seguridad + gestión usuarios/roles |
| 3 | 22/12/2025 – 04/01/2026 | Solicitudes docentes, convocatorias con IA, prepostulación |
| 4 | 05/01 – 18/01/2026 | Carga de documentos, validación documental, evaluación de méritos |
| 5 | 19/01 – 01/02/2026 | Entrevistas y clases demostrativas, rúbricas |
| 6 | 02/02 – 15/02/2026 | Reportes PDF/Excel, notificaciones SMTP, respaldos, configuración |
| 7 | 16/02 – 01/03/2026 | Pruebas funcionales, UAT, SUS, auditoría |
| 8 | 02/03 – 15/03/2026 | Estabilización, lazy loading, documentación, producción |

---

## Requisitos previos

- Java 17 LTS
- Node.js 18+
- Angular CLI 17
- PostgreSQL 18.1
- Maven 3.9+
- Cuenta en Supabase (desarrollo) o PostgreSQL local (producción)
- API Key de Groq
- API Key de HuggingFace

---

## Configuración

### Backend (`application.properties`)

```properties
# Base de datos
spring.datasource.url=jdbc:postgresql://<HOST>:<PORT>/<DB>
spring.datasource.username=<USER>
spring.datasource.password=<PASSWORD>

# JWT
jwt.secret=<SECRET_KEY>
jwt.expiration=28800000

# Groq API
groq.api.key=<GROQ_API_KEY>

# HuggingFace
huggingface.api.key=<HF_API_KEY>
huggingface.model=black-forest-labs/FLUX.1-schnell

# Supabase Storage
supabase.url=<SUPABASE_URL>
supabase.key=<SUPABASE_KEY>
supabase.bucket=<BUCKET_NAME>

# AES-256 (cifrado de credenciales)
encryption.secret=<AES_SECRET_KEY>
```

### Frontend (`environment.ts`)

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

---

## Ejecución

### Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
ng serve
```

La aplicación estará disponible en `http://localhost:4200`.

---

## Equipo de desarrollo

| Nombre | Rol |
|---|---|
| Calderón Saltos Joseph | Developer |
| Herrera Barco Humberto | Developer |
| Reinoso Vélez Eduardo | Developer |
| Silva Triviño John | Developer |

**Docente:** Ing. Gleiston Cicerón Guerrero Ulloa  
**Institución:** Universidad Técnica Estatal de Quevedo — Facultad de Ciencias de la Computación  
**Asignatura:** Aplicaciones Web 'A' — Marzo 2026
