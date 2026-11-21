/** Nederlandse omschrijving van een actie in het auditlog. Onbekende acties tonen hun code. */
const ACTIONS: Record<string, string> = {
  MOSQUE_CREATED: 'Moskee geregistreerd',
  MOSQUE_UPDATED: 'Moskeegegevens gewijzigd',
  MOSQUE_IMAGES_UPDATED: 'Logo of omslagfoto gewijzigd',
  MOSQUE_SETTINGS_UPDATED: 'Instellingen van de moskee gewijzigd',
  MOSQUE_STATUS_ACTIVE: 'Moskee geactiveerd',
  MOSQUE_STATUS_INACTIVE: 'Moskee gedeactiveerd',
  MOSQUE_STATUS_BLOCKED: 'Moskee geblokkeerd',
  ADMINISTRATOR_ADDED: 'Beheerder toegevoegd',
  ADMINISTRATOR_ROLE_CHANGED: 'Rol van beheerder gewijzigd',
  ADMINISTRATOR_REMOVED: 'Beheerder verwijderd',
  EVENT_CREATED: 'Activiteit aangemaakt',
  EVENT_UPDATED: 'Activiteit gewijzigd',
  EVENT_PUBLISHED: 'Activiteit gepubliceerd',
  EVENT_UNPUBLISHED: 'Activiteit offline gehaald',
  EVENT_CANCELLED: 'Activiteit geannuleerd',
  EVENT_DELETED: 'Activiteit verwijderd',
  VIDEO_CREATED: 'Video aangemaakt',
  VIDEO_UPDATED: 'Video gewijzigd',
  VIDEO_PUBLISHED: 'Video gepubliceerd',
  VIDEO_UNPUBLISHED: 'Video offline gehaald',
  VIDEO_DELETED: 'Video verwijderd',
  ANNOUNCEMENT_CREATED: 'Aankondiging aangemaakt',
  ANNOUNCEMENT_UPDATED: 'Aankondiging gewijzigd',
  ANNOUNCEMENT_PUBLISHED: 'Aankondiging gepubliceerd',
  ANNOUNCEMENT_UNPUBLISHED: 'Aankondiging offline gehaald',
  ANNOUNCEMENT_DELETED: 'Aankondiging verwijderd',
  PUSH_SENT: 'Pushmelding verstuurd',
  ACCOUNT_CREATED: 'Account aangemaakt',
  ACCOUNT_INVITED: 'Account aangemaakt met uitnodiging',
  ACCOUNT_BLOCKED: 'Account geblokkeerd',
  ACCOUNT_UNBLOCKED: 'Blokkade van account opgeheven',
  ACCOUNT_PASSWORD_CODE_SENT: 'Code voor nieuw wachtwoord verstuurd',
};

export function auditActionLabel(action: string): string {
  return ACTIONS[action] ?? action;
}
