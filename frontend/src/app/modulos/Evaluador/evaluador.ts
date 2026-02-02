import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../component/navbar';
import { FooterComponent } from '../../component/footer.component';

@Component({
  selector: 'app-evaluador',
  standalone: true,
  imports: [CommonModule, NavbarComponent, FooterComponent],
  templateUrl: './evaluador.html',
  styleUrls: ['./evaluador.scss']
})
export class EvaluadorComponent {

  constructor(private router: Router) {}

  navegarA(ruta: string): void {
    this.router.navigate([`/evaluador/${ruta}`]);
  }
}
