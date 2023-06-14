package com.example.parkinsonassistant;

import com.github.mikephil.charting.data.Entry;

public class SmileyEntry extends Entry {
    private String smiley; // Stores the smiley associated with the entry

    public SmileyEntry(float x, float y, String smiley) {
        super(x, y); // Call the constructor of the superclass (Entry) with provided x and y values
        this.smiley = smiley; // Set the smiley value
    }

    public String getSmiley() {
        return smiley; // Get the smiley value
    }

    @Override
    public Entry copy() {
        return new SmileyEntry(getX(), getY(), smiley); // Create a copy of the SmileyEntry object with the same x, y, and smiley values
    }
}






