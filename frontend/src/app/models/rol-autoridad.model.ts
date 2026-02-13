export interface RolUsuarioDto {
  idRolUsuario: number;
  nombre: string;
}

export interface RolAutoridadDto {
  idRolAutoridad: number;
  nombre: string;
  rolesUsuario: RolUsuarioDto[];
}

export interface RolAutoridadSavePayload {
  nombre: string;
  rolesUsuarioIds: number[];
}
