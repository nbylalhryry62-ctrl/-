import 'package:flutter/material.dart';

/// Line chart of exam percentages (oldest to newest) with a 60% pass line.
class TrendChart extends StatelessWidget {
  const TrendChart(this.points, {super.key});
  final List<int> points;
  @override
  Widget build(BuildContext context) => SizedBox(
        height: 120,
        width: double.infinity,
        child: CustomPaint(painter: _Painter(points, Theme.of(context).colorScheme.primary, Theme.of(context).dividerColor)),
      );
}

class _Painter extends CustomPainter {
  _Painter(this.p, this.color, this.grid);
  final List<int> p;
  final Color color, grid;
  @override
  void paint(Canvas c, Size s) {
    final gp = Paint()..color = grid..strokeWidth = 1;
    for (final v in [0, 60, 100]) {
      final y = s.height - s.height * v / 100;
      c.drawLine(Offset(0, y), Offset(s.width, y), gp..color = v == 60 ? color.withValues(alpha: .35) : grid);
    }
    if (p.isEmpty) return;
    final dx = p.length == 1 ? 0.0 : s.width / (p.length - 1);
    Offset at(int i) => Offset(p.length == 1 ? s.width / 2 : dx * i, s.height - s.height * p[i] / 100);
    final path = Path()..moveTo(at(0).dx, at(0).dy);
    for (var i = 1; i < p.length; i++) {
      path.lineTo(at(i).dx, at(i).dy);
    }
    c.drawPath(path, Paint()..color = color..style = PaintingStyle.stroke..strokeWidth = 3..strokeJoin = StrokeJoin.round);
    for (var i = 0; i < p.length; i++) {
      c.drawCircle(at(i), 4, Paint()..color = color);
    }
  }

  @override
  bool shouldRepaint(_Painter o) => o.p != p || o.color != color;
}
