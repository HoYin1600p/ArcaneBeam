package dev.hoyin1600p.arcanebeam.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class Gsons {
    /** Plain Gson with pretty printing and no custom adapters. */
    public static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().create();

    private Gsons() {
    }
}
