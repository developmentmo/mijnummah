import '../api/api_exception.dart';

/// Begrijpelijke titel en uitleg bij een fout, zonder technische details.
({String title, String message}) describeFailure(Object error) => switch (error) {
  NetworkException() => (
    title: 'Geen verbinding',
    message: 'We kunnen de moskee nu niet bereiken. Controleer je internetverbinding en probeer het opnieuw.',
  ),
  ApiException(isNotFound: true) => (
    title: 'Niet gevonden',
    message: 'Dit onderdeel bestaat niet meer of is niet openbaar.',
  ),
  ApiException(status: 429) => (
    title: 'Even geduld',
    message: 'Er zijn te veel verzoeken achter elkaar gedaan. Probeer het over een minuut opnieuw.',
  ),
  ApiException(status: 401) => (
    title: 'Opnieuw inloggen',
    message: 'Je sessie is verlopen. Log opnieuw in om verder te gaan.',
  ),
  _ => (
    title: 'Er ging iets mis',
    message: 'Dit kon niet worden geladen. Probeer het opnieuw; blijft het misgaan, laat het de moskee dan weten.',
  ),
};
