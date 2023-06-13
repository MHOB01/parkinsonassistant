package com.example.parkinsonassistant;

import com.github.mikephil.charting.data.Entry;

public class SmileyEntry extends Entry {
    private String smiley;

    public SmileyEntry(float x, float y, String smiley) {
        super(x, y);
        this.smiley = smiley;
    }

    public String getSmiley() {
        return smiley;
    }

    @Override
    public Entry copy() {
        return new SmileyEntry(getX(), getY(), smiley);
    }
}





