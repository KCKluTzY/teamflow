import { HttpClient, HttpErrorResponse } from "@angular/common/http";
import { Injectable, inject } from "@angular/core";
import { Observable, catchError, map, of } from "rxjs";

export type ApiHealth = 'UP' | 'DOWN' | 'UNREACHABLE';

interface HealthResponse {
    status: string;
}

@Injectable({providedIn: 'root'})
export class HealthService {
    private readonly http = inject(HttpClient);

    check(): Observable<ApiHealth>{
        return this.http.get<HealthResponse>('/actuator/health').pipe(
            map((response): ApiHealth => (response.status === 'UP' ? 'UP' : 'DOWN')),
            catchError((error: HttpErrorResponse) =>
            of<ApiHealth>(error.status === 503 ? 'DOWN' : 'UNREACHABLE'),
            ),
        );
    }
}