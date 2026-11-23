import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { ApiStatusCard } from './shared/api-status-card/api-status-card';

@Component({
  imports: [RouterOutlet, ApiStatusCard],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('TeamFlow');
}