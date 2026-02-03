import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NavbarComponent } from '../../component/navbar';
import { FooterComponent } from '../../component/footer.component';


@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule,
    NavbarComponent,
    FooterComponent,
  ],

  templateUrl: './admin.html',
  styleUrls: ['./admin.scss']
})
export class AdminComponent {

  constructor(private router: Router) {}

  navegarA(ruta: string): void {
    this.router.navigateByUrl('/' + ruta);
  }

}
