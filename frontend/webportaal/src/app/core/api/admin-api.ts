import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { APP_CONFIG } from '../config/app-config';
import {
  AdminAnnouncement,
  AdminEvent,
  AdminMedia,
  AdminMosque,
  Administrator,
  AnnouncementRequest,
  AnnouncementStatus,
  CreateUserRequest,
  CurrentUser,
  EventRequest,
  EventStatus,
  MediaRequest,
  MediaStatus,
  MosqueDetailsRequest,
  MosqueRole,
  Page,
  PushAudience,
  PushContentType,
  PushNotification,
  PushText,
  Registrant,
} from './models';

const PUSH_PATHS: Record<PushContentType, string> = {
  ANNOUNCEMENT: 'announcements',
  EVENT: 'events',
  MEDIA: 'media',
};

type Query = Record<string, string | number | null | undefined>;

function params(query: Query): HttpParams {
  let result = new HttpParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== null && value !== undefined && value !== '') result = result.set(key, value);
  }
  return result;
}

export interface EventQuery {
  status?: EventStatus | null;
  from?: string | null;
  page?: number;
  size?: number;
}

export interface MediaQuery {
  status?: MediaStatus | null;
  q?: string | null;
  page?: number;
  size?: number;
}

export interface AnnouncementQuery {
  status?: AnnouncementStatus | null;
  page?: number;
  size?: number;
}

