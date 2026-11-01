package com.example.client.setting;

public class NumberSetting extends Setting {
    private double value;
    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String name, double defaultValue, double min, double max, double step) {
        super(name);
        this.value = defaultValue;
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double getValue() {
        return value;
    }

    public int getValueInt() {
        return (int) Math.round(value);
    }

    public float getValueFloat() {
        return (float) value;
    }

    public void setValue(double value) {
        double clamped = Math.max(min, Math.min(max, value));
        if (step > 0) {
            double steps = Math.round((clamped - min) / step);
            this.value = min + steps * step;
        } else {
            this.value = clamped;
        }
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }
}
