import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
})
export class App {
  constructor(
    public readonly auth: AuthService,
    private readonly router: Router,
  ) {}

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}
