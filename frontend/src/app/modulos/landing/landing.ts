import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LogoService } from '../../services/logo.service';
import { AsyncPipe } from '@angular/common';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, AsyncPipe],
  templateUrl: './landing.html',
  styleUrls: ['./landing.scss']
})
export class LandingComponent {
  constructor(private router: Router,
              public logoService: LogoService  ) {}

  irAConvocatorias(): void { this.router.navigate(['/convocatorias']); }
  irALogin():         void { this.router.navigate(['/login']); }
  onLogoError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'imgs/logo-uteq.png';
  }
}

