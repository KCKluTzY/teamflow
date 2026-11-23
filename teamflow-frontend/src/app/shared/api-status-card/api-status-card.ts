import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';

import { HealthService } from '../../core/services/health.service';

@Component({
  selector: 'app-api-status-card',
  imports: [MatCardModule],
  templateUrl: './api-status-card.html',
  styleUrl: './api-status-card.scss',
})
export class ApiStatusCard {
  private readonly health = inject(HealthService);

  protected readonly status = toSignal(this.health.check());
}