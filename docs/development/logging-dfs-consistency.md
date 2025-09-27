# Logging DFS Consistency

Context: Feature logging should be consistent across shards when using dfs_query_then_fetch. This document summarizes the implementation approach and relevant code paths.

What changed
- Removed legacy manual DFS “priming” during fetch logging.
- Capture global (DFS) term and collection statistics at Weight construction and reuse them during logging/scoring.
- Introduced a per-request Weight cache during fetch logging to ensure reuse of weights built in the correct DFS context.

Key code paths
- com.o19s.es.ltr.logging.LoggingFetchSubPhase
  - Builds a combined query for logging and sets a per-request Weight cache via RankerQuery.setPerRequestWeightCache(Map<Query, Weight>).
  - Creates a single Weight for the rewritten query and clears the cache after constructing the LoggingFetchSubPhaseProcessor.
- com.o19s.es.ltr.query.RankerQuery
  - ThreadLocal Map<Query, Weight> PER_REQUEST_WEIGHT_CACHE.
  - createWeightInternal() reuses weights for feature queries when the cache is present.
- com.o19s.es.termstat.TermStatQuery / TermStatScorer / TermStatSupplier
  - TermStatQuery.TermStatWeight builds TermStates and captures TermStatistics (docFreq, totalTermFreq) and field docCount via IndexSearcher.collectionStatistics().
  - TermStatScorer delegates to TermStatSupplier.bumpPrecomputed(...) to compute df/idf/tf/ttf/tp using precomputed global stats.
- com.o19s.es.ltr.feature.store.ScriptFeature
  - For term stats injection into script features, builds TermStates and calls IndexSearcher.collectionStatistics()/termStatistics() in the Weight constructor. Under dfs_query_then_fetch, these statistics reflect global (DFS) values. Uses TermStatSupplier.bump(...) at scoring time.

Tests
- LoggingDfsConsistencyIT verifies that df/idf-based feature logs are identical across shards under dfs_query_then_fetch with asymmetric shard-level DFs.
- TermStatQuery tests cover basic expression behavior, empty terms, matchCount/unique counts, extractTerms, and toString.
- TermStatSupplier tests verify correct mapping of entrySet() to the underlying stats (df/idf/tf/ttf/tp).

Notable fixes related to this change
- TermStatSupplier.entrySet() returned the df list for all keys; fixed to return the appropriate lists for idf/tf/ttf/tp.
- TermStatQuery.TermStatWeight.extractTerms(...) now correctly adds all query terms to the output set.
- TermStatQuery.toString(String) implemented for improved debugging.

Developer notes
- Per-request Weight cache is intentionally short-lived (created/reset within LoggingFetchSubPhase.getProcessor) to avoid memory leaks.
- TermStatWeight.isCacheable(...) returns true for per-leaf reuse; higher-level query caching is not relied upon for logging.
- When adding new term-stat-based features, ensure they follow the pattern of capturing DFS/global stats at Weight construction for correctness under dfs_query_then_fetch.
