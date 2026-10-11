import {
  AnnouncementStatus,
  EventCategory,
  EventStatus,
  MediaCategory,
  MediaStatus,
  MosqueRole,
  Visibility,
} from '../api/models';

/** Nederlandse namen voor de waarden uit de API. Dezelfde woorden als in de app. */
export const EVENT_CATEGORY_LABELS: Record<EventCategory, string> = {
  LECTURE: 'Lezing',
  FRIDAY_PROGRAM: 'Vrijdagprogramma',
  CLASS: 'Les',
  YOUTH: 'Jongeren',
  COMMUNITY: 'Gemeenschap',
  OTHER: 'Overig',
};

export const MEDIA_CATEGORY_LABELS: Record<MediaCategory, string> = {
  LECTURE: 'Lezing',
  KHUTBAH: 'Khutbah',
  QURAN: 'Koran',
  COURSE: 'Cursus',
  YOUTH: 'Jongeren',
  OTHER: 'Overig',
};

export const VISIBILITY_LABELS: Record<Visibility, string> = {
  PUBLIC: 'Iedereen',
  REGISTERED_USERS: 'Alleen ingelogde gebruikers',
};

export const EVENT_STATUS_LABELS: Record<EventStatus, string> = {
  DRAFT: 'Concept',
  PUBLISHED: 'Gepubliceerd',
  CANCELLED: 'Geannuleerd',
};

export const CONTENT_STATUS_LABELS: Record<MediaStatus | AnnouncementStatus, string> = {
  DRAFT: 'Concept',
  PUBLISHED: 'Gepubliceerd',
};

export const MOSQUE_ROLE_LABELS: Record<MosqueRole, string> = {
  ADMIN: 'Beheerder',
  EDITOR: 'Redacteur',
};

export const MOSQUE_ROLE_DESCRIPTIONS: Record<MosqueRole, string> = {
  ADMIN: 'Alles: content, moskeegegevens, beheerders en aanmeldingen',
  EDITOR: 'Alleen content: agenda, media en aankondigingen',
};

export function options<T extends string>(
  labels: Record<T, string>,
): { value: T; label: string }[] {
  return (Object.keys(labels) as T[]).map((value) => ({ value, label: labels[value] }));
}
