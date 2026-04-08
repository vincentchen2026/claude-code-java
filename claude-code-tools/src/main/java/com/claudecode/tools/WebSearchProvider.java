package com.claudecode.tools;

/**
 * Interface for web search providers.
 * Implementations integrate with specific search APIs (e.g., Brave, Google, etc.).
 */
public interface WebSearchProvider {

    /**
     * Performs a web search and returns results as text.
     *
     * @param query the search query
     * @return search results formatted as text
     */
    String search(String query);
}
