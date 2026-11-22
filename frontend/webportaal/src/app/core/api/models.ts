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
  /** Laatste keer ingelogd met het wachtwoord; null als dat nog nooit is gebeurd (bijv. uitgenodigd). */
  lastLoginAt: string | null;
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

/** ACTIVE: zichtbaar in de app. INACTIVE: verborgen, beheerders kunnen voorbereiden. BLOCKED: verborgen en alleen
 * de platformbeheerder kan haar beheren. */
export type MosqueStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED';

export interface AdminMosque {
  id: string;
  slug: string;
  name: string;
  description: string | null;
  address: Address;
  contact: Contact;
  timeZone: string;
  /** Pad van het logo en de omslagfoto (vanaf de API-basis), of null. */
  logoUrl: string | null;
  coverUrl: string | null;
  logoImageId: string | null;
  coverImageId: string | null;
  status: MosqueStatus;
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
  /** Null: het account krijgt een uitnodiging per e-mail om zelf een wachtwoord te kiezen. */
  password: string | null;
  platformRole: PlatformRole;
}

/** Configuratie van een moskee; alleen de platformbeheerder wijzigt die. */
export interface MosqueSettings {
  registrationsEnabled: boolean;
  pushEnabled: boolean;
}

export interface CreateMosqueRequest {
  /** Null: de backend maakt een unieke slug uit de naam. */
  slug: string | null;
  details: MosqueDetailsRequest;
  firstAdministrator: { email: string; displayName: string } | null;
  settings: MosqueSettings;
}

export interface CreatedMosque {
  mosque: AdminMosque;
  settings: MosqueSettings;
  /** Null als er geen eerste beheerder is opgegeven. invited: er is een nieuw account met uitnodiging. */
  firstAdministrator: { userId: string; invited: boolean } | null;
}

export interface PlatformTotals {
  mosques: { total: number; active: number; inactive: number; blocked: number };
  users: number;
  /** Nieuwe accounts in de laatste 30 dagen. */
  newUsers: number;
  /** Mensen die minstens één moskee volgen. */
  followers: number;
  follows: number;
  events: number;
  upcomingEvents: number;
  publishedVideos: number;
  publishedAnnouncements: number;
  /** Pushmeldingen in de laatste 30 dagen. */
  recentPushNotifications: number;
}

/** Een moskee met haar aantallen, voor het platformoverzicht. */
export interface MosqueCounts {
  id: string;
  slug: string;
  name: string;
  city: string | null;
  status: MosqueStatus;
  createdAt: string;
  administrators: number;
  followers: number;
  events: number;
  videos: number;
  announcements: number;
}

export interface AuditEntry {
  id: string;
  occurredAt: string;
  /** Null als het account inmiddels is verwijderd. */
  actor: { id: string; displayName: string } | null;
  mosqueId: string | null;
  mosqueName: string | null;
  action: string;
  targetType: string;
  targetId: string;
}

export interface StoredImage {
  id: string;
  path: string;
  width: number;
  height: number;
}

export type PushContentType = 'ANNOUNCEMENT' | 'EVENT' | 'MEDIA';
export type PushStatus = 'SENDING' | 'SENT' | 'FAILED';

export interface PushNotification {
  id: string;
  mosqueId: string;
  contentType: PushContentType;
  contentId: string;
  title: string;
  body: string;
  status: PushStatus;
  recipientCount: number;
  deliveredCount: number;
  failedCount: number;
  createdAt: string;
  completedAt: string | null;
}

export interface PushAudience {
  configured: boolean;
  simulated: boolean;
  /** False als de platformbeheerder push voor deze moskee heeft uitgezet. */
  enabledForMosque: boolean;
  followers: number;
  devices: number;
}

/** Leeg of weggelaten: de backend stelt de tekst samen uit het item. */
export interface PushText {
  title: string | null;
  body: string | null;
}
