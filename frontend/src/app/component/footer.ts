import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule],
  // CORRECCIÓN: Apuntar a 'footer.html'
  templateUrl: './footer.html',
  styleUrls: ['./footer.scss']
})
export class FooterComponent {}
