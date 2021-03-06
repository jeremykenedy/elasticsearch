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

package org.elasticsearch.rest.action.document;

import org.apache.logging.log4j.LogManager;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.logging.DeprecationLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestActions;
import org.elasticsearch.rest.action.RestStatusToXContentListener;

import java.io.IOException;

import static org.elasticsearch.rest.RestRequest.Method.DELETE;

public class RestDeleteAction extends BaseRestHandler {
    private static final DeprecationLogger deprecationLogger = new DeprecationLogger(
        LogManager.getLogger(RestDeleteAction.class));
    static final String TYPES_DEPRECATION_MESSAGE = "[types removal] Specifying types in " +
        "document index requests is deprecated, use the /{index}/_doc/{id} endpoint instead.";

    public RestDeleteAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(DELETE, "/{index}/{type}/{id}", this);
    }

    @Override
    public String getName() {
        return "document_delete_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        String type = request.param("type");
        if (!type.equals(MapperService.SINGLE_MAPPING_NAME)) {
            deprecationLogger.deprecatedAndMaybeLog("delete_with_types", TYPES_DEPRECATION_MESSAGE);
        }

        DeleteRequest deleteRequest = new DeleteRequest(request.param("index"), type, request.param("id"));
        deleteRequest.routing(request.param("routing"));
        deleteRequest.timeout(request.paramAsTime("timeout", DeleteRequest.DEFAULT_TIMEOUT));
        deleteRequest.setRefreshPolicy(request.param("refresh"));
        deleteRequest.version(RestActions.parseVersion(request));
        deleteRequest.versionType(VersionType.fromString(request.param("version_type"), deleteRequest.versionType()));

        String waitForActiveShards = request.param("wait_for_active_shards");
        if (waitForActiveShards != null) {
            deleteRequest.waitForActiveShards(ActiveShardCount.parseString(waitForActiveShards));
        }

        return channel -> client.delete(deleteRequest, new RestStatusToXContentListener<>(channel));
    }
}
