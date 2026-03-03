import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {Router, RouterOutlet} from '@angular/router';
import { NavbarComponent } from '../../component/navbar';
import { FooterComponent } from '../../component/footer.component';

@Component({
  selector: 'app-postulante',
  standalone: true,
  imports: [
    CommonModule,
    NavbarComponent,
    FooterComponent,
  ],
  templateUrl: './postulante.html',
  styleUrls: ['./postulante.scss']
})
export class PostulanteComponent {

  constructor(private router: Router) {}

  navegarA(ruta: string): void {
    this.router.navigate([`/postulante/${ruta}`]);
  }

}
