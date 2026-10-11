/// Fouten die de app aan de gebruiker kan uitleggen. Schermen reageren op het type en op [ApiException.code],
/// nooit op de technische melding van de server.
sealed class AppFailure implements Exception {
  const AppFailure();
}

/// De server gaf een foutrespons in het uniforme formaat (`{status, code, message, ...}`).
class ApiException extends AppFailure {
  const ApiException({required this.status, required this.code, this.message, this.retryAfter});

  final int status;

  /// Stabiele foutcode van de backend, bijvoorbeeld `EVENT_FULL` of `INVALID_CREDENTIALS`.
  final String code;

  /// Technische melding van de server; alleen voor logging, niet voor de gebruiker.
  final String? message;

  /// Wachttijd bij `429`, uit de header `Retry-After`.
  final Duration? retryAfter;

  bool get isNotFound => status == 404;

  @override
  String toString() => 'ApiException($status, $code)';
}

/// De server was niet bereikbaar: geen verbinding, time-out of een onleesbaar antwoord.
class NetworkException extends AppFailure {
  const NetworkException([this.cause]);

  final Object? cause;

  @override
  String toString() => 'NetworkException($cause)';
}