/** Alle beheeraanroepen. Rechten worden door de backend afgedwongen; het portaal toont alleen wat mag. */
@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(APP_CONFIG).apiBaseUrl}/api/v1/admin`;

  private mosque(mosqueId: string, path = ''): string {
    return `${this.base}/mosques/${encodeURIComponent(mosqueId)}${path}`;
  }

  // ---------- moskee

  /** Alleen platformbeheerder. De slug is dezelfde als die van de app (brands/<slug>/brand.json). */
  createMosque(slug: string, details: MosqueDetailsRequest): Observable<AdminMosque> {
    return this.http.post<AdminMosque>(`${this.base}/mosques`, { slug, details });
  }

  /** Alleen platformbeheerder. Inactief verbergt de moskee en al haar content in de app; er wordt niets gewist. */
  setMosqueActive(mosqueId: string, active: boolean): Observable<AdminMosque> {
    return this.http.put<AdminMosque>(this.mosque(mosqueId, '/active'), { active });
  }

  updateMosque(mosqueId: string, request: MosqueDetailsRequest): Observable<AdminMosque> {
    return this.http.put<AdminMosque>(this.mosque(mosqueId), request);
  }

  // ---------- agenda

  events(mosqueId: string, query: EventQuery = {}): Observable<Page<AdminEvent>> {
    return this.http.get<Page<AdminEvent>>(this.mosque(mosqueId, '/events'), {
      params: params({ ...query }),
    });
  }

  event(mosqueId: string, eventId: string): Observable<AdminEvent> {
    return this.http.get<AdminEvent>(this.mosque(mosqueId, `/events/${eventId}`));
  }

  createEvent(mosqueId: string, request: EventRequest): Observable<AdminEvent> {
    return this.http.post<AdminEvent>(this.mosque(mosqueId, '/events'), request);
  }

  updateEvent(mosqueId: string, eventId: string, request: EventRequest): Observable<AdminEvent> {
    return this.http.put<AdminEvent>(this.mosque(mosqueId, `/events/${eventId}`), request);
  }

  eventAction(
    mosqueId: string,
    eventId: string,
    action: 'publish' | 'unpublish' | 'cancel',
  ): Observable<AdminEvent> {
    return this.http.post<AdminEvent>(this.mosque(mosqueId, `/events/${eventId}/${action}`), null);
  }

  deleteEvent(mosqueId: string, eventId: string): Observable<void> {
    return this.http.delete<void>(this.mosque(mosqueId, `/events/${eventId}`));
  }

  registrations(mosqueId: string, eventId: string): Observable<Registrant[]> {
    return this.http.get<Registrant[]>(this.mosque(mosqueId, `/events/${eventId}/registrations`));
  }

  // ---------- media

  mediaList(mosqueId: string, query: MediaQuery = {}): Observable<Page<AdminMedia>> {
    return this.http.get<Page<AdminMedia>>(this.mosque(mosqueId, '/media'), {
      params: params({ ...query }),
    });
  }

  media(mosqueId: string, mediaId: string): Observable<AdminMedia> {
    return this.http.get<AdminMedia>(this.mosque(mosqueId, `/media/${mediaId}`));
  }

  createMedia(mosqueId: string, request: MediaRequest): Observable<AdminMedia> {
    return this.http.post<AdminMedia>(this.mosque(mosqueId, '/media'), request);
  }

  updateMedia(mosqueId: string, mediaId: string, request: MediaRequest): Observable<AdminMedia> {
    return this.http.put<AdminMedia>(this.mosque(mosqueId, `/media/${mediaId}`), request);
  }

  mediaAction(
    mosqueId: string,
    mediaId: string,
    action: 'publish' | 'unpublish',
  ): Observable<AdminMedia> {
    return this.http.post<AdminMedia>(this.mosque(mosqueId, `/media/${mediaId}/${action}`), null);
  }

  deleteMedia(mosqueId: string, mediaId: string): Observable<void> {
    return this.http.delete<void>(this.mosque(mosqueId, `/media/${mediaId}`));
  }

  // ---------- aankondigingen

  announcements(
    mosqueId: string,
    query: AnnouncementQuery = {},
  ): Observable<Page<AdminAnnouncement>> {
    return this.http.get<Page<AdminAnnouncement>>(this.mosque(mosqueId, '/announcements'), {
      params: params({ ...query }),
    });
  }

  announcement(mosqueId: string, id: string): Observable<AdminAnnouncement> {
    return this.http.get<AdminAnnouncement>(this.mosque(mosqueId, `/announcements/${id}`));
  }

  createAnnouncement(
    mosqueId: string,
    request: AnnouncementRequest,
  ): Observable<AdminAnnouncement> {
    return this.http.post<AdminAnnouncement>(this.mosque(mosqueId, '/announcements'), request);
  }

  updateAnnouncement(
    mosqueId: string,
    id: string,
    request: AnnouncementRequest,
  ): Observable<AdminAnnouncement> {
    return this.http.put<AdminAnnouncement>(this.mosque(mosqueId, `/announcements/${id}`), request);
  }

  announcementAction(
    mosqueId: string,
    id: string,
    action: 'publish' | 'unpublish',
  ): Observable<AdminAnnouncement> {
    return this.http.post<AdminAnnouncement>(
      this.mosque(mosqueId, `/announcements/${id}/${action}`),
      null,
    );
  }

  deleteAnnouncement(mosqueId: string, id: string): Observable<void> {
    return this.http.delete<void>(this.mosque(mosqueId, `/announcements/${id}`));
  }

  // ---------- beheerders van de moskee

  administrators(mosqueId: string): Observable<Administrator[]> {
    return this.http.get<Administrator[]>(this.mosque(mosqueId, '/administrators'));
  }

  addAdministrator(mosqueId: string, email: string, role: MosqueRole): Observable<Administrator> {
    return this.http.post<Administrator>(this.mosque(mosqueId, '/administrators'), { email, role });
  }

  changeAdministratorRole(
    mosqueId: string,
    administratorId: string,
    role: MosqueRole,
  ): Observable<Administrator> {
    return this.http.put<Administrator>(
      this.mosque(mosqueId, `/administrators/${administratorId}`),
      { role },
    );
  }

  removeAdministrator(mosqueId: string, administratorId: string): Observable<void> {
    return this.http.delete<void>(this.mosque(mosqueId, `/administrators/${administratorId}`));
  }

  // ---------- accounts (platformbeheer)

  users(page = 0, size = 20): Observable<Page<CurrentUser>> {
    return this.http.get<Page<CurrentUser>>(`${this.base}/users`, {
      params: params({ page, size }),
    });
  }

  createUser(request: CreateUserRequest): Observable<CurrentUser> {
    return this.http.post<CurrentUser>(`${this.base}/users`, request);
  }

  setUserBlocked(userId: string, blocked: boolean): Observable<CurrentUser> {
    return this.http.post<CurrentUser>(
      `${this.base}/users/${userId}/${blocked ? 'block' : 'unblock'}`,
      null,
    );
  }

  /** Verstuurt de pushmelding over een gepubliceerd item; los van publiceren (alleen ADMIN). */
  sendPush(
    mosqueId: string,
    contentType: PushContentType,
    contentId: string,
    text: PushText,
  ): Observable<PushNotification> {
    return this.http.post<PushNotification>(
      this.mosque(mosqueId, `/${PUSH_PATHS[contentType]}/${contentId}/push`),
      text,
    );
  }

  /** De pushmelding over één item (hooguit één). */
  pushFor(
    mosqueId: string,
    contentType: PushContentType,
    contentId: string,
  ): Observable<Page<PushNotification>> {
    return this.http.get<Page<PushNotification>>(this.mosque(mosqueId, '/notifications'), {
      params: params({ contentType, contentId }),
    });
  }

  pushAudience(mosqueId: string, contentType: PushContentType): Observable<PushAudience> {
    return this.http.get<PushAudience>(this.mosque(mosqueId, '/notifications/audience'), {
      params: params({ contentType }),
    });
  }
}
