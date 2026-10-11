/** Typen voor de beheer-API (zie de OpenAPI-documentatie van de backend). Tijden zijn ISO-8601-strings. */

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export type PlatformRole = 'PLATFORM_ADMIN' | 'USER';
export type UserStatus = 'ACTIVE' | 'BLOCKED';
export type MosqueRole = 'ADMIN' | 'EDITOR';

export interface CurrentUser {
  id: string;
  email: string;
  displayName: string;
  platformRole: PlatformRole;
  status: UserStatus;
  createdAt: string;
}

export interface TokenResponse {
  tokenType: string;
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
}

export interface Address {
  street: string | null;
  postalCode: string | null;
  city: string | null;
  countryCode: string | null;
}

export interface Contact {
  email: string | null;
  phone: string | null;
  websiteUrl: string | null;
}

export interface AdminMosque {
  id: string;
  slug: string;
  name: string;
  description: string | null;
  address: Address;
  contact: Contact;
  timeZone: string;
  active: boolean;
  /** Eigen rol; null voor een platformbeheerder zonder eigen rol (die mag alles). */
  myRole: MosqueRole | null;
  createdAt: string;
  updatedAt: string;
}

export interface MosqueDetailsRequest {
  name: string;
  description: string | null;
  street: string | null;
  postalCode: string | null;
  city: string | null;
  countryCode: string | null;
  email: string | null;
  phone: string | null;
  websiteUrl: string | null;
  timeZone: string | null;
}

export type Visibility = 'PUBLIC' | 'REGISTERED_USERS';
export type EventCategory =
  'LECTURE' | 'FRIDAY_PROGRAM' | 'CLASS' | 'YOUTH' | 'COMMUNITY' | 'OTHER';
export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED';

export interface AdminEvent {
  id: string;
  mosqueId: string;
  title: string;
  description: string | null;
  category: EventCategory;
  startsAt: string;
  endsAt: string | null;
  location: string | null;
  imageUrl: string | null;
  visibility: Visibility;
  registrationEnabled: boolean;
  capacity: number | null;
  confirmedRegistrations: number;
  availablePlaces: number | null;
  status: EventStatus;
  publishedAt: string | null;
  cancelledAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface EventRequest {
  title: string;
  description: string | null;
  category: EventCategory;
  startsAt: string;
  endsAt: string | null;
  location: string | null;
  imageUrl: string | null;
  visibility: Visibility;
  registrationEnabled: boolean;
  capacity: number | null;
}

export type RegistrationStatus = 'CONFIRMED' | 'CANCELLED';

export interface Registrant {
  registrationId: string;
  userId: string;
  displayName: string;
  email: string;
  status: RegistrationStatus;
  registeredAt: string;
  updatedAt: string;
}

export type MediaCategory = 'LECTURE' | 'KHUTBAH' | 'QURAN' | 'COURSE' | 'YOUTH' | 'OTHER';
export type MediaStatus = 'DRAFT' | 'PUBLISHED';

export interface Video {
  provider: string;
  videoId: string;
  watchUrl: string;
  embedUrl: string;
}

export interface AdminMedia {
  id: string;
  mosqueId: string;
  title: string;
  description: string | null;
  speaker: string | null;
  category: MediaCategory;
  video: Video;
  /** Getoonde thumbnail: de eigen afbeelding of die van YouTube. */
  thumbnailUrl: string | null;
  /** Alleen de zelf ingestelde thumbnail. */
  customThumbnailUrl: string | null;
  durationSeconds: number | null;
  recordedOn: string | null;
  relatedEventId: string | null;
  visibility: Visibility;
  status: MediaStatus;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface MediaRequest {
  title: string;
  description: string | null;
  speaker: string | null;
  category: MediaCategory;
  videoUrl: string;
  thumbnailUrl: string | null;
  durationSeconds: number | null;
  recordedOn: string | null;
  relatedEventId: string | null;
  visibility: Visibility;
}

export type AnnouncementStatus = 'DRAFT' | 'PUBLISHED';

export interface AdminAnnouncement {
  id: string;
  mosqueId: string;
  title: string;
  body: string;
  featured: boolean;
  status: AnnouncementStatus;
  expired: boolean;
  publishedAt: string | null;
  expiresAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AnnouncementRequest {
  title: string;
  body: string;
  featured: boolean;
  expiresAt: string | null;
}

export interface Administrator {
  id: string;
  userId: string;
  email: string;
  displayName: string;
  role: MosqueRole;
  createdAt: string;
}

export interface CreateUserRequest {
  email: string;
  displayName: string;
  password: string;
  platformRole: PlatformRole;
}
