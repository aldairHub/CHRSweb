import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-test-conexion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './test-conexion.html',
  styleUrls: ['./test-conexion.scss']
})
export class TestConexionComponent {
  respuesta: string = '';
  nombre: string = '';
  mensaje: string = '';

  private apiUrl = 'http://localhost:8080/api/test';

  constructor(private http: HttpClient) {}

  probarConexion(): void {
    this.http.get<any>(`${this.apiUrl}/connection`).subscribe({
      next: (data: any) => {
        this.respuesta = JSON.stringify(data);
        console.log('Éxito:', data);
      },
      error: (error: any) => {
        this.respuesta = 'Error en la conexión';
        console.error('Error:', error);
      }
    });
  }

  saludar(): void {
    this.http.get<any>(`${this.apiUrl}/saludar/${this.nombre}`).subscribe({
      next: (data: any) => this.respuesta = data,
      error: (error: any) => console.error(error)
    });
  }

  enviarMensaje(): void {
    this.http.post<any>(`${this.apiUrl}/mensaje`, { mensaje: this.mensaje }).subscribe({
      next: (data: any) => this.respuesta = data,
      error: (error: any) => console.error(error)
    });
  }
}
