package ru.study.equation;

import java.util.ArrayList;
import java.util.Collections;

class Equation {
    static ArrayList<Double> roots(double a, double b, double c, double d) {
        ArrayList<Double> roots = new ArrayList<>();

        if (d == 0) {
            // Выносим x² за скобки.
            roots.add(0.0);
            addRoots(roots, quadratic(a, -b, -c), false);
        } else if (b == 0) {
            // Биквадратное уравнение: t = x².
            for (double t : quadratic(a, -c, d)) {
                if (t > 0) {
                    roots.add(-Math.sqrt(t));
                    roots.add(Math.sqrt(t));
                }
            }
        } else {
            // Подставляем y = 1/x. Получаем y⁴ + p*y² + q*y + r = 0.
            double p = -c / d;
            double q = -b / d;
            double r = a / d;

            if (a == 0) {
                // При a = 0 делим на y, поскольку y не может быть нулём.
                addRoots(roots, cubic(0, p, q), true);
            } else {
                // Если y⁴ и квадратная часть дают положительную сумму, корней нет.
                if (p >= 0 && r > 0 && q * q <= 4 * p * r) {
                    return roots;
                }
                // Метод Феррари: находим m и раскладываем на два квадратных множителя.
                double m = 0;
                for (double value : cubic(2 * p, p * p - 4 * r, -q * q)) {
                    m = Math.max(m, value);
                }
                // Из уравнения для m можно восстановить очень малое значение.
                if (p > 0 && m < p * 0.000001 && p * p > 4 * r) {
                    m = q * q / ((p + m) * (p + m) - 4 * r);
                }
                if (m <= 0 || Double.isInfinite(m) || Double.isNaN(m)) {
                    throw new ArithmeticException();
                }

                double u = Math.sqrt(m);
                double difference = q / u;
                double sum = p + m;
                if (r > 0) {
                    // Здесь v+w > 0, а (v+w)² = (w−v)² + 4r.
                    sum = Math.sqrt(difference * difference + 4 * r);
                }
                double v = (sum - difference) / 2;
                double w = (sum + difference) / 2;
                // Произведение v*w равно r. Так точнее находится меньший коэффициент.
                if (Math.abs(v) > Math.abs(w)) {
                    w = r / v;
                } else {
                    v = r / w;
                }
                addRoots(roots, quadratic(1, u, v), true);
                addRoots(roots, quadratic(1, -u, w), true);
            }
        }

        Collections.sort(roots);
        for (int i = roots.size() - 1; i > 0; i--) {
            double root = roots.get(i);
            if (Math.abs(root - roots.get(i - 1)) <= 32 * Math.ulp(root)) {
                roots.remove(i);
            }
        }
        return roots;
    }

    private static void addRoots(ArrayList<Double> roots, double[] values, boolean invert) {
        for (double value : values) {
            double root = invert ? 1 / value : value;
            if (Double.isInfinite(root) || Double.isNaN(root)) {
                throw new ArithmeticException();
            }
            roots.add(root);
        }
    }

    private static double[] quadratic(double a, double b, double c) {
        if (a == 0) {
            if (b == 0) {
                return new double[0];
            }
            return new double[]{-c / b};
        }

        double discriminant = b * b - 4 * a * c;
        double tolerance = 16 * Math.ulp(b * b + Math.abs(4 * a * c));
        if (discriminant < -tolerance) {
            return new double[0];
        }
        if (Math.abs(discriminant) <= tolerance) {
            return new double[]{-b / (2 * a)};
        }

        // Сначала считаем корень без вычитания близких чисел.
        double term = -0.5 * (b + Math.copySign(Math.sqrt(discriminant), b));
        return new double[]{term / a, c / term};
    }

