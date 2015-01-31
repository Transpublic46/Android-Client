package se.poochoo.search;

import android.content.SearchRecentSuggestionsProvider;

public class CustomSuggestionProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = CustomSuggestionProvider.class.getName();
    public final static int MODE = DATABASE_MODE_QUERIES;
    public CustomSuggestionProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}
