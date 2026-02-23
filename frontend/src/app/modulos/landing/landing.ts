import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './landing.html',
  styleUrls: ['./landing.scss']
})
export class LandingComponent {
  constructor(private router: Router) {}

  irAConvocatorias(): void { this.router.navigate(['/convocatorias']); }
  irALogin():         void { this.router.navigate(['/login']); }
}