    // Формула Кардано для z³ + b*z² + c*z + d = 0.
    private static double[] cubic(double b, double c, double d) {
        double shift = b / 3;
        double p = c - b * shift;
        double q = (2 * shift * shift - c) * shift + d;
        double half = q / 2;
        double third = p / 3;
        double square = half * half;
        double cube = third * third * third;
        double discriminant = square + cube;
        double tolerance = 16 * Math.ulp(square + Math.abs(cube));

        if (discriminant > tolerance) {
            double u = Math.cbrt(-half - Math.copySign(Math.sqrt(discriminant), half));
            return new double[]{u - third / u - shift};
        }
        if (Math.abs(discriminant) <= tolerance) {
            double u = Math.cbrt(-half);
            return new double[]{2 * u - shift, -u - shift};
        }

        // При трёх действительных корнях используется запись через косинус.
        double cosine = -half / Math.sqrt(-cube);
        double angle = Math.acos(Math.max(-1, Math.min(1, cosine))) / 3;
        double[] roots = new double[3];
        for (int i = 0; i < roots.length; i++) {
            roots[i] = 2 * Math.sqrt(-third) * Math.cos(angle - 2 * Math.PI * i / 3) - shift;
        }
        return roots;
    }

    static Probe refine(double[] coefficients, ArrayList<Double> roots) {
        for (int pass = 0; pass < 2; pass++) {
            // У корня чётной кратности знак меняет производная.
            boolean derivative = pass == 1;
            ArrayList<Double> criticalPoints = new ArrayList<>();
            if (derivative) {
                criticalPoints.add(0.0);
                addRoots(criticalPoints,
                        quadratic(4 * coefficients[0], -3 * coefficients[1], -2 * coefficients[2]),
                        false);
            }
            for (int i = 0; i < roots.size(); i++) {
                double root = roots.get(i);
                double left = Math.floor(root);
                double right = Math.ceil(root);
                if (left == right) {
                    left--;
                    right++;
                }
                // Ограничиваем отрезок серединами между соседними корнями.
                if (i > 0) {
                    left = Math.max(left, (roots.get(i - 1) + root) / 2);
                }
                if (i + 1 < roots.size()) {
                    right = Math.min(right, (root + roots.get(i + 1)) / 2);
                }
                // При проверке производной исключаем её соседние нули.
                for (double point : criticalPoints) {
                    double gap = point - root;
                    double tolerance = 128 * Math.ulp(root);
                    if (gap < -tolerance) {
                        left = Math.max(left, (point + root) / 2);
                    } else if (gap > tolerance) {
                        right = Math.min(right, (point + root) / 2);
                    }
                }
                if (left >= right || Math.ulp(root) > 0.01) {
                    continue;
                }
                double startLeft = left;
                double startRight = right;
                double fLeft = value(coefficients, left, derivative);
                double fRight = value(coefficients, right, derivative);
                int tries = 2;
                if (Double.isNaN(fLeft) || Double.isNaN(fRight)) {
                    continue;
                }

                if (fLeft == 0) {
                    right = left;
                } else if (fRight == 0) {
                    left = right;
                } else if (Math.signum(fLeft) != Math.signum(fRight)) {
                    while (right - left > 0.01) {
                        double middle = (left + right) / 2;
                        double fMiddle = value(coefficients, middle, derivative);
                        tries++;
                        if (fMiddle == 0) {
                            left = middle;
                            right = middle;
                        } else if (Math.signum(fLeft) == Math.signum(fMiddle)) {
                            left = middle;
                            fLeft = fMiddle;
                        } else {
                            right = middle;
                        }
                    }
                } else {
                    continue;
                }
                return new Probe(startLeft, startRight, left, right, tries, derivative);
            }
        }
        return null;
    }

    private static double value(double[] coefficients, double x, boolean derivative) {
        double a = coefficients[0];
        double b = coefficients[1];
        double c = coefficients[2];
        double d = coefficients[3];
        if (derivative) {
            return x * ((4 * a * x - 3 * b) * x - 2 * c);
        }
        return (((a * x - b) * x - c) * x) * x + d;
    }

    static class Probe {
        final double startLeft;
        final double startRight;
        final double left;
        final double right;
        final int tries;
        final boolean derivative;

        Probe(double startLeft, double startRight, double left, double right,
              int tries, boolean derivative) {
            this.startLeft = startLeft;
            this.startRight = startRight;
            this.left = left;
            this.right = right;
            this.tries = tries;
            this.derivative = derivative;
        }
    }
}
