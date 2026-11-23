import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ApiStatusCard } from './api-status-card';

describe('ApiStatusCard', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiStatusCard],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('shows a checking state while the request is pending', () => {
    const fixture = TestBed.createComponent(ApiStatusCard);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Vérification');
  });

  it('shows the online badge when the API is UP', async () => {
    const fixture = TestBed.createComponent(ApiStatusCard);
    TestBed.inject(HttpTestingController).expectOne('/actuator/health').flush({ status: 'UP' });
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('En ligne');
  });
});