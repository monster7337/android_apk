package ru.study.equation;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private EditText[] fields;
    private double[] lastCoefficients;
    private final DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("ru", "RU"));
    private final DecimalFormat number = new DecimalFormat("0.######", symbols);
    private final DecimalFormat scientific = new DecimalFormat("0.#####E0", symbols);
    private final DecimalFormat hundredths = new DecimalFormat("0.00", symbols);

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.main);

        fields = new EditText[]{
                findViewById(R.id.a), findViewById(R.id.b),
                findViewById(R.id.c), findViewById(R.id.d)
        };
        for (EditText field : fields) {
            field.setKeyListener(DigitsKeyListener.getInstance("0123456789-.,"));
            field.setRawInputType(InputType.TYPE_CLASS_NUMBER
                    | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        }
        findViewById(R.id.solve).setOnClickListener(view -> solve());
        fields[3].setOnEditorActionListener((view, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE) {
                solve();
                return true;
            }
            return false;
        });

        if (savedState != null) {
            lastCoefficients = savedState.getDoubleArray("coefficients");
            if (lastCoefficients != null) {
                showSolution(lastCoefficients);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putDoubleArray("coefficients", lastCoefficients);
    }

    private void solve() {
        InputMethodManager keyboard = getSystemService(InputMethodManager.class);
        double[] coefficients = new double[4];
        for (int i = 0; i < fields.length; i++) {
            EditText field = fields[i];
            try {
                double value = Double.parseDouble(field.getText().toString().trim().replace(',', '.'));
                if (Double.isInfinite(value) || Double.isNaN(value)) {
                    throw new NumberFormatException();
                }
                coefficients[i] = value;
            } catch (NumberFormatException error) {
                field.requestFocus();
                field.setError("Введите число");
                keyboard.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT);
                findViewById(R.id.result).setVisibility(View.GONE);
                lastCoefficients = null;
                return;
            }
        }
        lastCoefficients = coefficients;
        keyboard.hideSoftInputFromWindow(fields[0].getWindowToken(), 0);
        findViewById(R.id.screen).requestFocus();
        showSolution(coefficients);

        ScrollView scroll = findViewById(R.id.scroll);
        View result = findViewById(R.id.result);
        scroll.post(() -> scroll.smoothScrollTo(0, result.getTop()));
    }

    private void showSolution(double[] coefficients) {
        double a = coefficients[0];
        double b = coefficients[1];
        double c = coefficients[2];
        double d = coefficients[3];
        findViewById(R.id.result).setVisibility(View.VISIBLE);
        findViewById(R.id.root_list).setVisibility(View.GONE);
        findViewById(R.id.probe).setVisibility(View.GONE);
        TextView method = findViewById(R.id.method);
        if (a == 0 && b == 0 && c == 0) {
            method.setText(d == 0 ? "Подходит любое действительное число" : "Решений нет");
            return;
        }

        ArrayList<Double> roots;
        try {
            roots = Equation.roots(a, b, c, d);
        } catch (ArithmeticException error) {
            method.setText("Не удалось вычислить корни с этими коэффициентами");
            return;
        }
        if (roots.isEmpty()) {
            method.setText("Действительных корней нет");
            return;
        }

        if (d == 0) {
            method.setText("Вынесение x² за скобки");
        } else if (b == 0) {
            method.setText("Замена t = x²");
        } else if (a == 0) {
            method.setText("Формула Кардано");
        } else {
            method.setText("Метод Феррари");
        }

        StringBuilder left = new StringBuilder();
        StringBuilder right = new StringBuilder();
        String indices = "₁₂₃₄";
        for (int i = 0; i < roots.size(); i++) {
            StringBuilder column = i % 2 == 0 ? left : right;
            if (column.length() > 0) {
                column.append('\n');
            }
            column.append("x").append(indices.charAt(i)).append(" ≈ ").append(format(roots.get(i)));
        }
        findViewById(R.id.root_list).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.roots_left)).setText(left);
        TextView rightColumn = findViewById(R.id.roots_right);
        rightColumn.setText(right);
        rightColumn.setVisibility(right.length() == 0 ? View.GONE : View.VISIBLE);

        Equation.Probe probe = Equation.refine(coefficients, roots);
        findViewById(R.id.probe).setVisibility(View.VISIBLE);
        TextView approximation = findViewById(R.id.approximation);
        TextView details = findViewById(R.id.probe_details);
        if (probe == null) {
            approximation.setVisibility(View.GONE);
            details.setText("Не удалось уточнить корень до 0,01");
            return;
        }
        double middle = (probe.left + probe.right) / 2;
        approximation.setVisibility(View.VISIBLE);
        approximation.setText("x ≈ " + hundredths.format(middle == 0 ? 0 : middle));

        String text = "Начальный отрезок от " + format(probe.startLeft)
                + " до " + format(probe.startRight);
        if (probe.left != probe.right) {
            text += "\nИтоговый отрезок от " + format(probe.left) + " до " + format(probe.right);
        }
        text += "\nКоличество проб " + probe.tries;
        if (probe.derivative) {
            text += "\nДля кратного корня проверялась производная";
        }
        details.setText(text);
    }

    private String format(double value) {
        double magnitude = Math.abs(value);
        if (magnitude >= 10000000 || (magnitude > 0 && magnitude < 0.000001)) {
            return scientific.format(value);
        }
        return number.format(value == 0 ? 0 : value);
    }
}
