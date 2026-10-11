/// Kleine hulpfuncties om JSON van de backend veilig te lezen.
typedef Json = Map<String, dynamic>;

extension JsonRead on Json {
  String str(String key) => this[key] as String;

  String? strOrNull(String key) {
    final value = this[key] as String?;
    return value == null || value.trim().isEmpty ? null : value;
  }

  DateTime time(String key) => DateTime.parse(this[key] as String).toUtc();

  DateTime? timeOrNull(String key) => this[key] == null ? null : DateTime.parse(this[key] as String).toUtc();

  int? intOrNull(String key) => (this[key] as num?)?.toInt();

  bool boolOr(String key, bool fallback) => this[key] as bool? ?? fallback;

  Json? objOrNull(String key) => (this[key] as Map?)?.cast<String, dynamic>();

  List<T> list<T>(String key, T Function(Json json) convert) => [
    for (final item in (this[key] as List<dynamic>? ?? const [])) convert((item as Map).cast<String, dynamic>()),
  ];
}

/// Eén pagina uit een lijst-endpoint: `{items, page, size, totalItems, totalPages}`.
class PageResult<T> {
  const PageResult({required this.items, required this.page, required this.totalItems, required this.totalPages});

  factory PageResult.fromJson(Json json, T Function(Json json) convert) => PageResult(
    items: json.list('items', convert),
    page: json['page'] as int,
    totalItems: json['totalItems'] as int,
    totalPages: json['totalPages'] as int,
  );

  final List<T> items;
  final int page;
  final int totalItems;
  final int totalPages;

  bool get hasMore => page + 1 < totalPages;
}
