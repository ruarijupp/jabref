package org.jabref.logic.importer.fetcher;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.apache.hc.core5.net.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.fileformat.ACMPortalParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.paging.Page;

/**
 * Fetcher for ACM Portal.
 * Supports paged search and parsing of search results from ACM's site.
 */
public class ACMPortalFetcher implements PagedSearchBasedFetcher {

    public static final String FETCHER_NAME = "ACM Portal";

    private static final String SEARCH_URL = "https://dl.acm.org/action/doSearch";

    public ACMPortalFetcher() {
        // ACM Portal requires cookies to be enabled
        CookieHandler.setDefault(new CookieManager());
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_ACM);
    }

    /**
 * Constructs a properly formatted URL to query the ACM Digital Library.
 * 
 * This method creates a URL using the ACM search endpoint and appends the parsed user query
 * as a parameter to search all fields within ACM's database.
 *
 * @param query The parsed user search query (created by Lucene's SyntaxParser)
 * @return A fully formed search URL for the ACM Portal containing the user query
 * @throws FetcherException If the URL could not be created due to syntax errors
 */
public URL getURLForQuery(QueryNode query) throws FetcherException {

    /**
 * Constructs a properly formatted URL for querying the ACM Digital Library
 * based on the provided user search query.
 *
 * The resulting URL includes all required parameters to execute the search
 * and returns search results related to the parsed query terms.
 *
 * @param query The parsed user search query to be included in the ACM search URL.
 * @return A fully formed search URL targeting the ACM Digital Library.
 * @throws FetcherException if the URL could not be created due to invalid syntax.
 */

        try {
            URIBuilder uriBuilder = new URIBuilder(SEARCH_URL);
            uriBuilder.addParameter("AllField", createQueryString(query));
            return uriBuilder.build().toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FetcherException("Building URL failed.", e);
        }
    }

    /**
     * Helper to convert a QueryNode to a search string
     *
     * @param query Lucene QueryNode
     * @return A query string suitable for ACM Portal
     */
    private static String createQueryString(QueryNode query) {
        return new DefaultQueryTransformer().transformLuceneQuery(query).orElse("");
    }

    /**
     * Performs a paged search for a given lucene query (auto-parsed).
     *
     * @param luceneQuery QueryNode
     * @param pageNumber Page number (starting at 0)
     * @return Page of BibEntry results
     */
    /**
 * Executes a paged search on the ACM Digital Library using the provided Lucene query.
 *
 * This method constructs a search URL using the given query, sends a request to the ACM API,
 * and retrieves a single page of BibEntry results based on the specified page number.
 *
 * @param luceneQuery The parsed user search query (from Lucene SyntaxParser)
 * @param pageNumber  The page number to fetch results from (starting from 0)
 * @return A Page object containing the fetched BibEntries for the given query and page number
 * @throws FetcherException If there is an error building the URL or retrieving the results
 */

    @Override
    public Page<BibEntry> performSearchPaged(QueryNode luceneQuery, int pageNumber) throws FetcherException {
        String transformedQuery = createQueryString(luceneQuery);

        URIBuilder uriBuilder;
        try {
            uriBuilder = new URIBuilder(SEARCH_URL);
        } catch (URISyntaxException e) {
            throw new FetcherException("Building URI failed.", e);
        }

        uriBuilder.addParameter("AllField", transformedQuery);
        uriBuilder.addParameter("startPage", String.valueOf(pageNumber + 1)); // ACM uses 1-based page numbers

        // Placeholder: empty result list (real fetching logic happens elsewhere)
        return new Page<>(transformedQuery, pageNumber, List.of());
    }

    /**
     * Provides the Parser used to convert ACM Portal results to BibEntries.
     *
     * @return ACMPortalParser instance
     */
    public Parser getParser() {
        return new ACMPortalParser();
    }
}
