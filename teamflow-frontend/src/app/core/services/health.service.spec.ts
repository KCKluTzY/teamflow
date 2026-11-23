import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ApiHealth, HealthService } from './health.service';

describe('HealthService', () => {
  let service: HealthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(HealthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('returns UP when the actuator reports UP', () => {
    let result: ApiHealth | undefined;
    service.check().subscribe((status) => (result = status));

    http.expectOne('/actuator/health').flush({ status: 'UP' });

    expect(result).toBe('UP');
  });

  it('returns DOWN when the actuator answers 503', () => {
    let result: ApiHealth | undefined;
    service.check().subscribe((status) => (result = status));

    http
      .expectOne('/actuator/health')
      .flush({ status: 'DOWN' }, { status: 503, statusText: 'Service Unavailable' });

    expect(result).toBe('DOWN');
  });

  it('returns UNREACHABLE when the request fails', () => {
    let result: ApiHealth | undefined;
    service.check().subscribe((status) => (result = status));

    http.expectOne('/actuator/health').error(new ProgressEvent('error'));

    expect(result).toBe('UNREACHABLE');
  });
});