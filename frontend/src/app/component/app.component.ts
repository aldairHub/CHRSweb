import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './navbar';   // IMPORTAR
import { FooterComponent } from './footer';   // IMPORTAR
import { ToastComponent } from './toast.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterOutlet,
    NavbarComponent,
    FooterComponent,
    ToastComponent
  ],
  templateUrl: './app.component.html'
})
export class AppComponent {
  title = 'Portal UTEQ';
}
