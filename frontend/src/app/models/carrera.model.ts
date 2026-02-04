export interface Carrera {
  id: number;

  // Estos campos son para enviar datos al Backend (Spring Boot)
  idFacultad?: number;
  nombreCarrera?: string;

  // Estos campos son para mostrar datos en la Tabla (vienen del GET)
  facultad?: string | any;
  nombre?: string;

  modalidad: string;
  estado: boolean;
}
