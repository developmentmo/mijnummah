import 'package:flutter_test/flutter_test.dart';
import 'package:mijnummah_app/core/widgets/network_image_box.dart';

void main() {
  test('een geüpload pad komt van de eigen API, een volledige link blijft gelijk', () {
    expect(
      resolveImageUrl('/api/v1/public/images/abc', 'https://api.example.org/'),
      'https://api.example.org/api/v1/public/images/abc',
    );
    expect(
      resolveImageUrl('https://cdn.example.org/a.jpg', 'https://api.example.org'),
      'https://cdn.example.org/a.jpg',
    );
    expect(resolveImageUrl(null, 'https://api.example.org'), isNull);
    expect(resolveImageUrl('', 'https://api.example.org'), isNull);
  });
}
