/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.shard;

import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.ShardLock;
import org.elasticsearch.index.cache.bitset.ShardBitsetFilterCache;
import org.elasticsearch.index.cache.filter.ShardFilterCache;
import org.elasticsearch.index.cache.query.ShardQueryCache;
import org.elasticsearch.index.engine.EngineFactory;
import org.elasticsearch.index.engine.InternalEngineFactory;
import org.elasticsearch.index.fielddata.ShardFieldData;
import org.elasticsearch.index.gateway.IndexShardGateway;
import org.elasticsearch.index.gateway.IndexShardGatewayService;
import org.elasticsearch.index.get.ShardGetService;
import org.elasticsearch.index.indexing.ShardIndexingService;
import org.elasticsearch.index.indexing.slowlog.ShardSlowLogIndexingService;
import org.elasticsearch.index.merge.scheduler.ConcurrentMergeSchedulerProvider;
import org.elasticsearch.index.merge.scheduler.MergeSchedulerProvider;
import org.elasticsearch.index.percolator.PercolatorQueriesRegistry;
import org.elasticsearch.index.percolator.stats.ShardPercolateService;
import org.elasticsearch.index.search.slowlog.ShardSlowLogSearchService;
import org.elasticsearch.index.search.stats.ShardSearchService;
import org.elasticsearch.index.snapshots.IndexShardSnapshotAndRestoreService;
import org.elasticsearch.index.store.DirectoryService;
import org.elasticsearch.index.store.Store;
import org.elasticsearch.index.suggest.stats.ShardSuggestService;
import org.elasticsearch.index.termvectors.ShardTermVectorsService;
import org.elasticsearch.index.translog.TranslogService;
import org.elasticsearch.index.warmer.ShardIndexWarmerService;

/**
 * The {@code IndexShardModule} module is responsible for binding the correct
 * shard id, index shard, engine factory, and warming service for a newly
 * created shard.
 */
public class IndexShardModule extends AbstractModule {

    public static final String ENGINE_FACTORY = "index.engine.factory";
    private static final Class<? extends EngineFactory> DEFAULT_ENGINE_FACTORY_CLASS = InternalEngineFactory.class;

    private static final String ENGINE_PREFIX = "org.elasticsearch.index.engine.";
    private static final String ENGINE_SUFFIX = "EngineFactory";

    private final ShardId shardId;
    private final Settings settings;
    private final boolean primary;
    private final ShardFilterCache shardFilterCache;

    public IndexShardModule(ShardId shardId, boolean primary, Settings settings, ShardFilterCache shardFilterCache) {
        this.settings = settings;
        this.shardFilterCache = shardFilterCache;
        this.shardId = shardId;
        this.primary = primary;
        if (settings.get("index.translog.type") != null) {
            throw new IllegalStateException("a custom translog type is no longer supported. got [" + settings.get("index.translog.type") + "]");
        }
    }

    /** Return true if a shadow engine should be used */
    protected boolean useShadowEngine() {
        return primary == false && IndexMetaData.isIndexUsingShadowReplicas(settings);
    }

    @Override
    protected void configure() {
        bind(ShardId.class).toInstance(shardId);
        if (useShadowEngine()) {
            bind(IndexShard.class).to(ShadowIndexShard.class).asEagerSingleton();
        } else {
            bind(IndexShard.class).asEagerSingleton();
            bind(TranslogService.class).asEagerSingleton();
        }

        bind(EngineFactory.class).to(settings.getAsClass(ENGINE_FACTORY, DEFAULT_ENGINE_FACTORY_CLASS, ENGINE_PREFIX, ENGINE_SUFFIX));
        bind(MergeSchedulerProvider.class).to(ConcurrentMergeSchedulerProvider.class).asEagerSingleton();
        bind(ShardIndexWarmerService.class).asEagerSingleton();
        bind(ShardIndexingService.class).asEagerSingleton();
        bind(ShardSlowLogIndexingService.class).asEagerSingleton();
        bind(ShardSearchService.class).asEagerSingleton();
        bind(ShardSlowLogSearchService.class).asEagerSingleton();
        bind(ShardGetService.class).asEagerSingleton();
        bind(ShardFilterCache.class).toInstance(shardFilterCache);
        bind(ShardQueryCache.class).asEagerSingleton();
        bind(ShardBitsetFilterCache.class).asEagerSingleton();
        bind(ShardFieldData.class).asEagerSingleton();
        bind(IndexShardGateway.class).asEagerSingleton();
        bind(IndexShardGatewayService.class).asEagerSingleton();
        bind(PercolatorQueriesRegistry.class).asEagerSingleton();
        bind(ShardPercolateService.class).asEagerSingleton();
        bind(ShardTermVectorsService.class).asEagerSingleton();
        bind(IndexShardSnapshotAndRestoreService.class).asEagerSingleton();
        bind(ShardSuggestService.class).asEagerSingleton();
    }


}